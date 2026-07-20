package com.cortex.app.data.repository

import com.cortex.app.data.db.dao.MemoryDao
import com.cortex.app.data.mapper.toDomain
import com.cortex.app.data.mapper.toEntity
import com.cortex.app.domain.model.MemoryItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class MemoryRepositoryImpl(private val dao: MemoryDao) : MemoryRepository {

    override fun observeActive(): Flow<List<MemoryItem>> =
        dao.observeActive().map { list -> list.map { it.toDomain() } }

    override fun observePinned(): Flow<List<MemoryItem>> =
        dao.observePinned().map { list -> list.map { it.toDomain() } }

    override fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()

    override suspend fun getById(id: String): MemoryItem? = dao.getById(id)?.toDomain()

    override suspend fun save(item: MemoryItem) = dao.insert(item.toEntity())

    override suspend fun searchByKeyword(query: String, limit: Int): List<MemoryItem> =
        dao.searchByKeyword(query, limit).map { it.toDomain() }

    override suspend fun getRecent(limit: Int): List<MemoryItem> =
        dao.getRecent(limit).map { it.toDomain() }

    override suspend fun archive(id: String) = dao.archive(id, Instant.now().toEpochMilli())

    override suspend fun setPinned(id: String, pinned: Boolean) =
        dao.setPinned(id, pinned, Instant.now().toEpochMilli())
}
