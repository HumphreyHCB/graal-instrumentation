#!/usr/bin/env bash

benchmark="List"
file="${benchmark}Async2.txt"

# build a separate var for the profile-replay subdirectory
replay_subdir="${benchmark}_CompilerReplay"

# full LoadProfiles path
load_profiles="/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/${benchmark}/${replay_subdir}"

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
  Harness "$benchmark" 500 5000

echo "Wrote to here $file"
