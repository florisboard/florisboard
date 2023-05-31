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

#include "fl_nlp_core_latin_nlp_session.hpp"
#include "utils/jni_exception.h"
#include "utils/jni_utils.h"

#include <unicode/udata.h>

#include <jni.h>

#include <fstream>
#include <vector>

extern "C" JNIEXPORT jlong JNICALL
Java_dev_patrickgold_florisboard_ime_nlp_latin_LatinNlpSession_00024CXX_nativeInit( //
    JNIEnv* env,
    jobject
) {
    return fl::jni::run_in_exception_container(env, [&] {
        auto* session = new fl::nlp::LatinNlpSession();
        return reinterpret_cast<jlong>(session);
    });
}

extern "C" JNIEXPORT void JNICALL
Java_dev_patrickgold_florisboard_ime_nlp_latin_LatinNlpSession_00024CXX_nativeDispose( //
    JNIEnv* env,
    jobject,
    jlong native_ptr
) {
    return fl::jni::run_in_exception_container(env, [&] {
        auto* session = reinterpret_cast<fl::nlp::LatinNlpSession*>(native_ptr);
        delete session;
    });
}

extern "C" JNIEXPORT void JNICALL
Java_dev_patrickgold_florisboard_ime_nlp_latin_LatinNlpSession_00024CXX_nativeLoadFromConfigFile( //
    JNIEnv* env,
    jobject,
    jlong native_ptr,
    fl::jni::NativeStr j_config_path
) {
    return fl::jni::run_in_exception_container(env, [&] {
        auto* session = reinterpret_cast<fl::nlp::LatinNlpSession*>(native_ptr);
        auto config_path = fl::jni::j2std_string(env, j_config_path);
        session->loadConfigFromFile(config_path);
    });
}

extern "C" JNIEXPORT fl::jni::NativeStr JNICALL
Java_dev_patrickgold_florisboard_ime_nlp_latin_LatinNlpSession_00024CXX_nativeSpell( //
    JNIEnv* env,
    jobject,
    jlong native_ptr,
    fl::jni::NativeStr j_word,
    fl::jni::NativeList j_prev_words,
    jint flags
) {
    auto* session = reinterpret_cast<fl::nlp::LatinNlpSession*>(native_ptr);
    auto word = fl::jni::j2std_string(env, j_word);
    auto prev_words = fl::jni::j2std_list<std::string>(env, j_prev_words);
    auto spelling_result = session->spell(word, prev_words, flags);
    auto json = nlohmann::json();
    json["suggestionAttributes"] = spelling_result.suggestion_attributes;
    json["suggestions"] = spelling_result.suggestions;
    return fl::jni::std2j_string(env, json.dump());
}

extern "C" JNIEXPORT fl::jni::NativeList JNICALL
Java_dev_patrickgold_florisboard_ime_nlp_latin_LatinNlpSession_00024CXX_nativeSuggest( //
    JNIEnv* env,
    jobject,
    jlong native_ptr,
    fl::jni::NativeStr j_word,
    fl::jni::NativeList j_prev_words,
    jint flags
) {
    auto* session = reinterpret_cast<fl::nlp::LatinNlpSession*>(native_ptr);
    auto word = fl::jni::j2std_string(env, j_word);
    auto prev_words = fl::jni::j2std_list<std::string>(env, j_prev_words);
    fl::nlp::SuggestionResults suggestion_results;
    session->suggest(word, prev_words, flags, suggestion_results);
    std::vector<fl::nlp::SuggestionCandidate> candidates;
    candidates.reserve(suggestion_results.size());
    for (auto& candidate_ptr : suggestion_results) {
        candidates.push_back(std::move(*candidate_ptr));
    }
    return fl::jni::std2j_list(env, candidates);
}
