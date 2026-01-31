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

#ifndef LATINIME_NGRAM_PROPERTY_H
#define LATINIME_NGRAM_PROPERTY_H

#include <vector>

#include "defines.h"
#include "dictionary/property/historical_info.h"
#include "dictionary/property/ngram_context.h"

namespace latinime {

class NgramProperty {
 public:
    NgramProperty(const NgramContext &ngramContext, const std::vector<int> &&targetCodePoints,
            const int probability, const HistoricalInfo historicalInfo)
            : mNgramContext(ngramContext), mTargetCodePoints(std::move(targetCodePoints)),
              mProbability(probability), mHistoricalInfo(historicalInfo) {}

    const NgramContext *getNgramContext() const {
        return &mNgramContext;
    }

    const std::vector<int> *getTargetCodePoints() const {
        return &mTargetCodePoints;
    }

    int getProbability() const {
        return mProbability;
    }

    const HistoricalInfo getHistoricalInfo() const {
        return mHistoricalInfo;
    }

 private:
    // Default copy constructor is used for using in std::vector.
    DISALLOW_DEFAULT_CONSTRUCTOR(NgramProperty);
    DISALLOW_ASSIGNMENT_OPERATOR(NgramProperty);

    const NgramContext mNgramContext;
    const std::vector<int> mTargetCodePoints;
    const int mProbability;
    const HistoricalInfo mHistoricalInfo;
};
} // namespace latinime
#endif // LATINIME_NGRAM_PROPERTY_H
