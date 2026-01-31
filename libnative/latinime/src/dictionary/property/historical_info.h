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

#ifndef LATINIME_HISTORICAL_INFO_H
#define LATINIME_HISTORICAL_INFO_H

#include "defines.h"

namespace latinime {

class HistoricalInfo {
 public:
    // Invalid historical info.
    HistoricalInfo()
            : mTimestamp(NOT_A_TIMESTAMP), mLevel(0), mCount(0) {}

    HistoricalInfo(const int timestamp, const int level, const int count)
            : mTimestamp(timestamp), mLevel(level), mCount(count) {}

    bool isValid() const {
        return mTimestamp != NOT_A_TIMESTAMP;
    }

    int getTimestamp() const {
        return mTimestamp;
    }

    // TODO: Remove
    int getLevel() const {
        return mLevel;
    }

    int getCount() const {
        return mCount;
    }

 private:
    // Default copy constructor is used for using in std::vector.
    DISALLOW_ASSIGNMENT_OPERATOR(HistoricalInfo);

    const int mTimestamp;
    const int mLevel;
    const int mCount;
};
} // namespace latinime
#endif /* LATINIME_HISTORICAL_INFO_H */
