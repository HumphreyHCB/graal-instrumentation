#! /bin/bash 


 ./latest_graalvm_home/bin/java \
  -Djdk.graal.GTDebugInfoLog=true -Djdk.graal.LogFile=DebugInfo.log \
  -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation \
  -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.TrackNodeInsertion=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java:benchmarks.jar \
  -Djdk.graal.CompilationFailureAction=Diagnose \
  -Djdk.graal.WarnAboutGraphSignatureMismatch=false \
    -Djdk.graal.WarnAboutCodeSignatureMismatch=false \
    -Djdk.graal.WarnAboutNotCachedLoadedAccess=false \
   -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 500 10000


# -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \