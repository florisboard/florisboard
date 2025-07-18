#!/bin/bash

IMAGE_NAME="fl-repr-build"
IMAGE_TAG="default"
IMAGE="$IMAGE_NAME:$IMAGE_TAG"

USER_NAME="runner"
REPO_MOUNT="/home/$USER_NAME/florisboard"
GRADLE_CACHE_VOLUME_NAME="fl-repr-build-gradle-cache"
GRADLE_CACHE_VOLUME_BIND="$GRADLE_CACHE_VOLUME_NAME:/home/$USER_NAME/.gradle"

read_property() {
  local version
  version="$(grep -w "$1" gradle/tools.versions.toml | awk -F'[="]' '{print $3}')"
  if [ -z "$version" ]; then
    version="$(grep "$1" gradle.properties | awk -F'[=]' '{print $2}')"
  fi
  if [ -z "$version" ]; then
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

docker build -t "$IMAGE_NAME:$IMAGE_TAG" -f "utils/repr_build/Dockerfile" . \
  --build-arg "PROJECT_COMPILE_SDK=$PROJECT_COMPILE_SDK" \
  --build-arg "CMDLINE_TOOLS_VERSION=$CMDLINE_TOOLS_VERSION" \
  --build-arg "CMDLINE_TOOLS_CHECKSUM=$CMDLINE_TOOLS_CHECKSUM" \
  --build-arg "BUILD_TOOLS_VERSION=$BUILD_TOOLS_VERSION" \
  --build-arg "CMAKE_VERSION=$CMAKE_VERSION" \
  --build-arg "JDK_VERSION=$JDK_VERSION" \
  --build-arg "NDK_VERSION=$NDK_VERSION" \
  --build-arg "RUSTUP_VERSION=$RUSTUP_VERSION" \
  --build-arg "RUST_TOOLCHAIN_VERSION=$RUST_TOOLCHAIN_VERSION" \
  --build-arg "USER_NAME=$USER_NAME" \
  || exit 1

docker volume inspect "$GRADLE_CACHE_VOLUME_NAME" > /dev/null 2>&1 || docker volume create "$GRADLE_CACHE_VOLUME_NAME"

docker_run() {
  docker_run_with_env() {
    if [ "$FLSEC_SIGNING_ENABLED" = "1" ]; then
      local num_missing=0
      ensure_defined() {
        [ -n "${!1}" ] || {
          echo "fatal: $1 not provided, but required for signing"
          num_missing=$(( num_missing + 1 ))
        }
      }
      ensure_defined "FLSEC_KEYFILE"
      ensure_defined "FLSEC_KEYALIAS"
      ensure_defined "FLSEC_KS_PASS"
      ensure_defined "FLSEC_KEY_PASS"
      [ $num_missing -eq 0 ] || exit 1
      [ -f "$FLSEC_KEYFILE" ] || {
        echo "fatal: FLSEC_KEYFILE provided, but file '$FLSEC_KEYFILE' does not exist"
        exit 1
      }
      local KEYFILE_IN_CONTAINER="/home/$USER_NAME/keystore.jks"
      docker run \
        -e FLSEC_SIGNING_ENABLED=1 \
        -e FLSEC_KEYFILE="$KEYFILE_IN_CONTAINER" \
        -e FLSEC_KEYALIAS="$FLSEC_KEYALIAS" \
        -e FLSEC_KS_PASS="$FLSEC_KS_PASS" \
        -e FLSEC_KEY_PASS="$FLSEC_KEY_PASS" \
        -v "$FLSEC_KEYFILE:$KEYFILE_IN_CONTAINER:ro" \
        "$@"
    else
      docker run "$@"
    fi
  }
  docker_run_with_env \
    -w "$REPO_MOUNT" \
    --user "$USER_NAME" \
    -v "$GRADLE_CACHE_VOLUME_BIND" \
    "$@"
}

docker_run_assemble() {
  local container
  container="fl-build-$(uuidgen)" || {
    echo "fatal: uuidgen not available, aborting"
    exit 1
  }
  local track="$1"
  local tmp_out_dir="/tmp/$container"
  local final_out_dir="${2:-out}"
  mkdir -p "$tmp_out_dir" || exit 1
  mkdir -p "$final_out_dir" || exit 1
  docker_run --name "$container" "$IMAGE" ./utils/repr_build/scripts/assemble.sh "$track" && {
    # TODO: also copy on failure?
    docker cp "$container:$REPO_MOUNT/out" "$tmp_out_dir"
    cp -r "$tmp_out_dir/out"/* "$final_out_dir"
    rm -r "$tmp_out_dir"
  }
  docker rm "$container"
}

action="$1"
shift

case "$action" in
  "interactive"|"it")
    docker_run --rm -it "$IMAGE" /bin/bash
    ;;
  "assemble")
    docker_run_assemble "$@"
    ;;
  "clean")
    docker_run --rm "$IMAGE" ./gradlew clean --no-daemon
    ;;
  *)
    echo "Unknown action: $action"
    exit 1
    ;;
esac
