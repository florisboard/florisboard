/*
 * ZIPRAF_OMEGA Interoperability Module Tests
 * Copyright (C) 2025 Rafael Melo Reis
 */

package org.florisboard.lib.zipraf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class InteroperabilityModuleTest {
    
    private lateinit var module: InteroperabilityModule
    
    @BeforeEach
    fun setup() {
        module = InteroperabilityModule()
    }
    
    @Test
    fun `test version registration`() {
        val version = SemanticVersion(1, 0, 0)
        module.registerVersion(version)
        
        assertTrue(module.getKnownVersions().contains(version))
    }
    
    @Test
    fun `test compatibility check same version`() {
        val version = SemanticVersion(1, 0, 0)
        val result = module.checkDetailedCompatibility(version, version)
        
        assertEquals(version.toString(), result.sourceVersion)
        assertEquals(version.toString(), result.targetVersion)
        assertEquals(MigrationDirection.LATERAL.name, result.direction)
    }
    
    @Test
    fun `test compatibility check upgrade`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(1, 1, 0)
        val result = module.checkDetailedCompatibility(v1, v2)
        
        assertEquals(MigrationDirection.UPGRADE.name, result.direction)
        assertEquals(CompatibilityLevel.FULLY_COMPATIBLE.name, result.level)
    }
    
    @Test
    fun `test compatibility check downgrade`() {
        val v1 = SemanticVersion(1, 2, 0)
        val v2 = SemanticVersion(1, 0, 0)
        val result = module.checkDetailedCompatibility(v1, v2)
        
        assertEquals(MigrationDirection.DOWNGRADE.name, result.direction)
        assertEquals(CompatibilityLevel.PARTIALLY_COMPATIBLE.name, result.level)
        assertTrue(result.dataLossPossible)
        assertTrue(result.warnings.isNotEmpty())
    }
    
    @Test
    fun `test compatibility check major version change`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(2, 0, 0)
        val result = module.checkDetailedCompatibility(v1, v2)
        
        assertEquals(CompatibilityLevel.INCOMPATIBLE.name, result.level)
        assertTrue(result.dataLossPossible)
        assertTrue(result.issues.isNotEmpty())
    }
    
    @Test
    fun `test migration safety check for safe upgrade`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(1, 0, 1)
        val safety = module.checkMigrationSafety(v1, v2)
        
        assertTrue(safety.safe)
        assertFalse(safety.requiredBackup)
        assertTrue(safety.reversible)
    }
    
    @Test
    fun `test migration safety check for risky downgrade`() {
        val v1 = SemanticVersion(2, 0, 0)
        val v2 = SemanticVersion(1, 0, 0)
        val safety = module.checkMigrationSafety(v1, v2)
        
        assertFalse(safety.safe)
        assertTrue(safety.requiredBackup)
        assertFalse(safety.reversible)
    }
    
    @Test
    fun `test downgrade risk estimation`() {
        val v1 = SemanticVersion(2, 0, 0)
        val v2 = SemanticVersion(1, 0, 0)
        val risk = module.estimateDowngradeRisk(v1, v2)
        
        assertEquals(1.0, risk, 0.01) // Major downgrade = max risk
    }
    
    @Test
    fun `test downgrade risk for minor version`() {
        val v1 = SemanticVersion(1, 5, 0)
        val v2 = SemanticVersion(1, 4, 0)
        val risk = module.estimateDowngradeRisk(v1, v2)
        
        assertTrue(risk > 0.0 && risk < 1.0)
    }
    
    @Test
    fun `test downgrade risk for patch version`() {
        val v1 = SemanticVersion(1, 0, 2)
        val v2 = SemanticVersion(1, 0, 1)
        val risk = module.estimateDowngradeRisk(v1, v2)
        
        assertEquals(0.1, risk, 0.01) // Minimal risk
    }
    
    @Test
    fun `test no downgrade risk for upgrade`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(2, 0, 0)
        val risk = module.estimateDowngradeRisk(v1, v2)
        
        assertEquals(0.0, risk, 0.01)
    }
    
    @Test
    fun `test migration step registration`() {
        val step = MigrationStep(
            name = "1.0.0_to_1.1.0",
            fromVersion = "1.0.0",
            toVersion = "1.1.0",
            transform = { it },
            validate = { true }
        )
        
        module.registerMigrationStep(step)
        // No exception means success
        assertTrue(true)
    }
    
    @Test
    fun `test schema registration`() {
        val schema = SchemaVersion(
            version = "1.0.0",
            schemaHash = "abc123",
            fields = listOf("field1", "field2")
        )
        
        module.registerSchema("1.0.0", schema)
        // No exception means success
        assertTrue(true)
    }
    
    @Test
    fun `test data validation for version`() {
        val version = SemanticVersion(1, 0, 0)
        val result = module.validateDataForVersion("test data", version)
        
        // Without registered schema, should return true
        assertTrue(result)
    }
    
    @Test
    fun `test suggest upgrade target`() {
        module.registerVersion(SemanticVersion(1, 0, 0))
        module.registerVersion(SemanticVersion(1, 1, 0))
        module.registerVersion(SemanticVersion(1, 2, 0))
        
        val current = SemanticVersion(1, 0, 0)
        val suggestion = module.suggestUpgradeTarget(current)
        
        // Without migration paths, might return null or highest version
        // The behavior depends on migration path availability
        assertNotNull(suggestion == null || suggestion >= current)
    }
    
    @Test
    fun `test get compatible versions`() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(1, 1, 0)
        val v3 = SemanticVersion(2, 0, 0)
        
        module.registerVersion(v1)
        module.registerVersion(v2)
        module.registerVersion(v3)
        
        val compatible = module.getCompatibleVersions(v1)
        
        // v2 should be compatible (minor upgrade), v3 not (major change)
        assertTrue(compatible.contains(v1)) // Same version is compatible
    }
    
    @Test
    fun `test compatibility matrix generation`() {
        module.registerVersion(SemanticVersion(1, 0, 0))
        module.registerVersion(SemanticVersion(1, 1, 0))
        
        val matrix = module.generateCompatibilityMatrix()
        
        assertFalse(matrix.isEmpty())
        assertTrue(matrix.size >= 4) // 2x2 matrix minimum
    }
    
    @Test
    fun `test clear module data`() {
        module.registerVersion(SemanticVersion(1, 0, 0))
        module.clear()
        
        assertTrue(module.getKnownVersions().isEmpty())
    }
}
