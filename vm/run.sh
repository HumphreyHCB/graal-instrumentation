
AGENT=/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar
MODULE=jdk.graal.compiler          # or jdk.internal.vm.compiler on some JDKs
BUBO_JAR=/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/bubo-runtime.jar


export JVMCI_VERSION_CHECK=ignore

./latest_graalvm_home/bin/java \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.EnableProfiler=true \
  -Djdk.graal.MinGraphSize=0 \
  -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
  -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose \
  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  Harness Towers 5 2500


  # --add-exports=${MODULE}/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  # --add-exports=${MODULE}/jdk.graal.compiler.phases.common=ALL-UNNAMED \
  # -javaagent:"$AGENT" \