#!/bin/bash

CHECKSUM=$(sha256sum Dockerfile | awk '{print $1}' | head -c 4)
CHECKSUM+="$(sha256sum gradle.properties | awk '{print $1}' | head -c 4)"
IMAGE_NAME="fl-build"
IMAGE_TAG="$CHECKSUM"
REPO_MOUNT="/florisboard"

read_property() {
  local version
  version="$(grep "$1" gradle.properties | awk -F'=' '{print $2}')"
  declare -g "$2"="$version"
}

read_property projectCompileSdk       PROJECT_COMPILE_SDK
read_property cmdlineToolsVersion     CMDLINE_TOOLS_VERSION
read_property cmdlineToolsChecksum    CMDLINE_TOOLS_CHECKSUM
read_property buildToolsVersion       BUILD_TOOLS_VERSION
read_property cmakeVersion            CMAKE_VERSION
read_property jdkVersion              JDK_VERSION
read_property ndkVersion              NDK_VERSION
read_property rustupVersion           RUSTUP_VERSION
read_property rustToolchainVersion    RUST_TOOLCHAIN_VERSION

if docker images | grep "$IMAGE_NAME" | grep -q "$IMAGE_TAG"; then
  echo "Docker image $IMAGE_NAME:$IMAGE_TAG exists, using it"
else
  echo "Docker image $IMAGE_NAME:$IMAGE_TAG does not exist, building it"
  docker build -t "$IMAGE_NAME:$IMAGE_TAG" . \
    --build-arg "PROJECT_COMPILE_SDK=$PROJECT_COMPILE_SDK" \
    --build-arg "CMDLINE_TOOLS_VERSION=$CMDLINE_TOOLS_VERSION" \
    --build-arg "CMDLINE_TOOLS_CHECKSUM=$CMDLINE_TOOLS_CHECKSUM" \
    --build-arg "BUILD_TOOLS_VERSION=$BUILD_TOOLS_VERSION" \
    --build-arg "CMAKE_VERSION=$CMAKE_VERSION" \
    --build-arg "JDK_VERSION=$JDK_VERSION" \
    --build-arg "NDK_VERSION=$NDK_VERSION" \
    --build-arg "RUSTUP_VERSION=$RUSTUP_VERSION" \
    --build-arg "RUST_TOOLCHAIN_VERSION=$RUST_TOOLCHAIN_VERSION" \
    --build-arg "REPO_MOUNT=$REPO_MOUNT" \
    || exit 1
fi

docker_run_it() {
  docker run --rm -it -v "$(pwd)":"$REPO_MOUNT" -w "$REPO_MOUNT" "$IMAGE_NAME:$IMAGE_TAG" "$@"
}

docker_run() {
  docker run --rm -v "$(pwd)":"$REPO_MOUNT" -w "$REPO_MOUNT" "$IMAGE_NAME:$IMAGE_TAG" "$@"
}

action="$1"

case "$action" in
  "interactive")
    docker_run_it /bin/bash
    ;;
  "assemble:stable")
    docker_run ./gradlew assembleRelease --no-daemon
    ;;
  "assemble:preview")
    docker_run ./gradlew assembleBeta --no-daemon
    ;;
  "assemble:debug")
    docker_run ./gradlew assembleDebug --no-daemon
    ;;
  "clean")
    docker_run ./gradlew clean --no-daemon
    ;;
  *)
    echo "Unknown action: $action"
    exit 1
    ;;
esac
