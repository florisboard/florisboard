/*
 * Copyright (C) 2021 Patrick Goldinger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "token.h"

#include <utility>

using namespace ime::nlp;

Token::Token(word_t _data) : data(std::move(_data)) {}

bool ime::nlp::operator==(const Token &t1, const Token &t2) {
    return t1.data == t2.data;
}

bool ime::nlp::operator!=(const Token &t1, const Token &t2) {
    return t1.data != t2.data;
}

WeightedToken::WeightedToken(word_t _data, freq_t _freq) : Token(std::move(_data)), freq(_freq) {}

bool ime::nlp::operator==(const WeightedToken &t1, const WeightedToken &t2) {
    return t1.data == t2.data && t1.freq == t2.freq;
}

bool ime::nlp::operator!=(const WeightedToken &t1, const WeightedToken &t2) {
    return t1.data != t2.data || t1.freq != t2.freq;
}

bool ime::nlp::operator<(const WeightedToken &t1, const WeightedToken &t2) {
    return t1.freq < t2.freq;
}

bool ime::nlp::operator<=(const WeightedToken &t1, const WeightedToken &t2) {
    return t1.freq <= t2.freq;
}

bool ime::nlp::operator>(const WeightedToken &t1, const WeightedToken &t2) {
    return t1.freq > t2.freq;
}

bool ime::nlp::operator>=(const WeightedToken &t1, const WeightedToken &t2) {
    return t1.freq >= t2.freq;
}
