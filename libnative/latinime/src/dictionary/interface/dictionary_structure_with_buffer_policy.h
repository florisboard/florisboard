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

#ifndef LATINIME_DICTIONARY_STRUCTURE_POLICY_H
#define LATINIME_DICTIONARY_STRUCTURE_POLICY_H

#include <memory>

#include "defines.h"
#include "dictionary/property/historical_info.h"
#include "dictionary/property/word_attributes.h"
#include "dictionary/property/word_property.h"
#include "dictionary/utils/binary_dictionary_shortcut_iterator.h"
#include "utils/int_array_view.h"

namespace latinime {

class DicNode;
class DicNodeVector;
class DictionaryHeaderStructurePolicy;
class MultiBigramMap;
class NgramListener;
class NgramContext;
class UnigramProperty;

/*
 * This class abstracts the structure of dictionaries.
 * Implement this policy to support additional dictionaries.
 */
class DictionaryStructureWithBufferPolicy {
 public:
    typedef std::unique_ptr<DictionaryStructureWithBufferPolicy> StructurePolicyPtr;

    virtual ~DictionaryStructureWithBufferPolicy() {}

    virtual int getRootPosition() const = 0;

    virtual void createAndGetAllChildDicNodes(const DicNode *const dicNode,
            DicNodeVector *const childDicNodes) const = 0;

    virtual int getCodePointsAndReturnCodePointCount(const int wordId, const int maxCodePointCount,
            int *const outCodePoints) const = 0;

    virtual int getWordId(const CodePointArrayView wordCodePoints,
            const bool forceLowerCaseSearch) const = 0;

    virtual const WordAttributes getWordAttributesInContext(const WordIdArrayView prevWordIds,
            const int wordId, MultiBigramMap *const multiBigramMap) const = 0;

    // TODO: Remove
    virtual int getProbability(const int unigramProbability, const int bigramProbability) const = 0;

    virtual int getProbabilityOfWord(const WordIdArrayView prevWordIds, const int wordId) const = 0;

    virtual void iterateNgramEntries(const WordIdArrayView prevWordIds,
            NgramListener *const listener) const = 0;

    virtual BinaryDictionaryShortcutIterator getShortcutIterator(const int wordId) const = 0;

    virtual const DictionaryHeaderStructurePolicy *getHeaderStructurePolicy() const = 0;

    // Returns whether the update was success or not.
    virtual bool addUnigramEntry(const CodePointArrayView wordCodePoints,
            const UnigramProperty *const unigramProperty) = 0;

    // Returns whether the update was success or not.
    virtual bool removeUnigramEntry(const CodePointArrayView wordCodePoints) = 0;

    // Returns whether the update was success or not.
    virtual bool addNgramEntry(const NgramProperty *const ngramProperty) = 0;

    // Returns whether the update was success or not.
    virtual bool removeNgramEntry(const NgramContext *const ngramContext,
            const CodePointArrayView wordCodePoints) = 0;

    // Returns whether the update was success or not.
    virtual bool updateEntriesForWordWithNgramContext(const NgramContext *const ngramContext,
            const CodePointArrayView wordCodePoints, const bool isValidWord,
            const HistoricalInfo historicalInfo) = 0;

    // Returns whether the flush was success or not.
    virtual bool flush(const char *const filePath) = 0;

    // Returns whether the GC and flush were success or not.
    virtual bool flushWithGC(const char *const filePath) = 0;

    virtual bool needsToRunGC(const bool mindsBlockByGC) const = 0;

    // Currently, this method is used only for testing. You may want to consider creating new
    // dedicated method instead of this if you want to use this in the production.
    virtual void getProperty(const char *const query, const int queryLength, char *const outResult,
            const int maxResultLength) = 0;

    virtual const WordProperty getWordProperty(const CodePointArrayView wordCodePoints) const = 0;

    // Method to iterate all words in the dictionary.
    // The returned token has to be used to get the next word. If token is 0, this method newly
    // starts iterating the dictionary.
    virtual int getNextWordAndNextToken(const int token, int *const outCodePoints,
            int *const outCodePointCount) = 0;

    virtual bool isCorrupted() const = 0;

 protected:
    DictionaryStructureWithBufferPolicy() {}

 private:
    DISALLOW_COPY_AND_ASSIGN(DictionaryStructureWithBufferPolicy);
};
} // namespace latinime
#endif /* LATINIME_DICTIONARY_STRUCTURE_POLICY_H */
