package com.financeiro.financeiro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "account_settings")
data class AccountSettingsEntity(
    @PrimaryKey val accountId: Long,
    val expenseCategories: String,
    val incomeCategories: String,
    val paymentMethods: String,
    val paymentMethodCardConfigs: String,
    val expenseCategoryLimits: String,
    val updatedAt: Long
)
