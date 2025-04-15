#! /bin/bash 

 ./latest_graalvm_home/bin/java \
  -Djdk.graal.DumpBlockDebugInfo=true -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.EnableGTSlowDown=false -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=true -Djdk.graal.LIRBlockSlowdownFileName=BlockSlowdown1.json \
 -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose  \
  -XX:+UseJVMCICompiler -XX:-UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -cp /home/hburchell/Repos/graal-dev/graal-instrumentation/compiler:benchmarks.jar \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined50/CD/CD_CompilerReplay \
  Harness CD 500 1000


