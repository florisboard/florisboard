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

#ifndef LATINIME_DAEMARU_LEVENSHTEIN_EDIT_DISTANCE_POLICY_H
#define LATINIME_DAEMARU_LEVENSHTEIN_EDIT_DISTANCE_POLICY_H

#include "suggest/policyimpl/utils/edit_distance_policy.h"
#include "utils/char_utils.h"

namespace latinime {

class DamerauLevenshteinEditDistancePolicy : public EditDistancePolicy {
 public:
    DamerauLevenshteinEditDistancePolicy(const int *const string0, const int length0,
            const int *const string1, const int length1)
            : mString0(string0), mString0Length(length0), mString1(string1),
              mString1Length(length1) {}
    ~DamerauLevenshteinEditDistancePolicy() {}

    AK_FORCE_INLINE float getSubstitutionCost(const int index0, const int index1) const {
        const int c0 = CharUtils::toBaseLowerCase(mString0[index0]);
        const int c1 = CharUtils::toBaseLowerCase(mString1[index1]);
        return (c0 == c1) ? 0.0f : 1.0f;
    }

    AK_FORCE_INLINE float getDeletionCost(const int index0, const int index1) const {
        return 1.0f;
    }

    AK_FORCE_INLINE float getInsertionCost(const int index0, const int index1) const {
        return 1.0f;
    }

    AK_FORCE_INLINE bool allowTransposition(const int index0, const int index1) const {
        const int c0 = CharUtils::toBaseLowerCase(mString0[index0]);
        const int c1 = CharUtils::toBaseLowerCase(mString1[index1]);
        if (index0 > 0 && index1 > 0 && c0 == CharUtils::toBaseLowerCase(mString1[index1 - 1])
                && c1 == CharUtils::toBaseLowerCase(mString0[index0 - 1])) {
            return true;
        }
        return false;
    }

    AK_FORCE_INLINE float getTranspositionCost(const int index0, const int index1) const {
        return getSubstitutionCost(index0, index1);
    }

    AK_FORCE_INLINE int getString0Length() const {
        return mString0Length;
    }

    AK_FORCE_INLINE int getString1Length() const {
        return mString1Length;
    }

 private:
    DISALLOW_COPY_AND_ASSIGN (DamerauLevenshteinEditDistancePolicy);

    const int *const mString0;
    const int mString0Length;
    const int *const mString1;
    const int mString1Length;
};
} // namespace latinime

#endif  // LATINIME_DAEMARU_LEVENSHTEIN_EDIT_DISTANCE_POLICY_H
