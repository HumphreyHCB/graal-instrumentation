
#! /bin/bash 
# -Xlog:jit+compilation
#/home/hburchell/Repos/graal-dev/labs-openjdk/build/linux-x86_64-server-release/images/jdk/bin
# /home/hburchell/Downloads/labsjdk-ce-11.0.20+8-jvmci-22.3-b22-debug-linux-amd64/labsjdk-ce-11.0.20-jvmci-22.3-b22-debug
# /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33

# mx --java-home /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 igv
# JVMCI_VERSION_CHECK=ignore JDK_VERSION_CHECK=ignore JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.22.0.7-2.el9.x86_64 mx c1visualizer
mx --java-home \
 /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 \
 vm  \
  -Dgraal.EnableGTSlowDown=true \
 -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+EnableJVMCI \
  -Dgraal.CompilationFailureAction=Diagnose \
  -Dgraal.Dump=:3 -Dgraal.PrintGraph=Network -Dgraal.PrintBackendCFG=true \
  -XX:+UseJVMCICompiler  \
  --add-exports \
  jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  Harness Towers 300 2500  

#  HelloWorld 
  
#  Harness DeltaBlue 1200 6000 
# labsjdk-ce-21.0.2-jvmci-23.1-b33
#  -XX:CompileOnly= -Dgraal.Dump=:2 -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true \
# -Dgraal.Dump -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true 
# -XX:-UseOnStackReplacement
# -Dgraal.CompilerConfiguration=economy
# mx --java-home /home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17  vm  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI   -Dgraal.Dump="*" -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true   -Dgraal.CompilationFailureAction="Print"     -XX:+UseJVMCICompiler  --add-exports   jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED   -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:/home/hburchell/Repos/AWFY-Profilers/AWFY/benchmarks/Java/benchmarks.jar   -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar Harness DeltaBlue 2000 100
# -Dgraal.SnippetCounters=true -Dgraal.Counters=
# mx --java-home \
# /home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17 \
#  vm -Xmx10g  \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dgraal.EnableProfiler=false \
#  -Dgraal.Dump -Dgraal.DumpOnError=true -Dgraal.DumpingErrorsAreFatal=true \
#   -Dgraal.CompilationFailureAction="Print"  \
#   -XX:+UseJVMCICompiler  \
#   -XX:CompileOnly=HelloWorld  --add-exports \
#   jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
#   -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler \
#   -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
#   HelloWorld 
