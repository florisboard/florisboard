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

#ifndef DIGRAPH_UTILS_H
#define DIGRAPH_UTILS_H

#include "defines.h"

namespace latinime {

class DictionaryHeaderStructurePolicy;

class DigraphUtils {
 public:
    typedef enum {
        NOT_A_DIGRAPH_INDEX,
        FIRST_DIGRAPH_CODEPOINT,
        SECOND_DIGRAPH_CODEPOINT
    } DigraphCodePointIndex;

    typedef enum {
        DIGRAPH_TYPE_NONE,
        DIGRAPH_TYPE_GERMAN_UMLAUT,
    } DigraphType;

    typedef struct { int first; int second; int compositeGlyph; } digraph_t;

    static bool hasDigraphForCodePoint(const DictionaryHeaderStructurePolicy *const headerPolicy,
            const int compositeGlyphCodePoint);
    static int getDigraphCodePointForIndex(const int compositeGlyphCodePoint,
            const DigraphCodePointIndex digraphCodePointIndex);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(DigraphUtils);
    static DigraphType getDigraphTypeForDictionary(
            const DictionaryHeaderStructurePolicy *const headerPolicy);
    static int getAllDigraphsForDigraphTypeAndReturnSize(
            const DigraphType digraphType, const digraph_t **const digraphs);
    static const digraph_t *getDigraphForCodePoint(const int compositeGlyphCodePoint);
    static const digraph_t *getDigraphForDigraphTypeAndCodePoint(
            const DigraphType digraphType, const int compositeGlyphCodePoint);

    static const digraph_t GERMAN_UMLAUT_DIGRAPHS[];
    static const DigraphType USED_DIGRAPH_TYPES[];
};
} // namespace latinime
#endif // DIGRAPH_UTILS_H
