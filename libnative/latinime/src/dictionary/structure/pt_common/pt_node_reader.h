/*
 * Copyright (C) 2013, The Android Open Source Project
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

#ifndef LATINIME_PT_NODE_READER_H
#define LATINIME_PT_NODE_READER_H

#include "defines.h"

#include "dictionary/structure/pt_common/pt_node_params.h"

namespace latinime {

// Interface class used to read PtNode information.
class PtNodeReader {
 public:
    virtual ~PtNodeReader() {}
    virtual const PtNodeParams fetchPtNodeParamsInBufferFromPtNodePos(
            const int ptNodePos) const = 0;

 protected:
    PtNodeReader() {};

 private:
    DISALLOW_COPY_AND_ASSIGN(PtNodeReader);
};
} // namespace latinime
#endif /* LATINIME_PT_NODE_READER_H */
