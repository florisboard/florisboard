/*
 * Copyright (C) 2023 Patrick Goldinger
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

#include "fl_nlp_core_latin_dictionary.hpp"
#include "utils/jni_exception.h"
#include "utils/jni_utils.h"

#include <jni.h>

extern "C" JNIEXPORT void JNICALL
Java_dev_patrickgold_florisboard_ime_nlp_latin_LatinLanguageProvider_00024Companion_nativeInitEmptyDictionary( //
    JNIEnv* env,
    jobject,
    fl::jni::NativeStr j_dict_path
) {
    return fl::jni::run_in_exception_container(env, [&] {
        auto dict_path = fl::jni::j2std_string(env, j_dict_path);
        auto dict = fl::nlp::LatinDictionary(0);
        dict.file_path = dict_path;
        dict.persistToDisk();
    });
}
