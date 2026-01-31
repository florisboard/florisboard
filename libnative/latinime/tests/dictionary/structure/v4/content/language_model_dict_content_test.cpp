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

#include "dictionary/structure/v4/content/language_model_dict_content.h"

#include <gtest/gtest.h>

#include <array>
#include <unordered_set>

#include "utils/int_array_view.h"

namespace latinime {
namespace {

TEST(LanguageModelDictContentTest, TestUnigramProbability) {
    LanguageModelDictContent languageModelDictContent(false /* useHistoricalInfo */);

    const int flag = 0xF0;
    const int probability = 10;
    const int wordId = 100;
    const ProbabilityEntry probabilityEntry(flag, probability);
    languageModelDictContent.setProbabilityEntry(wordId, &probabilityEntry);
    const ProbabilityEntry entry =
            languageModelDictContent.getProbabilityEntry(wordId);
    EXPECT_EQ(flag, entry.getFlags());
    EXPECT_EQ(probability, entry.getProbability());

    // Remove
    EXPECT_TRUE(languageModelDictContent.removeProbabilityEntry(wordId));
    EXPECT_FALSE(languageModelDictContent.getProbabilityEntry(wordId).isValid());
    EXPECT_FALSE(languageModelDictContent.removeProbabilityEntry(wordId));
    EXPECT_TRUE(languageModelDictContent.setProbabilityEntry(wordId, &probabilityEntry));
    EXPECT_TRUE(languageModelDictContent.getProbabilityEntry(wordId).isValid());
}

TEST(LanguageModelDictContentTest, TestUnigramProbabilityWithHistoricalInfo) {
    LanguageModelDictContent languageModelDictContent(true /* useHistoricalInfo */);

    const int flag = 0xF0;
    const int timestamp = 0x3FFFFFFF;
    const int count = 10;
    const int wordId = 100;
    const HistoricalInfo historicalInfo(timestamp, 0 /* level */, count);
    const ProbabilityEntry probabilityEntry(flag, &historicalInfo);
    languageModelDictContent.setProbabilityEntry(wordId, &probabilityEntry);
    const ProbabilityEntry entry = languageModelDictContent.getProbabilityEntry(wordId);
    EXPECT_EQ(flag, entry.getFlags());
    EXPECT_EQ(timestamp, entry.getHistoricalInfo()->getTimestamp());
    EXPECT_EQ(count, entry.getHistoricalInfo()->getCount());

    // Remove
    EXPECT_TRUE(languageModelDictContent.removeProbabilityEntry(wordId));
    EXPECT_FALSE(languageModelDictContent.getProbabilityEntry(wordId).isValid());
    EXPECT_FALSE(languageModelDictContent.removeProbabilityEntry(wordId));
    EXPECT_TRUE(languageModelDictContent.setProbabilityEntry(wordId, &probabilityEntry));
    EXPECT_TRUE(languageModelDictContent.removeProbabilityEntry(wordId));
}

TEST(LanguageModelDictContentTest, TestIterateProbabilityEntry) {
    LanguageModelDictContent languageModelDictContent(false /* useHistoricalInfo */);

    const ProbabilityEntry originalEntry(0xFC, 100);

    const int wordIds[] = { 1, 2, 3, 4, 5 };
    for (const int wordId : wordIds) {
        languageModelDictContent.setProbabilityEntry(wordId, &originalEntry);
    }
    std::unordered_set<int> wordIdSet(std::begin(wordIds), std::end(wordIds));
    for (const auto& entry : languageModelDictContent.getProbabilityEntries(WordIdArrayView())) {
        EXPECT_EQ(originalEntry.getFlags(), entry.getProbabilityEntry().getFlags());
        EXPECT_EQ(originalEntry.getProbability(), entry.getProbabilityEntry().getProbability());
        wordIdSet.erase(entry.getWordId());
    }
    EXPECT_TRUE(wordIdSet.empty());
}

TEST(LanguageModelDictContentTest, TestGetWordProbability) {
    LanguageModelDictContent languageModelDictContent(false /* useHistoricalInfo */);

    const int flag = 0xFF;
    const int probability = 10;
    const int bigramProbability = 20;
    const int trigramProbability = 30;
    const int wordId = 100;
    const std::array<int, 2> prevWordIdArray = {{ 1, 2 }};
    const WordIdArrayView prevWordIds = WordIdArrayView::fromArray(prevWordIdArray);

    const ProbabilityEntry probabilityEntry(flag, probability);
    languageModelDictContent.setProbabilityEntry(wordId, &probabilityEntry);
    const ProbabilityEntry bigramProbabilityEntry(flag, bigramProbability);
    languageModelDictContent.setProbabilityEntry(prevWordIds[0], &probabilityEntry);
    languageModelDictContent.setNgramProbabilityEntry(prevWordIds.limit(1), wordId,
            &bigramProbabilityEntry);
    EXPECT_EQ(bigramProbability, languageModelDictContent.getWordAttributes(prevWordIds, wordId,
            false /* mustMatchAllPrevWords */, nullptr /* headerPolicy */).getProbability());
    const ProbabilityEntry trigramProbabilityEntry(flag, trigramProbability);
    languageModelDictContent.setNgramProbabilityEntry(prevWordIds.limit(1),
            prevWordIds[1], &probabilityEntry);
    languageModelDictContent.setNgramProbabilityEntry(prevWordIds.limit(2), wordId,
            &trigramProbabilityEntry);
    EXPECT_EQ(trigramProbability, languageModelDictContent.getWordAttributes(prevWordIds, wordId,
            false /* mustMatchAllPrevWords */, nullptr /* headerPolicy */).getProbability());
}

}  // namespace
}  // namespace latinime
