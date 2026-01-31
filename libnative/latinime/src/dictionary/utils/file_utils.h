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

#ifndef LATINIME_FILE_UTILS_H
#define LATINIME_FILE_UTILS_H

#include "defines.h"

namespace latinime {

class FileUtils {
 public:
    // Returns -1 on error.
    static int getFileSize(const char *const filePath);

    static bool existsDir(const char *const dirPath);

    // Remove a directory and all files in the directory.
    static bool removeDirAndFiles(const char *const dirPath);

    static int getFilePathWithSuffixBufSize(const char *const filePath, const char *const suffix);

    static void getFilePathWithSuffix(const char *const filePath, const char *const suffix,
            const int filePathBufSize, char *const outFilePath);

    static int getFilePathBufSize(const char *const dirPath, const char *const fileName);

    static void getFilePath(const char *const dirPath, const char *const fileName,
            const int filePathBufSize, char *const outFilePath);

    // Returns whether the filePath have the suffix.
    static bool getFilePathWithoutSuffix(const char *const filePath, const char *const suffix,
            const int dirPathBufSize, char *const outDirPath);

    static void getDirPath(const char *const filePath, const int dirPathBufSize,
            char *const outDirPath);

    static void getBasename(const char *const filePath, const int outNameBufSize,
            char *const outName);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(FileUtils);

    static bool removeDirAndFiles(const char *const dirPath, const int maxTries);
};
} // namespace latinime
#endif /* LATINIME_FILE_UTILS_H */
