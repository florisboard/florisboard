/*
 * Copyright (C) 2013, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "log_utils.h"

#include <cstdio>
#include <stdarg.h>

#include "defines.h"

namespace latinime {
    /* static */ void LogUtils::logToJava(JNIEnv *const env, const char *const format, ...) {
        static const char *TAG = "LatinIME:LogUtils";
        const jclass androidUtilLogClass = env->FindClass("android/util/Log");
        if (!androidUtilLogClass) {
            // If we can't find the class, we are probably in off-device testing, and
            // it's expected. Regardless, logging is not essential to functionality, so
            // we should just return. However, FindClass has thrown an exception behind
            // our back and there is no way to prevent it from doing that, so we clear
            // the exception before we return.
            env->ExceptionClear();
            return;
        }
        const jmethodID logDotIMethodId = env->GetStaticMethodID(androidUtilLogClass, "i",
                "(Ljava/lang/String;Ljava/lang/String;)I");
        if (!logDotIMethodId) {
            env->ExceptionClear();
            if (androidUtilLogClass) env->DeleteLocalRef(androidUtilLogClass);
            return;
        }
        const jstring javaTag = env->NewStringUTF(TAG);

        static const int DEFAULT_LINE_SIZE = 128;
        char fixedSizeCString[DEFAULT_LINE_SIZE];
        va_list argList;
        va_start(argList, format);
        // Get the necessary size. Add 1 for the 0 terminator.
        const int size = vsnprintf(fixedSizeCString, DEFAULT_LINE_SIZE, format, argList) + 1;
        va_end(argList);

        jstring javaString;
        if (size <= DEFAULT_LINE_SIZE) {
            // The buffer was large enough.
            javaString = env->NewStringUTF(fixedSizeCString);
        } else {
            // The buffer was not large enough.
            va_start(argList, format);
            char variableSizeCString[size];
            vsnprintf(variableSizeCString, size, format, argList);
            va_end(argList);
            javaString = env->NewStringUTF(variableSizeCString);
        }

        env->CallStaticIntMethod(androidUtilLogClass, logDotIMethodId, javaTag, javaString);
        if (javaString) env->DeleteLocalRef(javaString);
        if (javaTag) env->DeleteLocalRef(javaTag);
        if (androidUtilLogClass) env->DeleteLocalRef(androidUtilLogClass);
    }
}
