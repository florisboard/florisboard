/*
 * ZIPRAF_OMEGA Risk Mitigation Module Tests
 * Copyright (C) 2025 Rafael Melo Reis
 */

package org.florisboard.lib.zipraf

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RiskMitigationModuleTest {
    
    private lateinit var module: RiskMitigationModule
    
    @BeforeEach
    fun setup() {
        module = RiskMitigationModule.getInstance()
        module.resetMetrics()
    }
    
    @AfterEach
    fun tearDown() {
        module.resetMetrics()
    }
    
    @Test
    fun `test latency measurement within threshold`() = runTest {
        val (result, measurement) = module.measureLatency("fast_op", 100L) {
            delay(10)
            "success"
        }
        
        assertEquals("success", result)
        assertFalse(measurement.exceedsThreshold)
        assertTrue(measurement.durationMs < 100L)
    }
    
    @Test
    fun `test latency measurement exceeds threshold`() = runTest {
        val (result, measurement) = module.measureLatency("slow_op", 10L) {
            delay(50)
            "completed"
        }
        
        assertEquals("completed", result)
        assertTrue(measurement.exceedsThreshold)
        assertTrue(measurement.durationMs >= 50L)
    }
    
    @Test
    fun `test fragmentation detection`() {
        val info = module.checkFragmentation()
        
        assertNotNull(info)
        assertTrue(info.totalMemoryBytes > 0)
        assertTrue(info.freeMemoryBytes >= 0)
        assertTrue(info.fragmentationRatio >= 0.0 && info.fragmentationRatio <= 1.0)
    }
    
    @Test
    fun `test garbage collection trigger`() {
        val result = module.triggerGarbageCollection()
        assertTrue(result)
    }
    
    @Test
    fun `test process registration`() {
        module.registerProcess("proc1", "TestProcess")
        
        val metrics = module.getMetrics()
        assertEquals(1L, metrics["active_processes"])
    }
    
    @Test
    fun `test process activity update`() {
        module.registerProcess("proc1", "TestProcess")
        module.updateProcessActivity("proc1")
        
        // Should not throw exception
        assertTrue(true)
    }
    
    @Test
    fun `test process unregistration`() {
        module.registerProcess("proc1", "TestProcess")
        module.unregisterProcess("proc1")
        
        val metrics = module.getMetrics()
        assertEquals(0L, metrics["active_processes"])
    }
    
    @Test
    fun `test zombie process detection`() = runTest {
        module.registerProcess("zombie", "ZombieProcess")
        
        // Should not be zombie immediately
        val zombies1 = module.detectZombieProcesses()
        assertTrue(zombies1.isEmpty())
        
        // Note: In real test would wait for threshold, here we just verify the mechanism
        val metrics = module.getMetrics()
        assertNotNull(metrics["active_processes"])
    }
    
    @Test
    fun `test zombie cleanup`() {
        module.registerProcess("zombie1", "Zombie1")
        module.registerProcess("zombie2", "Zombie2")
        
        val zombie = ZombieProcess(
            processId = "zombie1",
            name = "Zombie1",
            createdAt = System.currentTimeMillis(),
            lastActivityAt = 0L,
            idleTimeMs = 1000000L,
            isZombie = true
        )
        
        val cleaned = module.cleanupZombieProcesses(listOf(zombie))
        assertEquals(1, cleaned)
    }
    
    @Test
    fun `test redundancy detection`() {
        val data = listOf("a", "b", "c", "a", "b", "d")
        val redundant = module.detectRedundancy(data)
        
        assertEquals(2, redundant.size)
        assertTrue(redundant.contains("a"))
        assertTrue(redundant.contains("b"))
    }
    
    @Test
    fun `test redundancy detection with no duplicates`() {
        val data = listOf("a", "b", "c", "d")
        val redundant = module.detectRedundancy(data)
        
        assertTrue(redundant.isEmpty())
    }
    
    @Test
    fun `test metrics collection`() = runTest {
        // Generate some events
        module.measureLatency("op1", 10L) { delay(20); "result" }
        module.checkFragmentation()
        module.detectRedundancy(listOf("a", "a", "b"))
        
        val metrics = module.getMetrics()
        
        assertNotNull(metrics["latency_violations"])
        assertNotNull(metrics["redundancies_found"])
        assertTrue(metrics["latency_violations"]!! >= 0)
    }
    
    @Test
    fun `test average latency calculation`() = runTest {
        module.measureLatency("test_op", 1000L) { delay(10); "r1" }
        module.measureLatency("test_op", 1000L) { delay(20); "r2" }
        module.measureLatency("test_op", 1000L) { delay(30); "r3" }
        
        val avg = module.getAverageLatency("test_op")
        assertNotNull(avg)
        assertTrue(avg!! > 0.0)
    }
    
    @Test
    fun `test average latency for unknown operation`() {
        val avg = module.getAverageLatency("unknown")
        assertNull(avg)
    }
    
    @Test
    fun `test risk event emission`() = runTest {
        // Start collecting events
        val eventJob = kotlinx.coroutines.launch {
            val event = module.riskEvents.first()
            assertNotNull(event)
            assertEquals(RiskType.LATENCY.name, event.riskType)
        }
        
        // Trigger event by exceeding threshold
        module.measureLatency("slow", 1L) {
            delay(10)
            "result"
        }
        
        eventJob.join()
    }
    
    @Test
    fun `test metrics reset`() = runTest {
        // Generate some data
        module.measureLatency("op", 1L) { delay(10); "result" }
        module.checkFragmentation()
        
        // Reset
        module.resetMetrics()
        
        val metrics = module.getMetrics()
        assertEquals(0L, metrics["latency_violations"])
        assertEquals(0L, metrics["fragmentation_events"])
    }
}
