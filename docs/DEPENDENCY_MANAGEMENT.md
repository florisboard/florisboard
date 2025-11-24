# Dependency Management Guidelines

## Overview

This document provides guidelines for managing dependencies in FlorisBoard, ensuring security, stability, and maintainability.

## Dependency Selection Criteria

### Evaluation Checklist

Before adding a new dependency, evaluate:

- [ ] **Necessity**: Does it solve a real problem that can't be easily solved in-house?
- [ ] **License**: Is it compatible with Apache 2.0?
- [ ] **Maintenance**: Is it actively maintained?
- [ ] **Security**: Are there known vulnerabilities?
- [ ] **Size**: What is the impact on APK size?
- [ ] **Stability**: Is it production-ready (not alpha/preview)?
- [ ] **Documentation**: Is it well-documented?
- [ ] **Community**: Does it have good community support?

### Preferred Sources

1. **AndroidX Libraries**: Official Android libraries
2. **Kotlin Libraries**: Official Kotlin libraries (kotlinx.*)
3. **Square Libraries**: Well-maintained, widely used (OkHttp, Retrofit, etc.)
4. **Google Libraries**: Material Components, Gson, etc.
5. **Community Libraries**: Established, well-maintained projects

## Version Management

### Version Catalogs

FlorisBoard uses Gradle Version Catalogs for centralized dependency management:

```toml
# gradle/libs.versions.toml

[versions]
kotlin = "1.9.20"
androidx-core = "1.12.0"
compose = "1.5.4"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidx-core" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui", version.ref = "compose" }

[plugins]
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

### Version Strategy

#### Semantic Versioning
- Use stable versions for production dependencies
- Avoid alpha/beta versions unless necessary
- Pin specific versions to ensure reproducible builds

#### Update Policy
- Review dependencies quarterly
- Update promptly for security patches
- Test thoroughly after major version updates
- Document breaking changes in PR

### Version Ranges

```kotlin
// Bad: Open-ended ranges can break builds
implementation("com.example:library:1.+")

// Good: Exact versions
implementation("com.example:library:1.2.3")

// Acceptable: Patch version ranges (after thorough testing)
implementation("com.example:library:[1.2.0,1.3.0)")
```

## Dependency Categories

### Core Dependencies

#### Android Platform
```kotlin
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
```

#### Kotlin Standard Library
```kotlin
dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}
```

#### Jetpack Compose
```kotlin
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
}
```

### Optional Dependencies

#### Testing
```kotlin
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
```

#### Development Tools
```kotlin
dependencies {
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.leakcanary.android)
}
```

## Security Considerations

### Vulnerability Scanning

#### Automated Scanning
- Enable Dependabot alerts on GitHub
- Use OWASP Dependency-Check in CI
- Review security advisories regularly

#### Response Process
1. **Identify**: Monitor security advisories
2. **Assess**: Determine impact on FlorisBoard
3. **Update**: Apply patches promptly
4. **Test**: Verify fixes don't break functionality
5. **Release**: Deploy security updates quickly

### Safe Dependency Practices

#### Verify Checksums
```kotlin
// Gradle verifies checksums automatically
// For manual verification:
// sha256sum downloaded-library.jar
```

#### Use HTTPS for Repositories
```kotlin
repositories {
    // Good: HTTPS
    maven { url = uri("https://repo.example.com") }
    
    // Bad: HTTP (insecure)
    // maven { url = uri("http://repo.example.com") }
}
```

#### Avoid Untrusted Sources
```kotlin
// Bad: Unknown repository
repositories {
    maven { url = uri("https://sketchy-repo.com") }
}

// Good: Well-known repositories
repositories {
    google()
    mavenCentral()
}
```

## Transitive Dependencies

### Managing Transitive Dependencies

#### Exclude Unwanted Dependencies
```kotlin
implementation("com.example:library:1.0.0") {
    exclude(group = "com.unwanted", module = "module")
}
```

#### Force Specific Versions
```kotlin
configurations.all {
    resolutionStrategy {
        force("com.example:library:1.2.3")
    }
}
```

#### View Dependency Tree
```bash
./gradlew app:dependencies
```

### Conflict Resolution

#### Version Conflicts
```kotlin
// When multiple dependencies require different versions
dependencies {
    implementation("com.example:library-a:1.0.0") // requires library-c:1.0.0
    implementation("com.example:library-b:1.0.0") // requires library-c:2.0.0
    
    // Force specific version
    implementation("com.example:library-c:2.0.0") {
        because("library-b requires 2.0.0 and is backwards compatible")
    }
}
```

## APK Size Optimization

### Minimize Dependencies

#### Avoid Heavy Libraries
```kotlin
// Bad: Adds 5MB for simple JSON parsing
implementation("com.example:heavy-json:1.0.0")

// Good: Use lightweight alternative
implementation("com.squareup.moshi:moshi:1.15.0")
```

#### Use ProGuard/R8
```kotlin
android {
    buildTypes {
        release {
            minifyEnabled = true
            shrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

### Split APKs

#### ABI Splits
```kotlin
android {
    splits {
        abi {
            isEnabled = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
}
```

## Native Dependencies

### NDK Libraries

#### JNI Bridges
```kotlin
dependencies {
    implementation(libs.native.library)
}
```

#### Native Library Loading
```kotlin
companion object {
    init {
        System.loadLibrary("native-lib")
    }
}
```

### Rust Dependencies

#### Rust Crate Integration
```toml
# Cargo.toml
[dependencies]
jni = "0.21"
```

```kotlin
// Kotlin side
external fun processNative(input: String): String
```

## Build Performance

### Dependency Configuration

#### Use Implementation vs API
```kotlin
dependencies {
    // Faster builds: dependency not exposed to consumers
    implementation(libs.internal.library)
    
    // Slower builds: dependency exposed to consumers
    api(libs.public.library)
}
```

#### Enable Gradle Caching
```properties
# gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

#### Use Dependency Constraints
```kotlin
dependencies {
    constraints {
        implementation("com.example:library:1.2.3") {
            because("previous versions have security issues")
        }
    }
}
```

## Documentation Requirements

### Dependency Documentation

When adding a dependency, document:

1. **Purpose**: Why is this dependency needed?
2. **Alternatives**: What alternatives were considered?
3. **Size Impact**: How much does it increase APK size?
4. **License**: What is its license?
5. **Maintenance**: Is it actively maintained?

#### Example
```kotlin
dependencies {
    // AboutLibraries: Automatically generates license screen
    // - Replaces manual license management
    // - APK size impact: ~100KB
    // - License: Apache 2.0
    // - Actively maintained by mikepenz
    implementation(libs.aboutlibraries.core)
}
```

## Dependency Update Process

### Update Workflow

1. **Check for Updates**
   ```bash
   ./gradlew dependencyUpdates
   ```

2. **Review Changes**
   - Read release notes
   - Check for breaking changes
   - Review migration guides

3. **Update Version Catalog**
   ```toml
   [versions]
   library = "2.0.0"  # Updated from 1.0.0
   ```

4. **Test Thoroughly**
   - Run all unit tests
   - Run integration tests
   - Manual testing on devices

5. **Document Changes**
   - Update changelog
   - Document breaking changes
   - Update documentation

### Automated Updates

#### Dependabot Configuration
```yaml
# .github/dependabot.yml
version: 2
updates:
  - package-ecosystem: "gradle"
    directory: "/"
    schedule:
      interval: "weekly"
    reviewers:
      - "maintainers"
```

## Best Practices Summary

### Do's
- ✅ Use version catalogs for dependency management
- ✅ Pin specific versions for reproducibility
- ✅ Keep dependencies up-to-date
- ✅ Monitor for security vulnerabilities
- ✅ Document why each dependency is needed
- ✅ Use stable versions in production
- ✅ Test after updates
- ✅ Consider APK size impact

### Don'ts
- ❌ Add dependencies without evaluation
- ❌ Use open-ended version ranges
- ❌ Ignore security advisories
- ❌ Use unmaintained libraries
- ❌ Add dependencies for trivial functionality
- ❌ Use incompatible licenses
- ❌ Skip testing after updates
- ❌ Ignore transitive dependencies

## Tools and Resources

### Analysis Tools
- **Gradle Dependency Report**: `./gradlew dependencies`
- **APK Analyzer**: Android Studio tool
- **Dependency-Check**: OWASP tool
- **Dependabot**: GitHub automated updates

### References
- [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html)
- [Android App Bundle](https://developer.android.com/guide/app-bundle)
- [Dependency Management Best Practices](https://docs.gradle.org/current/userguide/dependency_management.html)
- [OWASP Dependency-Check](https://owasp.org/www-project-dependency-check/)
