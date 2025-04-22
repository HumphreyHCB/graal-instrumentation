#!/bin/bash
# runDumpDebugStatsRenaissance.sh
#
# This script runs each Renaissance benchmark one by one using the
# renaissance-gpl-0.16.0.jar.
# It passes extra options (-r for repetitions) to extend the run duration.
# Benchmarks are specified as command-line arguments.
#
# Adjust the paths and options as needed.

# Path to your GraalVM installation.
GRAALVM_HOME="./latest_graalvm_home"

# Path to the Renaissance GPL jar.
REN_JAR="/home/hb478/repos/renaissance/renaissance-gpl-0.16.0.jar"

# Common JVM & Graal options.
COMMON_OPTS="-Djdk.graal.GTDebugInfoLog=true \
-Djdk.graal.CompilationFailureAction=Diagnose \
-Djdk.graal.WarnAboutGraphSignatureMismatch=false \
-Djdk.graal.WarnAboutCodeSignatureMismatch=false \
-Djdk.graal.WarnAboutNotCachedLoadedAccess=false \
-Djdk.graal.StrictProfiles=false \
-XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
-Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.TrackNodeInsertion=true"

# Extra options to extend the benchmark execution.
# Here, -r 100 means to repeat each measured operation 100 times.
EXTRA_OPTS="-r 100"

# List of Renaissance benchmark identifiers (names).
benchmarks=("akka-uct" "akka-streams" "als" "chi" "db" "datamunging" "dotty" "neo4j" "rxjava" "santa" "streams" "tpcx" "v8" "xbase")

# Loop over each benchmark.
for bm in "${benchmarks[@]}"; do
    echo "------------------------------------------------"
    echo "Running Renaissance benchmark: ${bm}"
    
    # Create a log file name that includes the benchmark name.
    LOGFILE="DebugInfo_${bm}.log"
    
    # Construct the Java command.
    # Note: We simply list the benchmark name after the jar; Renaissance uses these as benchmark-specifications.
    CMD="${GRAALVM_HOME}/bin/java ${COMMON_OPTS} -Djdk.graal.LogFile=DebugInfoDump/${LOGFILE} -jar ${REN_JAR} ${bm} ${EXTRA_OPTS}"
    
    echo "Executing: ${CMD}"
    ${CMD}
    
    echo "Finished benchmark: ${bm}"
    echo "------------------------------------------------"
done
