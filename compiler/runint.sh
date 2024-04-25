#!/bin/bash
export PATH=/home/hburchell/Repos/graal-dev/mx:$PATH
exec mx --java-home /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -XX:+UseJVMCICompiler -Dgraal.EnableProfiler=true -Dgraal.MinGraphSize=80 -Dgraal.TrackNodeSourcePosition=true \
  --add-exports jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar"$@"