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

#include <android/log.h>
#include <cerrno>
#include <cstring>
#include <fstream>
#include <iostream>
#include <thread>
#include <unistd.h>

#include "log.h"

void utils::log(int log_priority, const std::string &tag, const std::string &msg) {
    __android_log_print(log_priority, tag.c_str(), "%s", msg.c_str());
}

/**
 * Code below based on:
 *  https://codelab.wordpress.com/2014/11/03/how-to-use-standard-output-streams-for-logging-in-android-apps/
 */
int utils::start_stdout_stderr_logger(const std::string &app_name) {
    static bool already_started = false;
    if (already_started)
        return 0;

    int piperw[2];
    if (pipe(piperw) < 0) {
        std::string msg = "pipe(): ";
        msg += strerror(errno);
        utils::log(ANDROID_LOG_ERROR, "stdout/stderr logger", std::ref(msg));
        return 1;
    }

    /* make stdout line-buffered and stderr unbuffered */
    setvbuf(stdout, nullptr, _IOLBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    /* create the pipe and redirect stdout and stderr */
    dup2(piperw[0], STDIN_FILENO);
    dup2(piperw[1], STDOUT_FILENO);
    dup2(piperw[1], STDERR_FILENO);
    close(piperw[0]);
    close(piperw[1]);

    auto f = [](const std::string &tag) {
        std::string buf;
        while (std::getline(std::cin, buf)) {
            char &back = buf.back();
            if (back == '\n')
                back = '\0';
            utils::log(ANDROID_LOG_DEBUG, tag, std::ref(buf));
        }
    };

    /* spawn the logging thread */
    std::thread thr(f, app_name);
    thr.detach();

    already_started = true;
    return 0;
}
