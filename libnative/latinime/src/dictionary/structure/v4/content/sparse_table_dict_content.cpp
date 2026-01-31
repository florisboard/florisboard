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

#include "dictionary/structure/v4/content/sparse_table_dict_content.h"

#include "dictionary/utils/dict_file_writing_utils.h"

namespace latinime {

const int SparseTableDictContent::LOOKUP_TABLE_BUFFER_INDEX = 0;
const int SparseTableDictContent::ADDRESS_TABLE_BUFFER_INDEX = 1;
const int SparseTableDictContent::CONTENT_BUFFER_INDEX = 2;

bool SparseTableDictContent::flush(FILE *const file) const {
    if (!DictFileWritingUtils::writeBufferToFileTail(file, &mExpandableLookupTableBuffer)) {
        return false;
    }
    if (!DictFileWritingUtils::writeBufferToFileTail(file, &mExpandableAddressTableBuffer)) {
        return false;
    }
    if (!DictFileWritingUtils::writeBufferToFileTail(file, &mExpandableContentBuffer)) {
        return false;
    }
    return true;
}

} // namespace latinime
