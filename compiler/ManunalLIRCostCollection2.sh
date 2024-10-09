#!/bin/bash

JAVA_HOME="/home/hburchell/Downloads/labsjdk-ce-21.0.2-jvmci-23.1-b33"
GRAAL_JAR="/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler/mxbuild/dists/graal.jar"
GRAAL_COMPILER_PATH="/home/hburchell/Repos/graal-dev/graal-instrumentation/compiler"
BENCHMARK_JAR="benchmarks.jar"

COMMON_ARGS="-XX:-TieredCompilation -XX:-BackgroundCompilation -XX:CompileCommand=dontinline,*::* -Djdk.graal.TrivialInliningSize=0 -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler"

INSTRUMENTATION_ARGS="-Djdk.graal.EnableGTSlowDown=false -Djdk.graal.LIRGTSlowDown=false -Djdk.graal.CollectLIRCostInformation=true -Djdk.graal.LIRGTSlowDownDebugMode=true"

run_benchmark() {
    local benchmark_name=$1
    local extra_args=$2
    local cost_file="CollectionDump/GraalCompileOnly_AltCost_Queens_${benchmark_name}.json"
    local compile_only_arg="-Djdk.graal.GraalCompileOnly=Queens.*"

    mx --java-home $JAVA_HOME vm \
        $INSTRUMENTATION_ARGS -Djdk.graal.LIRCostInformationFile=$cost_file \
        $COMMON_ARGS $compile_only_arg \
        -cp "$GRAAL_JAR:$GRAAL_COMPILER_PATH:$BENCHMARK_JAR" \
        Harness $benchmark_name 100 $extra_args
}

# Run all benchmarks
# run_benchmark "DeltaBlue" 60000
# run_benchmark "Richards" 100
# run_benchmark "Json" 100
# run_benchmark "CD" 1000
# run_benchmark "Havlak" 15000
# run_benchmark "Bounce" 10000
# run_benchmark "List" 10000
# run_benchmark "Mandelbrot" 750
# run_benchmark "NBody" 1200000
# run_benchmark "Permute" 3500
 run_benchmark "Queens" 5000
# run_benchmark "Sieve" 10000
# run_benchmark "Storage" 1800
# run_benchmark "Towers" 2500
