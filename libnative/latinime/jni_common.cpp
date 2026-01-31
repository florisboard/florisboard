/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "LatinIME: jni"

#include "jni_common.h"

#include "com_android_inputmethod_keyboard_ProximityInfo.h"
#include "com_android_inputmethod_latin_BinaryDictionary.h"
#include "com_android_inputmethod_latin_BinaryDictionaryUtils.h"
#include "com_android_inputmethod_latin_DicTraverseSession.h"
#include "defines.h"

/*
 * Returns the JNI version on success, -1 on failure.
 */
jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env = 0;

    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK) {
        AKLOGE("ERROR: GetEnv failed");
        return -1;
    }
    ASSERT(env);
    if (!env) {
        AKLOGE("ERROR: JNIEnv is invalid");
        return -1;
    }
    if (!latinime::register_BinaryDictionary(env)) {
        AKLOGE("ERROR: BinaryDictionary native registration failed");
        return -1;
    }
    if (!latinime::register_BinaryDictionaryUtils(env)) {
        AKLOGE("ERROR: BinaryDictionaryUtils native registration failed");
        return -1;
    }
    if (!latinime::register_DicTraverseSession(env)) {
        AKLOGE("ERROR: DicTraverseSession native registration failed");
        return -1;
    }
    if (!latinime::register_ProximityInfo(env)) {
        AKLOGE("ERROR: ProximityInfo native registration failed");
        return -1;
    }
    /* success -- return valid version number */
    return JNI_VERSION_1_6;
}

namespace latinime {
int registerNativeMethods(JNIEnv *env, const char *const className, const JNINativeMethod *methods,
        const int numMethods) {
    jclass clazz = env->FindClass(className);
    if (!clazz) {
        AKLOGE("Native registration unable to find class '%s'", className);
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods, numMethods) != 0) {
        AKLOGE("RegisterNatives failed for '%s'", className);
        env->DeleteLocalRef(clazz);
        return JNI_FALSE;
    }
    env->DeleteLocalRef(clazz);
    return JNI_TRUE;
}
} // namespace latinime
