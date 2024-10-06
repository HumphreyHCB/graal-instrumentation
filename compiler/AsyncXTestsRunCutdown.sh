#! /bin/bash

# No Slowdown Iteration 1000
# mx  --java-home \
#  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 \
#  vm -Djdk.graal.LIRGTSlowDown=false -XX:-TieredCompilation  -XX:-BackgroundCompilation \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
#   -XX:+UseJVMCICompiler  \
#   -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
#   -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=1ms,file=GTDump/AsyncNBodyNoSlowdownNOBack.txt \
#   Harness NBody 1000 1200000  

file=JsonAsyncNoSlowdownNoOSR.txt

# Slowdown Iteration 1000
mx  --java-home \
 /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 \
 vm -Djdk.graal.EnableGTSlowDown=false -Djdk.graal.LIRGTSlowDown=false -XX:-TieredCompilation -XX:-BackgroundCompilation \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI  \
 -XX:+UseJVMCICompiler -Djdk.graal.TrackNodeSourcePosition=true '-XX:CompileCommand=dontinline,*::*' -Djdk.graal.TrivialInliningSize=0 \
 -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
 -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=1ms,file=$file \
 Harness Json 200 100 

echo "Wrote to here $file"