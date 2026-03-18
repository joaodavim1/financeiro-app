package com.financeiro.financeiro.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    suspend fun listAll(): List<FinancialTransactionEntity>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateMillis DESC, id DESC")
    fun observeByAccount(accountId: Long): Flow<List<FinancialTransactionEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY dateMillis DESC, id DESC")
    suspend fun listByAccount(accountId: Long): List<FinancialTransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE dateMillis BETWEEN :startMillis AND :endMillis")
    suspend fun countByDateRange(startMillis: Long, endMillis: Long): Int

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE accountId = :accountId
          AND type = 'DESPESA'
          AND dateMillis BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun sumExpenseByAccountAndRange(accountId: Long, startMillis: Long, endMillis: Long): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0)
        FROM transactions
        WHERE accountId = :accountId
          AND type = 'DESPESA'
          AND category = :category
          AND dateMillis BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun sumExpenseByAccountCategoryAndRange(
        accountId: Long,
        category: String,
        startMillis: Long,
        endMillis: Long
    ): Double

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FinancialTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<FinancialTransactionEntity>)

    @Update
    suspend fun update(item: FinancialTransactionEntity)

    @Delete
    suspend fun delete(item: FinancialTransactionEntity)

    @Query("UPDATE transactions SET accountId = :newAccountId WHERE accountId = :oldAccountId")
    suspend fun reassignAccount(oldAccountId: Long, newAccountId: Long)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
