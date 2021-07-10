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

#ifndef FLORISBOARD_LOG_H
#define FLORISBOARD_LOG_H

#include <string>

namespace utils {

void log_debug(const std::string& tag, const std::string& msg);
void log_info(const std::string& tag, const std::string& msg);
void log_warning(const std::string& tag, const std::string& msg);
void log_error(const std::string& tag, const std::string& msg);
void log_wtf(const std::string& tag, const std::string& msg);

int start_stdout_stderr_logger(const char *app_name);

} // namespace utils

#endif // FLORISBOARD_LOG_H
