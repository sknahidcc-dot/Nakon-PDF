package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NakonApp
import com.example.data.PdfRepository
import com.example.data.model.PdfDocumentItem
import com.example.engine.PdfEngine
import com.example.model.CapturedPage
import com.example.model.ScanFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PdfRepository = (application as NakonApp).repository

    val allPdfs: StateFlow<List<PdfDocumentItem>> = repository.allPdfs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _capturedPages = MutableStateFlow<List<CapturedPage>>(emptyList())
    val capturedPages: StateFlow<List<CapturedPage>> = _capturedPages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(Pair(0, 0))
    val generationProgress: StateFlow<Pair<Int, Int>> = _generationProgress.asStateFlow()

    private val _globalFilter = MutableStateFlow(ScanFilter.BW_DOCUMENT)
    val globalFilter: StateFlow<ScanFilter> = _globalFilter.asStateFlow()

    fun addPage(uri: Uri) {
        val currentList = _capturedPages.value.toMutableList()
        val newPage = CapturedPage(
            imageUri = uri,
            rotationDegrees = 0,
            filter = _globalFilter.value,
            pageNumber = currentList.size + 1
        )
        currentList.add(newPage)
        _capturedPages.value = currentList
    }

    fun addMultiplePages(uris: List<Uri>) {
        val currentList = _capturedPages.value.toMutableList()
        for (uri in uris) {
            currentList.add(
                CapturedPage(
                    imageUri = uri,
                    rotationDegrees = 0,
                    filter = _globalFilter.value,
                    pageNumber = currentList.size + 1
                )
            )
        }
        _capturedPages.value = currentList
    }

    fun removePage(pageId: String) {
        val updated = _capturedPages.value.filter { it.id != pageId }
            .mapIndexed { idx, page -> page.copy(pageNumber = idx + 1) }
        _capturedPages.value = updated
    }

    fun rotatePage(pageId: String) {
        _capturedPages.value = _capturedPages.value.map { page ->
            if (page.id == pageId) {
                page.copy(rotationDegrees = (page.rotationDegrees + 90) % 360)
            } else {
                page
            }
        }
    }

    fun movePage(fromIndex: Int, toIndex: Int) {
        val list = _capturedPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _capturedPages.value = list.mapIndexed { index, p -> p.copy(pageNumber = index + 1) }
        }
    }

    fun setPageFilter(pageId: String, filter: ScanFilter) {
        _capturedPages.value = _capturedPages.value.map { page ->
            if (page.id == pageId) {
                page.copy(filter = filter)
            } else {
                page
            }
        }
    }

    fun setGlobalFilter(filter: ScanFilter) {
        _globalFilter.value = filter
        _capturedPages.value = _capturedPages.value.map { it.copy(filter = filter) }
    }

    fun clearPages() {
        _capturedPages.value = emptyList()
    }

    fun generateDefaultTitle(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "Nakon_Scan_$timestamp"
    }

    fun createPdf(
        title: String,
        password: String?,
        onComplete: (PdfDocumentItem?) -> Unit
    ) {
        val pages = _capturedPages.value
        if (pages.isEmpty()) {
            onComplete(null)
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationProgress.value = Pair(0, pages.size)

            val safeTitle = title.ifBlank { generateDefaultTitle() }
            val targetFile = repository.createDestinationFile(safeTitle)

            val result = PdfEngine.createPdf(
                context = getApplication(),
                pages = pages,
                outputFile = targetFile,
                password = if (password.isNullOrBlank()) null else password.trim(),
                onProgress = { current, total ->
                    _generationProgress.value = Pair(current, total)
                }
            )

            if (result.success && result.outputFile != null) {
                val insertedId = repository.savePdfRecord(
                    title = safeTitle,
                    file = result.outputFile,
                    pageCount = result.pageCount,
                    isEncrypted = !password.isNullOrBlank(),
                    thumbnailFile = result.thumbnailFile,
                    passwordHint = if (!password.isNullOrBlank()) "Password protected" else null
                )
                val createdItem = repository.getPdfByIdSync(insertedId)
                _isGenerating.value = false
                clearPages()
                onComplete(createdItem)
            } else {
                _isGenerating.value = false
                onComplete(null)
            }
        }
    }

    fun deletePdf(id: Long, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.deletePdf(id)
            onDone(ok)
        }
    }

    fun renamePdf(id: Long, newTitle: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.renamePdf(id, newTitle)
            onDone(ok)
        }
    }

    fun lockPdf(id: Long, password: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.lockExistingPdf(id, password)
            onDone(ok)
        }
    }

    fun unlockPdf(id: Long, password: String, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = repository.unlockExistingPdf(id, password)
            onDone(ok)
        }
    }

    fun sharePdf(file: File) {
        repository.sharePdf(file)
    }

    fun openExternal(file: File) {
        repository.openExternal(file)
    }

    fun formatFileSize(bytes: Long): String = repository.formatFileSize(bytes)
    fun formatDate(timestamp: Long): String = repository.formatDate(timestamp)
}
