/*
 * Copyright (C) 2012 The Android Open Source Project
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

#include "suggest/core/session/dic_traverse_session.h"

#include "defines.h"
#include "dictionary/interface/dictionary_header_structure_policy.h"
#include "dictionary/interface/dictionary_structure_with_buffer_policy.h"
#include "dictionary/property/ngram_context.h"
#include "suggest/core/dictionary/dictionary.h"

namespace latinime {

// 256K bytes threshold is heuristically used to distinguish dictionaries containing many unigrams
// (e.g. main dictionary) from small dictionaries (e.g. contacts...)
const int DicTraverseSession::DICTIONARY_SIZE_THRESHOLD_TO_USE_LARGE_CACHE_FOR_SUGGESTION =
        256 * 1024;

void DicTraverseSession::init(const Dictionary *const dictionary,
        const NgramContext *const ngramContext, const SuggestOptions *const suggestOptions) {
    mDictionary = dictionary;
    mMultiWordCostMultiplier = getDictionaryStructurePolicy()->getHeaderStructurePolicy()
            ->getMultiWordCostMultiplier();
    mSuggestOptions = suggestOptions;
    mPrevWordIdCount = ngramContext->getPrevWordIds(getDictionaryStructurePolicy(),
            &mPrevWordIdArray, true /* tryLowerCaseSearch */).size();
}

void DicTraverseSession::setupForGetSuggestions(const ProximityInfo *pInfo,
        const int *inputCodePoints, const int inputSize, const int *const inputXs,
        const int *const inputYs, const int *const times, const int *const pointerIds,
        const float maxSpatialDistance, const int maxPointerCount) {
    mProximityInfo = pInfo;
    mMaxPointerCount = maxPointerCount;
    initializeProximityInfoStates(inputCodePoints, inputXs, inputYs, times, pointerIds, inputSize,
            maxSpatialDistance, maxPointerCount);
}

const DictionaryStructureWithBufferPolicy *DicTraverseSession::getDictionaryStructurePolicy()
        const {
    return mDictionary->getDictionaryStructurePolicy();
}

void DicTraverseSession::resetCache(const int thresholdForNextActiveDicNodes, const int maxWords) {
    mDicNodesCache.reset(thresholdForNextActiveDicNodes /* nextActiveSize */,
            maxWords /* terminalSize */);
    mMultiBigramMap.clear();
}

void DicTraverseSession::initializeProximityInfoStates(const int *const inputCodePoints,
        const int *const inputXs, const int *const inputYs, const int *const times,
        const int *const pointerIds, const int inputSize, const float maxSpatialDistance,
        const int maxPointerCount) {
    ASSERT(1 <= maxPointerCount && maxPointerCount <= MAX_POINTER_COUNT_G);
    mInputSize = 0;
    for (int i = 0; i < maxPointerCount; ++i) {
        mProximityInfoStates[i].initInputParams(i, maxSpatialDistance, getProximityInfo(),
                inputCodePoints, inputSize, inputXs, inputYs, times, pointerIds,
                // Right now the line below is trying to figure out whether this is a gesture by
                // looking at the pointer count and assuming whatever is above the cutoff is
                // a gesture and whatever is below is type. This is hacky and incorrect, we
                // should pass the correct information instead.
                maxPointerCount == MAX_POINTER_COUNT_G,
                getDictionaryStructurePolicy()->getHeaderStructurePolicy()->getLocale());
        mInputSize += mProximityInfoStates[i].size();
    }
}
} // namespace latinime
