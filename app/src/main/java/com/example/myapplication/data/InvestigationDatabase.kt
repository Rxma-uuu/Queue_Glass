package com.example.myapplication.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedInvestigation::class, UserAccount::class],
    version = 2,
    exportSchema = false
)
abstract class InvestigationDatabase : RoomDatabase() {

    abstract fun investigationDao(): InvestigationDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: InvestigationDatabase? = null

        fun get(context: Context): InvestigationDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    InvestigationDatabase::class.java,
                    "queglass_investigations.db",
                )
                .fallbackToDestructiveMigration(true)
                .build()
                .also { INSTANCE = it }
            }
    }
}
