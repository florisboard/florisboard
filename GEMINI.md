# FlorisBoard Project Overview

## Project Summary
FlorisBoard is a free and open-source keyboard for Android devices (Android 8.0+). It is built with modern technologies, focusing on user-friendliness, customization, and privacy. The project is currently in a beta state.

## Core Technologies
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose
- **Build Tool:** Gradle

## Architecture
The project is a standard Android application with a multi-module structure. The main application module is in the `app` directory, and there are several library modules in the `lib` directory.

# Building and Running

## Build Command
To build the project from the command line, use the following command:

```bash
./gradlew clean && ./gradlew assembleDebug
```

This will create a debuggable APK file in the `app/build/outputs/apk/debug` directory.

**NOTE: Do not attempt to build the project locally. It gets built in the cloud runner.**

## Running the Application
The application can be run on an Android emulator or a physical device through Android Studio or by installing the generated APK file.

# Development Conventions

## Coding Style
The project follows standard Kotlin coding conventions.

## Testing
The project includes unit tests and Android instrumented tests. Unit tests can be run with the following command:

```bash
./gradlew test
```

Instrumented tests are located in the `app/src/androidTest` directory.

## Contributions
Contributions to the project are welcome. The `CONTRIBUTING.md` file provides detailed guidelines for contributing, including:
- All contributions must be in English.
- Translations are managed through Crowdin.
- Bug reports and feature requests should be submitted through the appropriate issue templates.
- A code of conduct must be followed.

# Whisper API Integration

This section outlines the integration of the OpenAI Whisper API for speech-to-text transcription.

## Overview

The Whisper API integration allows users to dictate text using their voice, which is then transcribed and inserted into the current text field.

## Code Location

The core logic for the Whisper API integration is located in the following file:

- `app/src/main/kotlin/dev/patrickgold/florisboard/FlorisImeService.kt`

## Implementation Details

### 1. Triggering Voice Input

- The voice input is triggered by pressing a key with the `KeyCode.VOICE_INPUT` code.
- The `app/src/main/kotlin/dev/patrickgold/florisboard/ime/keyboard/KeyboardManager.kt` file handles the key press and calls `FlorisImeService.startWhisperVoiceInput()`.

### 2. Audio Recording

- The `FlorisImeService.kt` file contains the `toggleWhisperVoiceInput()` method, which manages the audio recording.
- When voice input is activated, it checks for the `RECORD_AUDIO` permission.
- It creates a temporary audio file in the `.3gp` format using `MediaRecorder`.
- The audio is encoded using the `AMR_NB` encoder.

### 3. Transcription

- When the user stops the voice input, the `stopAndTranscribe()` method is called.
- This method sends the recorded audio file to the OpenAI Whisper API endpoint at `https://api.openai.com/v1/audio/transcriptions`.
- The request is a multipart form data POST request, including the audio file and the model name "whisper-1".
- The API key is retrieved from `BuildConfig.OPENAI_API_KEY`.

### 4. Response Handling and Cleanup

- The transcribed text from the API response is inserted into the active input connection.
- The temporary audio file is deleted after the transcription is complete or if an error occurs.
- The `onDestroy()` method in `FlorisImeService` includes cleanup logic to ensure that the `MediaRecorder` is released and any temporary files are deleted when the service is destroyed.