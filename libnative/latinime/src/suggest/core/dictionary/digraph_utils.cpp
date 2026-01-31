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

#include "suggest/core/dictionary/digraph_utils.h"

#include <cstdlib>

#include "defines.h"
#include "dictionary/interface/dictionary_header_structure_policy.h"
#include "utils/char_utils.h"

namespace latinime {

const DigraphUtils::digraph_t DigraphUtils::GERMAN_UMLAUT_DIGRAPHS[] =
        { { 'a', 'e', 0x00E4 }, // U+00E4 : LATIN SMALL LETTER A WITH DIAERESIS
        { 'o', 'e', 0x00F6 },   // U+00F6 : LATIN SMALL LETTER O WITH DIAERESIS
        { 'u', 'e', 0x00FC } }; // U+00FC : LATIN SMALL LETTER U WITH DIAERESIS
const DigraphUtils::DigraphType DigraphUtils::USED_DIGRAPH_TYPES[] =
        { DIGRAPH_TYPE_GERMAN_UMLAUT };

/* static */ bool DigraphUtils::hasDigraphForCodePoint(
        const DictionaryHeaderStructurePolicy *const headerPolicy,
        const int compositeGlyphCodePoint) {
    const DigraphUtils::DigraphType digraphType = getDigraphTypeForDictionary(headerPolicy);
    if (DigraphUtils::getDigraphForDigraphTypeAndCodePoint(digraphType, compositeGlyphCodePoint)) {
        return true;
    }
    return false;
}

// Returns the digraph type associated with the given dictionary.
/* static */ DigraphUtils::DigraphType DigraphUtils::getDigraphTypeForDictionary(
        const DictionaryHeaderStructurePolicy *const headerPolicy) {
    if (headerPolicy->requiresGermanUmlautProcessing()) {
        return DIGRAPH_TYPE_GERMAN_UMLAUT;
    }
    return DIGRAPH_TYPE_NONE;
}

// Returns the digraph codepoint for the given composite glyph codepoint and digraph codepoint index
// (which specifies the first or second codepoint in the digraph).
/* static */ int DigraphUtils::getDigraphCodePointForIndex(const int compositeGlyphCodePoint,
        const DigraphCodePointIndex digraphCodePointIndex) {
    if (digraphCodePointIndex == NOT_A_DIGRAPH_INDEX) {
        return NOT_A_CODE_POINT;
    }
    const DigraphUtils::digraph_t *const digraph =
            DigraphUtils::getDigraphForCodePoint(compositeGlyphCodePoint);
    if (!digraph) {
        return NOT_A_CODE_POINT;
    }
    if (digraphCodePointIndex == FIRST_DIGRAPH_CODEPOINT) {
        return digraph->first;
    } else if (digraphCodePointIndex == SECOND_DIGRAPH_CODEPOINT) {
        return digraph->second;
    }
    ASSERT(false);
    return NOT_A_CODE_POINT;
}

// Retrieves the set of all digraphs associated with the given digraph type.
// Returns the size of the digraph array, or 0 if none exist.
/* static */ int DigraphUtils::getAllDigraphsForDigraphTypeAndReturnSize(
        const DigraphUtils::DigraphType digraphType,
        const DigraphUtils::digraph_t **const digraphs) {
    if (digraphType == DigraphUtils::DIGRAPH_TYPE_GERMAN_UMLAUT) {
        *digraphs = GERMAN_UMLAUT_DIGRAPHS;
        return NELEMS(GERMAN_UMLAUT_DIGRAPHS);
    }
    return 0;
}

/**
 * Returns the digraph for the input composite glyph codepoint, or nullptr if none exists.
 * compositeGlyphCodePoint: the method returns the digraph corresponding to this codepoint.
 */
/* static */ const DigraphUtils::digraph_t *DigraphUtils::getDigraphForCodePoint(
        const int compositeGlyphCodePoint) {
    for (size_t i = 0; i < NELEMS(USED_DIGRAPH_TYPES); i++) {
        const DigraphUtils::digraph_t *const digraph = getDigraphForDigraphTypeAndCodePoint(
                USED_DIGRAPH_TYPES[i], compositeGlyphCodePoint);
        if (digraph) {
            return digraph;
        }
    }
    return nullptr;
}

/**
 * Returns the digraph for the input composite glyph codepoint, or nullptr if none exists.
 * digraphType: the type of digraphs supported.
 * compositeGlyphCodePoint: the method returns the digraph corresponding to this codepoint.
 */
/* static */ const DigraphUtils::digraph_t *DigraphUtils::getDigraphForDigraphTypeAndCodePoint(
        const DigraphUtils::DigraphType digraphType, const int compositeGlyphCodePoint) {
    const DigraphUtils::digraph_t *digraphs = nullptr;
    const int compositeGlyphLowerCodePoint = CharUtils::toLowerCase(compositeGlyphCodePoint);
    const int digraphsSize =
            DigraphUtils::getAllDigraphsForDigraphTypeAndReturnSize(digraphType, &digraphs);
    for (int i = 0; i < digraphsSize; i++) {
        if (digraphs[i].compositeGlyph == compositeGlyphLowerCodePoint) {
            return &digraphs[i];
        }
    }
    return nullptr;
}

} // namespace latinime
