/*
 * Performance Optimization Module
 * Copyright (C) 2025 The FlorisBoard Contributors
 * 
 * Implements performance optimizations including:
 * - Memory management and GC pressure reduction
 * - Matrix-based variable management
 * - Queue and latency optimization
 * - Dependency minimization
 * - Caching strategies
 * 
 * Standards Compliance: ISO 25010 (Software Quality)
 */

package org.florisboard.lib.zipraf

import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance optimizer for reducing GC pressure and improving throughput
 * 
 * Key optimizations:
 * 1. Object pooling to reduce allocations
 * 2. Matrix-based data structures for cache efficiency
 * 3. Weak references for memory-sensitive caching
 * 4. Lock-free data structures where possible
 * 5. Batch processing to reduce per-operation overhead
 */
class PerformanceOptimizer {
    companion object {
        // Singleton instance for global optimization
        @Volatile
        private var instance: PerformanceOptimizer? = null
        
        fun getInstance(): PerformanceOptimizer {
            return instance ?: synchronized(this) {
                instance ?: PerformanceOptimizer().also { instance = it }
            }
        }
    }
    
    // Performance metrics (lock-free)
    private val operationCount = AtomicLong(0)
    private val cacheHits = AtomicLong(0)
    private val cacheMisses = AtomicLong(0)
    
    // Cache with automatic eviction (weak references)
    private val cache = ConcurrentHashMap<String, WeakReference<Any>>()
    
    // Matrix pool for reuse (reduces allocations)
    private val matrixPool = MatrixPool()
    
    /**
     * Gets cached value or computes if not present
     * Uses weak references to allow GC when memory is needed
     * 
     * @param key Cache key
     * @param compute Function to compute value if not cached
     * @return Cached or computed value
     */
    fun <T : Any> getOrCompute(key: String, compute: () -> T): T {
        operationCount.incrementAndGet()
        
        // Try to get from cache
        cache[key]?.get()?.let { cached ->
            cacheHits.incrementAndGet()
            @Suppress("UNCHECKED_CAST")
            return cached as T
        }
        
        // Cache miss - compute and store
        cacheMisses.incrementAndGet()
        val value = compute()
        cache[key] = WeakReference(value)
        return value
    }
    
    /**
     * Clears cache entries
     * Useful for memory pressure situations
     */
    fun clearCache() {
        cache.clear()
    }
    
    /**
     * Gets performance statistics
     * 
     * @return Performance metrics
     */
    fun getMetrics(): PerformanceMetrics {
        val ops = operationCount.get()
        val hits = cacheHits.get()
        val misses = cacheMisses.get()
        
        return PerformanceMetrics(
            totalOperations = ops,
            cacheHits = hits,
            cacheMisses = misses,
            hitRate = if (ops > 0) hits.toDouble() / ops else 0.0
        )
    }
    
    /**
     * Acquires a matrix from the pool
     * Reduces allocation overhead for matrix operations
     * 
     * @param rows Number of rows
     * @param cols Number of columns
     * @return Pooled matrix
     */
    fun acquireMatrix(rows: Int, cols: Int): Matrix {
        return matrixPool.acquire(rows, cols)
    }
    
    /**
     * Returns a matrix to the pool
     * 
     * @param matrix Matrix to return
     */
    fun releaseMatrix(matrix: Matrix) {
        matrixPool.release(matrix)
    }
}

/**
 * Performance metrics data class
 */
data class PerformanceMetrics(
    val totalOperations: Long,
    val cacheHits: Long,
    val cacheMisses: Long,
    val hitRate: Double
)

/**
 * Matrix data structure for efficient numerical operations
 * Uses flat array internally for cache-friendly access
 * 
 * Implements matrix-based variable management as required
 */
class Matrix(val rows: Int, val cols: Int) {
    // Flat array for cache efficiency (row-major order)
    private val data = DoubleArray(rows * cols)
    
    /**
     * Gets value at position (row, col)
     * 
     * @param row Row index
     * @param col Column index
     * @return Value at position
     */
    operator fun get(row: Int, col: Int): Double {
        require(row in 0 until rows && col in 0 until cols) {
            "Index out of bounds: ($row, $col)"
        }
        return data[row * cols + col]
    }
    
    /**
     * Sets value at position (row, col)
     * 
     * @param row Row index
     * @param col Column index
     * @param value Value to set
     */
    operator fun set(row: Int, col: Int, value: Double) {
        require(row in 0 until rows && col in 0 until cols) {
            "Index out of bounds: ($row, $col)"
        }
        data[row * cols + col] = value
    }
    
    /**
     * Fills matrix with value
     * 
     * @param value Fill value
     */
    fun fill(value: Double) {
        data.fill(value)
    }
    
    /**
     * Resets matrix to zeros
     */
    fun reset() {
        fill(0.0)
    }
    
    /**
     * Matrix multiplication
     * Optimized for cache efficiency
     * 
     * @param other Matrix to multiply with
     * @return Result matrix
     */
    fun multiply(other: Matrix): Matrix {
        require(cols == other.rows) {
            "Matrix dimensions incompatible for multiplication"
        }
        
        val result = Matrix(rows, other.cols)
        
        // Cache-friendly multiplication (iterate in row-major order)
        for (i in 0 until rows) {
            for (j in 0 until other.cols) {
                var sum = 0.0
                for (k in 0 until cols) {
                    sum += this[i, k] * other[k, j]
                }
                result[i, j] = sum
            }
        }
        
        return result
    }
    
    /**
     * Adds two matrices
     * 
     * @param other Matrix to add
     * @return Result matrix
     */
    fun add(other: Matrix): Matrix {
        require(rows == other.rows && cols == other.cols) {
            "Matrix dimensions must match for addition"
        }
        
        val result = Matrix(rows, cols)
        for (i in data.indices) {
            result.data[i] = data[i] + other.data[i]
        }
        
        return result
    }
}

/**
 * Matrix pool for reusing matrix objects
 * Reduces allocation overhead and GC pressure
 */
class MatrixPool {
    private val pools = ConcurrentHashMap<String, ArrayDeque<Matrix>>()
    private val poolSizes = ConcurrentHashMap<String, AtomicInteger>()
    
    // Maximum matrices to keep in pool per size
    private val maxPoolSize = 16
    
    /**
     * Gets key for pool lookup
     */
    private fun getKey(rows: Int, cols: Int): String = "${rows}x${cols}"
    
    /**
     * Acquires matrix from pool or creates new one
     * 
     * @param rows Number of rows
     * @param cols Number of columns
     * @return Matrix instance
     */
    fun acquire(rows: Int, cols: Int): Matrix {
        val key = getKey(rows, cols)
        val pool = pools.getOrPut(key) { ArrayDeque() }
        
        synchronized(pool) {
            return if (pool.isNotEmpty()) {
                pool.removeFirst().also { it.reset() }
            } else {
                Matrix(rows, cols)
            }
        }
    }
    
    /**
     * Returns matrix to pool
     * 
     * @param matrix Matrix to return
     */
    fun release(matrix: Matrix) {
        val key = getKey(matrix.rows, matrix.cols)
        val pool = pools.getOrPut(key) { ArrayDeque() }
        val counter = poolSizes.getOrPut(key) { AtomicInteger(0) }
        
        synchronized(pool) {
            // Only keep up to maxPoolSize matrices
            if (counter.get() < maxPoolSize) {
                matrix.reset()
                pool.addLast(matrix)
                counter.incrementAndGet()
            }
        }
    }
    
    /**
     * Clears all pools
     */
    fun clear() {
        pools.clear()
        poolSizes.clear()
    }
}

/**
 * Queue optimizer for reducing latency
 * Uses concurrent data structures for lock-free operations
 * 
 * Note: This uses java.util.concurrent.ConcurrentLinkedQueue for
 * better performance in high-concurrency scenarios. For single-threaded
 * use cases, ArrayDeque with synchronization may be more efficient.
 */
class QueueOptimizer<T> {
    // Use ConcurrentLinkedQueue for lock-free operations
    private val queue = java.util.concurrent.ConcurrentLinkedQueue<T>()
    private val sizeCounter = java.util.concurrent.atomic.AtomicInteger(0)
    
    /**
     * Enqueues item with lock-free operation
     * 
     * @param item Item to enqueue
     */
    fun enqueue(item: T) {
        queue.offer(item)
        sizeCounter.incrementAndGet()
    }
    
    /**
     * Dequeues item with lock-free operation
     * 
     * @return Item or null if empty
     */
    fun dequeue(): T? {
        val item = queue.poll()
        if (item != null) {
            sizeCounter.decrementAndGet()
        }
        return item
    }
    
    /**
     * Batch dequeue for reduced overhead
     * 
     * @param maxItems Maximum items to dequeue
     * @return List of dequeued items
     */
    fun dequeueBatch(maxItems: Int): List<T> {
        val result = mutableListOf<T>()
        var count = 0
        while (count < maxItems) {
            val item = queue.poll() ?: break
            result.add(item)
            sizeCounter.decrementAndGet()
            count++
        }
        return result
    }
    
    /**
     * Gets queue size (approximate for concurrent access)
     * 
     * @return Current size (may be approximate due to concurrent modifications)
     */
    fun size(): Int {
        return sizeCounter.get()
    }
    
    /**
     * Checks if queue is empty
     * 
     * @return true if empty (may change immediately due to concurrent access)
     */
    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }
}
