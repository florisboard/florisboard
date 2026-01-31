/*
 * Copyright (C) 2014, The Android Open Source Project
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

#ifndef LATINIME_WORD_ATTRIBUTES_H
#define LATINIME_WORD_ATTRIBUTES_H

#include "defines.h"

class WordAttributes {
 public:
    // Invalid word attributes.
    WordAttributes()
            : mProbability(NOT_A_PROBABILITY), mIsBlacklisted(false), mIsNotAWord(false),
              mIsPossiblyOffensive(false) {}

    WordAttributes(const int probability, const bool isBlacklisted, const bool isNotAWord,
            const bool isPossiblyOffensive)
            : mProbability(probability), mIsBlacklisted(isBlacklisted), mIsNotAWord(isNotAWord),
              mIsPossiblyOffensive(isPossiblyOffensive) {}

    int getProbability() const {
        return mProbability;
    }

    bool isBlacklisted() const {
        return mIsBlacklisted;
    }

    bool isNotAWord() const {
        return mIsNotAWord;
    }

    // Whether or not a word is possibly offensive.
    // * Static dictionaries <v202, as well as dynamic dictionaries <v403, will set this based on
    //   whether or not the probability of the word is zero.
    // * Static dictionaries >=v203 will set this based on the IS_POSSIBLY_OFFENSIVE PtNode flag.
    // * Dynamic dictionaries >=v403 will set this based on the IS_POSSIBLY_OFFENSIVE language model
    //   flag (the PtNode flag IS_BLACKLISTED is ignored and kept as zero)
    //
    // See the ::getWordAttributes function for each of these dictionary policies for more details.
    bool isPossiblyOffensive() const {
        return mIsPossiblyOffensive;
    }

 private:
    DISALLOW_ASSIGNMENT_OPERATOR(WordAttributes);

    int mProbability;
    bool mIsBlacklisted;
    bool mIsNotAWord;
    bool mIsPossiblyOffensive;
};

 // namespace
#endif /* LATINIME_WORD_ATTRIBUTES_H */
