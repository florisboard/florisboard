# Interoperability and Versioning Guidelines

## Overview

This document outlines best practices for maintaining interoperability, version compatibility, and stable APIs in FlorisBoard.

## Version Management

### Semantic Versioning

FlorisBoard follows [Semantic Versioning 2.0.0](https://semver.org/):

- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible functionality additions
- **PATCH**: Backwards-compatible bug fixes

### API Stability

#### Public APIs
- All public APIs in `org.florisboard.*` packages must maintain backwards compatibility within major versions
- Deprecation notices must be provided at least one minor version before removal
- Breaking changes are only allowed in major version updates

#### Internal APIs
- Internal APIs (marked with `@InternalApi` or in `*.internal` packages) may change without notice
- These should not be used by external consumers

## Dependency Management

### Version Compatibility

#### Minimum SDK Requirements
- **minSdk**: Android 8.0 (API 26)
- **targetSdk**: Latest stable Android version
- **compileSdk**: Latest stable Android version

#### Kotlin Compatibility
- Use stable Kotlin versions
- Follow Kotlin's evolution policy
- Test against multiple Kotlin versions in CI

#### AndroidX Libraries
- Use stable AndroidX library versions
- Monitor AndroidX release notes for breaking changes
- Update dependencies incrementally with testing

### Dependency Best Practices

1. **Version Catalogs**: Use Gradle version catalogs (`libs.versions.toml`) for centralized dependency management
2. **Minimal Dependencies**: Only add dependencies that provide significant value
3. **Security Updates**: Regularly update dependencies to patch security vulnerabilities
4. **License Compatibility**: Ensure all dependencies are Apache 2.0 compatible

## Backwards Compatibility

### Data Migration

#### Shared Preferences
```kotlin
// Always provide migration paths for preference changes
object PreferenceMigration {
    fun migrateFrom(oldVersion: Int, newVersion: Int) {
        when {
            oldVersion < 2 && newVersion >= 2 -> migrateV1toV2()
            // Add more migrations as needed
        }
    }
}
```

#### Database Schemas
- Use Room migrations for database schema changes
- Never delete migration code - it may be needed for users upgrading from old versions
- Test migration paths thoroughly

### Configuration Files

#### Theme Files
- Support loading of legacy theme formats
- Provide clear error messages for unsupported formats
- Document format changes in release notes

#### Layout Files
- Maintain support for older layout definitions
- Gracefully handle unknown properties
- Provide sensible defaults for new properties

## Platform Interoperability

### Android System Integration

#### Input Method Framework
```kotlin
// Follow Android InputMethodService best practices
// Handle lifecycle events properly
override fun onCreate() {
    super.onCreate()
    // Initialization code
}

override fun onDestroy() {
    // Cleanup code
    super.onDestroy()
}
```

#### Content Providers
- Implement proper URI schemes
- Handle concurrent access safely
- Respect Android's permission model

### Extension System

#### Extension API
- Define clear extension interfaces
- Version extension APIs separately
- Provide compatibility layers for older extensions

#### Extension Manifest
```kotlin
data class ExtensionManifest(
    val id: String,
    val version: String,
    val minFlorisVersion: String,
    val maxFlorisVersion: String? = null
)
```

## Testing for Compatibility

### Automated Tests

#### Unit Tests
```kotlin
@Test
fun `test backwards compatible preference loading`() {
    // Test that old preference format can still be loaded
    val oldPrefs = loadLegacyPreferences()
    val newPrefs = migratePreferences(oldPrefs)
    assertEquals(expected, newPrefs.getValue())
}
```

#### Integration Tests
- Test upgrade scenarios from previous versions
- Verify data migration paths
- Ensure no data loss during upgrades

### Manual Testing Checklist

- [ ] Test fresh install
- [ ] Test upgrade from previous stable version
- [ ] Test upgrade from previous beta version
- [ ] Test downgrade scenarios (if supported)
- [ ] Verify user data preservation
- [ ] Check extension compatibility

## Release Process

### Pre-Release Checklist

1. **Version Update**
   - Update version code and name
   - Update changelog
   - Tag release in git

2. **Compatibility Testing**
   - Run full test suite
   - Test on minimum SDK device
   - Test on latest SDK device
   - Verify extension compatibility

3. **Documentation**
   - Update API documentation
   - Update user-facing documentation
   - Document breaking changes (if any)

### Release Notes Template

```markdown
## Version X.Y.Z

### New Features
- Feature description

### Improvements
- Improvement description

### Bug Fixes
- Bug fix description

### Breaking Changes (for major versions only)
- Breaking change description
- Migration guide

### Deprecations
- Deprecated API
- Replacement recommendation

### Minimum Requirements
- Android X.Y (API ZZ)
- FlorisBoard Extension API vX.Y
```

## Standards Compliance

### Relevant Standards

FlorisBoard aims to comply with industry best practices:

- **Android Design Guidelines**: Follow Material Design principles
- **Accessibility**: WCAG 2.1 Level AA compliance
- **Privacy**: GDPR, CCPA compliance for data handling
- **Security**: OWASP Mobile Top 10 awareness

### Code Quality

#### Static Analysis
- Use ktlint for Kotlin code style
- Run Android Lint checks
- Address all critical issues before release

#### Documentation
- Document all public APIs with KDoc
- Provide usage examples
- Keep documentation in sync with code

## Version Support Policy

### Support Tiers

1. **Latest Stable**: Full support, regular updates
2. **Previous Stable**: Security fixes only
3. **Older Versions**: No official support

### End of Life

- Major versions receive support for at least 6 months after the next major release
- Users are encouraged to upgrade to latest stable version
- Critical security fixes may be backported to supported versions

## References

- [Semantic Versioning](https://semver.org/)
- [Android API Levels](https://developer.android.com/guide/topics/manifest/uses-sdk-element)
- [Kotlin Evolution Principles](https://kotlinlang.org/docs/kotlin-evolution.html)
- [Material Design](https://material.io/design)
