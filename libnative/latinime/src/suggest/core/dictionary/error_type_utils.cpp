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

#include "suggest/core/dictionary/error_type_utils.h"

namespace latinime {

const ErrorTypeUtils::ErrorType ErrorTypeUtils::NOT_AN_ERROR = 0x0;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::MATCH_WITH_WRONG_CASE = 0x1;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::MATCH_WITH_MISSING_ACCENT = 0x2;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::MATCH_WITH_MISSING_EXPLICIT_ACCENT = 0x4;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::MATCH_WITH_WRONG_ACCENT = 0x8;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::MATCH_WITH_DIGRAPH = 0x10;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::INTENTIONAL_OMISSION = 0x20;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::EDIT_CORRECTION = 0x40;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::PROXIMITY_CORRECTION = 0x80;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::COMPLETION = 0x100;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::NEW_WORD = 0x200;

const ErrorTypeUtils::ErrorType ErrorTypeUtils::ERRORS_TREATED_AS_AN_EXACT_MATCH =
        NOT_AN_ERROR | MATCH_WITH_WRONG_CASE | MATCH_WITH_MISSING_ACCENT | MATCH_WITH_DIGRAPH;
const ErrorTypeUtils::ErrorType ErrorTypeUtils::ERRORS_TREATED_AS_A_PERFECT_MATCH = NOT_AN_ERROR;

const ErrorTypeUtils::ErrorType
        ErrorTypeUtils::ERRORS_TREATED_AS_AN_EXACT_MATCH_WITH_INTENTIONAL_OMISSION =
                ERRORS_TREATED_AS_AN_EXACT_MATCH | INTENTIONAL_OMISSION;

} // namespace latinime
