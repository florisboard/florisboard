add_library(
        # Name
        utils
        SHARED

        # Headers
        jni_exception.h
        jni_utils.h
        log.h

        # Sources
        jni_exception.cpp
        jni_utils.cpp
        log.cpp
)

target_link_libraries(utils PUBLIC log)
