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

#include "dictionary/utils/multi_bigram_map.h"

#include <cstddef>
#include <unordered_map>

namespace latinime {

// Max number of bigram maps (previous word contexts) to be cached. Increasing this number
// could improve bigram lookup speed for multi-word suggestions, but at the cost of more memory
// usage. Also, there are diminishing returns since the most frequently used bigrams are
// typically near the beginning of the input and are thus the first ones to be cached. Note
// that these bigrams are reset for each new composing word.
const size_t MultiBigramMap::MAX_CACHED_PREV_WORDS_IN_BIGRAM_MAP = 25;

// Most common previous word contexts currently have 100 bigrams
const int MultiBigramMap::BigramMap::DEFAULT_HASH_MAP_SIZE_FOR_EACH_BIGRAM_MAP = 100;

// Look up the bigram probability for the given word pair from the cached bigram maps.
// Also caches the bigrams if there is space remaining and they have not been cached already.
int MultiBigramMap::getBigramProbability(
        const DictionaryStructureWithBufferPolicy *const structurePolicy,
        const WordIdArrayView prevWordIds, const int nextWordId,
        const int unigramProbability) {
    if (prevWordIds.empty() || prevWordIds[0] == NOT_A_WORD_ID) {
        return structurePolicy->getProbability(unigramProbability, NOT_A_PROBABILITY);
    }
    const auto mapPosition = mBigramMaps.find(prevWordIds[0]);
    if (mapPosition != mBigramMaps.end()) {
        return mapPosition->second.getBigramProbability(structurePolicy, nextWordId,
                unigramProbability);
    }
    if (mBigramMaps.size() < MAX_CACHED_PREV_WORDS_IN_BIGRAM_MAP) {
        addBigramsForWord(structurePolicy, prevWordIds);
        return mBigramMaps[prevWordIds[0]].getBigramProbability(structurePolicy,
                nextWordId, unigramProbability);
    }
    return readBigramProbabilityFromBinaryDictionary(structurePolicy, prevWordIds,
            nextWordId, unigramProbability);
}

void MultiBigramMap::BigramMap::init(
        const DictionaryStructureWithBufferPolicy *const structurePolicy,
        const WordIdArrayView prevWordIds) {
    structurePolicy->iterateNgramEntries(prevWordIds, this /* listener */);
}

int MultiBigramMap::BigramMap::getBigramProbability(
        const DictionaryStructureWithBufferPolicy *const structurePolicy,
        const int nextWordId, const int unigramProbability) const {
    int bigramProbability = NOT_A_PROBABILITY;
    if (mBloomFilter.isInFilter(nextWordId)) {
        const auto bigramProbabilityIt = mBigramMap.find(nextWordId);
        if (bigramProbabilityIt != mBigramMap.end()) {
            bigramProbability = bigramProbabilityIt->second;
        }
    }
    return structurePolicy->getProbability(unigramProbability, bigramProbability);
}

void MultiBigramMap::BigramMap::onVisitEntry(const int ngramProbability, const int targetWordId) {
    if (targetWordId == NOT_A_WORD_ID) {
        return;
    }
    mBigramMap[targetWordId] = ngramProbability;
    mBloomFilter.setInFilter(targetWordId);
}

void MultiBigramMap::addBigramsForWord(
        const DictionaryStructureWithBufferPolicy *const structurePolicy,
        const WordIdArrayView prevWordIds) {
    mBigramMaps[prevWordIds[0]].init(structurePolicy, prevWordIds);
}

int MultiBigramMap::readBigramProbabilityFromBinaryDictionary(
        const DictionaryStructureWithBufferPolicy *const structurePolicy,
        const WordIdArrayView prevWordIds, const int nextWordId, const int unigramProbability) {
    const int bigramProbability = structurePolicy->getProbabilityOfWord(prevWordIds, nextWordId);
    if (bigramProbability != NOT_A_PROBABILITY) {
        return bigramProbability;
    }
    return structurePolicy->getProbability(unigramProbability, NOT_A_PROBABILITY);
}

} // namespace latinime
