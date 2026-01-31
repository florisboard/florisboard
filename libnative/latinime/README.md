# libnative/LatinIME

This directory represents a snapshot state of AOSP LatinIME JNI code for the Android 16 QPR 2 release (tag `android16-qpr2-release`).
See https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/refs/heads/android16-qpr2-release

In particular, this directory is a copy of `native/jni/` in LatinIME.

`CMakeLists.txt` is newly created, and a direct translation of the upstream `Android.bp` script. This allows the LatinIME JNI source code to be built by FlorisBoard and linked into `fl_native`.

All files in this directory are licensed under the Apache 2.0 license. See the respective heder file for detailed copyright information.
