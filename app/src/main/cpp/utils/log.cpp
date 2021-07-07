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
#include <unistd.h>
#include "log.h"

void utils::log_debug(const std::string &tag, const std::string &msg) {
    __android_log_print(ANDROID_LOG_DEBUG, tag.c_str(), "%s", msg.c_str());
}

void utils::log_info(const std::string &tag, const std::string &msg) {
    __android_log_print(ANDROID_LOG_INFO, tag.c_str(), "%s", msg.c_str());
}

void utils::log_warning(const std::string &tag, const std::string &msg) {
    __android_log_print(ANDROID_LOG_WARN, tag.c_str(), "%s", msg.c_str());
}

void utils::log_error(const std::string &tag, const std::string &msg) {
    __android_log_print(ANDROID_LOG_ERROR, tag.c_str(), "%s", msg.c_str());
}

void utils::log_wtf(const std::string &tag, const std::string &msg) {
    __android_log_print(ANDROID_LOG_FATAL, tag.c_str(), "%s", msg.c_str());
}

/**
 * Code below taken from here:
 *  https://codelab.wordpress.com/2014/11/03/how-to-use-standard-output-streams-for-logging-in-android-apps/
 */
static int pfd[2];
static pthread_t thr;
static const char *tag = "myapp";
static bool already_started = false;

static void *thread_func(void*) {
    ssize_t rdsz;
    char buf[2048];
    while ((rdsz = read(pfd[0], buf, sizeof buf - 1)) > 0) {
        if (buf[rdsz - 1] == '\n') --rdsz;
        buf[rdsz] = 0;  /* add null-terminator */
        __android_log_write(ANDROID_LOG_DEBUG, tag, buf);
    }
    return nullptr;
}

int utils::start_stdout_stderr_logger(const char *app_name) {
    if (already_started) return 0;
    already_started = true;
    tag = app_name;

    /* make stdout line-buffered and stderr unbuffered */
    setvbuf(stdout, nullptr, _IOLBF, 0);
    setvbuf(stderr, nullptr, _IONBF, 0);

    /* create the pipe and redirect stdout and stderr */
    pipe(pfd);
    dup2(pfd[1], 1);
    dup2(pfd[1], 2);

    /* spawn the logging thread */
    if (pthread_create(&thr, nullptr, thread_func, nullptr) != 0) {
        return -1;
    }
    pthread_detach(thr);
    return 0;
}
