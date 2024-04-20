#! /bin/bash

 ./latest_graalvm_home/bin/java -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary \
   -Dgraal.EnableProfiler=false -Dgraal.MinGraphSize=80 -Dgraal.CountCompiledMethods=false -Dgraal.BuboDebugMode=false \
  -Djdk.graal.LogFile=YourKitAOT.txt -Djdk.graal.PrintCompilation=true  \
  --add-exports jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  -agentpath:/home/hburchell/YourKit-JavaProfiler-2024.3/bin/linux-x86-64/libyjpagent.so=@profiler-options.txt \
  Harness DeltaBlue 10 60000  