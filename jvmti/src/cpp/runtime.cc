#include <iostream>
#include <jvmti.h>

void CallRuntimeMethod(const char* method, JNIEnv_ *jni_env) {
    const static auto runtime_class = "cmu/pasta/fray/runtime/Runtime";
    const static auto callback_method_signature = "()V";
    auto clazz = jni_env->FindClass(runtime_class);
    auto method_id = jni_env->GetStaticMethodID(clazz, method, callback_method_signature);
    jni_env->CallStaticVoidMethod(clazz, method_id);
}

void JNICALL ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {
    CallRuntimeMethod("onThreadRun", jni_env);
}

void JNICALL ThreadStop(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {
    CallRuntimeMethod("onThreadEnd", jni_env);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
  jvmtiEnv *jvmti;
  vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_0);

  jvmtiCapabilities capabilities = {0};
  jvmti->AddCapabilities(&capabilities);

  jvmtiEventCallbacks callbacks = {0};
  callbacks.ThreadStart = ThreadStart;
  callbacks.ThreadEnd = ThreadStop;
  jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL);
  return 0;
}