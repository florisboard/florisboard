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

#ifndef LATINIME_DICTIONARY_HEADER_STRUCTURE_POLICY_H
#define LATINIME_DICTIONARY_HEADER_STRUCTURE_POLICY_H

#include <map>
#include <vector>

#include "defines.h"

namespace latinime {

/*
 * This class abstracts structure of dictionaries.
 * Implement this policy to support additional dictionaries.
 */
class DictionaryHeaderStructurePolicy {
 public:
    typedef std::map<std::vector<int>, std::vector<int>> AttributeMap;

    virtual ~DictionaryHeaderStructurePolicy() {}

    virtual int getFormatVersionNumber() const = 0;

    virtual int getSize() const = 0;

    virtual const AttributeMap *getAttributeMap() const = 0;

    virtual bool requiresGermanUmlautProcessing() const = 0;

    virtual float getMultiWordCostMultiplier() const = 0;

    virtual void readHeaderValueOrQuestionMark(const char *const key, int *outValue,
            int outValueSize) const = 0;

    virtual bool shouldBoostExactMatches() const = 0;

    virtual const std::vector<int> *getLocale() const = 0;

    virtual bool supportsBeginningOfSentence() const = 0;

 protected:
    DictionaryHeaderStructurePolicy() {}

 private:
    DISALLOW_COPY_AND_ASSIGN(DictionaryHeaderStructurePolicy);
};
} // namespace latinime
#endif /* LATINIME_DICTIONARY_HEADER_STRUCTURE_POLICY_H */
