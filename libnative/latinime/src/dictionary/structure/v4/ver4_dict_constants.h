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

#ifndef LATINIME_VER4_DICT_CONSTANTS_H
#define LATINIME_VER4_DICT_CONSTANTS_H

#include "defines.h"

#include <cstddef>
#include <cstdint>

namespace latinime {

// TODO: Create PtConstants under the pt_common and move some constant values there.
// Note that there are corresponding definitions in FormatSpec.java.
class Ver4DictConstants {
 public:
    static const char *const BODY_FILE_EXTENSION;
    static const char *const HEADER_FILE_EXTENSION;
    static const int MAX_DICTIONARY_SIZE;
    static const int MAX_DICT_EXTENDED_REGION_SIZE;

    static const size_t NUM_OF_CONTENT_BUFFERS_IN_BODY_FILE;
    static const int TRIE_BUFFER_INDEX;
    static const int TERMINAL_ADDRESS_LOOKUP_TABLE_BUFFER_INDEX;
    static const int LANGUAGE_MODEL_BUFFER_INDEX;
    static const int BIGRAM_BUFFERS_INDEX;
    static const int SHORTCUT_BUFFERS_INDEX;

    static const int NOT_A_TERMINAL_ID;
    static const int PROBABILITY_SIZE;
    static const int FLAGS_IN_LANGUAGE_MODEL_SIZE;
    static const int TERMINAL_ADDRESS_TABLE_ADDRESS_SIZE;
    static const int NOT_A_TERMINAL_ADDRESS;
    static const int TERMINAL_ID_FIELD_SIZE;
    static const int TIME_STAMP_FIELD_SIZE;
    // TODO: Remove
    static const int WORD_LEVEL_FIELD_SIZE;
    static const int WORD_COUNT_FIELD_SIZE;
    // Flags in probability entry.
    static const uint8_t FLAG_REPRESENTS_BEGINNING_OF_SENTENCE;
    static const uint8_t FLAG_NOT_A_VALID_ENTRY;
    static const uint8_t FLAG_NOT_A_WORD;
    static const uint8_t FLAG_BLACKLISTED;
    static const uint8_t FLAG_POSSIBLY_OFFENSIVE;

    static const int SHORTCUT_ADDRESS_TABLE_BLOCK_SIZE;
    static const int SHORTCUT_ADDRESS_TABLE_DATA_SIZE;

    static const int SHORTCUT_FLAGS_FIELD_SIZE;
    static const int SHORTCUT_PROBABILITY_MASK;
    static const int SHORTCUT_HAS_NEXT_MASK;

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(Ver4DictConstants);

    static const size_t NUM_OF_BUFFERS_FOR_SINGLE_DICT_CONTENT;
    static const size_t NUM_OF_BUFFERS_FOR_SPARSE_TABLE_DICT_CONTENT;
    static const size_t NUM_OF_BUFFERS_FOR_LANGUAGE_MODEL_DICT_CONTENT;
};
} // namespace latinime
#endif /* LATINIME_VER4_DICT_CONSTANTS_H */
