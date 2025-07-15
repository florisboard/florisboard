#!/bin/bash

readonly TRACK_STABLE="stable"
readonly TRACK_PREVIEW="preview"
readonly TRACK_DEBUG="debug"

TRACK="$1"
OUTPUT_DIR="out"

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

BUILD_VERSION_NAME=""
build() {
  local TYPE="$1"
  BUILD_LOG_PATH="build.log"
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
  if ./gradlew "$TASK" > "$BUILD_LOG_PATH" 2>&1; then
    echo "done"
  else
    echo "failed, aborting..."
    exit 1
  fi
  if [ "$TYPE" = "apk" ]; then
    BUILD_METADATA_JSON="$BUILD_DIR/output-metadata.json"
    BUILD_FILE_NAME="$(jq -r ".elements[].outputFile" "$BUILD_METADATA_JSON")"
    BUILD_VERSION_NAME="$(jq -r ".elements[].versionName" "$BUILD_METADATA_JSON")"
  else
    BUILD_FILE_NAME="app-$GRADLE_TASK_LC.aab"
    if [ -z "$BUILD_VERSION_NAME" ]; then
      echo "fatal: BUILD_VERSION_NAME was empty, should not be the case if apk is built before aab, aborting..."
      exit 1
    fi
  fi
  BUILD_FILE_PATH="$BUILD_DIR/$BUILD_FILE_NAME"
  if [ "$TRACK" = "$TRACK_DEBUG" ]; then
    OUTPUT_FILE_PATH="$OUTPUT_DIR/florisboard-$BUILD_VERSION_NAME.$TYPE"
  else
    OUTPUT_FILE_PATH="$OUTPUT_DIR/florisboard-$BUILD_VERSION_NAME-$TRACK.$TYPE"
  fi
  OUTPUT_LOG_PATH="$OUTPUT_FILE_PATH.log"
  echo "generated $TYPE: $BUILD_FILE_PATH"
  if [ ! -f "$BUILD_FILE_PATH" ]; then
    echo "fatal: $TYPE not generated, aborting..."
    exit 1
  fi
  cp "$BUILD_FILE_PATH" "$OUTPUT_FILE_PATH" || exit 1
  mv "$BUILD_LOG_PATH" "$OUTPUT_LOG_PATH" || exit 1
}

mkdir "$OUTPUT_DIR"
build apk
if [ "$AAB_ENABLED" -eq 1 ]; then
  build aab
fi
