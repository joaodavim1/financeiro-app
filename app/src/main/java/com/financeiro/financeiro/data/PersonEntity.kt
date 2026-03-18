package com.financeiro.financeiro.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "people",
    indices = [
        Index(value = ["phone"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String?,
    val email: String?,
    val isActive: Boolean,
    val updatedAt: Long
)
