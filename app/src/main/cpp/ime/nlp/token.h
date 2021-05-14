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

#ifndef FLORISBOARD_TOKEN_H
#define FLORISBOARD_TOKEN_H

#include "nlp.h"
#include <string>

namespace ime::nlp {

class Token {
public:
    word_t data;
    Token();
    Token(word_t &&_data);

    friend bool operator==(const Token &t1, const Token &t2);
    friend bool operator!=(const Token &t1, const Token &t2);
};

class WeightedToken : public Token {
public:
    freq_t freq;
    WeightedToken();
    WeightedToken(word_t &&_data, freq_t _freq);

    friend bool operator==(const WeightedToken &t1, const WeightedToken &t2);
    friend bool operator!=(const WeightedToken &t1, const WeightedToken &t2);
    friend bool operator<(const WeightedToken &t1, const WeightedToken &t2);
    friend bool operator<=(const WeightedToken &t1, const WeightedToken &t2);
    friend bool operator>(const WeightedToken &t1, const WeightedToken &t2);
    friend bool operator>=(const WeightedToken &t1, const WeightedToken &t2);
};

} // namespace ime::nlp

#endif // FLORISBOARD_TOKEN_H
