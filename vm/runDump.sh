
#! /bin/bash 
# -Xlog:jit+compilation
#/home/hburchell/Repos/graal-dev/labs-openjdk/build/linux-x86_64-server-release/images/jdk/bin
# /home/hburchell/Downloads/labsjdk-ce-11.0.20+8-jvmci-22.3-b22-debug-linux-amd64/labsjdk-ce-11.0.20-jvmci-22.3-b22-debug
# /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33

# mx --java-home /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 igv
# JVMCI_VERSION_CHECK=ignore JDK_VERSION_CHECK=ignore JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.25.0.9-2.el9.x86_64 mx c1visualizer
 ./latest_graalvm_home/bin/java \
  -Djdk.graal.EnableGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=false -Djdk.graal.LIRGTSlowDown=false \
 -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
  -Djdk.graal.CompilationFailureAction=Diagnose -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.LogFile=crashOut.txt \
  -Djdk.graal.Dump=:5 -Djdk.graal.PrintGraph=Network -Djdk.graal.PrintBackendCFG=true -Djdk.graal.ObjdumpExecutables=objdump -Djdk.graal.ObjdumpExecutables=gobjdump \
   -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar:/home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 50 10000 

#  HelloWorld 
# /home/hb478/repos/GTSlowdownSchedular/Data/2025_01_15_11_15_17_CompilerReplay  
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
