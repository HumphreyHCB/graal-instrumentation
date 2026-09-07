#!/usr/bin/env bash

JAVA_BIN=./latest_graalvm_home/bin/java
CP=/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar
LOG_FILE=./Bubo_AllBenchmarks.log

# RUNS count (second Harness argument), taken from your Sieve example: "Harness Sieve 20 10000"
RUNS=500

# extra_args for each benchmark (from your JSON snippet)
declare -A EXTRA_ARGS=(
  [Mandelbrot]=750
  [NBody]=1200000
  [Sieve]=10000
  [Json]=100
  [Bounce]=10000
  [LoopBenchmarks]=12000
)

{
  echo "=== Bubo run of all AWFY benchmarks ==="
  date
  echo

  for BENCH in DeltaBlue Richards Json CD Havlak Bounce List Mandelbrot NBody Permute Queens Sieve Storage Towers; do
    EXTRA=${EXTRA_ARGS[$BENCH]}
    echo "------------------------------------------------------------"
    echo "Benchmark: $BENCH  (extra_args=$EXTRA, RUNS=$RUNS)"
    echo "Command: Harness $BENCH $RUNS $EXTRA"
    echo "Timestamp: $(date)"
    echo



    "${JAVA_BIN}" \
      -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
      --enable-native-access=ALL-UNNAMED \
      -Djdk.graal.TrackNodeSourcePosition=true \
      -Djdk.graal.IsolatedLoopHeaderAlignment=0 \
      -Djdk.graal.LoopHeaderAlignment=0 \
      -Djdk.graal.StrictProfiles=false \
      -Djdk.graal.DisableCodeEntryAlignment=true \
      -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithoutProbe/AWFY/"$BENCH"/Final_"$BENCH".json \
      -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
      -cp "${CP}" \
      Harness "${BENCH}" "${RUNS}" "${EXTRA}"

    echo
    echo "Finished $BENCH at $(date)"
    echo
  done

  echo "=== All benchmarks completed at $(date) ==="
} >"${LOG_FILE}" 2>&1
