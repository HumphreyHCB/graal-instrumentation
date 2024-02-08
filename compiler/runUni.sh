#! /bin/bash


# mx --java-home \
# /home/hb478/Downloads/labsjdk-ce-17.0.9+6-jvmci-23.0-b19-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b19 \
#  vm -Xmx20g \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI  -Dgraal.EnableProfiler=true \
#  -Dgraal.Dump="*" -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true \
#   -Dgraal.CompilationFailureAction="Print"  -Dlibgraal.Xmx=16G  \
#   -XX:+UseJVMCICompiler  \
#   -XX:CompileOnly= -XX:-TieredCompilation --add-exports \
#   jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
#   -cp /home/hb478/repos/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hb478/repos/graal-instrumentation/compiler \
#   -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
#   HelloWorld 

mx --java-home \
/home/hb478/Downloads/labsjdk-ce-17.0.9+6-jvmci-23.0-b19-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b19 \
 vm -Xmx20g \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI  -Dgraal.EnableProfiler=true \
 -Dgraal.Dump="*" -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true \
  -Dgraal.CompilationFailureAction="Print"  -Dlibgraal.Xmx=16G  \
  -XX:+UseJVMCICompiler  \
  -XX:CompileOnly= -XX:-TieredCompilation --add-exports \
  jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hb478/repos/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hb478/repos/graal-instrumentation/compiler:benchmarks.jar \
  -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  Harness DeltaBlue 50 6000
