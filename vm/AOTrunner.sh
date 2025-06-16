#! /bin/bash

 ./latest_graalvm_home/bin/java -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary \
  -Dgraal.EnableProfiler=true -Dgraal.MinGraphSize=80 -Dgraal.CountCompiledMethods=false -Dgraal.BuboDebugMode=false \
  -Djdk.graal.PrintCompilation=true -Xlog:jit+compilation \
  --add-exports jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  Harness DeltaBlue 30 60000  