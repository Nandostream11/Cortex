package com.cortex.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connector_accounts")
data class ConnectorAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val enabled: Boolean,
    val authMode: String,
    val scopeJson: String,
    val lastSyncAt: Long?,
    val lastError: String?
)
