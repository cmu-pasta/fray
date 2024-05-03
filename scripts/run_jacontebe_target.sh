#!/bin/bash

# Check if all arguments are provided
if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <sfuzz_root> <jacontebe_experiment_root> <target_name>"
    exit 1
fi

sfuzz_root=$(realpath "$1")
experiment_root=$(realpath "$2")
export experiment_root=$experiment_root
target="$3"

cd $sfuzz_root

echo "TARGET: $target"

# Skip directory names containing "jdk"
if [[ $target == *"jdk"* ]]; then
    echo "Skipping target: $target (contains 'jdk')"
    exit 0
fi

# Run install script
${experiment_root}/JaConTeBe/scripts/install.sh orig $target
echo ${experiment_root}/JaConTeBe/scripts/install.sh orig $target

# Extract target_class from ${target}.sh file
target_class=$(grep "class_to_run=" "$experiment_root/JaConTeBe/testplans.alt/testscript/${target}.sh" | cut -d'=' -f2)

echo "TARGET CLASS: $target_class"

# Run gradlew command
echo ./gradlew runJC -Pclasspath="${experiment_root}/JaConTeBe/source/:${experiment_root}/JaConTeBe/versions.alt/lib/${target}.jar" -PmainClass="$target_class" -PextraArgs="--scheduler pct --num-switch-points 3"
./gradlew runJC -Pclasspath="${experiment_root}/JaConTeBe/source/:${experiment_root}/JaConTeBe/versions.alt/lib/${target}.jar" -PmainClass="$target_class" -PextraArgs="--scheduler pct --num-switch-points 3"
