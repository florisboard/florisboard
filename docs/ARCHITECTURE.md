# Architecture and Code Structure Guidelines

## Overview

This document outlines the architectural principles, code organization, and design patterns used in FlorisBoard to ensure maintainability, testability, and scalability.

## Project Structure

### Module Organization

```
florisboard/
├── app/                    # Main application module
│   ├── src/main/
│   │   ├── kotlin/        # Kotlin source files
│   │   └── res/           # Android resources
├── lib/                    # Reusable library modules
│   ├── android/           # Android utilities
│   ├── color/             # Color utilities
│   ├── compose/           # Compose components
│   ├── kotlin/            # Kotlin utilities
│   ├── native/            # Native code bridges
│   └── snygg/             # Theme engine
└── docs/                   # Documentation
```

### Package Structure

```kotlin
org.florisboard
├── app                     # Application layer
│   ├── ui                 # User interface
│   ├── setup              # Setup wizard
│   └── settings           # Settings screens
├── ime                     # Input method engine
│   ├── core               # Core IME functionality
│   ├── keyboard           # Keyboard layouts
│   ├── text               # Text processing
│   ├── nlp                # Natural language processing
│   ├── clipboard          # Clipboard management
│   ├── theme              # Theming system
│   └── extension          # Extension system
└── lib                     # Shared utilities
    ├── android            # Android extensions
    ├── io                 # I/O operations
    ├── cache              # Caching
    └── util               # General utilities
```

## Architectural Patterns

### Layer Architecture

FlorisBoard follows a layered architecture:

```
┌─────────────────────────────────────┐
│         Presentation Layer          │
│  (Compose UI, ViewModels, State)    │
└─────────────────────────────────────┘
              │
┌─────────────────────────────────────┐
│          Domain Layer                │
│   (Use Cases, Business Logic)       │
└─────────────────────────────────────┘
              │
┌─────────────────────────────────────┐
│           Data Layer                 │
│  (Repositories, Data Sources)       │
└─────────────────────────────────────┘
```

#### Presentation Layer
```kotlin
// Composable UI
@Composable
fun KeyboardScreen(
    viewModel: KeyboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    KeyboardContent(state = state)
}

// ViewModel
class KeyboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()
    
    fun onKeyPress(key: Key) {
        viewModelScope.launch {
            processKeyUseCase(key)
        }
    }
}
```

#### Domain Layer
```kotlin
// Use Case
class ProcessKeyUseCase(
    private val textProcessor: TextProcessor,
    private val inputConnection: InputConnection
) {
    suspend operator fun invoke(key: Key): Result<Unit> {
        return try {
            val processedText = textProcessor.process(key)
            inputConnection.commitText(processedText)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

#### Data Layer
```kotlin
// Repository
interface ThemeRepository {
    suspend fun getTheme(id: String): Theme
    suspend fun saveTheme(theme: Theme)
    fun observeThemes(): Flow<List<Theme>>
}

class ThemeRepositoryImpl(
    private val localDataSource: ThemeLocalDataSource,
    private val remoteDataSource: ThemeRemoteDataSource
) : ThemeRepository {
    override suspend fun getTheme(id: String): Theme {
        return localDataSource.getTheme(id) 
            ?: remoteDataSource.getTheme(id).also {
                localDataSource.saveTheme(it)
            }
    }
}
```

### Design Patterns

#### Repository Pattern
```kotlin
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    val preferences: Flow<UserPreferences> = dataStore.data.map { 
        it.toUserPreferences() 
    }
    
    suspend fun updatePreference(key: String, value: Any) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
```

#### Factory Pattern
```kotlin
interface KeyboardLayoutFactory {
    fun create(type: LayoutType): KeyboardLayout
}

class KeyboardLayoutFactoryImpl : KeyboardLayoutFactory {
    override fun create(type: LayoutType): KeyboardLayout {
        return when (type) {
            LayoutType.QWERTY -> QwertyLayout()
            LayoutType.AZERTY -> AzertyLayout()
            LayoutType.DVORAK -> DvorakLayout()
        }
    }
}
```

#### Observer Pattern
```kotlin
interface ThemeObserver {
    fun onThemeChanged(theme: Theme)
}

class ThemeManager {
    private val observers = mutableListOf<ThemeObserver>()
    
    fun registerObserver(observer: ThemeObserver) {
        observers.add(observer)
    }
    
    fun unregisterObserver(observer: ThemeObserver) {
        observers.remove(observer)
    }
    
    private fun notifyThemeChanged(theme: Theme) {
        observers.forEach { it.onThemeChanged(theme) }
    }
}
```

#### Strategy Pattern
```kotlin
interface TextPredictionStrategy {
    suspend fun predict(context: String): List<String>
}

class MarkovChainStrategy : TextPredictionStrategy {
    override suspend fun predict(context: String): List<String> {
        // Markov chain prediction
    }
}

class NeuralNetworkStrategy : TextPredictionStrategy {
    override suspend fun predict(context: String): List<String> {
        // Neural network prediction
    }
}
```

## Code Organization Principles

### Single Responsibility Principle

Each class should have one reason to change:

```kotlin
// Bad: Multiple responsibilities
class KeyboardManager {
    fun handleKeyPress() { }
    fun saveSettings() { }
    fun loadTheme() { }
    fun sendAnalytics() { }
}

// Good: Separated responsibilities
class KeyPressHandler {
    fun handleKeyPress() { }
}

class SettingsManager {
    fun saveSettings() { }
}

class ThemeLoader {
    fun loadTheme() { }
}
```

### Dependency Inversion Principle

Depend on abstractions, not concretions:

```kotlin
// Bad: Depends on concrete implementation
class TextProcessor(
    private val spellChecker: HunspellSpellChecker
) {
    fun process(text: String) {
        if (!spellChecker.check(text)) {
            // ...
        }
    }
}

// Good: Depends on interface
interface SpellChecker {
    fun check(text: String): Boolean
}

class TextProcessor(
    private val spellChecker: SpellChecker
) {
    fun process(text: String) {
        if (!spellChecker.check(text)) {
            // ...
        }
    }
}
```

### Interface Segregation

Keep interfaces focused and minimal:

```kotlin
// Bad: Fat interface
interface KeyboardComponent {
    fun onKeyPress()
    fun onKeyRelease()
    fun onSwipe()
    fun onLongPress()
    fun loadTheme()
    fun saveSettings()
}

// Good: Segregated interfaces
interface KeyEventHandler {
    fun onKeyPress()
    fun onKeyRelease()
}

interface GestureHandler {
    fun onSwipe()
    fun onLongPress()
}

interface Themeable {
    fun loadTheme()
}
```

## State Management

### Unidirectional Data Flow

```kotlin
// State
data class KeyboardState(
    val currentLayout: Layout = Layout.QWERTY,
    val capsLockEnabled: Boolean = false,
    val suggestions: List<String> = emptyList()
)

// Events
sealed class KeyboardEvent {
    data class KeyPressed(val key: Key) : KeyboardEvent()
    object ToggleCapsLock : KeyboardEvent()
    data class SuggestionSelected(val text: String) : KeyboardEvent()
}

// ViewModel
class KeyboardViewModel : ViewModel() {
    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()
    
    fun handleEvent(event: KeyboardEvent) {
        when (event) {
            is KeyboardEvent.KeyPressed -> handleKeyPress(event.key)
            KeyboardEvent.ToggleCapsLock -> toggleCapsLock()
            is KeyboardEvent.SuggestionSelected -> applySuggestion(event.text)
        }
    }
    
    private fun handleKeyPress(key: Key) {
        _state.update { it.copy(/* updates */) }
    }
}
```

### Compose State

```kotlin
// Stateless Composable
@Composable
fun KeyButton(
    key: Key,
    onClick: (Key) -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { onClick(key) },
        modifier = modifier
    ) {
        Text(key.label)
    }
}

// Stateful Composable
@Composable
fun KeyboardLayout(
    viewModel: KeyboardViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    
    KeyboardLayoutContent(
        state = state,
        onKeyClick = { key ->
            viewModel.handleEvent(KeyboardEvent.KeyPressed(key))
        }
    )
}
```

## Error Handling

### Result Type

```kotlin
// Define Result type
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// Usage
suspend fun loadDictionary(): Result<Dictionary> {
    return try {
        val dictionary = dictionaryLoader.load()
        Result.Success(dictionary)
    } catch (e: IOException) {
        Result.Error(e)
    }
}

// Handling
when (val result = loadDictionary()) {
    is Result.Success -> use(result.data)
    is Result.Error -> handle(result.exception)
    Result.Loading -> showLoading()
}
```

### Exception Hierarchy

```kotlin
// Base exception
sealed class FlorisException(message: String) : Exception(message)

// Specific exceptions
class ThemeNotFoundException(themeId: String) : 
    FlorisException("Theme not found: $themeId")

class InvalidLayoutException(reason: String) : 
    FlorisException("Invalid layout: $reason")

class DictionaryLoadException(cause: Throwable) : 
    FlorisException("Failed to load dictionary: ${cause.message}")
```

## Testing Strategy

### Unit Tests

```kotlin
class TextProcessorTest {
    private lateinit var textProcessor: TextProcessor
    
    @Before
    fun setup() {
        textProcessor = TextProcessor()
    }
    
    @Test
    fun `process should capitalize first letter`() {
        val input = "hello"
        val result = textProcessor.process(input)
        assertEquals("Hello", result)
    }
}
```

### Integration Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class KeyboardServiceTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(TestActivity::class.java)
    
    @Test
    fun testKeyboardIntegration() {
        // Test keyboard interaction with Android system
    }
}
```

### UI Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class KeyboardUITest {
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testKeyPress() {
        composeTestRule.setContent {
            KeyboardScreen()
        }
        
        composeTestRule.onNodeWithText("A").performClick()
        // Verify expected behavior
    }
}
```

## Code Quality Standards

### Naming Conventions

```kotlin
// Classes: PascalCase
class KeyboardManager

// Functions: camelCase
fun processKeyPress()

// Constants: UPPER_SNAKE_CASE
const val MAX_SUGGESTIONS = 5

// Private properties: camelCase with underscore
private val _state = MutableStateFlow()

// Properties: camelCase
val state: StateFlow<State>

// Interfaces: PascalCase (no "I" prefix)
interface ThemeProvider
```

### Documentation

```kotlin
/**
 * Processes a key press and updates the input connection.
 *
 * This function handles the complete key press lifecycle including:
 * - Key validation
 * - Text processing
 * - Suggestion updates
 * - Input connection commit
 *
 * @param key The key that was pressed
 * @return Result indicating success or failure
 * @throws IllegalArgumentException if key is invalid
 */
suspend fun processKeyPress(key: Key): Result<Unit>
```

### Code Style

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use trailing commas
- Prefer expression bodies for simple functions
- Use named arguments for clarity

```kotlin
// Good code style
data class Theme(
    val id: String,
    val name: String,
    val colors: ThemeColors,
    val isDark: Boolean = false,
)

fun calculateSize(width: Int, height: Int) = width * height

fun createTheme(
    id: String,
    name: String,
    isDark: Boolean = false,
): Theme = Theme(
    id = id,
    name = name,
    colors = generateColors(isDark),
    isDark = isDark,
)
```

## Performance Considerations

### Lazy Initialization

```kotlin
class ResourceManager {
    private val heavyResource by lazy {
        loadHeavyResource()
    }
    
    fun getResource() = heavyResource
}
```

### Coroutine Scope Management

```kotlin
class KeyboardService : LifecycleService() {
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
    
    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

### Memory-Efficient Collections

```kotlin
// Use appropriate collection types
val smallSet: Set<String> = setOf("a", "b")  // Optimized for small sets
val largeSet: Set<String> = HashSet()         // Optimized for large sets

// Use sequences for large operations
val result = largeList.asSequence()
    .filter { it.isValid() }
    .map { it.transform() }
    .toList()
```

## Best Practices Summary

### Do's
- ✅ Follow SOLID principles
- ✅ Write testable code
- ✅ Use dependency injection
- ✅ Document public APIs
- ✅ Handle errors gracefully
- ✅ Use appropriate design patterns
- ✅ Keep functions small and focused
- ✅ Use type-safe builders

### Don'ts
- ❌ Create God classes
- ❌ Use global mutable state
- ❌ Ignore exceptions
- ❌ Mix concerns
- ❌ Create deep inheritance hierarchies
- ❌ Use reflection unnecessarily
- ❌ Ignore code smells
- ❌ Skip documentation

## References

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Effective Kotlin](https://kt.academy/book/effectivekotlin)
