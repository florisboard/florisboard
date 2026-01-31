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

#include "dictionary/structure/v4/content/terminal_position_lookup_table.h"

#include <gtest/gtest.h>

#include <vector>

#include "defines.h"
#include "dictionary/structure/v4/ver4_dict_constants.h"

namespace latinime {
namespace {

TEST(TerminalPositionLookupTableTest, TestGetFromEmptyTable) {
    TerminalPositionLookupTable lookupTable;

    EXPECT_EQ(NOT_A_DICT_POS, lookupTable.getTerminalPtNodePosition(0));
    EXPECT_EQ(NOT_A_DICT_POS, lookupTable.getTerminalPtNodePosition(-1));
    EXPECT_EQ(NOT_A_DICT_POS, lookupTable.getTerminalPtNodePosition(
            Ver4DictConstants::NOT_A_TERMINAL_ID));
}

TEST(TerminalPositionLookupTableTest, TestSetAndGet) {
    TerminalPositionLookupTable lookupTable;

    EXPECT_TRUE(lookupTable.setTerminalPtNodePosition(10, 100));
    EXPECT_EQ(100, lookupTable.getTerminalPtNodePosition(10));
    EXPECT_EQ(NOT_A_DICT_POS, lookupTable.getTerminalPtNodePosition(9));
    EXPECT_TRUE(lookupTable.setTerminalPtNodePosition(9, 200));
    EXPECT_EQ(200, lookupTable.getTerminalPtNodePosition(9));
    EXPECT_TRUE(lookupTable.setTerminalPtNodePosition(10, 300));
    EXPECT_EQ(300, lookupTable.getTerminalPtNodePosition(10));
    EXPECT_FALSE(lookupTable.setTerminalPtNodePosition(-1, 400));
    EXPECT_EQ(NOT_A_DICT_POS, lookupTable.getTerminalPtNodePosition(-1));
    EXPECT_FALSE(lookupTable.setTerminalPtNodePosition(Ver4DictConstants::NOT_A_TERMINAL_ID, 500));
    EXPECT_EQ(NOT_A_DICT_POS, lookupTable.getTerminalPtNodePosition(
            Ver4DictConstants::NOT_A_TERMINAL_ID));
}

TEST(TerminalPositionLookupTableTest, TestGC) {
    TerminalPositionLookupTable lookupTable;

    const std::vector<int> terminalIds = { 10, 20, 30 };
    const std::vector<int> terminalPositions = { 100, 200, 300 };

    for (size_t i = 0; i < terminalIds.size(); ++i) {
        EXPECT_TRUE(lookupTable.setTerminalPtNodePosition(terminalIds[i], terminalPositions[i]));
    }

    TerminalPositionLookupTable::TerminalIdMap terminalIdMap;
    EXPECT_TRUE(lookupTable.runGCTerminalIds(&terminalIdMap));

    for (size_t i = 0; i < terminalIds.size(); ++i) {
        EXPECT_EQ(static_cast<int>(i), terminalIdMap[terminalIds[i]])
                << "Terminal id (" << terminalIds[i] << ") should be changed to " << i;
        EXPECT_EQ(terminalPositions[i], lookupTable.getTerminalPtNodePosition(i));
    }
}

}  // namespace
}  // namespace latinime
