#!/usr/bin/env bash
set -euo pipefail

/home/hb478/repos/graal-instrumentation/vm/latest_graalvm_home/bin/java \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+EnableJVMCI \
  -XX:+UseJVMCICompiler \
  -XX:+UseJVMCINativeLibrary \
  -XX:+DebugNonSafepoints \
  -Djdk.graal.StrictProfiles=false \
  -Djdk.graal.WarnAboutCodeSignatureMismatch=false \
  -Djdk.graal.TrackNodeSourcePosition=true \
  --enable-native-access=ALL-UNNAMED \
  -XX:-TieredCompilation \
  -XX:-BackgroundCompilation \
  -Djdk.graal.LoopHeaderAlignment=0 \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 \
  -Djdk.graal.GTAssignDebug=true \
  -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithoutProbe/AWFY/Bounce/Bounce_CompilerReplay \
  -Djdk.graal.LIRGTSlowDown=false \
  -agentpath:/home/hb478/repos/are-we-fast-yet/Async/async-profiler-4.2.1-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10us,file=PostLinz_Bounce_async_no_slowdown_WithDebug.txt \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  Harness Bounce 500 10000 \

/home/hb478/repos/graal-instrumentation/vm/latest_graalvm_home/bin/java \
  -XX:+UnlockExperimentalVMOptions \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+EnableJVMCI \
  -XX:+UseJVMCICompiler \
  -XX:+UseJVMCINativeLibrary \
  -XX:+DebugNonSafepoints \
  -Djdk.graal.StrictProfiles=false \
  -Djdk.graal.WarnAboutCodeSignatureMismatch=false \
  -Djdk.graal.TrackNodeSourcePosition=true \
  --enable-native-access=ALL-UNNAMED \
  -XX:-TieredCompilation \
  -XX:-BackgroundCompilation \
  -Djdk.graal.LoopHeaderAlignment=0 \
  -Djdk.graal.IsolatedLoopHeaderAlignment=0 \
  -Djdk.graal.GTAssignDebug=true \
  -Djdk.graal.LoadProfiles=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithoutProbe/AWFY/Bounce/Bounce_CompilerReplay \
  -Djdk.graal.LIRBlockSlowdownFileName=/home/hb478/repos/GTSlowdownSchedular/FinalBuboTests/WithoutProbe/AWFY/Bounce/Final_Bounce.json \
  -Djdk.graal.LIRGTSlowDown=true \
  -agentpath:/home/hb478/repos/are-we-fast-yet/Async/async-profiler-4.2.1-linux-x64/lib/libasyncProfiler.so=start,event=cpu,interval=10us,file=PostLinz_Bounce_async_slowdown_WithDebug.txt \
  -cp /home/hb478/repos/are-we-fast-yet/benchmarks/Java/benchmarks.jar \
  Harness Bounce 500 10000 \