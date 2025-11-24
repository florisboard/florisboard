/*
 * ZIPRAF_OMEGA Error Handling Module Tests
 * Copyright (C) 2025 Rafael Melo Reis
 */

package org.florisboard.lib.zipraf

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ErrorHandlingModuleTest {
    
    private lateinit var module: ErrorHandlingModule
    
    @BeforeEach
    fun setup() {
        module = ErrorHandlingModule.getInstance()
        module.clearErrorLog()
    }
    
    @Test
    fun `test error classification for validation error`() {
        val exception = IllegalArgumentException("Invalid argument")
        val category = module.classifyError(exception)
        assertEquals(ErrorCategory.VALIDATION, category)
    }
    
    @Test
    fun `test error classification for security error`() {
        val exception = SecurityException("Access denied")
        val category = module.classifyError(exception)
        assertEquals(ErrorCategory.SECURITY, category)
    }
    
    @Test
    fun `test error severity determination`() {
        val exception = SecurityException("Security violation")
        val category = ErrorCategory.SECURITY
        val severity = module.determineSeverity(exception, category)
        assertEquals(ErrorSeverity.CRITICAL, severity)
    }
    
    @Test
    fun `test recovery strategy determination`() {
        val strategy1 = module.determineRecoveryStrategy(ErrorCategory.NETWORK, ErrorSeverity.WARNING)
        assertEquals(RecoveryStrategy.RETRY, strategy1)
        
        val strategy2 = module.determineRecoveryStrategy(ErrorCategory.SECURITY, ErrorSeverity.CRITICAL)
        assertEquals(RecoveryStrategy.FAIL_FAST, strategy2)
    }
    
    @Test
    fun `test error logging`() {
        val exception = IllegalArgumentException("Test error")
        val errorInfo = module.logError(exception, mapOf("context" to "test"))
        
        assertNotNull(errorInfo)
        assertEquals(ErrorCategory.VALIDATION.name, errorInfo.category)
        assertTrue(errorInfo.message.contains("Test error"))
        assertEquals("test", errorInfo.context["context"])
    }
    
    @Test
    fun `test retry with success on first attempt`() = runTest {
        var attempts = 0
        val result = module.withRetry {
            attempts++
            "success"
        }
        
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(1, attempts)
    }
    
    @Test
    fun `test retry with success after failures`() = runTest {
        var attempts = 0
        val result = module.withRetry(RetryConfig(maxAttempts = 3)) {
            attempts++
            if (attempts < 3) throw Exception("Temporary failure")
            "success"
        }
        
        assertTrue(result.isSuccess)
        assertEquals("success", result.getOrNull())
        assertEquals(3, attempts)
    }
    
    @Test
    fun `test retry exhausts attempts`() = runTest {
        var attempts = 0
        val result = module.withRetry(RetryConfig(maxAttempts = 3)) {
            attempts++
            throw Exception("Persistent failure")
        }
        
        assertTrue(result.isFailure)
        assertEquals(3, attempts)
    }
    
    @Test
    fun `test circuit breaker starts closed`() {
        val breaker = module.getCircuitBreaker("test_circuit")
        assertEquals(CircuitState.CLOSED, breaker.getState())
        assertFalse(breaker.isOpen())
    }
    
    @Test
    fun `test circuit breaker opens after failures`() = runTest {
        val breaker = module.getCircuitBreaker("test_circuit", failureThreshold = 2)
        
        // First failure
        breaker.execute { throw Exception("Failure 1") }
        assertEquals(CircuitState.CLOSED, breaker.getState())
        
        // Second failure should open circuit
        breaker.execute { throw Exception("Failure 2") }
        assertTrue(breaker.isOpen())
    }
    
    @Test
    fun `test circuit breaker with module helper`() = runTest {
        var attempts = 0
        
        // Generate failures to open circuit
        repeat(6) {
            module.withCircuitBreaker("test") {
                attempts++
                throw Exception("Failure")
            }
        }
        
        // Circuit should be open now
        val result = module.withCircuitBreaker("test") { "success" }
        assertTrue(result.isFailure)
        result.exceptionOrNull()?.let {
            assertTrue(it is CircuitBreakerOpenException)
        }
    }
    
    @Test
    fun `test fallback execution on success`() = runTest {
        val result = module.withFallback(
            primary = { "primary success" },
            fallback = { "fallback" }
        )
        
        assertEquals("primary success", result)
    }
    
    @Test
    fun `test fallback execution on failure`() = runTest {
        val result = module.withFallback(
            primary = { throw Exception("Primary failed") },
            fallback = { "fallback success" }
        )
        
        assertEquals("fallback success", result)
    }
    
    @Test
    fun `test timeout success`() = runTest {
        val result = module.withTimeout(100L) {
            delay(10)
            "completed"
        }
        
        assertTrue(result.isSuccess)
        assertEquals("completed", result.getOrNull())
    }
    
    @Test
    fun `test timeout failure`() = runTest {
        val result = module.withTimeout(10L) {
            delay(100)
            "completed"
        }
        
        assertTrue(result.isFailure)
    }
    
    @Test
    fun `test error statistics tracking`() {
        module.logError(IllegalArgumentException("Error 1"))
        module.logError(SecurityException("Error 2"))
        module.logError(IllegalArgumentException("Error 3"))
        
        val stats = module.getErrorStatistics()
        assertTrue(stats[ErrorCategory.VALIDATION.name]!! >= 2)
        assertTrue(stats[ErrorCategory.SECURITY.name]!! >= 1)
    }
    
    @Test
    fun `test get recent errors`() {
        module.logError(Exception("Error 1"))
        module.logError(Exception("Error 2"))
        module.logError(Exception("Error 3"))
        
        val recent = module.getRecentErrors(2)
        assertEquals(2, recent.size)
    }
    
    @Test
    fun `test get errors by category`() {
        module.logError(IllegalArgumentException("Validation 1"))
        module.logError(SecurityException("Security 1"))
        module.logError(IllegalArgumentException("Validation 2"))
        
        val validationErrors = module.getErrorsByCategory(ErrorCategory.VALIDATION)
        assertTrue(validationErrors.size >= 2)
    }
    
    @Test
    fun `test get errors by severity`() {
        module.logError(SecurityException("Critical"))
        module.logError(IllegalArgumentException("Warning"))
        
        val criticalErrors = module.getErrorsBySeverity(ErrorSeverity.CRITICAL)
        assertTrue(criticalErrors.size >= 1)
    }
    
    @Test
    fun `test total error count`() {
        module.clearErrorLog()
        assertEquals(0, module.getTotalErrorCount())
        
        module.logError(Exception("Error 1"))
        module.logError(Exception("Error 2"))
        assertEquals(2, module.getTotalErrorCount())
    }
    
    @Test
    fun `test circuit breaker reset`() {
        val breaker = module.getCircuitBreaker("reset_test", failureThreshold = 1)
        
        // Open the circuit
        runTest {
            breaker.execute { throw Exception("Failure") }
        }
        
        assertTrue(breaker.isOpen())
        
        // Reset
        module.resetCircuitBreaker("reset_test")
        assertEquals(CircuitState.CLOSED, breaker.getState())
    }
}
