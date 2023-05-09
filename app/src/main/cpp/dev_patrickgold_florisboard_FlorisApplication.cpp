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

#include <fstream>
#include <vector>
#include <jni.h>
#include <unicode/udata.h>
#include "utils/jni_utils.h"

#include "fl_icuext.hpp"

#pragma ide diagnostic ignored "UnusedLocalVariable"

extern "C"
JNIEXPORT jint JNICALL
Java_dev_patrickgold_florisboard_FlorisApplication_00024Companion_nativeInitICUData(
        JNIEnv *env, jobject, fl::jni::NativeStr path)
{
    auto path_str = fl::jni::j2std_string(env, path);
    auto status = fl::icuext::loadAndSetCommonData(path_str);
    return status;
}
