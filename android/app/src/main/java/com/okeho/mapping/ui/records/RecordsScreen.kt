package com.okeho.mapping.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.ui.components.SyncStatusBadge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordsScreen(
    onBack: () -> Unit,
    viewModel: RecordsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    label = { Text("Pending") }
                )
                FilterChip(
                    selected = uiState.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    label = { Text("Synced") }
                )
                FilterChip(
                    selected = uiState.selectedTab == 3,
                    onClick = { viewModel.selectTab(3) },
                    label = { Text("Failed") }
                )
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            } else {
                when (uiState.selectedTab) {
                    0 -> {
                        val allItems = uiState.captures + uiState.streets
                        if (allItems.isEmpty()) {
                            Text(
                                "No records yet",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.captures) { capture ->
                                    CaptureCard(
                                        capture = capture,
                                        onDelete = { viewModel.deleteCapture(capture) }
                                    )
                                }
                                items(uiState.streets) { street ->
                                    StreetCard(
                                        street = street,
                                        onDelete = { viewModel.deleteStreet(street) }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        val pendingCaptures = uiState.captures.filter { it.syncStatus == SyncStatus.PENDING }
                        val pendingStreets = uiState.streets.filter { it.syncStatus == SyncStatus.PENDING }
                        if (pendingCaptures.isEmpty() && pendingStreets.isEmpty()) {
                            Text(
                                "No pending records",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pendingCaptures) { capture ->
                                    CaptureCard(
                                        capture = capture,
                                        onDelete = { viewModel.deleteCapture(capture) }
                                    )
                                }
                                items(pendingStreets) { street ->
                                    StreetCard(
                                        street = street,
                                        onDelete = { viewModel.deleteStreet(street) }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        val syncedCaptures = uiState.captures.filter { it.syncStatus == SyncStatus.SYNCED }
                        val syncedStreets = uiState.streets.filter { it.syncStatus == SyncStatus.SYNCED }
                        if (syncedCaptures.isEmpty() && syncedStreets.isEmpty()) {
                            Text(
                                "No synced records",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(syncedCaptures) { capture ->
                                    CaptureCard(
                                        capture = capture,
                                        onDelete = { viewModel.deleteCapture(capture) }
                                    )
                                }
                                items(syncedStreets) { street ->
                                    StreetCard(
                                        street = street,
                                        onDelete = { viewModel.deleteStreet(street) }
                                    )
                                }
                            }
                        }
                    }
                    3 -> {
                        val failedCaptures = uiState.captures.filter { it.syncStatus == SyncStatus.FAILED }
                        val failedStreets = uiState.streets.filter { it.syncStatus == SyncStatus.FAILED }
                        if (failedCaptures.isEmpty() && failedStreets.isEmpty()) {
                            Text(
                                "No failed records",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(failedCaptures) { capture ->
                                    CaptureCard(
                                        capture = capture,
                                        onDelete = { viewModel.deleteCapture(capture) }
                                    )
                                }
                                items(failedStreets) { street ->
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
        }
    }
}

@Composable
private fun CaptureCard(
    capture: com.okeho.mapping.domain.model.Capture,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    var showDetails by remember { mutableStateOf(false) }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(capture.name.ifBlank { capture.featureType.displayName }) },
            text = {
                Column {
                    Text("Type: ${capture.featureType.displayName}")
                    Text("Accuracy: ${String.format("%.1f", capture.accuracy)} m")
                    Text("Lat: ${String.format("%.6f", capture.latitude)}")
                    Text("Lng: ${String.format("%.6f", capture.longitude)}")
                    if (!capture.ocrText.isNullOrBlank()) {
                        Text("OCR: ${capture.ocrText}")
                    }
                    Text("Synced: ${capture.syncStatus.displayName}")
                    Text("Date: ${dateFormat.format(Date(capture.createdAt))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetails = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = capture.name.ifBlank { capture.featureType.displayName },
                    style = MaterialTheme.typography.titleMedium
                )
                SyncStatusBadge(status = capture.syncStatus)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = capture.featureType.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Accuracy: ${String.format("%.1f", capture.accuracy)} m",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = dateFormat.format(Date(capture.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
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
private fun StreetCard(
    street: com.okeho.mapping.domain.model.Street,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    var showDetails by remember { mutableStateOf(false) }

    if (showDetails) {
        AlertDialog(
            onDismissRequest = { showDetails = false },
            title = { Text(street.name.ifBlank { "Unnamed Street" }) },
            text = {
                Column {
                    Text("Surface: ${street.surfaceType.displayName}")
                    Text("Traffic: ${street.trafficDirection.displayName}")
                    Text("Points: ${street.points.size}")
                    if (street.points.isNotEmpty()) {
                        Text("First point: ${String.format("%.6f", street.points.first().first)}, ${String.format("%.6f", street.points.first().second)}")
                        Text("Last point: ${String.format("%.6f", street.points.last().first)}, ${String.format("%.6f", street.points.last().second)}")
                    }
                    Text("Synced: ${street.syncStatus.displayName}")
                    Text("Date: ${dateFormat.format(Date(street.createdAt))}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetails = false }) {
                    Text("Close")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetails = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = street.name.ifBlank { "Unnamed Street" },
                    style = MaterialTheme.typography.titleMedium
                )
                SyncStatusBadge(status = street.syncStatus)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${street.surfaceType.displayName} • ${street.trafficDirection.displayName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Points: ${street.points.size}",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = dateFormat.format(Date(street.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
