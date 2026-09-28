package com.example.ui.builder

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.data.model.PdfDocumentItem
import com.example.model.ScanFilter
import com.example.ui.viewmodel.ScanViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfBuilderScreen(
    viewModel: ScanViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToViewer: (Long) -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val capturedPages by viewModel.capturedPages.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val generationProgress by viewModel.generationProgress.collectAsStateWithLifecycle()
    val globalFilter by viewModel.globalFilter.collectAsStateWithLifecycle()

    var pdfTitle by remember { mutableStateOf(viewModel.generateDefaultTitle()) }
    var isPasswordProtected by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedPageIndex by remember { mutableIntStateOf(0) }
    var completedPdfItem by remember { mutableStateOf<PdfDocumentItem?>(null) }

    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addMultiplePages(uris)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "PDF তৈরি করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${capturedPages.size} টি পেজ প্রস্তুত",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNavigateToCamera,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("আরও ছবি")
                    }

                    Button(
                        onClick = {
                            if (capturedPages.isEmpty()) {
                                Toast.makeText(context, "কোনো ছবি নেই!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isPasswordProtected && passwordInput.isBlank()) {
                                Toast.makeText(context, "পাসওয়ার্ড দিন অথবা লক অপশন বন্ধ করুন", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.createPdf(
                                title = pdfTitle,
                                password = if (isPasswordProtected) passwordInput else null
                            ) { createdItem ->
                                if (createdItem != null) {
                                    completedPdfItem = createdItem
                                } else {
                                    Toast.makeText(context, "পিডিএফ তৈরি করতে সমস্যা হয়েছে", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = capturedPages.isNotEmpty() && !isGenerating,
                        modifier = Modifier
                            .weight(1.8f)
                            .height(48.dp)
                            .testTag("generate_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "PDF সেভ করুন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Document Title Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "পিডিএফ ফাইলের নাম",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = pdfTitle,
                        onValueChange = { pdfTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pdf_title_input"),
                        singleLine = true,
                        placeholder = { Text("ফাইল নাম...") },
                        trailingIcon = {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                    )
                }
            }

            // Document Scanner Filters (The core user requirement: A4 white background, black text)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "স্ক্যান ফিল্টার (A4 Document Filter)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "হাত কাপলেও লেখা স্পষ্ট ও ব্যাকগ্রাউন্ড পরিষ্কার হবে",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Filter Options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScanFilter.entries.forEach { filter ->
                            val isSelected = globalFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setGlobalFilter(filter) },
                                label = {
                                    Text(
                                        text = filter.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    } else {
                                        when (filter) {
                                            ScanFilter.BW_DOCUMENT -> Icon(Icons.Default.FilterBAndW, contentDescription = null, modifier = Modifier.size(14.dp))
                                            ScanFilter.MAGIC_COLOR -> Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                            ScanFilter.GRAYSCALE -> Icon(Icons.Default.FilterBAndW, contentDescription = null, modifier = Modifier.size(14.dp))
                                            ScanFilter.ORIGINAL -> Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Filter Description banner
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${globalFilter.displayName}: ${globalFilter.description}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Pages Carousel / Reorder / Rotation Management
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পেজ ব্যবস্থাপনা (${capturedPages.size})",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Add gallery images button
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ছবি যোগ", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Pages List Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(2.dp)
                    ) {
                        itemsIndexed(capturedPages, key = { _, p -> p.id }) { index, page ->
                            val isFocused = selectedPageIndex == index
                            Card(
                                modifier = Modifier
                                    .width(130.dp)
                                    .clickable { selectedPageIndex = index }
                                    .testTag("page_card_$index"),
                                shape = RoundedCornerShape(10.dp),
                                border = if (isFocused) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Image with rotation applied
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(170.dp)
                                            .background(Color.White)
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(page.imageUri),
                                            contentDescription = "Page ${index + 1}",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .rotate(page.rotationDegrees.toFloat()),
                                            contentScale = ContentScale.Fit
                                        )

                                        // Page chip
                                        Surface(
                                            color = Color.Black.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(bottomEnd = 6.dp),
                                            modifier = Modifier.align(Alignment.TopStart)
                                        ) {
                                            Text(
                                                text = "P${index + 1}",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Page tools: Move Left, Rotate, Move Right, Delete
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Rotate 90
                                        IconButton(
                                            onClick = { viewModel.rotatePage(page.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.RotateRight,
                                                contentDescription = "Rotate",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Move backwards
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    viewModel.movePage(index, index - 1)
                                                    selectedPageIndex = index - 1
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Move Left",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Move forwards
                                        IconButton(
                                            onClick = {
                                                if (index < capturedPages.size - 1) {
                                                    viewModel.movePage(index, index + 1)
                                                    selectedPageIndex = index + 1
                                                }
                                            },
                                            enabled = index < capturedPages.size - 1,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = "Move Right",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Delete page
                                        IconButton(
                                            onClick = {
                                                viewModel.removePage(page.id)
                                                if (selectedPageIndex >= capturedPages.size - 1 && selectedPageIndex > 0) {
                                                    selectedPageIndex--
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Page",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Security & PDF Lock Card (Requirement 4: পিডিএফ লক/আনলক)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (isPasswordProtected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "PDF Lock",
                                        tint = if (isPasswordProtected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "পিডিএফ লক (Password Protect)",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "পাসওয়ার্ড ছাড়া ফাইলটি খোলা যাবে না",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isPasswordProtected,
                            onCheckedChange = { isPasswordProtected = it },
                            modifier = Modifier.testTag("password_protect_switch")
                        )
                    }

                    AnimatedVisibility(visible = isPasswordProtected) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("গোপন পাসওয়ার্ড লিখুন") },
                                singleLine = true,
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password visibility"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pdf_password_field")
                            )
                            Text(
                                text = "🔒 ১২৮-বিট শক্তিশালী এনক্রিপশন যুক্ত হবে।",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }

    // PDF Generation Progress Dialog
    if (isGenerating) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("পিডিএফ তৈরি হচ্ছে...") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "A4 পেজ ও স্ক্যান ফিল্টার প্রসেস হচ্ছে (${generationProgress.first}/${generationProgress.second})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = {
                            if (generationProgress.second > 0) {
                                generationProgress.first.toFloat() / generationProgress.second.toFloat()
                            } else 0f
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        )
    }

    // Success Dialog upon completion
    completedPdfItem?.let { item ->
        AlertDialog(
            onDismissRequest = { completedPdfItem = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("পিডিএফ প্রস্তুত!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "ফাইল: ${item.title}.pdf", fontWeight = FontWeight.Bold)
                    Text(text = "পেজ সংখ্যা: ${item.pageCount}")
                    Text(text = "সাইজ: ${viewModel.formatFileSize(item.fileSizeBytes)}")
                    if (item.isEncrypted) {
                        Text(
                            text = "🔐 পাসওয়ার্ড দিয়ে লক করা আছে",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = item.id
                        completedPdfItem = null
                        onNavigateToViewer(id)
                    },
                    modifier = Modifier.testTag("success_view_button")
                ) {
                    Text("পিডিএফ দেখুন")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            viewModel.sharePdf(File(item.filePath))
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার")
                    }
                    TextButton(
                        onClick = {
                            completedPdfItem = null
                            onNavigateHome()
                        }
                    ) {
                        Text("হোমে যান")
                    }
                }
            }
        )
    }
}
