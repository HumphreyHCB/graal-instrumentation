#!/usr/bin/env bash
set -euo pipefail

fileName="./LinzAsyncSlowdown2LongerSlowdown.txt"

/home/hb478/repos/graal-instrumentation/vm/latest_graalvm_home/bin/java \
  -agentpath:/home/hb478/repos/are-we-fast-yet/Async/async-profiler-4.2.1-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10us,file=Linz_Bounce_asyncTest5_nofastdebug.txt \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  Harness Bounce 50 750 \
# echo $fileName


# ./latest_graalvm_home/bin/java \
#   -XX:+UnlockExperimentalVMOptions \
#   -XX:+UnlockDiagnosticVMOptions \
#   -Djdk.graal.DisableCodeEntryAlignment=true \
#   -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
#   Harness LoopBenchmarks 12000

# echo $fileName
