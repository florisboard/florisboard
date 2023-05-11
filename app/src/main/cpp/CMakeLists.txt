cmake_minimum_required(VERSION 3.22)

project(florisboard)

set(CMAKE_CXX_STANDARD 20)

include_directories(.)

### FlorisBoard ###
add_subdirectory(utils)
add_subdirectory(nlp)

add_library(
        florisboard-native
        SHARED
        dev_patrickgold_florisboard_FlorisApplication.cpp
        dev_patrickgold_florisboard_ime_nlp_latin_LatinNlpSession.cpp
)

target_compile_options(florisboard-native PRIVATE -ffunction-sections -fdata-sections -fexceptions)
target_link_libraries(
        # Destination
        florisboard-native

        # Sources
        android
        log
        fl::nlp::core
        utils
)
