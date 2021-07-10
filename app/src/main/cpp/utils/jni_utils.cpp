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

std::string utils::j2std_string(JNIEnv *env, jobject jStr) {
    auto cStr = reinterpret_cast<const char *>(env->GetDirectBufferAddress(jStr));
    auto size = env->GetDirectBufferCapacity(jStr);
    std::string stdStr(cStr, size);
    log_debug("spell j2s", stdStr);
    return stdStr;
}

jobject utils::std2j_string(JNIEnv *env, const std::string& stdStr) {
    log_debug("spell s2j", stdStr);
    size_t byteCount = stdStr.length();
    auto cStr = stdStr.c_str();
    auto buffer = env->NewDirectByteBuffer((void *) cStr, byteCount);
    return buffer;
}
