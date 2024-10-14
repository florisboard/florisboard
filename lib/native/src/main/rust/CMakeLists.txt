cmake_minimum_required(VERSION 3.22)

project(florisboard)

if(CMAKE_HOST_WIN32)
    set(USER_HOME "$ENV{USERPROFILE}")
else()
    set(USER_HOME "$ENV{HOME}")
endif()

find_program(RUSTUP_EXECUTABLE NAMES rustup HINTS "${USER_HOME}/.cargo/bin")
find_program(CARGO_EXECUTABLE NAMES cargo HINTS "${USER_HOME}/.cargo/bin")
if(NOT RUSTUP_EXECUTABLE OR NOT CARGO_EXECUTABLE)
    message(FATAL_ERROR "Rust toolchain not found on this system. Please install Rust toolchain, then try again.")
else()
    message(STATUS "Using Rust toolchain: ${RUSTUP_EXECUTABLE}")
endif()

### FlorisBoard ###

add_library(fl_native SHARED src/lib.c)

if(CMAKE_ANDROID_ARCH_ABI STREQUAL "armeabi-v7a")
    set(ANDROID_TARGET "armv7a-linux-androideabi")
    set(RUST_TARGET "armv7-linux-androideabi")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(ANDROID_TARGET "aarch64-linux-android")
    set(RUST_TARGET "aarch64-linux-android")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86")
    set(ANDROID_TARGET "i686-linux-android")
    set(RUST_TARGET "i686-linux-android")
elseif(CMAKE_ANDROID_ARCH_ABI STREQUAL "x86_64")
    set(ANDROID_TARGET "x86_64-linux-android")
    set(RUST_TARGET "x86_64-linux-android")
else()
    message(FATAL_ERROR "Unsupported ABI: ${CMAKE_ANDROID_ARCH_ABI}")
endif()
get_filename_component(LLVM_TOOLCHAIN ${CMAKE_C_COMPILER} DIRECTORY)

set(FL_NATIVE_RUST_PATH "${CMAKE_CURRENT_SOURCE_DIR}/target/${RUST_TARGET}/release/libfl_native_rust.a")

add_custom_target(
    setup_rust_target ALL
    COMMAND ${RUSTUP_EXECUTABLE} target add ${RUST_TARGET}
)
add_custom_target(
    fl_native_rust_build ALL
    COMMAND ${CARGO_EXECUTABLE} rustc --release --locked --target ${RUST_TARGET} --
        -C linker="${LLVM_TOOLCHAIN}/${ANDROID_TARGET}${CMAKE_ANDROID_API}-clang"
    DEPENDS setup_rust_target
    BYPRODUCTS ${FL_NATIVE_RUST_PATH}
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
)

add_dependencies(fl_native fl_native_rust_build)

# TODO: check if there's a better way than allowing multiple symbol definitions
target_link_libraries(fl_native
    android log -Wl,--whole-archive -Wl,--allow-multiple-definition ${FL_NATIVE_RUST_PATH}
)
