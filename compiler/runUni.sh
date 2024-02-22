#! /bin/bash
# -XX:-UseOnStackReplacement
#-Dgraal.CompilerConfiguration=economy

  mx --java-home \
/home/hb478/Downloads/labsjdk-ce-17.0.9+6-jvmci-23.0-b19-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b19 \
 vm  \
 -Dgraal.EnableProfiler=true \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -Dgraal.CompilationFailureAction=Diagnose \
  -XX:+UseJVMCICompiler  \
  -XX:CompileOnly= --add-exports \
  jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hb478/repos/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hb478/repos/graal-instrumentation/compiler:benchmarks.jar \
  -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  Harness Permute 300 600  
  

# mx --java-home \
# /home/hb478/Downloads/labsjdk-ce-17.0.9+6-jvmci-23.0-b19-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b19 \
#  vm -XX:-UseOnStackReplacement \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI  -Dgraal.EnableProfiler=false  \
#   -Dgraal.CompilationFailureAction="Print"  \
#   -XX:+UseJVMCICompiler  \
#   --add-exports \
#   jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
#   -cp /home/hb478/repos/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hb478/repos/graal-instrumentation/compiler:benchmarks.jar \
#   -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
#   Harness DeltaBlue 500 6000
