FILE(GLOB Source *.cc)
add_library(jvmti SHARED ${Source})

target_include_directories(jvmti PUBLIC ${JNI_INCLUDE_DIRS})