#!/usr/bin/env bash

AGENT=/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar
MODULE=jdk.graal.compiler          # or jdk.internal.vm.compiler on some JDKs
BUBO_JAR=/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/bubo-runtime.jar

JAVA_BIN=./latest_graalvm_home/bin/java
CP=/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar
LOG_FILE=./Bubo_AllBenchmarks.log

# Warmup count (second Harness argument), taken from your Sieve example: "Harness Sieve 20 10000"
WARMUP=40

# extra_args for each benchmark (from your JSON snippet)
declare -A EXTRA_ARGS=(
  [Mandelbrot]=750
  [NBody]=1200000
  [Permute]=3500
  [Queens]=5000
  [Sieve]=10000
  [Storage]=1800
  [Towers]=2500
  [DeltaBlue]=60000
  [Richards]=100
  [Json]=100
  [CD]=1000
  [Havlak]=15000
  [Bounce]=10000
  [List]=10000
)

{
  echo "=== Bubo run of all AWFY benchmarks ==="
  date
  echo

  for BENCH in DeltaBlue Richards Json CD Havlak Bounce List Mandelbrot NBody Permute Queens Sieve Storage Towers; do
    EXTRA=${EXTRA_ARGS[$BENCH]}
    echo "------------------------------------------------------------"
    echo "Benchmark: $BENCH  (extra_args=$EXTRA, warmup=$WARMUP)"
    echo "Command: Harness $BENCH $WARMUP $EXTRA"
    echo "Timestamp: $(date)"
    echo

    "${JAVA_BIN}" \
      --enable-native-access=ALL-UNNAMED \
      -Djdk.graal.TrackNodeSourcePosition=true \
      -Djdk.graal.BuboLIRPhase=false \
      -Djdk.graal.GTAssignDebug=false \
      -Djdk.graal.BuboLIRPhase=true \
      -Djdk.graal.HumphreysDebugData=false \
      -Djdk.graal.IsolatedLoopHeaderAlignment=0 \
      -Djdk.graal.LoopHeaderAlignment=0 \
      -Djdk.graal.DisableCodeEntryAlignment=true \
      -Djdk.graal.StrictProfiles=false \
      -Djdk.graal.LIRGTSlowDown=false \
      -Djdk.graal.IsolatedLoopHeaderAlignment=0 \
      -Djdk.graal.LoopHeaderAlignment=0 \
      -Djdk.graal.DisableCodeEntryAlignment=true \
      -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/Permute/Final_Permute.json \
      -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
      -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose \
      -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
      -cp "${CP}" \
      -javaagent:"${AGENT}" \
      Harness "${BENCH}" "${WARMUP}" "${EXTRA}"

    echo
    echo "Finished $BENCH at $(date)"
    echo
  done

  echo "=== All benchmarks completed at $(date) ==="
} >"${LOG_FILE}" 2>&1
