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

#include <jni.h>
#include <algorithm>
#include "ime/spelling/spellingdict.h"
#include "utils/jni_utils.h"

#pragma ide diagnostic ignored "UnusedLocalVariable"

using namespace ime::spellcheck;

extern "C"
JNIEXPORT jlong JNICALL
Java_dev_patrickgold_florisboard_ime_spelling_SpellingDict_00024Companion_nativeInitialize(
        JNIEnv *env,
        jobject thiz,
        jobject base_path) {
    auto strBasePath = utils::j2std_string(env, base_path);

    auto *spellingDict = SpellingDict::load(strBasePath);

    if (spellingDict == nullptr) {
        return 0L;
    } else {
        return reinterpret_cast<jlong>(spellingDict);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_dev_patrickgold_florisboard_ime_spelling_SpellingDict_00024Companion_nativeDispose(
        JNIEnv *env,
        jobject thiz,
        jlong native_ptr) {
    auto spellingDict = reinterpret_cast<SpellingDict *>(native_ptr);

    delete spellingDict;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_dev_patrickgold_florisboard_ime_spelling_SpellingDict_00024Companion_nativeSpell(
        JNIEnv *env,
        jobject thiz,
        jlong native_ptr,
        jobject word) {
    auto strWord = utils::j2std_string(env, word);

    auto spellingDict = reinterpret_cast<SpellingDict *>(native_ptr);
    auto result = spellingDict->spell(strWord);

    return result;
}

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_dev_patrickgold_florisboard_ime_spelling_SpellingDict_00024Companion_nativeSuggest(
        JNIEnv *env,
        jobject thiz,
        jlong native_ptr,
        jobject word,
        jint limit) {
    auto strWord = utils::j2std_string(env, word);

    auto spellingDict = reinterpret_cast<SpellingDict *>(native_ptr);
    auto result = spellingDict->suggest(strWord);
    auto retSize = std::min(result.size(), (size_t)std::max(0, limit));

    jclass jByteArrayClass = env->FindClass("java/nio/ByteBuffer");
    jobjectArray jSuggestions = env->NewObjectArray(retSize, jByteArrayClass, nullptr);
    for (int n = 0; n < retSize; n++) {
        env->SetObjectArrayElement(jSuggestions, n, utils::std2j_string(env, result[n]));
    }

    return jSuggestions;
}
