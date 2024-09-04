#!/bin/bash
export PATH=/home/hburchell/Repos/graal-dev/mx:$PATH
exec mx --java-home /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -XX:+UseJVMCICompiler -Dgraal.MinGraphSize=80 -Dgraal.TrackNodeSourcePosition=true -XX:-TieredCompilation  \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar  \
  "$@"