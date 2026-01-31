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

#include "suggest/core/layout/additional_proximity_chars.h"

namespace latinime {
// TODO: Stop using hardcoded additional proximity characters.
// TODO: Have proximity character informations in each language's binary dictionary.
const int AdditionalProximityChars::LOCALE_EN_US[LOCALE_EN_US_SIZE] = { 'e', 'n' };

const int AdditionalProximityChars::EN_US_ADDITIONAL_A[EN_US_ADDITIONAL_A_SIZE] = {
    'e', 'i', 'o', 'u'
};

const int AdditionalProximityChars::EN_US_ADDITIONAL_E[EN_US_ADDITIONAL_E_SIZE] = {
    'a', 'i', 'o', 'u'
};

const int AdditionalProximityChars::EN_US_ADDITIONAL_I[EN_US_ADDITIONAL_I_SIZE] = {
    'a', 'e', 'o', 'u'
};

const int AdditionalProximityChars::EN_US_ADDITIONAL_O[EN_US_ADDITIONAL_O_SIZE] = {
    'a', 'e', 'i', 'u'
};

const int AdditionalProximityChars::EN_US_ADDITIONAL_U[EN_US_ADDITIONAL_U_SIZE] = {
    'a', 'e', 'i', 'o'
};
} // namespace latinime
