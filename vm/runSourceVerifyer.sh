
AGENT=/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/JavaAgent-1.0-SNAPSHOT-jar-with-dependencies.jar
MODULE=jdk.graal.compiler          # or jdk.internal.vm.compiler on some JDKs
BUBO_JAR=/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/bubo-runtime.jar


./latest_graalvm_home/bin/java \
  --enable-native-access=ALL-UNNAMED \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.EnableProfiler=false \
  -Djdk.graal.BuboLIRPhase=false \
  -Djdk.graal.GTAssignDebug=true \
  -Djdk.graal.HumphreysDebugData=false \
  -Djdk.graal.StrictProfiles=false \
  -Djdk.graal.LIRGTSlowDown=false \
  -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/Bounce/Bounce_CompilerReplay \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/Bounce/Final_Bounce.json\
  -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions \
  -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose \
  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -javaagent:"$AGENT" \
  Harness Mandelbrot 20 750
  

#   Harness Sieve 20 10000
#   Harness NBody 20 1200000
#   -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \

  # --add-exports=${MODULE}/jdk.graal.compiler.hotspot.meta.Bubo=ALL-UNNAMED \
  # --add-exports=${MODULE}/jdk.graal.compiler.phases.common=ALL-UNNAMED \
  # -javaagent:"$AGENT" -javaagent:/home/hb478/repos/graal-instrumentation/Bubo-Agent/target/bubo-agent-1.0.jar \




#   {
#     "AWFY Benchmarks": {
#         "DeltaBlue": {
#             "extra_args": 60000,
#             "SingleFile": false
#         },
#         "Richards": {
#             "extra_args": 100,
#             "SingleFile": false
#         },
#         "Json": {
#             "extra_args": 100,
#             "SingleFile": false
#         },
#         "CD": {
#             "extra_args": 1000,
#             "SingleFile": false
#         },
#         "Havlak": {
#             "extra_args": 15000,
#             "SingleFile": false
#         },
#         "Bounce": {
#             "extra_args": 10000,
#             "SingleFile": true
#         },
#         "List": {
#             "extra_args": 10000,
#             "SingleFile": true
#         },
#         "Mandelbrot": {
#             "extra_args": 750,
#             "SingleFile": true
#         },
#         "NBody": {
#             "extra_args": 1200000,
#             "SingleFile": false
#         },
#         "Permute": {
#             "extra_args": 3500,
#             "SingleFile": true
#         },
#         "Queens": {
#             "extra_args": 5000,
#             "SingleFile": true
#         },
#         "Sieve": {
#             "extra_args": 10000,
#             "SingleFile": true
#         },
#         "Storage": {
#             "extra_args": 1800,
#             "SingleFile": false
#         },
#         "Towers": {
#             "extra_args": 2500,
#             "SingleFile": true
#         }
#     }
# }
