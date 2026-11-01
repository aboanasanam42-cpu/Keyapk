package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFileBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onFilesSelected: (List<Uri>) -> Unit
) {
    // Launcher for Storage Access Framework OpenMultipleDocuments
    val openDocsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            onFilesSelected(uris)
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VaultSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(VaultBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "إضافة ملفات إلى الخزنة السرية",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary,
                    fontSize = 18.sp
                )
            )
            Text(
                text = "حدد نوع الملفات لجلبها وتخزينها في المجلد المعزول",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )

            // Category Options
            FileImportOptionItem(
                title = "صور (Photos)",
                subtitle = "جلب الصور من ذاكرة الجهاز والمعرض",
                icon = Icons.Default.Image,
                iconTint = Color(0xFF38BDF8),
                tag = "import_images_option",
                onClick = {
                    openDocsLauncher.launch(arrayOf("image/*"))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FileImportOptionItem(
                title = "فيديو (Videos)",
                subtitle = "جلب مقاطع الفيديو وحفظها بشكل مشفر",
                icon = Icons.Default.Movie,
                iconTint = Color(0xFFA78BFA),
                tag = "import_videos_option",
                onClick = {
                    openDocsLauncher.launch(arrayOf("video/*"))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FileImportOptionItem(
                title = "صوتيات (Audio)",
                subtitle = "تسجيلات صوتية، ملفات MP3 و WAV",
                icon = Icons.Default.Audiotrack,
                iconTint = Color(0xFF34D399),
                tag = "import_audio_option",
                onClick = {
                    openDocsLauncher.launch(arrayOf("audio/*"))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            FileImportOptionItem(
                title = "عروض بوربوينت ومستندات (Presentations & Docs)",
                subtitle = "ملفات PPT, PPTX, PDF, Word والمستندات العامة",
                icon = Icons.Default.Slideshow,
                iconTint = Color(0xFFF59E0B),
                tag = "import_docs_option",
                onClick = {
                    openDocsLauncher.launch(
                        arrayOf(
                            "application/vnd.ms-powerpoint",
                            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                            "application/pdf",
                            "application/msword",
                            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                            "application/*",
                            "*/*"
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun FileImportOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(VaultCard, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}
