package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PdfDocumentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDao {
    @Query("SELECT * FROM pdf_documents ORDER BY createdAt DESC")
    fun getAllPdfs(): Flow<List<PdfDocumentItem>>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    fun getPdfById(id: Long): Flow<PdfDocumentItem?>

    @Query("SELECT * FROM pdf_documents WHERE id = :id LIMIT 1")
    suspend fun getPdfByIdSync(id: Long): PdfDocumentItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdf(pdf: PdfDocumentItem): Long

    @Update
    suspend fun updatePdf(pdf: PdfDocumentItem)

    @Delete
    suspend fun deletePdf(pdf: PdfDocumentItem)

    @Query("DELETE FROM pdf_documents WHERE id = :id")
    suspend fun deletePdfById(id: Long)

    @Query("UPDATE pdf_documents SET title = :newTitle WHERE id = :id")
    suspend fun renamePdf(id: Long, newTitle: String)

    @Query("UPDATE pdf_documents SET isEncrypted = :isEncrypted WHERE id = :id")
    suspend fun updateEncryptionStatus(id: Long, isEncrypted: Boolean)
}
