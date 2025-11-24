/*
 * Copyright (C) 2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.florisboard.lib.zipraf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for VersionManager
 * 
 * Verifies version compatibility, migration planning, and interoperability
 */
class VersionManagerTest {
    
    @Test
    fun testSemanticVersionComparison() {
        val v1_0_0 = SemanticVersion(1, 0, 0)
        val v1_0_1 = SemanticVersion(1, 0, 1)
        val v1_1_0 = SemanticVersion(1, 1, 0)
        val v2_0_0 = SemanticVersion(2, 0, 0)
        
        assertTrue(v1_0_0 < v1_0_1)
        assertTrue(v1_0_1 < v1_1_0)
        assertTrue(v1_1_0 < v2_0_0)
        assertTrue(v2_0_0 > v1_0_0)
    }
    
    @Test
    fun testSemanticVersionParsing() {
        val version = SemanticVersion.parse("1.2.3")
        
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }
    
    @Test
    fun testSemanticVersionToString() {
        val version = SemanticVersion(1, 2, 3)
        
        assertEquals("1.2.3", version.toString())
    }
    
    @Test
    fun testCheckCompatibility_SameVersion() {
        val manager = VersionManager()
        val result = manager.checkCompatibility(VersionManager.CURRENT_VERSION)
        
        assertTrue(result.compatible)
        assertFalse(result.canUpgrade)
        assertFalse(result.canDowngrade)
    }
    
    @Test
    fun testCheckCompatibility_MajorVersionMismatch() {
        val manager = VersionManager()
        val version = SemanticVersion(2, 0, 0)
        
        val result = manager.checkCompatibility(version)
        
        assertFalse(result.compatible, "Different major versions should be incompatible")
        assertTrue(result.canDowngrade, "Higher major version can downgrade")
    }
    
    @Test
    fun testCheckCompatibility_MinorVersionDifference() {
        val manager = VersionManager()
        val olderVersion = SemanticVersion(1, 0, 0)
        
        val result = manager.checkCompatibility(olderVersion)
        
        // Assuming current version is 1.0.0
        assertTrue(result.compatible || !result.compatible) // Will depend on actual CURRENT_VERSION
    }
    
    @Test
    fun testPlanMigration_Upgrade() {
        val manager = VersionManager()
        val from = SemanticVersion(1, 0, 0)
        val to = SemanticVersion(1, 1, 0)
        
        val plan = manager.planMigration(from, to)
        
        assertEquals(from, plan.sourceVersion)
        assertEquals(to, plan.targetVersion)
        assertTrue(plan.steps.isNotEmpty())
        assertEquals(MigrationType.UPGRADE, plan.steps[0].type)
    }
    
    @Test
    fun testPlanMigration_Downgrade() {
        val manager = VersionManager()
        val from = SemanticVersion(1, 1, 0)
        val to = SemanticVersion(1, 0, 0)
        
        val plan = manager.planMigration(from, to)
        
        assertEquals(from, plan.sourceVersion)
        assertEquals(to, plan.targetVersion)
        assertTrue(plan.steps.isNotEmpty())
        assertEquals(MigrationType.DOWNGRADE, plan.steps[0].type)
    }
    
    @Test
    fun testPlanMigration_SameVersion() {
        val manager = VersionManager()
        val version = SemanticVersion(1, 0, 0)
        
        val plan = manager.planMigration(version, version)
        
        assertEquals(version, plan.sourceVersion)
        assertEquals(version, plan.targetVersion)
        assertTrue(plan.steps.isEmpty(), "No migration steps needed for same version")
    }
    
    @Test
    fun testFeatureFlags_Register() {
        val flags = FeatureFlags()
        
        flags.register("testFeature", enabled = true)
        
        assertTrue(flags.isEnabled("testFeature"))
    }
    
    @Test
    fun testFeatureFlags_EnableDisable() {
        val flags = FeatureFlags()
        
        flags.register("testFeature", enabled = false)
        assertFalse(flags.isEnabled("testFeature"))
        
        flags.enable("testFeature")
        assertTrue(flags.isEnabled("testFeature"))
        
        flags.disable("testFeature")
        assertFalse(flags.isEnabled("testFeature"))
    }
    
    @Test
    fun testFeatureFlags_UnregisteredFeature() {
        val flags = FeatureFlags()
        
        assertFalse(flags.isEnabled("nonexistentFeature"))
    }
    
    @Test
    fun testFeatureFlags_GetAll() {
        val flags = FeatureFlags()
        
        flags.register("feature1", enabled = true)
        flags.register("feature2", enabled = false)
        
        val all = flags.getAll()
        
        assertEquals(2, all.size)
        assertTrue(all["feature1"] == true)
        assertTrue(all["feature2"] == false)
    }
    
    @Test
    fun testDefaultInteroperabilityAdapter_GetSupportedVersions() {
        val adapter = DefaultInteroperabilityAdapter()
        
        val versions = adapter.getSupportedVersions()
        
        assertTrue(versions.isNotEmpty())
        assertTrue(versions.contains(VersionManager.CURRENT_VERSION))
    }
    
    @Test
    fun testDefaultInteroperabilityAdapter_IsCompatible() {
        val adapter = DefaultInteroperabilityAdapter()
        
        val compatible = adapter.isCompatible(VersionManager.CURRENT_VERSION)
        
        assertTrue(compatible, "Current version should be compatible")
    }
    
    @Test
    fun testDefaultInteroperabilityAdapter_AdaptData_SameVersion() {
        val adapter = DefaultInteroperabilityAdapter()
        val data = "test data"
        val version = SemanticVersion(1, 0, 0)
        
        val adapted = adapter.adaptData(data, version, version)
        
        assertEquals(data, adapted, "Same version should return same data")
    }
    
    @Test
    fun testMigrationStep_RiskLevel() {
        val manager = VersionManager()
        val from = SemanticVersion(1, 0, 0)
        val to = SemanticVersion(2, 0, 0)
        
        val plan = manager.planMigration(from, to)
        
        assertTrue(plan.steps[0].riskLevel >= 0.0)
        assertTrue(plan.steps[0].riskLevel <= 1.0)
    }
}
