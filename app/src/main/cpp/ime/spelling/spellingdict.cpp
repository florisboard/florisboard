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

#include "spellingdict.h"

using namespace ime::spellcheck;

SpellingDict::SpellingDict(const std::string& aff, const std::string& dic) :
    hunspell(std::make_unique<Hunspell>(aff.c_str(), dic.c_str()))
{ }

SpellingDict::~SpellingDict() = default;

bool SpellingDict::spell(const std::string& word) {
    bool result = hunspell->spell(word);
    return result;
}

std::vector<std::string> SpellingDict::suggest(const std::string &word) {
    auto result = hunspell->suggest(word);
    return result;
}
