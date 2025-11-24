/*
 * ZIPRAF_OMEGA Error Handling Module v999
 * Copyright (C) 2025 Rafael Melo Reis
 * 
 * This module implements comprehensive error handling:
 * - Centralized error classification
 * - Error recovery strategies
 * - Error logging and tracking
 * - Circuit breaker pattern for fault tolerance
 * - Retry mechanisms with exponential backoff
 * 
 * License: Apache 2.0
 * Authorship credentials: Rafael Melo Reis
 * 
 * Standards Compliance:
 * - ISO 27001 (Information Security Management)
 * - IEEE 1012 (Software Verification and Validation)
 * - IEEE 14764 (Software Maintenance)
 * - NIST 800-53 (Security and Privacy Controls)
 */

package org.florisboard.lib.zipraf

import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.min
import kotlin.math.pow

/**
 * Error categories for classification
 */
enum class ErrorCategory {
    VALIDATION,
    PERMISSION,
    RESOURCE,
    NETWORK,
    TIMEOUT,
    CONCURRENCY,
    DATA_CORRUPTION,
    SECURITY,
    LOGIC,
    UNKNOWN
}

/**
 * Error severity levels
 */
enum class ErrorSeverity {
    FATAL,      // System cannot continue
    CRITICAL,   // Major functionality broken
    ERROR,      // Feature not working
    WARNING,    // Potential issue
    INFO        // Informational
}

/**
 * Recovery strategy for errors
 */
enum class RecoveryStrategy {
    RETRY,              // Retry the operation
    FALLBACK,           // Use alternative approach
    FAIL_FAST,          // Fail immediately
    CIRCUIT_BREAKER,    // Stop trying after threshold
    COMPENSATE,         // Undo and try alternative
    IGNORE              // Log and continue
}

/**
 * Error information
 */
@Serializable
data class ErrorInfo(
    val category: String,
    val severity: String,
    val message: String,
    val stackTrace: String,
    val timestamp: Long = System.currentTimeMillis(),
    val context: Map<String, String> = emptyMap(),
    val recoveryStrategy: String = RecoveryStrategy.FAIL_FAST.name
)

/**
 * Circuit breaker state
 */
enum class CircuitState {
    CLOSED,     // Normal operation
    OPEN,       // Failing, reject requests
    HALF_OPEN   // Testing if recovered
}

/**
 * Circuit breaker for fault tolerance
 */
class CircuitBreaker(
    private val name: String,
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 60_000L,
    private val halfOpenMaxAttempts: Int = 3
) {
    private var state = CircuitState.CLOSED
    private val failureCount = AtomicInteger(0)
    private val successCount = AtomicInteger(0)
    private var lastFailureTime = 0L
    
    fun getState(): CircuitState = state
    
    fun isOpen(): Boolean = state == CircuitState.OPEN
    
    suspend fun <T> execute(operation: suspend () -> T): Result<T> {
        when (state) {
            CircuitState.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeoutMs) {
                    state = CircuitState.HALF_OPEN
                    successCount.set(0)
                } else {
                    return Result.failure(CircuitBreakerOpenException("Circuit breaker $name is OPEN"))
                }
            }
            CircuitState.HALF_OPEN -> {
                // Allow limited attempts in half-open state
            }
            CircuitState.CLOSED -> {
                // Normal operation
            }
        }
        
        return try {
            val result = operation()
            onSuccess()
            Result.success(result)
        } catch (e: Exception) {
            onFailure()
            Result.failure(e)
        }
    }
    
    private fun onSuccess() {
        when (state) {
            CircuitState.HALF_OPEN -> {
                if (successCount.incrementAndGet() >= halfOpenMaxAttempts) {
                    state = CircuitState.CLOSED
                    failureCount.set(0)
                }
            }
            CircuitState.CLOSED -> {
                failureCount.set(0)
            }
            else -> {}
        }
    }
    
    private fun onFailure() {
        lastFailureTime = System.currentTimeMillis()
        val failures = failureCount.incrementAndGet()
        
        when (state) {
            CircuitState.CLOSED -> {
                if (failures >= failureThreshold) {
                    state = CircuitState.OPEN
                }
            }
            CircuitState.HALF_OPEN -> {
                state = CircuitState.OPEN
                failureCount.set(0)
            }
            else -> {}
        }
    }
    
    fun reset() {
        state = CircuitState.CLOSED
        failureCount.set(0)
        successCount.set(0)
    }
}

/**
 * Circuit breaker exception
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

/**
 * Retry configuration
 */
data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 100L,
    val maxDelayMs: Long = 10_000L,
    val multiplier: Double = 2.0,
    val retryableExceptions: Set<Class<out Exception>> = emptySet()
)

/**
 * Comprehensive Error Handling Module
 * 
 * Provides:
 * - Error classification and tracking
 * - Circuit breaker pattern
 * - Retry with exponential backoff
 * - Error recovery strategies
 * - Error logging and metrics
 */
class ErrorHandlingModule {
    
    private val errorLog = mutableListOf<ErrorInfo>()
    private val errorCounts = ConcurrentHashMap<String, AtomicLong>()
    private val circuitBreakers = ConcurrentHashMap<String, CircuitBreaker>()
    
    companion object {
        private var instance: ErrorHandlingModule? = null
        
        fun getInstance(): ErrorHandlingModule {
            return instance ?: synchronized(this) {
                instance ?: ErrorHandlingModule().also { instance = it }
            }
        }
    }
    
    /**
     * Classify an exception into error category
     * 
     * @param exception Exception to classify
     * @return ErrorCategory
     */
    fun classifyError(exception: Exception): ErrorCategory {
        return when {
            exception is IllegalArgumentException -> ErrorCategory.VALIDATION
            exception is SecurityException -> ErrorCategory.SECURITY
            exception is OutOfMemoryError -> ErrorCategory.RESOURCE
            exception is InterruptedException -> ErrorCategory.CONCURRENCY
            exception is java.net.SocketTimeoutException -> ErrorCategory.TIMEOUT
            exception is java.io.IOException -> ErrorCategory.NETWORK
            exception.message?.contains("permission", ignoreCase = true) == true -> ErrorCategory.PERMISSION
            exception.message?.contains("corrupt", ignoreCase = true) == true -> ErrorCategory.DATA_CORRUPTION
            else -> ErrorCategory.UNKNOWN
        }
    }
    
    /**
     * Determine error severity
     * 
     * @param exception Exception to analyze
     * @param category Error category
     * @return ErrorSeverity
     */
    fun determineSeverity(exception: Exception, category: ErrorCategory): ErrorSeverity {
        return when (category) {
            ErrorCategory.SECURITY -> ErrorSeverity.CRITICAL
            ErrorCategory.DATA_CORRUPTION -> ErrorSeverity.CRITICAL
            ErrorCategory.RESOURCE -> if (exception is OutOfMemoryError) ErrorSeverity.FATAL else ErrorSeverity.ERROR
            ErrorCategory.PERMISSION -> ErrorSeverity.ERROR
            ErrorCategory.NETWORK, ErrorCategory.TIMEOUT -> ErrorSeverity.WARNING
            ErrorCategory.VALIDATION -> ErrorSeverity.WARNING
            ErrorCategory.CONCURRENCY -> ErrorSeverity.ERROR
            else -> ErrorSeverity.ERROR
        }
    }
    
    /**
     * Determine recovery strategy for error
     * 
     * @param category Error category
     * @param severity Error severity
     * @return RecoveryStrategy
     */
    fun determineRecoveryStrategy(category: ErrorCategory, severity: ErrorSeverity): RecoveryStrategy {
        return when {
            severity == ErrorSeverity.FATAL -> RecoveryStrategy.FAIL_FAST
            severity == ErrorSeverity.CRITICAL -> RecoveryStrategy.FAIL_FAST
            category == ErrorCategory.NETWORK -> RecoveryStrategy.RETRY
            category == ErrorCategory.TIMEOUT -> RecoveryStrategy.RETRY
            category == ErrorCategory.CONCURRENCY -> RecoveryStrategy.RETRY
            category == ErrorCategory.RESOURCE -> RecoveryStrategy.FALLBACK
            category == ErrorCategory.VALIDATION -> RecoveryStrategy.FAIL_FAST
            else -> RecoveryStrategy.FAIL_FAST
        }
    }
    
    /**
     * Log an error with classification
     * 
     * @param exception Exception that occurred
     * @param context Additional context information
     * @return ErrorInfo
     */
    fun logError(
        exception: Exception,
        context: Map<String, String> = emptyMap()
    ): ErrorInfo {
        val category = classifyError(exception)
        val severity = determineSeverity(exception, category)
        val strategy = determineRecoveryStrategy(category, severity)
        
        val errorInfo = ErrorInfo(
            category = category.name,
            severity = severity.name,
            message = exception.message ?: "Unknown error",
            stackTrace = exception.stackTraceToString(),
            context = context,
            recoveryStrategy = strategy.name
        )
        
        synchronized(errorLog) {
            errorLog.add(errorInfo)
        }
        
        errorCounts.getOrPut(category.name) { AtomicLong(0) }.incrementAndGet()
        
        return errorInfo
    }
    
    /**
     * Execute operation with retry and exponential backoff
     * 
     * @param config Retry configuration
     * @param operation Operation to retry
     * @return Result of operation
     */
    suspend fun <T> withRetry(
        config: RetryConfig = RetryConfig(),
        operation: suspend () -> T
    ): Result<T> {
        var attempt = 0
        var lastException: Exception? = null
        
        while (attempt < config.maxAttempts) {
            try {
                return Result.success(operation())
            } catch (e: Exception) {
                lastException = e
                attempt++
                
                // Check if exception is retryable
                if (config.retryableExceptions.isNotEmpty() && 
                    config.retryableExceptions.none { it.isInstance(e) }) {
                    break
                }
                
                if (attempt < config.maxAttempts) {
                    val delayMs = calculateBackoff(attempt, config)
                    delay(delayMs)
                }
            }
        }
        
        return Result.failure(lastException ?: Exception("Unknown error during retry"))
    }
    
    /**
     * Calculate exponential backoff delay
     * 
     * @param attempt Current attempt number (1-based)
     * @param config Retry configuration
     * @return Delay in milliseconds
     */
    private fun calculateBackoff(attempt: Int, config: RetryConfig): Long {
        val exponentialDelay = (config.initialDelayMs * config.multiplier.pow(attempt - 1)).toLong()
        return min(exponentialDelay, config.maxDelayMs)
    }
    
    /**
     * Get or create circuit breaker
     * 
     * @param name Circuit breaker name
     * @param failureThreshold Number of failures before opening
     * @param resetTimeoutMs Time before attempting recovery
     * @return CircuitBreaker instance
     */
    fun getCircuitBreaker(
        name: String,
        failureThreshold: Int = 5,
        resetTimeoutMs: Long = 60_000L
    ): CircuitBreaker {
        return circuitBreakers.getOrPut(name) {
            CircuitBreaker(name, failureThreshold, resetTimeoutMs)
        }
    }
    
    /**
     * Execute operation with circuit breaker protection
     * 
     * @param circuitName Name of the circuit breaker
     * @param operation Operation to execute
     * @return Result of operation
     */
    suspend fun <T> withCircuitBreaker(
        circuitName: String,
        operation: suspend () -> T
    ): Result<T> {
        val breaker = getCircuitBreaker(circuitName)
        return breaker.execute(operation)
    }
    
    /**
     * Execute operation with fallback
     * 
     * @param primary Primary operation to try
     * @param fallback Fallback operation if primary fails
     * @return Result from primary or fallback
     */
    suspend fun <T> withFallback(
        primary: suspend () -> T,
        fallback: suspend (Exception) -> T
    ): T {
        return try {
            primary()
        } catch (e: Exception) {
            logError(e, mapOf("strategy" to "fallback"))
            fallback(e)
        }
    }
    
    /**
     * Execute operation with timeout
     * 
     * @param timeoutMs Timeout in milliseconds
     * @param operation Operation to execute
     * @return Result of operation or timeout exception
     */
    suspend fun <T> withTimeout(
        timeoutMs: Long,
        operation: suspend () -> T
    ): Result<T> {
        return try {
            Result.success(
                kotlinx.coroutines.withTimeout(timeoutMs) {
                    operation()
                }
            )
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            logError(Exception("Operation timed out after ${timeoutMs}ms"), 
                    mapOf("timeout_ms" to timeoutMs.toString()))
            Result.failure(e)
        } catch (e: Exception) {
            logError(e)
            Result.failure(e)
        }
    }
    
    /**
     * Get error statistics
     * 
     * @return Map of error category to count
     */
    fun getErrorStatistics(): Map<String, Long> {
        return errorCounts.mapValues { it.value.get() }
    }
    
    /**
     * Get recent errors
     * 
     * @param limit Maximum number of errors to return
     * @return List of recent errors
     */
    fun getRecentErrors(limit: Int = 100): List<ErrorInfo> {
        return synchronized(errorLog) {
            errorLog.takeLast(limit)
        }
    }
    
    /**
     * Get errors by category
     * 
     * @param category Error category to filter
     * @return List of errors in category
     */
    fun getErrorsByCategory(category: ErrorCategory): List<ErrorInfo> {
        return synchronized(errorLog) {
            errorLog.filter { it.category == category.name }
        }
    }
    
    /**
     * Get errors by severity
     * 
     * @param severity Error severity to filter
     * @return List of errors with severity
     */
    fun getErrorsBySeverity(severity: ErrorSeverity): List<ErrorInfo> {
        return synchronized(errorLog) {
            errorLog.filter { it.severity == severity.name }
        }
    }
    
    /**
     * Clear error log (for testing)
     */
    fun clearErrorLog() {
        synchronized(errorLog) {
            errorLog.clear()
        }
        errorCounts.clear()
    }
    
    /**
     * Reset circuit breaker
     * 
     * @param name Circuit breaker name
     */
    fun resetCircuitBreaker(name: String) {
        circuitBreakers[name]?.reset()
    }
    
    /**
     * Get total error count
     * 
     * @return Total number of errors logged
     */
    fun getTotalErrorCount(): Int {
        return synchronized(errorLog) {
            errorLog.size
        }
    }
}
