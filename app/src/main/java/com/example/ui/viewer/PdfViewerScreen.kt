package com.example.ui.viewer

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PdfDocumentItem
import com.example.engine.PdfEngine
import com.example.ui.viewmodel.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfId: Long,
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val allPdfs by viewModel.allPdfs.collectAsStateWithLifecycle()
    val pdfItem = remember(allPdfs, pdfId) { allPdfs.find { it.id == pdfId } }

    var passwordToUse by remember { mutableStateOf<String?>(null) }
    var isPasswordPromptRequired by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var pageCount by remember { mutableIntStateOf(0) }
    var isLoadingPages by remember { mutableStateOf(true) }
    val renderedPages = remember { mutableStateMapOf<Int, Bitmap>() }

    // Lock/Unlock dialogs
    var showLockDialog by remember { mutableStateOf(false) }
    var newLockPassword by remember { mutableStateOf("") }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var unlockPasswordInput by remember { mutableStateOf("") }

    BackHandler {
        onNavigateBack()
    }

    fun loadPdfPages(file: File, pass: String?) {
        coroutineScope.launch {
            isLoadingPages = true
            renderedPages.clear()

            val count = PdfEngine.getPageCount(file, pass)
            if (count == -1) {
                // Password required / invalid password
                isPasswordPromptRequired = true
                isLoadingPages = false
                return@launch
            }

            pageCount = count
            isPasswordPromptRequired = false
            passwordError = null

            // Render first page immediately for instant display
            if (count > 0) {
                val p0 = PdfEngine.renderPageToBitmap(context, file, 0, pass, 1080)
                if (p0 != null) {
                    renderedPages[0] = p0
                }
            }
            isLoadingPages = false

            // Render remaining pages in background
            withContext(Dispatchers.IO) {
                for (i in 1 until count) {
                    val bmp = PdfEngine.renderPageToBitmap(context, file, i, pass, 1080)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            renderedPages[i] = bmp
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(pdfItem?.filePath, passwordToUse) {
        if (pdfItem != null) {
            val file = File(pdfItem.filePath)
            if (file.exists()) {
                val isEncrypted = PdfEngine.isPdfEncrypted(file)
                if (isEncrypted && passwordToUse == null) {
                    isPasswordPromptRequired = true
                    isLoadingPages = false
                } else {
                    loadPdfPages(file, passwordToUse)
                }
            }
        }
    }

    val listState = rememberLazyListState()
    val currentPageIndex by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex + 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pdfItem?.title ?: "পিডিএফ ভিউয়ার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (pdfItem != null) {
                            Text(
                                text = "${viewModel.formatFileSize(pdfItem.fileSizeBytes)} • ${if (pdfItem.isEncrypted) "🔐 লকড" else "আনলকড"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Lock or Unlock Button
                    if (pdfItem != null) {
                        if (pdfItem.isEncrypted) {
                            IconButton(onClick = { showUnlockDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.LockOpen,
                                    contentDescription = "Unlock PDF",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        } else {
                            IconButton(onClick = { showLockDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock PDF",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Share
                        IconButton(onClick = { viewModel.sharePdf(File(pdfItem.filePath)) }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share PDF",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Open with external
                        IconButton(onClick = { viewModel.openExternal(File(pdfItem.filePath)) }) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open Externally",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE2E8F0)) // Neutral document paper reader backdrop
        ) {
            when {
                isPasswordPromptRequired -> {
                    // Password Required Card
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "এই PDF ফাইলটি লক করা আছে",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )

                                Text(
                                    text = "ডকুমেন্টটি দেখার জন্য দয়া করে সঠিক পাসওয়ার্ড দিন:",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = {
                                        passwordInput = it
                                        passwordError = null
                                    },
                                    label = { Text("পাসওয়ার্ড") },
                                    isError = passwordError != null,
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                passwordError?.let { err ->
                                    Text(text = err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        if (passwordInput.isNotBlank() && pdfItem != null) {
                                            val file = File(pdfItem.filePath)
                                            coroutineScope.launch {
                                                val count = PdfEngine.getPageCount(file, passwordInput)
                                                if (count > 0) {
                                                    passwordToUse = passwordInput
                                                    isPasswordPromptRequired = false
                                                    loadPdfPages(file, passwordInput)
                                                } else {
                                                    passwordError = "ভুল পাসওয়ার্ড! অনুগ্রহ করে আবার চেষ্টা করুন।"
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("আনলক করে দেখুন")
                                }
                            }
                        }
                    }
                }

                isLoadingPages && renderedPages.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(text = "PDF লোড হচ্ছে...", fontSize = 14.sp)
                        }
                    }
                }

                else -> {
                    // Document Pages List with Zoom & Pinch
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(pageCount) { pageIndex ->
                            val bitmap = renderedPages[pageIndex]

                            var scale by remember { mutableFloatStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }
                            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                                scale = (scale * zoomChange).coerceIn(1f, 3.5f)
                                if (scale > 1f) {
                                    offset += panChange
                                } else {
                                    offset = Offset.Zero
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("viewer_page_$pageIndex"),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .transformable(state = transformState)
                                    ) {
                                        if (bitmap != null) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Page ${pageIndex + 1}",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer(
                                                        scaleX = scale,
                                                        scaleY = scale,
                                                        translationX = offset.x,
                                                        translationY = offset.y
                                                    ),
                                                contentScale = ContentScale.FillWidth
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(380.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                            }
                                        }

                                        // Page pill
                                        Surface(
                                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = "পৃষ্ঠা ${pageIndex + 1} / $pageCount",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating Page indicator pill at bottom center
                    if (pageCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp)
                        ) {
                            Text(
                                text = "পৃষ্ঠা $currentPageIndex / $pageCount",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Lock PDF Dialog
    if (showLockDialog && pdfItem != null) {
        AlertDialog(
            onDismissRequest = { showLockDialog = false },
            title = { Text("পিডিএফ লক করুন (Password Protect)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "একটি পাসওয়ার্ড সেট করুন। এর পর থেকে ফাইলটি খুলতে এই পাসওয়ার্ড লাগবে।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newLockPassword,
                        onValueChange = { newLockPassword = it },
                        label = { Text("নতুন পাসওয়ার্ড") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLockPassword.isNotBlank()) {
                            viewModel.lockPdf(pdfItem.id, newLockPassword) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "পিডিএফ সফলভাবে লক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    passwordToUse = newLockPassword
                                } else {
                                    Toast.makeText(context, "লক করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showLockDialog = false
                        }
                    }
                ) {
                    Text("লক করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLockDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Unlock PDF Dialog
    if (showUnlockDialog && pdfItem != null) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("পিডিএফ আনলক করুন (পাসওয়ার্ড সরান)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "সুরক্ষা সরানোর জন্য বর্তমান পাসওয়ার্ডটি প্রদান করুন:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = unlockPasswordInput,
                        onValueChange = { unlockPasswordInput = it },
                        label = { Text("বর্তমান পাসওয়ার্ড") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (unlockPasswordInput.isNotBlank()) {
                            viewModel.unlockPdf(pdfItem.id, unlockPasswordInput) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "পিডিএফ সম্পূর্ণ আনলক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    passwordToUse = null
                                } else {
                                    Toast.makeText(context, "ভুল পাসওয়ার্ড! আনলক করা যায়নি।", Toast.LENGTH_SHORT).show()
                                }
                            }
                            showUnlockDialog = false
                        }
                    }
                ) {
                    Text("আনলক করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}
