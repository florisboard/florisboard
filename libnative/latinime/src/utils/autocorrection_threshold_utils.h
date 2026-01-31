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

#ifndef LATINIME_AUTOCORRECTION_THRESHOLD_UTILS_H
#define LATINIME_AUTOCORRECTION_THRESHOLD_UTILS_H

#include "defines.h"

namespace latinime {

class AutocorrectionThresholdUtils {
 public:
    static float calcNormalizedScore(const int *before, const int beforeLength,
            const int *after, const int afterLength, const int score);
    static int editDistance(const int *before, const int beforeLength, const int *after,
            const int afterLength);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(AutocorrectionThresholdUtils);

    static const int MAX_INITIAL_SCORE;
    static const int TYPED_LETTER_MULTIPLIER;
    static const int FULL_WORD_MULTIPLIER;
};
} // namespace latinime
#endif // LATINIME_AUTOCORRECTION_THRESHOLD_UTILS_H
