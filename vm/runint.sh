#!/bin/bash
export PATH=/home/hburchell/Repos/graal-dev/mx:$PATH
exec ./latest_graalvm_home/bin/java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation  \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  "$@"