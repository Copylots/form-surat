package com.example.data

import kotlinx.coroutines.flow.Flow

class LetterDraftRepository(private val letterDraftDao: LetterDraftDao) {
    val allDrafts: Flow<List<LetterDraft>> = letterDraftDao.getAllDrafts()

    suspend fun getDraftById(id: Int): LetterDraft? {
        return letterDraftDao.getDraftById(id)
    }

    suspend fun insertDraft(draft: LetterDraft): Long {
        return letterDraftDao.insertDraft(draft)
    }

    suspend fun deleteDraftById(id: Int) {
        letterDraftDao.deleteDraftById(id)
    }
}
