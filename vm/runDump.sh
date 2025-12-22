
AGENT=/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar
MODULE=jdk.graal.compiler          # or jdk.internal.vm.compiler on some JDKs
BUBO_JAR=/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/bubo-runtime.jar


./latest_graalvm_home/bin/java \
  --enable-native-access=ALL-UNNAMED \
  -Dgraal.Dump=:2 -Dgraal.PrintGraph=Network \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.EnableProfiler=false \
  -Djdk.graal.BuboLIRPhase=true \
  -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
  -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose \
    --add-exports=java.base/jdk.internal.misc=ALL-UNNAMED \
    --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED \
    --add-exports=jdk.graal.compiler/jdk.graal.compiler.serviceprovider=ALL-UNNAMED \
  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  Harness Mandelbrot 20 750
  

#   Harness Sieve 20 10000
#   Harness NBody 20 1200000
#   -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \

  # --add-exports=${MODULE}/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  # --add-exports=${MODULE}/jdk.graal.compiler.phases.common=ALL-UNNAMED \
  # -javaagent:"$AGENT" \
  #    -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/bubo-agent-1.0.jar \