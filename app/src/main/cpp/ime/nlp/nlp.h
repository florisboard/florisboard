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

#ifndef FLORISBOARD_NLP_H
#define FLORISBOARD_NLP_H

#include <string>

namespace ime::nlp {

typedef std::string word_t;
typedef int16_t freq_t;

const freq_t FREQ_MIN =                 0x00;
const freq_t FREQ_MAX =                 0xFF;
const freq_t FREQ_POSSIBLY_OFFENSIVE =  0x01;

} // namespace ime::nlp

#endif // FLORISBOARD_NLP_H
