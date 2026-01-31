/*
 * Copyright (C) 2012 The Android Open Source Project
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

#ifndef LATINIME_SUGGEST_INTERFACE_H
#define LATINIME_SUGGEST_INTERFACE_H

#include "defines.h"

namespace latinime {

class ProximityInfo;
class SuggestionResults;

class SuggestInterface {
 public:
    virtual void getSuggestions(ProximityInfo *pInfo, void *traverseSession, int *inputXs,
            int *inputYs, int *times, int *pointerIds, int *inputCodePoints, int inputSize,
            const float weightOfLangModelVsSpatialModel,
            SuggestionResults *const suggestionResults) const = 0;
    SuggestInterface() {}
    virtual ~SuggestInterface() {}
 private:
    DISALLOW_COPY_AND_ASSIGN(SuggestInterface);
};
} // namespace latinime
#endif // LATINIME_SUGGEST_INTERFACE_H
