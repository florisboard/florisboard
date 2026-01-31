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

#include "dictionary/structure/v4/ver4_dict_buffers.h"

#include <cerrno>
#include <cstring>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <vector>

#include "dictionary/utils/byte_array_utils.h"
#include "dictionary/utils/dict_file_writing_utils.h"
#include "dictionary/utils/file_utils.h"
#include "utils/byte_array_view.h"

namespace latinime {

/* static */ Ver4DictBuffers::Ver4DictBuffersPtr Ver4DictBuffers::openVer4DictBuffers(
        const char *const dictPath, MmappedBuffer::MmappedBufferPtr &&headerBuffer,
        const FormatUtils::FORMAT_VERSION formatVersion) {
    if (!headerBuffer) {
        ASSERT(false);
        AKLOGE("The header buffer must be valid to open ver4 dict buffers.");
        return Ver4DictBuffersPtr(nullptr);
    }
    // TODO: take only dictDirPath, and open both header and trie files in the constructor below
    const bool isUpdatable = headerBuffer->isUpdatable();
    MmappedBuffer::MmappedBufferPtr bodyBuffer = MmappedBuffer::openBuffer(dictPath,
            Ver4DictConstants::BODY_FILE_EXTENSION, isUpdatable);
    if (!bodyBuffer) {
        return Ver4DictBuffersPtr(nullptr);
    }
    std::vector<ReadWriteByteArrayView> buffers;
    const ReadWriteByteArrayView buffer = bodyBuffer->getReadWriteByteArrayView();
    int position = 0;
    while (position < static_cast<int>(buffer.size())) {
        const int bufferSize = ByteArrayUtils::readUint32AndAdvancePosition(
                buffer.data(), &position);
        buffers.push_back(buffer.subView(position, bufferSize));
        position += bufferSize;
        if (bufferSize < 0 || position < 0 || position > static_cast<int>(buffer.size())) {
            AKLOGE("The dict body file is corrupted.");
            return Ver4DictBuffersPtr(nullptr);
        }
    }
    if (buffers.size() != Ver4DictConstants::NUM_OF_CONTENT_BUFFERS_IN_BODY_FILE) {
        AKLOGE("The dict body file is corrupted.");
        return Ver4DictBuffersPtr(nullptr);
    }
    return Ver4DictBuffersPtr(new Ver4DictBuffers(std::move(headerBuffer), std::move(bodyBuffer),
            formatVersion, buffers));
}

bool Ver4DictBuffers::flushHeaderAndDictBuffers(const char *const dictDirPath,
        const BufferWithExtendableBuffer *const headerBuffer) const {
    // Create temporary directory.
    const int tmpDirPathBufSize = FileUtils::getFilePathWithSuffixBufSize(dictDirPath,
            DictFileWritingUtils::TEMP_FILE_SUFFIX_FOR_WRITING_DICT_FILE);
    char tmpDirPath[tmpDirPathBufSize];
    FileUtils::getFilePathWithSuffix(dictDirPath,
            DictFileWritingUtils::TEMP_FILE_SUFFIX_FOR_WRITING_DICT_FILE, tmpDirPathBufSize,
            tmpDirPath);
    if (FileUtils::existsDir(tmpDirPath)) {
        if (!FileUtils::removeDirAndFiles(tmpDirPath)) {
            AKLOGE("Existing directory %s cannot be removed.", tmpDirPath);
            ASSERT(false);
            return false;
        }
    }
    umask(S_IWGRP | S_IWOTH);
    if (mkdir(tmpDirPath, S_IRWXU) == -1) {
        AKLOGE("Cannot create directory: %s. errno: %d.", tmpDirPath, errno);
        return false;
    }
    // Get dictionary base path.
    const int dictNameBufSize = strlen(dictDirPath) + 1 /* terminator */;
    char dictName[dictNameBufSize];
    FileUtils::getBasename(dictDirPath, dictNameBufSize, dictName);
    const int dictPathBufSize = FileUtils::getFilePathBufSize(tmpDirPath, dictName);
    char dictPath[dictPathBufSize];
    FileUtils::getFilePath(tmpDirPath, dictName, dictPathBufSize, dictPath);

    // Write header file.
    if (!DictFileWritingUtils::flushBufferToFileWithSuffix(dictPath,
            Ver4DictConstants::HEADER_FILE_EXTENSION, headerBuffer)) {
        AKLOGE("Dictionary header file %s%s cannot be written.", tmpDirPath,
                Ver4DictConstants::HEADER_FILE_EXTENSION);
        return false;
    }

    // Write body file.
    const int bodyFilePathBufSize = FileUtils::getFilePathWithSuffixBufSize(dictPath,
            Ver4DictConstants::BODY_FILE_EXTENSION);
    char bodyFilePath[bodyFilePathBufSize];
    FileUtils::getFilePathWithSuffix(dictPath, Ver4DictConstants::BODY_FILE_EXTENSION,
            bodyFilePathBufSize, bodyFilePath);

    const int fd = open(bodyFilePath, O_WRONLY | O_CREAT | O_EXCL, S_IRUSR | S_IWUSR);
    if (fd == -1) {
        AKLOGE("File %s cannot be opened. errno: %d", bodyFilePath, errno);
        ASSERT(false);
        return false;
    }
    FILE *const file = fdopen(fd, "wb");
    if (!file) {
        AKLOGE("fdopen failed for the file %s. errno: %d", bodyFilePath, errno);
        ASSERT(false);
        return false;
    }

    if (!flushDictBuffers(file)) {
        fclose(file);
        return false;
    }
    fclose(file);
    // Remove existing dictionary.
    if (!FileUtils::removeDirAndFiles(dictDirPath)) {
        AKLOGE("Existing directory %s cannot be removed.", dictDirPath);
        ASSERT(false);
        return false;
    }
    // Rename temporary directory.
    if (rename(tmpDirPath, dictDirPath) != 0) {
        AKLOGE("%s cannot be renamed to %s", tmpDirPath, dictDirPath);
        ASSERT(false);
        return false;
    }
    return true;
}

bool Ver4DictBuffers::flushDictBuffers(FILE *const file) const {
    // Write trie.
    if (!DictFileWritingUtils::writeBufferToFileTail(file, &mExpandableTrieBuffer)) {
        AKLOGE("Trie cannot be written.");
        return false;
    }
    // Write terminal position lookup table.
    if (!mTerminalPositionLookupTable.flushToFile(file)) {
        AKLOGE("Terminal position lookup table cannot be written.");
        return false;
    }
    // Write language model content.
    if (!mLanguageModelDictContent.save(file)) {
        AKLOGE("Language model dict content cannot be written.");
        return false;
    }
    // Write shortcut dict content.
    if (!mShortcutDictContent.flushToFile(file)) {
        AKLOGE("Shortcut dict content cannot be written.");
        return false;
    }
    return true;
}

Ver4DictBuffers::Ver4DictBuffers(MmappedBuffer::MmappedBufferPtr &&headerBuffer,
        MmappedBuffer::MmappedBufferPtr &&bodyBuffer,
        const FormatUtils::FORMAT_VERSION formatVersion,
        const std::vector<ReadWriteByteArrayView> &contentBuffers)
        : mHeaderBuffer(std::move(headerBuffer)), mDictBuffer(std::move(bodyBuffer)),
          mHeaderPolicy(mHeaderBuffer->getReadOnlyByteArrayView().data(), formatVersion),
          mExpandableHeaderBuffer(mHeaderBuffer->getReadWriteByteArrayView(),
                  BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE),
          mExpandableTrieBuffer(contentBuffers[Ver4DictConstants::TRIE_BUFFER_INDEX],
                  BufferWithExtendableBuffer::DEFAULT_MAX_ADDITIONAL_BUFFER_SIZE),
          mTerminalPositionLookupTable(
                  contentBuffers[Ver4DictConstants::TERMINAL_ADDRESS_LOOKUP_TABLE_BUFFER_INDEX]),
          mLanguageModelDictContent(&contentBuffers[Ver4DictConstants::LANGUAGE_MODEL_BUFFER_INDEX],
                  mHeaderPolicy.hasHistoricalInfoOfWords()),
          mShortcutDictContent(&contentBuffers[Ver4DictConstants::SHORTCUT_BUFFERS_INDEX]),
          mIsUpdatable(mDictBuffer->isUpdatable()) {}

Ver4DictBuffers::Ver4DictBuffers(const HeaderPolicy *const headerPolicy, const int maxTrieSize)
        : mHeaderBuffer(nullptr), mDictBuffer(nullptr), mHeaderPolicy(headerPolicy),
          mExpandableHeaderBuffer(Ver4DictConstants::MAX_DICTIONARY_SIZE),
          mExpandableTrieBuffer(maxTrieSize), mTerminalPositionLookupTable(),
          mLanguageModelDictContent(headerPolicy->hasHistoricalInfoOfWords()),
          mShortcutDictContent(),  mIsUpdatable(true) {}

} // namespace latinime
