#!/bin/bash

readonly TRACK_STABLE="stable"
readonly TRACK_PREVIEW="preview"
readonly TRACK_DEBUG="debug"

TRACK="$1"
OUTPUT_DIR="out"
OUTPUT_VERSION_DIR=""

if [ "$TRACK" = "$TRACK_STABLE" ]; then
  GRADLE_TASK_LC="release"
  GRADLE_TASK_TC="Release"
  AAB_ENABLED=1
elif [ "$TRACK" = "$TRACK_PREVIEW" ]; then
  GRADLE_TASK_LC="beta"
  GRADLE_TASK_TC="Beta"
  AAB_ENABLED=1
elif [ "$TRACK" = "$TRACK_DEBUG" ]; then
  GRADLE_TASK_LC="debug"
  GRADLE_TASK_TC="Debug"
  AAB_ENABLED=0
else
  echo "fatal: specified unknown track '$TRACK', aborting"
  exit 1
fi

find_executable() {
  local executable_path
  local executable_name="$1"
  local target_var_name="$2"
  executable_path="$(find / -name "$executable_name" 2>/dev/null | head -n 1)"
  [ -n "$executable_path" ] || {
    echo "fatal: $executable_name not found, aborting..."
    return 1
  }
  executable_path="$(realpath "$executable_path")"
  echo "using $executable_name at: $executable_path"
  declare -g "$target_var_name"="$executable_path"
}

init_signing() {
  if [ "$FLSEC_SIGNING_ENABLED" = "1" ]; then
    echo "signing requested, initialize..."
    [ "$TRACK" != "$TRACK_DEBUG" ] || {
      echo "fatal: requested signing for track '$TRACK_DEBUG', aborting..."
      return 1
    }
    find_executable apksigner APKSIGNER_PATH || return 1
    find_executable jarsigner JARSIGNER_PATH || return 1
  else
    echo "signing not requested, skipping..."
  fi
}

sign_and_write_apk() {
  local APK_PATH="$1"
  echo -n "signing APK ... "
  "$APKSIGNER_PATH" sign -v \
    --ks "${FLSEC_KEYFILE}" \
    --ks-key-alias "$FLSEC_KEYALIAS" \
    --ks-pass "pass:$FLSEC_KS_PASS" \
    --key-pass "pass:$FLSEC_KEY_PASS" \
    --out "$APK_PATH" \
    "$APK_PATH"
}

sign_and_write_aab() {
  local AAB_PATH="$1"
  echo -n "signing AAB ... "
  "$JARSIGNER_PATH" -sigalg SHA256withRSA -digestalg SHA256 \
    -keystore "${FLSEC_KEYFILE}" \
    -storepass "$FLSEC_KS_PASS" \
    -keypass "$FLSEC_KEY_PASS" \
    -signedjar "$AAB_PATH" \
    "$AAB_PATH" "$FLSEC_KEYALIAS"
}

calculate_and_write_sha256sum() {
  local FILE_PATH="$1"
  local DIR_PATH
  local FILE_NAME
  DIR_PATH="$(dirname "$FILE_PATH")"
  FILE_NAME="$(basename "$FILE_PATH")"
  SHA256_NAME="$FILE_NAME.sha256"
  CURR_DIR="$(pwd)"
  echo -n "calculate sha256sum ... "
  cd "$DIR_PATH" || { echo "failed (cd dir)"; return 1; }
  sha256sum "$FILE_NAME" > "$SHA256_NAME" || { echo "failed (sha256 generate)"; return 1; }
  sha256sum -c "$SHA256_NAME" || { echo "failed (sha256 verify)"; return 1; }
  cd "$CURR_DIR" || { echo "failed (cd pwd)"; return 1; }
}

BUILD_VERSION_NAME=""
build() {
  local TYPE="$1"
  BUILD_LOG_PATH="$OUTPUT_DIR/tmp_$TYPE.log"
  if [ "$TYPE" = "apk" ]; then
    TASK="assemble$GRADLE_TASK_TC"
    BUILD_DIR="app/build/outputs/apk/$GRADLE_TASK_LC"
  elif [ "$TYPE" = "aab" ]; then
    TASK="bundle$GRADLE_TASK_TC"
    BUILD_DIR="app/build/outputs/bundle/$GRADLE_TASK_LC"
  else
    echo "fatal: unknown type '$TYPE', aborting..."
  fi
  echo -n "build: $TASK ... "
  if ./gradlew "$TASK" --profile > "$BUILD_LOG_PATH" 2>&1; then
    echo "done"
  else
    echo "failed, aborting..."
    return 1
  fi
  if [ "$TYPE" = "apk" ]; then
    BUILD_METADATA_JSON="$BUILD_DIR/output-metadata.json"
    BUILD_FILE_NAME="$(jq -r ".elements[].outputFile" "$BUILD_METADATA_JSON")"
    BUILD_VERSION_NAME="$(jq -r ".elements[].versionName" "$BUILD_METADATA_JSON")"
  else
    BUILD_FILE_NAME="app-$GRADLE_TASK_LC.aab"
    if [ -z "$BUILD_VERSION_NAME" ]; then
      echo "fatal: BUILD_VERSION_NAME was empty, should not be the case if apk is built before aab, aborting..."
      return 1
    fi
  fi
  BUILD_FILE_PATH="$BUILD_DIR/$BUILD_FILE_NAME"
  if [ "$TRACK" = "$TRACK_DEBUG" ]; then
    OUTPUT_VERSION_DIR="$OUTPUT_DIR/$BUILD_VERSION_NAME"
    OUTPUT_FILE_PATH="$OUTPUT_VERSION_DIR/florisboard-$BUILD_VERSION_NAME.$TYPE"
  else
    OUTPUT_VERSION_DIR="$OUTPUT_DIR/$BUILD_VERSION_NAME-$TRACK"
    OUTPUT_FILE_PATH="$OUTPUT_VERSION_DIR/florisboard-$BUILD_VERSION_NAME-$TRACK.$TYPE"
  fi
  [ ! -d "$OUTPUT_VERSION_DIR" ] && mkdir "$OUTPUT_VERSION_DIR"
  OUTPUT_LOG_PATH="$OUTPUT_FILE_PATH.log"
  echo "generated $TYPE: $BUILD_FILE_PATH"
  if [ ! -f "$BUILD_FILE_PATH" ]; then
    echo "fatal: $TYPE not generated, aborting..."
    return 1
  fi
  cp "$BUILD_FILE_PATH" "$OUTPUT_FILE_PATH" || return 1
  mv "$BUILD_LOG_PATH" "$OUTPUT_LOG_PATH" || return 1
  if [ "$FLSEC_SIGNING_ENABLED" = "1" ]; then
    if [ "$TYPE" = "apk" ]; then
      sign_and_write_apk "$OUTPUT_FILE_PATH" || return 1
    else
      sign_and_write_aab "$OUTPUT_FILE_PATH" || return 1
    fi
  fi
  calculate_and_write_sha256sum "$OUTPUT_FILE_PATH" || return 1
}

mkdir "$OUTPUT_DIR" || exit 1
init_signing || exit 1
build apk || exit 1
if [ "$AAB_ENABLED" -eq 1 ]; then
  build aab || exit 1
fi

cp -r "build/reports" "$OUTPUT_VERSION_DIR/" || exit 1
