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
#include "utils/log.h"

using namespace ime::spellcheck;

SpellingDict::SpellingDict(const nuspell::Dictionary& dict) : dictionary(std::make_unique<nuspell::Dictionary>(dict))
{ }

SpellingDict::~SpellingDict() = default;

SpellingDict* SpellingDict::load(const std::string &basePath) {
    utils::start_stdout_stderr_logger("spell-floris");
    try {
        auto temp = nuspell::Dictionary::load_from_path(basePath);
        auto spellingDict = new SpellingDict(temp);
        return spellingDict;
    } catch (const nuspell::Dictionary_Loading_Error& e) {
        utils::log_error("SpellingDict.load()", e.what());
        return nullptr;
    } catch (...) {
        utils::log_error("SpellingDict.load()", "An unknown error occurred!");
        return nullptr;
    }
}

bool SpellingDict::spell(const std::string& word) {
    bool result = dictionary->spell(word);
    return result;
}

std::vector<std::string> SpellingDict::suggest(const std::string &word) {
    auto result = std::vector<std::string>();
    dictionary->suggest(word, result);
    return result;
}
