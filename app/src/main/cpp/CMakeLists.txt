# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.10.2)

project("florisboard")

set(CMAKE_CXX_STANDARD 17)

include_directories(.)

### ICU4C ###
include_directories(icu4c/include)
#link_directories(${CMAKE_SOURCE_DIR}/../${ANDROID_ABI})
set(JNI_LIBS ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
add_library(ICU::data STATIC IMPORTED)
set_property(TARGET ICU::data PROPERTY IMPORTED_LOCATION "${JNI_LIBS}/libicudata.a")
#add_library(ICU::i18n STATIC IMPORTED)
#set_property(TARGET ICU::i18n PROPERTY IMPORTED_LOCATION "${JNI_LIBS}/libicui18n.a")
#add_library(ICU::tu STATIC IMPORTED)
#set_property(TARGET ICU::tu PROPERTY IMPORTED_LOCATION "${JNI_LIBS}/libicutu.a")
add_library(ICU::uc STATIC IMPORTED)
set_property(TARGET ICU::uc PROPERTY IMPORTED_LOCATION "${JNI_LIBS}/libicuuc.a")

### FlorisBoard ###
add_subdirectory(nuspell)
add_subdirectory(glob_ndk)
add_subdirectory(utils)
add_subdirectory(ime/nlp)
add_subdirectory(ime/spelling)

add_library(
        florisboard-native
        SHARED
        dev_patrickgold_florisboard_ime_nlp_SuggestionList.cpp
        dev_patrickgold_florisboard_ime_spelling_SpellingDict.cpp
)

target_compile_options(florisboard-native PRIVATE -ffunction-sections -fdata-sections -fexceptions)
target_link_libraries(
        # Destination
        florisboard-native

        # Sources
        android
        log
        glob_ndk
        ICU::uc
        ICU::data
        Nuspell::nuspell
        utils
        ime-nlp
        ime-spelling
)
