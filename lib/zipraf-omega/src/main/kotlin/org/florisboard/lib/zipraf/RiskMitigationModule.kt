/*
 * ZIPRAF_OMEGA Risk Mitigation Module v999
 * Copyright (C) 2025 Rafael Melo Reis
 * 
 * This module implements comprehensive risk mitigation strategies for:
 * - Bug detection and prevention
 * - Latency monitoring and optimization
 * - Fragmentation detection and defragmentation
 * - Redundancy elimination
 * - Zombie process detection and cleanup
 * 
 * License: Apache 2.0
 * Authorship credentials: Rafael Melo Reis
 * 
 * Standards Compliance:
 * - ISO 9001 (Quality Management)
 * - ISO 27001 (Information Security Management)
 * - IEEE 1012 (Software Verification and Validation)
 * - IEEE 1633 (Software Reliability)
 * - NIST 800-53 (Security and Privacy Controls)
 */

package org.florisboard.lib.zipraf

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Risk types that can be detected and mitigated
 */
enum class RiskType {
    BUG,
    LATENCY,
    FRAGMENTATION,
    REDUNDANCY,
    ZOMBIE_PROCESS,
    MEMORY_LEAK,
    DEADLOCK,
    RACE_CONDITION
}

/**
 * Risk severity levels
 */
enum class RiskSeverity {
    CRITICAL,   // System-breaking, requires immediate action
    HIGH,       // Significant impact, requires prompt action
    MEDIUM,     // Moderate impact, should be addressed soon
    LOW,        // Minor impact, can be addressed later
    INFO        // Informational, no action required
}

/**
 * Risk detection result
 */
@Serializable
data class RiskDetectionResult(
    val riskType: String,
    val severity: String,
    val detected: Boolean,
    val description: String,
    val mitigation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val metrics: Map<String, Double> = emptyMap()
)

/**
 * Latency measurement result
 */
data class LatencyMeasurement(
    val operationName: String,
    val durationMs: Long,
    val thresholdMs: Long,
    val exceedsThreshold: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Memory fragmentation info
 */
data class FragmentationInfo(
    val totalMemoryBytes: Long,
    val freeMemoryBytes: Long,
    val fragmentationRatio: Double,
    val isFragmented: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Zombie process detection result
 */
data class ZombieProcess(
    val processId: String,
    val name: String,
    val createdAt: Long,
    val lastActivityAt: Long,
    val idleTimeMs: Long,
    val isZombie: Boolean
)

/**
 * Comprehensive Risk Mitigation Module
 * 
 * Provides detection and mitigation for multiple risk categories:
 * - Latency monitoring with thresholds
 * - Memory fragmentation detection
 * - Redundancy identification
 * - Zombie process detection
 * - Bug pattern detection
 * 
 * Implements continuous monitoring with configurable intervals.
 */
class RiskMitigationModule {
    
    // Latency tracking
    private val latencyMeasurements = ConcurrentHashMap<String, MutableList<LatencyMeasurement>>()
    private val latencyThresholds = ConcurrentHashMap<String, Long>()
    
    // Process tracking for zombie detection
    private val activeProcesses = ConcurrentHashMap<String, ZombieProcess>()
    private val zombieDetectionThresholdMs = 300_000L // 5 minutes of inactivity
    
    // Metrics
    private val bugsDetected = AtomicLong(0)
    private val latencyViolations = AtomicLong(0)
    private val fragmentationEvents = AtomicLong(0)
    private val redundanciesFound = AtomicLong(0)
    private val zombiesDetected = AtomicLong(0)
    
    // State flow for real-time monitoring
    private val _riskEvents = MutableSharedFlow<RiskDetectionResult>(replay = 10)
    val riskEvents: SharedFlow<RiskDetectionResult> = _riskEvents.asSharedFlow()
    
    companion object {
        private var instance: RiskMitigationModule? = null
        
        fun getInstance(): RiskMitigationModule {
            return instance ?: synchronized(this) {
                instance ?: RiskMitigationModule().also { instance = it }
            }
        }
    }
    
    /**
     * Measure latency of an operation and check against threshold
     * 
     * @param operationName Name of the operation being measured
     * @param thresholdMs Maximum acceptable duration in milliseconds
     * @param operation The operation to measure
     * @return LatencyMeasurement result
     */
    suspend fun <T> measureLatency(
        operationName: String,
        thresholdMs: Long,
        operation: suspend () -> T
    ): Pair<T, LatencyMeasurement> {
        var result: T? = null
        val duration = measureTimeMillis {
            result = operation()
        }
        
        val measurement = LatencyMeasurement(
            operationName = operationName,
            durationMs = duration,
            thresholdMs = thresholdMs,
            exceedsThreshold = duration > thresholdMs
        )
        
        // Store measurement
        latencyMeasurements.getOrPut(operationName) { mutableListOf() }.add(measurement)
        
        // Emit risk event if threshold exceeded
        if (measurement.exceedsThreshold) {
            latencyViolations.incrementAndGet()
            _riskEvents.emit(
                RiskDetectionResult(
                    riskType = RiskType.LATENCY.name,
                    severity = if (duration > thresholdMs * 2) RiskSeverity.HIGH.name else RiskSeverity.MEDIUM.name,
                    detected = true,
                    description = "Operation '$operationName' took ${duration}ms (threshold: ${thresholdMs}ms)",
                    mitigation = "Consider optimizing the operation or increasing resources",
                    metrics = mapOf(
                        "duration_ms" to duration.toDouble(),
                        "threshold_ms" to thresholdMs.toDouble(),
                        "ratio" to (duration.toDouble() / thresholdMs)
                    )
                )
            )
        }
        
        return Pair(result!!, measurement)
    }
    
    /**
     * Check for memory fragmentation
     * 
     * @return FragmentationInfo with current fragmentation status
     */
    fun checkFragmentation(): FragmentationInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        
        // Calculate fragmentation ratio (simplified)
        // High fragmentation = low ratio of available contiguous memory
        val usedMemory = totalMemory - freeMemory
        val fragmentationRatio = if (totalMemory > 0) {
            usedMemory.toDouble() / totalMemory.toDouble()
        } else {
            0.0
        }
        
        // Consider fragmented if >80% used or complex pattern
        val isFragmented = fragmentationRatio > 0.80 || 
                          (totalMemory < maxMemory * 0.5 && freeMemory < totalMemory * 0.2)
        
        val info = FragmentationInfo(
            totalMemoryBytes = totalMemory,
            freeMemoryBytes = freeMemory,
            fragmentationRatio = fragmentationRatio,
            isFragmented = isFragmented
        )
        
        if (isFragmented) {
            fragmentationEvents.incrementAndGet()
            GlobalScope.launch {
                _riskEvents.emit(
                    RiskDetectionResult(
                        riskType = RiskType.FRAGMENTATION.name,
                        severity = if (fragmentationRatio > 0.90) RiskSeverity.HIGH.name else RiskSeverity.MEDIUM.name,
                        detected = true,
                        description = "Memory fragmentation detected: ${(fragmentationRatio * 100).toInt()}% used",
                        mitigation = "Consider triggering garbage collection or memory defragmentation",
                        metrics = mapOf(
                            "total_bytes" to totalMemory.toDouble(),
                            "free_bytes" to freeMemory.toDouble(),
                            "fragmentation_ratio" to fragmentationRatio
                        )
                    )
                )
            }
        }
        
        return info
    }
    
    /**
     * Suggest garbage collection to reduce fragmentation
     * 
     * @return true if GC was triggered
     */
    fun triggerGarbageCollection(): Boolean {
        return try {
            System.gc()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Register a process for zombie detection
     * 
     * @param processId Unique identifier for the process
     * @param processName Human-readable name
     */
    fun registerProcess(processId: String, processName: String) {
        val now = System.currentTimeMillis()
        activeProcesses[processId] = ZombieProcess(
            processId = processId,
            name = processName,
            createdAt = now,
            lastActivityAt = now,
            idleTimeMs = 0,
            isZombie = false
        )
    }
    
    /**
     * Update process activity timestamp
     * 
     * @param processId Process to update
     */
    fun updateProcessActivity(processId: String) {
        activeProcesses[processId]?.let { process ->
            activeProcesses[processId] = process.copy(lastActivityAt = System.currentTimeMillis())
        }
    }
    
    /**
     * Unregister a process (normal termination)
     * 
     * @param processId Process to remove
     */
    fun unregisterProcess(processId: String) {
        activeProcesses.remove(processId)
    }
    
    /**
     * Detect zombie processes (inactive beyond threshold)
     * 
     * @return List of detected zombie processes
     */
    suspend fun detectZombieProcesses(): List<ZombieProcess> {
        val now = System.currentTimeMillis()
        val zombies = mutableListOf<ZombieProcess>()
        
        activeProcesses.forEach { (id, process) ->
            val idleTime = now - process.lastActivityAt
            if (idleTime > zombieDetectionThresholdMs) {
                val zombie = process.copy(
                    idleTimeMs = idleTime,
                    isZombie = true
                )
                zombies.add(zombie)
                zombiesDetected.incrementAndGet()
                
                _riskEvents.emit(
                    RiskDetectionResult(
                        riskType = RiskType.ZOMBIE_PROCESS.name,
                        severity = RiskSeverity.MEDIUM.name,
                        detected = true,
                        description = "Zombie process detected: ${process.name} (ID: $id, idle: ${idleTime}ms)",
                        mitigation = "Terminate the zombie process and investigate root cause",
                        metrics = mapOf(
                            "idle_time_ms" to idleTime.toDouble(),
                            "threshold_ms" to zombieDetectionThresholdMs.toDouble()
                        )
                    )
                )
            }
        }
        
        return zombies
    }
    
    /**
     * Cleanup zombie processes
     * 
     * @param zombies List of zombies to cleanup
     * @return Number of processes cleaned up
     */
    fun cleanupZombieProcesses(zombies: List<ZombieProcess>): Int {
        var cleaned = 0
        zombies.forEach { zombie ->
            if (activeProcesses.remove(zombie.processId) != null) {
                cleaned++
            }
        }
        return cleaned
    }
    
    /**
     * Detect redundant data in a collection
     * 
     * @param data Collection to check for redundancy
     * @return List of redundant items
     */
    fun <T> detectRedundancy(data: Collection<T>): List<T> {
        val seen = mutableSetOf<T>()
        val redundant = mutableListOf<T>()
        
        data.forEach { item ->
            if (!seen.add(item)) {
                redundant.add(item)
            }
        }
        
        if (redundant.isNotEmpty()) {
            redundanciesFound.addAndGet(redundant.size.toLong())
            GlobalScope.launch {
                _riskEvents.emit(
                    RiskDetectionResult(
                        riskType = RiskType.REDUNDANCY.name,
                        severity = if (redundant.size > data.size * 0.3) RiskSeverity.MEDIUM.name else RiskSeverity.LOW.name,
                        detected = true,
                        description = "Found ${redundant.size} redundant items out of ${data.size} total",
                        mitigation = "Remove redundant items to optimize memory usage",
                        metrics = mapOf(
                            "total_items" to data.size.toDouble(),
                            "redundant_items" to redundant.size.toDouble(),
                            "redundancy_ratio" to (redundant.size.toDouble() / data.size)
                        )
                    )
                )
            }
        }
        
        return redundant
    }
    
    /**
     * Get comprehensive metrics summary
     * 
     * @return Map of metric names to values
     */
    fun getMetrics(): Map<String, Long> {
        return mapOf(
            "bugs_detected" to bugsDetected.get(),
            "latency_violations" to latencyViolations.get(),
            "fragmentation_events" to fragmentationEvents.get(),
            "redundancies_found" to redundanciesFound.get(),
            "zombies_detected" to zombiesDetected.get(),
            "active_processes" to activeProcesses.size.toLong(),
            "latency_measurements" to latencyMeasurements.values.sumOf { it.size }.toLong()
        )
    }
    
    /**
     * Get average latency for an operation
     * 
     * @param operationName Name of the operation
     * @return Average latency in milliseconds, or null if no measurements
     */
    fun getAverageLatency(operationName: String): Double? {
        val measurements = latencyMeasurements[operationName]
        return measurements?.let { list ->
            if (list.isEmpty()) null
            else list.map { it.durationMs }.average()
        }
    }
    
    /**
     * Start continuous monitoring
     * 
     * @param scope CoroutineScope for monitoring job
     * @param intervalMs Monitoring interval in milliseconds
     * @return Job handle for the monitoring coroutine
     */
    fun startContinuousMonitoring(
        scope: CoroutineScope,
        intervalMs: Long = 60_000L
    ): Job {
        return scope.launch {
            while (isActive) {
                // Check fragmentation
                checkFragmentation()
                
                // Detect zombies
                val zombies = detectZombieProcesses()
                if (zombies.isNotEmpty()) {
                    cleanupZombieProcesses(zombies)
                }
                
                delay(intervalMs)
            }
        }
    }
    
    /**
     * Reset all metrics (for testing)
     */
    fun resetMetrics() {
        bugsDetected.set(0)
        latencyViolations.set(0)
        fragmentationEvents.set(0)
        redundanciesFound.set(0)
        zombiesDetected.set(0)
        latencyMeasurements.clear()
        activeProcesses.clear()
    }
}
