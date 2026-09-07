#!/usr/bin/env bash
set -euo pipefail

# =========================
# Config
# =========================
JAVA_BIN="./latest_graalvm_home/bin/java"
CP="/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar"

RUNS=12000

OUT_ROOT="./bubo_runs4_BuboWithDebug"
LOG_DIR="${OUT_ROOT}/logs"
ASYNC_DIR="${OUT_ROOT}/async"
SUMMARY_FILE="${OUT_ROOT}/async_results.txt"

mkdir -p "${LOG_DIR}" "${ASYNC_DIR}"

# =========================
# Benchmarks to run
# ONLY these will be executed
# =========================
# declare -A EXTRA_ARGS=(
#   [Mandelbrot]=750
#   [NBody]=1200000
#   [Sieve]=10000
#   [Json]=100
#   [Bounce]=10000
#   [LoopBenchmarks]=12000
# )

declare -A EXTRA_ARGS=(
  [LoopBenchmarks]=12000
)

# =========================
# Paths
# =========================
SLOWDOWN_ROOT="/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithoutProbe/AWFY"
ASYNC_LIB="/home/hb478/repos/are-we-fast-yet/Async/async-profiler-4.2.1-linux-x64/lib/libasyncProfiler.so"

ASYNC_EVENT="cpu"
ASYNC_INTERVAL="10"
ASYNC_OUTPUT="txt"

# =========================
# One run
# =========================
run_one() {
  local bench="$1"
  local extra="$2"
  local bubo_phase="$3"
  local tag="$4"

  local log_file="${LOG_DIR}/${bench}_${tag}.log"
  local async_file="${ASYNC_DIR}/${bench}_${tag}.txt"

  local slowdown_file="${SLOWDOWN_ROOT}/${bench}/Final_${bench}.json"
  local replay_file="${SLOWDOWN_ROOT}/${bench}/${bench}_CompilerReplay"

  mkdir -p "${SLOWDOWN_ROOT}/${bench}"

  {
    echo "============================================================"
    echo "Benchmark: ${bench}"
    echo "BuboLIRPhase: ${bubo_phase}"
    echo "RUNS: ${RUNS}"
    echo "extra_args: ${extra}"
    echo "Timestamp: $(date)"
    echo

    "${JAVA_BIN}" \
      -agentpath:"${ASYNC_LIB}"=start,event="${ASYNC_EVENT}",interval="${ASYNC_INTERVAL}",output="${ASYNC_OUTPUT}",file="${async_file}" \
      -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
      --enable-native-access=ALL-UNNAMED \
      -Djdk.graal.TrackNodeSourcePosition=true \
      -Djdk.graal.IsolatedLoopHeaderAlignment=0 \
      -Djdk.graal.LoopHeaderAlignment=0 \
      -Djdk.graal.StrictProfiles=false \
      -Djdk.graal.BuboLIRPhase="${bubo_phase}" \
      -Djdk.graal.LoadProfiles="${replay_file}" \
      -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary \
      -XX:-TieredCompilation -XX:-BackgroundCompilation \
      -cp "${CP}" \
      Harness "${bench}" "${RUNS}" "${extra}"

    echo
    echo "Finished ${bench} ${tag} at $(date)"
    echo "============================================================"
  } > "${log_file}" 2>&1

  echo "${async_file}"
}

# =========================
# Main
# =========================
: > "${SUMMARY_FILE}"

echo "=== Bubo run started at $(date) ==="
echo "Benchmarks to run:"
printf "  %s\n" "${!EXTRA_ARGS[@]}"
echo

for BENCH in "${!EXTRA_ARGS[@]}"; do
  EXTRA="${EXTRA_ARGS[$BENCH]}"

  ASYNC_PATH="$(run_one "${BENCH}" "${EXTRA}" "true"  "BuboOn")"
  echo "${BENCH} BuboOn  ${ASYNC_PATH}" >> "${SUMMARY_FILE}"

  ASYNC_PATH="$(run_one "${BENCH}" "${EXTRA}" "false" "BuboOff")"
  echo "${BENCH} BuboOff ${ASYNC_PATH}" >> "${SUMMARY_FILE}"
done

echo
echo "=== All benchmarks completed at $(date) ==="
echo "Async summary written to ${SUMMARY_FILE}"
