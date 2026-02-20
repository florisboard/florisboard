# AGENTS.md — SpeekEZ

## Project Overview

SpeekEZ is an Android keyboard (IME) that adds voice-to-text transcription with AI text refinement. Built as a fork of Florisboard (Kotlin, Jetpack Compose). Supports two API modes: OpenRouter (single key for everything) or Separate Keys (OpenAI Whisper + Anthropic Claude). Users choose their provider setup and model tier (Cheap/Best/Custom).

## Architecture

### Module Structure

```
:app           — MainActivity, SpeekEZ companion app (Dashboard, History, Settings)
:keyboard      — SpeekEZInputMethodService, ComposeKeyboardView, Smartbar, Presets UI
:voice         — VoiceManager, AudioHandler, VoiceStateMachine
:api           — SttClient, RefinementClient interfaces + provider implementations + ApiRouterManager
:core          — Constants, NetworkUtils, Theme, Extensions
:security      — EncryptedPreferencesManager (API key storage)
:data          — Room database, DAOs, entities, repositories
:widget        — FloatingWidgetService, HomeScreenWidgetProvider(s)
```

### Package Naming

All packages under: `com.speekez.<module>`

Examples:
- `com.speekez.app` — Main app
- `com.speekez.keyboard` — Keyboard service
- `com.speekez.voice` — Voice recording and processing
- `com.speekez.api` — API clients
- `com.speekez.core` — Shared utilities
- `com.speekez.security` — Encrypted preferences
- `com.speekez.data` — Room database
- `com.speekez.widget` — Widgets

### Key Design Patterns

1. **Manual DI** — Following Florisboard pattern. Register managers in Application class. No Dagger/Hilt.
2. **Manager Pattern** — VoiceManager, PresetManager, etc. Registered in SpeekEZApplication.
3. **Observable State** — Use Kotlin StateFlow for UI reactivity. No LiveData.
4. **Compose-First UI** — All new UI in Jetpack Compose. Existing Florisboard Views wrapped with ComposeView.
5. **Repository Pattern** — Room DAOs wrapped in repositories. ViewModels use repositories.

### Data Flow

```
User holds preset icon → VoiceManager.startRecording(presetId)
  → AudioHandler records (16kHz mono, AAC, .m4a)
  → User releases → AudioHandler stops → returns File
  → NetworkUtils checks connectivity
  → ApiRouterManager.getSttClient().transcribe(audioFile, model, languages)
    (OpenRouter: base64 audio via chat/completions, OpenAI: multipart via /audio/transcriptions)
  → Raw text → ApiRouterManager.getRefinementClient().refine(rawText, model, systemPrompt)
    (OpenRouter: Claude via chat/completions, Anthropic: Claude via /messages)
  → Refined text → InputConnection.commitText(result)
  → Save to Room DB → Update stats → Delete audio file
```

## Coding Conventions

### Kotlin Style
- **Naming:** camelCase for functions/vars, PascalCase for classes, SCREAMING_SNAKE for constants
- **Null Safety:** Prefer non-null types. Use `?.let {}` over null checks. Never use `!!` except in tests.
- **Coroutines:** Use structured concurrency. Launch from lifecycle scope. Never use GlobalScope.
- **Extensions:** Prefer extension functions for utility methods on existing types.

### File Organization
- One class per file (exceptions: sealed classes with small variants, data classes used together)
- File name matches primary class name
- Group related files in packages, not flat directories

### Error Handling
- Network errors: catch specific exceptions (IOException, HttpException), wrap in Result<T>
- Never catch generic Exception unless re-throwing
- User-visible errors: use sealed class ErrorState with specific variants
- Log errors with tag matching class name: `Log.e("VoiceManager", "...")`

### API Clients
- Use Retrofit + OkHttp for REST APIs
- Provider-agnostic: code against SttClient and RefinementClient interfaces
- OpenRouter audio: base64-encoded audio in chat/completions input_audio block
- OpenAI Whisper: multipart form upload to /audio/transcriptions
- Anthropic Claude: JSON body to /v1/messages with x-api-key header
- OpenRouter Claude: JSON body to /api/v1/chat/completions with Bearer token
- Exponential backoff: 1s, 2s, 4s — max 3 retries
- Timeout: 30s connect, 60s read (audio processing needs time)

### Database (Room)
- Entity classes in `com.speekez.data.entity`
- DAOs in `com.speekez.data.dao`
- Database class: `SpeekEZDatabase`
- Repositories in `com.speekez.data.repository`
- Migrations: incremental, never destructive in production

### Security
- API key: EncryptedSharedPreferences (AES-256, Android Keystore)
- Never log API keys or tokens
- Audio files: app cache directory, delete after transcription
- No analytics, no telemetry, no tracking

### UI (Jetpack Compose)
- Theme colors defined in `com.speekez.core.theme`
- Use MaterialTheme for consistent styling
- Dark theme: deep backgrounds (#0a0a14, #12121f, #1a1a2e), teal accent (#00d4aa)
- Light theme: white/light gray backgrounds, teal accent, dark text
- Accessibility: contentDescription on all interactive elements
- Settings has 3 sections: General, Model, Presets

### Testing
- Unit tests: JUnit 5 + MockK
- UI tests: Compose testing rules
- Integration tests: for API clients using MockWebServer
- Test file naming: `<ClassName>Test.kt`
- Include relevant tests in every task (not as separate test-only tasks)

## Important Files

### Core Entry Points
- `FlorisImeService.kt` → Renamed/extended as `SpeekEZInputMethodService`
- `FlorisApplication.kt` → Extended as `SpeekEZApplication`
- `ImeRootView.kt` — Bridge between Android Views and Jetpack Compose

### Voice Pipeline
- `VoiceManager.kt` — Orchestrates recording → transcription → refinement → commit
- `AudioHandler.kt` — MediaRecorder wrapper (16kHz mono M4A)
- `VoiceStateMachine.kt` — States: IDLE, RECORDING, TRANSCRIBING, REFINING, COMMITTING, ERROR

### API
- `ApiRouterManager.kt` — Resolves which provider implementation to use based on keys/mode
- `SttClient.kt` — Interface for speech-to-text
- `OpenRouterAudioClient.kt` — STT via OpenRouter chat/completions (base64 audio)
- `OpenAiWhisperClient.kt` — STT via OpenAI /audio/transcriptions (multipart)
- `RefinementClient.kt` — Interface for text refinement
- `OpenRouterClaudeClient.kt` — Refinement via OpenRouter chat/completions
- `AnthropicClaudeClient.kt` — Refinement via Anthropic /messages

### Keyboard UI
- `Smartbar.kt` — Toolbar above keyboard, holds preset icons and mic button
- `ComposeKeyboardView.kt` — Main keyboard Compose UI
- `PresetChip.kt` — Individual preset icon in smartbar

### App UI
- `MainActivity.kt` — Bottom navigation: Dashboard, History, Settings
- `DashboardScreen.kt` — Stats cards, weekly chart, recent transcriptions
- `HistoryScreen.kt` — Searchable transcription list
- `SettingsScreen.kt` — General settings, Model settings, Preset management

## Build & Run

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Run unit tests
./gradlew test

# Run lint
./gradlew lint

# Install on device
./gradlew :app:installDebug
```

## Dependencies

Key dependencies (see gradle/libs.versions.toml for versions):
- Kotlin 2.0+
- Jetpack Compose (BOM)
- Room (database)
- Retrofit + OkHttp (networking)
- EncryptedSharedPreferences (security)
- Coroutines (async)
- Material3 (theming)
- JUnit 5 + MockK (testing)

## Spelling

Always use **SpeekEZ** (capital S, E, Z). Never "Speakeasy", "speakeasy", or "SpeakEZ".
