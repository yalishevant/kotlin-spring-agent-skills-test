#!/usr/bin/env bash
# compare.sh — Compare benchmark results across modes
#
# Usage: ./compare.sh [results-dir]
#
# Reads all summary.json files from results/ and produces a comparison table.
# Compatible with Bash 3.2+ (macOS default).

set -euo pipefail

RESULTS_DIR="${1:-$(dirname "${BASH_SOURCE[0]}")/../results}"

if [[ ! -d "$RESULTS_DIR" ]]; then
  echo "No results directory found at $RESULTS_DIR" >&2
  echo "Run benchmarks first with: ./run.sh <benchmark> <mode>" >&2
  exit 1
fi

echo "# Benchmark Comparison Report"
echo ""
echo "Generated: $(date -u +"%Y-%m-%d %H:%M:%S UTC")"
echo ""

# Collect results into parallel arrays (Bash 3.2 compatible, no declare -A)
score_keys=()
score_vals=()

for summary in "$RESULTS_DIR"/*/summary.json; do
  [[ -f "$summary" ]] || continue

  bench=$(grep -o '"benchmark": "[^"]*"' "$summary" | head -1 | sed 's/.*: "//;s/"//')
  mode=$(grep -o '"mode": "[^"]*"' "$summary" | head -1 | sed 's/.*: "//;s/"//')

  total_passed=0
  total_failed=0

  while IFS= read -r line; do
    if [[ "$line" =~ \"passed\":\ ([0-9]+) ]]; then
      ((total_passed += BASH_REMATCH[1])) || true
    fi
    if [[ "$line" =~ \"failed\":\ ([0-9]+) ]]; then
      ((total_failed += BASH_REMATCH[1])) || true
    fi
  done < "$summary"

  total=$((total_passed + total_failed))
  if [[ $total -gt 0 ]]; then
    pct=$((total_passed * 100 / total))
  else
    pct=0
  fi

  score_keys+=("${bench}|${mode}")
  score_vals+=("${total_passed}/${total} (${pct}%)")
done

# Helper: lookup score by key
lookup_score() {
  local key="$1"
  for i in "${!score_keys[@]}"; do
    if [[ "${score_keys[$i]}" == "$key" ]]; then
      echo "${score_vals[$i]}"
      return
    fi
  done
  echo "—"
}

# Determine unique benchmarks and modes
benchmarks=()
modes=()
for key in "${score_keys[@]}"; do
  bench="${key%%|*}"
  mode="${key##*|}"
  local_found=false
  for b in "${benchmarks[@]+"${benchmarks[@]}"}"; do
    [[ "$b" == "$bench" ]] && local_found=true
  done
  $local_found || benchmarks+=("$bench")

  local_found=false
  for m in "${modes[@]+"${modes[@]}"}"; do
    [[ "$m" == "$mode" ]] && local_found=true
  done
  $local_found || modes+=("$mode")
done

# Sort
IFS=$'\n' benchmarks=($(sort <<<"$(printf '%s\n' "${benchmarks[@]}")"))
IFS=$'\n' modes=($(sort <<<"$(printf '%s\n' "${modes[@]}")"))
unset IFS

# Print comparison table
header="| Benchmark"
separator="| ---"
for mode in "${modes[@]}"; do
  header+=" | $mode"
  separator+=" | ---"
done
header+=" |"
separator+=" |"

echo "$header"
echo "$separator"

for bench in "${benchmarks[@]}"; do
  row="| $bench"
  for mode in "${modes[@]}"; do
    val=$(lookup_score "${bench}|${mode}")
    row+=" | $val"
  done
  row+=" |"
  echo "$row"
done

echo ""

# Totals row
echo "| **TOTAL**"
for mode in "${modes[@]}"; do
  mode_passed=0
  mode_total=0
  for i in "${!score_keys[@]}"; do
    k="${score_keys[$i]}"
    if [[ "${k##*|}" == "$mode" ]]; then
      v="${score_vals[$i]}"
      p="${v%%/*}"
      t="${v#*/}"
      t="${t%% *}"
      ((mode_passed += p)) || true
      ((mode_total += t)) || true
    fi
  done
  if [[ $mode_total -gt 0 ]]; then
    mode_pct=$((mode_passed * 100 / mode_total))
  else
    mode_pct=0
  fi
  printf " | **%d/%d (%d%%)**" "$mode_passed" "$mode_total" "$mode_pct"
done
echo " |"

echo ""
echo "---"
echo ""
echo "## Legend"
echo "- **passed/total (%)** — automated checks passed out of total"
echo "- Modes: claude+skills, claude-skills, codex+skills, codex-skills"
echo "- Higher % = better AI performance on automated checks"
echo "- LLM-as-Judge scores must be run separately (see llm-judge-prompt.md)"
