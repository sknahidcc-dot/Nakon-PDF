package com.example.ui.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.PdfDocumentItem
import com.example.ui.viewmodel.ScanViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ScanViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToBuilder: () -> Unit,
    onNavigateToViewer: (Long) -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val context = LocalContext.current
    val pdfs by viewModel.allPdfs.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPdfForRename by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var selectedPdfForDelete by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var selectedPdfForLock by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var lockPasswordInput by remember { mutableStateOf("") }
    var selectedPdfForUnlock by remember { mutableStateOf<PdfDocumentItem?>(null) }
    var unlockPasswordInput by remember { mutableStateOf("") }

    // Multi-photo picker for selecting images from gallery
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.clearPages()
            viewModel.addMultiplePages(uris)
            onNavigateToBuilder()
        }
    }

    val filteredPdfs = remember(pdfs, searchQuery) {
        if (searchQuery.isBlank()) pdfs
        else pdfs.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "Nakon-PDF Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Nakon-PDF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "স্মার্ট ইমেজ টু পিডিএফ মেকার",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAbout,
                        modifier = Modifier.testTag("about_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Developer & App Info",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats banner
            item {
                StatsCard(
                    totalCount = pdfs.size,
                    totalPages = pdfs.sumOf { it.pageCount },
                    lockedCount = pdfs.count { it.isEncrypted }
                )
            }

            // Quick Action CTAs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Continuous Live Camera
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToCamera() }
                            .testTag("continuous_camera_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Live Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "লাইভ ক্যামেরা স্ক্যান",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "একের পর এক ছবি তুলে দ্রুত পিডিএফ",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // 2. Gallery Image to PDF
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                galleryPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .testTag("gallery_picker_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Import Photos",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Text(
                                text = "গ্যালারি ছবি দিন",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "এক বা একাধিক ছবি সিলেক্ট করে পিডিএফ",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Search Bar & Section Header
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_field"),
                        placeholder = { Text("পিডিএফ খুঁজুন...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "আপনার সংরক্ষিত পিডিএফ (${filteredPdfs.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Empty state
            if (filteredPdfs.isEmpty()) {
                item {
                    EmptyPdfView(
                        isSearching = searchQuery.isNotBlank(),
                        onStartCamera = onNavigateToCamera,
                        onPickGallery = {
                            galleryPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            } else {
                items(filteredPdfs, key = { it.id }) { pdf ->
                    PdfItemCard(
                        pdf = pdf,
                        viewModel = viewModel,
                        onClick = { onNavigateToViewer(pdf.id) },
                        onShare = { viewModel.sharePdf(File(pdf.filePath)) },
                        onRename = {
                            selectedPdfForRename = pdf
                            renameInput = pdf.title
                        },
                        onDelete = { selectedPdfForDelete = pdf },
                        onLock = {
                            selectedPdfForLock = pdf
                            lockPasswordInput = ""
                        },
                        onUnlock = {
                            selectedPdfForUnlock = pdf
                            unlockPasswordInput = ""
                        }
                    )
                }
            }
        }
    }

    // Rename Dialog
    selectedPdfForRename?.let { pdf ->
        AlertDialog(
            onDismissRequest = { selectedPdfForRename = null },
            title = { Text("পিডিএফ রিনেম করুন") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("নতুন ফাইলের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renamePdf(pdf.id, renameInput) { success ->
                                if (success) {
                                    Toast.makeText(context, "রিনেম সফল হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                            selectedPdfForRename = null
                        }
                    }
                ) {
                    Text("সংরক্ষণ")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPdfForRename = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    selectedPdfForDelete?.let { pdf ->
        AlertDialog(
            onDismissRequest = { selectedPdfForDelete = null },
            title = { Text("পিডিএফ ডিলিট করবেন?") },
            text = { Text("\"${pdf.title}\" ফাইলটি স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePdf(pdf.id) {
                            Toast.makeText(context, "ফাইল ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                        selectedPdfForDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPdfForDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // PDF Lock (Set Password) Dialog
    selectedPdfForLock?.let { pdf ->
        AlertDialog(
            onDismissRequest = { selectedPdfForLock = null },
            title = { Text("পিডিএফ লক করুন (Password Protect)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "এই পিডিএফ খোলার জন্য পাসওয়ার্ড প্রয়োজন হবে।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = lockPasswordInput,
                        onValueChange = { lockPasswordInput = it },
                        label = { Text("পাসওয়ার্ড দিন") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (lockPasswordInput.isNotBlank()) {
                            viewModel.lockPdf(pdf.id, lockPasswordInput) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "পিডিএফ সফলভাবে লক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "লক করতে ব্যর্থ হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            }
                            selectedPdfForLock = null
                        }
                    }
                ) {
                    Text("লক করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPdfForLock = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // PDF Unlock (Remove Password) Dialog
    selectedPdfForUnlock?.let { pdf ->
        AlertDialog(
            onDismissRequest = { selectedPdfForUnlock = null },
            title = { Text("পিডিএফ আনলক করুন (পাসওয়ার্ড সরান)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "সুরক্ষা সরাতে বর্তমান পাসওয়ার্ডটি প্রদান করুন:",
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
                            viewModel.unlockPdf(pdf.id, unlockPasswordInput) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "পিডিএফ আনলক করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "ভুল পাসওয়ার্ড! আনলক করা যায়নি।", Toast.LENGTH_SHORT).show()
                                }
                            }
                            selectedPdfForUnlock = null
                        }
                    }
                ) {
                    Text("আনলক করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedPdfForUnlock = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun StatsCard(totalCount: Int, totalPages: Int, lockedCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalCount",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "মোট ফাইল",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$totalPages",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "মোট পেজ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(30.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$lockedCount",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "লকড ফাইল",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PdfItemCard(
    pdf: PdfDocumentItem,
    viewModel: ScanViewModel,
    onClick: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onLock: () -> Unit,
    onUnlock: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("pdf_item_${pdf.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail or Document Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (pdf.thumbnailPath != null && File(pdf.thumbnailPath).exists()) {
                    Image(
                        painter = rememberAsyncImagePainter(File(pdf.thumbnailPath)),
                        contentDescription = "Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF Icon",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Lock overlay icon if encrypted
                if (pdf.isEncrypted) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = pdf.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (pdf.isEncrypted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "LOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${pdf.pageCount} Pages",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = viewModel.formatFileSize(pdf.fileSizeBytes),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = viewModel.formatDate(pdf.createdAt),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Direct Share action button
            IconButton(onClick = onShare) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share PDF",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // More Options menu
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("পিডিএফ দেখুন") },
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("শেয়ার করুন") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShare()
                        }
                    )
                    if (pdf.isEncrypted) {
                        DropdownMenuItem(
                            text = { Text("আনলক করুন (পাসওয়ার্ড সরান)") },
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onUnlock()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("পাসওয়ার্ড দিয়ে লক করুন") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onLock()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("রিনেম করুন") },
                        leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("ডিলিট করুন", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyPdfView(
    isSearching: Boolean,
    onStartCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearching) Icons.Default.Search else Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (isSearching) {
                Text(
                    text = "কোনো পিডিএফ ফাইল পাওয়া যায়নি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "অনুগ্রহ করে অন্য কোনো নাম দিয়ে সার্চ করুন।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "এখনও কোনো পিডিএফ তৈরি করেননি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "লাইভ ক্যামেরা দিয়ে একের পর এক ছবি তুলে বা গ্যালারি থেকে ছবি নিয়ে ঝটপট A4 সাইজের পরিষ্কার পিডিএফ তৈরি করুন।",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onStartCamera,
                        modifier = Modifier.testTag("empty_start_camera")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ক্যামেরা স্ক্যান")
                    }

                    OutlinedButton(onClick = onPickGallery) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("গ্যালারি")
                    }
                }
            }
        }
    }
}
