#! /bin/bash 

 ./latest_graalvm_home/bin/java \
  -Djdk.graal.TrackNodeSourcePosition=true -Djdk.graal.EnableGTSlowDown=false \
  -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.DumpBlockDebugInfo=true \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:+UseJVMCICompiler -XX:+UseJVMCINativeLibrary -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=AysncSlowdowTest2.txt \
  Harness List 500 10000


#   -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/Bounce/Bounce_CompilerReplay