#!/bin/bash

# This file uses the ./build_icu.sh script and copies the generated files to
# their designated location. This script must only be executed if the icu version
# is switched out and may take a hot quarter of an hour to complete.

# This script requires that the host is either darwin* or linux*, as required
# by the provided ./build_icu.sh script.

# Note: This file depends on the fact that it lives in the icu4c directory. If it
#       is moved, relative links are broken and must be fixed!

usage() {
    cat <<EOE
FlorisBoard ICU4C build utility

Usage: $0 <action>

Where <action> can be:
    build           Builds ICU4C for the host, then cross-builds it for all ABIs
    clean           Cleans everything
    clean-build     Cleans the build output
    clean-incl      Cleans the header include dir
    help            Shows this screen

EOE
}

working_dir=$(pwd)
icu4c_dir=$(dirname "$(readlink -f "$0")")

build_dir="$icu4c_dir/build"

jni_libs_dir="$icu4c_dir/../../jniLibs"
include_dir="$icu4c_dir/include"

clean_build() {
    cd "$icu4c_dir" || return 1
    yes | ./build_icu.sh -c || return 1
    cd "$working_dir" || return 1
}

clean_incl() {
    rm -r "${include_dir:?}"/* 2>/dev/null
}

copy_lib_output() {
    local src_lib_dir="$1/lib"
    local dst_lib_dir="$jni_libs_dir/$2"
    mkdir -p "$dst_lib_dir"

    echo "Copying binary lib files"
    echo " from src: $src_lib_dir"
    echo " to dst:   $dst_lib_dir"
    if cp "$src_lib_dir/"* "$dst_lib_dir"; then
        echo "OK"
        return 0
    else
        echo "FAILED"
        return 1
    fi
    return 0
}

copy_host_include_files() {
    local include_src_dir="$build_dir/host/icu_build/include"

    mkdir -p "$include_dir"
    clean_incl

    echo "Copying include files"
    echo " from src: $include_src_dir"
    echo " to dst:   $include_dir"
    if cp -r "$include_src_dir/"* "$include_dir"; then
        echo "OK"
        return 0
    else
        echo "FAILED"
        return 1
    fi
}

build_arm() {
    local build_id="armeabi-v7a"

    cd "$icu4c_dir" || return 1
    ./build_icu.sh arm || return 1
    copy_lib_output "$build_dir/android/arm" "$build_id" || return 1
    cd "$working_dir" || return 1

    return 0
}

build_arm64() {
    local build_id="arm64-v8a"

    cd "$icu4c_dir" || return 1
    ./build_icu.sh arm64 || return 1
    copy_lib_output "$build_dir/android/arm64" "$build_id" || return 1
    cd "$working_dir" || return 1

    return 0
}

build_x86() {
    local build_id="x86"

    cd "$icu4c_dir" || return 1
    ./build_icu.sh x86 || return 1
    copy_lib_output "$build_dir/android/x86" "$build_id" || return 1
    cd "$working_dir" || return 1

    return 0
}

build_x86_64() {
    local build_id="x86_64"

    cd "$icu4c_dir" || return 1
    ./build_icu.sh x86_64 || return 1
    copy_lib_output "$build_dir/android/x86_64" "$build_id" || return 1
    cd "$working_dir" || return 1

    return 0
}

case "$1" in
'help')
    usage
    ;;
'clean')
    clean_build
    clean_incl
    ;;
'clean_build')
    clean_build
    ;;
'clean_incl')
    clean_incl
    ;;
'build')
    case "$2" in
    'arm')
        if ! build_arm; then
            echo "Failed to build ICU4C for target 'arm'! Exiting..."
            exit
        fi
        ;;
    'arm64')
        if ! build_arm64; then
            echo "Failed to build ICU4C for target 'arm64'! Exiting..."
            exit
        fi
        ;;
    'x86')
        if ! build_x86; then
            echo "Failed to build ICU4C for target 'x86'! Exiting..."
            exit
        fi
        ;;
    'x86_64')
        if ! build_x86_64; then
            echo "Failed to build ICU4C for target 'x86_64'! Exiting..."
            exit
        fi
        ;;
    *)
        if ! build_arm; then
            echo "Failed to build ICU4C for target 'arm'! Exiting..."
            exit
        fi
        if ! build_arm64; then
            echo "Failed to build ICU4C for target 'arm64'! Exiting..."
            exit
        fi
        if ! build_x86; then
            echo "Failed to build ICU4C for target 'x86'! Exiting..."
            exit
        fi
        if ! build_x86_64; then
            echo "Failed to build ICU4C for target 'x86_64'! Exiting..."
            exit
        fi
        ;;
    esac
    if ! copy_host_include_files; then
        echo "Failed to copy ICU4C include header files! Exiting..."
        exit
    fi
    echo "Finished!"
    ;;
*)
    usage
    ;;
esac
