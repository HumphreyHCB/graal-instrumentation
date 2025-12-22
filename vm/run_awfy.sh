#!/usr/bin/env bash
set -euo pipefail

#######################################
# 1. Benchmark-specific parameters
#######################################
declare -A EXTRA_ARGS=(
  [DeltaBlue]=60000
  [Richards]=100
  [Json]=100
  [CD]=1000
  [Havlak]=15000
  [Bounce]=10000
  [List]=10000
  [Mandelbrot]=750
  [NBody]=1200000
  [Permute]=3500
  [Queens]=5000
  [Sieve]=10000
  [Storage]=1800
  [Towers]=2500
)

BENCHMARKS=(
  DeltaBlue Richards Json CD Havlak Bounce List Mandelbrot
  NBody Permute Queens Sieve Storage Towers
)

#######################################
# 2. Run each benchmark
#######################################
for benchmark in "${BENCHMARKS[@]}"; do
  extra=${EXTRA_ARGS[$benchmark]}
  file="tests/DebugTests/MissingDataRuns/${benchmark}AsyncHIRandLIRExtra.txt"

  replay_subdir="${benchmark}_CompilerReplay"
  load_profiles="/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/${benchmark}/${replay_subdir}"

  echo "=== Running $benchmark (extra_args=$extra) ==="

  ./latest_graalvm_home/bin/java \
    -Djdk.graal.AdditionalCompilerDebugInformation=true \
    -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+EnableJVMCI \
    -Djdk.graal.CompilationFailureAction=Diagnose \
    -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.TrackNodeInsertion=true \
    -XX:+UseJVMCINativeLibrary -XX:+UseJVMCICompiler -XX:-TieredCompilation -XX:-BackgroundCompilation \
    -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
    -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=1ms,file="$file" \
    -Djdk.graal.StrictProfiles=false \
    -Djdk.graal.LoadProfiles="$load_profiles" \
    -Djdk.graal.WarnAboutGraphSignatureMismatch=false \
    -Djdk.graal.WarnAboutCodeSignatureMismatch=false \
    -Djdk.graal.WarnAboutNotCachedLoadedAccess=false \
    Harness "$benchmark" 500 "$extra"

  echo "  ↳ wrote $file"
done

#######################################
# 3. Extract the CompilerBackend percent
#######################################
output_csv="compiler_backend_percent.csv"
echo "Benchmark,CompilerBackendPercent" > "$output_csv"

for benchmark in "${BENCHMARKS[@]}"; do
  file="tests/DebugTests/MissingDataRuns/${benchmark}Async.txt"

  percent=$(
    awk '
      /jdk\.graal\.compiler\.debug\.Markers\.CompilerMarkers\.CompilerBackend/ {
        for (i = 1; i <= NF; i++) {
          if ($i ~ /%$/) {          # field ending in %
            gsub(/%/, "", $i);      # drop the %
            print $i;               # emit the number
            exit                    # done for this file
          }
        }
      }
    ' "$file"
  )

  [[ -z $percent ]] && percent="NA"
  echo "${benchmark},${percent}" >> "$output_csv"
done

echo
echo "Summary written to $output_csv"
