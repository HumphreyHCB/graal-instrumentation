#! /bin/bash

# Define the GraalVM home path and common JVM options
GRAALVM_HOME="./latest_graalvm_home/bin/java"
COMMON_OPTS="-Djdk.graal.DumpBlockDebugInfo=true -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.EnableGTSlowDown=false -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=false -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -Djdk.graal.DisableCodeEntryAlignment=true -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar -Djdk.graal.StrictProfiles=false"

# Define benchmarks and their extra arguments
declare -A benchmarks=(
    [Richards]=100
    [Json]=100
    [CD]=1000
    [Bounce]=10000
    [List]=10000
    [Mandelbrot]=750
    [NBody]=1200000
    [Permute]=3500
    [Queens]=5000
    [Sieve]=10000
    [Towers]=2500
    [DeltaBlue]=60000
    [Storage]=1800
    [Havlak]=15000
)


# Base path for profile loading
PROFILE_BASE="/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100"

# Execute each benchmark
for benchmark in "${!benchmarks[@]}"; do
    echo "Running benchmark: $benchmark"
    EXTRA_ARG=${benchmarks[$benchmark]}
    PROFILE_PATH="$PROFILE_BASE/$benchmark/${benchmark}_CompilerReplay"
    LOG_FILE="/home/hb478/repos/GTResearchExperiments/GTExperimentBBInfo/Raw/${benchmark}_BBSourceInfo2.txt"
    
    $GRAALVM_HOME $COMMON_OPTS -Djdk.graal.LoadProfiles=$PROFILE_PATH Harness $benchmark 200 $EXTRA_ARG > "$LOG_FILE" 2>&1
    echo "Finished benchmark: $benchmark"
    echo "--------------------------------------"
done