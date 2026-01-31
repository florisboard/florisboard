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

#ifndef LATINIME_BYTE_ARRAY_UTILS_H
#define LATINIME_BYTE_ARRAY_UTILS_H

#include <cstdint>

#include "defines.h"

namespace latinime {

/**
 * Utility methods for reading byte arrays.
 */
class ByteArrayUtils {
 public:
    /**
     * Integer writing
     *
     * Each method write a corresponding size integer in a big endian manner.
     */
    static AK_FORCE_INLINE void writeUintAndAdvancePosition(uint8_t *const buffer,
            const uint32_t data, const int size, int *const pos) {
        // size must be in 1 to 4.
        ASSERT(size >= 1 && size <= 4);
        switch (size) {
            case 1:
                ByteArrayUtils::writeUint8AndAdvancePosition(buffer, data, pos);
                return;
            case 2:
                ByteArrayUtils::writeUint16AndAdvancePosition(buffer, data, pos);
                return;
            case 3:
                ByteArrayUtils::writeUint24AndAdvancePosition(buffer, data, pos);
                return;
            case 4:
                ByteArrayUtils::writeUint32AndAdvancePosition(buffer, data, pos);
                return;
            default:
                break;
        }
    }

    /**
     * Integer reading
     *
     * Each method read a corresponding size integer in a big endian manner.
     */
    static AK_FORCE_INLINE uint32_t readUint32(const uint8_t *const buffer, const int pos) {
        return (buffer[pos] << 24) ^ (buffer[pos + 1] << 16)
                ^ (buffer[pos + 2] << 8) ^ buffer[pos + 3];
    }

    static AK_FORCE_INLINE uint32_t readUint24(const uint8_t *const buffer, const int pos) {
        return (buffer[pos] << 16) ^ (buffer[pos + 1] << 8) ^ buffer[pos + 2];
    }

    static AK_FORCE_INLINE uint16_t readUint16(const uint8_t *const buffer, const int pos) {
        return (buffer[pos] << 8) ^ buffer[pos + 1];
    }

    static AK_FORCE_INLINE uint8_t readUint8(const uint8_t *const buffer, const int pos) {
        return buffer[pos];
    }

    static AK_FORCE_INLINE uint32_t readUint32AndAdvancePosition(
            const uint8_t *const buffer, int *const pos) {
        const uint32_t value = readUint32(buffer, *pos);
        *pos += 4;
        return value;
    }

    static AK_FORCE_INLINE int readSint24AndAdvancePosition(
            const uint8_t *const buffer, int *const pos) {
        const uint8_t value = readUint8(buffer, *pos);
        if (value < 0x80) {
            return readUint24AndAdvancePosition(buffer, pos);
        } else {
            (*pos)++;
            return -(((value & 0x7F) << 16) ^ readUint16AndAdvancePosition(buffer, pos));
        }
    }

    static AK_FORCE_INLINE uint32_t readUint24AndAdvancePosition(
            const uint8_t *const buffer, int *const pos) {
        const uint32_t value = readUint24(buffer, *pos);
        *pos += 3;
        return value;
    }

    static AK_FORCE_INLINE uint16_t readUint16AndAdvancePosition(
            const uint8_t *const buffer, int *const pos) {
        const uint16_t value = readUint16(buffer, *pos);
        *pos += 2;
        return value;
    }

    static AK_FORCE_INLINE uint8_t readUint8AndAdvancePosition(
            const uint8_t *const buffer, int *const pos) {
        return buffer[(*pos)++];
    }

    static AK_FORCE_INLINE uint32_t readUint(const uint8_t *const buffer,
            const int size, const int pos) {
        // size must be in 1 to 4.
        ASSERT(size >= 1 && size <= 4);
        switch (size) {
            case 1:
                return ByteArrayUtils::readUint8(buffer, pos);
            case 2:
                return ByteArrayUtils::readUint16(buffer, pos);
            case 3:
                return ByteArrayUtils::readUint24(buffer, pos);
            case 4:
                return ByteArrayUtils::readUint32(buffer, pos);
            default:
                return 0;
        }
    }

    /**
     * Code Point Reading
     *
     * 1 byte = bbbbbbbb match
     * case 000xxxxx: xxxxx << 16 + next byte << 8 + next byte
     * else: if 00011111 (= 0x1F) : this is the terminator. This is a relevant choice because
     *       unicode code points range from 0 to 0x10FFFF, so any 3-byte value starting with
     *       00011111 would be outside unicode.
     * else: iso-latin-1 code
     * This allows for the whole unicode range to be encoded, including chars outside of
     * the BMP. Also everything in the iso-latin-1 charset is only 1 byte, except control
     * characters which should never happen anyway (and still work, but take 3 bytes).
     */
    static AK_FORCE_INLINE int readCodePoint(const uint8_t *const buffer, const int pos) {
        int p = pos;
        return readCodePointAndAdvancePosition(buffer, nullptr /* codePointTable */, &p);
    }

    static AK_FORCE_INLINE int readCodePointAndAdvancePosition(
            const uint8_t *const buffer, const int *const codePointTable, int *const pos) {
        /*
         * codePointTable is an array to convert the most frequent characters in this dictionary to
         * 1 byte code points. It is only made of the original code points of the most frequent
         * characters used in this dictionary. 0x20 - 0xFF is used for the 1 byte characters.
         * The original code points are restored by picking the code points at the indices of the
         * codePointTable. The indices are calculated by subtracting 0x20 from the firstByte.
         */
        const uint8_t firstByte = readUint8(buffer, *pos);
        if (firstByte < MINIMUM_ONE_BYTE_CHARACTER_VALUE) {
            if (firstByte == CHARACTER_ARRAY_TERMINATOR) {
                *pos += 1;
                return NOT_A_CODE_POINT;
            } else {
                return readUint24AndAdvancePosition(buffer, pos);
            }
        } else {
            *pos += 1;
            if (codePointTable) {
                return codePointTable[firstByte - MINIMUM_ONE_BYTE_CHARACTER_VALUE];
            }
            return firstByte;
        }
    }

    /**
     * String (array of code points) Reading
     *
     * Reads code points until the terminator is found.
     */
    // Returns the length of the string.
    static int readStringAndAdvancePosition(const uint8_t *const buffer,
            const int maxLength, const int *const codePointTable, int *const outBuffer,
            int *const pos) {
        int length = 0;
        int codePoint = readCodePointAndAdvancePosition(buffer, codePointTable, pos);
        while (NOT_A_CODE_POINT != codePoint && length < maxLength) {
            outBuffer[length++] = codePoint;
            codePoint = readCodePointAndAdvancePosition(buffer, codePointTable, pos);
        }
        return length;
    }

    // Advances the position and returns the length of the string.
    static int advancePositionToBehindString(
            const uint8_t *const buffer, const int maxLength, int *const pos) {
        int length = 0;
        int codePoint = readCodePointAndAdvancePosition(buffer, nullptr /* codePointTable */, pos);
        while (NOT_A_CODE_POINT != codePoint && length < maxLength) {
            codePoint = readCodePointAndAdvancePosition(buffer, nullptr /* codePointTable */, pos);
            length++;
        }
        return length;
    }

    /**
     * String (array of code points) Writing
     */
    static void writeCodePointsAndAdvancePosition(uint8_t *const buffer,
            const int *const codePoints, const int codePointCount, const bool writesTerminator,
            int *const pos) {
        for (int i = 0; i < codePointCount; ++i) {
            const int codePoint = codePoints[i];
            if (codePoint == NOT_A_CODE_POINT || codePoint == CHARACTER_ARRAY_TERMINATOR) {
                break;
            } else if (codePoint < MINIMUM_ONE_BYTE_CHARACTER_VALUE
                    || codePoint > MAXIMUM_ONE_BYTE_CHARACTER_VALUE) {
                // three bytes character.
                writeUint24AndAdvancePosition(buffer, codePoint, pos);
            } else {
                // one byte character.
                writeUint8AndAdvancePosition(buffer, codePoint, pos);
            }
        }
        if (writesTerminator) {
            writeUint8AndAdvancePosition(buffer, CHARACTER_ARRAY_TERMINATOR, pos);
        }
    }

    static int calculateRequiredByteCountToStoreCodePoints(const int *const codePoints,
            const int codePointCount, const bool writesTerminator) {
        int byteCount = 0;
        for (int i = 0; i < codePointCount; ++i) {
            const int codePoint = codePoints[i];
            if (codePoint == NOT_A_CODE_POINT || codePoint == CHARACTER_ARRAY_TERMINATOR) {
                break;
            } else if (codePoint < MINIMUM_ONE_BYTE_CHARACTER_VALUE
                    || codePoint > MAXIMUM_ONE_BYTE_CHARACTER_VALUE) {
                // three bytes character.
                byteCount += 3;
            } else {
                // one byte character.
                byteCount += 1;
            }
        }
        if (writesTerminator) {
            // The terminator is one byte.
            byteCount += 1;
        }
        return byteCount;
    }

 private:
    DISALLOW_IMPLICIT_CONSTRUCTORS(ByteArrayUtils);

    static const uint8_t MINIMUM_ONE_BYTE_CHARACTER_VALUE;
    static const uint8_t MAXIMUM_ONE_BYTE_CHARACTER_VALUE;
    static const uint8_t CHARACTER_ARRAY_TERMINATOR;

    static AK_FORCE_INLINE void writeUint32AndAdvancePosition(uint8_t *const buffer,
            const uint32_t data, int *const pos) {
        buffer[(*pos)++] = (data >> 24) & 0xFF;
        buffer[(*pos)++] = (data >> 16) & 0xFF;
        buffer[(*pos)++] = (data >> 8) & 0xFF;
        buffer[(*pos)++] = data & 0xFF;
    }

    static AK_FORCE_INLINE void writeUint24AndAdvancePosition(uint8_t *const buffer,
            const uint32_t data, int *const pos) {
        buffer[(*pos)++] = (data >> 16) & 0xFF;
        buffer[(*pos)++] = (data >> 8) & 0xFF;
        buffer[(*pos)++] = data & 0xFF;
    }

    static AK_FORCE_INLINE void writeUint16AndAdvancePosition(uint8_t *const buffer,
            const uint16_t data, int *const pos) {
        buffer[(*pos)++] = (data >> 8) & 0xFF;
        buffer[(*pos)++] = data & 0xFF;
    }

    static AK_FORCE_INLINE void writeUint8AndAdvancePosition(uint8_t *const buffer,
            const uint8_t data, int *const pos) {
        buffer[(*pos)++] = data & 0xFF;
    }
};
} // namespace latinime
#endif /* LATINIME_BYTE_ARRAY_UTILS_H */
