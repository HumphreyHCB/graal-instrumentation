#!/bin/bash

# Define the base directory
base_dir="BuboDumps"

# Define the list of benchmarks
benchmarks=("Bounce" "CD" "DeltaBlue" "Havlak" "Json" "List" "Mandelbrot" "NBody" "Permute" "Queens" "Richards" "Sieve" "Storage" "Towers")

# Loop through each dump directory
for i in {1..10}; do
    # Construct the directory path
    dump_dir="$base_dir/BuboDump$i"

    # Check if the directory exists
    if [[ -d "$dump_dir" ]]; then
        # Loop through each benchmark
        for benchmark in "${benchmarks[@]}"; do
            # Construct the file pattern to remove
            file_pattern="${dump_dir}/${benchmark}.txt"

            # Check if the file exists and remove it
            if [[ -f "$file_pattern" ]]; then
                echo "Removing $file_pattern"
                rm "$file_pattern"
            else
                echo "File not found: $file_pattern"
            fi
        done
    else
        echo "Directory not found: $dump_dir"
    fi
done
