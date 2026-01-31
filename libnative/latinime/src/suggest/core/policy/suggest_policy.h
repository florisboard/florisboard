/*
 * Copyright (C) 2013 The Android Open Source Project
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

#ifndef LATINIME_SUGGEST_POLICY_H
#define LATINIME_SUGGEST_POLICY_H

#include "defines.h"

namespace latinime {

class Traversal;
class Scoring;
class Weighting;

class SuggestPolicy {
 public:
    SuggestPolicy() {}
    virtual ~SuggestPolicy() {}
    virtual const Traversal *getTraversal() const = 0;
    virtual const Scoring *getScoring() const = 0;
    virtual const Weighting *getWeighting() const = 0;

 private:
    DISALLOW_COPY_AND_ASSIGN(SuggestPolicy);
};
} // namespace latinime
#endif // LATINIME_SUGGEST_POLICY_H
