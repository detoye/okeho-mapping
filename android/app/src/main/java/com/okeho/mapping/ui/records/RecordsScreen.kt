package com.okeho.mapping.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.okeho.mapping.domain.model.Capture
import com.okeho.mapping.domain.model.Street
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.ui.components.SyncStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAB_ALL = 0
private const val TAB_PENDING = 1
private const val TAB_SYNCED = 2
private const val TAB_FAILED = 3

private val dateFormat get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    viewModel: RecordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val statusFilter = when (uiState.selectedTab) {
        TAB_PENDING -> SyncStatus.PENDING
        TAB_SYNCED -> SyncStatus.SYNCED
        TAB_FAILED -> SyncStatus.FAILED
        else -> null
    }
    val captures = uiState.captures.filter { statusFilter == null || it.syncStatus == statusFilter }
    val streets = uiState.streets.filter { statusFilter == null || it.syncStatus == statusFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Records") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.selectedTab == TAB_ALL,
                    onClick = { viewModel.selectTab(TAB_ALL) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.selectedTab == TAB_PENDING,
                    onClick = { viewModel.selectTab(TAB_PENDING) },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = uiState.selectedTab == TAB_SYNCED,
                    onClick = { viewModel.selectTab(TAB_SYNCED) },
                    label = { Text("Synced") }
                )
                FilterChip(
                    selected = uiState.selectedTab == TAB_FAILED,
                    onClick = { viewModel.selectTab(TAB_FAILED) },
                    label = { Text("Failed") }
                )
            }

            when {
                uiState.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                captures.isEmpty() && streets.isEmpty() -> Text(
                    text = when (uiState.selectedTab) {
                        TAB_PENDING -> "No pending records"
                        TAB_SYNCED -> "No synced records"
                        TAB_FAILED -> "No failed records"
                        else -> "No records yet"
                    },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )

                else -> LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(captures, key = { it.id }) { capture ->
                        CaptureCard(
                            capture = capture,
                            onDelete = { viewModel.deleteCapture(capture) }
                        )
                    }
                    items(streets, key = { it.id }) { street ->
                        StreetCard(
                            street = street,
                            onDelete = { viewModel.deleteStreet(street) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureCard(
    capture: Capture,
    onDelete: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    if (showDetails) {
        RecordPreviewDialog(
            title = capture.name.ifBlank { capture.featureType.displayName },
            photoUrl = capture.photoUrl,
            syncStatus = capture.syncStatus,
            createdAt = capture.createdAt,
            onDismiss = { showDetails = false }
        ) {
            DetailRow("Type", capture.featureType.displayName)
            DetailRow("Accuracy", "${String.format("%.1f", capture.accuracy)} m")
            DetailRow("Latitude", String.format("%.6f", capture.latitude))
            DetailRow("Longitude", String.format("%.6f", capture.longitude))
            if (!capture.ocrText.isNullOrBlank()) {
                DetailRow("OCR", capture.ocrText)
            }
        }
    }

    RecordCard(
        title = capture.name.ifBlank { capture.featureType.displayName },
        subtitle = capture.featureType.displayName,
        detail = "Accuracy: ${String.format("%.1f", capture.accuracy)} m",
        createdAt = capture.createdAt,
        syncStatus = capture.syncStatus,
        photoUrl = capture.photoUrl,
        fallbackIcon = null,
        onClick = { showDetails = true },
        onDelete = onDelete
    )
}

@Composable
private fun StreetCard(
    street: Street,
    onDelete: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }

    if (showDetails) {
        RecordPreviewDialog(
            title = street.name.ifBlank { "Unnamed Street" },
            photoUrl = null,
            syncStatus = street.syncStatus,
            createdAt = street.createdAt,
            onDismiss = { showDetails = false }
        ) {
            DetailRow("Surface", street.surfaceType.displayName)
            DetailRow("Traffic", street.trafficDirection.displayName)
            DetailRow("Points", street.points.size.toString())
            street.points.firstOrNull()?.let {
                DetailRow("First point", "${String.format("%.6f", it.first)}, ${String.format("%.6f", it.second)}")
            }
            street.points.lastOrNull()?.let {
                DetailRow("Last point", "${String.format("%.6f", it.first)}, ${String.format("%.6f", it.second)}")
            }
        }
    }

    RecordCard(
        title = street.name.ifBlank { "Unnamed Street" },
        subtitle = "${street.surfaceType.displayName} • ${street.trafficDirection.displayName}",
        detail = "Points: ${street.points.size}",
        createdAt = street.createdAt,
        syncStatus = street.syncStatus,
        photoUrl = null,
        fallbackIcon = Icons.Default.Timeline,
        onClick = { showDetails = true },
        onDelete = onDelete
    )
}

/**
 * Card layout shared by captures and streets: a leading 64dp thumbnail, the
 * text block, and a trailing delete button all on one row, so the row height
 * stays constant whether or not the record has a photo.
 */
@Composable
private fun RecordCard(
    title: String,
    subtitle: String,
    detail: String,
    createdAt: Long,
    syncStatus: SyncStatus,
    photoUrl: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Thumbnail(photoUrl = photoUrl, fallbackIcon = fallbackIcon)

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    SyncStatusBadge(status = syncStatus)
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(text = detail, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = dateFormat.format(Date(createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(
    photoUrl: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (photoUrl.isNullOrBlank()) {
            Icon(
                imageVector = fallbackIcon ?: Icons.Default.ImageNotSupported,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Record photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * The photo is the point of the preview, so it leads. The body scrolls because
 * a capture with OCR text can overflow a short screen.
 */
@Composable
private fun RecordPreviewDialog(
    title: String,
    photoUrl: String?,
    syncStatus: SyncStatus,
    createdAt: Long,
    onDismiss: () -> Unit,
    details: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "Record photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(4f / 3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                details()

                DetailRow("Status", syncStatus.displayName)
                DetailRow("Date", dateFormat.format(Date(createdAt)))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}
