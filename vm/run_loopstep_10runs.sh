#!/usr/bin/env bash
set -euo pipefail

# =========================
# Paths and config
# =========================

ROOT="/home/hb478/repos/BuboExperiments/BuboLoopStepOeverhead"
DATA_DIR="${ROOT}/Data"

JAVA="./latest_graalvm_home/bin/java"
CP="/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar"

BENCH="InnerLoopStepBenchmark"
ITER=500
EXTRA=750

RUNS=10

mkdir -p "${DATA_DIR}"

# =========================
# Common JVM flags
# =========================

COMMON_FLAGS=(
  -XX:+UnlockExperimentalVMOptions
  -XX:+UnlockDiagnosticVMOptions
  -XX:+EnableJVMCI
  -XX:+UseJVMCICompiler
  -XX:+UseJVMCINativeLibrary
  -XX:+DebugNonSafepoints
  -XX:-TieredCompilation
  -XX:-BackgroundCompilation
  -Djdk.graal.GTAssignDebug=false
  -Djdk.graal.HumphreysDebugData=false
  -Djdk.graal.StrictProfiles=false
  -Djdk.graal.LoadProfiles="/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithProbe/AWFY/InnerLoopStepBenchmark/InnerLoopStepBenchmark_CompilerReplay"
  -Djdk.graal.TrackNodeSourcePosition=true
  -Djdk.graal.LIRGTSlowDown=false
  --enable-native-access=ALL-UNNAMED
  -cp "${CP}"
)

# =========================
# Run loop
# =========================

for i in $(seq 1 "${RUNS}"); do
  echo
  echo "=============================="
  echo " Run ${i}/${RUNS}"
  echo "=============================="

  # -------------------------
  # Nested (all loops)
  # -------------------------
  echo "[RUN ${i}] Nested"
  "${JAVA}" \
    "${COMMON_FLAGS[@]}" \
    -Djdk.graal.BuboLIRPhase=true \
    Harness "${BENCH}" "${ITER}" "${EXTRA}" \
    > "${DATA_DIR}/run${i}.txt" 2>&1

done

echo
echo "All ${RUNS} runs complete."
echo "Output written to ${DATA_DIR}"
