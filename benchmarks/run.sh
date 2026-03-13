#!/usr/bin/env bash
# run.sh — Benchmark runner for Kotlin/Spring AI skill evaluation
#
# Runs a benchmark in one of 4 modes:
#   claude+skills, claude-skills, codex+skills, codex-skills
#
# Usage:
#   ./run.sh <benchmark-id> <mode> [--dry-run]
#
# Examples:
#   ./run.sh B-01 claude+skills
#   ./run.sh B-03 codex-skills
#   ./run.sh all claude+skills          # run all benchmarks
#   ./run.sh B-01 claude+skills --dry-run  # print commands, don't execute
#
# Prerequisites:
#   - claude (Claude Code CLI) installed and authenticated
#   - codex (OpenAI Codex CLI) installed and authenticated
#   - Java 17+, Gradle installed or wrapper will be used
#
# Environment variables:
#   BENCHMARK_DIR  — path to benchmarks/ directory (default: script's directory)
#   SKILLS_DIR     — path to agent-skills/ directory (default: ../agent-skills)
#   RESULTS_DIR    — where to store results (default: ../results)
#   MAX_FIX_LOOPS  — number of fix iterations (default: 3)
#   CLAUDE_MODEL   — Claude model to use (default: none, uses CLI default)
#   CODEX_MODEL    — Codex model override (default: none, uses config.toml)
#   BENCHMARK_CLEANUP_AFTER_RUN — if set to 1, runs cleanup.sh after benchmarks finish

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

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BENCHMARK_DIR="${BENCHMARK_DIR:-$SCRIPT_DIR}"
SKILLS_DIR="${SKILLS_DIR:-$(dirname "$SCRIPT_DIR")/agent-skills}"
RESULTS_DIR="${RESULTS_DIR:-$(dirname "$SCRIPT_DIR")/results}"
MAX_FIX_LOOPS="${MAX_FIX_LOOPS:-3}"
CLAUDE_MODEL="${CLAUDE_MODEL:-}"
CODEX_MODEL="${CODEX_MODEL:-}"
AGENT_TIMEOUT_SECONDS="${AGENT_TIMEOUT_SECONDS:-900}"
AGENT_WORK_ROOT="${AGENT_WORK_ROOT:-${TMPDIR:-/tmp}/benchmark-agent-work}"
BENCHMARK_CLEANUP_AFTER_RUN="${BENCHMARK_CLEANUP_AFTER_RUN:-0}"

BENCHMARK_ID="${1:?Usage: run.sh <benchmark-id|all> <mode> [--dry-run]}"
MODE="${2:?Usage: run.sh <benchmark-id|all> <mode>. Modes: claude+skills, claude-skills, codex+skills, codex-skills}"
DRY_RUN="${3:-}"

# --- Validate mode ---

case "$MODE" in
  claude+skills|claude-skills|codex+skills|codex-skills) ;;
  *) echo "ERROR: Invalid mode '$MODE'. Use: claude+skills, claude-skills, codex+skills, codex-skills" >&2; exit 1 ;;
esac

# Parse mode components
AGENT=$(echo "$MODE" | sed 's/[+-].*//')
WITH_SKILLS=false
[[ "$MODE" == *"+skills" ]] && WITH_SKILLS=true

# --- Helpers ---

timestamp() { date -u +"%Y%m%dT%H%M%SZ"; }

log() { echo "[$(timestamp)] $*" >&2; }

get_benchmarks() {
  if [[ "$BENCHMARK_ID" == "all" ]]; then
    find "$BENCHMARK_DIR" -name "meta.yaml" -exec dirname {} \; | sort
  else
    local dir="$BENCHMARK_DIR"/$BENCHMARK_ID*
    # shellcheck disable=SC2086
    if compgen -G "$dir" > /dev/null; then
      echo $dir
    else
      echo "ERROR: Benchmark '$BENCHMARK_ID' not found" >&2
      exit 1
    fi
  fi
}

get_step_count() {
  local bench_dir="$1"
  grep "^steps:" "$bench_dir/meta.yaml" | awk '{print $2}'
}

get_meta_value() {
  local bench_dir="$1"
  local key="$2"
  awk -F':' -v key="$key" '
    $1 == key {
      value = substr($0, index($0, ":") + 1)
      sub(/^[[:space:]]+/, "", value)
      sub(/^"/, "", value)
      sub(/"$/, "", value)
      print value
      exit
    }
  ' "$bench_dir/meta.yaml"
}

get_max_fix_loops() {
  local bench_dir="$1"
  local meta_value
  meta_value=$(get_meta_value "$bench_dir" "max_fix_loops")
  if [[ -n "$meta_value" ]]; then
    echo "$meta_value"
  else
    echo "$MAX_FIX_LOOPS"
  fi
}

get_bench_name() {
  basename "$1"
}

collect_skill_dirs() {
  for tier_glob in "$SKILLS_DIR"/tier-*-*/*; do
    if [[ -d "$tier_glob" ]]; then
      echo "$tier_glob"
    fi
  done
}

stage_skills_bundle() {
  local workspace="$1"
  local bundle_dir="$workspace/.benchmark-skills"
  mkdir -p "$bundle_dir"

  while IFS= read -r skill_dir; do
    cp -R "$skill_dir" "$bundle_dir/"
  done < <(collect_skill_dirs | sort)
}

skill_description() {
  local skill_file="$1"
  sed -n 's/^description:[[:space:]]*//p' "$skill_file" | head -n 1 | sed 's/^"//; s/"$//'
}

# Build the CLAUDE.md / AGENTS.md content for with-skills mode.
# Provides skill descriptions + extracts concrete pattern sections so agents
# get actionable guidance without overwhelming context (~5-10KB vs 152KB full).
build_skills_context() {
  local workspace="$1"

  echo "# Kotlin + Spring Development Skills"
  echo ""
  echo "You have expert skill guides available under \`.benchmark-skills/\`."
  echo "Read the relevant SKILL.md when you need the full diagnostic workflow."
  echo ""

  # List all skills with one-line descriptions
  for skill_dir in "$workspace"/.benchmark-skills/*; do
    if [[ -f "$skill_dir/SKILL.md" ]]; then
      local skill_name
      skill_name=$(basename "$skill_dir")
      local skill_desc
      skill_desc=$(skill_description "$skill_dir/SKILL.md")
      echo "- **$skill_name**: ${skill_desc:-See SKILL.md} → \`.benchmark-skills/$skill_name/SKILL.md\`"
    fi
  done

  echo ""
  echo "---"
  echo ""
  echo "## Critical Patterns — Apply These Directly"
  echo ""

  # Extract concrete pattern sections from skills that have them.
  # These are the sections between "## <Pattern Title>" and the next "## " heading,
  # specifically the ones containing code blocks that agents should copy-paste.
  for skill_dir in "$workspace"/.benchmark-skills/*; do
    if [[ -f "$skill_dir/SKILL.md" ]]; then
      # Extract sections with "Pattern" or "Concrete" in heading (the actionable ones)
      awk '
        /^## .*[Pp]attern|^## .*[Cc]oncrete|^## .*[Ee]xpand.[Cc]ontract|^## .*[Tt]ri.[Ss]tate/ { found=1; print; next }
        found && /^## / { found=0 }
        found { print }
      ' "$skill_dir/SKILL.md"
    fi
  done

  echo ""
  echo "## Key Kotlin + Spring Rules"
  echo ""
  echo "### Entity & Persistence"
  echo "- JPA entities must NOT be \`data class\` — use regular class with manual equals/hashCode by ID"
  echo "- Keep DTOs and entities separate. Don't expose persistence model as API contract."
  echo "- \`FetchType.LAZY\` by default. Use \`@EntityGraph\` or \`JOIN FETCH\` for eager loading."
  echo "- \`toString()\` must not reference lazy collections — triggers unexpected loading"
  echo "- Use \`orphanRemoval = true\` on bidirectional \`@OneToMany\` when children shouldn't exist alone"
  echo ""
  echo "### Proxy & AOP"
  echo "- Self-invocation — \`this.method()\` bypasses \`@Transactional\`/\`@Cacheable\`/\`@CacheEvict\` proxies. Inject self or use \`@CachePut\`."
  echo "- Private methods CANNOT be proxied by Spring AOP — move to public or extract to separate bean."
  echo "- Kotlin classes are final by default. \`kotlin(\"plugin.spring\")\` opens annotated classes for proxying."
  echo "- \`kotlin(\"plugin.jpa\")\` generates no-arg constructors for \`@Entity\` classes."
  echo "- \`@EnableCaching\` required for \`@Cacheable\`/\`@CacheEvict\`. \`@EnableAsync\` required for \`@Async\`. \`@EnableRetry\` required for \`@Retryable\`. Without these, annotations silently do nothing."
  echo ""
  echo "### Transactions"
  echo "- \`@Transactional\` only works on proxied public entry points — not private, not self-invoked."
  echo "- \`REQUIRES_NEW\` creates independent tx — use for audit-on-failure. \`REQUIRED\` joins caller tx — use for outbox-on-success."
  echo "- \`@TransactionalEventListener(AFTER_COMMIT)\` instead of \`@EventListener\` for side-effect-on-success."
  echo "- Never hold a DB transaction open across external HTTP calls."
  echo "- Use \`@Version\` for optimistic locking on concurrent updates. Catch \`OptimisticLockingFailureException\` and RETRY the operation (use \`@Retryable\` or a manual retry loop)."
  echo "- Optimistic lock retry pattern: add \`spring-retry\` dependency, \`@EnableRetry\` on config, \`@Retryable(include = [OptimisticLockingFailureException::class], maxAttempts = 3)\` on the service method."
  echo "- Unchecked exceptions roll back by default; checked exceptions may NOT roll back."
  echo ""
  echo "### N+1 Query Prevention"
  echo "- When a service loops a collection and queries per item (\`for (x in list) { repo.findByX(x.id) }\`), replace with batch query: \`repo.findByXIn(ids)\`."
  echo "- For parent entity with lazy children: add \`@EntityGraph(attributePaths = [\"children\"])\` on the repository method, or use \`@Query(\"SELECT DISTINCT p FROM Parent p LEFT JOIN FETCH p.children\")\`."
  echo "- For aggregate summaries: collect IDs first, then use one batch query (\`findByVariantIdIn(ids)\`), NOT per-item loops."
  echo "- If you add a new repository method (\`findAllWithVariants\`), update the SERVICE to call it — not the old \`findAll()\`."
  echo ""
  echo "### Batch Processing"
  echo "- NEVER use \`REQUIRES_NEW\` per row in batch imports — causes partial commits on failure."
  echo "- Wrap entire batch in single \`@Transactional\`, collect ALL errors in a \`mutableListOf<String>()\`, throw composite exception AFTER processing ALL rows."
  echo "- Process ALL rows before rejecting — stopping at first error loses visibility into remaining problems."
  echo "- Error collection pattern: \`val errors = mutableListOf<String>()\`; loop all rows; add to errors on failure; after loop: \`if (errors.isNotEmpty()) throw BatchImportException(..., errors)\`."
  echo ""
  echo "### Duplicate Prevention"
  echo "- Add \`@Table(uniqueConstraints = [...])\` for business-key uniqueness — application checks alone have race conditions."
  echo "- Add \`findByXAndY\` repository method for application-level duplicate guard with clean error messages."
  echo "- Both DB constraint + app check together: app catches common case, DB catches concurrent race."
  echo ""
  echo "### Serialization"
  echo "- Register \`jackson-module-kotlin\` for Kotlin data class deserialization."
  echo "- Register \`JavaTimeModule\` for \`java.time.*\` types (Instant, LocalDate, etc.)."
  echo "- Sealed class polymorphism: use \`@JsonTypeInfo\` + \`@JsonSubTypes\` on the sealed parent."
  echo "- PATCH null semantics: \`?:\` (Elvis) cannot distinguish omitted from explicit null — use tri-state wrapper."
  echo ""
  echo "### Validation"
  echo "- In Kotlin data class DTOs, use \`@field:NotBlank\`, \`@field:Positive\`, \`@field:Min(1)\` — NOT bare \`@NotBlank\`, \`@Positive\`, \`@Min(1)\`."
  echo "- Without \`@field:\`, annotations target constructor params, not fields — Bean Validation silently skips them."
  echo "- Always use \`@Valid\` on controller \`@RequestBody\` parameters to trigger validation."
  echo ""
  echo "### Security"
  echo "- \`@PreAuthorize\`/\`@PostAuthorize\` require \`@EnableMethodSecurity\` on a \`@Configuration\` class — without it, annotations are silently ignored."
  echo "- URL patterns in \`SecurityFilterChain\` protect URLs. Method-level \`@PreAuthorize\` protects individual service methods."
  echo ""
  echo "### Migration & Schema"
  echo "- Column renames: NEVER use ALTER RENAME directly — use expand-contract (add new, dual-write both, drop old later)."
  echo ""
  echo "### Configuration Binding"
  echo "- Use immutable \`data class\` with \`val\` for \`@ConfigurationProperties\` — NOT \`lateinit var\`."
  echo "- Convert \`Long\`/\`Int\` timeouts to \`java.time.Duration\` type. Change YAML from \`timeout: 5000\` to \`timeout: 5000ms\` or \`timeout: 5s\`. Spring auto-binds Duration."
  echo "- Required properties must NOT have defaults — let startup fail fast on missing config."
  echo "- When converting: change \`lateinit var baseUrl: String\` to \`val baseUrl: String\` (constructor param), change \`var timeout: Long = 0\` to \`val timeout: Duration\`."
  echo ""
  echo "### Error Handling"
  echo "- Handle \`DataIntegrityViolationException\` in \`@RestControllerAdvice\` → return 409 Conflict, not 500."
  echo "- Service-level duplicate check (\`existsByEmail\`) gives friendly message; DB constraint catches race conditions."
  echo "- Return \`201 Created\` for POST creation endpoints, not \`200 OK\`."
  echo "- Never leak SQL details, stack traces, or class names in error responses."
  echo "- Map downstream/gateway failures to proper status codes: timeout → 502 Bad Gateway or 504 Gateway Timeout, not 500. Use \`@RestControllerAdvice\` with \`@ExceptionHandler\` for centralized error mapping."
  echo "- When a service calls an external gateway and catches exceptions to record failure state, RETHROW gateway-level exceptions (GatewayTimeoutException, GatewayException) AFTER saving state. Do NOT swallow them in a blanket \`catch (e: Exception)\`. The \`@RestControllerAdvice\` needs to see the exception to map it to 502/504. Pattern: catch specific gateway exceptions first, save FAILED status, then \`throw e\`. Only catch generic \`Exception\` for non-gateway failures."
  echo ""
  echo "### Resilience"
  echo "- Spring Retry requires ALL THREE: (1) \`spring-retry\` + \`spring-boot-starter-aop\` dependencies, (2) \`@EnableRetry\` on a \`@Configuration\` class, (3) \`@Retryable\` on the target method. Missing ANY one makes retry silently non-functional."
  echo "- When you see a gateway/HTTP client that can fail transiently, add \`@Retryable(retryFor = [GatewayTimeoutException::class], maxAttempts = 3, backoff = @Backoff(delay = 200, multiplier = 2.0))\` on the method, and ensure \`@EnableRetry\` is on a config class."
  echo "- Always add \`spring-boot-starter-aop\` alongside \`spring-retry\` — it provides the AOP proxy infrastructure that \`@Retryable\` needs."
  echo ""
  echo "### Kotlin Idioms"
  echo "- Never use \`!!\` — prefer safe calls, \`requireNotNull\`, or sealed class results."
  echo "- Use MockK, not Mockito for Kotlin tests."
}

workspace_signature() {
  local workspace="$1"
  (
    cd "$workspace"
    find . \
      -path './build' -prune -o \
      -path './.gradle' -prune -o \
      -type f -print \
      | LC_ALL=C sort \
      | while IFS= read -r file; do
          shasum "$file"
        done \
      | shasum \
      | awk '{print $1}'
  )
}

mask_sensitive_assets() {
  local bench_dir="$1"
  local mask_state_file="$2"
  local lock_dir="$bench_dir/.mask_lock"

  # Use mkdir for atomic lock acquisition to prevent TOCTOU race conditions
  # when two benchmark runs for the same benchmark ID run in parallel.
  # mkdir is atomic on POSIX — it either succeeds or fails, no race window.
  if ! mkdir "$lock_dir" 2>/dev/null; then
    # Another run already masked these files — record that we did NOT mask
    echo "SKIP" > "$mask_state_file"
    return
  fi

  : > "$mask_state_file"
  for asset in eval.yaml eval-hidden.sh hidden-tests; do
    local asset_path="$bench_dir/$asset"
    if [[ -e "$asset_path" ]]; then
      local asset_mode
      asset_mode=$(stat -f '%Lp' "$asset_path")
      printf '%s\t%s\n' "$asset" "$asset_mode" >> "$mask_state_file"
      chmod 000 "$asset_path"
    fi
  done
}

unmask_sensitive_assets() {
  local bench_dir="$1"
  local mask_state_file="$2"
  local lock_dir="$bench_dir/.mask_lock"

  if [[ -f "$mask_state_file" ]]; then
    local first_line
    first_line=$(head -1 "$mask_state_file" 2>/dev/null)
    if [[ "$first_line" == "SKIP" ]]; then
      # We did not do the masking — don't try to unmask
      rm -f "$mask_state_file"
      return
    fi
    while IFS=$'\t' read -r asset asset_mode; do
      if [[ -n "$asset" && -e "$bench_dir/$asset" ]]; then
        chmod "$asset_mode" "$bench_dir/$asset"
      fi
    done < "$mask_state_file"
    rm -f "$mask_state_file"
    rm -rf "$lock_dir"
  fi
}

write_agent_step_meta() {
  local meta_file="$1"
  local agent_exit_code="$2"
  local timed_out="$3"
  local workspace_changed="$4"
  local elapsed_seconds="$5"
  local agent_status="$6"

  cat > "$meta_file" <<EOF
{
  "exit_code": $agent_exit_code,
  "timed_out": $timed_out,
  "workspace_changed": $workspace_changed,
  "elapsed_seconds": $elapsed_seconds,
  "status": "$agent_status"
}
EOF
}

run_agent_command() {
  local workspace="$1"
  local prompt_file="$2"
  local step_log="$3"
  local timeout_seconds="$4"
  local timeout_flag="$5"
  RUN_AGENT_EXIT_CODE=0

  rm -f "$timeout_flag"

  case "$AGENT" in
    claude)
      local claude_args="--print --dangerously-skip-permissions"
      [[ -n "$CLAUDE_MODEL" ]] && claude_args="$claude_args --model $CLAUDE_MODEL"
      (
        cd "$workspace"
        TIMEOUT_FLAG="$timeout_flag" perl -e '
          use POSIX qw(setpgid);
          my $timeout = shift @ARGV;
          my $pid = fork();
          die "fork failed" unless defined $pid;
          if ($pid == 0) {
            setpgid(0, 0);
            exec @ARGV or die "exec failed";
          }
          local $SIG{ALRM} = sub {
            if ($ENV{TIMEOUT_FLAG}) {
              open my $fh, ">", $ENV{TIMEOUT_FLAG};
              close $fh;
            }
            kill "TERM", -$pid;
            sleep 5;
            kill "KILL", -$pid;
          };
          alarm $timeout;
          waitpid($pid, 0);
          my $status = $?;
          alarm 0;
          exit($status >> 8);
        ' "$timeout_seconds" bash -lc 'set -o pipefail; cat "$1" | claude '"$claude_args"' 2>&1 | tee -a "$2"' _ "$prompt_file" "$step_log"
      ) || RUN_AGENT_EXIT_CODE=$?
      ;;
    codex)
      local codex_args="exec --dangerously-bypass-approvals-and-sandbox"
      [[ -n "$CODEX_MODEL" ]] && codex_args="$codex_args --model $CODEX_MODEL"
      (
        cd "$workspace"
        TIMEOUT_FLAG="$timeout_flag" perl -e '
          use POSIX qw(setpgid);
          my $timeout = shift @ARGV;
          my $pid = fork();
          die "fork failed" unless defined $pid;
          if ($pid == 0) {
            setpgid(0, 0);
            exec @ARGV or die "exec failed";
          }
          local $SIG{ALRM} = sub {
            if ($ENV{TIMEOUT_FLAG}) {
              open my $fh, ">", $ENV{TIMEOUT_FLAG};
              close $fh;
            }
            kill "TERM", -$pid;
            sleep 5;
            kill "KILL", -$pid;
          };
          alarm $timeout;
          waitpid($pid, 0);
          my $status = $?;
          alarm 0;
          exit($status >> 8);
        ' "$timeout_seconds" bash -lc 'set -o pipefail; cat "$1" | codex '"$codex_args"' - 2>&1 | tee -a "$2"' _ "$prompt_file" "$step_log"
      ) || RUN_AGENT_EXIT_CODE=$?
      ;;
  esac
}

# --- Run a single benchmark ---

run_benchmark() {
  local bench_dir="$1"
  local bench_name
  bench_name=$(get_bench_name "$bench_dir")
  local steps
  steps=$(get_step_count "$bench_dir")
  local bench_max_fix_loops
  bench_max_fix_loops=$(get_max_fix_loops "$bench_dir")
  local run_id="${bench_name}_${MODE}_$(timestamp)"
  local result_dir="$RESULTS_DIR/$run_id"
  local result_workspace="$result_dir/workspace"
  local agent_root
  local workspace

  log "Starting: $bench_name | mode=$MODE | steps=$steps | max_fix_loops=$bench_max_fix_loops | agent_timeout=${AGENT_TIMEOUT_SECONDS}s"

  mkdir -p "$result_dir"
  mkdir -p "$AGENT_WORK_ROOT"
  mkdir -p "$result_workspace"

  agent_root=$(mktemp -d "$AGENT_WORK_ROOT/agent-run.XXXXXX")
  workspace="$agent_root/workspace"
  mkdir -p "$workspace"

  # If benchmark has starter code, copy it into workspace
  if [[ -d "$bench_dir/starter" ]]; then
    cp -r "$bench_dir/starter/." "$workspace/"
    log "  Starter code copied to workspace"
    # Ensure gradle wrapper exists
    if [[ ! -f "$workspace/gradlew" ]]; then
      (cd "$workspace" && gradle wrapper --gradle-version 9.4.0 --quiet) 2>/dev/null || true
      log "  Gradle wrapper generated"
    fi
  fi

  # If with-skills, create the skills context file
  if $WITH_SKILLS; then
    stage_skills_bundle "$workspace"
    if [[ "$AGENT" == "claude" ]]; then
      build_skills_context "$workspace" > "$workspace/CLAUDE.md"
      log "  Skills injected into CLAUDE.md"
    else
      build_skills_context "$workspace" > "$workspace/AGENTS.md"
      log "  Skills injected into AGENTS.md"
    fi
  fi

  # Run each step
  for step in $(seq 1 "$steps"); do
    local step_file="$bench_dir/step-${step}.md"
    if [[ ! -f "$step_file" ]]; then
      log "  WARNING: $step_file not found, skipping step $step"
      continue
    fi

    local prompt
    prompt=$(cat "$step_file")

    log "  Step $step/$steps: sending prompt to $AGENT"

    if [[ -n "$DRY_RUN" ]]; then
      log "  [DRY RUN] Would send prompt from $step_file to $AGENT in $workspace"
      continue
    fi

    # Execute with the agent
    local step_log="$result_dir/step-${step}.log"
    local prompt_file="$result_dir/prompt-step-${step}.txt"
    printf "%s" "$prompt" > "$prompt_file"
    local attempt=0

    while [[ $attempt -le $bench_max_fix_loops ]]; do
      if [[ $attempt -gt 0 ]]; then
        log "  Step $step — fix loop $attempt/$bench_max_fix_loops"
        # Run eval to get feedback
        local eval_output
        eval_output=$("$BENCHMARK_DIR/eval-check.sh" "$bench_dir" "$workspace" "$step" 2>/dev/null || true)
        local failed
        failed=$(echo "$eval_output" | grep -o '"failed": [0-9]*' | awk '{print $2}')

        if [[ "${failed:-0}" == "0" ]]; then
          log "  Step $step — all checks pass!"
          break
        fi

        # Feed eval output back to agent as fix prompt
        prompt="The automated evaluation found issues. Fix them:\n\n$eval_output\n\nFix the failing checks and ensure all tests pass."
      fi

      # Wrap prompt with execution instructions for non-interactive mode
      local wrapped_prompt
      wrapped_prompt="IMPORTANT: You are running in non-interactive benchmark mode. Implement the solution DIRECTLY — do NOT propose approaches, do NOT ask for confirmation, do NOT brainstorm alternatives. Write all code files immediately. The project must compile and tests must pass.

$prompt"
      printf "%s" "$wrapped_prompt" > "$prompt_file"

      local before_signature
      before_signature=$(workspace_signature "$workspace")
      local mask_state_file="$result_dir/step-${step}.masked-assets"
      local timeout_flag="$result_dir/step-${step}.timed-out"
      local agent_meta_file="$result_dir/agent-step-${step}.json"
      local started_at
      started_at=$(date +%s)
      local agent_exit_code=0

      mask_sensitive_assets "$bench_dir" "$mask_state_file"
      run_agent_command "$workspace" "$prompt_file" "$step_log" "$AGENT_TIMEOUT_SECONDS" "$timeout_flag"
      agent_exit_code="$RUN_AGENT_EXIT_CODE"
      unmask_sensitive_assets "$bench_dir" "$mask_state_file"

      local finished_at
      finished_at=$(date +%s)
      local elapsed_seconds=$((finished_at - started_at))
      local after_signature
      after_signature=$(workspace_signature "$workspace")
      local workspace_changed=false
      [[ "$before_signature" != "$after_signature" ]] && workspace_changed=true
      local timed_out=false
      [[ -f "$timeout_flag" ]] && timed_out=true

      local agent_status="changed"
      if [[ "$timed_out" == "true" ]]; then
        agent_status="timed_out"
      elif [[ "$workspace_changed" == "false" ]]; then
        agent_status="no_edit"
      elif [[ "$agent_exit_code" -ne 0 ]]; then
        agent_status="changed_nonzero_exit"
      fi

      write_agent_step_meta "$agent_meta_file" "$agent_exit_code" "$timed_out" "$workspace_changed" "$elapsed_seconds" "$agent_status"

      if [[ "$agent_exit_code" -ne 0 ]]; then
        log "  WARNING: $AGENT exited non-zero (code=$agent_exit_code, status=$agent_status)"
      fi

      ((attempt++)) || true
    done

    # Run final eval for this step
    log "  Step $step — running evaluation"
    "$BENCHMARK_DIR/eval-check.sh" "$bench_dir" "$workspace" "$step" > "$result_dir/eval-step-${step}.json" 2>"$result_dir/eval-step-${step}.log" || true
  done

  cp -R "$workspace/." "$result_workspace/"
  rm -rf "$agent_root"

  # Summary
  log "Completed: $bench_name | results in $result_dir"

  # Combine all step evals into a summary
  {
    echo "{"
    echo "  \"benchmark\": \"$bench_name\","
    echo "  \"mode\": \"$MODE\","
    echo "  \"timestamp\": \"$(timestamp)\","
    echo "  \"agent_runs\": ["
    local first_agent=true
    for step in $(seq 1 "$steps"); do
      local agent_file="$result_dir/agent-step-${step}.json"
      if [[ -s "$agent_file" ]]; then
        $first_agent || echo "    ,"
        first_agent=false
        echo "    $(cat "$agent_file")"
      fi
    done
    echo "  ],"
    echo "  \"steps\": ["
    local first=true
    for step in $(seq 1 "$steps"); do
      local eval_file="$result_dir/eval-step-${step}.json"
      if [[ -s "$eval_file" ]]; then
        $first || echo "    ,"
        first=false
        echo "    $(cat "$eval_file")"
      fi
    done
    echo "  ]"
    echo "}"
  } > "$result_dir/summary.json"
}

# --- Main ---

log "Benchmark runner starting"
log "  Agent: $AGENT"
log "  Skills: $WITH_SKILLS"
[[ -n "$CLAUDE_MODEL" ]] && log "  Claude model: $CLAUDE_MODEL"
[[ -n "$CODEX_MODEL" ]] && log "  Codex model: $CODEX_MODEL"
log "  Benchmarks dir: $BENCHMARK_DIR"
log "  Skills dir: $SKILLS_DIR"
log "  Results dir: $RESULTS_DIR"
log ""

mkdir -p "$RESULTS_DIR"

for bench_dir in $(get_benchmarks); do
  run_benchmark "$bench_dir"
done

log ""
log "All benchmarks complete. Results in $RESULTS_DIR"

if [[ "$BENCHMARK_CLEANUP_AFTER_RUN" == "1" ]] && [[ "$DRY_RUN" != "--dry-run" ]]; then
  log "Post-run cleanup enabled; pruning generated artifacts"
  "$BENCHMARK_DIR/cleanup.sh"
fi
