#! /bin/bash
# -Xlog:jit+compilation
#/home/hburchell/Repos/graal-dev/labs-openjdk/build/linux-x86_64-server-release/images/jdk/bin
# /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33
# /home/hburchell/JDKS/labsjdk-ce-22-jvmci-b02-debug/
# /home/hburchell/JDKS/graalvm-jdk-22+36.1
mx --java-home \
 /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 \
 vm -Dgraal.EnableProfiler=true -Dgraal.MinGraphSize=80 -Dgraal.BuboDump="BbuboTestout.txt" \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -Dgraal.TrackNodeSourcePosition=true -XX:+UseJVMCICompiler \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  Harness DeltaBlue 30 60000  