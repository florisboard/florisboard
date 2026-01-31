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

#include "dictionary/utils/byte_array_utils.h"

namespace latinime {

const uint8_t ByteArrayUtils::MINIMUM_ONE_BYTE_CHARACTER_VALUE = 0x20;
const uint8_t ByteArrayUtils::MAXIMUM_ONE_BYTE_CHARACTER_VALUE = 0xFF;
const uint8_t ByteArrayUtils::CHARACTER_ARRAY_TERMINATOR = 0x1F;

} // namespace latinime
