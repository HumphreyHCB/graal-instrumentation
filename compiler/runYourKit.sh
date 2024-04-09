#! /bin/bash
# -Xlog:jit+compilation
#/home/hburchell/Repos/graal-dev/labs-openjdk/build/linux-x86_64-server-release/images/jdk/bin
# /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33
mx --java-home \
 /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 \
 vm  \
 -Dgraal.EnableProfiler=false -Dgraal.MinGraphSize=80 -Dgraal.CountCompiledMethods=true \
 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI \
  -Dgraal.CompilationFailureAction=Diagnose  \
  -XX:+UseJVMCICompiler \
  --add-exports \
  jdk.graal.compiler/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  -agentpath:/home/hburchell/YourKit-JavaProfiler-2024.3/bin/linux-x86-64/libyjpagent.so=@profiler-options.txt \
  Harness DeltaBlue 10 60000  
 
 #options_file=profiler-options.txt
  # -agentpath:/home/hburchell/Downloads/visualvm_218/visualvm/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so=/home/hburchell/Downloads/visualvm_218/visualvm/lib,5135 \


# java \
#   -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
#   -agentpath:/home/hburchell/Downloads/visualvm_218/visualvm/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so=/home/hburchell/Downloads/visualvm_218/visualvm/lib,5138 \
#   Harness DeltaBlue 300 60000   

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
