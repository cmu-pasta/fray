#include <jvmti.h>

void CallRuntimeMethod(const char* method, JNIEnv_ *jni_env, const char *method_signature, ...) {
    const static auto runtime_class = "org/pastalab/fray/runtime/Runtime";
    auto clazz = jni_env->FindClass(runtime_class);
    auto method_id = jni_env->GetStaticMethodID(clazz, method, method_signature);
    va_list args;
    va_start(args, method_signature);
    jni_env->CallStaticVoidMethodV(clazz, method_id, args);
    va_end(args);
}

void JNICALL ThreadStart(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {
    CallRuntimeMethod("onThreadRun", jni_env, "()V");
}

void JNICALL ThreadStop(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread) {
    CallRuntimeMethod("onThreadEnd", jni_env, "()V");
}

void JNICALL ClassPrepare(jvmtiEnv *jvmti_env, JNIEnv *jni_env, jthread thread, jclass klass) {
    CallRuntimeMethod("onClassPrepare", jni_env, "(Ljava/lang/Class;)V", klass);
}

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *vm, char *options, void *reserved) {
  jvmtiEnv *jvmti;
  vm->GetEnv((void **)&jvmti, JVMTI_VERSION_1_0);

  jvmtiCapabilities capabilities = {0};
  jvmti->AddCapabilities(&capabilities);

  jvmtiEventCallbacks callbacks = {0};
  callbacks.ThreadStart = ThreadStart;
  callbacks.ThreadEnd = ThreadStop;
  callbacks.ClassPrepare = ClassPrepare;
  jvmti->SetEventCallbacks(&callbacks, sizeof(callbacks));
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_START, NULL);
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_THREAD_END, NULL);
  jvmti->SetEventNotificationMode(JVMTI_ENABLE, JVMTI_EVENT_CLASS_PREPARE, NULL);
  return 0;
}
