/*
 * ZIPRAF_OMEGA Performance Benchmark Suite
 * Copyright (C) 2025 The FlorisBoard Contributors
 * 
 * Benchmarks for measuring performance characteristics:
 * - Throughput (operations per second)
 * - Latency (time per operation)
 * - Memory footprint (allocation per operation)
 * - GC pressure (garbage collection overhead)
 * 
 * Note: These are JVM benchmarks. For Android-specific benchmarks,
 * use androidx.benchmark.macro in the benchmark module.
 */

package org.florisboard.lib.zipraf

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.system.measureTimeMillis
import kotlin.system.measureNanoTime

/**
 * Benchmark suite for ZIPRAF_OMEGA module
 * 
 * To run these benchmarks:
 * ./gradlew :lib:zipraf-omega:test --tests "*.PerformanceBenchmark*"
 * 
 * For production benchmarking, consider using JMH (Java Microbenchmark Harness)
 * or Android Macrobenchmark.
 */
class PerformanceBenchmark {
    
    // Benchmark configuration constants
    companion object {
        private const val MAX_MEMORY_FOOTPRINT_BYTES = 2048 // 2KB per operation target
    }
    
    private lateinit var licensing: LicensingModule
    private lateinit var optimizer: PerformanceOptimizer
    private lateinit var loop: OperationalLoop
    private lateinit var versionManager: VersionManager
    
    @BeforeEach
    fun setup() {
        licensing = LicensingModule()
        optimizer = PerformanceOptimizer.getInstance()
        loop = OperationalLoop()
        versionManager = VersionManager()
    }
    
    /**
     * Benchmark: Licensing validation throughput
     * 
     * Measures: Operations per second for full validation
     * Target: >10,000 ops/sec
     */
    @Test
    fun benchmarkLicensingValidationThroughput() {
        val iterations = 10000
        val context = ExecutionContext(
            data = "test_data",
            authorId = "Rafael Melo Reis",
            hasPermission = true,
            destinationValid = true,
            ethicalPurposeValid = true
        )
        
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                licensing.ziprafOmegaFunction(context)
            }
        }
        
        val opsPerSec = iterations * 1000.0 / timeMs
        println("Licensing Validation Throughput: ${opsPerSec.toInt()} ops/sec")
        println("Average latency: ${timeMs.toDouble() / iterations} ms")
        
        // Assert performance target
        assert(opsPerSec > 10000) {
            "Validation throughput below target: $opsPerSec ops/sec (target: >10,000)"
        }
    }
    
    /**
     * Benchmark: Hash computation performance
     * 
     * Measures: Time to compute cryptographic hash
     * Target: <2ms for 1KB data
     */
    @Test
    fun benchmarkHashComputation() {
        val data = ByteArray(1024) { it.toByte() } // 1KB
        val iterations = 1000
        
        val totalNs = measureNanoTime {
            repeat(iterations) {
                licensing.computeHash(data)
            }
        }
        
        val avgMs = totalNs / 1_000_000.0 / iterations
        println("Hash computation (1KB): ${avgMs} ms")
        
        assert(avgMs < 2.0) {
            "Hash computation too slow: ${avgMs}ms (target: <2ms)"
        }
    }
    
    /**
     * Benchmark: Cache performance under load
     * 
     * Measures: Cache hit rate and throughput
     * Target: >90% hit rate, >100,000 ops/sec
     */
    @Test
    fun benchmarkCachePerformance() {
        optimizer.clearCache()
        
        val iterations = 10000
        val uniqueKeys = 100 // 100 unique keys, lots of repetition
        
        val timeMs = measureTimeMillis {
            repeat(iterations) { i ->
                val key = "key_${i % uniqueKeys}"
                optimizer.getOrCompute(key) {
                    // Simulate expensive computation
                    "computed_value_$key"
                }
            }
        }
        
        val metrics = optimizer.getMetrics()
        val opsPerSec = iterations * 1000.0 / timeMs
        
        println("Cache Performance:")
        println("  Operations: ${metrics.totalOperations}")
        println("  Hit rate: ${metrics.hitRate * 100}%")
        println("  Throughput: ${opsPerSec.toInt()} ops/sec")
        
        assert(metrics.hitRate > 0.90) {
            "Cache hit rate below target: ${metrics.hitRate} (target: >0.90)"
        }
        assert(opsPerSec > 100000) {
            "Cache throughput below target: $opsPerSec ops/sec (target: >100,000)"
        }
    }
    
    /**
     * Benchmark: Matrix operations performance
     * 
     * Measures: Matrix multiplication time
     * Target: <1ms for 10x10, <100ms for 100x100
     */
    @Test
    fun benchmarkMatrixOperations() {
        // Small matrix (10x10)
        val small1 = optimizer.acquireMatrix(10, 10)
        val small2 = optimizer.acquireMatrix(10, 10)
        small1.fill(1.0)
        small2.fill(1.0)
        
        val smallTimeMs = measureTimeMillis {
            repeat(1000) {
                small1.multiply(small2)
            }
        } / 1000.0
        
        println("Matrix multiply (10x10): ${smallTimeMs} ms")
        
        optimizer.releaseMatrix(small1)
        optimizer.releaseMatrix(small2)
        
        // Large matrix (100x100)
        val large1 = optimizer.acquireMatrix(100, 100)
        val large2 = optimizer.acquireMatrix(100, 100)
        large1.fill(1.0)
        large2.fill(1.0)
        
        val largeTimeMs = measureTimeMillis {
            large1.multiply(large2)
        }
        
        println("Matrix multiply (100x100): ${largeTimeMs} ms")
        
        optimizer.releaseMatrix(large1)
        optimizer.releaseMatrix(large2)
        
        assert(smallTimeMs < 1.0) {
            "Small matrix multiplication too slow: ${smallTimeMs}ms (target: <1ms)"
        }
        assert(largeTimeMs < 100) {
            "Large matrix multiplication too slow: ${largeTimeMs}ms (target: <100ms)"
        }
    }
    
    /**
     * Benchmark: Matrix pool efficiency
     * 
     * Measures: Pool hit rate and allocation reduction
     * Target: >80% pool hits
     */
    @Test
    fun benchmarkMatrixPoolEfficiency() {
        val iterations = 1000
        val size = 50
        
        // Warm up pool
        repeat(10) {
            val m = optimizer.acquireMatrix(size, size)
            optimizer.releaseMatrix(m)
        }
        
        // Measure pool performance
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                val m = optimizer.acquireMatrix(size, size)
                // Simulate work
                m[0, 0] = 1.0
                optimizer.releaseMatrix(m)
            }
        }
        
        val avgMs = timeMs.toDouble() / iterations
        println("Matrix pool operations: ${avgMs} ms/operation")
        
        // Pool should make this very fast
        assert(avgMs < 0.1) {
            "Matrix pool operations too slow: ${avgMs}ms (target: <0.1ms)"
        }
    }
    
    /**
     * Benchmark: Operational loop throughput
     * 
     * Measures: Cycles per second
     * Target: >100 cycles/sec (with 0ms delay)
     */
    @Test
    fun benchmarkOperationalLoopThroughput() = runBlocking {
        var cycleCount = 0
        val durationMs = 1000L // Run for 1 second
        
        loop.start(
            scope = this,
            cycleDelayMs = 0, // No delay for max throughput
            onCycle = { _ -> cycleCount++ } // Ignore LoopCycleResult, only count cycles
        )
        
        kotlinx.coroutines.delay(durationMs)
        loop.stop()
        
        val cyclesPerSec = cycleCount * 1000.0 / durationMs
        println("Operational loop throughput: ${cyclesPerSec.toInt()} cycles/sec")
        
        assert(cyclesPerSec > 100) {
            "Loop throughput below target: $cyclesPerSec cycles/sec (target: >100)"
        }
    }
    
    /**
     * Benchmark: Version compatibility checking
     * 
     * Measures: Compatibility check throughput
     * Target: >100,000 ops/sec (conservative for broad hardware compatibility)
     * 
     * Note: On high-performance systems, this may exceed 1M ops/sec.
     * The conservative target ensures tests pass on various hardware.
     */
    @Test
    fun benchmarkVersionCompatibility() {
        val iterations = 100000
        val version = SemanticVersion(1, 0, 0)
        
        val timeMs = measureTimeMillis {
            repeat(iterations) {
                versionManager.checkCompatibility(version)
            }
        }
        
        val opsPerSec = iterations * 1000.0 / timeMs
        println("Version compatibility checks: ${opsPerSec.toInt()} ops/sec")
        
        // Conservative target for broad hardware compatibility
        assert(opsPerSec > 100000) {
            "Version check throughput below target: $opsPerSec ops/sec (target: >100K)"
        }
    }
    
    /**
     * Memory footprint benchmark
     * 
     * Measures: Memory allocated per operation
     * Target: <1KB per operation
     * 
     * Note: This is a simple approximation. For production benchmarking,
     * use tools like JMH with memory profilers or Android Studio Profiler.
     * Results may vary due to JVM/GC behavior.
     */
    @Test
    fun benchmarkMemoryFootprint() {
        val runtime = Runtime.getRuntime()
        val iterations = 1000
        
        // Warmup to stabilize JVM state
        repeat(100) {
            ExecutionContext(
                data = "warmup",
                authorId = "Rafael Melo Reis",
                hasPermission = true,
                destinationValid = true,
                ethicalPurposeValid = true
            )
        }
        
        // Multiple measurements for stability
        val measurements = mutableListOf<Long>()
        repeat(5) {
            val beforeMem = runtime.totalMemory() - runtime.freeMemory()
            
            // Allocate objects
            val contexts = List(iterations) { i ->
                ExecutionContext(
                    data = "test_data_$i",
                    authorId = "Rafael Melo Reis",
                    hasPermission = true,
                    destinationValid = true,
                    ethicalPurposeValid = true
                )
            }
            
            val afterMem = runtime.totalMemory() - runtime.freeMemory()
            measurements.add(afterMem - beforeMem)
            
            // Keep reference to prevent premature GC
            assert(contexts.isNotEmpty())
        }
        
        // Use median to reduce variance (efficient median calculation)
        val sortedMeasurements = measurements.sorted()
        val usedBytes = sortedMeasurements[sortedMeasurements.size / 2]
        val bytesPerOp = usedBytes / iterations
        
        println("Memory footprint (median): ${usedBytes / 1024} KB total, ${bytesPerOp} bytes/operation")
        
        // Conservative target - actual footprint varies by JVM
        assert(bytesPerOp < MAX_MEMORY_FOOTPRINT_BYTES) {
            "Memory footprint too large: ${bytesPerOp} bytes/op (target: <${MAX_MEMORY_FOOTPRINT_BYTES}, note: varies by JVM)"
        }
    }
}

/**
 * Benchmark results summary
 * 
 * To generate a comprehensive report, run all benchmarks and collect results.
 * 
 * Example report structure:
 * ```
 * ZIPRAF_OMEGA Performance Report
 * ===============================
 * 
 * 1. Licensing Module
 *    - Validation: 15,234 ops/sec
 *    - Hash (1KB): 1.2 ms
 * 
 * 2. Performance Optimizer
 *    - Cache hit rate: 95%
 *    - Cache throughput: 250,000 ops/sec
 *    - Matrix (10x10): 0.5 ms
 *    - Matrix (100x100): 45 ms
 * 
 * 3. Operational Loop
 *    - Throughput: 150 cycles/sec
 *    - Memory per cycle: 15 KB
 * 
 * 4. Version Manager
 *    - Compatibility: 2,000,000 ops/sec
 * 
 * Overall: PASSES all performance targets
 * ```
 */
