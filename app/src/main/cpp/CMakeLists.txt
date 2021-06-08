# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.10.2)

project("florisboard")

set(CMAKE_CXX_STANDARD 17)

add_subdirectory(ime/nlp)
add_subdirectory(ime/spelling)
add_subdirectory(ime/spelling/hunspell)

add_library(
        # Name
        florisboard-native

        # Type
        SHARED

        # Sources
        dev_patrickgold_florisboard_ime_nlp_SuggestionList.cpp
        dev_patrickgold_florisboard_ime_spelling_SpellingDict.cpp
)

find_library(
        # Save to var
        log-lib

        # Original name
        log
)

target_link_libraries(
        # Destination
        florisboard-native

        # Sources
        ${log-lib}
        ime-nlp
        ime-spelling
        ime-spelling-hunspell
)
