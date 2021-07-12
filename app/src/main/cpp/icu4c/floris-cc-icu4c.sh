#!/bin/bash

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

# Build script for ICU4C, tailored exactly for FlorisBoard's needs.

# Before executing this script to manually rebuild the ICU libraries, make sure to execute
#  git submodule update --init --recursive
# else the script won't find the ICU source files!

###### Build ICU4C ######

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

###### Clean up unused header files in include/unicode header dir ######

readonly SEP=":"
readonly NUSPELL_DIR=$(realpath ../nuspell)
readonly UNICODE_DIR=$(realpath include/unicode)

scan_file() {
    file=$1
    local -n var=$2
    #echo "Scanning '$file'..."
    while IFS= read -r line; do
        case $line in
        *"#include <unicode/"*)
            # shellcheck disable=SC2001
            header=$(sed -e 's/.*<unicode\/\(.*\)>.*/\1/' <<< "$line")
            ;;
        *"#include \"unicode/"*)
            # shellcheck disable=SC2001
            header=$(sed -e 's/.*\"unicode\/\(.*\)\".*/\1/' <<< "$line")
            ;;
        *)
            header=""
            ;;
        esac
        if [ -z "$header" ]; then
            continue
        fi
        # shellcheck disable=SC2091
        # shellcheck disable=SC2086
        if [[ ! "$var" == *"$header"* ]]; then
            # shellcheck disable=SC2140
            var+="$SEP$header"
        fi
    done < "$file"
}

req_headers=""

for nsrcfile in "$NUSPELL_DIR"/*; do
    scan_file "$nsrcfile" "req_headers"
done

if [ -n "$req_headers" ]; then
    req_headers=${req_headers:1}
fi

while true; do
    old_req_headers=$req_headers
    IFS="$SEP" read -ra req_header_splitted <<< "$req_headers"
    for req_header in "${req_header_splitted[@]}"; do
        scan_file "$UNICODE_DIR/$req_header" "req_headers"
    done
    [ ! $req_headers = $old_req_headers ] || break
done

#echo "$req_headers"

for headerfile in "$UNICODE_DIR"/*; do
    header=$(basename "$headerfile")
    if [[ "$req_headers" == *"$header"* ]]; then
        echo "KEEP      '$headerfile'"
    else
        echo "DELETE    '$headerfile'"
        rm "$headerfile"
    fi
done

