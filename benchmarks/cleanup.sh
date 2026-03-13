#!/usr/bin/env bash
# cleanup.sh — prune generated benchmark artifacts while preserving analytics.
#
# Default behavior:
#   - removes copied result workspaces under results/*/workspace
#   - removes copied result workspaces under results-v1-backup/*/workspace
#   - removes benchmark starter build caches under benchmarks/*/starter
#   - preserves summary.json, eval-step-*.json, step logs, and reports
#
# Usage:
#   ./cleanup.sh
#   ./cleanup.sh --dry-run
#   ./cleanup.sh --artifacts-only
#   ./cleanup.sh --purge-v1-backup

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
RESULTS_DIR="${RESULTS_DIR:-$REPO_ROOT/results}"
RESULTS_V1_BACKUP_DIR="${RESULTS_V1_BACKUP_DIR:-$REPO_ROOT/results-v1-backup}"
BENCHMARKS_DIR="${BENCHMARKS_DIR:-$SCRIPT_DIR}"

DRY_RUN=false
ARTIFACTS_ONLY=false
PURGE_V1_BACKUP=false

RESULT_TARGETS=()
STARTER_TARGETS=()
PURGE_TARGETS=()

usage() {
  cat <<'EOF'
Usage: ./cleanup.sh [options]

Options:
  --dry-run          Print what would be removed without deleting anything
  --artifacts-only   Keep result workspace source snapshots; remove only build/cache dirs
  --purge-v1-backup  Remove results-v1-backup/ entirely after conservative cleanup
  -h, --help         Show this help

Environment:
  RESULTS_DIR            Override current results directory
  RESULTS_V1_BACKUP_DIR  Override archived V1 results directory
  BENCHMARKS_DIR         Override benchmarks directory
EOF
}

format_kb() {
  local kb="${1:-0}"
  awk -v kb="$kb" 'BEGIN {
    if (kb >= 1048576) {
      printf "%.1fG", kb / 1048576
    } else if (kb >= 1024) {
      printf "%.1fM", kb / 1024
    } else {
      printf "%dK", kb
    }
  }'
}

measure_targets() {
  MEASURE_COUNT=0
  MEASURE_KB=0
  local target
  local kb

  for target in "$@"; do
    [[ -e "$target" ]] || continue
    MEASURE_COUNT=$((MEASURE_COUNT + 1))
    kb=$(du -sk "$target" 2>/dev/null | awk '{print $1}')
    kb="${kb:-0}"
    MEASURE_KB=$((MEASURE_KB + kb))
  done
}

collect_result_targets() {
  local root="$1"
  [[ -d "$root" ]] || return 0

  if $ARTIFACTS_ONLY; then
    while IFS= read -r target; do
      [[ -n "$target" ]] && RESULT_TARGETS+=("$target")
    done < <(find "$root" -type d \( -name build -o -name .gradle -o -name .kotlin -o -name .benchmark-skills \) -print)
  else
    while IFS= read -r target; do
      [[ -n "$target" ]] && RESULT_TARGETS+=("$target")
    done < <(find "$root" -type d -name workspace -prune -print)
  fi
}

collect_starter_targets() {
  [[ -d "$BENCHMARKS_DIR" ]] || return 0

  while IFS= read -r target; do
    [[ -n "$target" ]] && STARTER_TARGETS+=("$target")
  done < <(find "$BENCHMARKS_DIR" -maxdepth 3 \
    \( -path '*/starter/build' -o -path '*/starter/.gradle' -o -path '*/starter/.kotlin' \) \
    -type d -print)
}

print_targets() {
  local target
  for target in "$@"; do
    echo "  - $target"
  done
}

delete_targets() {
  local target
  for target in "$@"; do
    [[ -e "$target" ]] || continue
    rm -rf "$target"
  done
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      ;;
    --artifacts-only)
      ARTIFACTS_ONLY=true
      ;;
    --purge-v1-backup)
      PURGE_V1_BACKUP=true
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
  shift
done

collect_result_targets "$RESULTS_DIR"
if ! $PURGE_V1_BACKUP; then
  collect_result_targets "$RESULTS_V1_BACKUP_DIR"
fi
collect_starter_targets

if $PURGE_V1_BACKUP && [[ -d "$RESULTS_V1_BACKUP_DIR" ]]; then
  PURGE_TARGETS+=("$RESULTS_V1_BACKUP_DIR")
fi

measure_targets "${RESULT_TARGETS[@]+"${RESULT_TARGETS[@]}"}"
result_count="$MEASURE_COUNT"
result_kb="$MEASURE_KB"

measure_targets "${STARTER_TARGETS[@]+"${STARTER_TARGETS[@]}"}"
starter_count="$MEASURE_COUNT"
starter_kb="$MEASURE_KB"

measure_targets "${PURGE_TARGETS[@]+"${PURGE_TARGETS[@]}"}"
purge_count="$MEASURE_COUNT"
purge_kb="$MEASURE_KB"

total_count=$((result_count + starter_count + purge_count))
total_kb=$((result_kb + starter_kb + purge_kb))

if [[ "$total_count" -eq 0 ]]; then
  echo "Nothing to clean."
  exit 0
fi

mode_label="workspace snapshots"
$ARTIFACTS_ONLY && mode_label="workspace build artifacts"

echo "Cleanup plan:"
echo "  - result $mode_label: $result_count target(s), $(format_kb "$result_kb")"
echo "  - benchmark starter build caches: $starter_count target(s), $(format_kb "$starter_kb")"
if [[ "$purge_count" -gt 0 ]]; then
  echo "  - purge archived V1 backup: $purge_count target(s), $(format_kb "$purge_kb")"
fi
echo "  - total: $total_count target(s), $(format_kb "$total_kb")"

if $DRY_RUN; then
  echo ""
  echo "Dry run; nothing deleted."
  [[ "$result_count" -gt 0 ]] && print_targets "${RESULT_TARGETS[@]+"${RESULT_TARGETS[@]}"}"
  [[ "$starter_count" -gt 0 ]] && print_targets "${STARTER_TARGETS[@]+"${STARTER_TARGETS[@]}"}"
  [[ "$purge_count" -gt 0 ]] && print_targets "${PURGE_TARGETS[@]+"${PURGE_TARGETS[@]}"}"
  exit 0
fi

delete_targets "${RESULT_TARGETS[@]+"${RESULT_TARGETS[@]}"}"
delete_targets "${STARTER_TARGETS[@]+"${STARTER_TARGETS[@]}"}"
delete_targets "${PURGE_TARGETS[@]+"${PURGE_TARGETS[@]}"}"

echo "Cleanup complete."
