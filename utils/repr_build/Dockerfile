FROM debian:bookworm-slim AS setup_system

ARG PROJECT_COMPILE_SDK
ARG CMDLINE_TOOLS_VERSION
ARG CMDLINE_TOOLS_CHECKSUM
ARG BUILD_TOOLS_VERSION
ARG CMAKE_VERSION
ARG JDK_VERSION
ARG NDK_VERSION
ARG RUSTUP_VERSION
ARG RUST_TOOLCHAIN_VERSION

ARG USER_NAME
ARG USER_DIR="/home/${USER_NAME}"
ARG REPO_NAME="florisboard"
ARG REPO_DIR="${USER_DIR}/${REPO_NAME}"

# Install dependencies and OpenJDK
RUN apt-get update && apt-get install -y \
    curl \
    jq \
    unzip \
    wget \
    git \
    build-essential \
    openjdk-${JDK_VERSION}-jdk \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-${JDK_VERSION}-openjdk-amd64

# Setup runner user
RUN useradd -m "${USER_NAME}"
USER "${USER_NAME}"
ENV HOME="${USER_DIR}"
WORKDIR ${REPO_DIR}

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain ${RUST_TOOLCHAIN_VERSION} --profile minimal
ENV PATH="$HOME/.cargo/bin:$PATH"

# Install sdkmanager
ARG CMDLINE_TOOLS_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
ENV ANDROID_HOME="$HOME/Android/Sdk"
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && cd ${ANDROID_HOME}/cmdline-tools \
    && wget https://dl.google.com/android/repository/${CMDLINE_TOOLS_ZIP} \
    && echo "${CMDLINE_TOOLS_CHECKSUM} ${CMDLINE_TOOLS_ZIP}" | sha256sum -c - \
    && unzip ${CMDLINE_TOOLS_ZIP} \
    && rm ${CMDLINE_TOOLS_ZIP} \
    && mv cmdline-tools latest
ENV PATH="${ANDROID_HOME}/cmdline-tools/latest/bin:$PATH"

# Install build components
RUN yes | sdkmanager --install \
    "platform-tools" \
    "platforms;android-${PROJECT_COMPILE_SDK}" \
    "build-tools;${BUILD_TOOLS_VERSION}" \
    "cmake;${CMAKE_VERSION}" \
    "ndk;${NDK_VERSION}"
ENV ANDROID_NDK_HOME="${ANDROID_HOME}/ndk/${NDK_VERSION}"
ENV PATH="${ANDROID_HOME}/platform-tools:$PATH"

# Setup other stuff
RUN git config --global --add safe.directory "${REPO_DIR}"
RUN mkdir "${HOME}/.gradle"



FROM setup_system AS setup_androiddebugkey

RUN keytool -genkey -v \
  -keystore "${ANDROID_HOME}/debug.keystore" \
  -alias androiddebugkey \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"



FROM setup_androiddebugkey AS copy_proj

COPY --chown="${USER_NAME}:${USER_NAME}" . .
