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

#ifndef LATINIME_BIGRAM_LIST_READ_WRITE_UTILS_H
#define LATINIME_BIGRAM_LIST_READ_WRITE_UTILS_H

#include <cstdint>
#include <cstdlib>

#include "defines.h"
#include "utils/byte_array_view.h"

namespace latinime {

class BufferWithExtendableBuffer;

class BigramListReadWriteUtils {
public:
   typedef uint8_t BigramFlags;

   static bool getBigramEntryPropertiesAndAdvancePosition(const ReadOnlyByteArrayView buffer,
           BigramFlags *const outBigramFlags, int *const outTargetPtNodePos,
           int *const bigramEntryPos);

   static AK_FORCE_INLINE int getProbabilityFromFlags(const BigramFlags flags) {
       return flags & MASK_ATTRIBUTE_PROBABILITY;
   }

   static AK_FORCE_INLINE bool hasNext(const BigramFlags flags) {
       return (flags & FLAG_ATTRIBUTE_HAS_NEXT) != 0;
   }

   // Bigrams reading methods
   static bool skipExistingBigrams(const ReadOnlyByteArrayView buffer, int *const bigramListPos);

private:
   DISALLOW_IMPLICIT_CONSTRUCTORS(BigramListReadWriteUtils);

   static const BigramFlags MASK_ATTRIBUTE_ADDRESS_TYPE;
   static const BigramFlags FLAG_ATTRIBUTE_ADDRESS_TYPE_ONEBYTE;
   static const BigramFlags FLAG_ATTRIBUTE_ADDRESS_TYPE_TWOBYTES;
   static const BigramFlags FLAG_ATTRIBUTE_ADDRESS_TYPE_THREEBYTES;
   static const BigramFlags FLAG_ATTRIBUTE_OFFSET_NEGATIVE;
   static const BigramFlags FLAG_ATTRIBUTE_HAS_NEXT;
   static const BigramFlags MASK_ATTRIBUTE_PROBABILITY;

   static AK_FORCE_INLINE bool isOffsetNegative(const BigramFlags flags) {
       return (flags & FLAG_ATTRIBUTE_OFFSET_NEGATIVE) != 0;
   }

   static int getBigramAddressAndAdvancePosition(const ReadOnlyByteArrayView buffer,
           const BigramFlags flags, int *const pos);
};
} // namespace latinime
#endif // LATINIME_BIGRAM_LIST_READ_WRITE_UTILS_H
