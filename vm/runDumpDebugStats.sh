#!/bin/bash
# run_all_awfy_benchmarks.sh
#
# This script runs all 14 AWFY benchmarks with the required extra arguments.
# It updates the log file name and load profile path based on the benchmark name.

# Set the base paths.
GRAALVM_HOME="./latest_graalvm_home"
CLASSPATH="/home/hb478/repos/are-we-fast-yet/benchmarks/Java:benchmarks.jar"
LOAD_PROFILES_BASE="/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100"

# Common JVM & Graal options.
COMMON_OPTS="-Djdk.graal.GTDebugInfoLog=true \
-Djdk.graal.CompilationFailureAction=Diagnose \
-Djdk.graal.WarnAboutGraphSignatureMismatch=false \
-Djdk.graal.WarnAboutCodeSignatureMismatch=false \
-Djdk.graal.WarnAboutNotCachedLoadedAccess=false \
-Djdk.graal.StrictProfiles=false \
-XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
-Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.TrackNodeInsertion=true"

# Number of warmup iterations.
WARMUP=500

# Define benchmarks and their extra argument values.
declare -A EXTRA_ARGS
EXTRA_ARGS["DeltaBlue"]=60000
EXTRA_ARGS["Richards"]=100
EXTRA_ARGS["Json"]=100
EXTRA_ARGS["CD"]=1000
EXTRA_ARGS["Havlak"]=15000
EXTRA_ARGS["Bounce"]=10000
EXTRA_ARGS["List"]=10000
EXTRA_ARGS["Mandelbrot"]=750
EXTRA_ARGS["NBody"]=1200000
EXTRA_ARGS["Permute"]=3500
EXTRA_ARGS["Queens"]=5000
EXTRA_ARGS["Sieve"]=10000
EXTRA_ARGS["Storage"]=1800
EXTRA_ARGS["Towers"]=2500

# List of benchmark names.
benchmarks=("DeltaBlue" "Richards" "Json" "CD" "Havlak" "Bounce" "List" "Mandelbrot" "NBody" "Permute" "Queens" "Sieve" "Storage" "Towers")

# Loop over each benchmark.
for bm in "${benchmarks[@]}"; do
    echo "------------------------------------------------"
    echo "Running benchmark: ${bm}"
    
    # Update the log file name so it includes the benchmark name.
    LOGFILE="DebugInfo_${bm}.log"
    
    # Build the load profile option using the benchmark name.
    LOAD_PROFILE="${LOAD_PROFILES_BASE}/${bm}/${bm}_CompilerReplay"
    
    # Get the extra argument value.
    EXTRA_ARG=${EXTRA_ARGS[$bm]}
    
    # Construct the full Java command.
    CMD="${GRAALVM_HOME}/bin/java ${COMMON_OPTS} \
-Djdk.graal.LogFile=DebugInfoDump/${LOGFILE} \
-Djdk.graal.LoadProfiles=${LOAD_PROFILE} \
-cp ${CLASSPATH} Harness ${bm} ${WARMUP} ${EXTRA_ARG}"
    
    echo "Executing: ${CMD}"
    ${CMD}
    echo "Finished benchmark: ${bm}"
    echo "------------------------------------------------"
done
