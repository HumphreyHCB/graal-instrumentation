#! /bin/bash

 ./latest_graalvm_home/bin/java -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary \
   -Dgraal.EnableProfiler=false -Dgraal.MinGraphSize=80 -Dgraal.CountCompiledMethods=false -Dgraal.BuboDebugMode=false \
  -Djdk.graal.LogFile=JProfilerAOT.txt -Djdk.graal.PrintCompilation=false  \
  --add-exports jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  -agentpath:/home/hburchell/jprofiler14/bin/linux-x64/libjprofilerti.so=port=8849,nowait,config=/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/jprofiler_config.xml \
  Harness DeltaBlue 10 60000   