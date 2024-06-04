#! /bin/bash

 ./latest_graalvm_home/bin/java -Dgraal.EnableProfiler=true -Dgraal.MinGraphSize=80 -Dgraal.BuboDebugMode=false \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -Dgraal.TrackNodeSourcePosition=true -XX:+UseJVMCICompiler -XX:-UseJVMCINativeLibrary \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  Harness DeltaBlue 30 60000  