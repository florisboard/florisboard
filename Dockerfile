FROM debian:bookworm-slim

ARG PROJECT_COMPILE_SDK
ARG CMDLINE_TOOLS_VERSION
ARG CMDLINE_TOOLS_CHECKSUM
ARG BUILD_TOOLS_VERSION
ARG CMAKE_VERSION
ARG JDK_VERSION
ARG NDK_VERSION
ARG RUSTUP_VERSION
ARG RUST_TOOLCHAIN_VERSION
ARG REPO_MOUNT

# Install dependencies and OpenJDK
RUN apt-get update && apt-get install -y \
    curl \
    unzip \
    wget \
    git \
    build-essential \
    openjdk-${JDK_VERSION}-jdk \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/java-${JDK_VERSION}-openjdk-amd64

# Setup runner user
RUN useradd -m runner
USER runner
ENV HOME="/home/runner"
WORKDIR $HOME

# Install Rust
RUN curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --default-toolchain ${RUST_TOOLCHAIN_VERSION} --profile minimal
ENV PATH="$HOME/.cargo/bin:$PATH"

# Install sdkmanager
ARG CMDLINE_TOOLS_ZIP="commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip"
ENV ANDROID_HOME="$HOME/.android"
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

# Setup git
RUN git config --global --add safe.directory ${REPO_MOUNT}
