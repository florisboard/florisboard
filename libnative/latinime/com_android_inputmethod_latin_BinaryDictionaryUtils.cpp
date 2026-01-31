/*
 * Copyright (C) 2014 The Android Open Source Project
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

#define LOG_TAG "LatinIME: jni: BinaryDictionaryUtils"

#include "com_android_inputmethod_latin_BinaryDictionaryUtils.h"

#include "defines.h"
#include "dictionary/utils/dict_file_writing_utils.h"
#include "jni.h"
#include "jni_common.h"
#include "utils/autocorrection_threshold_utils.h"
#include "utils/char_utils.h"
#include "utils/jni_data_utils.h"
#include "utils/time_keeper.h"

namespace latinime {

static jboolean latinime_BinaryDictionaryUtils_createEmptyDictFile(JNIEnv *env, jclass clazz,
        jstring filePath, jlong dictVersion, jstring locale, jobjectArray attributeKeyStringArray,
        jobjectArray attributeValueStringArray) {
    const jsize filePathUtf8Length = env->GetStringUTFLength(filePath);
    char filePathChars[filePathUtf8Length + 1];
    env->GetStringUTFRegion(filePath, 0, env->GetStringLength(filePath), filePathChars);
    filePathChars[filePathUtf8Length] = '\0';

    const jsize localeUtf8Length = env->GetStringUTFLength(locale);
    char localeChars[localeUtf8Length + 1];
    env->GetStringUTFRegion(locale, 0, env->GetStringLength(locale), localeChars);
    localeChars[localeUtf8Length] = '\0';
    std::vector<int> localeCodePoints;
    HeaderReadWriteUtils::insertCharactersIntoVector(localeChars, &localeCodePoints);

    const int keyCount = env->GetArrayLength(attributeKeyStringArray);
    const int valueCount = env->GetArrayLength(attributeValueStringArray);
    if (keyCount != valueCount) {
        return false;
    }
    DictionaryHeaderStructurePolicy::AttributeMap attributeMap =
            JniDataUtils::constructAttributeMap(env, attributeKeyStringArray,
                    attributeValueStringArray);
    return DictFileWritingUtils::createEmptyDictFile(filePathChars, static_cast<int>(dictVersion),
            localeCodePoints, &attributeMap);
}

static jfloat latinime_BinaryDictionaryUtils_calcNormalizedScore(JNIEnv *env, jclass clazz,
        jintArray before, jintArray after, jint score) {
    jsize beforeLength = env->GetArrayLength(before);
    jsize afterLength = env->GetArrayLength(after);
    int beforeCodePoints[beforeLength];
    int afterCodePoints[afterLength];
    env->GetIntArrayRegion(before, 0, beforeLength, beforeCodePoints);
    env->GetIntArrayRegion(after, 0, afterLength, afterCodePoints);
    return AutocorrectionThresholdUtils::calcNormalizedScore(beforeCodePoints, beforeLength,
            afterCodePoints, afterLength, score);
}

static int latinime_BinaryDictionaryUtils_setCurrentTimeForTest(JNIEnv *env, jclass clazz,
        jint currentTime) {
    if (currentTime >= 0) {
        TimeKeeper::startTestModeWithForceCurrentTime(currentTime);
    } else {
        TimeKeeper::stopTestMode();
    }
    TimeKeeper::setCurrentTime();
    return TimeKeeper::peekCurrentTime();
}

static const JNINativeMethod sMethods[] = {
    {
        const_cast<char *>("createEmptyDictFileNative"),
        const_cast<char *>(
                "(Ljava/lang/String;JLjava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionaryUtils_createEmptyDictFile)
    },
    {
        const_cast<char *>("calcNormalizedScoreNative"),
        const_cast<char *>("([I[II)F"),
        reinterpret_cast<void *>(latinime_BinaryDictionaryUtils_calcNormalizedScore)
    },
    {
        const_cast<char *>("setCurrentTimeForTestNative"),
        const_cast<char *>("(I)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionaryUtils_setCurrentTimeForTest)
    }
};

int register_BinaryDictionaryUtils(JNIEnv *env) {
    const char *const kClassPathName = "com/android/inputmethod/latin/utils/BinaryDictionaryUtils";
    return registerNativeMethods(env, kClassPathName, sMethods, NELEMS(sMethods));
}
} // namespace latinime
