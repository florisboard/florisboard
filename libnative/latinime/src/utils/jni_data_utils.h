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

#ifndef LATINIME_JNI_DATA_UTILS_H
#define LATINIME_JNI_DATA_UTILS_H

#include <vector>

#include "defines.h"
#include "dictionary/header/header_read_write_utils.h"
#include "dictionary/interface/dictionary_header_structure_policy.h"
#include "dictionary/property/ngram_context.h"
#include "dictionary/property/word_property.h"
#include "jni.h"
#include "utils/char_utils.h"

namespace latinime {

class JniDataUtils {
 public:
    static void jintarrayToVector(JNIEnv *env, jintArray array, std::vector<int> *const outVector) {
        if (!array) {
            outVector->clear();
            return;
        }
        const jsize arrayLength = env->GetArrayLength(array);
        outVector->resize(arrayLength);
        env->GetIntArrayRegion(array, 0 /* start */, arrayLength, outVector->data());
    }

    static DictionaryHeaderStructurePolicy::AttributeMap constructAttributeMap(JNIEnv *env,
            jobjectArray attributeKeyStringArray, jobjectArray attributeValueStringArray) {
        DictionaryHeaderStructurePolicy::AttributeMap attributeMap;
        const int keyCount = env->GetArrayLength(attributeKeyStringArray);
        for (int i = 0; i < keyCount; i++) {
            jstring keyString = static_cast<jstring>(
                    env->GetObjectArrayElement(attributeKeyStringArray, i));
            const jsize keyUtf8Length = env->GetStringUTFLength(keyString);
            char keyChars[keyUtf8Length + 1];
            env->GetStringUTFRegion(keyString, 0, env->GetStringLength(keyString), keyChars);
            env->DeleteLocalRef(keyString);
            keyChars[keyUtf8Length] = '\0';
            DictionaryHeaderStructurePolicy::AttributeMap::key_type key;
            HeaderReadWriteUtils::insertCharactersIntoVector(keyChars, &key);

            jstring valueString = static_cast<jstring>(
                    env->GetObjectArrayElement(attributeValueStringArray, i));
            const jsize valueUtf8Length = env->GetStringUTFLength(valueString);
            char valueChars[valueUtf8Length + 1];
            env->GetStringUTFRegion(valueString, 0, env->GetStringLength(valueString), valueChars);
            env->DeleteLocalRef(valueString);
            valueChars[valueUtf8Length] = '\0';
            DictionaryHeaderStructurePolicy::AttributeMap::mapped_type value;
            HeaderReadWriteUtils::insertCharactersIntoVector(valueChars, &value);
            attributeMap[key] = value;
        }
        return attributeMap;
    }

    static void outputCodePoints(JNIEnv *env, jintArray intArrayToOutputCodePoints, const int start,
            const int maxLength, const int *const codePoints, const int codePointCount,
            const bool needsNullTermination) {
        const int codePointBufSize = std::min(maxLength, codePointCount);
        int outputCodePonts[codePointBufSize];
        int outputCodePointCount = 0;
        for (int i = 0; i < codePointBufSize; ++i) {
            const int codePoint = codePoints[i];
            int codePointToOutput = codePoint;
            if (!CharUtils::isInUnicodeSpace(codePoint)) {
                if (codePoint == CODE_POINT_BEGINNING_OF_SENTENCE) {
                    // Just skip Beginning-of-Sentence marker.
                    continue;
                }
                codePointToOutput = CODE_POINT_REPLACEMENT_CHARACTER;
            } else if (codePoint >= 0x01 && codePoint <= 0x1F) {
                // Control code.
                codePointToOutput = CODE_POINT_REPLACEMENT_CHARACTER;
            }
            outputCodePonts[outputCodePointCount++] = codePointToOutput;
        }
        env->SetIntArrayRegion(intArrayToOutputCodePoints, start, outputCodePointCount,
                outputCodePonts);
        if (needsNullTermination && outputCodePointCount < maxLength) {
            env->SetIntArrayRegion(intArrayToOutputCodePoints, start + outputCodePointCount,
                    1 /* len */, &CODE_POINT_NULL);
        }
    }

    static NgramContext constructNgramContext(JNIEnv *env, jobjectArray prevWordCodePointArrays,
            jbooleanArray isBeginningOfSentenceArray, const size_t prevWordCount) {
        int prevWordCodePoints[MAX_PREV_WORD_COUNT_FOR_N_GRAM][MAX_WORD_LENGTH];
        int prevWordCodePointCount[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
        bool isBeginningOfSentence[MAX_PREV_WORD_COUNT_FOR_N_GRAM];
        for (size_t i = 0; i < prevWordCount; ++i) {
            prevWordCodePointCount[i] = 0;
            isBeginningOfSentence[i] = false;
            jintArray prevWord = (jintArray)env->GetObjectArrayElement(prevWordCodePointArrays, i);
            if (!prevWord) {
                continue;
            }
            jsize prevWordLength = env->GetArrayLength(prevWord);
            if (prevWordLength > MAX_WORD_LENGTH) {
                continue;
            }
            env->GetIntArrayRegion(prevWord, 0, prevWordLength, prevWordCodePoints[i]);
            env->DeleteLocalRef(prevWord);
            prevWordCodePointCount[i] = prevWordLength;
            jboolean isBeginningOfSentenceBoolean = JNI_FALSE;
            env->GetBooleanArrayRegion(isBeginningOfSentenceArray, i, 1 /* len */,
                    &isBeginningOfSentenceBoolean);
            isBeginningOfSentence[i] = isBeginningOfSentenceBoolean == JNI_TRUE;
        }
        return NgramContext(prevWordCodePoints, prevWordCodePointCount, isBeginningOfSentence,
                prevWordCount);
    }

    static void putBooleanToArray(JNIEnv *env, jbooleanArray array, const int index,
            const jboolean value) {
        env->SetBooleanArrayRegion(array, index, 1 /* len */, &value);
    }

    static void putIntToArray(JNIEnv *env, jintArray array, const int index, const int value) {
        env->SetIntArrayRegion(array, index, 1 /* len */, &value);
    }

    static void putFloatToArray(JNIEnv *env, jfloatArray array, const int index,
            const float value) {
        env->SetFloatArrayRegion(array, index, 1 /* len */, &value);
    }

    static void outputWordProperty(JNIEnv *const env, const WordProperty &wordProperty,
            jintArray outCodePoints, jbooleanArray outFlags, jintArray outProbabilityInfo,
            jobject outNgramPrevWordsArray, jobject outNgramPrevWordIsBeginningOfSentenceArray,
            jobject outNgramTargets, jobject outNgramProbabilities, jobject outShortcutTargets,
            jobject outShortcutProbabilities);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(JniDataUtils);

    static const int CODE_POINT_REPLACEMENT_CHARACTER;
    static const int CODE_POINT_NULL;
};
} // namespace latinime
#endif // LATINIME_JNI_DATA_UTILS_H
