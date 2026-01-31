/*
 * Copyright (C) 2014, The Android Open Source Project
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

#ifndef LATINIME_DYNAMIC_LANGUAGE_MODEL_PROBABILITY_UTILS_H
#define LATINIME_DYNAMIC_LANGUAGE_MODEL_PROBABILITY_UTILS_H

#include <algorithm>

#include "defines.h"
#include "dictionary/property/historical_info.h"
#include "utils/ngram_utils.h"
#include "utils/time_keeper.h"

namespace latinime {

class DynamicLanguageModelProbabilityUtils {
 public:
    static float computeRawProbabilityFromCounts(const int count, const int contextCount,
            const NgramType ngramType) {
        const int minCount = ASSUMED_MIN_COUNTS[static_cast<int>(ngramType)];
        return static_cast<float>(count) / static_cast<float>(std::max(contextCount, minCount));
    }

    static float backoff(const int ngramProbability, const NgramType ngramType) {
        const int probability =
                ngramProbability + ENCODED_BACKOFF_WEIGHTS[static_cast<int>(ngramType)];
        return std::min(std::max(probability, NOT_A_PROBABILITY), MAX_PROBABILITY);
    }

    static int getDecayedProbability(const int probability, const HistoricalInfo historicalInfo) {
        const int elapsedTime = TimeKeeper::peekCurrentTime() - historicalInfo.getTimestamp();
        if (elapsedTime < 0) {
            AKLOGE("The elapsed time is negatime value. Timestamp overflow?");
            return NOT_A_PROBABILITY;
        }
        // TODO: Improve this logic.
        // We don't modify probability depending on the elapsed time.
        return probability;
    }

    static int shouldRemoveEntryDuringGC(const HistoricalInfo historicalInfo) {
        // TODO: Improve this logic.
        const int elapsedTime = TimeKeeper::peekCurrentTime() - historicalInfo.getTimestamp();
        return elapsedTime > DURATION_TO_DISCARD_ENTRY_IN_SECONDS;
    }

    static int getPriorityToPreventFromEviction(const HistoricalInfo historicalInfo) {
        // TODO: Improve this logic.
        // More recently input entries get higher priority.
        return historicalInfo.getTimestamp();
    }

private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(DynamicLanguageModelProbabilityUtils);

    static_assert(MAX_PREV_WORD_COUNT_FOR_N_GRAM <= 3, "Max supported Ngram is Quadgram.");

    static const int ASSUMED_MIN_COUNTS[];
    static const int ENCODED_BACKOFF_WEIGHTS[];
    static const int DURATION_TO_DISCARD_ENTRY_IN_SECONDS;
};

} // namespace latinime
#endif /* LATINIME_DYNAMIC_LANGUAGE_MODEL_PROBABILITY_UTILS_H */
