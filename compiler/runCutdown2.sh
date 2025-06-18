#! /bin/bash

AGENT=/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar
MODULE=jdk.graal.compiler          # or jdk.internal.vm.compiler on some JDKs

mx --java-home \
 /home/hburchell/Downloads/labsjdk-ce-26-jvmci-b01 \
  vm \
  --add-exports=${MODULE}/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  --add-exports=${MODULE}/jdk.graal.compiler.phases.common=ALL-UNNAMED \
  -javaagent:"$AGENT" \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.EnableProfiler=true \
  -Djdk.graal.MinGraphSize=0 \
  -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
   -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -cp benchmarks.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler \
  Harness Towers 1 2500