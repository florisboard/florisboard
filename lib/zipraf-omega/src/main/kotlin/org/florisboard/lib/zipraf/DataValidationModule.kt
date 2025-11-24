/*
 * ZIPRAF_OMEGA Data Validation Module v999
 * Copyright (C) 2025 Rafael Melo Reis
 * 
 * This module implements comprehensive data validation through:
 * - Desk checking (manual verification simulation)
 * - Logic validation and consistency checks
 * - Data mapping and transformation verification
 * - Boundary condition testing
 * - Invariant checking
 * 
 * License: Apache 2.0
 * Authorship credentials: Rafael Melo Reis
 * 
 * Standards Compliance:
 * - ISO 8000 (Data Quality)
 * - ISO 25010 (Software Product Quality)
 * - IEEE 830 (Software Requirements Specification)
 * - IEEE 1012 (Software Verification and Validation)
 */

package org.florisboard.lib.zipraf

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Validation result types
 */
enum class ValidationResult {
    VALID,
    INVALID,
    WARNING,
    ERROR
}

/**
 * Data validation error
 */
@Serializable
data class ValidationError(
    val field: String,
    val value: String,
    val rule: String,
    val message: String,
    val severity: String
)

/**
 * Desk check result
 */
@Serializable
data class DeskCheckResult(
    val testName: String,
    val passed: Boolean,
    val input: String,
    val expectedOutput: String,
    val actualOutput: String,
    val errors: List<ValidationError> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Logic validation result
 */
@Serializable
data class LogicValidationResult(
    val validationName: String,
    val result: String,
    val message: String,
    val conditions: Map<String, Boolean>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Data mapping rule
 */
data class MappingRule<I, O>(
    val name: String,
    val transform: (I) -> O,
    val validate: (I, O) -> Boolean
)

/**
 * Invariant condition
 */
data class Invariant<T>(
    val name: String,
    val condition: (T) -> Boolean,
    val errorMessage: String
)

/**
 * Comprehensive Data Validation Module
 * 
 * Provides systematic validation through:
 * - Desk checking: Step-by-step verification of logic
 * - Type safety: Runtime type checking
 * - Boundary validation: Min/max/range checking
 * - Logic consistency: Invariant checking
 * - Data mapping: Transformation verification
 */
class DataValidationModule {
    
    private val validationRules = mutableMapOf<String, MutableList<ValidationRule<*>>>()
    private val invariants = mutableMapOf<String, MutableList<Invariant<*>>>()
    private val deskCheckResults = mutableListOf<DeskCheckResult>()
    
    /**
     * Generic validation rule
     */
    data class ValidationRule<T>(
        val name: String,
        val predicate: (T) -> Boolean,
        val errorMessage: String,
        val severity: ValidationResult = ValidationResult.ERROR
    )
    
    /**
     * Register a validation rule for a data type
     * 
     * @param typeName Name of the data type
     * @param rule Validation rule to register
     */
    fun <T> registerValidationRule(typeName: String, rule: ValidationRule<T>) {
        validationRules.getOrPut(typeName) { mutableListOf() }.add(rule)
    }
    
    /**
     * Register an invariant for a data type
     * 
     * @param typeName Name of the data type
     * @param invariant Invariant condition to register
     */
    fun <T> registerInvariant(typeName: String, invariant: Invariant<T>) {
        invariants.getOrPut(typeName) { mutableListOf() }.add(invariant)
    }
    
    /**
     * Validate data against all registered rules
     * 
     * @param typeName Type of data being validated
     * @param data Data to validate
     * @return List of validation errors (empty if valid)
     */
    fun <T> validate(typeName: String, data: T): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        validationRules[typeName]?.forEach { rule ->
            @Suppress("UNCHECKED_CAST")
            val typedRule = rule as ValidationRule<T>
            if (!typedRule.predicate(data)) {
                errors.add(
                    ValidationError(
                        field = typeName,
                        value = data.toString(),
                        rule = typedRule.name,
                        message = typedRule.errorMessage,
                        severity = typedRule.severity.name
                    )
                )
            }
        }
        
        return errors
    }
    
    /**
     * Check invariants for data
     * 
     * @param typeName Type of data
     * @param data Data to check
     * @return List of validation errors for violated invariants
     */
    fun <T> checkInvariants(typeName: String, data: T): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()
        
        invariants[typeName]?.forEach { invariant ->
            @Suppress("UNCHECKED_CAST")
            val typedInvariant = invariant as Invariant<T>
            if (!typedInvariant.condition(data)) {
                errors.add(
                    ValidationError(
                        field = typeName,
                        value = data.toString(),
                        rule = "invariant:${typedInvariant.name}",
                        message = typedInvariant.errorMessage,
                        severity = ValidationResult.ERROR.name
                    )
                )
            }
        }
        
        return errors
    }
    
    /**
     * Perform desk check: manually trace through logic
     * 
     * @param testName Name of the test
     * @param input Input value as string
     * @param expectedOutput Expected result as string
     * @param operation Operation to test
     * @return DeskCheckResult with test outcome
     */
    fun <I, O> performDeskCheck(
        testName: String,
        input: I,
        expectedOutput: O,
        operation: (I) -> O
    ): DeskCheckResult {
        val actualOutput = try {
            operation(input)
        } catch (e: Exception) {
            val result = DeskCheckResult(
                testName = testName,
                passed = false,
                input = input.toString(),
                expectedOutput = expectedOutput.toString(),
                actualOutput = "ERROR: ${e.message}",
                errors = listOf(
                    ValidationError(
                        field = "operation",
                        value = input.toString(),
                        rule = "execution",
                        message = "Operation failed: ${e.message}",
                        severity = ValidationResult.ERROR.name
                    )
                )
            )
            deskCheckResults.add(result)
            return result
        }
        
        val passed = actualOutput == expectedOutput
        val result = DeskCheckResult(
            testName = testName,
            passed = passed,
            input = input.toString(),
            expectedOutput = expectedOutput.toString(),
            actualOutput = actualOutput.toString(),
            errors = if (passed) emptyList() else listOf(
                ValidationError(
                    field = "output",
                    value = actualOutput.toString(),
                    rule = "equality",
                    message = "Expected $expectedOutput but got $actualOutput",
                    severity = ValidationResult.ERROR.name
                )
            )
        )
        
        deskCheckResults.add(result)
        return result
    }
    
    /**
     * Validate data mapping transformation
     * 
     * @param rule Mapping rule to validate
     * @param input Input data
     * @return True if mapping is valid
     */
    fun <I, O> validateMapping(rule: MappingRule<I, O>, input: I): Boolean {
        return try {
            val output = rule.transform(input)
            rule.validate(input, output)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Validate numeric range
     * 
     * @param value Value to check
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @param inclusive Whether bounds are inclusive
     * @return Validation error if out of range, null otherwise
     */
    fun <T : Comparable<T>> validateRange(
        value: T,
        min: T,
        max: T,
        fieldName: String = "value",
        inclusive: Boolean = true
    ): ValidationError? {
        val inRange = if (inclusive) {
            value >= min && value <= max
        } else {
            value > min && value < max
        }
        
        return if (!inRange) {
            ValidationError(
                field = fieldName,
                value = value.toString(),
                rule = "range",
                message = "Value $value is outside range [$min, $max]",
                severity = ValidationResult.ERROR.name
            )
        } else null
    }
    
    /**
     * Validate string format
     * 
     * @param value String to validate
     * @param pattern Regex pattern to match
     * @param fieldName Field name for error reporting
     * @return Validation error if format invalid, null otherwise
     */
    fun validateFormat(
        value: String,
        pattern: Regex,
        fieldName: String = "string"
    ): ValidationError? {
        return if (!pattern.matches(value)) {
            ValidationError(
                field = fieldName,
                value = value,
                rule = "format",
                message = "Value does not match required pattern: ${pattern.pattern}",
                severity = ValidationResult.ERROR.name
            )
        } else null
    }
    
    /**
     * Validate string length
     * 
     * @param value String to validate
     * @param minLength Minimum length (inclusive)
     * @param maxLength Maximum length (inclusive)
     * @param fieldName Field name for error reporting
     * @return Validation error if length invalid, null otherwise
     */
    fun validateLength(
        value: String,
        minLength: Int,
        maxLength: Int,
        fieldName: String = "string"
    ): ValidationError? {
        return if (value.length < minLength || value.length > maxLength) {
            ValidationError(
                field = fieldName,
                value = value,
                rule = "length",
                message = "Length ${value.length} is outside range [$minLength, $maxLength]",
                severity = ValidationResult.ERROR.name
            )
        } else null
    }
    
    /**
     * Validate collection size
     * 
     * @param collection Collection to validate
     * @param minSize Minimum size (inclusive)
     * @param maxSize Maximum size (inclusive)
     * @param fieldName Field name for error reporting
     * @return Validation error if size invalid, null otherwise
     */
    fun <T> validateCollectionSize(
        collection: Collection<T>,
        minSize: Int,
        maxSize: Int,
        fieldName: String = "collection"
    ): ValidationError? {
        return if (collection.size < minSize || collection.size > maxSize) {
            ValidationError(
                field = fieldName,
                value = "size=${collection.size}",
                rule = "collection_size",
                message = "Collection size ${collection.size} is outside range [$minSize, $maxSize]",
                severity = ValidationResult.ERROR.name
            )
        } else null
    }
    
    /**
     * Validate non-null
     * 
     * @param value Value to check
     * @param fieldName Field name for error reporting
     * @return Validation error if null, null otherwise
     */
    fun validateNonNull(
        value: Any?,
        fieldName: String = "value"
    ): ValidationError? {
        return if (value == null) {
            ValidationError(
                field = fieldName,
                value = "null",
                rule = "non_null",
                message = "Value must not be null",
                severity = ValidationResult.ERROR.name
            )
        } else null
    }
    
    /**
     * Validate logic condition
     * 
     * @param name Name of the validation
     * @param conditions Map of condition name to result
     * @return LogicValidationResult
     */
    fun validateLogic(
        name: String,
        conditions: Map<String, Boolean>
    ): LogicValidationResult {
        val allPassed = conditions.values.all { it }
        val failedConditions = conditions.filter { !it.value }.keys
        
        return LogicValidationResult(
            validationName = name,
            result = if (allPassed) ValidationResult.VALID.name else ValidationResult.INVALID.name,
            message = if (allPassed) {
                "All conditions passed"
            } else {
                "Failed conditions: ${failedConditions.joinToString(", ")}"
            },
            conditions = conditions
        )
    }
    
    /**
     * Batch validate multiple values
     * 
     * @param validations Map of field name to validation function
     * @return List of all validation errors
     */
    fun batchValidate(
        validations: Map<String, () -> ValidationError?>
    ): List<ValidationError> {
        return validations.mapNotNull { (_, validator) ->
            validator()
        }
    }
    
    /**
     * Get all desk check results
     * 
     * @return List of all performed desk checks
     */
    fun getDeskCheckResults(): List<DeskCheckResult> {
        return deskCheckResults.toList()
    }
    
    /**
     * Get desk check statistics
     * 
     * @return Map of statistics
     */
    fun getDeskCheckStatistics(): Map<String, Int> {
        val total = deskCheckResults.size
        val passed = deskCheckResults.count { it.passed }
        val failed = total - passed
        
        return mapOf(
            "total" to total,
            "passed" to passed,
            "failed" to failed,
            "pass_rate_percent" to if (total > 0) (passed * 100 / total) else 0
        )
    }
    
    /**
     * Clear all desk check results (for testing)
     */
    fun clearDeskCheckResults() {
        deskCheckResults.clear()
    }
    
    /**
     * Create a composite validator
     * 
     * @param validators List of validation functions
     * @return Combined validation function
     */
    fun <T> composite(
        vararg validators: (T) -> ValidationError?
    ): (T) -> List<ValidationError> {
        return { value ->
            validators.mapNotNull { it(value) }
        }
    }
}
