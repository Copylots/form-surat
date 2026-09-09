package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "letter_drafts")
data class LetterDraft(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateName: String,
    val header: String = "",
    val title: String = "",
    val nomor: String = "",
    val lampiran: String = "",
    val perihal: String = "",
    val alignment: String = "Justify",
    val body: String = "",
    val ketuaNama: String = "",
    val sekretarisNama: String = "",
    val usePj: Boolean = true,
    val pjNama: String = "",
    val footerText: String = "",
    val logoPath: String? = null,
    val sigKetuaPath: String? = null,
    val sigSekretarisPath: String? = null,
    val sigPjPath: String? = null,
    val lastModified: Long = System.currentTimeMillis()
)
