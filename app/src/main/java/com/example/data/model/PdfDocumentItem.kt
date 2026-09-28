package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val pageCount: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = false,
    val passwordHint: String? = null,
    val thumbnailPath: String? = null
)
