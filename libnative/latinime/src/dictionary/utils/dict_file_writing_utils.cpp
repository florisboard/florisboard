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

#include "dictionary/utils/dict_file_writing_utils.h"

#include <cstdio>
#include <errno.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>

#include "dictionary/header/header_policy.h"
#include "dictionary/structure/backward/v402/ver4_dict_buffers.h"
#include "dictionary/structure/pt_common/dynamic_pt_writing_utils.h"
#include "dictionary/structure/v4/ver4_dict_buffers.h"
#include "dictionary/utils/buffer_with_extendable_buffer.h"
#include "dictionary/utils/entry_counters.h"
#include "dictionary/utils/file_utils.h"
#include "dictionary/utils/format_utils.h"
#include "utils/time_keeper.h"

namespace latinime {

const char *const DictFileWritingUtils::TEMP_FILE_SUFFIX_FOR_WRITING_DICT_FILE = ".tmp";
// Enough size to describe buffer size.
const int DictFileWritingUtils::SIZE_OF_BUFFER_SIZE_FIELD = 4;

/* static */ bool DictFileWritingUtils::createEmptyDictFile(const char *const filePath,
        const int dictVersion, const std::vector<int> localeAsCodePointVector,
        const DictionaryHeaderStructurePolicy::AttributeMap *const attributeMap) {
    TimeKeeper::setCurrentTime();
    const FormatUtils::FORMAT_VERSION formatVersion = FormatUtils::getFormatVersion(dictVersion);
    switch (formatVersion) {
        case FormatUtils::VERSION_402:
            return createEmptyV4DictFile<backward::v402::Ver4DictConstants,
                    backward::v402::Ver4DictBuffers,
                    backward::v402::Ver4DictBuffers::Ver4DictBuffersPtr>(
                            filePath, localeAsCodePointVector, attributeMap, formatVersion);
        case FormatUtils::VERSION_4_ONLY_FOR_TESTING:
        case FormatUtils::VERSION_403:
            return createEmptyV4DictFile<Ver4DictConstants, Ver4DictBuffers,
                    Ver4DictBuffers::Ver4DictBuffersPtr>(
                            filePath, localeAsCodePointVector, attributeMap, formatVersion);
        default:
            AKLOGE("Cannot create dictionary %s because format version %d is not supported.",
                    filePath, dictVersion);
            return false;
    }
}

template<class DictConstants, class DictBuffers, class DictBuffersPtr>
/* static */ bool DictFileWritingUtils::createEmptyV4DictFile(const char *const dirPath,
        const std::vector<int> localeAsCodePointVector,
        const DictionaryHeaderStructurePolicy::AttributeMap *const attributeMap,
        const FormatUtils::FORMAT_VERSION formatVersion) {
    HeaderPolicy headerPolicy(formatVersion, localeAsCodePointVector, attributeMap);
    DictBuffersPtr dictBuffers = DictBuffers::createVer4DictBuffers(&headerPolicy,
            DictConstants::MAX_DICT_EXTENDED_REGION_SIZE);
    headerPolicy.fillInAndWriteHeaderToBuffer(true /* updatesLastDecayedTime */,
            EntryCounts(), 0 /* extendedRegionSize */, dictBuffers->getWritableHeaderBuffer());
    if (!DynamicPtWritingUtils::writeEmptyDictionary(
            dictBuffers->getWritableTrieBuffer(), 0 /* rootPos */)) {
        AKLOGE("Empty ver4 dictionary structure cannot be created on memory.");
        return false;
    }
    return dictBuffers->flush(dirPath);
}

/* static */ bool DictFileWritingUtils::flushBufferToFileWithSuffix(const char *const basePath,
        const char *const suffix, const BufferWithExtendableBuffer *const buffer) {
    const int filePathBufSize = FileUtils::getFilePathWithSuffixBufSize(basePath, suffix);
    char filePath[filePathBufSize];
    FileUtils::getFilePathWithSuffix(basePath, suffix, filePathBufSize, filePath);
    return flushBufferToFile(filePath, buffer);
}

/* static */ bool DictFileWritingUtils::writeBufferToFileTail(FILE *const file,
        const BufferWithExtendableBuffer *const buffer) {
    uint8_t bufferSize[SIZE_OF_BUFFER_SIZE_FIELD];
    int writingPos = 0;
    ByteArrayUtils::writeUintAndAdvancePosition(bufferSize, buffer->getTailPosition(),
            SIZE_OF_BUFFER_SIZE_FIELD, &writingPos);
    if (fwrite(bufferSize, SIZE_OF_BUFFER_SIZE_FIELD, 1 /* count */, file) < 1) {
        return false;
    }
    return writeBufferToFile(file, buffer);
}

/* static */ bool DictFileWritingUtils::flushBufferToFile(const char *const filePath,
        const BufferWithExtendableBuffer *const buffer) {
    const int fd = open(filePath, O_WRONLY | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR);
    if (fd == -1) {
        AKLOGE("File %s cannot be opened. errno: %d", filePath, errno);
        ASSERT(false);
        return false;
    }
    FILE *const file = fdopen(fd, "wb");
    if (!file) {
        AKLOGE("fdopen failed for the file %s. errno: %d", filePath, errno);
        ASSERT(false);
        return false;
    }
    if (!writeBufferToFile(file, buffer)) {
        fclose(file);
        remove(filePath);
        AKLOGE("Buffer cannot be written to the file %s. size: %d", filePath,
                buffer->getTailPosition());
        ASSERT(false);
        return false;
    }
    fclose(file);
    return true;
}

// Returns whether the writing was succeeded or not.
/* static */ bool DictFileWritingUtils::writeBufferToFile(FILE *const file,
        const BufferWithExtendableBuffer *const buffer) {
    const int originalBufSize = buffer->getOriginalBufferSize();
    if (originalBufSize > 0 && fwrite(buffer->getBuffer(false /* usesAdditionalBuffer */),
            originalBufSize, 1, file) < 1) {
        return false;
    }
    const int additionalBufSize = buffer->getUsedAdditionalBufferSize();
    if (additionalBufSize > 0 && fwrite(buffer->getBuffer(true /* usesAdditionalBuffer */),
            additionalBufSize, 1, file) < 1) {
        return false;
    }
    return true;
}

} // namespace latinime
