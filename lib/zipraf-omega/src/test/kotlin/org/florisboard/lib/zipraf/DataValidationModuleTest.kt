/*
 * ZIPRAF_OMEGA Data Validation Module Tests
 * Copyright (C) 2025 Rafael Melo Reis
 */

package org.florisboard.lib.zipraf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DataValidationModuleTest {
    
    private lateinit var module: DataValidationModule
    
    @BeforeEach
    fun setup() {
        module = DataValidationModule()
    }
    
    @Test
    fun `test range validation within bounds`() {
        val error = module.validateRange(5, 1, 10, "testField")
        assertNull(error)
    }
    
    @Test
    fun `test range validation outside bounds`() {
        val error = module.validateRange(15, 1, 10, "testField")
        assertNotNull(error)
        assertEquals("testField", error?.field)
        assertEquals(ValidationResult.ERROR.name, error?.severity)
    }
    
    @Test
    fun `test format validation with valid format`() {
        val error = module.validateFormat("test@example.com", Regex("^[\\w.]+@[\\w.]+\\.\\w+$"), "email")
        assertNull(error)
    }
    
    @Test
    fun `test format validation with invalid format`() {
        val error = module.validateFormat("invalid-email", Regex("^[\\w.]+@[\\w.]+\\.\\w+$"), "email")
        assertNotNull(error)
        assertEquals("email", error?.field)
    }
    
    @Test
    fun `test length validation within bounds`() {
        val error = module.validateLength("hello", 1, 10, "greeting")
        assertNull(error)
    }
    
    @Test
    fun `test length validation too short`() {
        val error = module.validateLength("hi", 5, 10, "text")
        assertNotNull(error)
        assertEquals("text", error?.field)
    }
    
    @Test
    fun `test length validation too long`() {
        val error = module.validateLength("this is a very long string", 1, 10, "text")
        assertNotNull(error)
    }
    
    @Test
    fun `test collection size validation`() {
        val validList = listOf(1, 2, 3)
        val error = module.validateCollectionSize(validList, 1, 5, "numbers")
        assertNull(error)
    }
    
    @Test
    fun `test collection size too small`() {
        val emptyList = emptyList<Int>()
        val error = module.validateCollectionSize(emptyList, 1, 5, "numbers")
        assertNotNull(error)
    }
    
    @Test
    fun `test collection size too large`() {
        val largeList = List(100) { it }
        val error = module.validateCollectionSize(largeList, 1, 10, "numbers")
        assertNotNull(error)
    }
    
    @Test
    fun `test non-null validation with valid value`() {
        val error = module.validateNonNull("value", "field")
        assertNull(error)
    }
    
    @Test
    fun `test non-null validation with null value`() {
        val error = module.validateNonNull(null, "field")
        assertNotNull(error)
        assertEquals("field", error?.field)
        assertEquals("non_null", error?.rule)
    }
    
    @Test
    fun `test desk check with passing result`() {
        val result = module.performDeskCheck(
            "addition_test",
            5,
            8
        ) { x -> x + 3 }
        
        assertTrue(result.passed)
        assertEquals("5", result.input)
        assertEquals("8", result.expectedOutput)
        assertEquals("8", result.actualOutput)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `test desk check with failing result`() {
        val result = module.performDeskCheck(
            "subtraction_test",
            10,
            7
        ) { x -> x - 2 } // Correct would be x - 3
        
        assertFalse(result.passed)
        assertEquals("10", result.input)
        assertEquals("7", result.expectedOutput)
        assertEquals("8", result.actualOutput)
        assertFalse(result.errors.isEmpty())
    }
    
    @Test
    fun `test desk check with exception`() {
        val result = module.performDeskCheck<Int, Int>(
            "division_test",
            10,
            5
        ) { x -> x / 0 } // Will throw exception
        
        assertFalse(result.passed)
        assertTrue(result.actualOutput.contains("ERROR"))
        assertFalse(result.errors.isEmpty())
    }
    
    @Test
    fun `test logic validation all conditions pass`() {
        val result = module.validateLogic(
            "test_logic",
            mapOf(
                "condition1" to true,
                "condition2" to true,
                "condition3" to true
            )
        )
        
        assertEquals(ValidationResult.VALID.name, result.result)
        assertTrue(result.message.contains("All conditions passed"))
    }
    
    @Test
    fun `test logic validation some conditions fail`() {
        val result = module.validateLogic(
            "test_logic",
            mapOf(
                "condition1" to true,
                "condition2" to false,
                "condition3" to true
            )
        )
        
        assertEquals(ValidationResult.INVALID.name, result.result)
        assertTrue(result.message.contains("condition2"))
    }
    
    @Test
    fun `test batch validation with no errors`() {
        val errors = module.batchValidate(mapOf(
            "field1" to { module.validateRange(5, 1, 10, "field1") },
            "field2" to { module.validateNonNull("value", "field2") },
            "field3" to { module.validateLength("test", 1, 10, "field3") }
        ))
        
        assertTrue(errors.isEmpty())
    }
    
    @Test
    fun `test batch validation with errors`() {
        val errors = module.batchValidate(mapOf(
            "field1" to { module.validateRange(15, 1, 10, "field1") },
            "field2" to { module.validateNonNull(null, "field2") },
            "field3" to { module.validateLength("test", 10, 20, "field3") }
        ))
        
        assertEquals(3, errors.size)
    }
    
    @Test
    fun `test desk check results tracking`() {
        module.performDeskCheck("test1", 1, 2) { it + 1 }
        module.performDeskCheck("test2", 2, 4) { it * 2 }
        
        val results = module.getDeskCheckResults()
        assertEquals(2, results.size)
    }
    
    @Test
    fun `test desk check statistics`() {
        module.performDeskCheck("pass1", 1, 2) { it + 1 }
        module.performDeskCheck("pass2", 2, 4) { it * 2 }
        module.performDeskCheck("fail1", 5, 10) { it * 3 } // Wrong: 15 != 10
        
        val stats = module.getDeskCheckStatistics()
        assertEquals(3, stats["total"])
        assertEquals(2, stats["passed"])
        assertEquals(1, stats["failed"])
        assertEquals(66, stats["pass_rate_percent"]) // 2/3 * 100
    }
    
    @Test
    fun `test validation rule registration and usage`() {
        val rule = DataValidationModule.ValidationRule<Int>(
            name = "positive",
            predicate = { it > 0 },
            errorMessage = "Value must be positive"
        )
        
        module.registerValidationRule("Integer", rule)
        
        val errors1 = module.validate("Integer", 5)
        assertTrue(errors1.isEmpty())
        
        val errors2 = module.validate("Integer", -5)
        assertEquals(1, errors2.size)
        assertEquals("positive", errors2[0].rule)
    }
    
    @Test
    fun `test invariant checking`() {
        val invariant = Invariant<String>(
            name = "not_empty",
            condition = { it.isNotEmpty() },
            errorMessage = "String must not be empty"
        )
        
        module.registerInvariant("String", invariant)
        
        val errors1 = module.checkInvariants("String", "hello")
        assertTrue(errors1.isEmpty())
        
        val errors2 = module.checkInvariants("String", "")
        assertEquals(1, errors2.size)
    }
    
    @Test
    fun `test composite validator`() {
        val validator = module.composite<String>(
            { module.validateNonNull(it, "text") },
            { module.validateLength(it, 5, 10, "text") },
            { module.validateFormat(it, Regex("^[a-z]+$"), "text") }
        )
        
        val errors1 = validator("hello")
        assertTrue(errors1.isEmpty())
        
        val errors2 = validator("Hi")
        assertTrue(errors2.isNotEmpty()) // Too short and has uppercase
    }
    
    @Test
    fun `test clear desk check results`() {
        module.performDeskCheck("test1", 1, 2) { it + 1 }
        assertEquals(1, module.getDeskCheckResults().size)
        
        module.clearDeskCheckResults()
        assertEquals(0, module.getDeskCheckResults().size)
    }
}
