#! /bin/bash 

mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false  -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false -Djdk.graal.CompilationFailureAction=Diagnose \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=ResolveMethodtest14SetsetNext.txt \
  Harness List 50 10000

 # -ea
  #  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \