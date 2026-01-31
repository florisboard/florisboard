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

#ifndef LATINIME_PT_NODE_ARRAY_READER_H
#define LATINIME_PT_NODE_ARRAY_READER_H

#include "defines.h"

namespace latinime {

// Interface class used to read PtNode array information.
class PtNodeArrayReader {
 public:
    virtual ~PtNodeArrayReader() {}

    // Returns if the position is valid or not.
    virtual bool readPtNodeArrayInfoAndReturnIfValid(const int ptNodeArrayPos,
            int *const outPtNodeCount, int *const outFirstPtNodePos) const = 0;

    // Returns if the position is valid or not. NOT_A_DICT_POS is set to outNextPtNodeArrayPos when
    // the next array doesn't exist.
    virtual bool readForwardLinkAndReturnIfValid(const int forwordLinkPos,
            int *const outNextPtNodeArrayPos) const = 0;

 protected:
    PtNodeArrayReader() {};

 private:
    DISALLOW_COPY_AND_ASSIGN(PtNodeArrayReader);
};
} // namespace latinime
#endif /* LATINIME_PT_NODE_READER_H */
