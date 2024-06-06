#! /bin/bash

 ./latest_graalvm_home/bin/java -Dgraal.EnableProfiler=true -Dgraal.MinGraphSize=80 -Dgraal.BuboDebugMode=false \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dgraal.CompilationFailureAction=Diagnose -Xlog:jit+compilation \
  -Dgraal.TrackNodeSourcePosition=true -XX:+UseJVMCICompiler -XX:JVMCICounterSize=10 -Dgraal.BenchmarkDynamicCounters="err, starting ====, PASSED in " \
  --add-exports \
  jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  Harness DeltaBlue 30 60000  

  # -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar
  # -XX:-UseJVMCINativeLibrary
  # -XX:-BackgroundCompilation