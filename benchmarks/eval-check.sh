#!/usr/bin/env bash
# eval-check.sh — Automated evaluation checker for benchmark solutions
# Usage: ./eval-check.sh <benchmark-dir> <solution-dir> [step]
#
# Reads eval.yaml from <benchmark-dir> and runs checks against <solution-dir>.
# If [step] is specified, only runs checks for that step.
# Outputs a JSON report to stdout.

set -euo pipefail

# Kotlin 2.0.x daemon cannot parse JDK 25+ version strings.
# Force JDK 17 if available and JAVA_HOME is not already set to a compatible JDK.
if [[ -z "${JAVA_HOME:-}" ]] || [[ "$(java -version 2>&1 | head -1)" == *"25."* ]]; then
  for jdk17 in /Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home \
                /Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home \
                /usr/lib/jvm/java-17-openjdk-amd64; do
    if [[ -d "$jdk17" ]]; then
      export JAVA_HOME="$jdk17"
      break
    fi
  done
fi

BENCHMARK_DIR="${1:?Usage: eval-check.sh <benchmark-dir> <solution-dir> [step]}"
SOLUTION_DIR="${2:?Usage: eval-check.sh <benchmark-dir> <solution-dir> [step]}"
STEP="${3:-all}"

EVAL_FILE="$BENCHMARK_DIR/eval.yaml"

if [[ ! -f "$EVAL_FILE" ]]; then
  echo "ERROR: eval.yaml not found at $EVAL_FILE" >&2
  exit 1
fi

cd "$SOLUTION_DIR"

# --- Helpers ---

pass_count=0
fail_count=0
results=()

record_result() {
  local name="$1" status="$2" message="${3:-}"
  if [[ "$status" == "PASS" ]]; then
    ((pass_count++)) || true
  else
    ((fail_count++)) || true
  fi
  results+=("{\"name\":\"$name\",\"status\":\"$status\",\"message\":\"$message\"}")
}

# --- Gate checks ---

run_gates() {
  echo "=== Running gates ===" >&2

  # Extract gate commands from eval.yaml (simple parsing)
  local gate_names=()
  local gate_cmds=()
  local in_gates=false
  local current_cmd="" current_name=""

  process_gate() {
    if [[ -n "$current_cmd" ]]; then
      gate_cmds+=("$current_cmd")
      gate_names+=("$current_name")
      current_cmd=""
      current_name=""
    fi
  }

  while IFS= read -r line; do
    if [[ "$line" =~ ^gates: ]]; then
      in_gates=true
      continue
    fi
    if $in_gates; then
      if [[ "$line" =~ ^[a-z] && ! "$line" =~ ^[[:space:]] ]]; then
        in_gates=false
        process_gate
        continue
      fi
      if [[ "$line" =~ ^[[:space:]]*-\ *name:\ *\"(.+)\" ]]; then
        process_gate
        current_name="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*-\ *name:\ *(.+) ]]; then
        process_gate
        current_name="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ command:\ *\"(.+)\" ]]; then
        current_cmd="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ command:\ *(.+) ]]; then
        current_cmd="${BASH_REMATCH[1]}"
      fi
    fi
  done < "$EVAL_FILE"
  process_gate

  local gate_passed=true
  for i in "${!gate_cmds[@]}"; do
    local cmd="${gate_cmds[$i]}"
    local name="${gate_names[$i]:-Gate $((i+1))}"
    echo "  Gate: $name ($cmd)" >&2
    if eval "$cmd" > /dev/null 2>&1; then
      record_result "GATE: $name" "PASS"
      echo "    -> PASS" >&2
    else
      record_result "GATE: $name" "FAIL" "Command failed: $cmd"
      echo "    -> FAIL" >&2
      gate_passed=false
    fi
  done

  if ! $gate_passed; then
    echo "  Gates failed — skipping automated checks" >&2
    return 1
  fi
  return 0
}

# --- Automated checks ---

run_checks_for_step() {
  local step_key="$1"
  echo "=== Checks for $step_key ===" >&2

  # Parse checks from eval.yaml for this step
  local in_step=false
  local check_name="" check_type="" check_pattern="" check_glob="" check_fail_msg="" check_count="" check_cmd=""

  process_check() {
    if [[ -z "$check_name" || -z "$check_type" ]]; then
      return
    fi

    echo "  Check: $check_name (type=$check_type)" >&2

    case "$check_type" in
      grep_present)
        if rg -q "$check_pattern" --glob "${check_glob:-**/*}" . 2>/dev/null; then
          record_result "$check_name" "PASS"
          echo "    -> PASS" >&2
        else
          record_result "$check_name" "FAIL" "${check_fail_msg:-Pattern not found: $check_pattern}"
          echo "    -> FAIL (pattern not found)" >&2
        fi
        ;;
      grep_absent)
        if rg -q "$check_pattern" --glob "${check_glob:-**/*}" . 2>/dev/null; then
          record_result "$check_name" "FAIL" "${check_fail_msg:-Pattern should be absent: $check_pattern}"
          echo "    -> FAIL (pattern found but should be absent)" >&2
        else
          record_result "$check_name" "PASS"
          echo "    -> PASS" >&2
        fi
        ;;
      test_count_min)
        local actual_count
        actual_count=$(rg -c "@Test" --glob "**/*.kt" --glob "**/*.java" . 2>/dev/null | awk -F: '{s+=$NF}END{print s+0}')
        local min_count="${check_count:-0}"
        if [[ "$actual_count" -ge "$min_count" ]]; then
          record_result "$check_name" "PASS" "Found $actual_count tests (min: $min_count)"
          echo "    -> PASS ($actual_count >= $min_count)" >&2
        else
          record_result "$check_name" "FAIL" "Found $actual_count tests, need at least $min_count"
          echo "    -> FAIL ($actual_count < $min_count)" >&2
        fi
        ;;
      command)
        local cmd_output=""
        if cmd_output=$(eval "$check_cmd" 2>&1); then
          record_result "$check_name" "PASS"
          echo "    -> PASS" >&2
        else
          local fail_msg="${check_fail_msg:-Command failed: $check_cmd}"
          if [[ -n "$cmd_output" ]]; then
            fail_msg="$fail_msg | $cmd_output"
          fi
          record_result "$check_name" "FAIL" "$fail_msg"
          echo "    -> FAIL (command failed)" >&2
        fi
        ;;
      review_bug_count)
        # Special scoring for B-14 — just check review doc exists
        if [[ -f "REVIEW.md" ]]; then
          record_result "$check_name" "PASS" "Review document found — manual bug counting required"
          echo "    -> PASS (review exists, manual scoring needed)" >&2
        else
          record_result "$check_name" "FAIL" "REVIEW.md not found"
          echo "    -> FAIL" >&2
        fi
        ;;
      *)
        record_result "$check_name" "SKIP" "Unknown check type: $check_type"
        echo "    -> SKIP (unknown type)" >&2
        ;;
    esac

    check_name="" check_type="" check_pattern="" check_glob="" check_fail_msg="" check_count="" check_cmd=""
  }

  while IFS= read -r line; do
    # Detect start of our step section
    if [[ "$line" =~ ^[[:space:]]*${step_key}: ]]; then
      in_step=true
      continue
    fi

    if $in_step; then
      # End of our step section (next step or top-level key)
      if [[ "$line" =~ ^[[:space:]]{2}step_ || ( "$line" =~ ^[a-z] && ! "$line" =~ ^[[:space:]] ) ]]; then
        process_check
        in_step=false
        continue
      fi

      # New check item
      if [[ "$line" =~ ^[[:space:]]*-\ *name:\ *\"(.+)\" ]]; then
        process_check
        check_name="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*-\ *name:\ *(.+) ]]; then
        process_check
        check_name="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*type:\ *\"?([a-z_]+)\"? ]]; then
        check_type="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*pattern:\ *[\"\'](.+)[\"\'] ]]; then
        check_pattern="${BASH_REMATCH[1]}"
        # Unescape YAML double-backslash sequences to single backslash for rg
        check_pattern="${check_pattern//\\\\/\\}"
      elif [[ "$line" =~ ^[[:space:]]*pattern:\ *(.+) ]]; then
        check_pattern="${BASH_REMATCH[1]}"
        check_pattern="${check_pattern//\\\\/\\}"
      elif [[ "$line" =~ ^[[:space:]]*glob:\ *\"(.+)\" ]]; then
        check_glob="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*glob:\ *(.+) ]]; then
        check_glob="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*command:\ *\"(.+)\" ]]; then
        check_cmd="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*command:\ *(.+) ]]; then
        check_cmd="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*fail_message:\ *\"(.+)\" ]]; then
        check_fail_msg="${BASH_REMATCH[1]}"
      elif [[ "$line" =~ ^[[:space:]]*count:\ *([0-9]+) ]]; then
        check_count="${BASH_REMATCH[1]}"
      fi
    fi
  done < "$EVAL_FILE"

  # Process last check
  if $in_step; then
    process_check
  fi
}

# --- Main ---

echo "Evaluating: $(basename "$BENCHMARK_DIR") in $SOLUTION_DIR" >&2
echo "Step: $STEP" >&2
echo "" >&2

if run_gates; then
  if [[ "$STEP" == "all" ]]; then
    # Run all steps found in eval.yaml
    for step_key in $(grep -oE 'step_[0-9]+' "$EVAL_FILE" | sort -u); do
      run_checks_for_step "$step_key"
    done
  else
    run_checks_for_step "step_$STEP"
  fi
fi

# --- Output JSON report ---

echo "" >&2
echo "=== Summary: $pass_count passed, $fail_count failed ===" >&2

json_results=$(printf "%s," "${results[@]}" | sed 's/,$//')
cat <<EOF
{
  "benchmark": "$(basename "$BENCHMARK_DIR")",
  "solution_dir": "$SOLUTION_DIR",
  "step": "$STEP",
  "passed": $pass_count,
  "failed": $fail_count,
  "total": $((pass_count + fail_count)),
  "results": [$json_results]
}
EOF
