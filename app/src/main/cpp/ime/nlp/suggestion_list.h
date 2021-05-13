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

#ifndef FLORISBOARD_SUGGESTION_LIST_H
#define FLORISBOARD_SUGGESTION_LIST_H

#include <optional>
#include <vector>
#include "token.h"

namespace ime::nlp {

class SuggestionList {
public:
    SuggestionList(size_t _maxSize);
    ~SuggestionList();

    bool add(word_t &&word, freq_t &&freq);
    void clear();
    bool contains(const WeightedToken &element) const;
    bool containsWord(const word_t &word) const;
    const WeightedToken *get(size_t index) const;
    std::optional<size_t> indexOf(const WeightedToken &element) const;
    std::optional<size_t> indexOfWord(const word_t &word) const;
    bool isEmpty() const;
    size_t size() const;

    bool isPrimaryTokenAutoInsert;

private:
    std::vector<WeightedToken> tokens;
    size_t internalSize;
    size_t maxSize;
};

} // namespace ime::nlp

#endif // FLORISBOARD_SUGGESTION_LIST_H
