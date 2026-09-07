#!/usr/bin/env bash
set -euo pipefail

# Print a useful message if anything fails
trap 'ec=$?; echo; echo "[ERROR] Script failed (exit=$ec) at line $LINENO:"; echo "  $BASH_COMMAND"; exit $ec' ERR

# =========================
# Config
# =========================
JAVA_BIN="./latest_graalvm_home/bin/java"
CP="/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar"

RUNS=500

OUT_ROOT="./bubo_runs23_BuboNoNested"
LOG_DIR="${OUT_ROOT}/logs"

mkdir -p "${LOG_DIR}"

# =========================
# Benchmarks to run
# ONLY these will be executed
# =========================
declare -A EXTRA_ARGS=(
  [Mandelbrot]=750
  [NBody]=1200000
  [Sieve]=10000
  [Json]=100
  [Bounce]=10000
  [LoopBenchmarks]=12000
)


# =========================
# Paths (for compiler replay)
# =========================
SLOWDOWN_ROOT="/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithoutProbe/AWFY"

# =========================
# Sanity checks (fail loudly)
# =========================
[[ -x "${JAVA_BIN}" ]] || { echo "[ERROR] JAVA_BIN not found or not executable: ${JAVA_BIN}"; exit 1; }
[[ -f "${CP}" ]] || { echo "[ERROR] benchmarks.jar not found: ${CP}"; exit 1; }

# =========================
# One run
# =========================
run_one() {
  local bench="$1"
  local extra="$2"
  local bubo_phase="$3"
  local tag="$4"

  local log_file="${LOG_DIR}/${bench}_${tag}.log"
  local replay_file="${SLOWDOWN_ROOT}/${bench}/${bench}_CompilerReplay"

  echo "[INFO] Running ${bench} (${tag}), log -> ${log_file}"


  {
    echo "============================================================"
    echo "Benchmark: ${bench}"
    echo "BuboLIRPhase: ${bubo_phase}"
    echo "RUNS: ${RUNS}"
    echo "extra_args: ${extra}"
    echo "Replay file: ${replay_file}"
    echo "Timestamp: $(date)"
    echo

    "${JAVA_BIN}" \
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

  echo "[OK] ${bench} ${tag} completed"
}

# =========================
# LoopBenchmarks special run
# Harness LoopBenchmarks 12000 1
# =========================
run_loopbenchmarks() {
  local bench="LoopBenchmarks"
  local extra="${EXTRA_ARGS[LoopBenchmarks]}"
  local bubo_phase="$1"
  local tag="$2"

  local log_file="${LOG_DIR}/${bench}_${tag}.log"
  local replay_file="${SLOWDOWN_ROOT}/${bench}/${bench}_CompilerReplay"

  echo "[INFO] Running ${bench} (${tag}), log -> ${log_file}"

  {
    echo "============================================================"
    echo "Benchmark: ${bench}"
    echo "BuboLIRPhase: ${bubo_phase}"
    echo "Signature: Harness ${bench} ${extra} 1"
    echo "Replay file: ${replay_file}"
    echo "Timestamp: $(date)"
    echo

    "${JAVA_BIN}" \
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
      Harness "${bench}" "${extra}" 1

    echo
    echo "Finished ${bench} ${tag} at $(date)"
    echo "============================================================"
  } > "${log_file}" 2>&1

  echo "[OK] ${bench} ${tag} completed"
}

# =========================
# Main
# =========================
echo "=== Bubo run started at $(date) ==="
echo "Benchmarks to run:"
for b in "${!EXTRA_ARGS[@]}"; do
  echo "  ${b} (extra=${EXTRA_ARGS[$b]})"
done
echo

BENCH_ORDER=(Mandelbrot NBody Sieve Json Bounce)

for BENCH in "${BENCH_ORDER[@]}"; do
  EXTRA="${EXTRA_ARGS[$BENCH]}"
  run_one "${BENCH}" "${EXTRA}" "true" "BuboOn"
done

run_loopbenchmarks "true" "BuboOn"

echo
echo "=== All benchmarks completed at $(date) ==="
echo "Logs written to ${LOG_DIR}"
