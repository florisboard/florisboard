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

#ifndef LATINIME_EDIT_DISTANCE_POLICY_H
#define LATINIME_EDIT_DISTANCE_POLICY_H

#include "defines.h"

namespace latinime {

class EditDistancePolicy {
 public:
    virtual float getSubstitutionCost(const int index0, const int index1) const = 0;
    virtual float getDeletionCost(const int index0, const int index1) const = 0;
    virtual float getInsertionCost(const int index0, const int index1) const = 0;
    virtual bool allowTransposition(const int index0, const int index1) const = 0;
    virtual float getTranspositionCost(const int index0, const int index1) const = 0;
    virtual int getString0Length() const = 0;
    virtual int getString1Length() const = 0;

 protected:
    EditDistancePolicy() {}
    virtual ~EditDistancePolicy() {}

 private:
    DISALLOW_COPY_AND_ASSIGN(EditDistancePolicy);
};
} // namespace latinime

#endif  // LATINIME_EDIT_DISTANCE_POLICY_H
