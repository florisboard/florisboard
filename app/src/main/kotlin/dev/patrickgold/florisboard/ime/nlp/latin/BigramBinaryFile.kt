/*
 * Copyright (C) 2022-2025 The FlorisBoard Contributors
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

package dev.patrickgold.florisboard.ime.nlp.latin

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

class BigramBinaryFile private constructor(private val buffer: ByteBuffer) {
    
    companion object {
        private const val MAGIC = 0x42494752
        private const val HEADER_SIZE = 16
        private const val FILE_NAME = "bigrams.bin"
        
        @Volatile
        private var INSTANCE: BigramBinaryFile? = null

        private fun reset() {
            INSTANCE = null
        }
        
        fun getInstance(context: Context): BigramBinaryFile {
            INSTANCE?.let { instance ->
                if (instance.indexCount > 0) {
                    return instance
                } else {
                    reset()
                }
            }

            return INSTANCE ?: synchronized(this) {
                val instance = loadFromAssets(context)
                INSTANCE = instance
                instance
            }
        }
        
        private fun loadFromAssets(context: Context): BigramBinaryFile {
            val cacheFile = File(context.cacheDir, FILE_NAME)
            
            if (!cacheFile.exists()) {
                context.assets.open("ime/dict/$FILE_NAME").use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            
            val channel = FileInputStream(cacheFile).channel
            val buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, cacheFile.length())
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            
            val magic = buffer.getInt(0)
            require(magic == MAGIC) { "Invalid bigram file magic: $magic" }
            
            return BigramBinaryFile(buffer)
        }
    }

    private val indexOffset: Int = buffer.getInt(8)
    private val indexCount: Int = buffer.getInt(12)
    private val indexPositions: IntArray

    init {
        indexPositions = IntArray(indexCount)
        var pos = indexOffset
        for (i in 0 until indexCount) {
            indexPositions[i] = pos
            val wordLen = buffer.getShort(pos).toInt() and 0xFFFF
            pos += 2 + wordLen + 4 + 4
        }
    }

    private val version: Int = buffer.getInt(4)
    
    fun getBigrams(word1: String): Map<String, Double> {
        val indexPos = binarySearchIndex(word1)
        if (indexPos < 0) return emptyMap()

        val wordLen = getWordLength(indexPos)
        
        val dataOffset = buffer.getInt(indexPos + 2 + wordLen)
        val count = buffer.getInt(indexPos + 2 + wordLen + 4)
        
        val result = mutableMapOf<String, Double>()
        var pos = HEADER_SIZE + dataOffset
        
        repeat(count) {
            val word2Len = buffer.getShort(pos).toInt() and 0xFFFF
            pos += 2
            
            val word2Bytes = ByteArray(word2Len)
            buffer.position(pos)
            buffer.get(word2Bytes)
            val word2 = String(word2Bytes, Charsets.UTF_8)
            pos += word2Len
            
            val probability = buffer.getFloat(pos).toDouble()
            pos += 4
            
            result[word2] = probability
        }
        
        return result
    }
    
    private fun binarySearchIndex(target: String): Int {
        val targetBytes = target.toByteArray(Charsets.UTF_8)
        var low = 0
        var high = indexCount - 1
    
        while (low <= high) {
            val mid = (low + high) ushr 1
            val indexEntryPos = indexPositions[mid]
        
            val word1Len = buffer.getShort(indexEntryPos).toInt() and 0xFFFF
        
            var cmp = 0
            var i = 0
            while (i < word1Len && i < targetBytes.size && cmp == 0) {
                val bufByte = (buffer.get(indexEntryPos + 2 + i).toInt() and 0xFF)
                val tgtByte = (targetBytes[i].toInt() and 0xFF)
                cmp = bufByte - tgtByte
                i++
            }
        
            if (cmp == 0) {
                cmp = word1Len - targetBytes.size
            }
        
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return indexEntryPos
            }
        }
    return -1
}
    
    private fun getWordLength(indexEntryPos: Int): Int {
        return buffer.getShort(indexEntryPos).toInt() and 0xFFFF
    }
}
