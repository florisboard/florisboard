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

#ifndef LATINIME_FORGETTING_CURVE_UTILS_H
#define LATINIME_FORGETTING_CURVE_UTILS_H

#include <vector>

#include "defines.h"
#include "dictionary/property/historical_info.h"
#include "dictionary/utils/entry_counters.h"

namespace latinime {

class HeaderPolicy;

class ForgettingCurveUtils {
 public:
    static const HistoricalInfo createUpdatedHistoricalInfo(
            const HistoricalInfo *const originalHistoricalInfo, const int newProbability,
            const HistoricalInfo *const newHistoricalInfo, const HeaderPolicy *const headerPolicy);

    static const HistoricalInfo createHistoricalInfoToSave(
            const HistoricalInfo *const originalHistoricalInfo,
            const HeaderPolicy *const headerPolicy);

    static int decodeProbability(const HistoricalInfo *const historicalInfo,
            const HeaderPolicy *const headerPolicy);

    static bool needsToKeep(const HistoricalInfo *const historicalInfo,
            const HeaderPolicy *const headerPolicy);

    static bool needsToDecay(const bool mindsBlockByDecay, const EntryCounts &entryCounters,
            const HeaderPolicy *const headerPolicy);

    // TODO: Improve probability computation method and remove this.
    static int getProbabilityBiasForNgram(const int n) {
        return (n - 1) * MULTIPLIER_TWO_IN_PROBABILITY_SCALE;
    }

    AK_FORCE_INLINE static int getEntryCountHardLimit(const int maxEntryCount) {
        return static_cast<int>(static_cast<float>(maxEntryCount)
                * ENTRY_COUNT_HARD_LIMIT_WEIGHT);
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(ForgettingCurveUtils);

    class ProbabilityTable {
     public:
        ProbabilityTable();

        int getProbability(const int tableId, const int level,
                const int elapsedTimeStepCount) const {
            return mTables[tableId][level][elapsedTimeStepCount];
        }

     private:
        DISALLOW_COPY_AND_ASSIGN(ProbabilityTable);

        static const int PROBABILITY_TABLE_COUNT;
        static const int WEAK_PROBABILITY_TABLE_ID;
        static const int MODEST_PROBABILITY_TABLE_ID;
        static const int STRONG_PROBABILITY_TABLE_ID;
        static const int AGGRESSIVE_PROBABILITY_TABLE_ID;

        static const int WEAK_MAX_PROBABILITY;
        static const int MODEST_BASE_PROBABILITY;
        static const int STRONG_BASE_PROBABILITY;
        static const int AGGRESSIVE_BASE_PROBABILITY;

        std::vector<std::vector<std::vector<int>>> mTables;

        static int getBaseProbabilityForLevel(const int tableId, const int level);
    };

    static const int MULTIPLIER_TWO_IN_PROBABILITY_SCALE;
    static const int DECAY_INTERVAL_SECONDS;

    static const int MAX_LEVEL;
    static const int MIN_VISIBLE_LEVEL;
    static const int MAX_ELAPSED_TIME_STEP_COUNT;
    static const int DISCARD_LEVEL_ZERO_ENTRY_TIME_STEP_COUNT_THRESHOLD;
    static const int OCCURRENCES_TO_RAISE_THE_LEVEL;
    static const int DURATION_TO_LOWER_THE_LEVEL_IN_SECONDS;

    static const float ENTRY_COUNT_HARD_LIMIT_WEIGHT;

    static const ProbabilityTable sProbabilityTable;

    static int backoff(const int unigramProbability);
    static int getElapsedTimeStepCount(const int timestamp, const int durationToLevelDown);
    static int clampToVisibleEntryLevelRange(const int level);
    static int clampToValidLevelRange(const int level);
    static int clampToValidCountRange(const int count, const HeaderPolicy *const headerPolicy);
    static int clampToValidTimeStepCountRange(const int timeStepCount);
};
} // namespace latinime
#endif /* LATINIME_FORGETTING_CURVE_UTILS_H */
