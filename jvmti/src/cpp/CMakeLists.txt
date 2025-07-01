FILE(GLOB Source *.cc)
add_library(jvmti SHARED ${Source})

# set static lib suffix
if(WIN32)
    set_target_properties(jvmti PROPERTIES SUFFIX ".dll")
else()
    set_target_properties(jvmti PROPERTIES SUFFIX ".so")
endif()

# Statically link libstdc++ to avoid runtime dependencies
if(CMAKE_CXX_COMPILER_ID MATCHES "GNU")
    target_link_options(jvmti PRIVATE -static-libstdc++ -static-libgcc)
endif()

target_include_directories(jvmti PUBLIC ${JNI_INCLUDE_DIRS})
