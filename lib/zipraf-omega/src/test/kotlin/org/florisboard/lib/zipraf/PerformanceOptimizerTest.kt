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
 * Test suite for PerformanceOptimizer
 * 
 * Verifies caching, matrix operations, and performance optimizations
 */
class PerformanceOptimizerTest {
    
    @Test
    fun testGetOrCompute_CacheMiss() {
        val optimizer = PerformanceOptimizer.getInstance()
        optimizer.clearCache()
        
        var computeCount = 0
        val result = optimizer.getOrCompute("key1") {
            computeCount++
            "computed value"
        }
        
        assertEquals("computed value", result)
        assertEquals(1, computeCount, "Should compute once on cache miss")
    }
    
    @Test
    fun testGetOrCompute_CacheHit() {
        val optimizer = PerformanceOptimizer.getInstance()
        optimizer.clearCache()
        
        var computeCount = 0
        
        // First call - cache miss
        optimizer.getOrCompute("key1") {
            computeCount++
            "computed value"
        }
        
        // Second call - cache hit
        val result = optimizer.getOrCompute("key1") {
            computeCount++
            "should not be computed"
        }
        
        assertEquals("computed value", result)
        assertEquals(1, computeCount, "Should not recompute on cache hit")
    }
    
    @Test
    fun testClearCache() {
        val optimizer = PerformanceOptimizer.getInstance()
        
        optimizer.getOrCompute("key1") { "value1" }
        optimizer.clearCache()
        
        var computeCount = 0
        optimizer.getOrCompute("key1") {
            computeCount++
            "value2"
        }
        
        assertEquals(1, computeCount, "Should recompute after cache clear")
    }
    
    @Test
    fun testGetMetrics() {
        val optimizer = PerformanceOptimizer.getInstance()
        optimizer.clearCache()
        
        optimizer.getOrCompute("key1") { "value1" }
        optimizer.getOrCompute("key1") { "value1" }
        optimizer.getOrCompute("key2") { "value2" }
        
        val metrics = optimizer.getMetrics()
        
        assertTrue(metrics.totalOperations >= 3)
        assertTrue(metrics.cacheHits >= 1, "Should have at least one cache hit")
        assertTrue(metrics.hitRate > 0.0, "Hit rate should be positive")
    }
    
    @Test
    fun testMatrix_Creation() {
        val matrix = Matrix(3, 3)
        
        assertEquals(3, matrix.rows)
        assertEquals(3, matrix.cols)
    }
    
    @Test
    fun testMatrix_GetSet() {
        val matrix = Matrix(2, 2)
        
        matrix[0, 0] = 1.0
        matrix[0, 1] = 2.0
        matrix[1, 0] = 3.0
        matrix[1, 1] = 4.0
        
        assertEquals(1.0, matrix[0, 0])
        assertEquals(2.0, matrix[0, 1])
        assertEquals(3.0, matrix[1, 0])
        assertEquals(4.0, matrix[1, 1])
    }
    
    @Test
    fun testMatrix_Fill() {
        val matrix = Matrix(3, 3)
        
        matrix.fill(5.0)
        
        for (i in 0 until 3) {
            for (j in 0 until 3) {
                assertEquals(5.0, matrix[i, j])
            }
        }
    }
    
    @Test
    fun testMatrix_Reset() {
        val matrix = Matrix(2, 2)
        
        matrix[0, 0] = 1.0
        matrix[1, 1] = 2.0
        matrix.reset()
        
        assertEquals(0.0, matrix[0, 0])
        assertEquals(0.0, matrix[1, 1])
    }
    
    @Test
    fun testMatrix_Multiply() {
        val m1 = Matrix(2, 2)
        m1[0, 0] = 1.0
        m1[0, 1] = 2.0
        m1[1, 0] = 3.0
        m1[1, 1] = 4.0
        
        val m2 = Matrix(2, 2)
        m2[0, 0] = 2.0
        m2[0, 1] = 0.0
        m2[1, 0] = 1.0
        m2[1, 1] = 2.0
        
        val result = m1.multiply(m2)
        
        assertEquals(4.0, result[0, 0]) // 1*2 + 2*1
        assertEquals(4.0, result[0, 1]) // 1*0 + 2*2
        assertEquals(10.0, result[1, 0]) // 3*2 + 4*1
        assertEquals(8.0, result[1, 1]) // 3*0 + 4*2
    }
    
    @Test
    fun testMatrix_Add() {
        val m1 = Matrix(2, 2)
        m1[0, 0] = 1.0
        m1[0, 1] = 2.0
        m1[1, 0] = 3.0
        m1[1, 1] = 4.0
        
        val m2 = Matrix(2, 2)
        m2[0, 0] = 1.0
        m2[0, 1] = 1.0
        m2[1, 0] = 1.0
        m2[1, 1] = 1.0
        
        val result = m1.add(m2)
        
        assertEquals(2.0, result[0, 0])
        assertEquals(3.0, result[0, 1])
        assertEquals(4.0, result[1, 0])
        assertEquals(5.0, result[1, 1])
    }
    
    @Test
    fun testMatrixPool_AcquireRelease() {
        val optimizer = PerformanceOptimizer.getInstance()
        
        val matrix1 = optimizer.acquireMatrix(3, 3)
        assertEquals(3, matrix1.rows)
        assertEquals(3, matrix1.cols)
        
        matrix1[0, 0] = 5.0
        optimizer.releaseMatrix(matrix1)
        
        val matrix2 = optimizer.acquireMatrix(3, 3)
        // Matrix should be reset after release
        assertEquals(0.0, matrix2[0, 0])
        
        optimizer.releaseMatrix(matrix2)
    }
    
    @Test
    fun testQueueOptimizer_EnqueueDequeue() {
        val queue = QueueOptimizer<String>()
        
        queue.enqueue("item1")
        queue.enqueue("item2")
        
        assertEquals("item1", queue.dequeue())
        assertEquals("item2", queue.dequeue())
        assertEquals(null, queue.dequeue())
    }
    
    @Test
    fun testQueueOptimizer_Size() {
        val queue = QueueOptimizer<Int>()
        
        assertEquals(0, queue.size())
        assertTrue(queue.isEmpty())
        
        queue.enqueue(1)
        queue.enqueue(2)
        
        assertEquals(2, queue.size())
        assertFalse(queue.isEmpty())
        
        queue.dequeue()
        assertEquals(1, queue.size())
    }
    
    @Test
    fun testQueueOptimizer_BatchDequeue() {
        val queue = QueueOptimizer<Int>()
        
        for (i in 1..10) {
            queue.enqueue(i)
        }
        
        val batch = queue.dequeueBatch(5)
        
        assertEquals(5, batch.size)
        assertEquals(listOf(1, 2, 3, 4, 5), batch)
        assertEquals(5, queue.size())
    }
    
    @Test
    fun testQueueOptimizer_BatchDequeueMoreThanAvailable() {
        val queue = QueueOptimizer<Int>()
        
        queue.enqueue(1)
        queue.enqueue(2)
        
        val batch = queue.dequeueBatch(10)
        
        assertEquals(2, batch.size)
        assertTrue(queue.isEmpty())
    }
}
