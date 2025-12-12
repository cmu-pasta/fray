cmake_minimum_required(VERSION 3.10)
project(FrayProfiler)

set(CMAKE_CXX_STANDARD 11)

include_directories(headers)

add_library(FrayProfiler SHARED Profiler.cpp ClassFactory.cpp)

if(UNIX)
    # Don't prefix with "lib", the CLR expects just the name sometimes, but usually .so has lib.
    # Actually, CORECLR_PROFILER_PATH can point to anything.
    # But usually Linux shared objects start with lib.
    set_target_properties(FrayProfiler PROPERTIES PREFIX "lib")
endif()
