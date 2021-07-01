#!/bin/bash

# This script is based on the work of NanoMichael:
#  https://github.com/NanoMichael/cross_compile_icu4c_for_android
#
# Note that the original script has been heavily modified to match with
# modern NDK standards and FlorisBoard's needs.
#
# Copyright 2018 Nano Michael
# Copyright 2021 Patrick Goldinger
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

min_sdk_version=23
working_dir=$(pwd)

usage() {
    cat <<EOE
Cross compile icu4c for Android, see README.md for details.
Options: -h, --help  Print this message and exit
         -c, --clean Clear all build files
Usage:
    First make sure you have ndk installed, currently only support linux* and darwin*
    like OS.
    Run the following commands to build:
        chmod +x build_icu
        ./build_icu <TARGET_ARCH>

    <TARGET_ARCH> is the target abi-architecture you want to build, you can specify
    it with 'arm' that corresponding to 'armabi-v7a' or 'arm64' that corresponding
    to 'arm64-v8a', the default is 'arm'.
EOE
    exit 0
}

# Clean all build files
clean() {
    while true; do
        read -p "Do you wish to clean all build files (Y/N)? " yn
        case $yn in
        [Yy]* )
            rm -rf build
            rm -rf ./*-toolchain
            break;;
        [Nn]* )
            break;;
        * ) ;;
        esac
    done
    exit 0
}

case "$1" in
'-h'|'--help' )
    usage
    ;;
'-c'|'--clean' )
    clean
    ;;
esac

ERR_COLOR='\033[1;31m'
SUCCESS_COLOR='\033[1;32m'
WARNING_COLOR='\033[1;33m'
NO_COLOR='\033[0m'

echo_error() {
    echo -e "${ERR_COLOR}$1${NO_COLOR}"
}
echo_warning() {
    echo -e "${WARNING_COLOR}$1${NO_COLOR}"
}
echo_success() {
    echo -e "${SUCCESS_COLOR}$1${NO_COLOR}"
}

case $OSTYPE in
darwin*)
    host_os_name="darwin"
    host_os_arch="darwin-x86_64"
    host_os_build_type="MacOSX/GCC"
    ;;
linux*)
    host_os_name="linux"
    host_os_arch="linux-x86_64"
    host_os_build_type="Linux"
    ;;
*)
    echo_error "${OSTYPE} is not supported, currently only support darwin* and linux*. Exiting"
    exit 1
    ;;
esac
echo "Host OS name:         $host_os_name"
echo "Host OS arch:         $host_os_arch"
echo "Host OS build type:   $host_os_build_type"
echo

arch=$1
if [ -z "$arch" ]; then
    echo_warning "No arch specified, using 'arm' as the default"
    arch="arm"
fi
case $arch in
"arm" )
    abi="armeabi-v7a"
    target="armv7a-linux-androideabi"
    ;;
"arm64" )
    abi="arm64-v8a"
    target="aarch64-linux-android"
    ;;
"x86" )
    abi="x86"
    target="i686-linux-android"
    ;;
"x86_64" )
    abi="x86_64"
    target="x86_64-linux-android"
    ;;
* )
    echo_error "Specified arch '$arch' is not supported by this build script. Exiting"
    exit 1
esac
echo "Arch:     $arch"
echo "ABI:      $abi"
echo "Target:   $target"
echo

echo "Searching for NDK installation..."
if ! NDK=$(dirname "$(command -v ndk-build)"); then
    echo_error "Failed to find an NDK installation. Either it is not installed or missing from \$PATH. Exiting"
    exit 1
fi
if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo_error "Found an NDK installation but could not verify its validity. Exiting"
    exit 1
fi
echo "Found and using NDK installation at '$NDK'"
echo

icu_configure_args="\
    --enable-static=yes --enable-shared=no --enable-strict=no \
    --enable-extras=no --enable-tests=no --enable-samples=no \
    --enable-dyload=no --enable-renaming=no \
    --with-data-packaging=static"

host_build_dir="$working_dir/build/host"
android_build_dir="$working_dir/build/android/$arch"

build_host() {
    echo "Begin build process for host"

    mkdir -p "$host_build_dir"
    cd "$host_build_dir" || return 1

    export ICU_SOURCES=$working_dir/icu
    export LDFLAGS="-std=gnu++11"
    export CFLAGS="-fPIC -DU_STATIC_IMPLEMENTATION"
    export CXXFLAGS="-Os -fno-short-wchar -fno-short-enums \
        -DU_USING_ICU_NAMESPACE=0 \
        -DU_HAVE_NL_LANGINFO_CODESET=0 \
        -D__STDC_INT64__ -DU_TIMEZONE=0 \
        -DUCONFIG_NO_LEGACY_CONVERSION=1 \
        -DU_DISABLE_RENAMING=1 -DU_STATIC_IMPLEMENTATION \
        -ffunction-sections -fdata-sections -fvisibility=hidden -fPIC"
    export CPPFLAGS="-Os -fno-short-wchar -fno-short-enums \
        -DU_USING_ICU_NAMESPACE=0 \
        -DU_HAVE_NL_LANGINFO_CODESET=0 \
        -D__STDC_INT64__ -DU_TIMEZONE=0 \
        -DUCONFIG_NO_LEGACY_CONVERSION=1 \
        -DU_DISABLE_RENAMING=1 -DU_STATIC_IMPLEMENTATION \
        -ffunction-sections -fdata-sections -fvisibility=hidden -fPIC"

    if [ $host_os_name = "linux" ]; then
        export LDFLAGS="-Wl,--gc-sections"
    elif [ $host_os_name = "darwin" ]; then
        # gcc on OSX does not support --gc-sections
        export LDFLAGS="-Wl,-dead_strip"
    fi

    # Set --prefix option to disable install to the system,
    # since we only need the libraries and header files
    # shellcheck disable=SC2086
    (exec "$ICU_SOURCES/source/runConfigureICU" $host_os_build_type \
        --prefix="$host_build_dir/icu_build" $icu_configure_args)

    if ! make -j16; then
        cd "$working_dir" || return 1
        return 1
    fi

    if ! make install; then
        cd "$working_dir" || return 1
        return 1
    fi

    if [ ! -d "$host_build_dir/icu_build/include/unicode" ]; then
        echo_error "Host build failed. Exiting"
        exit 1
    fi

    #if ! test; then
    #    cd "$working_dir" || return 1
    #    return 1
    #fi

    cd "$working_dir" || return 1
    return 0
}

build_android() {
    # Relevant docs:
    #  https://developer.android.com/ndk/guides/other_build_systems

    echo "Begin build process"

    local toolchain="$NDK/toolchains/llvm/prebuilt/$host_os_arch"
    if [ ! -d "$toolchain" ]; then
        echo_error "Expected toolchain '$toolchain', could not resolve path. Exiting"
        exit 1
    fi
    echo "Selecting toolchain '$toolchain'"

    mkdir -p "$android_build_dir"
    cd "$android_build_dir" || return 1

    export TARGET=$target
    export TOOLCHAIN=$toolchain
    export API=$min_sdk_version
    export AR=$TOOLCHAIN/bin/llvm-ar
    export CC=$TOOLCHAIN/bin/$TARGET$API-clang
    export AS=$CC
    export CXX=$TOOLCHAIN/bin/$TARGET$API-clang++
    export LD=$TOOLCHAIN/bin/ld

    export ICU_SOURCES=$working_dir/icu
    export ICU_CROSS_BUILD=$host_build_dir
    export ANDROIDVER=$API
    export NDK_STANDARD_ROOT=$TOOLCHAIN
    export CFLAGS="-fPIC -DU_STATIC_IMPLEMENTATION"
    export CXXFLAGS="-Os -fno-short-wchar -fno-short-enums \
        -DU_USING_ICU_NAMESPACE=0 \
        -DU_HAVE_NL_LANGINFO_CODESET=0 \
        -D__STDC_INT64__ -DU_TIMEZONE=0 \
        -DUCONFIG_NO_LEGACY_CONVERSION=1 \
        -DU_DISABLE_RENAMING=1 -DU_STATIC_IMPLEMENTATION \
        -ffunction-sections -fdata-sections -fvisibility=hidden -fPIC"
    export CPPFLAGS="-Os -fno-short-wchar -fno-short-enums \
        -DU_USING_ICU_NAMESPACE=0 \
        -DU_HAVE_NL_LANGINFO_CODESET=0 \
        -D__STDC_INT64__ -DU_TIMEZONE=0 \
        -DUCONFIG_NO_LEGACY_CONVERSION=1 \
        -DU_DISABLE_RENAMING=1 -DU_STATIC_IMPLEMENTATION \
        -ffunction-sections -fdata-sections -fvisibility=hidden -fPIC"
    export LDFLAGS="-lc -lstdc++ -Wl,--gc-sections,-rpath-link=$NDK_STANDARD_ROOT/sysroot/usr/lib/"
    export PATH=$PATH:$NDK_STANDARD_ROOT/bin

    # shellcheck disable=SC2086
    (exec "$ICU_SOURCES/source/configure" --with-cross-build="$ICU_CROSS_BUILD" \
        $icu_configure_args --host=$TARGET --prefix="$PWD/icu_build")

    if ! make -j16; then
        cd "$working_dir" || return 1
        return 1
    fi

    cd "$working_dir" || return 1
    return 0
}

if [ -d "$host_build_dir/icu_build" ]; then
    echo "Host build already exists at '$host_build_dir', reusing."
    echo
else
    if ! build_host; then
        echo_error "Building for host failed. Exiting"
        exit 1
    fi
fi

if ! build_android; then
    echo_error "Cross build for Android failed. Exiting"
    exit 1
else
    echo_success "Build success, congratulations!"
    echo_success "The libraries can be found at: $android_build_dir/lib/"
fi
