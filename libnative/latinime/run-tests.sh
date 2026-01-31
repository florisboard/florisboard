#!/bin/bash
# Copyright 2014, The Android Open Source Project
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

function usage() {
    echo "usage: source run-tests.sh [--host] [--target] [-h] [--help]"  1>&2
    echo "    --host: run test on the host environment"  1>&2
    echo "    --no-host: skip host test"  1>&2
    echo "    --target: run test on the target environment"  1>&2
    echo "    --no-target: skip target device test"  1>&2
}

# check script arguments
if [[ $(type -t mmm) != function ]]; then
usage
if [[ ${BASH_SOURCE[0]} != $0 ]]; then return; else exit 1; fi
fi

show_usage=no
enable_host_test=yes
enable_target_device_test=no
while [ "$1" != "" ]
  do
  case "$1" in
    "-h") show_usage=yes;;
    "--help") show_usage=yes;;
    "--target") enable_target_device_test=yes;;
    "--no-target") enable_target_device_test=no;;
    "--host") enable_host_test=yes;;
    "--no-host") enable_host_test=no;;
  esac
  shift
done

if [[ $show_usage == yes ]]; then
  usage
  if [[ ${BASH_SOURCE[0]} != $0 ]]; then return; else exit 1; fi
fi

# Host build is never supported in unbundled (NDK/tapas) build
if [[ $enable_host_test == yes && -n $TARGET_BUILD_APPS ]]; then
  echo "Host build is never supported in tapas build."  1>&2
  echo "Use lunch command instead."  1>&2
  if [[ ${BASH_SOURCE[0]} != $0 ]]; then return; else exit 1; fi
fi

target_test_name=liblatinime_target_unittests
host_test_name=liblatinime_host_unittests

pushd $PWD > /dev/null
cd $(gettop)
mmm -j16 packages/inputmethods/LatinIME/native/jni || \
    make -j16 adb $target_test_name $host_test_name
if [[ $enable_host_test == yes ]]; then
  $ANDROID_HOST_OUT/bin/$host_test_name
fi
if [[ $enable_target_device_test == yes ]]; then
  target_test_local=$ANDROID_PRODUCT_OUT/data/nativetest/$target_test_name/$target_test_name
  target_test_device=/data/nativetest/$target_test_name/$target_test_name
  adb push $target_test_local $target_test_device
  adb shell $target_test_device
  adb shell rm -rf $target_test_device
fi
popd > /dev/null
