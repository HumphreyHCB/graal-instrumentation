#! /bin/bash

 ./latest_graalvm_home/bin/java -Dgraal.EnableProfiler=true -Dgraal.MinGraphSize=80 -Dgraal.BuboDebugMode=false \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dgraal.CompilationFailureAction=Diagnose \
  -Dgraal.TrackNodeSourcePosition=true -XX:+UseJVMCICompiler \
  --add-exports \
  jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  Harness DeltaBlue 300 60000  

  # -XX:-UseJVMCINativeLibrary