/*
 * Copyright (C) 2021 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "staged_suggestion_list.h"

#include <utility>

using namespace ime::nlp;

SuggestionList::SuggestionList(size_t _maxSize) :
    maxSize(_maxSize), internalSize(0), internalArray(new WeightedToken*[_maxSize]), isPrimaryTokenAutoInsert(false)
{
    // Initialize the internal array to null pointers
    for (size_t n = 0; n < maxSize; n++) {
        internalArray[n] = nullptr;
    }
}

SuggestionList::~SuggestionList() {
    delete[] internalArray;
}

bool SuggestionList::add(word_t word, freq_t freq) {
    auto entryIndex = indexOfWord(word);
    if (entryIndex.has_value()) {
        // Word exists already
        auto entry = get(entryIndex.value());
        if (entry->freq < freq) {
            // Need to update freq
            entry->freq = freq;
        } else {
            return false;
        }
    } else {
        if (internalSize < maxSize) {
            internalArray[internalSize++] = new WeightedToken(std::move(word), freq);
        } else {
            WeightedToken *last = internalArray[internalSize - 1];
            if (last->freq < freq) {
                internalArray[internalSize - 1] = new WeightedToken(std::move(word), freq);
            } else {
                return false;
            }
        }
    }
    std::sort(internalArray, internalArray + internalSize, std::greater<>());
    return true;
}

void SuggestionList::clear() {
    for (size_t n = 0; n < internalSize; n++) {
        delete internalArray[n];
        internalArray[n] = nullptr;
    }
    internalSize = 0;
    isPrimaryTokenAutoInsert = false;
}

bool SuggestionList::contains(WeightedToken &element) {
    return indexOf(element).has_value();
}

bool SuggestionList::containsWord(const word_t &word) {
    return indexOfWord(word).has_value();
}

WeightedToken *SuggestionList::get(size_t index) {
    if (index < 0 || index >= maxSize) return nullptr;
    return internalArray[index];
}

std::optional<size_t> SuggestionList::indexOf(WeightedToken &element) {
    for (size_t n = 0; n < internalSize; n++) {
        if (element == *internalArray[n]) {
            return n;
        }
    }
    return std::nullopt;
}

std::optional<size_t> SuggestionList::indexOfWord(const word_t &word) {
    for (size_t n = 0; n < internalSize; n++) {
        if (word == internalArray[n]->data) {
            return n;
        }
    }
    return std::nullopt;
}

bool SuggestionList::isEmpty() const {
    return internalSize == 0;
}

size_t SuggestionList::size() const {
    return internalSize;
}
