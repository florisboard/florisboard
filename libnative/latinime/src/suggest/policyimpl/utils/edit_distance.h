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

#ifndef LATINIME_EDIT_DISTANCE_H
#define LATINIME_EDIT_DISTANCE_H

#include <algorithm>

#include "defines.h"
#include "suggest/policyimpl/utils/edit_distance_policy.h"

namespace latinime {

class EditDistance {
 public:
    // CAVEAT: There may be performance penalty if you need the edit distance as an integer value.
    AK_FORCE_INLINE static float getEditDistance(const EditDistancePolicy *const policy) {
        const int beforeLength = policy->getString0Length();
        const int afterLength = policy->getString1Length();
        float dp[(beforeLength + 1) * (afterLength + 1)];
        for (int i = 0; i <= beforeLength; ++i) {
            dp[(afterLength + 1) * i] = i * policy->getInsertionCost(i - 1, -1);
        }
        for (int i = 0; i <= afterLength; ++i) {
            dp[i] = i * policy->getDeletionCost(-1, i - 1);
        }

        for (int i = 0; i < beforeLength; ++i) {
            for (int j = 0; j < afterLength; ++j) {
                dp[(afterLength + 1) * (i + 1) + (j + 1)] = std::min(
                        dp[(afterLength + 1) * i + (j + 1)] + policy->getInsertionCost(i, j),
                        std::min(
                                dp[(afterLength + 1) * (i + 1) + j] + policy->getDeletionCost(i, j),
                                dp[(afterLength + 1) * i + j] + policy->getSubstitutionCost(i, j)));
                if (policy->allowTransposition(i, j)) {
                    dp[(afterLength + 1) * (i + 1) + (j + 1)] = std::min(
                            dp[(afterLength + 1) * (i + 1) + (j + 1)],
                            dp[(afterLength + 1) * (i - 1) + (j - 1)]
                                    + policy->getTranspositionCost(i, j));
                }
            }
        }
        if (DEBUG_EDIT_DISTANCE) {
            AKLOGI("IN = %d, OUT = %d", beforeLength, afterLength);
            for (int i = 0; i < beforeLength + 1; ++i) {
                for (int j = 0; j < afterLength + 1; ++j) {
                    AKLOGI("EDIT[%d][%d], %f", i, j, dp[(afterLength + 1) * i + j]);
                }
            }
        }
        return dp[(beforeLength + 1) * (afterLength + 1) - 1];
    }

    AK_FORCE_INLINE static void dumpEditDistance10ForDebug(const float *const editDistanceTable,
            const int editDistanceTableWidth, const int outputLength) {
        if (DEBUG_DICT) {
            AKLOGI("EditDistanceTable");
            for (int i = 0; i <= 10; ++i) {
                float c[11];
                for (int j = 0; j <= 10; ++j) {
                    if (j < editDistanceTableWidth + 1 && i < outputLength + 1) {
                        c[j] = (editDistanceTable + i * (editDistanceTableWidth + 1))[j];
                    } else {
                        c[j] = -1.0f;
                    }
                }
                AKLOGI("[ %f, %f, %f, %f, %f, %f, %f, %f, %f, %f, %f ]",
                        c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8], c[9], c[10]);
                (void)c; // To suppress compiler warning
            }
        }
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(EditDistance);
};
} // namespace latinime

#endif  // LATINIME_EDIT_DISTANCE_H
