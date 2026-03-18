package com.financeiro.financeiro.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [FinancialTransactionEntity::class, PersonEntity::class, AccountSettingsEntity::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(TransactionTypeConverter::class)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun personDao(): PersonDao
    abstract fun accountSettingsDao(): AccountSettingsDao

    companion object {
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `account_settings` (
                        `accountId` INTEGER NOT NULL,
                        `expenseCategories` TEXT NOT NULL,
                        `incomeCategories` TEXT NOT NULL,
                        `paymentMethods` TEXT NOT NULL,
                        `paymentMethodCardConfigs` TEXT NOT NULL,
                        `expenseCategoryLimits` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`accountId`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: FinanceDatabase? = null

        fun getInstance(context: Context): FinanceDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FinanceDatabase::class.java,
                    "financeiro.db"
                ).addMigrations(MIGRATION_7_8)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
