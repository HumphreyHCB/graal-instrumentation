#! /bin/bash

 ./latest_graalvm_home/bin/java -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary \
   -XX:-TieredCompilation  -Djdk.graal.LIRGTSlowDown=true  \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/benchmarks.jar \
  -agentpath:/opt/AMDuProf_4.2-850//bin/ProfileAgents/x64/libAMDJvmtiAgent.so \
  Harness Mandelbrot 1000 750  
