# Gemini Session State

## Objective
Fix recurring "Transcription failed" errors in the FlorisFork keyboard application. The goal is to harden audio capture, add a foreground service for microphone access, ensure proper WAV formatting, and improve error logging.

## Current Status
I am currently implementing **Task 1: Add Foreground Microphone Service support**.

### Progress
- Added `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE` permissions to `AndroidManifest.xml`.
- Preparing to create the `SpeechCaptureService.kt` file as requested.

### Blocker & Investigation
- The user's instructions specified using `R.drawable.ic_mic` for the service notification icon, but this resource does not exist.
- To find the correct icon, I am investigating how the voice input is triggered.
- I have determined the following:
    1. The action is initiated by `KeyCode.VOICE_INPUT`.
    2. The integer value for `KeyCode.VOICE_INPUT` is `-233`, found in `app/src/main/kotlin/dev/patrickgold/florisboard/ime/text/key/KeyCode.kt`.
    3. I am currently searching the JSON keyboard layout files in `app/src/main/assets/ime/` for the code `-233` to identify the key definition and the icon it uses.

## Next Steps
1. Analyze the results of the search for the keycode `-233`.
2. Identify the correct drawable resource for the microphone icon.
3. Create the `SpeechCaptureService.kt` file using the identified icon.
4. Continue with the user's plan to harden the audio capture and transcription process.
