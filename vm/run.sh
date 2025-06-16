#! /bin/bash 

 ./latest_graalvm_home/bin/java \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/Data/2025_05_19_12_58_29_SlowDown_Data/Test.json \
 -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose  \
  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  Harness Towers 500 2500

# /home/hb478/repos/GTSlowdownSchedular/Data/2025_05_15_18_09_38_SlowDown_Data/CD_Test.json
# ./latest_graalvm_home/bin/java \
#  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.EnableGTSlowDown=false -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=true -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/Data/2025_05_15_18_09_38_SlowDown_Data/CD_Test.json -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar Harness CD 500 1000