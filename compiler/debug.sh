#! /bin/bash


mx -d --java-home \
/home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17 \
 vm \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dgraal.Dump -XX:+UseJVMCICompiler -Dgraal.SnippetCounters=true \
  -Dgraal.Counters= -XX:CompileOnly=HelloWorld -XX:-TieredCompilation --add-exports \
  jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler \
  -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  HelloWorld 

#   mx --java-home \
# /home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17 \
#  vm \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dgraal.Dump -XX:+UseJVMCICompiler -Dgraal.SnippetCounters=true \
#   -Dgraal.Counters= -XX:CompileOnly=HelloWorld -XX:-TieredCompilation \
#   HelloWorld 