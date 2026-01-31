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

#ifndef LATINIME_DIC_NODE_PROFILER_H
#define LATINIME_DIC_NODE_PROFILER_H

#include "defines.h"

#if DEBUG_DICT
#define PROF_SPACE_SUBSTITUTION(profiler) profiler.profSpaceSubstitution()
#define PROF_SPACE_OMISSION(profiler) profiler.profSpaceOmission()
#define PROF_ADDITIONAL_PROXIMITY(profiler) profiler.profAdditionalProximity()
#define PROF_SUBSTITUTION(profiler) profiler.profSubstitution()
#define PROF_OMISSION(profiler) profiler.profOmission()
#define PROF_INSERTION(profiler) profiler.profInsertion()
#define PROF_MATCH(profiler) profiler.profMatch()
#define PROF_COMPLETION(profiler) profiler.profCompletion()
#define PROF_TRANSPOSITION(profiler) profiler.profTransposition()
#define PROF_NEARESTKEY(profiler) profiler.profNearestKey()
#define PROF_TERMINAL(profiler) profiler.profTerminal()
#define PROF_TERMINAL_INSERTION(profiler) profiler.profTerminalInsertion()
#define PROF_NEW_WORD(profiler) profiler.profNewWord()
#define PROF_NEW_WORD_BIGRAM(profiler) profiler.profNewWordBigram()
#define PROF_NODE_RESET(profiler) profiler.reset()
#define PROF_NODE_COPY(src, dest) dest.copy(src)
#else
#define PROF_SPACE_SUBSTITUTION(profiler)
#define PROF_SPACE_OMISSION(profiler)
#define PROF_ADDITONAL_PROXIMITY(profiler)
#define PROF_SUBSTITUTION(profiler)
#define PROF_OMISSION(profiler)
#define PROF_INSERTION(profiler)
#define PROF_MATCH(profiler)
#define PROF_COMPLETION(profiler)
#define PROF_TRANSPOSITION(profiler)
#define PROF_NEARESTKEY(profiler)
#define PROF_TERMINAL(profiler)
#define PROF_TERMINAL_INSERTION(profiler)
#define PROF_NEW_WORD(profiler)
#define PROF_NEW_WORD_BIGRAM(profiler)
#define PROF_NODE_RESET(profiler)
#define PROF_NODE_COPY(src, dest)
#endif

namespace latinime {

class DicNodeProfiler {
 public:
#if DEBUG_DICT
    AK_FORCE_INLINE DicNodeProfiler()
            : mProfOmission(0), mProfInsertion(0), mProfTransposition(0),
              mProfAdditionalProximity(0), mProfSubstitution(0),
              mProfSpaceSubstitution(0), mProfSpaceOmission(0),
              mProfMatch(0), mProfCompletion(0), mProfTerminal(0), mProfTerminalInsertion(0),
              mProfNearestKey(0), mProfNewWord(0), mProfNewWordBigram(0) {}

    int mProfOmission;
    int mProfInsertion;
    int mProfTransposition;
    int mProfAdditionalProximity;
    int mProfSubstitution;
    int mProfSpaceSubstitution;
    int mProfSpaceOmission;
    int mProfMatch;
    int mProfCompletion;
    int mProfTerminal;
    int mProfTerminalInsertion;
    int mProfNearestKey;
    int mProfNewWord;
    int mProfNewWordBigram;

    void profSpaceSubstitution() {
        ++mProfSpaceSubstitution;
    }

    void profSpaceOmission() {
        ++mProfSpaceOmission;
    }

    void profAdditionalProximity() {
        ++mProfAdditionalProximity;
    }

    void profSubstitution() {
        ++mProfSubstitution;
    }

    void profOmission() {
        ++mProfOmission;
    }

    void profInsertion() {
        ++mProfInsertion;
    }

    void profMatch() {
        ++mProfMatch;
    }

    void profCompletion() {
        ++mProfCompletion;
    }

    void profTransposition() {
        ++mProfTransposition;
    }

    void profNearestKey() {
        ++mProfNearestKey;
    }

    void profTerminal() {
        ++mProfTerminal;
    }

    void profTerminalInsertion() {
        ++mProfTerminalInsertion;
    }

    void profNewWord() {
        ++mProfNewWord;
    }

    void profNewWordBigram() {
        ++mProfNewWordBigram;
    }

    void reset() {
        mProfSpaceSubstitution = 0;
        mProfSpaceOmission = 0;
        mProfAdditionalProximity = 0;
        mProfSubstitution = 0;
        mProfOmission = 0;
        mProfInsertion = 0;
        mProfMatch = 0;
        mProfCompletion = 0;
        mProfTransposition = 0;
        mProfNearestKey = 0;
        mProfTerminal = 0;
        mProfNewWord = 0;
        mProfNewWordBigram = 0;
    }

    void copy(const DicNodeProfiler *const profiler) {
        mProfSpaceSubstitution = profiler->mProfSpaceSubstitution;
        mProfSpaceOmission = profiler->mProfSpaceOmission;
        mProfAdditionalProximity = profiler->mProfAdditionalProximity;
        mProfSubstitution = profiler->mProfSubstitution;
        mProfOmission = profiler->mProfOmission;
        mProfInsertion = profiler->mProfInsertion;
        mProfMatch = profiler->mProfMatch;
        mProfCompletion = profiler->mProfCompletion;
        mProfTransposition = profiler->mProfTransposition;
        mProfNearestKey = profiler->mProfNearestKey;
        mProfTerminal = profiler->mProfTerminal;
        mProfNewWord = profiler->mProfNewWord;
        mProfNewWordBigram = profiler->mProfNewWordBigram;
    }

    void dump() const {
        AKLOGI("O %d, I %d, T %d, AP %d, S %d, SS %d, SO %d, M %d, C %d, TE %d, NW = %d, NWB = %d",
                mProfOmission, mProfInsertion, mProfTransposition, mProfAdditionalProximity,
                mProfSubstitution, mProfSpaceSubstitution, mProfSpaceOmission, mProfMatch,
                mProfCompletion, mProfTerminal, mProfNewWord, mProfNewWordBigram);
    }
#else
    DicNodeProfiler() {}
#endif
 private:
    // Caution!!!
    // Use a default copy constructor and an assign operator because shallow copies are ok
    // for this class
};
}
#endif // LATINIME_DIC_NODE_PROFILER_H
