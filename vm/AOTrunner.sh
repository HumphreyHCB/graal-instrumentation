#! /bin/bash

 ./latest_graalvm_home/bin/java -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary \
   -Dgraal.EnableProfiler=false -Dgraal.MinGraphSize=80 -Dgraal.CountCompiledMethods=true -Dgraal.BuboDebugMode=false \
   -Dgraal.HotSpotPrintInlining=true -Djdk.graal.LogFile=NormalQuickAOT.txt \
  --add-exports jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  Harness DeltaBlue 10 60000  