#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
Java_dev_patrickgold_florisboard_ime_core_FlorisBoard_jniHelloWorld(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
