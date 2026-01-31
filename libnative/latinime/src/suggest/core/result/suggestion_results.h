/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef LATINIME_SUGGESTION_RESULTS_H
#define LATINIME_SUGGESTION_RESULTS_H

#include <queue>
#include <vector>

#include "defines.h"
#include "jni.h"
#include "suggest/core/result/suggested_word.h"

namespace latinime {

class SuggestionResults {
 public:
    explicit SuggestionResults(const int maxSuggestionCount)
            : mMaxSuggestionCount(maxSuggestionCount),
              mWeightOfLangModelVsSpatialModel(NOT_A_WEIGHT_OF_LANG_MODEL_VS_SPATIAL_MODEL),
              mSuggestedWords() {}

    // Returns suggestion count.
    void outputSuggestions(JNIEnv *env, jintArray outSuggestionCount, jintArray outCodePointsArray,
            jintArray outScoresArray, jintArray outSpaceIndicesArray, jintArray outTypesArray,
            jintArray outAutoCommitFirstWordConfidenceArray,
            jfloatArray outWeightOfLangModelVsSpatialModel);
    void addPrediction(const int *const codePoints, const int codePointCount, const int score);
    void addSuggestion(const int *const codePoints, const int codePointCount,
            const int score, const int type, const int indexToPartialCommit,
            const int autocimmitFirstWordConfindence);
    void getSortedScores(int *const outScores) const;
    void dumpSuggestions() const;

    void setWeightOfLangModelVsSpatialModel(const float weightOfLangModelVsSpatialModel) {
        mWeightOfLangModelVsSpatialModel = weightOfLangModelVsSpatialModel;
    }

    int getSuggestionCount() const {
        return mSuggestedWords.size();
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(SuggestionResults);

    const int mMaxSuggestionCount;
    float mWeightOfLangModelVsSpatialModel;
    std::priority_queue<
            SuggestedWord, std::vector<SuggestedWord>, SuggestedWord::Comparator> mSuggestedWords;
};
} // namespace latinime
#endif // LATINIME_SUGGESTION_RESULTS_H
