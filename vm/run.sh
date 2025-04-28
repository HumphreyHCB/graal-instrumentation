#! /bin/bash 


 ./latest_graalvm_home/bin/java \
 -Djdk.graal.AdditionalCompilerDebugInformation=true -Djdk.graal.LIRAdditionalCompilerDebugInformation=true -XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+EnableJVMCI -Djdk.graal.CompilationFailureAction=Diagnose  -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.TrackNodeInsertion=true  \
  -XX:+UseJVMCINativeLibrary -XX:+UseJVMCICompiler -XX:-TieredCompilation -XX:-BackgroundCompilation -XX:JVMCILibPath=/home/hb478/repos/graal-instrumentation/sdk/mxbuild/linux-amd64/libjvmcicompiler.so.image  \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
   -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
     -Djdk.graal.WarnAboutGraphSignatureMismatch=false \
    -Djdk.graal.WarnAboutCodeSignatureMismatch=false \
    -Djdk.graal.WarnAboutNotCachedLoadedAccess=false \
  Harness List 100 10000


# -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \