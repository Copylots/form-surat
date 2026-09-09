package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LetterDraftDao {
    @Query("SELECT * FROM letter_drafts ORDER BY lastModified DESC")
    fun getAllDrafts(): Flow<List<LetterDraft>>

    @Query("SELECT * FROM letter_drafts WHERE id = :id LIMIT 1")
    suspend fun getDraftById(id: Int): LetterDraft?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDraft(draft: LetterDraft): Long

    @Query("DELETE FROM letter_drafts WHERE id = :id")
    suspend fun deleteDraftById(id: Int)
}
