/*
 * Version Management and Interoperability Module
 * Copyright (C) 2025 The FlorisBoard Contributors
 * 
 * Implements version management with upgrade/downgrade compatibility,
 * interoperability interfaces, and migration strategies.
 * 
 * Standards Compliance: IEEE 14764 (Software Maintenance),
 * ISO 9126 (Software Quality), Semantic Versioning 2.0.0
 */

package org.florisboard.lib.zipraf

import kotlinx.serialization.Serializable

/**
 * Version manager for handling compatibility across versions
 * 
 * Supports:
 * - Semantic versioning (major.minor.patch)
 * - Upgrade compatibility checks
 * - Downgrade compatibility checks
 * - Feature flag management
 * - Migration path planning
 */
class VersionManager {
    companion object {
        // Current module version
        const val MAJOR = 1
        const val MINOR = 0
        const val PATCH = 0
        
        val CURRENT_VERSION = SemanticVersion(MAJOR, MINOR, PATCH)
        
        // Minimum compatible version
        val MIN_COMPATIBLE_VERSION = SemanticVersion(1, 0, 0)
        
        // Risk calculation weights
        private const val MAJOR_RISK_WEIGHT = 0.5
        private const val MINOR_RISK_WEIGHT = 0.3
        private const val PATCH_RISK_WEIGHT = 0.1
    }
    
    /**
     * Checks if version is compatible with current version
     * 
     * @param version Version to check
     * @return Compatibility result
     */
    fun checkCompatibility(version: SemanticVersion): CompatibilityResult {
        // Major version must match for compatibility
        if (version.major != CURRENT_VERSION.major) {
            return CompatibilityResult(
                compatible = false,
                reason = "Major version mismatch",
                canUpgrade = version.major < CURRENT_VERSION.major,
                canDowngrade = version.major > CURRENT_VERSION.major
            )
        }
        
        // Check against minimum compatible version
        if (version < MIN_COMPATIBLE_VERSION) {
            return CompatibilityResult(
                compatible = false,
                reason = "Version below minimum compatible version",
                canUpgrade = true,
                canDowngrade = false
            )
        }
        
        return CompatibilityResult(
            compatible = true,
            reason = "Version compatible",
            canUpgrade = version < CURRENT_VERSION,
            canDowngrade = version > CURRENT_VERSION
        )
    }
    
    /**
     * Plans migration path from source to target version
     * 
     * @param from Source version
     * @param to Target version
     * @return Migration plan
     */
    fun planMigration(from: SemanticVersion, to: SemanticVersion): MigrationPlan {
        val steps = mutableListOf<MigrationStep>()
        
        if (from < to) {
            // Upgrade path
            steps.add(MigrationStep(
                fromVersion = from,
                toVersion = to,
                type = MigrationType.UPGRADE,
                description = "Upgrade from ${from} to ${to}",
                riskLevel = calculateRisk(from, to)
            ))
        } else if (from > to) {
            // Downgrade path
            steps.add(MigrationStep(
                fromVersion = from,
                toVersion = to,
                type = MigrationType.DOWNGRADE,
                description = "Downgrade from ${from} to ${to}",
                riskLevel = calculateRisk(from, to)
            ))
        }
        
        return MigrationPlan(
            sourceVersion = from,
            targetVersion = to,
            steps = steps,
            estimatedDurationMs = steps.size * 1000L
        )
    }
    
    /**
     * Calculates migration risk level
     * 
     * Risk is calculated based on weighted version component differences:
     * - Major version changes: 50% weight (highest risk)
     * - Minor version changes: 30% weight (moderate risk)
     * - Patch version changes: 10% weight (lowest risk)
     * 
     * @param from Source version
     * @param to Target version
     * @return Risk level (0.0 = no risk, 1.0 = high risk)
     */
    private fun calculateRisk(from: SemanticVersion, to: SemanticVersion): Double {
        val majorDiff = kotlin.math.abs(to.major - from.major)
        val minorDiff = kotlin.math.abs(to.minor - from.minor)
        val patchDiff = kotlin.math.abs(to.patch - from.patch)
        
        // Calculate weighted risk based on version component differences
        return (majorDiff * MAJOR_RISK_WEIGHT + 
                minorDiff * MINOR_RISK_WEIGHT + 
                patchDiff * PATCH_RISK_WEIGHT)
            .coerceIn(0.0, 1.0)
    }
}

/**
 * Semantic version representation
 * Follows Semantic Versioning 2.0.0 specification
 */
@Serializable
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }
    
    override fun toString(): String = "$major.$minor.$patch"
    
    companion object {
        /**
         * Parses version string in semantic versioning format
         * 
         * @param version Version string (e.g., "1.2.3")
         * @return Parsed version
         * @throws IllegalArgumentException if version format is invalid
         * @throws NumberFormatException if version components are not valid integers
         */
        fun parse(version: String): SemanticVersion {
            val parts = version.split(".")
            require(parts.size == 3) { "Invalid version format: $version (expected major.minor.patch)" }
            
            // Validate each part is a valid non-negative integer
            val major = parts[0].toIntOrNull()
                ?: throw NumberFormatException("Invalid major version: ${parts[0]}")
            val minor = parts[1].toIntOrNull()
                ?: throw NumberFormatException("Invalid minor version: ${parts[1]}")
            val patch = parts[2].toIntOrNull()
                ?: throw NumberFormatException("Invalid patch version: ${parts[2]}")
            
            require(major >= 0 && minor >= 0 && patch >= 0) {
                "Version components must be non-negative: $version"
            }
            
            return SemanticVersion(major, minor, patch)
        }
    }
}

/**
 * Compatibility check result
 */
@Serializable
data class CompatibilityResult(
    val compatible: Boolean,
    val reason: String,
    val canUpgrade: Boolean,
    val canDowngrade: Boolean
)

/**
 * Migration type enumeration
 */
enum class MigrationType {
    UPGRADE,
    DOWNGRADE,
    LATERAL // Same major version, different minor/patch
}

/**
 * Migration step
 * Represents a single step in a migration plan
 */
@Serializable
data class MigrationStep(
    val fromVersion: SemanticVersion,
    val toVersion: SemanticVersion,
    val type: MigrationType,
    val description: String,
    val riskLevel: Double
)

/**
 * Migration plan
 * Contains all steps needed to migrate between versions
 */
@Serializable
data class MigrationPlan(
    val sourceVersion: SemanticVersion,
    val targetVersion: SemanticVersion,
    val steps: List<MigrationStep>,
    val estimatedDurationMs: Long
)

/**
 * Interoperability adapter
 * Provides interface for cross-version communication
 */
interface InteroperabilityAdapter {
    /**
     * Gets supported versions
     * 
     * @return List of supported versions
     */
    fun getSupportedVersions(): List<SemanticVersion>
    
    /**
     * Adapts data from source version to target version
     * 
     * @param data Input data
     * @param sourceVersion Source version
     * @param targetVersion Target version
     * @return Adapted data
     */
    fun adaptData(
        data: Any,
        sourceVersion: SemanticVersion,
        targetVersion: SemanticVersion
    ): Any
    
    /**
     * Validates compatibility
     * 
     * @param version Version to check
     * @return true if compatible
     */
    fun isCompatible(version: SemanticVersion): Boolean
}

/**
 * Default interoperability adapter implementation
 */
class DefaultInteroperabilityAdapter : InteroperabilityAdapter {
    private val versionManager = VersionManager()
    
    override fun getSupportedVersions(): List<SemanticVersion> {
        return listOf(
            VersionManager.CURRENT_VERSION,
            VersionManager.MIN_COMPATIBLE_VERSION
        )
    }
    
    override fun adaptData(
        data: Any,
        sourceVersion: SemanticVersion,
        targetVersion: SemanticVersion
    ): Any {
        // In production, this would perform actual data transformation
        // based on schema changes between versions
        if (sourceVersion == targetVersion) {
            return data
        }
        
        // Placeholder for version-specific adaptations
        return data
    }
    
    override fun isCompatible(version: SemanticVersion): Boolean {
        val result = versionManager.checkCompatibility(version)
        return result.compatible
    }
}

/**
 * Feature flag manager for gradual rollout
 * Enables/disables features based on version and configuration
 */
class FeatureFlags {
    private val flags = mutableMapOf<String, Boolean>()
    
    /**
     * Registers a feature flag
     * 
     * @param name Feature name
     * @param enabled Initial state
     */
    fun register(name: String, enabled: Boolean = false) {
        flags[name] = enabled
    }
    
    /**
     * Checks if feature is enabled
     * 
     * @param name Feature name
     * @return true if enabled
     */
    fun isEnabled(name: String): Boolean {
        return flags[name] ?: false
    }
    
    /**
     * Enables feature
     * 
     * @param name Feature name
     */
    fun enable(name: String) {
        flags[name] = true
    }
    
    /**
     * Disables feature
     * 
     * @param name Feature name
     */
    fun disable(name: String) {
        flags[name] = false
    }
    
    /**
     * Gets all registered flags
     * 
     * @return Map of feature flags
     */
    fun getAll(): Map<String, Boolean> {
        return flags.toMap()
    }
}
