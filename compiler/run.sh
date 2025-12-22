#! /bin/bash
# -Xlog:jit+compilation
#/home/hburchell/Repos/graal-dev/labs-openjdk/build/linux-x86_64-server-release/images/jdk/bin
# /home/hburchell/Downloads/labsjdk-ce-26-jvmci-b01
# /home/hburchell/JDKS/labsjdk-ce-22-jvmci-b02-debug/
# /home/hburchell/JDKS/graalvm-jdk-22+36.1
mx --java-home \
 /home/hburchell/Downloads/labsjdk-ce-26-jvmci-b01 \
 vm -jdk.graal.EnableProfiler=true -jdk.graal.MinGraphSize=80 -jdk.graal.CountCompiledMethods=false -jdk.graal.BuboDebugMode=false \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -jdk.graal.TrackNodeSourcePosition=true -XX:+UseJVMCICompiler \
  --add-exports \
  jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
  Harness DeltaBlue 300 60000  

#  HelloWorld 
#   -jdk.graal.EnableProfiler=false -jdk.graal.MinGraphSize=80 -jdk.graal.CountCompiledMethods=true \
#  Harness DeltaBlue 1200 6000 
# labsjdk-ce-26-jvmci-b01
#  -XX:CompileOnly= -jdk.graal.Dump=:2 -jdk.graal.DumpOnError=true -jdk.graal.DumpingErrorsAreFatal=true \
# -jdk.graal.Dump -jdk.graal.DumpOnError=true -jdk.graal.DumpingErrorsAreFatal=true 
# -XX:-UseOnStackReplacement
# -jdk.graal.CompilerConfiguration=economy
# mx --java-home /home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17  vm  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI   -jdk.graal.Dump="*" -jdk.graal.DumpOnError=true -jdk.graal.DumpingErrorsAreFatal=true   -jdk.graal.CompilationFailureAction="Print"     -XX:+UseJVMCICompiler  --add-exports   jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED   -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:/home/hburchell/Repos/AWFY-Profilers/AWFY/benchmarks/Java/benchmarks.jar   -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar Harness DeltaBlue 2000 100
# -jdk.graal.SnippetCounters=true -jdk.graal.Counters=
# mx --java-home \
# /home/hburchell/Downloads/labsjdk-ce-17.0.9+4-jvmci-23.0-b17-linux-amd64/labsjdk-ce-17.0.9-jvmci-23.0-b17 \
#  vm -Xmx10g  \
#  -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -jdk.graal.EnableProfiler=false \
#  -jdk.graal.Dump -jdk.graal.DumpOnError=true -jdk.graal.DumpingErrorsAreFatal=true \
#   -jdk.graal.CompilationFailureAction="Print"  \
#   -XX:+UseJVMCICompiler  \
#   -XX:CompileOnly=HelloWorld  --add-exports \
#   jdk.internal.vm.compiler/org.graalvm.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
#   -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler \
#   -javaagent:/home/hburchell/Repos/graal-dev/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar \
#   HelloWorld 
