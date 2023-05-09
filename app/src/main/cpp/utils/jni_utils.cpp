/*
 * Copyright (C) 2021 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "jni_utils.h"
#include "log.h"

std::string fl::jni::j2std_string(JNIEnv *env, NativeStr jStr) {
    auto length = env->GetArrayLength(jStr);
    jbyte* bytes = env->GetByteArrayElements(jStr, nullptr);
    std::string stdStr(reinterpret_cast<char*>(bytes), length);
    env->ReleaseByteArrayElements(jStr, bytes, JNI_ABORT);
    //utils::log(ANDROID_LOG_DEBUG, "fl::jni::j2s", stdStr);
    return stdStr;
}

fl::jni::NativeStr fl::jni::std2j_string(JNIEnv *env, const std::string& stdStr) {
    //utils::log(ANDROID_LOG_DEBUG, "fl::jni::s2j", stdStr);
    auto length = static_cast<jsize>(stdStr.size());
    NativeStr jStr = env->NewByteArray(length);
    env->SetByteArrayRegion(jStr, 0, length, reinterpret_cast<const jbyte*>(stdStr.c_str()));
    return jStr;
}
