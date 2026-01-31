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

#include "dictionary/structure/v4/content/language_model_dict_content_global_counters.h"

#include <climits>

#include "dictionary/structure/v4/ver4_dict_constants.h"

namespace latinime {

const int LanguageModelDictContentGlobalCounters::COUNTER_VALUE_NEAR_LIMIT_THRESHOLD =
        (1 << (Ver4DictConstants::WORD_COUNT_FIELD_SIZE * CHAR_BIT)) - 64;
const int LanguageModelDictContentGlobalCounters::TOTAL_COUNT_VALUE_NEAR_LIMIT_THRESHOLD = 1 << 30;
const int LanguageModelDictContentGlobalCounters::COUNTER_SIZE_IN_BYTES = 4;
const int LanguageModelDictContentGlobalCounters::TOTAL_COUNT_INDEX = 0;
const int LanguageModelDictContentGlobalCounters::MAX_VALUE_OF_COUNTERS_INDEX = 1;

} // namespace latinime
