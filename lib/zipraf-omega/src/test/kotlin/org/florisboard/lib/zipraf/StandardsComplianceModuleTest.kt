/*
 * ZIPRAF_OMEGA Standards Compliance Module Tests
 * Copyright (C) 2025 Rafael Melo Reis
 */

package org.florisboard.lib.zipraf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StandardsComplianceModuleTest {
    
    private lateinit var module: StandardsComplianceModule
    
    @BeforeEach
    fun setup() {
        module = StandardsComplianceModule()
    }
    
    @Test
    fun `test module has predefined standards`() {
        val standards = module.getAllStandards()
        assertTrue(standards.isNotEmpty())
        assertTrue(standards.size >= 15) // Should have at least the common standards
    }
    
    @Test
    fun `test ISO standards are registered`() {
        val isoStandards = module.getStandardsByCategory(StandardCategory.ISO)
        assertTrue(isoStandards.isNotEmpty())
        assertTrue(isoStandards.any { it.id == "ISO-9001" })
        assertTrue(isoStandards.any { it.id == "ISO-27001" })
    }
    
    @Test
    fun `test IEEE standards are registered`() {
        val ieeeStandards = module.getStandardsByCategory(StandardCategory.IEEE)
        assertTrue(ieeeStandards.isNotEmpty())
        assertTrue(ieeeStandards.any { it.id == "IEEE-830" })
        assertTrue(ieeeStandards.any { it.id == "IEEE-1012" })
    }
    
    @Test
    fun `test NIST standards are registered`() {
        val nistStandards = module.getStandardsByCategory(StandardCategory.NIST)
        assertTrue(nistStandards.isNotEmpty())
        assertTrue(nistStandards.any { it.id == "NIST-CSF" })
        assertTrue(nistStandards.any { it.id == "NIST-800-53" })
    }
    
    @Test
    fun `test custom standard registration`() {
        val customStandard = Standard(
            id = "CUSTOM-001",
            name = "Custom Standard",
            category = StandardCategory.OTHER.name,
            description = "Custom test standard",
            requirements = listOf("Requirement 1", "Requirement 2")
        )
        
        module.registerStandard(customStandard)
        val standards = module.getAllStandards()
        assertTrue(standards.any { it.id == "CUSTOM-001" })
    }
    
    @Test
    fun `test compliance check fully compliant`() {
        val result = module.checkCompliance("ISO-9001") { requirement ->
            true // All requirements met
        }
        
        assertEquals("ISO-9001", result.standardId)
        assertEquals(ComplianceLevel.FULLY_COMPLIANT.name, result.level)
        assertTrue(result.unmetRequirements.isEmpty())
    }
    
    @Test
    fun `test compliance check non compliant`() {
        val result = module.checkCompliance("ISO-9001") { requirement ->
            false // No requirements met
        }
        
        assertEquals(ComplianceLevel.NON_COMPLIANT.name, result.level)
        assertTrue(result.metRequirements.isEmpty())
        assertFalse(result.unmetRequirements.isEmpty())
    }
    
    @Test
    fun `test compliance check partially compliant`() {
        var count = 0
        val result = module.checkCompliance("ISO-9001") { requirement ->
            count++
            count % 2 == 0 // Meet every other requirement (~50%)
        }
        
        assertEquals(ComplianceLevel.PARTIALLY_COMPLIANT.name, result.level)
        assertFalse(result.metRequirements.isEmpty())
        assertFalse(result.unmetRequirements.isEmpty())
    }
    
    @Test
    fun `test compliance check substantially compliant`() {
        var count = 0
        val result = module.checkCompliance("ISO-9001") { requirement ->
            count++
            count <= 4 // Meet first 4 out of 5 requirements (80%)
        }
        
        assertEquals(ComplianceLevel.SUBSTANTIALLY_COMPLIANT.name, result.level)
    }
    
    @Test
    fun `test mark standard as compliant`() {
        module.markCompliant("ISO-9001", "Manual verification completed")
        
        val status = module.getComplianceStatus("ISO-9001")
        assertNotNull(status)
        assertEquals(ComplianceLevel.FULLY_COMPLIANT.name, status?.level)
        assertEquals("Manual verification completed", status?.notes)
    }
    
    @Test
    fun `test compliance report generation`() {
        // Mark some standards as compliant
        module.markCompliant("ISO-9001")
        module.markCompliant("ISO-27001")
        module.checkCompliance("IEEE-830") { false } // Non-compliant
        
        val report = module.generateComplianceReport()
        
        assertTrue(report.totalStandards > 0)
        assertTrue(report.compliantCount >= 2)
        assertTrue(report.complianceRate >= 0.0 && report.complianceRate <= 1.0)
        assertFalse(report.results.isEmpty())
    }
    
    @Test
    fun `test get compliance status for unknown standard`() {
        val status = module.getComplianceStatus("UNKNOWN-STANDARD")
        assertNull(status)
    }
    
    @Test
    fun `test get compliance status for known standard`() {
        module.markCompliant("ISO-9001")
        val status = module.getComplianceStatus("ISO-9001")
        assertNotNull(status)
    }
    
    @Test
    fun `test compliance statistics by category`() {
        module.markCompliant("ISO-9001")
        module.markCompliant("ISO-27001")
        module.checkCompliance("IEEE-830") { true }
        
        val stats = module.getComplianceStatsByCategory()
        
        assertFalse(stats.isEmpty())
        assertTrue(stats.containsKey(StandardCategory.ISO.name) || 
                   stats.containsKey(StandardCategory.IEEE.name))
    }
    
    @Test
    fun `test meets minimum standards when no compliance checks`() {
        // Fresh module with no compliance checks
        val meets = module.meetsMinimumStandards(ComplianceLevel.PARTIALLY_COMPLIANT)
        // Should be false because no standards are marked as compliant
        assertFalse(meets)
    }
    
    @Test
    fun `test meets minimum standards with sufficient compliance`() {
        val allStandards = module.getAllStandards()
        
        // Mark majority as compliant
        allStandards.take((allStandards.size * 0.9).toInt()).forEach {
            module.markCompliant(it.id)
        }
        
        val meets = module.meetsMinimumStandards(ComplianceLevel.SUBSTANTIALLY_COMPLIANT)
        assertTrue(meets)
    }
    
    @Test
    fun `test standards count`() {
        val count = module.getStandardsCount()
        assertTrue(count >= 15) // Should have at least the common standards
    }
    
    @Test
    fun `test GDPR standard exists`() {
        val gdpr = module.getAllStandards().find { it.id == "GDPR" }
        assertNotNull(gdpr)
        assertEquals(StandardCategory.GDPR.name, gdpr?.category)
    }
    
    @Test
    fun `test LGPD standard exists`() {
        val lgpd = module.getAllStandards().find { it.id == "LGPD" }
        assertNotNull(lgpd)
        assertEquals(StandardCategory.LGPD.name, lgpd?.category)
    }
    
    @Test
    fun `test standard has requirements`() {
        val iso9001 = module.getAllStandards().find { it.id == "ISO-9001" }
        assertNotNull(iso9001)
        assertFalse(iso9001?.requirements.isNullOrEmpty())
    }
    
    @Test
    fun `test multiple categories represented`() {
        val categories = module.getAllStandards()
            .map { it.category }
            .toSet()
        
        assertTrue(categories.size >= 4) // Should have at least ISO, IEEE, NIST, and one more
    }
}
