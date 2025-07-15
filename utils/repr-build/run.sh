#!/bin/bash

IMAGE_NAME="fl-repr-build"
IMAGE_TAG="default"
REPO_MOUNT="/home/runner/florisboard"

read_property() {
  local version
  version="$(grep -w "$1" gradle/tools.versions.toml | awk -F'[="]' '{print $3}')"
  if [[ -z "$version" ]]; then
    version="$(grep "$1" gradle.properties | awk -F'[=]' '{print $2}')"
  fi
  if [[ -z "$version" ]]; then
    echo "fatal: $2 is empty, aborting"
    exit 1
  fi
  declare -g "$2"="$version"
  echo "$2 = $version"
}

read_property projectCompileSdk     PROJECT_COMPILE_SDK
read_property cmdlineTools          CMDLINE_TOOLS_VERSION
read_property cmdlineToolsChecksum  CMDLINE_TOOLS_CHECKSUM
read_property buildTools            BUILD_TOOLS_VERSION
read_property cmake                 CMAKE_VERSION
read_property jdk                   JDK_VERSION
read_property ndk                   NDK_VERSION
read_property rustup                RUSTUP_VERSION
read_property rustToolchain         RUST_TOOLCHAIN_VERSION

#if docker images | grep "$IMAGE_NAME" | grep -q "$IMAGE_TAG"; then
#  echo "Docker image $IMAGE_NAME:$IMAGE_TAG exists, using it"
#else
#  echo "Docker image $IMAGE_NAME:$IMAGE_TAG does not exist, building it"
  docker build -t "$IMAGE_NAME:$IMAGE_TAG" -f "utils/repr-build/Dockerfile" . \
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
#fi

docker_run_it() {
  docker run --rm -it \
    -w "$REPO_MOUNT" \
    "$IMAGE_NAME:$IMAGE_TAG" "$@"
}

docker_run() {
  local container
  container="fl-build-$(uuidgen)" || {
    echo "fatal: uuidgen not available, aborting"
    exit 1
  }
  docker run --name "$container" \
    -w "$REPO_MOUNT" \
    "$IMAGE_NAME:$IMAGE_TAG" "$@"
  docker rm "$container"
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
