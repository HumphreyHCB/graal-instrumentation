#! /bin/bash


mx --java-home \
/home/hb478/Downloads/labsjdk-ce-17.0.9+6-jvmci-23.0-b19-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b19 \
 vm \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI  \
 -Dgraal.Dump="*" -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true \
  -Dgraal.CompilationFailureAction="Print"   \
  -XX:+UseJVMCICompiler  \
  -XX:CompileOnly=HelloWorld -XX:-TieredCompilation --add-exports \
  jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hb478/repos/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hb478/repos/graal-instrumentation/compiler \
  -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  HelloWorld 

#   mx --java-home \
# /home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17 \
#  vm \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dgraal.Dump -XX:+UseJVMCICompiler -Dgraal.SnippetCounters=true \
#   -Dgraal.Counters= -XX:CompileOnly=HelloWorld -XX:-TieredCompilation \
#   HelloWorld 