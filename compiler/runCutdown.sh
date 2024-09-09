#! /bin/bash
# -Xlog:jit+compilation
#/home/hburchell/Repos/graal-dev/labs-openjdk/build/linux-x86_64-server-release/images/jdk/bin
# /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33
# /home/hburchell/JDKS/labsjdk-ce-22-jvmci-b02-debug/
# /home/hburchell/JDKS/graalvm-jdk-22+36.1
 mx --java-home \
 /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 \
 vm -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.CollectLIRCostInformation=true -Djdk.graal.LIRGTSlowDownDebugMode=true -Djdk.graal.LIRCostInformationFile=x.json \
 -XX:-TieredCompilation  -XX:-BackgroundCompilation \
 -Djdk.graal.LIRNubers="" \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
 -XX:+UseJVMCICompiler  \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  Harness Havlak 1000 15000  

# -agentpath:/home/hburchell/Repos/AWFY-Profilers/Profilers/Async/build/libasyncProfiler.so=start,event=cpu,interval=10ms,file=AsyncTowersSlowdown.txt \
#  -XX:CompileThreshold=1 -XX:UseAVX=0 -XX:MaxVectorSize=4
# good numbers 8,9,10,12