package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.PdfRepository
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class NakonApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: PdfRepository
        private set

    override fun onCreate() {
        super.onCreate()
        // Initialize PdfBox Android library
        PDFBoxResourceLoader.init(applicationContext)

        // Initialize Room Database
        database = AppDatabase.getDatabase(this)
        repository = PdfRepository(this, database.pdfDao())
    }
}
