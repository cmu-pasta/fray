FILE(GLOB Source *.cc)
add_library(jvmti SHARED ${Source})

if(WIN32)
    set_target_properties(jvmti PROPERTIES SUFFIX ".dll")
else()
    set_target_properties(jvmti PROPERTIES SUFFIX ".so")
endif()

if (CMAKE_SYSTEM_NAME STREQUAL "Linux")
    target_link_options(jvmti PRIVATE -nostdlib)
endif()

target_include_directories(jvmti PUBLIC ${JNI_INCLUDE_DIRS})
