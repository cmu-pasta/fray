FILE(GLOB Source *.cc)
add_library(jvmti SHARED ${Source})
set_target_properties(jvmti PROPERTIES SUFFIX ".so")

target_include_directories(jvmti PUBLIC ${JNI_INCLUDE_DIRS})