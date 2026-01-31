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

#include "dictionary/utils/forgetting_curve_utils.h"

#include <algorithm>
#include <cmath>
#include <stdlib.h>

#include "dictionary/header/header_policy.h"
#include "dictionary/utils/probability_utils.h"
#include "utils/time_keeper.h"

namespace latinime {

const int ForgettingCurveUtils::MULTIPLIER_TWO_IN_PROBABILITY_SCALE = 8;
const int ForgettingCurveUtils::DECAY_INTERVAL_SECONDS = 2 * 60 * 60;

const int ForgettingCurveUtils::MAX_LEVEL = 15;
const int ForgettingCurveUtils::MIN_VISIBLE_LEVEL = 2;
const int ForgettingCurveUtils::MAX_ELAPSED_TIME_STEP_COUNT = 31;
const int ForgettingCurveUtils::DISCARD_LEVEL_ZERO_ENTRY_TIME_STEP_COUNT_THRESHOLD = 30;
const int ForgettingCurveUtils::OCCURRENCES_TO_RAISE_THE_LEVEL = 1;
// TODO: Evaluate whether this should be 7.5 days.
// 15 days
const int ForgettingCurveUtils::DURATION_TO_LOWER_THE_LEVEL_IN_SECONDS = 15 * 24 * 60 * 60;

const float ForgettingCurveUtils::ENTRY_COUNT_HARD_LIMIT_WEIGHT = 1.2;

const ForgettingCurveUtils::ProbabilityTable ForgettingCurveUtils::sProbabilityTable;

// TODO: Revise the logic to decide the initial probability depending on the given probability.
/* static */ const HistoricalInfo ForgettingCurveUtils::createUpdatedHistoricalInfo(
        const HistoricalInfo *const originalHistoricalInfo, const int newProbability,
        const HistoricalInfo *const newHistoricalInfo, const HeaderPolicy *const headerPolicy) {
    const int timestamp = newHistoricalInfo->getTimestamp();
    if (newProbability != NOT_A_PROBABILITY && originalHistoricalInfo->getLevel() == 0) {
        // Add entry as a valid word.
        const int level = clampToVisibleEntryLevelRange(newHistoricalInfo->getLevel());
        const int count = clampToValidCountRange(newHistoricalInfo->getCount(), headerPolicy);
        return HistoricalInfo(timestamp, level, count);
    } else if (!originalHistoricalInfo->isValid()
            || originalHistoricalInfo->getLevel() < newHistoricalInfo->getLevel()
            || (originalHistoricalInfo->getLevel() == newHistoricalInfo->getLevel()
                    && originalHistoricalInfo->getCount() < newHistoricalInfo->getCount())) {
        // Initial information.
        int count = newHistoricalInfo->getCount();
        if (count >= OCCURRENCES_TO_RAISE_THE_LEVEL) {
            const int level = clampToValidLevelRange(newHistoricalInfo->getLevel() + 1);
            return HistoricalInfo(timestamp, level, 0 /* count */);
        }
        const int level = clampToValidLevelRange(newHistoricalInfo->getLevel());
        return HistoricalInfo(timestamp, level, clampToValidCountRange(count, headerPolicy));
    } else {
        const int updatedCount = originalHistoricalInfo->getCount() + 1;
        if (updatedCount >= OCCURRENCES_TO_RAISE_THE_LEVEL) {
            // The count exceeds the max value the level can be incremented.
            if (originalHistoricalInfo->getLevel() >= MAX_LEVEL) {
                // The level is already max.
                return HistoricalInfo(timestamp,
                        originalHistoricalInfo->getLevel(), originalHistoricalInfo->getCount());
            } else {
                // Raise the level.
                return HistoricalInfo(timestamp,
                        originalHistoricalInfo->getLevel() + 1, 0 /* count */);
            }
        } else {
            return HistoricalInfo(timestamp, originalHistoricalInfo->getLevel(), updatedCount);
        }
    }
}

/* static */ int ForgettingCurveUtils::decodeProbability(
        const HistoricalInfo *const historicalInfo, const HeaderPolicy *const headerPolicy) {
    const int elapsedTimeStepCount = getElapsedTimeStepCount(historicalInfo->getTimestamp(),
            DURATION_TO_LOWER_THE_LEVEL_IN_SECONDS);
    return sProbabilityTable.getProbability(
            headerPolicy->getForgettingCurveProbabilityValuesTableId(),
            clampToValidLevelRange(historicalInfo->getLevel()),
            clampToValidTimeStepCountRange(elapsedTimeStepCount));
}

/* static */ bool ForgettingCurveUtils::needsToKeep(const HistoricalInfo *const historicalInfo,
        const HeaderPolicy *const headerPolicy) {
    return historicalInfo->getLevel() > 0
            || getElapsedTimeStepCount(historicalInfo->getTimestamp(),
                    DURATION_TO_LOWER_THE_LEVEL_IN_SECONDS)
                            < DISCARD_LEVEL_ZERO_ENTRY_TIME_STEP_COUNT_THRESHOLD;
}

/* static */ const HistoricalInfo ForgettingCurveUtils::createHistoricalInfoToSave(
        const HistoricalInfo *const originalHistoricalInfo,
        const HeaderPolicy *const headerPolicy) {
    if (originalHistoricalInfo->getTimestamp() == NOT_A_TIMESTAMP) {
        return HistoricalInfo();
    }
    const int durationToLevelDownInSeconds = DURATION_TO_LOWER_THE_LEVEL_IN_SECONDS;
    const int elapsedTimeStep = getElapsedTimeStepCount(
            originalHistoricalInfo->getTimestamp(), durationToLevelDownInSeconds);
    if (elapsedTimeStep <= MAX_ELAPSED_TIME_STEP_COUNT) {
        // No need to update historical info.
        return *originalHistoricalInfo;
    }
    // Lower the level.
    const int maxLevelDownAmonut = elapsedTimeStep / (MAX_ELAPSED_TIME_STEP_COUNT + 1);
    const int levelDownAmount = (maxLevelDownAmonut >= originalHistoricalInfo->getLevel()) ?
            originalHistoricalInfo->getLevel() : maxLevelDownAmonut;
    const int adjustedTimestampInSeconds = originalHistoricalInfo->getTimestamp() +
            levelDownAmount * durationToLevelDownInSeconds;
    return HistoricalInfo(adjustedTimestampInSeconds,
            originalHistoricalInfo->getLevel() - levelDownAmount, 0 /* count */);
}

/* static */ bool ForgettingCurveUtils::needsToDecay(const bool mindsBlockByDecay,
        const EntryCounts &entryCounts, const HeaderPolicy *const headerPolicy) {
    const EntryCounts &maxNgramCounts = headerPolicy->getMaxNgramCounts();
    for (const auto ngramType : AllNgramTypes::ASCENDING) {
        if (entryCounts.getNgramCount(ngramType)
                >= getEntryCountHardLimit(maxNgramCounts.getNgramCount(ngramType))) {
            // Unigram count exceeds the limit.
            return true;
        }
    }
    if (mindsBlockByDecay) {
        return false;
    }
    if (headerPolicy->getLastDecayedTime() + DECAY_INTERVAL_SECONDS
            < TimeKeeper::peekCurrentTime()) {
        // Time to decay.
        return true;
    }
    return false;
}

// See comments in ProbabilityUtils::backoff().
/* static */ int ForgettingCurveUtils::backoff(const int unigramProbability) {
    // See TODO comments in ForgettingCurveUtils::getProbability().
    return unigramProbability;
}

/* static */ int ForgettingCurveUtils::getElapsedTimeStepCount(const int timestamp,
        const int durationToLevelDownInSeconds) {
    const int elapsedTimeInSeconds = TimeKeeper::peekCurrentTime() - timestamp;
    const int timeStepDurationInSeconds =
            durationToLevelDownInSeconds / (MAX_ELAPSED_TIME_STEP_COUNT + 1);
    return elapsedTimeInSeconds / timeStepDurationInSeconds;
}

/* static */ int ForgettingCurveUtils::clampToVisibleEntryLevelRange(const int level) {
    return std::min(std::max(level, MIN_VISIBLE_LEVEL), MAX_LEVEL);
}

/* static */ int ForgettingCurveUtils::clampToValidCountRange(const int count,
        const HeaderPolicy *const headerPolicy) {
    return std::min(std::max(count, 0), OCCURRENCES_TO_RAISE_THE_LEVEL - 1);
}

/* static */ int ForgettingCurveUtils::clampToValidLevelRange(const int level) {
    return std::min(std::max(level, 0), MAX_LEVEL);
}

/* static */ int ForgettingCurveUtils::clampToValidTimeStepCountRange(const int timeStepCount) {
    return std::min(std::max(timeStepCount, 0), MAX_ELAPSED_TIME_STEP_COUNT);
}

const int ForgettingCurveUtils::ProbabilityTable::PROBABILITY_TABLE_COUNT = 4;
const int ForgettingCurveUtils::ProbabilityTable::WEAK_PROBABILITY_TABLE_ID = 0;
const int ForgettingCurveUtils::ProbabilityTable::MODEST_PROBABILITY_TABLE_ID = 1;
const int ForgettingCurveUtils::ProbabilityTable::STRONG_PROBABILITY_TABLE_ID = 2;
const int ForgettingCurveUtils::ProbabilityTable::AGGRESSIVE_PROBABILITY_TABLE_ID = 3;
const int ForgettingCurveUtils::ProbabilityTable::WEAK_MAX_PROBABILITY = 127;
const int ForgettingCurveUtils::ProbabilityTable::MODEST_BASE_PROBABILITY = 8;
const int ForgettingCurveUtils::ProbabilityTable::STRONG_BASE_PROBABILITY = 9;
const int ForgettingCurveUtils::ProbabilityTable::AGGRESSIVE_BASE_PROBABILITY = 10;


ForgettingCurveUtils::ProbabilityTable::ProbabilityTable() : mTables() {
    mTables.resize(PROBABILITY_TABLE_COUNT);
    for (int tableId = 0; tableId < PROBABILITY_TABLE_COUNT; ++tableId) {
        mTables[tableId].resize(MAX_LEVEL + 1);
        for (int level = 0; level <= MAX_LEVEL; ++level) {
            mTables[tableId][level].resize(MAX_ELAPSED_TIME_STEP_COUNT + 1);
            const float initialProbability = getBaseProbabilityForLevel(tableId, level);
            const float endProbability = getBaseProbabilityForLevel(tableId, level - 1);
            for (int timeStepCount = 0; timeStepCount <= MAX_ELAPSED_TIME_STEP_COUNT;
                    ++timeStepCount) {
                if (level < MIN_VISIBLE_LEVEL) {
                    mTables[tableId][level][timeStepCount] = NOT_A_PROBABILITY;
                    continue;
                }
                const float probability = initialProbability
                        * powf(initialProbability / endProbability,
                                -1.0f * static_cast<float>(timeStepCount)
                                        / static_cast<float>(MAX_ELAPSED_TIME_STEP_COUNT + 1));
                mTables[tableId][level][timeStepCount] =
                        std::min(std::max(static_cast<int>(probability), 1), MAX_PROBABILITY);
            }
        }
    }
}

/* static */ int ForgettingCurveUtils::ProbabilityTable::getBaseProbabilityForLevel(
        const int tableId, const int level) {
    if (tableId == WEAK_PROBABILITY_TABLE_ID) {
        // Max probability is 127.
        return static_cast<float>(WEAK_MAX_PROBABILITY / (1 << (MAX_LEVEL - level)));
    } else if (tableId == MODEST_PROBABILITY_TABLE_ID) {
        // Max probability is 128.
        return static_cast<float>(MODEST_BASE_PROBABILITY * (level + 1));
    } else if (tableId == STRONG_PROBABILITY_TABLE_ID) {
        // Max probability is 140.
        return static_cast<float>(STRONG_BASE_PROBABILITY * (level + 1));
    } else if (tableId == AGGRESSIVE_PROBABILITY_TABLE_ID) {
        // Max probability is 160.
        return static_cast<float>(AGGRESSIVE_BASE_PROBABILITY * (level + 1));
    } else {
        return NOT_A_PROBABILITY;
    }
}

} // namespace latinime
