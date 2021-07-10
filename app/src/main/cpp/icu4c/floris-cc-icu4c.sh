#!/bin/bash

# Before executing this script to manually rebuild the ICU libraries, make sure to execute
#  git submodule update --init --recursive
# else the script won't find the ICU source files!

./android/cc-icu4c.sh build \
    --arch=arm,arm64 \
    --api=23 \
    --library-type=static \
    --build-dir=./build \
    --icu-src-dir=./android/icu/icu4c \
    --install-include-dir=./include \
    --install-libs-dir=./../../jniLibs \
    --data-filter-file=./data-feature-filter.json \
    --enable-collation=no \
    --enable-formatting=no \
    --enable-regex=no \
    --enable-transliteration=no
