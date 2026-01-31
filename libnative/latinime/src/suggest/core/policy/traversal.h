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

#ifndef LATINIME_TRAVERSAL_H
#define LATINIME_TRAVERSAL_H

#include "defines.h"

namespace latinime {

class DicTraverseSession;

class Traversal {
 public:
    virtual int getMaxPointerCount() const = 0;
    virtual bool allowsErrorCorrections(const DicNode *const dicNode) const = 0;
    virtual bool isOmission(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode, const DicNode *const childDicNode,
            const bool allowsErrorCorrections) const = 0;
    virtual bool isSpaceSubstitutionTerminal(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const = 0;
    virtual bool isSpaceOmissionTerminal(const DicTraverseSession *const traverseSession,
               const DicNode *const dicNode) const = 0;
    virtual bool shouldDepthLevelCache(const DicTraverseSession *const traverseSession) const = 0;
    virtual bool shouldNodeLevelCache(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const = 0;
    virtual bool canDoLookAheadCorrection(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode) const = 0;
    virtual ProximityType getProximityType(const DicTraverseSession *const traverseSession,
            const DicNode *const dicNode, const DicNode *const childDicNode) const = 0;
    virtual bool needsToTraverseAllUserInput() const = 0;
    virtual float getMaxSpatialDistance() const = 0;
    virtual int getDefaultExpandDicNodeSize() const = 0;
    virtual int getMaxCacheSize(const int inputSize, const float weightForLocale) const = 0;
    virtual int getTerminalCacheSize() const = 0;
    virtual bool isPossibleOmissionChildNode(const DicTraverseSession *const traverseSession,
            const DicNode *const parentDicNode, const DicNode *const dicNode) const = 0;
    virtual bool isGoodToTraverseNextWord(const DicNode *const dicNode,
            const int probability) const = 0;

 protected:
    Traversal() {}
    virtual ~Traversal() {}

 private:
    DISALLOW_COPY_AND_ASSIGN(Traversal);
};
} // namespace latinime
#endif // LATINIME_TRAVERSAL_H
