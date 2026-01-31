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

#include "suggest/policyimpl/typing/typing_weighting.h"

#include "suggest/core/dicnode/dic_node.h"
#include "suggest/core/layout/proximity_info.h"
#include "suggest/policyimpl/typing/scoring_params.h"

namespace latinime {

const TypingWeighting TypingWeighting::sInstance;

ErrorTypeUtils::ErrorType TypingWeighting::getErrorType(const CorrectionType correctionType,
        const DicTraverseSession *const traverseSession, const DicNode *const parentDicNode,
        const DicNode *const dicNode) const {
    switch (correctionType) {
        case CT_MATCH:
            if (isProximityDicNode(traverseSession, dicNode)) {
                return ErrorTypeUtils::PROXIMITY_CORRECTION;
            } else if (dicNode->isInDigraph()) {
                return ErrorTypeUtils::MATCH_WITH_DIGRAPH;
            } else {
                // Compare the node code point with original primary code point on the keyboard.
                const ProximityInfoState *const pInfoState =
                        traverseSession->getProximityInfoState(0);
                const int primaryCodePoint = pInfoState->getPrimaryCodePointAt(
                        dicNode->getInputIndex(0));
                const int nodeCodePoint = dicNode->getNodeCodePoint();
                const int keyIndex = traverseSession->getProximityInfo()->getKeyIndexOf(
                        primaryCodePoint);
                // TODO: Check whether the input code point is on the keyboard.
                if (primaryCodePoint == nodeCodePoint) {
                    // Node code point is same as original code point on the keyboard.
                    return ErrorTypeUtils::NOT_AN_ERROR;
                } else if (CharUtils::toLowerCase(primaryCodePoint) ==
                        CharUtils::toLowerCase(nodeCodePoint)) {
                    // Only cases of the code points are different.
                    return ErrorTypeUtils::MATCH_WITH_WRONG_CASE;
                } else if (primaryCodePoint == CharUtils::toBaseCodePoint(nodeCodePoint)) {
                    // Node code point is a variant of original code point.
                    return ErrorTypeUtils::MATCH_WITH_MISSING_ACCENT;
                } else if (CharUtils::toBaseCodePoint(primaryCodePoint)
                        == CharUtils::toBaseCodePoint(nodeCodePoint)) {
                    // Base code points are the same but the code point is intentionally input.
                    if (keyIndex == NOT_AN_INDEX) {
                        return ErrorTypeUtils::MATCH_WITH_MISSING_EXPLICIT_ACCENT;
                    }
                    return ErrorTypeUtils::MATCH_WITH_WRONG_ACCENT;
                } else if (CharUtils::toLowerCase(primaryCodePoint)
                        == CharUtils::toBaseLowerCase(nodeCodePoint)) {
                    // Node code point is a variant of original code point and the cases are also
                    // different.
                    return ErrorTypeUtils::MATCH_WITH_MISSING_ACCENT
                            | ErrorTypeUtils::MATCH_WITH_WRONG_CASE;
                } else {
                    if (keyIndex == NOT_AN_INDEX) {
                        return ErrorTypeUtils::MATCH_WITH_MISSING_EXPLICIT_ACCENT
                                | ErrorTypeUtils::MATCH_WITH_WRONG_CASE;
                    }
                    // Base code points are the same and the cases are different.
                    return ErrorTypeUtils::MATCH_WITH_WRONG_ACCENT
                            | ErrorTypeUtils::MATCH_WITH_WRONG_CASE;
                }
            }
            break;
        case CT_ADDITIONAL_PROXIMITY:
            // TODO: Change to EDIT_CORRECTION.
            return ErrorTypeUtils::PROXIMITY_CORRECTION;
        case CT_OMISSION:
            if (parentDicNode->canBeIntentionalOmission()) {
                return ErrorTypeUtils::INTENTIONAL_OMISSION;
            } else {
                return ErrorTypeUtils::EDIT_CORRECTION;
            }
            break;
        case CT_SUBSTITUTION:
            // TODO: Quit settng PROXIMITY_CORRECTION.
            return ErrorTypeUtils::EDIT_CORRECTION | ErrorTypeUtils::PROXIMITY_CORRECTION;
        case CT_INSERTION:
        case CT_TERMINAL_INSERTION:
        case CT_TRANSPOSITION:
            return ErrorTypeUtils::EDIT_CORRECTION;
        case CT_NEW_WORD_SPACE_OMISSION:
        case CT_NEW_WORD_SPACE_SUBSTITUTION:
            return ErrorTypeUtils::NEW_WORD;
        case CT_TERMINAL:
            return ErrorTypeUtils::NOT_AN_ERROR;
        case CT_COMPLETION:
            return ErrorTypeUtils::COMPLETION;
        default:
            return ErrorTypeUtils::NOT_AN_ERROR;
    }
}
}  // namespace latinime
