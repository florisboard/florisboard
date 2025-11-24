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
 * Test suite for LicensingModule
 * 
 * Verifies all validation factors and licensing logic
 */
class LicensingModuleTest {
    
    @Test
    fun testLicensingModuleCreation() {
        val module = LicensingModule()
        
        assertEquals("RAFCODE-Φ", module.identity)
        assertEquals("BITRAF64", module.encoding)
        assertEquals("ΣΩΔΦBITRAF", module.seal)
        assertEquals(999, module.version)
        assertEquals("Rafael Melo Reis", module.author)
    }
    
    @Test
    fun testValidateExecution_AllFactorsPass() {
        val module = LicensingModule()
        
        val result = module.validateExecution(
            integrity = true,
            authorship = true,
            permission = true,
            destination = true,
            ethicalPurpose = true
        )
        
        assertTrue(result, "Execution should be permitted when all factors pass")
    }
    
    @Test
    fun testValidateExecution_IntegrityFails() {
        val module = LicensingModule()
        
        val result = module.validateExecution(
            integrity = false,
            authorship = true,
            permission = true,
            destination = true,
            ethicalPurpose = true
        )
        
        assertFalse(result, "Execution should be denied when integrity fails")
    }
    
    @Test
    fun testValidateExecution_AuthorshipFails() {
        val module = LicensingModule()
        
        val result = module.validateExecution(
            integrity = true,
            authorship = false,
            permission = true,
            destination = true,
            ethicalPurpose = true
        )
        
        assertFalse(result, "Execution should be denied when authorship fails")
    }
    
    @Test
    fun testValidateExecution_EthicalPurposeFails() {
        val module = LicensingModule()
        
        val result = module.validateExecution(
            integrity = true,
            authorship = true,
            permission = true,
            destination = true,
            ethicalPurpose = false
        )
        
        assertFalse(result, "Execution should be denied when ethical purpose fails (Ethica[8])")
    }
    
    @Test
    fun testComputeHash() {
        val module = LicensingModule()
        val data = "test data".toByteArray()
        
        val hash1 = module.computeHash(data)
        val hash2 = module.computeHash(data)
        
        assertEquals(hash1, hash2, "Same data should produce same hash")
        assertTrue(hash1.isNotEmpty(), "Hash should not be empty")
    }
    
    @Test
    fun testComputeHash_DifferentData() {
        val module = LicensingModule()
        val data1 = "test data 1".toByteArray()
        val data2 = "test data 2".toByteArray()
        
        val hash1 = module.computeHash(data1)
        val hash2 = module.computeHash(data2)
        
        assertTrue(hash1 != hash2, "Different data should produce different hashes")
    }
    
    @Test
    fun testValidateAuthorship_ValidAuthor() {
        val module = LicensingModule()
        
        assertTrue(module.validateAuthorship("Rafael Melo Reis"))
        assertTrue(module.validateAuthorship("Contains RAFCODE-Φ identifier"))
        assertTrue(module.validateAuthorship("Has ΣΩΔΦBITRAF seal"))
    }
    
    @Test
    fun testValidateAuthorship_InvalidAuthor() {
        val module = LicensingModule()
        
        assertFalse(module.validateAuthorship("Unknown Author"))
    }
    
    @Test
    fun testZiprafOmegaFunction_ValidContext() {
        val module = LicensingModule()
        
        val context = ExecutionContext(
            data = "test operation",
            authorId = "Rafael Melo Reis",
            hasPermission = true,
            destinationValid = true,
            ethicalPurposeValid = true
        )
        
        val result = module.ziprafOmegaFunction(context)
        
        assertTrue(result.valid, "Valid context should pass validation")
        assertTrue(result.authorshipPassed, "Authorship should pass")
        assertTrue(result.permissionPassed, "Permission should pass")
        assertTrue(result.destinationPassed, "Destination should pass")
        assertTrue(result.ethicalPassed, "Ethical purpose should pass")
        assertEquals("EXECUTION PERMITTED", result.message)
    }
    
    @Test
    fun testZiprafOmegaFunction_InvalidAuthor() {
        val module = LicensingModule()
        
        val context = ExecutionContext(
            data = "test operation",
            authorId = "Invalid Author",
            hasPermission = true,
            destinationValid = true,
            ethicalPurposeValid = true
        )
        
        val result = module.ziprafOmegaFunction(context)
        
        assertFalse(result.valid, "Invalid author should fail validation")
        assertFalse(result.authorshipPassed, "Authorship should fail")
        assertEquals("EXECUTION DENIED", result.message)
    }
    
    @Test
    fun testZiprafOmegaFunction_WithHashValidation() {
        val module = LicensingModule()
        val data = "test data"
        val correctHash = module.computeHash(data.toByteArray())
        
        val context = ExecutionContext(
            data = data,
            dataHash = correctHash,
            authorId = "Rafael Melo Reis",
            hasPermission = true,
            destinationValid = true,
            ethicalPurposeValid = true
        )
        
        val result = module.ziprafOmegaFunction(context)
        
        assertTrue(result.valid, "Correct hash should pass integrity check")
        assertTrue(result.integrityPassed, "Integrity should pass with correct hash")
    }
    
    @Test
    fun testZiprafOmegaFunction_WithIncorrectHash() {
        val module = LicensingModule()
        
        val context = ExecutionContext(
            data = "test data",
            dataHash = "incorrect_hash",
            authorId = "Rafael Melo Reis",
            hasPermission = true,
            destinationValid = true,
            ethicalPurposeValid = true
        )
        
        val result = module.ziprafOmegaFunction(context)
        
        assertFalse(result.valid, "Incorrect hash should fail integrity check")
        assertFalse(result.integrityPassed, "Integrity should fail with incorrect hash")
        assertEquals("EXECUTION DENIED", result.message)
    }
    
    @Test
    fun testSealsConstant() {
        assertEquals(10, LicensingModule.SEALS.size, "Should have 10 seals")
        assertTrue(LicensingModule.SEALS.contains("Σ"))
        assertTrue(LicensingModule.SEALS.contains("Ω"))
        assertTrue(LicensingModule.SEALS.contains("Φ"))
    }
    
    @Test
    fun testCorrelationConstant() {
        assertEquals(0.963999, LicensingModule.R_CORR, 0.000001)
    }
    
    @Test
    fun testFrequencies() {
        assertEquals(4, LicensingModule.FREQUENCIES.size)
        assertTrue(LicensingModule.FREQUENCIES.contains(100))
        assertTrue(LicensingModule.FREQUENCIES.contains(144_000))
        assertTrue(LicensingModule.FREQUENCIES.contains(288_000))
        assertTrue(LicensingModule.FREQUENCIES.contains(1008))
    }
}
