package com.example.model

import android.net.Uri

enum class ScanFilter(val displayName: String, val description: String) {
    BW_DOCUMENT("B&W Scan", "সাদা ব্যাকগ্রাউন্ড ও কালো লেখা"),
    MAGIC_COLOR("Magic Color", "উজ্জ্বল রঙিন ডকুমেন্ট"),
    GRAYSCALE("Grayscale", "মসৃণ ধূসর স্ক্যান"),
    ORIGINAL("Original", "আসল ছবি")
}

enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE,
    AUTO
}

data class CapturedPage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val imageUri: Uri,
    val rotationDegrees: Int = 0,
    val filter: ScanFilter = ScanFilter.BW_DOCUMENT,
    val pageNumber: Int = 1
)
