#! /bin/bash 

mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false \
  -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=5000ListNoSlowdownSlowdownNoMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 5000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=5000ListSlowdownNoMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 5000 10000


  mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=true \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=5000ListSlowdownMixMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 5000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false  \
  -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=10000ListNoSlowdownSlowdownNoMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 10000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=10000ListSlowdownNoMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 10000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=true \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=10000ListSlowdownMixMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 10000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false \
  -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=20000ListNoSlowdownSlowdownNoMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 20000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=false \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=20000ListSlowdownNoMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 20000 10000


mx --java-home \
  /home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33 vm \
  -Djdk.graal.DumpBlockDebugInfo=false -Djdk.graal.TrackNodeSourcePosition=false -Djdk.graal.EnableGTSlowDown=true \
  -Djdk.graal.LIRGTSlowDown=true -Djdk.graal.GTMarkBasicBlocks=false \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/Final_List.json \
  -Djdk.graal.MixGTSlowdown=true \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 -Djdk.graal.LoopHeaderAlignment=0 -XX:-TieredCompilation -XX:-BackgroundCompilation -Djdk.graal.DisableCodeEntryAlignment=true \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  -agentpath:/home/hburchell/ProgramFiles/async-profiler-3.0-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10ms,file=20000ListSlowdownMixMix.txt \
  -Djdk.graal.StrictProfiles=false -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalDataRefined100/List/List_CompilerReplay \
  Harness List 20000 10000
