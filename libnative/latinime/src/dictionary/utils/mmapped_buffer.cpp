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

#include "dictionary/utils/mmapped_buffer.h"

#include <cerrno>
#include <climits>
#include <cstdio>
#include <fcntl.h>
#include <sys/mman.h>
#include <unistd.h>

#include "dictionary/utils/file_utils.h"

namespace latinime {

/* static */ MmappedBuffer::MmappedBufferPtr MmappedBuffer::openBuffer(
        const char *const path, const int bufferOffset, const int bufferSize,
        const bool isUpdatable) {
    const int mmapFd = open(path, O_RDONLY);
    if (mmapFd < 0) {
        AKLOGE("DICT: Can't open the source. path=%s errno=%d", path, errno);
        return nullptr;
    }
    const int pagesize = sysconf(_SC_PAGESIZE);
    const int offset = bufferOffset % pagesize;
    int alignedOffset = bufferOffset - offset;
    int alignedSize = bufferSize + offset;
    const int protMode = isUpdatable ? PROT_READ | PROT_WRITE : PROT_READ;
    void *const mmappedBuffer = mmap(0, alignedSize, protMode, MAP_PRIVATE, mmapFd,
            alignedOffset);
    if (mmappedBuffer == MAP_FAILED) {
        AKLOGE("DICT: Can't mmap dictionary. errno=%d", errno);
        close(mmapFd);
        return nullptr;
    }
    uint8_t *const buffer = static_cast<uint8_t *>(mmappedBuffer) + offset;
    if (!buffer) {
        AKLOGE("DICT: buffer is null");
        close(mmapFd);
        return nullptr;
    }
    return MmappedBufferPtr(new MmappedBuffer(buffer, bufferSize, mmappedBuffer, alignedSize,
            mmapFd, isUpdatable));
}

/* static */ MmappedBuffer::MmappedBufferPtr MmappedBuffer::openBuffer(
        const char *const path, const bool isUpdatable) {
    const int fileSize = FileUtils::getFileSize(path);
    if (fileSize == -1) {
        return nullptr;
    } else if (fileSize == 0) {
        return MmappedBufferPtr(new MmappedBuffer(isUpdatable));
    } else {
        return openBuffer(path, 0 /* bufferOffset */, fileSize, isUpdatable);
    }
}

/* static */ MmappedBuffer::MmappedBufferPtr MmappedBuffer::openBuffer(
        const char *const dirPath, const char *const fileName, const bool isUpdatable) {
    const int filePathBufferSize = PATH_MAX + 1 /* terminator */;
    char filePath[filePathBufferSize];
    const int filePathLength = snprintf(filePath, filePathBufferSize, "%s%s", dirPath,
            fileName);
    if (filePathLength >= filePathBufferSize) {
        return nullptr;
    }
    return openBuffer(filePath, isUpdatable);
}

MmappedBuffer::~MmappedBuffer() {
    if (mAlignedSize == 0) {
        return;
    }
    int ret = munmap(mMmappedBuffer, mAlignedSize);
    if (ret != 0) {
        AKLOGE("DICT: Failure in munmap. ret=%d errno=%d", ret, errno);
    }
    ret = close(mMmapFd);
    if (ret != 0) {
        AKLOGE("DICT: Failure in close. ret=%d errno=%d", ret, errno);
    }
}

} // namespace latinime
