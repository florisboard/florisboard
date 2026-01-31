/*
 * Copyright (C) 2014 The Android Open Source Project
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

#ifndef LATINIME_WORD_PROPERTY_H
#define LATINIME_WORD_PROPERTY_H

#include <vector>

#include "defines.h"
#include "dictionary/property/ngram_property.h"
#include "dictionary/property/unigram_property.h"
#include "utils/int_array_view.h"

namespace latinime {

// This class is used for returning information belonging to a word to java side.
class WordProperty {
 public:
    // Default constructor is used to create an instance that indicates an invalid word.
    WordProperty()
            : mCodePoints(), mUnigramProperty(), mNgrams() {}

    WordProperty(const std::vector<int> &&codePoints, const UnigramProperty &unigramProperty,
            const std::vector<NgramProperty> &ngrams)
            : mCodePoints(std::move(codePoints)), mUnigramProperty(unigramProperty),
              mNgrams(ngrams) {}

    const CodePointArrayView getCodePoints() const {
        return CodePointArrayView(mCodePoints);
    }

    const UnigramProperty &getUnigramProperty() const {
        return mUnigramProperty;
    }

    const std::vector<NgramProperty> &getNgramProperties() const {
        return mNgrams;
    }

 private:
    // Default copy constructor is used for using as a return value.
    DISALLOW_ASSIGNMENT_OPERATOR(WordProperty);

    const std::vector<int> mCodePoints;
    const UnigramProperty mUnigramProperty;
    const std::vector<NgramProperty> mNgrams;
};
} // namespace latinime
#endif // LATINIME_WORD_PROPERTY_H
