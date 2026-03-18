package com.financeiro.financeiro.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class FinancialTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val paymentMethod: String,
    val installments: Int,
    val installmentNumber: Int,
    val originalTotalAmount: Double,
    val cardPaymentDateMillis: Long?,
    val notes: String,
    val dateMillis: Long
)
