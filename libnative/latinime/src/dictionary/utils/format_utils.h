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

#ifndef LATINIME_FORMAT_UTILS_H
#define LATINIME_FORMAT_UTILS_H

#include <cstdint>

#include "defines.h"
#include "utils/byte_array_view.h"

namespace latinime {

/**
 * Methods to handle binary dictionary format version.
 */
class FormatUtils {
 public:
    enum FORMAT_VERSION {
        // These MUST have the same values as the relevant constants in FormatSpec.java.
        // TODO: Remove VERSION_2 and VERSION_201 when we:
        // * Confirm that old versions of LatinIME download old-format dictionaries
        // * We no longer need the corresponding constants on the Java side for dicttool
        VERSION_2 = 2,
        VERSION_201 = 201,
        VERSION_202 = 202,
        VERSION_4_ONLY_FOR_TESTING = 399,
        VERSION_402 = 402,
        VERSION_403 = 403,
        UNKNOWN_VERSION = -1
    };

    // 32 bit magic number is stored at the beginning of the dictionary header to reject
    // unsupported or obsolete dictionary formats.
    static const uint32_t MAGIC_NUMBER;

    static FORMAT_VERSION getFormatVersion(const int formatVersion);
    static FORMAT_VERSION detectFormatVersion(const ReadOnlyByteArrayView dictBuffer);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(FormatUtils);

    static const size_t DICTIONARY_MINIMUM_SIZE;
};
} // namespace latinime
#endif /* LATINIME_FORMAT_UTILS_H */
