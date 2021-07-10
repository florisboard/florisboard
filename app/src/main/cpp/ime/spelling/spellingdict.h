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

#ifndef FLORISBOARD_SPELLINGDICT_H
#define FLORISBOARD_SPELLINGDICT_H

#include "nuspell/dictionary.hxx"
#include <string>
#include <vector>

namespace ime::spellcheck {

class SpellingDict {
public:
    SpellingDict(const nuspell::Dictionary& dict);
    ~SpellingDict();

    static SpellingDict* load(const std::string& basePath);

    bool spell(const std::string& word);
    std::vector<std::string> suggest(const std::string& word);

private:
    std::unique_ptr<nuspell::Dictionary> dictionary;
};

} // namespace ime::spellcheck

#endif // FLORISBOARD_SPELLINGDICT_H
