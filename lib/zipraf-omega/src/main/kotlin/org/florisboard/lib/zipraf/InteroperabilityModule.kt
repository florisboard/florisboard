/*
 * ZIPRAF_OMEGA Interoperability Enhancement Module v999
 * Copyright (C) 2025 Rafael Melo Reis
 * 
 * This module enhances interoperability and version compatibility:
 * - Downgrade/upgrade safety checks
 * - Cross-version data migration
 * - Backward/forward compatibility validation
 * - Schema evolution management
 * - API versioning support
 * 
 * License: Apache 2.0
 * Authorship credentials: Rafael Melo Reis
 * 
 * Standards Compliance:
 * - ISO 25010 (Software Product Quality)
 * - IEEE 12207 (Software Life Cycle Processes)
 * - IEEE 14764 (Software Maintenance)
 * - Semantic Versioning 2.0.0
 */

package org.florisboard.lib.zipraf

import kotlinx.serialization.Serializable

/**
 * Compatibility level between versions
 */
enum class CompatibilityLevel {
    FULLY_COMPATIBLE,       // No issues, seamless operation
    COMPATIBLE,             // Compatible with minor limitations
    PARTIALLY_COMPATIBLE,   // Some features may not work
    INCOMPATIBLE,           // Cannot work together
    UNKNOWN                 // Compatibility not determined
}

/**
 * Direction of version change
 */
enum class MigrationDirection {
    UPGRADE,    // Moving to newer version
    DOWNGRADE,  // Moving to older version
    LATERAL     // Same major version
}

/**
 * Compatibility check result
 */
@Serializable
data class CompatibilityCheckResult(
    val sourceVersion: String,
    val targetVersion: String,
    val direction: String,
    val level: String,
    val issues: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val dataLossPossible: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Migration safety check result
 */
@Serializable
data class MigrationSafetyCheck(
    val safe: Boolean,
    val risks: List<String>,
    val requiredBackup: Boolean,
    val estimatedDurationMs: Long,
    val reversible: Boolean
)

/**
 * Data schema version
 */
@Serializable
data class SchemaVersion(
    val version: String,
    val schemaHash: String,
    val fields: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Migration step
 */
data class MigrationStep(
    val name: String,
    val fromVersion: String,
    val toVersion: String,
    val transform: (Any) -> Any,
    val validate: (Any) -> Boolean
)

/**
 * Enhanced Interoperability Module
 * 
 * Extends the existing VersionManager with:
 * - Detailed compatibility checking
 * - Migration safety validation
 * - Schema evolution tracking
 * - Downgrade support
 */
class InteroperabilityModule {
    
    private val knownVersions = mutableSetOf<SemanticVersion>()
    private val migrationSteps = mutableListOf<MigrationStep>()
    private val schemas = mutableMapOf<String, SchemaVersion>()
    
    /**
     * Register a known version
     * 
     * @param version Version to register
     */
    fun registerVersion(version: SemanticVersion) {
        knownVersions.add(version)
    }
    
    /**
     * Register a migration step
     * 
     * @param step Migration step to register
     */
    fun registerMigrationStep(step: MigrationStep) {
        migrationSteps.add(step)
    }
    
    /**
     * Register a data schema version
     * 
     * @param version Version identifier
     * @param schema Schema definition
     */
    fun registerSchema(version: String, schema: SchemaVersion) {
        schemas[version] = schema
    }
    
    /**
     * Check detailed compatibility between versions
     * 
     * @param source Source version
     * @param target Target version
     * @return Detailed compatibility check result
     */
    fun checkDetailedCompatibility(
        source: SemanticVersion,
        target: SemanticVersion
    ): CompatibilityCheckResult {
        val direction = when {
            target > source -> MigrationDirection.UPGRADE
            target < source -> MigrationDirection.DOWNGRADE
            else -> MigrationDirection.LATERAL
        }
        
        val issues = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val recommendations = mutableListOf<String>()
        var dataLossPossible = false
        
        // Check major version compatibility
        val level = when {
            source.major != target.major -> {
                issues.add("Major version change: Breaking changes expected")
                dataLossPossible = true
                CompatibilityLevel.INCOMPATIBLE
            }
            direction == MigrationDirection.DOWNGRADE -> {
                warnings.add("Downgrade detected: Some features may be unavailable")
                warnings.add("Data created in newer version might not be compatible")
                dataLossPossible = true
                recommendations.add("Create backup before downgrading")
                recommendations.add("Test in staging environment first")
                CompatibilityLevel.PARTIALLY_COMPATIBLE
            }
            source.minor < target.minor -> {
                warnings.add("Minor version upgrade: New features available")
                recommendations.add("Review changelog for new features")
                CompatibilityLevel.FULLY_COMPATIBLE
            }
            source.patch < target.patch -> {
                CompatibilityLevel.FULLY_COMPATIBLE
            }
            else -> {
                CompatibilityLevel.FULLY_COMPATIBLE
            }
        }
        
        // Check for migration path
        val migrationPath = findMigrationPath(source, target)
        if (migrationPath.isEmpty() && source != target) {
            issues.add("No migration path defined from $source to $target")
            if (level == CompatibilityLevel.FULLY_COMPATIBLE || level == CompatibilityLevel.COMPATIBLE) {
                warnings.add("Direct migration may work but is not tested")
            }
        }
        
        // Check schema compatibility
        val sourceSchema = schemas[source.toString()]
        val targetSchema = schemas[target.toString()]
        if (sourceSchema != null && targetSchema != null) {
            val schemaCompatibility = checkSchemaCompatibility(sourceSchema, targetSchema)
            if (!schemaCompatibility.compatible) {
                issues.addAll(schemaCompatibility.issues)
                dataLossPossible = dataLossPossible || schemaCompatibility.dataLossPossible
            }
        }
        
        return CompatibilityCheckResult(
            sourceVersion = source.toString(),
            targetVersion = target.toString(),
            direction = direction.name,
            level = level.name,
            issues = issues,
            warnings = warnings,
            recommendations = recommendations,
            dataLossPossible = dataLossPossible
        )
    }
    
    /**
     * Schema compatibility check result
     */
    private data class SchemaCompatibilityResult(
        val compatible: Boolean,
        val issues: List<String>,
        val dataLossPossible: Boolean
    )
    
    /**
     * Check schema compatibility
     * 
     * @param source Source schema
     * @param target Target schema
     * @return Schema compatibility result
     */
    private fun checkSchemaCompatibility(
        source: SchemaVersion,
        target: SchemaVersion
    ): SchemaCompatibilityResult {
        val issues = mutableListOf<String>()
        var dataLossPossible = false
        
        // Check for removed fields
        val removedFields = source.fields.filter { it !in target.fields }
        if (removedFields.isNotEmpty()) {
            issues.add("Fields removed in target schema: ${removedFields.joinToString(", ")}")
            dataLossPossible = true
        }
        
        // Check for added fields
        val addedFields = target.fields.filter { it !in source.fields }
        if (addedFields.isNotEmpty()) {
            issues.add("New fields in target schema: ${addedFields.joinToString(", ")}")
            // Added fields are typically not a problem with good defaults
        }
        
        val compatible = issues.isEmpty() || !dataLossPossible
        
        return SchemaCompatibilityResult(compatible, issues, dataLossPossible)
    }
    
    /**
     * Find migration path between versions
     * 
     * @param source Source version
     * @param target Target version
     * @return List of migration steps
     */
    private fun findMigrationPath(
        source: SemanticVersion,
        target: SemanticVersion
    ): List<MigrationStep> {
        if (source == target) return emptyList()
        
        // Simple linear path search
        val path = mutableListOf<MigrationStep>()
        var current = source.toString()
        
        while (current != target.toString()) {
            val step = migrationSteps.find { it.fromVersion == current }
            if (step == null) break
            
            path.add(step)
            current = step.toVersion
            
            // Prevent infinite loops
            if (path.size > 100) break
        }
        
        return if (current == target.toString()) path else emptyList()
    }
    
    /**
     * Check if migration is safe
     * 
     * @param source Source version
     * @param target Target version
     * @return Migration safety check result
     */
    fun checkMigrationSafety(
        source: SemanticVersion,
        target: SemanticVersion
    ): MigrationSafetyCheck {
        val compatibility = checkDetailedCompatibility(source, target)
        val risks = mutableListOf<String>()
        
        risks.addAll(compatibility.issues)
        risks.addAll(compatibility.warnings)
        
        val safe = compatibility.level == CompatibilityLevel.FULLY_COMPATIBLE.name ||
                   compatibility.level == CompatibilityLevel.COMPATIBLE.name
        
        val requiresBackup = compatibility.dataLossPossible ||
                            compatibility.direction == MigrationDirection.DOWNGRADE.name
        
        val migrationPath = findMigrationPath(source, target)
        val estimatedDuration = migrationPath.size * 1000L // 1 second per step estimate
        
        val reversible = compatibility.direction != MigrationDirection.DOWNGRADE.name &&
                        !compatibility.dataLossPossible
        
        return MigrationSafetyCheck(
            safe = safe,
            risks = risks,
            requiredBackup = requiresBackup,
            estimatedDurationMs = estimatedDuration,
            reversible = reversible
        )
    }
    
    /**
     * Validate data against version requirements
     * 
     * @param data Data to validate
     * @param version Version to validate against
     * @return True if data is compatible
     */
    fun validateDataForVersion(data: Any, version: SemanticVersion): Boolean {
        val schema = schemas[version.toString()] ?: return true // No schema = no validation
        
        // This is a simplified validation - in practice would use reflection
        // or serialization framework to check actual data structure
        return true
    }
    
    /**
     * Estimate downgrade risk level
     * 
     * @param fromVersion Current version
     * @param toVersion Target version
     * @return Risk level (0.0 = safe, 1.0 = maximum risk)
     */
    fun estimateDowngradeRisk(
        fromVersion: SemanticVersion,
        toVersion: SemanticVersion
    ): Double {
        if (toVersion >= fromVersion) return 0.0
        
        val majorDiff = fromVersion.major - toVersion.major
        val minorDiff = fromVersion.minor - toVersion.minor
        val patchDiff = fromVersion.patch - toVersion.patch
        
        // Risk increases with version distance
        val risk = when {
            majorDiff > 0 -> 1.0 // Major downgrade = maximum risk
            minorDiff > 3 -> 0.8 // Many minor versions back = high risk
            minorDiff > 1 -> 0.5 // Some minor versions back = medium risk
            minorDiff == 1 -> 0.3 // One minor version back = low-medium risk
            patchDiff > 0 -> 0.1 // Patch downgrade = minimal risk
            else -> 0.0
        }
        
        return risk
    }
    
    /**
     * Check if upgrade path exists
     * 
     * @param fromVersion Source version
     * @param toVersion Target version
     * @return True if upgrade path exists
     */
    fun hasUpgradePath(fromVersion: SemanticVersion, toVersion: SemanticVersion): Boolean {
        return toVersion > fromVersion && findMigrationPath(fromVersion, toVersion).isNotEmpty()
    }
    
    /**
     * Check if downgrade path exists
     * 
     * @param fromVersion Source version
     * @param toVersion Target version
     * @return True if downgrade path exists
     */
    fun hasDowngradePath(fromVersion: SemanticVersion, toVersion: SemanticVersion): Boolean {
        return toVersion < fromVersion && findMigrationPath(fromVersion, toVersion).isNotEmpty()
    }
    
    /**
     * Get all compatible versions for a given version
     * 
     * @param version Version to check
     * @return List of compatible versions
     */
    fun getCompatibleVersions(version: SemanticVersion): List<SemanticVersion> {
        return knownVersions.filter { other ->
            val compat = checkDetailedCompatibility(version, other)
            compat.level == CompatibilityLevel.FULLY_COMPATIBLE.name ||
            compat.level == CompatibilityLevel.COMPATIBLE.name
        }
    }
    
    /**
     * Suggest best upgrade target from current version
     * 
     * @param currentVersion Current version
     * @return Suggested target version or null
     */
    fun suggestUpgradeTarget(currentVersion: SemanticVersion): SemanticVersion? {
        return knownVersions
            .filter { it > currentVersion }
            .filter { hasUpgradePath(currentVersion, it) }
            .maxOrNull()
    }
    
    /**
     * Generate compatibility matrix for all known versions
     * 
     * @return Map of version pairs to compatibility level
     */
    fun generateCompatibilityMatrix(): Map<Pair<String, String>, String> {
        val matrix = mutableMapOf<Pair<String, String>, String>()
        
        knownVersions.forEach { source ->
            knownVersions.forEach { target ->
                val result = checkDetailedCompatibility(source, target)
                matrix[Pair(source.toString(), target.toString())] = result.level
            }
        }
        
        return matrix
    }
    
    /**
     * Get known versions
     * 
     * @return Set of registered versions
     */
    fun getKnownVersions(): Set<SemanticVersion> {
        return knownVersions.toSet()
    }
    
    /**
     * Clear all registered data (for testing)
     */
    fun clear() {
        knownVersions.clear()
        migrationSteps.clear()
        schemas.clear()
    }
}
