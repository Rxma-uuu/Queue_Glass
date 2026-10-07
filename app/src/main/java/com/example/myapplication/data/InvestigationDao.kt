package com.example.myapplication.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestigationDao {

    @Query("SELECT * FROM investigations ORDER BY savedAtMs DESC")
    fun observeAll(): Flow<List<SavedInvestigation>>

    @Query("SELECT * FROM investigations WHERE id = :id")
    suspend fun getById(id: Long): SavedInvestigation?

    @Insert
    suspend fun insert(investigation: SavedInvestigation): Long

    @Query("DELETE FROM investigations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM investigations")
    suspend fun deleteAll()
}
