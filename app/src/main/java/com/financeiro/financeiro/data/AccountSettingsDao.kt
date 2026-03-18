package com.financeiro.financeiro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountSettingsDao {
    @Query("SELECT * FROM account_settings WHERE accountId = :accountId LIMIT 1")
    fun observeByAccount(accountId: Long): Flow<AccountSettingsEntity?>

    @Query("SELECT * FROM account_settings WHERE accountId = :accountId LIMIT 1")
    suspend fun findByAccount(accountId: Long): AccountSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: AccountSettingsEntity)

    @Query("DELETE FROM account_settings WHERE accountId = :accountId")
    suspend fun deleteByAccount(accountId: Long)
}
