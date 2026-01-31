/*
 * Copyright (C) 2010 The Android Open Source Project
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

#ifndef LATINIME_DEFINES_H
#define LATINIME_DEFINES_H

#include <cstdint>

#ifdef __GNUC__
#define AK_FORCE_INLINE __attribute__((always_inline)) __inline__
#else // __GNUC__
#define AK_FORCE_INLINE inline
#endif // __GNUC__

#if defined(FLAG_DBG)
#undef AK_FORCE_INLINE
#define AK_FORCE_INLINE inline
#endif // defined(FLAG_DBG)

// Must be equal to Constants.Dictionary.MAX_WORD_LENGTH in Java
#define MAX_WORD_LENGTH 48
// Must be equal to BinaryDictionary.MAX_RESULTS in Java
#define MAX_RESULTS 18
// Must be equal to ProximityInfo.MAX_PROXIMITY_CHARS_SIZE in Java
#define MAX_PROXIMITY_CHARS_SIZE 16
#define ADDITIONAL_PROXIMITY_CHAR_DELIMITER_CODE 2

// TODO: Use size_t instead of int.
// Disclaimer: You will see a compile error if you use this macro against a variable-length array.
// Sorry for the inconvenience. It isn't supported.
template <typename T, int N>
char (&ArraySizeHelper(T (&array)[N]))[N];
#define NELEMS(x) (sizeof(ArraySizeHelper(x)))

AK_FORCE_INLINE static int intArrayToCharArray(const int *const source, const int sourceSize,
        char *dest, const int destSize) {
    // We want to always terminate with a 0 char, so stop one short of the length to make
    // sure there is room.
    const int destLimit = destSize - 1;
    int si = 0;
    int di = 0;
    while (si < sourceSize && di < destLimit && 0 != source[si]) {
        const uint32_t codePoint = static_cast<uint32_t>(source[si++]);
        if (codePoint < 0x7F) { // One byte
            dest[di++] = codePoint;
        } else if (codePoint < 0x7FF) { // Two bytes
            if (di + 1 >= destLimit) break;
            dest[di++] = 0xC0 + (codePoint >> 6);
            dest[di++] = 0x80 + (codePoint & 0x3F);
        } else if (codePoint < 0xFFFF) { // Three bytes
            if (di + 2 >= destLimit) break;
            dest[di++] = 0xE0 + (codePoint >> 12);
            dest[di++] = 0x80 + ((codePoint >> 6) & 0x3F);
            dest[di++] = 0x80 + (codePoint & 0x3F);
        } else if (codePoint <= 0x1FFFFF) { // Four bytes
            if (di + 3 >= destLimit) break;
            dest[di++] = 0xF0 + (codePoint >> 18);
            dest[di++] = 0x80 + ((codePoint >> 12) & 0x3F);
            dest[di++] = 0x80 + ((codePoint >> 6) & 0x3F);
            dest[di++] = 0x80 + (codePoint & 0x3F);
        } else if (codePoint <= 0x3FFFFFF) { // Five bytes
            if (di + 4 >= destLimit) break;
            dest[di++] = 0xF8 + (codePoint >> 24);
            dest[di++] = 0x80 + ((codePoint >> 18) & 0x3F);
            dest[di++] = 0x80 + ((codePoint >> 12) & 0x3F);
            dest[di++] = 0x80 + ((codePoint >> 6) & 0x3F);
            dest[di++] = codePoint & 0x3F;
        } else if (codePoint <= 0x7FFFFFFF) { // Six bytes
            if (di + 5 >= destLimit) break;
            dest[di++] = 0xFC + (codePoint >> 30);
            dest[di++] = 0x80 + ((codePoint >> 24) & 0x3F);
            dest[di++] = 0x80 + ((codePoint >> 18) & 0x3F);
            dest[di++] = 0x80 + ((codePoint >> 12) & 0x3F);
            dest[di++] = 0x80 + ((codePoint >> 6) & 0x3F);
            dest[di++] = codePoint & 0x3F;
        } else {
            // Not a code point... skip.
        }
    }
    dest[di] = 0;
    return di;
}

#if defined(FLAG_DO_PROFILE) || defined(FLAG_DBG)
#if defined(__ANDROID__)
#include <android/log.h>
#endif // defined(__ANDROID__)
#ifndef LOG_TAG
#define LOG_TAG "LatinIME: "
#endif // LOG_TAG

#if defined(HOST_TOOL)
#include <stdio.h>
#define AKLOGE(fmt, ...) printf(fmt "\n", ##__VA_ARGS__)
#define AKLOGI(fmt, ...) printf(fmt "\n", ##__VA_ARGS__)
#else // defined(HOST_TOOL)
#define AKLOGE(fmt, ...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, fmt, ##__VA_ARGS__)
#define AKLOGI(fmt, ...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, fmt, ##__VA_ARGS__)
#endif // defined(HOST_TOOL)

#define DUMP_SUGGESTION(words, frequencies, index, score) \
        do { dumpWordInfo(words, frequencies, index, score); } while (0)
#define DUMP_WORD(word, length) do { dumpWord(word, length); } while (0)
#define INTS_TO_CHARS(input, length, output, outlength) do { \
        intArrayToCharArray(input, length, output, outlength); } while (0)

static inline void dumpWordInfo(const int *word, const int length, const int rank,
        const int probability) {
    static char charBuf[50];
    const int N = intArrayToCharArray(word, length, charBuf, NELEMS(charBuf));
    if (N > 0) {
        AKLOGI("%2d [ %s ] (%d)", rank, charBuf, probability);
    }
}

static AK_FORCE_INLINE void dumpWord(const int *word, const int length) {
    static char charBuf[50];
    const int N = intArrayToCharArray(word, length, charBuf, NELEMS(charBuf));
    if (N > 1) {
        AKLOGI("[ %s ]", charBuf);
    }
}

#ifndef __ANDROID__
#include <cassert>
#include <execinfo.h>
#include <stdlib.h>

#define DO_ASSERT_TEST
#define ASSERT(success) do { if (!(success)) { showStackTrace(); assert(success);} } while (0)
#define SHOW_STACK_TRACE do { showStackTrace(); } while (0)

static inline void showStackTrace() {
    void *callstack[128];
    int i, frames = backtrace(callstack, 128);
    char **strs = backtrace_symbols(callstack, frames);
    for (i = 0; i < frames; ++i) {
        if (i == 0) {
            AKLOGI("=== Trace ===");
            continue;
        }
        AKLOGI("%s", strs[i]);
    }
    free(strs);
}
#else // __ANDROID__
#include <cassert>
#define DO_ASSERT_TEST
#define ASSERT(success) assert(success)
#define SHOW_STACK_TRACE
#endif // __ANDROID__

#else // defined(FLAG_DO_PROFILE) || defined(FLAG_DBG)
#define AKLOGE(fmt, ...)
#define AKLOGI(fmt, ...)
#define DUMP_SUGGESTION(words, frequencies, index, score)
#define DUMP_WORD(word, length)
#undef DO_ASSERT_TEST
#define ASSERT(success)
#define SHOW_STACK_TRACE
#define INTS_TO_CHARS(input, length, output)
#endif // defined(FLAG_DO_PROFILE) || defined(FLAG_DBG)

#ifdef FLAG_DBG
#define DEBUG_DICT true
#define DEBUG_DICT_FULL false
#define DEBUG_EDIT_DISTANCE false
#define DEBUG_NODE DEBUG_DICT_FULL
#define DEBUG_TRACE DEBUG_DICT_FULL
#define DEBUG_PROXIMITY_INFO false
#define DEBUG_PROXIMITY_CHARS false
#define DEBUG_CORRECTION false
#define DEBUG_CORRECTION_FREQ false
#define DEBUG_SAMPLING_POINTS false
#define DEBUG_POINTS_PROBABILITY false
#define DEBUG_DOUBLE_LETTER false
#define DEBUG_CACHE false
#define DEBUG_DUMP_ERROR false
#define DEBUG_EVALUATE_MOST_PROBABLE_STRING false

#ifdef FLAG_FULL_DBG
#define DEBUG_GEO_FULL true
#else
#define DEBUG_GEO_FULL false
#endif

#else // FLAG_DBG

#define DEBUG_DICT false
#define DEBUG_DICT_FULL false
#define DEBUG_EDIT_DISTANCE false
#define DEBUG_NODE false
#define DEBUG_TRACE false
#define DEBUG_PROXIMITY_INFO false
#define DEBUG_PROXIMITY_CHARS false
#define DEBUG_CORRECTION false
#define DEBUG_CORRECTION_FREQ false
#define DEBUG_SAMPLING_POINTS false
#define DEBUG_POINTS_PROBABILITY false
#define DEBUG_DOUBLE_LETTER false
#define DEBUG_CACHE false
#define DEBUG_DUMP_ERROR false
#define DEBUG_EVALUATE_MOST_PROBABLE_STRING false

#define DEBUG_GEO_FULL false

#endif // FLAG_DBG

#ifndef S_INT_MAX
#define S_INT_MAX 2147483647 // ((1 << 31) - 1)
#endif
#ifndef S_INT_MIN
// The literal constant -2147483648 does not work in C prior C90, because
// the compiler tries to fit the positive number into an int and then negate it.
// GCC warns about this.
#define S_INT_MIN (-2147483647 - 1) // -(1 << 31)
#endif

#define M_PI_F 3.14159265f
#define MAX_PERCENTILE 100

#define NOT_A_CODE_POINT (-1)
#define NOT_A_DISTANCE (-1)
#define NOT_A_COORDINATE (-1)
#define NOT_AN_INDEX (-1)
#define NOT_A_PROBABILITY (-1)
#define NOT_A_DICT_POS (S_INT_MIN)
#define NOT_A_WORD_ID (S_INT_MIN)
#define NOT_A_TIMESTAMP (-1)
#define NOT_A_WEIGHT_OF_LANG_MODEL_VS_SPATIAL_MODEL (-1.0f)

// A special value to mean the first word confidence makes no sense in this case,
// e.g. this is not a multi-word suggestion.
#define NOT_A_FIRST_WORD_CONFIDENCE (S_INT_MIN)
// How high the confidence needs to be for us to auto-commit. Arbitrary.
// This needs to be the same as CONFIDENCE_FOR_AUTO_COMMIT in BinaryDictionary.java
#define CONFIDENCE_FOR_AUTO_COMMIT (1000000)
// 80% of the full confidence
#define DISTANCE_WEIGHT_FOR_AUTO_COMMIT (80 * CONFIDENCE_FOR_AUTO_COMMIT / 100)
// 100% of the full confidence
#define LENGTH_WEIGHT_FOR_AUTO_COMMIT (CONFIDENCE_FOR_AUTO_COMMIT)
// 80% of the full confidence
#define SPACE_COUNT_WEIGHT_FOR_AUTO_COMMIT (80 * CONFIDENCE_FOR_AUTO_COMMIT / 100)

#define KEYCODE_SPACE ' '
#define KEYCODE_SINGLE_QUOTE '\''
#define KEYCODE_HYPHEN_MINUS '-'
// Code point to indicate beginning-of-sentence. This is not in the code point space of unicode.
#define CODE_POINT_BEGINNING_OF_SENTENCE 0x110000

#define SUGGEST_INTERFACE_OUTPUT_SCALE 1000000.0f
#define MAX_PROBABILITY 255
#define MAX_BIGRAM_ENCODED_PROBABILITY 15

// Max value for length, distance and probability which are used in weighting
// TODO: Remove
#define MAX_VALUE_FOR_WEIGHTING 10000000

// The max number of the keys in one keyboard layout
#define MAX_KEY_COUNT_IN_A_KEYBOARD 64

// TODO: Remove
#define MAX_POINTER_COUNT 1
#define MAX_POINTER_COUNT_G 2

// (MAX_PREV_WORD_COUNT_FOR_N_GRAM + 1)-gram is supported.
#define MAX_PREV_WORD_COUNT_FOR_N_GRAM 3

#define DISALLOW_DEFAULT_CONSTRUCTOR(TypeName) \
  TypeName() = delete

#define DISALLOW_COPY_CONSTRUCTOR(TypeName) \
  TypeName(const TypeName&) = delete

#define DISALLOW_ASSIGNMENT_OPERATOR(TypeName)
  //void operator=(const TypeName&) = delete

#define DISALLOW_COPY_AND_ASSIGN(TypeName) \
  DISALLOW_COPY_CONSTRUCTOR(TypeName);     \
  DISALLOW_ASSIGNMENT_OPERATOR(TypeName)

#define DISALLOW_IMPLICIT_CONSTRUCTORS(TypeName) \
  DISALLOW_DEFAULT_CONSTRUCTOR(TypeName);        \
  DISALLOW_COPY_AND_ASSIGN(TypeName)

// Used as a return value for character comparison
typedef enum {
    // Same char, possibly with different case or accent
    MATCH_CHAR,
    // It is a char located nearby on the keyboard
    PROXIMITY_CHAR,
    // Additional proximity char which can differ by language.
    ADDITIONAL_PROXIMITY_CHAR,
    // It is a substitution char
    SUBSTITUTION_CHAR,
    // It is an unrelated char
    UNRELATED_CHAR,
} ProximityType;

typedef enum {
    NOT_A_DOUBLE_LETTER,
    A_DOUBLE_LETTER,
    A_STRONG_DOUBLE_LETTER
} DoubleLetterLevel;

typedef enum {
    // Correction for MATCH_CHAR
    CT_MATCH,
    // Correction for PROXIMITY_CHAR
    CT_PROXIMITY,
    // Correction for ADDITIONAL_PROXIMITY_CHAR
    CT_ADDITIONAL_PROXIMITY,
    // Correction for SUBSTITUTION_CHAR
    CT_SUBSTITUTION,
    // Skip one omitted letter
    CT_OMISSION,
    // Delete an unnecessarily inserted letter
    CT_INSERTION,
    // Swap the order of next two touch points
    CT_TRANSPOSITION,
    CT_COMPLETION,
    CT_TERMINAL,
    CT_TERMINAL_INSERTION,
    // Create new word with space omission
    CT_NEW_WORD_SPACE_OMISSION,
    // Create new word with space substitution
    CT_NEW_WORD_SPACE_SUBSTITUTION,
} CorrectionType;
#endif // LATINIME_DEFINES_H
