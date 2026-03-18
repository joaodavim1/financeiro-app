package com.financeiro.financeiro.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDao {
    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC, id ASC")
    suspend fun listAll(): List<PersonEntity>

    @Query("SELECT * FROM people ORDER BY name COLLATE NOCASE ASC, id ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE isActive = 1 ORDER BY updatedAt DESC, id DESC LIMIT 1")
    fun observeActive(): Flow<PersonEntity?>

    @Query("SELECT * FROM people WHERE isActive = 1 ORDER BY updatedAt DESC, id DESC LIMIT 1")
    suspend fun findActive(): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE LOWER(name) = LOWER(:name) ORDER BY id ASC LIMIT 1")
    suspend fun findByName(name: String): PersonEntity?

    @Query(
        """
        SELECT * FROM people
        WHERE LOWER(name) = 'cadastro' OR LOWER(name) = 'conta principal'
        ORDER BY id ASC
        """
    )
    suspend fun findDefaultAccounts(): List<PersonEntity>

    @Query(
        """
        SELECT * FROM people
        WHERE (:phone <> '' AND phone = :phone)
           OR (:email <> '' AND LOWER(email) = LOWER(:email))
           OR (:name <> '' AND LOWER(name) = LOWER(:name))
        ORDER BY updatedAt DESC, id DESC
        LIMIT 1
        """
    )
    suspend fun findByAny(name: String, phone: String, email: String): PersonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: PersonEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PersonEntity>)

    @Update
    suspend fun update(item: PersonEntity)

    @Query("UPDATE people SET isActive = 0")
    suspend fun clearActive()

    @Query("DELETE FROM people WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM people")
    suspend fun deleteAll()
}
