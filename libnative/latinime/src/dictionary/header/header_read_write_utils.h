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

#ifndef LATINIME_HEADER_READ_WRITE_UTILS_H
#define LATINIME_HEADER_READ_WRITE_UTILS_H

#include <cstdint>

#include "defines.h"
#include "dictionary/interface/dictionary_header_structure_policy.h"
#include "dictionary/utils/format_utils.h"

namespace latinime {

class BufferWithExtendableBuffer;

class HeaderReadWriteUtils {
 public:
    typedef uint16_t DictionaryFlags;

    static int getHeaderSize(const uint8_t *const dictBuf);

    static DictionaryFlags getFlags(const uint8_t *const dictBuf);

    static AK_FORCE_INLINE int getHeaderOptionsPosition() {
        return HEADER_MAGIC_NUMBER_SIZE + HEADER_DICTIONARY_VERSION_SIZE + HEADER_FLAG_SIZE
                + HEADER_SIZE_FIELD_SIZE;
    }

    static DictionaryFlags createAndGetDictionaryFlagsUsingAttributeMap(
            const DictionaryHeaderStructurePolicy::AttributeMap *const attributeMap);

    static void fetchAllHeaderAttributes(const uint8_t *const dictBuf,
            DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes);

    static const int *readCodePointTable(
            DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes);

    static bool writeDictionaryVersion(BufferWithExtendableBuffer *const buffer,
            const FormatUtils::FORMAT_VERSION version, int *const writingPos);

    static bool writeDictionaryFlags(BufferWithExtendableBuffer *const buffer,
            const DictionaryFlags flags, int *const writingPos);

    static bool writeDictionaryHeaderSize(BufferWithExtendableBuffer *const buffer,
            const int size, int *const writingPos);

    static bool writeHeaderAttributes(BufferWithExtendableBuffer *const buffer,
            const DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            int *const writingPos);

    /**
     * Methods for header attributes.
     */
    static void setCodePointVectorAttribute(
            DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const char *const key, const std::vector<int> &value);

    static void setBoolAttribute(
            DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const char *const key, const bool value);

    static void setIntAttribute(
            DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const char *const key, const int value);

    static const std::vector<int> readCodePointVectorAttributeValue(
            const DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const char *const key);

    static bool readBoolAttributeValue(
            const DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const char *const key, const bool defaultValue);

    static int readIntAttributeValue(
            const DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const char *const key, const int defaultValue);

    static void insertCharactersIntoVector(const char *const characters,
            DictionaryHeaderStructurePolicy::AttributeMap::key_type *const key);

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(HeaderReadWriteUtils);

    static const int LARGEST_INT_DIGIT_COUNT;
    static const int MAX_ATTRIBUTE_KEY_LENGTH;
    static const int MAX_ATTRIBUTE_VALUE_LENGTH;

    static const int HEADER_MAGIC_NUMBER_SIZE;
    static const int HEADER_DICTIONARY_VERSION_SIZE;
    static const int HEADER_FLAG_SIZE;
    static const int HEADER_SIZE_FIELD_SIZE;

    static const char *const CODE_POINT_TABLE_KEY;

    // Value for the "flags" field. It's unused at the moment.
    static const DictionaryFlags NO_FLAGS;

    static void setIntAttributeInner(
            DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const DictionaryHeaderStructurePolicy::AttributeMap::key_type *const key,
            const int value);

    static int readIntAttributeValueInner(
            const DictionaryHeaderStructurePolicy::AttributeMap *const headerAttributes,
            const DictionaryHeaderStructurePolicy::AttributeMap::key_type *const key,
            const int defaultValue);
};
}
#endif /* LATINIME_HEADER_READ_WRITE_UTILS_H */
