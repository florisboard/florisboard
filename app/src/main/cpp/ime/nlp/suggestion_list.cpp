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

#include "suggestion_list.h"

#include <utility>

using namespace ime::nlp;

SuggestionList::SuggestionList(size_t _maxSize) :
    maxSize(_maxSize), internalSize(0), tokens(_maxSize), isPrimaryTokenAutoInsert(false)
{ }

SuggestionList::~SuggestionList() = default;

bool SuggestionList::add(word_t &&word, freq_t &&freq) {
    auto entryIndex = indexOfWord(word);
    if (entryIndex.has_value()) {
        // Word exists already
        auto entry = tokens[entryIndex.value()];
        if (entry.freq < freq) {
            // Need to update freq
            entry.freq = freq;
        } else {
            return false;
        }
    } else {
        if (internalSize < maxSize) {
            tokens[internalSize++] = WeightedToken(std::move(word), freq);
        } else {
            auto last = tokens[internalSize - 1];
            if (last.freq < freq) {
                tokens[internalSize - 1] = WeightedToken(std::move(word), freq);
            } else {
                return false;
            }
        }
    }
    std::sort(tokens.begin(), tokens.begin() + internalSize, std::greater<>());
    return true;
}

void SuggestionList::clear() {
    internalSize = 0;
    isPrimaryTokenAutoInsert = false;
}

bool SuggestionList::contains(const WeightedToken &element) const {
    return indexOf(element).has_value();
}

bool SuggestionList::containsWord(const word_t &word) const {
    return indexOfWord(word).has_value();
}

const WeightedToken *SuggestionList::get(size_t index) const {
    if (index < 0 || index >= internalSize) return nullptr;
    return &tokens[index];
}

std::optional<size_t> SuggestionList::indexOf(const WeightedToken &element) const {
    for (size_t n = 0; n < internalSize; n++) {
        if (element == tokens[n]) {
            return n;
        }
    }
    return std::nullopt;
}

std::optional<size_t> SuggestionList::indexOfWord(const word_t &word) const {
    for (size_t n = 0; n < internalSize; n++) {
        if (word == tokens[n].data) {
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
