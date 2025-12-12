#!/bin/bash
export CORECLR_ENABLE_PROFILING=1
export CORECLR_PROFILER={846f66e7-53e3-4d88-b394-f70c15797305}
export CORECLR_PROFILER_PATH=$(pwd)/libFrayProfiler.so
# Also for older runtimes or different hosts
export CORECLR_PROFILER_PATH_64=$CORECLR_PROFILER_PATH

# Copy .so to the test directory to ensure DllImport finds it
cp libFrayProfiler.so dotnet_migration/Fray.Test/bin/Debug/net8.0/
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$(pwd)

echo "Running Fray POC..."
echo "Profiler Path: $CORECLR_PROFILER_PATH"

dotnet dotnet_migration/Fray.Test/bin/Debug/net8.0/Fray.Test.dll
