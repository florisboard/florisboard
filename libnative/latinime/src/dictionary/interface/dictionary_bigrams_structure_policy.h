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

#ifndef LATINIME_DICTIONARY_BIGRAMS_STRUCTURE_POLICY_H
#define LATINIME_DICTIONARY_BIGRAMS_STRUCTURE_POLICY_H

#include "defines.h"

namespace latinime {

/*
 * This class abstracts structure of bigrams.
 */
class DictionaryBigramsStructurePolicy {
 public:
    virtual ~DictionaryBigramsStructurePolicy() {}

    virtual void getNextBigram(int *const outBigramPos, int *const outProbability,
            bool *const outHasNext, int *const pos) const = 0;
    virtual bool skipAllBigrams(int *const pos) const = 0;

 protected:
    DictionaryBigramsStructurePolicy() {}

 private:
    DISALLOW_COPY_AND_ASSIGN(DictionaryBigramsStructurePolicy);
};
} // namespace latinime
#endif /* LATINIME_DICTIONARY_BIGRAMS_STRUCTURE_POLICY_H */
