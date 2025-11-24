# Performance Optimization Best Practices

## Overview

This document outlines performance optimization strategies for FlorisBoard, focusing on reducing memory footprint, minimizing garbage collection, reducing latency, and improving responsiveness.

## Memory Management

### Object Allocation Strategies

#### Reuse Objects
```kotlin
// Bad: Creates new object on every call
fun formatText(text: String): String {
    return StringBuilder().append(prefix).append(text).toString()
}

// Good: Reuse StringBuilder
private val stringBuilder = StringBuilder()

fun formatText(text: String): String {
    stringBuilder.clear()
    stringBuilder.append(prefix).append(text)
    return stringBuilder.toString()
}
```

#### Object Pools
```kotlin
// For frequently created/destroyed objects
class KeyEventPool(private val maxSize: Int = 10) {
    private val pool = ArrayDeque<KeyEvent>(maxSize)
    
    fun obtain(): KeyEvent {
        return pool.removeFirstOrNull() ?: KeyEvent()
    }
    
    fun recycle(event: KeyEvent) {
        event.reset()
        if (pool.size < maxSize) {
            pool.addLast(event)
        }
    }
}
```

#### Avoid Unnecessary Boxing
```kotlin
// Bad: Boxing primitives
val numbers: List<Integer> = listOf(1, 2, 3)

// Good: Use primitive arrays when possible
val numbers: IntArray = intArrayOf(1, 2, 3)
```

### Memory Leaks Prevention

#### Lifecycle-Aware Components
```kotlin
class KeyboardService : LifecycleService() {
    private val observers = mutableListOf<LifecycleObserver>()
    
    override fun onDestroy() {
        // Clean up observers
        observers.forEach { lifecycle.removeObserver(it) }
        observers.clear()
        super.onDestroy()
    }
}
```

#### WeakReferences for Caches
```kotlin
class BitmapCache {
    private val cache = mutableMapOf<String, WeakReference<Bitmap>>()
    
    fun get(key: String): Bitmap? {
        return cache[key]?.get()?.also { bitmap ->
            if (bitmap.isRecycled) {
                cache.remove(key)
                return null
            }
        }
    }
}
```

### Garbage Collection Optimization

#### Minimize Allocations in Hot Paths
```kotlin
// Bad: Allocates on every key press
fun onKeyPress(keyCode: Int) {
    val event = KeyEvent(keyCode, System.currentTimeMillis())
    processEvent(event)
}

// Good: Reuse event object
private val reusableEvent = KeyEvent()

fun onKeyPress(keyCode: Int) {
    reusableEvent.update(keyCode, System.currentTimeMillis())
    processEvent(reusableEvent)
}
```

#### Use Sequences for Lazy Evaluation
```kotlin
// Bad: Creates intermediate collections
val result = list
    .filter { it.isValid() }
    .map { it.transform() }
    .take(10)

// Good: Lazy evaluation with sequences
val result = list.asSequence()
    .filter { it.isValid() }
    .map { it.transform() }
    .take(10)
    .toList()
```

#### Avoid String Concatenation in Loops
```kotlin
// Bad: Creates many temporary strings
var result = ""
for (item in items) {
    result += item.toString()
}

// Good: Use StringBuilder
val result = buildString {
    items.forEach { append(it) }
}
```

## Latency Reduction

### UI Thread Optimization

#### Keep UI Thread Free
```kotlin
// Bad: Heavy computation on UI thread
fun loadDictionary() {
    val words = parseDictionaryFile() // Heavy operation
    updateUI(words)
}

// Good: Use coroutines
suspend fun loadDictionary() = withContext(Dispatchers.IO) {
    val words = parseDictionaryFile()
    withContext(Dispatchers.Main) {
        updateUI(words)
    }
}
```

#### Batch UI Updates
```kotlin
// Bad: Update UI for each item
items.forEach { item ->
    addViewForItem(item)
}

// Good: Batch updates
val views = items.map { createViewForItem(it) }
addAllViews(views)
```

### Input Latency

#### Direct Event Handling
```kotlin
// Minimize time from touch to display
override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            // Handle immediately, defer heavy work
            handleTouchDown(event)
            return true
        }
    }
    return super.onTouchEvent(event)
}
```

#### Predictive Processing
```kotlin
// Start preparing next likely action
fun onKeyPress(key: Key) {
    handleKeyPress(key)
    
    // Preload next likely keys
    if (key.isPredictable()) {
        preloadLikelyNextKeys(key)
    }
}
```

### Network and I/O

#### Asynchronous Operations
```kotlin
// Always perform I/O off main thread
suspend fun loadTheme(themeId: String): Theme = withContext(Dispatchers.IO) {
    val json = fileSystem.read(themeId)
    parseTheme(json)
}
```

#### Connection Pooling
```kotlin
// Reuse connections for API calls
private val httpClient = OkHttpClient.Builder()
    .connectionPool(ConnectionPool(maxIdleConnections = 5, keepAliveDuration = 5, TimeUnit.MINUTES))
    .build()
```

## CPU Optimization

### Algorithm Efficiency

#### Use Appropriate Data Structures
```kotlin
// Bad: O(n) lookup
val list = listOf<String>()
if (item in list) { ... }

// Good: O(1) lookup
val set = setOf<String>()
if (item in set) { ... }
```

#### Cache Expensive Computations
```kotlin
class LayoutCalculator {
    private val cache = LruCache<String, Layout>(maxSize = 100)
    
    fun calculateLayout(params: LayoutParams): Layout {
        val key = params.hashCode().toString()
        return cache.get(key) ?: run {
            val layout = computeLayout(params)
            cache.put(key, layout)
            layout
        }
    }
}
```

### Parallel Processing

#### Use Coroutines for Concurrency
```kotlin
suspend fun loadMultipleDictionaries(languages: List<String>) = coroutineScope {
    languages.map { language ->
        async(Dispatchers.IO) {
            loadDictionary(language)
        }
    }.awaitAll()
}
```

#### Work Distribution
```kotlin
// Distribute work across threads
fun processLargeDataset(data: List<Data>) = runBlocking {
    data.chunked(data.size / Runtime.getRuntime().availableProcessors())
        .map { chunk ->
            async(Dispatchers.Default) {
                processChunk(chunk)
            }
        }.awaitAll()
}
```

## Battery Optimization

### Reduce Wake Locks
```kotlin
// Minimize wake lock duration
fun syncData() {
    wakeLock.acquire(10 * 60 * 1000L) // 10 minutes max
    try {
        performSync()
    } finally {
        wakeLock.release()
    }
}
```

### Batch Background Work
```kotlin
// Use WorkManager for background tasks
val constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(true)
    .build()

val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
    .setConstraints(constraints)
    .build()
```

### Efficient Sensors Usage
```kotlin
// Stop sensors when not needed
override fun onPause() {
    sensorManager.unregisterListener(this)
    super.onPause()
}

override fun onResume() {
    super.onResume()
    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
}
```

## Resource Optimization

### Bitmap Management

#### Proper Scaling
```kotlin
fun loadBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
    return BitmapFactory.Options().run {
        inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, this)
        
        inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
        inJustDecodeBounds = false
        
        BitmapFactory.decodeFile(path, this)
    }
}
```

#### Recycle Bitmaps
```kotlin
class ImageCache {
    fun recycleBitmap(bitmap: Bitmap?) {
        if (bitmap != null && !bitmap.isRecycled) {
            bitmap.recycle()
        }
    }
}
```

### View Optimization

#### ViewHolder Pattern
```kotlin
class KeyboardAdapter : RecyclerView.Adapter<KeyboardAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val keyText: TextView = view.findViewById(R.id.key_text)
        // Cache view references
    }
}
```

#### Reduce View Hierarchy Depth
```kotlin
// Bad: Deep hierarchy
<LinearLayout>
    <LinearLayout>
        <LinearLayout>
            <TextView />
        </LinearLayout>
    </LinearLayout>
</LinearLayout>

// Good: Flat hierarchy with ConstraintLayout
<ConstraintLayout>
    <TextView />
</ConstraintLayout>
```

## Profiling and Monitoring

### Performance Metrics

#### Track Key Metrics
```kotlin
object PerformanceMonitor {
    fun trackInputLatency(startTime: Long, endTime: Long) {
        val latency = endTime - startTime
        if (latency > 16) { // More than one frame
            Log.w("Perf", "Input latency: ${latency}ms")
        }
    }
}
```

#### Memory Monitoring
```kotlin
fun logMemoryUsage() {
    val runtime = Runtime.getRuntime()
    val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
    Log.d("Memory", "Used: ${usedMemory}MB")
}
```

### Profiling Tools

1. **Android Profiler**: CPU, Memory, Network, Energy profiling
2. **Perfetto**: Modern system-level performance tracing (replaces Systrace)
3. **Layout Inspector**: UI hierarchy analysis
4. **LeakCanary**: Memory leak detection
5. **StrictMode**: Runtime policy violations detection

### Benchmarking

```kotlin
class KeyPressBenchmark {
    @Test
    fun benchmarkKeyPress() {
        val iterations = 1000
        val startTime = System.nanoTime()
        
        repeat(iterations) {
            simulateKeyPress()
        }
        
        val endTime = System.nanoTime()
        val avgTime = (endTime - startTime) / iterations / 1_000_000.0
        println("Average key press time: ${avgTime}ms")
    }
}
```

## Best Practices Summary

### Do's
- ✅ Profile before optimizing
- ✅ Use appropriate data structures
- ✅ Cache expensive computations
- ✅ Reuse objects in hot paths
- ✅ Perform I/O off main thread
- ✅ Batch UI updates
- ✅ Use lazy initialization
- ✅ Monitor memory usage

### Don'ts
- ❌ Premature optimization
- ❌ Allocate in tight loops
- ❌ Block UI thread
- ❌ Ignore memory leaks
- ❌ Create unnecessary objects
- ❌ Use reflection in hot paths
- ❌ Ignore profiler data
- ❌ Optimize without measuring

## References

- [Android Performance Best Practices](https://developer.android.com/topic/performance)
- [Kotlin Performance](https://kotlinlang.org/docs/performance.html)
- [Memory Management](https://developer.android.com/topic/performance/memory)
- [Reducing Overdraw](https://developer.android.com/topic/performance/rendering/overdraw)
