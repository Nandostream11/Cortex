package com.cortex.app.data.repository

import com.cortex.app.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    fun observeActive(): Flow<List<MemoryItem>>
    fun observePinned(): Flow<List<MemoryItem>>
    fun observeActiveCount(): Flow<Int>
    suspend fun getById(id: String): MemoryItem?
    suspend fun save(item: MemoryItem)
    suspend fun searchByKeyword(query: String, limit: Int = 50): List<MemoryItem>
    suspend fun getRecent(limit: Int = 20): List<MemoryItem>
    suspend fun archive(id: String)
    suspend fun setPinned(id: String, pinned: Boolean)
}
