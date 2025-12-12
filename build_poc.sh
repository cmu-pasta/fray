#!/bin/bash
set -e

# Build C++ Profiler
echo "Building Profiler..."
cd dotnet_migration/Fray.Profiler
mkdir -p build
cd build
cmake ..
make
cp libFrayProfiler.so ../../../
cd ../../..

# Build .NET Solution
echo "Building .NET Solution..."
dotnet build dotnet_migration/Fray.sln

echo "Build Complete."
