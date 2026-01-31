/*
 * Copyright (C) 2009, The Android Open Source Project
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

#define LOG_TAG "LatinIME: dictionary.cpp"

#include "suggest/core/dictionary/dictionary.h"

#include "defines.h"
#include "dictionary/interface/dictionary_header_structure_policy.h"
#include "dictionary/property/ngram_context.h"
#include "suggest/core/dictionary/dictionary_utils.h"
#include "suggest/core/result/suggestion_results.h"
#include "suggest/core/session/dic_traverse_session.h"
#include "suggest/core/suggest.h"
#include "suggest/core/suggest_options.h"
#include "suggest/policyimpl/gesture/gesture_suggest_policy_factory.h"
#include "suggest/policyimpl/typing/typing_suggest_policy_factory.h"
#include "utils/int_array_view.h"
#include "utils/log_utils.h"
#include "utils/time_keeper.h"

namespace latinime {

const int Dictionary::HEADER_ATTRIBUTE_BUFFER_SIZE = 32;

Dictionary::Dictionary(JNIEnv *env, DictionaryStructureWithBufferPolicy::StructurePolicyPtr
        dictionaryStructureWithBufferPolicy)
        : mDictionaryStructureWithBufferPolicy(std::move(dictionaryStructureWithBufferPolicy)),
          mGestureSuggest(new Suggest(GestureSuggestPolicyFactory::getGestureSuggestPolicy())),
          mTypingSuggest(new Suggest(TypingSuggestPolicyFactory::getTypingSuggestPolicy())) {
    logDictionaryInfo(env);
}

void Dictionary::getSuggestions(ProximityInfo *proximityInfo, DicTraverseSession *traverseSession,
        int *xcoordinates, int *ycoordinates, int *times, int *pointerIds, int *inputCodePoints,
        int inputSize, const NgramContext *const ngramContext,
        const SuggestOptions *const suggestOptions, const float weightOfLangModelVsSpatialModel,
        SuggestionResults *const outSuggestionResults) const {
    TimeKeeper::setCurrentTime();
    traverseSession->init(this, ngramContext, suggestOptions);
    const auto &suggest = suggestOptions->isGesture() ? mGestureSuggest : mTypingSuggest;
    suggest->getSuggestions(proximityInfo, traverseSession, xcoordinates,
            ycoordinates, times, pointerIds, inputCodePoints, inputSize,
            weightOfLangModelVsSpatialModel, outSuggestionResults);
}

Dictionary::NgramListenerForPrediction::NgramListenerForPrediction(
        const NgramContext *const ngramContext, const WordIdArrayView prevWordIds,
        SuggestionResults *const suggestionResults,
        const DictionaryStructureWithBufferPolicy *const dictStructurePolicy)
    : mNgramContext(ngramContext), mPrevWordIds(prevWordIds),
      mSuggestionResults(suggestionResults), mDictStructurePolicy(dictStructurePolicy) {}

void Dictionary::NgramListenerForPrediction::onVisitEntry(const int ngramProbability,
        const int targetWordId) {
    if (targetWordId == NOT_A_WORD_ID) {
        return;
    }
    if (mNgramContext->isNthPrevWordBeginningOfSentence(1 /* n */)
            && ngramProbability == NOT_A_PROBABILITY) {
        return;
    }
    int targetWordCodePoints[MAX_WORD_LENGTH];
    const int codePointCount = mDictStructurePolicy->getCodePointsAndReturnCodePointCount(
            targetWordId, MAX_WORD_LENGTH, targetWordCodePoints);
    if (codePointCount <= 0) {
        return;
    }
    const WordAttributes wordAttributes = mDictStructurePolicy->getWordAttributesInContext(
            mPrevWordIds, targetWordId, nullptr /* multiBigramMap */);
    if (wordAttributes.getProbability() == NOT_A_PROBABILITY) {
        return;
    }
    mSuggestionResults->addPrediction(targetWordCodePoints, codePointCount,
            wordAttributes.getProbability());
}

void Dictionary::getPredictions(const NgramContext *const ngramContext,
        SuggestionResults *const outSuggestionResults) const {
    TimeKeeper::setCurrentTime();
    WordIdArray<MAX_PREV_WORD_COUNT_FOR_N_GRAM> prevWordIdArray;
    const WordIdArrayView prevWordIds = ngramContext->getPrevWordIds(
            mDictionaryStructureWithBufferPolicy.get(), &prevWordIdArray,
            true /* tryLowerCaseSearch */);
    NgramListenerForPrediction listener(ngramContext, prevWordIds, outSuggestionResults,
            mDictionaryStructureWithBufferPolicy.get());
    mDictionaryStructureWithBufferPolicy->iterateNgramEntries(prevWordIds, &listener);
}

int Dictionary::getProbability(const CodePointArrayView codePoints) const {
    return getNgramProbability(nullptr /* ngramContext */, codePoints);
}

int Dictionary::getMaxProbabilityOfExactMatches(const CodePointArrayView codePoints) const {
    TimeKeeper::setCurrentTime();
    return DictionaryUtils::getMaxProbabilityOfExactMatches(
            mDictionaryStructureWithBufferPolicy.get(), codePoints);
}

int Dictionary::getNgramProbability(const NgramContext *const ngramContext,
        const CodePointArrayView codePoints) const {
    TimeKeeper::setCurrentTime();
    const int wordId = mDictionaryStructureWithBufferPolicy->getWordId(codePoints,
            false /* forceLowerCaseSearch */);
    if (wordId == NOT_A_WORD_ID) return NOT_A_PROBABILITY;
    if (!ngramContext) {
        return getDictionaryStructurePolicy()->getProbabilityOfWord(WordIdArrayView(), wordId);
    }
    WordIdArray<MAX_PREV_WORD_COUNT_FOR_N_GRAM> prevWordIdArray;
    const WordIdArrayView prevWordIds = ngramContext->getPrevWordIds(
            mDictionaryStructureWithBufferPolicy.get(), &prevWordIdArray,
            true /* tryLowerCaseSearch */);
    return getDictionaryStructurePolicy()->getProbabilityOfWord(prevWordIds, wordId);
}

bool Dictionary::addUnigramEntry(const CodePointArrayView codePoints,
        const UnigramProperty *const unigramProperty) {
    if (unigramProperty->representsBeginningOfSentence()
            && !mDictionaryStructureWithBufferPolicy->getHeaderStructurePolicy()
                    ->supportsBeginningOfSentence()) {
        AKLOGE("The dictionary doesn't support Beginning-of-Sentence.");
        return false;
    }
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->addUnigramEntry(codePoints, unigramProperty);
}

bool Dictionary::removeUnigramEntry(const CodePointArrayView codePoints) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->removeUnigramEntry(codePoints);
}

bool Dictionary::addNgramEntry(const NgramProperty *const ngramProperty) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->addNgramEntry(ngramProperty);
}

bool Dictionary::removeNgramEntry(const NgramContext *const ngramContext,
        const CodePointArrayView codePoints) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->removeNgramEntry(ngramContext, codePoints);
}

bool Dictionary::updateEntriesForWordWithNgramContext(const NgramContext *const ngramContext,
        const CodePointArrayView codePoints, const bool isValidWord,
        const HistoricalInfo historicalInfo) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->updateEntriesForWordWithNgramContext(ngramContext,
            codePoints, isValidWord, historicalInfo);
}

bool Dictionary::flush(const char *const filePath) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->flush(filePath);
}

bool Dictionary::flushWithGC(const char *const filePath) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->flushWithGC(filePath);
}

bool Dictionary::needsToRunGC(const bool mindsBlockByGC) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->needsToRunGC(mindsBlockByGC);
}

void Dictionary::getProperty(const char *const query, const int queryLength, char *const outResult,
        const int maxResultLength) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->getProperty(query, queryLength, outResult,
            maxResultLength);
}

const WordProperty Dictionary::getWordProperty(const CodePointArrayView codePoints) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->getWordProperty(codePoints);
}

int Dictionary::getNextWordAndNextToken(const int token, int *const outCodePoints,
        int *const outCodePointCount) {
    TimeKeeper::setCurrentTime();
    return mDictionaryStructureWithBufferPolicy->getNextWordAndNextToken(
            token, outCodePoints, outCodePointCount);
}

void Dictionary::logDictionaryInfo(JNIEnv *const env) const {
    int dictionaryIdCodePointBuffer[HEADER_ATTRIBUTE_BUFFER_SIZE];
    int versionStringCodePointBuffer[HEADER_ATTRIBUTE_BUFFER_SIZE];
    int dateStringCodePointBuffer[HEADER_ATTRIBUTE_BUFFER_SIZE];
    const DictionaryHeaderStructurePolicy *const headerPolicy =
            getDictionaryStructurePolicy()->getHeaderStructurePolicy();
    headerPolicy->readHeaderValueOrQuestionMark("dictionary", dictionaryIdCodePointBuffer,
            HEADER_ATTRIBUTE_BUFFER_SIZE);
    headerPolicy->readHeaderValueOrQuestionMark("version", versionStringCodePointBuffer,
            HEADER_ATTRIBUTE_BUFFER_SIZE);
    headerPolicy->readHeaderValueOrQuestionMark("date", dateStringCodePointBuffer,
            HEADER_ATTRIBUTE_BUFFER_SIZE);

    char dictionaryIdCharBuffer[HEADER_ATTRIBUTE_BUFFER_SIZE];
    char versionStringCharBuffer[HEADER_ATTRIBUTE_BUFFER_SIZE];
    char dateStringCharBuffer[HEADER_ATTRIBUTE_BUFFER_SIZE];
    intArrayToCharArray(dictionaryIdCodePointBuffer, HEADER_ATTRIBUTE_BUFFER_SIZE,
            dictionaryIdCharBuffer, HEADER_ATTRIBUTE_BUFFER_SIZE);
    intArrayToCharArray(versionStringCodePointBuffer, HEADER_ATTRIBUTE_BUFFER_SIZE,
            versionStringCharBuffer, HEADER_ATTRIBUTE_BUFFER_SIZE);
    intArrayToCharArray(dateStringCodePointBuffer, HEADER_ATTRIBUTE_BUFFER_SIZE,
            dateStringCharBuffer, HEADER_ATTRIBUTE_BUFFER_SIZE);

    LogUtils::logToJava(env,
            "Dictionary info: dictionary = %s ; version = %s ; date = %s",
            dictionaryIdCharBuffer, versionStringCharBuffer, dateStringCharBuffer);
}

} // namespace latinime
