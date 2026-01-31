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

#ifndef LATINIME_TYPING_SUGGEST_POLICY_FACTORY_H
#define LATINIME_TYPING_SUGGEST_POLICY_FACTORY_H

#include "defines.h"
#include "typing_suggest_policy.h"

namespace latinime {

class SuggestPolicy;

class TypingSuggestPolicyFactory {
 public:
    static const SuggestPolicy *getTypingSuggestPolicy() {
        return TypingSuggestPolicy::getInstance();
    }

 private:
    DISALLOW_COPY_AND_ASSIGN(TypingSuggestPolicyFactory);
};
} // namespace latinime
#endif // LATINIME_TYPING_SUGGEST_POLICY_FACTORY_H
