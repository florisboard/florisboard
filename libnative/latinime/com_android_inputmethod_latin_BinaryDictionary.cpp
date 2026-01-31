/*
 * Copyright (C) 2009 The Android Open Source Project
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

#define LOG_TAG "LatinIME: jni: BinaryDictionary"

#include "com_android_inputmethod_latin_BinaryDictionary.h"

#include <cstring> // for memset()
#include <vector>

#include "defines.h"
#include "dictionary/property/unigram_property.h"
#include "dictionary/property/ngram_context.h"
#include "dictionary/property/word_property.h"
#include "dictionary/structure/dictionary_structure_with_buffer_policy_factory.h"
#include "jni.h"
#include "jni_common.h"
#include "suggest/core/dictionary/dictionary.h"
#include "suggest/core/result/suggestion_results.h"
#include "suggest/core/suggest_options.h"
#include "utils/char_utils.h"
#include "utils/int_array_view.h"
#include "utils/jni_data_utils.h"
#include "utils/log_utils.h"
#include "utils/profiler.h"
#include "utils/time_keeper.h"

namespace latinime {

class ProximityInfo;

static jlong latinime_BinaryDictionary_open(JNIEnv *env, jclass clazz, jstring sourceDir,
        jlong dictOffset, jlong dictSize, jboolean isUpdatable) {
    PROF_INIT;
    PROF_TIMER_START(66);
    const jsize sourceDirUtf8Length = env->GetStringUTFLength(sourceDir);
    if (sourceDirUtf8Length <= 0) {
        AKLOGE("DICT: Can't get sourceDir string");
        return 0;
    }
    char sourceDirChars[sourceDirUtf8Length + 1];
    env->GetStringUTFRegion(sourceDir, 0, env->GetStringLength(sourceDir), sourceDirChars);
    sourceDirChars[sourceDirUtf8Length] = '\0';
    DictionaryStructureWithBufferPolicy::StructurePolicyPtr dictionaryStructureWithBufferPolicy(
            DictionaryStructureWithBufferPolicyFactory::newPolicyForExistingDictFile(
                    sourceDirChars, static_cast<int>(dictOffset), static_cast<int>(dictSize),
                    isUpdatable == JNI_TRUE));
    if (!dictionaryStructureWithBufferPolicy) {
        return 0;
    }

    Dictionary *const dictionary =
            new Dictionary(env, std::move(dictionaryStructureWithBufferPolicy));
    PROF_TIMER_END(66);
    return reinterpret_cast<jlong>(dictionary);
}

static jlong latinime_BinaryDictionary_createOnMemory(JNIEnv *env, jclass clazz,
        jlong formatVersion, jstring locale, jobjectArray attributeKeyStringArray,
        jobjectArray attributeValueStringArray) {
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
    DictionaryStructureWithBufferPolicy::StructurePolicyPtr dictionaryStructureWithBufferPolicy =
            DictionaryStructureWithBufferPolicyFactory::newPolicyForOnMemoryDict(
                    formatVersion, localeCodePoints, &attributeMap);
    if (!dictionaryStructureWithBufferPolicy) {
        return 0;
    }
    Dictionary *const dictionary =
            new Dictionary(env, std::move(dictionaryStructureWithBufferPolicy));
    return reinterpret_cast<jlong>(dictionary);
}

static bool latinime_BinaryDictionary_flush(JNIEnv *env, jclass clazz, jlong dict,
        jstring filePath) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return false;
    const jsize filePathUtf8Length = env->GetStringUTFLength(filePath);
    char filePathChars[filePathUtf8Length + 1];
    env->GetStringUTFRegion(filePath, 0, env->GetStringLength(filePath), filePathChars);
    filePathChars[filePathUtf8Length] = '\0';
    return dictionary->flush(filePathChars);
}

static bool latinime_BinaryDictionary_needsToRunGC(JNIEnv *env, jclass clazz,
        jlong dict, jboolean mindsBlockByGC) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return false;
    return dictionary->needsToRunGC(mindsBlockByGC == JNI_TRUE);
}

static bool latinime_BinaryDictionary_flushWithGC(JNIEnv *env, jclass clazz, jlong dict,
        jstring filePath) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return false;
    const jsize filePathUtf8Length = env->GetStringUTFLength(filePath);
    char filePathChars[filePathUtf8Length + 1];
    env->GetStringUTFRegion(filePath, 0, env->GetStringLength(filePath), filePathChars);
    filePathChars[filePathUtf8Length] = '\0';
    return dictionary->flushWithGC(filePathChars);
}

static void latinime_BinaryDictionary_close(JNIEnv *env, jclass clazz, jlong dict) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return;
    delete dictionary;
}

static void latinime_BinaryDictionary_getHeaderInfo(JNIEnv *env, jclass clazz, jlong dict,
        jintArray outHeaderSize, jintArray outFormatVersion, jobject outAttributeKeys,
        jobject outAttributeValues) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return;
    const DictionaryHeaderStructurePolicy *const headerPolicy =
            dictionary->getDictionaryStructurePolicy()->getHeaderStructurePolicy();
    JniDataUtils::putIntToArray(env, outHeaderSize, 0 /* index */, headerPolicy->getSize());
    JniDataUtils::putIntToArray(env, outFormatVersion, 0 /* index */,
            headerPolicy->getFormatVersionNumber());
    // Output attribute map
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID addMethodId = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    const DictionaryHeaderStructurePolicy::AttributeMap *const attributeMap =
            headerPolicy->getAttributeMap();
    for (DictionaryHeaderStructurePolicy::AttributeMap::const_iterator it = attributeMap->begin();
            it != attributeMap->end(); ++it) {
        // Output key
        jintArray keyCodePointArray = env->NewIntArray(it->first.size());
        JniDataUtils::outputCodePoints(env, keyCodePointArray, 0 /* start */,
                it->first.size(), it->first.data(), it->first.size(),
                false /* needsNullTermination */);
        env->CallBooleanMethod(outAttributeKeys, addMethodId, keyCodePointArray);
        env->DeleteLocalRef(keyCodePointArray);
        // Output value
        jintArray valueCodePointArray = env->NewIntArray(it->second.size());
        JniDataUtils::outputCodePoints(env, valueCodePointArray, 0 /* start */,
                it->second.size(), it->second.data(), it->second.size(),
                false /* needsNullTermination */);
        env->CallBooleanMethod(outAttributeValues, addMethodId, valueCodePointArray);
        env->DeleteLocalRef(valueCodePointArray);
    }
    env->DeleteLocalRef(arrayListClass);
    return;
}

static int latinime_BinaryDictionary_getFormatVersion(JNIEnv *env, jclass clazz, jlong dict) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return 0;
    const DictionaryHeaderStructurePolicy *const headerPolicy =
            dictionary->getDictionaryStructurePolicy()->getHeaderStructurePolicy();
    return headerPolicy->getFormatVersionNumber();
}

static void latinime_BinaryDictionary_getSuggestions(JNIEnv *env, jclass clazz, jlong dict,
        jlong proximityInfo, jlong dicTraverseSession, jintArray xCoordinatesArray,
        jintArray yCoordinatesArray, jintArray timesArray, jintArray pointerIdsArray,
        jintArray inputCodePointsArray, jint inputSize, jintArray suggestOptions,
        jobjectArray prevWordCodePointArrays, jbooleanArray isBeginningOfSentenceArray,
        jint prevWordCount, jintArray outSuggestionCount, jintArray outCodePointsArray,
        jintArray outScoresArray, jintArray outSpaceIndicesArray, jintArray outTypesArray,
        jintArray outAutoCommitFirstWordConfidenceArray,
        jfloatArray inOutWeightOfLangModelVsSpatialModel) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    // Assign 0 to outSuggestionCount here in case of returning earlier in this method.
    JniDataUtils::putIntToArray(env, outSuggestionCount, 0 /* index */, 0);
    if (!dictionary) {
        return;
    }
    ProximityInfo *pInfo = reinterpret_cast<ProximityInfo *>(proximityInfo);
    DicTraverseSession *traverseSession =
            reinterpret_cast<DicTraverseSession *>(dicTraverseSession);
    if (!traverseSession) {
        return;
    }
    // Input values
    int xCoordinates[inputSize];
    int yCoordinates[inputSize];
    int times[inputSize];
    int pointerIds[inputSize];
    const jsize inputCodePointsLength = env->GetArrayLength(inputCodePointsArray);
    int inputCodePoints[inputCodePointsLength];
    env->GetIntArrayRegion(xCoordinatesArray, 0, inputSize, xCoordinates);
    env->GetIntArrayRegion(yCoordinatesArray, 0, inputSize, yCoordinates);
    env->GetIntArrayRegion(timesArray, 0, inputSize, times);
    env->GetIntArrayRegion(pointerIdsArray, 0, inputSize, pointerIds);
    env->GetIntArrayRegion(inputCodePointsArray, 0, inputCodePointsLength, inputCodePoints);

    const jsize numberOfOptions = env->GetArrayLength(suggestOptions);
    int options[numberOfOptions];
    env->GetIntArrayRegion(suggestOptions, 0, numberOfOptions, options);
    SuggestOptions givenSuggestOptions(options, numberOfOptions);

    // Output values
    /* By the way, let's check the output array length here to make sure */
    const jsize outputCodePointsLength = env->GetArrayLength(outCodePointsArray);
    if (outputCodePointsLength != (MAX_WORD_LENGTH * MAX_RESULTS)) {
        AKLOGE("Invalid outputCodePointsLength: %d", outputCodePointsLength);
        ASSERT(false);
        return;
    }
    const jsize scoresLength = env->GetArrayLength(outScoresArray);
    if (scoresLength != MAX_RESULTS) {
        AKLOGE("Invalid scoresLength: %d", scoresLength);
        ASSERT(false);
        return;
    }
    const jsize outputAutoCommitFirstWordConfidenceLength =
            env->GetArrayLength(outAutoCommitFirstWordConfidenceArray);
    ASSERT(outputAutoCommitFirstWordConfidenceLength == 1);
    if (outputAutoCommitFirstWordConfidenceLength != 1) {
        // We only use the first result, as obviously we will only ever autocommit the first one
        AKLOGE("Invalid outputAutoCommitFirstWordConfidenceLength: %d",
                outputAutoCommitFirstWordConfidenceLength);
        ASSERT(false);
        return;
    }
    float weightOfLangModelVsSpatialModel;
    env->GetFloatArrayRegion(inOutWeightOfLangModelVsSpatialModel, 0, 1 /* len */,
            &weightOfLangModelVsSpatialModel);
    SuggestionResults suggestionResults(MAX_RESULTS);
    const NgramContext ngramContext = JniDataUtils::constructNgramContext(env,
            prevWordCodePointArrays, isBeginningOfSentenceArray, prevWordCount);
    if (givenSuggestOptions.isGesture() || inputSize > 0) {
        // TODO: Use SuggestionResults to return suggestions.
        dictionary->getSuggestions(pInfo, traverseSession, xCoordinates, yCoordinates,
                times, pointerIds, inputCodePoints, inputSize, &ngramContext,
                &givenSuggestOptions, weightOfLangModelVsSpatialModel, &suggestionResults);
    } else {
        dictionary->getPredictions(&ngramContext, &suggestionResults);
    }
    if (DEBUG_DICT) {
        suggestionResults.dumpSuggestions();
    }
    suggestionResults.outputSuggestions(env, outSuggestionCount, outCodePointsArray,
            outScoresArray, outSpaceIndicesArray, outTypesArray,
            outAutoCommitFirstWordConfidenceArray, inOutWeightOfLangModelVsSpatialModel);
}

static jint latinime_BinaryDictionary_getProbability(JNIEnv *env, jclass clazz, jlong dict,
        jintArray word) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return NOT_A_PROBABILITY;
    const jsize codePointCount = env->GetArrayLength(word);
    int codePoints[codePointCount];
    env->GetIntArrayRegion(word, 0, codePointCount, codePoints);
    return dictionary->getProbability(CodePointArrayView(codePoints, codePointCount));
}

static jint latinime_BinaryDictionary_getMaxProbabilityOfExactMatches(
        JNIEnv *env, jclass clazz, jlong dict, jintArray word) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return NOT_A_PROBABILITY;
    const jsize codePointCount = env->GetArrayLength(word);
    int codePoints[codePointCount];
    env->GetIntArrayRegion(word, 0, codePointCount, codePoints);
    return dictionary->getMaxProbabilityOfExactMatches(
            CodePointArrayView(codePoints, codePointCount));
}

static jint latinime_BinaryDictionary_getNgramProbability(JNIEnv *env, jclass clazz,
        jlong dict, jobjectArray prevWordCodePointArrays, jbooleanArray isBeginningOfSentenceArray,
        jintArray word) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return JNI_FALSE;
    const jsize wordLength = env->GetArrayLength(word);
    int wordCodePoints[wordLength];
    env->GetIntArrayRegion(word, 0, wordLength, wordCodePoints);
    const NgramContext ngramContext = JniDataUtils::constructNgramContext(env,
            prevWordCodePointArrays, isBeginningOfSentenceArray,
            env->GetArrayLength(prevWordCodePointArrays));
    return dictionary->getNgramProbability(&ngramContext,
            CodePointArrayView(wordCodePoints, wordLength));
}

// Method to iterate all words in the dictionary for makedict.
// If token is 0, this method newly starts iterating the dictionary. This method returns 0 when
// the dictionary does not have a next word.
static jint latinime_BinaryDictionary_getNextWord(JNIEnv *env, jclass clazz,
        jlong dict, jint token, jintArray outCodePoints, jbooleanArray outIsBeginningOfSentence) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return 0;
    const jsize codePointBufSize = env->GetArrayLength(outCodePoints);
    if (codePointBufSize != MAX_WORD_LENGTH) {
        AKLOGE("Invalid outCodePointsLength: %d", codePointBufSize);
        ASSERT(false);
        return 0;
    }
    int wordCodePoints[codePointBufSize];
    int wordCodePointCount = 0;
    const int nextToken = dictionary->getNextWordAndNextToken(token, wordCodePoints,
            &wordCodePointCount);
    JniDataUtils::outputCodePoints(env, outCodePoints, 0 /* start */,
            MAX_WORD_LENGTH /* maxLength */, wordCodePoints, wordCodePointCount,
            false /* needsNullTermination */);
    bool isBeginningOfSentence = false;
    if (wordCodePointCount > 0 && wordCodePoints[0] == CODE_POINT_BEGINNING_OF_SENTENCE) {
        isBeginningOfSentence = true;
    }
    JniDataUtils::putBooleanToArray(env, outIsBeginningOfSentence, 0 /* index */,
            isBeginningOfSentence);
    return nextToken;
}

static void latinime_BinaryDictionary_getWordProperty(JNIEnv *env, jclass clazz,
        jlong dict, jintArray word, jboolean isBeginningOfSentence, jintArray outCodePoints,
        jbooleanArray outFlags, jintArray outProbabilityInfo, jobject outNgramPrevWordsArray,
        jobject outNgramPrevWordIsBeginningOfSentenceArray, jobject outNgramTargets,
        jobject outNgramProbabilityInfo, jobject outShortcutTargets,
        jobject outShortcutProbabilities) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) return;
    const jsize wordLength = env->GetArrayLength(word);
    if (wordLength > MAX_WORD_LENGTH) {
        AKLOGE("Invalid wordLength: %d", wordLength);
        return;
    }
    int wordCodePoints[MAX_WORD_LENGTH];
    env->GetIntArrayRegion(word, 0, wordLength, wordCodePoints);
    int codePointCount = wordLength;
    if (isBeginningOfSentence) {
        codePointCount = CharUtils::attachBeginningOfSentenceMarker(
                wordCodePoints, wordLength, MAX_WORD_LENGTH);
        if (codePointCount < 0) {
            AKLOGE("Cannot attach Beginning-of-Sentence marker.");
            return;
        }
    }
    const WordProperty wordProperty = dictionary->getWordProperty(
            CodePointArrayView(wordCodePoints, codePointCount));
    JniDataUtils::outputWordProperty(env, wordProperty, outCodePoints, outFlags, outProbabilityInfo,
            outNgramPrevWordsArray, outNgramPrevWordIsBeginningOfSentenceArray,
            outNgramTargets, outNgramProbabilityInfo, outShortcutTargets, outShortcutProbabilities);
}

static bool latinime_BinaryDictionary_addUnigramEntry(JNIEnv *env, jclass clazz, jlong dict,
        jintArray word, jint probability, jintArray shortcutTarget, jint shortcutProbability,
        jboolean isBeginningOfSentence, jboolean isNotAWord, jboolean isPossiblyOffensive,
        jint timestamp) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    jsize codePointCount = env->GetArrayLength(word);
    int codePoints[codePointCount];
    env->GetIntArrayRegion(word, 0, codePointCount, codePoints);
    std::vector<UnigramProperty::ShortcutProperty> shortcuts;
    {
        std::vector<int> shortcutTargetCodePoints;
        JniDataUtils::jintarrayToVector(env, shortcutTarget, &shortcutTargetCodePoints);
        if (!shortcutTargetCodePoints.empty()) {
            shortcuts.emplace_back(std::move(shortcutTargetCodePoints), shortcutProbability);
        }
    }
    // Use 1 for count to indicate the word has inputted.
    const UnigramProperty unigramProperty(isBeginningOfSentence, isNotAWord,
            isPossiblyOffensive, probability, HistoricalInfo(timestamp, 0 /* level */,
            1 /* count */), std::move(shortcuts));
    return dictionary->addUnigramEntry(CodePointArrayView(codePoints, codePointCount),
            &unigramProperty);
}

static bool latinime_BinaryDictionary_removeUnigramEntry(JNIEnv *env, jclass clazz, jlong dict,
        jintArray word) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    jsize codePointCount = env->GetArrayLength(word);
    int codePoints[codePointCount];
    env->GetIntArrayRegion(word, 0, codePointCount, codePoints);
    return dictionary->removeUnigramEntry(CodePointArrayView(codePoints, codePointCount));
}

static bool latinime_BinaryDictionary_addNgramEntry(JNIEnv *env, jclass clazz, jlong dict,
        jobjectArray prevWordCodePointArrays, jbooleanArray isBeginningOfSentenceArray,
        jintArray word, jint probability, jint timestamp) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    const NgramContext ngramContext = JniDataUtils::constructNgramContext(env,
            prevWordCodePointArrays, isBeginningOfSentenceArray,
            env->GetArrayLength(prevWordCodePointArrays));
    jsize wordLength = env->GetArrayLength(word);
    int wordCodePoints[wordLength];
    env->GetIntArrayRegion(word, 0, wordLength, wordCodePoints);
    // Use 1 for count to indicate the ngram has inputted.
    const NgramProperty ngramProperty(ngramContext,
            CodePointArrayView(wordCodePoints, wordLength).toVector(),
            probability, HistoricalInfo(timestamp, 0 /* level */, 1 /* count */));
    return dictionary->addNgramEntry(&ngramProperty);
}

static bool latinime_BinaryDictionary_removeNgramEntry(JNIEnv *env, jclass clazz, jlong dict,
        jobjectArray prevWordCodePointArrays, jbooleanArray isBeginningOfSentenceArray,
        jintArray word) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    const NgramContext ngramContext = JniDataUtils::constructNgramContext(env,
            prevWordCodePointArrays, isBeginningOfSentenceArray,
            env->GetArrayLength(prevWordCodePointArrays));
    jsize codePointCount = env->GetArrayLength(word);
    int wordCodePoints[codePointCount];
    env->GetIntArrayRegion(word, 0, codePointCount, wordCodePoints);
    return dictionary->removeNgramEntry(&ngramContext,
            CodePointArrayView(wordCodePoints, codePointCount));
}

static bool latinime_BinaryDictionary_updateEntriesForWordWithNgramContext(JNIEnv *env,
        jclass clazz, jlong dict, jobjectArray prevWordCodePointArrays,
        jbooleanArray isBeginningOfSentenceArray, jintArray word, jboolean isValidWord, jint count,
        jint timestamp) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    const NgramContext ngramContext = JniDataUtils::constructNgramContext(env,
            prevWordCodePointArrays, isBeginningOfSentenceArray,
            env->GetArrayLength(prevWordCodePointArrays));
    jsize codePointCount = env->GetArrayLength(word);
    int wordCodePoints[codePointCount];
    env->GetIntArrayRegion(word, 0, codePointCount, wordCodePoints);
    const HistoricalInfo historicalInfo(timestamp, 0 /* level */, count);
    return dictionary->updateEntriesForWordWithNgramContext(&ngramContext,
            CodePointArrayView(wordCodePoints, codePointCount), isValidWord == JNI_TRUE,
            historicalInfo);
}

// Returns how many input events are processed.
static int latinime_BinaryDictionary_updateEntriesForInputEvents(JNIEnv *env, jclass clazz,
        jlong dict, jobjectArray inputEvents, jint startIndex) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return 0;
    }
    jsize inputEventCount = env->GetArrayLength(inputEvents);
    if (inputEventCount == 0 || startIndex >= inputEventCount) {
        return 0;
    }
    jobject inputEvent = env->GetObjectArrayElement(inputEvents, 0);
    jclass wordInputEventClass = env->GetObjectClass(inputEvent);
    env->DeleteLocalRef(inputEvent);

    jfieldID targetWordFieldId = env->GetFieldID(wordInputEventClass, "mTargetWord", "[I");
    jfieldID prevWordCountFieldId = env->GetFieldID(wordInputEventClass, "mPrevWordsCount", "I");
    jfieldID prevWordArrayFieldId = env->GetFieldID(wordInputEventClass, "mPrevWordArray", "[[I");
    jfieldID isPrevWordBoSArrayFieldId =
            env->GetFieldID(wordInputEventClass, "mIsPrevWordBeginningOfSentenceArray", "[Z");
    jfieldID isValidFieldId = env->GetFieldID(wordInputEventClass, "mIsValid", "Z");
    jfieldID timestampFieldId = env->GetFieldID(wordInputEventClass, "mTimestamp", "I");
    env->DeleteLocalRef(wordInputEventClass);

    for (int i = startIndex; i < inputEventCount; ++i) {
        jobject inputEvent = env->GetObjectArrayElement(inputEvents, i);
        jintArray targetWord = static_cast<jintArray>(
                env->GetObjectField(inputEvent, targetWordFieldId));
        jsize wordLength = env->GetArrayLength(targetWord);
        int wordCodePoints[wordLength];
        env->GetIntArrayRegion(targetWord, 0, wordLength, wordCodePoints);
        env->DeleteLocalRef(targetWord);

        jint prevWordCount = env->GetIntField(inputEvent, prevWordCountFieldId);
        jobjectArray prevWordArray =
                static_cast<jobjectArray>(env->GetObjectField(inputEvent, prevWordArrayFieldId));
        jbooleanArray isPrevWordBeginningOfSentenceArray = static_cast<jbooleanArray>(
                env->GetObjectField(inputEvent, isPrevWordBoSArrayFieldId));
        jboolean isValid = env->GetBooleanField(inputEvent, isValidFieldId);
        jint timestamp = env->GetIntField(inputEvent, timestampFieldId);
        const NgramContext ngramContext = JniDataUtils::constructNgramContext(env,
                prevWordArray, isPrevWordBeginningOfSentenceArray, prevWordCount);
        // Use 1 for count to indicate the word has inputted.
        dictionary->updateEntriesForWordWithNgramContext(&ngramContext,
                CodePointArrayView(wordCodePoints, wordLength), isValid,
                HistoricalInfo(timestamp, 0 /* level */, 1 /* count */));
        if (dictionary->needsToRunGC(true /* mindsBlockByGC */)) {
            return i + 1;
        }
        env->DeleteLocalRef(prevWordArray);
        env->DeleteLocalRef(isPrevWordBeginningOfSentenceArray);
        env->DeleteLocalRef(inputEvent);
    }
    return inputEventCount;
}

static jstring latinime_BinaryDictionary_getProperty(JNIEnv *env, jclass clazz, jlong dict,
        jstring query) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return env->NewStringUTF("");
    }
    const jsize queryUtf8Length = env->GetStringUTFLength(query);
    char queryChars[queryUtf8Length + 1];
    env->GetStringUTFRegion(query, 0, env->GetStringLength(query), queryChars);
    queryChars[queryUtf8Length] = '\0';
    static const int GET_PROPERTY_RESULT_LENGTH = 100;
    char resultChars[GET_PROPERTY_RESULT_LENGTH];
    resultChars[0] = '\0';
    dictionary->getProperty(queryChars, queryUtf8Length, resultChars, GET_PROPERTY_RESULT_LENGTH);
    return env->NewStringUTF(resultChars);
}

static bool latinime_BinaryDictionary_isCorruptedNative(JNIEnv *env, jclass clazz, jlong dict) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    return dictionary->getDictionaryStructurePolicy()->isCorrupted();
}

static DictionaryStructureWithBufferPolicy::StructurePolicyPtr runGCAndGetNewStructurePolicy(
        DictionaryStructureWithBufferPolicy::StructurePolicyPtr structurePolicy,
        const char *const dictFilePath) {
    structurePolicy->flushWithGC(dictFilePath);
    structurePolicy.release();
    return DictionaryStructureWithBufferPolicyFactory::newPolicyForExistingDictFile(
            dictFilePath, 0 /* offset */, 0 /* size */, true /* isUpdatable */);
}

static bool latinime_BinaryDictionary_migrateNative(JNIEnv *env, jclass clazz, jlong dict,
        jstring dictFilePath, jlong newFormatVersion) {
    Dictionary *dictionary = reinterpret_cast<Dictionary *>(dict);
    if (!dictionary) {
        return false;
    }
    const jsize filePathUtf8Length = env->GetStringUTFLength(dictFilePath);
    char dictFilePathChars[filePathUtf8Length + 1];
    env->GetStringUTFRegion(dictFilePath, 0, env->GetStringLength(dictFilePath), dictFilePathChars);
    dictFilePathChars[filePathUtf8Length] = '\0';

    const DictionaryHeaderStructurePolicy *const headerPolicy =
            dictionary->getDictionaryStructurePolicy()->getHeaderStructurePolicy();
    DictionaryStructureWithBufferPolicy::StructurePolicyPtr dictionaryStructureWithBufferPolicy =
            DictionaryStructureWithBufferPolicyFactory::newPolicyForOnMemoryDict(
                    newFormatVersion, *headerPolicy->getLocale(), headerPolicy->getAttributeMap());
    if (!dictionaryStructureWithBufferPolicy) {
        LogUtils::logToJava(env, "Cannot migrate header.");
        return false;
    }

    int wordCodePoints[MAX_WORD_LENGTH];
    int wordCodePointCount = 0;
    int token = 0;
    // Add unigrams.
    do {
        token = dictionary->getNextWordAndNextToken(token, wordCodePoints, &wordCodePointCount);
        const WordProperty wordProperty = dictionary->getWordProperty(
                CodePointArrayView(wordCodePoints, wordCodePointCount));
        if (wordCodePoints[0] == CODE_POINT_BEGINNING_OF_SENTENCE) {
            // Skip beginning-of-sentence unigram.
            continue;
        }
        if (dictionaryStructureWithBufferPolicy->needsToRunGC(true /* mindsBlockByGC */)) {
            dictionaryStructureWithBufferPolicy = runGCAndGetNewStructurePolicy(
                    std::move(dictionaryStructureWithBufferPolicy), dictFilePathChars);
            if (!dictionaryStructureWithBufferPolicy) {
                LogUtils::logToJava(env, "Cannot open dict after GC.");
                return false;
            }
        }
        if (!dictionaryStructureWithBufferPolicy->addUnigramEntry(
                CodePointArrayView(wordCodePoints, wordCodePointCount),
                &wordProperty.getUnigramProperty())) {
            LogUtils::logToJava(env, "Cannot add unigram to the new dict.");
            return false;
        }
    } while (token != 0);

    // Add ngrams.
    do {
        token = dictionary->getNextWordAndNextToken(token, wordCodePoints, &wordCodePointCount);
        const WordProperty wordProperty = dictionary->getWordProperty(
                CodePointArrayView(wordCodePoints, wordCodePointCount));
        if (dictionaryStructureWithBufferPolicy->needsToRunGC(true /* mindsBlockByGC */)) {
            dictionaryStructureWithBufferPolicy = runGCAndGetNewStructurePolicy(
                    std::move(dictionaryStructureWithBufferPolicy), dictFilePathChars);
            if (!dictionaryStructureWithBufferPolicy) {
                LogUtils::logToJava(env, "Cannot open dict after GC.");
                return false;
            }
        }
        for (const NgramProperty &ngramProperty : wordProperty.getNgramProperties()) {
            if (!dictionaryStructureWithBufferPolicy->addNgramEntry(&ngramProperty)) {
                LogUtils::logToJava(env, "Cannot add ngram to the new dict.");
                return false;
            }
        }
    } while (token != 0);
    // Save to File.
    dictionaryStructureWithBufferPolicy->flushWithGC(dictFilePathChars);
    return true;
}

static const JNINativeMethod sMethods[] = {
    {
        const_cast<char *>("openNative"),
        const_cast<char *>("(Ljava/lang/String;JJZ)J"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_open)
    },
    {
        const_cast<char *>("createOnMemoryNative"),
        const_cast<char *>("(JLjava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)J"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_createOnMemory)
    },
    {
        const_cast<char *>("closeNative"),
        const_cast<char *>("(J)V"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_close)
    },
    {
        const_cast<char *>("getFormatVersionNative"),
        const_cast<char *>("(J)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getFormatVersion)
    },
    {
        const_cast<char *>("getHeaderInfoNative"),
        const_cast<char *>("(J[I[ILjava/util/ArrayList;Ljava/util/ArrayList;)V"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getHeaderInfo)
    },
    {
        const_cast<char *>("flushNative"),
        const_cast<char *>("(JLjava/lang/String;)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_flush)
    },
    {
        const_cast<char *>("needsToRunGCNative"),
        const_cast<char *>("(JZ)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_needsToRunGC)
    },
    {
        const_cast<char *>("flushWithGCNative"),
        const_cast<char *>("(JLjava/lang/String;)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_flushWithGC)
    },
    {
        const_cast<char *>("getSuggestionsNative"),
        const_cast<char *>("(JJJ[I[I[I[I[II[I[[I[ZI[I[I[I[I[I[I[F)V"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getSuggestions)
    },
    {
        const_cast<char *>("getProbabilityNative"),
        const_cast<char *>("(J[I)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getProbability)
    },
    {
        const_cast<char *>("getMaxProbabilityOfExactMatchesNative"),
        const_cast<char *>("(J[I)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getMaxProbabilityOfExactMatches)
    },
    {
        const_cast<char *>("getNgramProbabilityNative"),
        const_cast<char *>("(J[[I[Z[I)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getNgramProbability)
    },
    {
        const_cast<char *>("getWordPropertyNative"),
        const_cast<char *>("(J[IZ[I[Z[ILjava/util/ArrayList;Ljava/util/ArrayList;"
                "Ljava/util/ArrayList;Ljava/util/ArrayList;Ljava/util/ArrayList;"
                "Ljava/util/ArrayList;)V"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getWordProperty)
    },
    {
        const_cast<char *>("getNextWordNative"),
        const_cast<char *>("(JI[I[Z)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getNextWord)
    },
    {
        const_cast<char *>("addUnigramEntryNative"),
        const_cast<char *>("(J[II[IIZZZI)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_addUnigramEntry)
    },
    {
        const_cast<char *>("removeUnigramEntryNative"),
        const_cast<char *>("(J[I)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_removeUnigramEntry)
    },
    {
        const_cast<char *>("addNgramEntryNative"),
        const_cast<char *>("(J[[I[Z[III)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_addNgramEntry)
    },
    {
        const_cast<char *>("removeNgramEntryNative"),
        const_cast<char *>("(J[[I[Z[I)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_removeNgramEntry)
    },
    {
        const_cast<char *>("updateEntriesForWordWithNgramContextNative"),
        const_cast<char *>("(J[[I[Z[IZII)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_updateEntriesForWordWithNgramContext)
    },
    {
        const_cast<char *>("updateEntriesForInputEventsNative"),
        const_cast<char *>(
                "(J[Lcom/android/inputmethod/latin/utils/WordInputEventForPersonalization;I)I"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_updateEntriesForInputEvents)
    },
    {
        const_cast<char *>("getPropertyNative"),
        const_cast<char *>("(JLjava/lang/String;)Ljava/lang/String;"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_getProperty)
    },
    {
        const_cast<char *>("isCorruptedNative"),
        const_cast<char *>("(J)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_isCorruptedNative)
    },
    {
        const_cast<char *>("migrateNative"),
        const_cast<char *>("(JLjava/lang/String;J)Z"),
        reinterpret_cast<void *>(latinime_BinaryDictionary_migrateNative)
    }
};

int register_BinaryDictionary(JNIEnv *env) {
    const char *const kClassPathName = "com/android/inputmethod/latin/BinaryDictionary";
    return registerNativeMethods(env, kClassPathName, sMethods, NELEMS(sMethods));
}
} // namespace latinime
