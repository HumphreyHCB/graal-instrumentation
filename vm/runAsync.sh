#!/usr/bin/env bash
set -euo pipefail

fileName="./LoopBenchmarks_noSlow_GTAssignDebug.txt"

./latest_graalvm_home/bin/java \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.GTAssignDebug=true \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+EnableJVMCI \
  -XX:+UseJVMCICompiler \
  -XX:+UseJVMCINativeLibrary \
  -XX:+DebugNonSafepoints \
  -XX:-TieredCompilation \
  -XX:-BackgroundCompilation \
  -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -Djdk.graal.StrictProfiles=false \
  -Djdk.graal.LIRGTSlowDown=false \
  -Djdk.graal.BuboLIRPhase=false \
  -agentpath:/home/hb478/repos/are-we-fast-yet/Async/async-profiler-4.2.1-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10,file=$fileName \
  Harness LoopBenchmarks 12000

echo $fileName
