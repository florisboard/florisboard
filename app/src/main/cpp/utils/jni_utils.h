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

#ifndef FLORISBOARD_JNI_UTILS_H
#define FLORISBOARD_JNI_UTILS_H

#include <nlohmann/json.hpp>

#include <jni.h>

#include <string>
#include <vector>

namespace fl::jni {

using NativeStr = jbyteArray;
using NativeList = jbyteArray;

std::string j2std_string(JNIEnv* env, NativeStr jStr);
NativeStr std2j_string(JNIEnv* env, const std::string& stdStr);

template<typename T>
std::vector<T> j2std_list(JNIEnv* env, NativeList j_list) {
    auto list_str = j2std_string(env, j_list);
    auto json = nlohmann::json::parse(list_str);
    return json.template get<std::vector<T>>();
}

template<typename T>
NativeList std2j_list(JNIEnv* env, std::vector<T> list) {
    auto json = nlohmann::json(list);
    auto list_str = json.dump();
    return std2j_string(env, list_str);
}

} // namespace fl::jni

#endif // FLORISBOARD_JNI_UTILS_H
