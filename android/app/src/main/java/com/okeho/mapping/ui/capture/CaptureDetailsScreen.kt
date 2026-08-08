package com.okeho.mapping.ui.capture

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDetailsScreen(
    featureType: String,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: CaptureDetailsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.startGpsCapture(context)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            uiState.photoUri?.let { uri ->
                viewModel.extractTextFromImage(context, uri)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            viewModel.startGpsCapture(context)
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture Point") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (uiState.accuracyStatus) {
                AccuracyStatus.WAITING, AccuracyStatus.SAMPLING -> {
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Getting your location...")
                    Text(
                        "GPS accuracy: ${String.format("%.1f", uiState.accuracy)} m",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (uiState.isCapturing) {
                        Text(
                            "Stay still for 3-5 seconds for the best accuracy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                AccuracyStatus.POOR -> {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "GPS accuracy: ${String.format("%.1f", uiState.accuracy)} m",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Accuracy is too low",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Move to a clearer location and try again",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retryGpsCapture(context) }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text(" Try Again")
                    }
                }

                AccuracyStatus.GOOD -> {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "GPS accuracy: ${String.format("%.1f", uiState.accuracy)} m",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Good position",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.photoUri != null) {
                        val bitmap = remember(uiState.photoUri) {
                            try {
                                context.contentResolver.openInputStream(uiState.photoUri)?.use { stream ->
                                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                                }
                            } catch (e: Exception) {
                                null
                            }
                        }
                        bitmap?.let { imageBitmap ->
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Captured photo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                val photoFile = File(
                                    context.cacheDir,
                                    "photo_${System.currentTimeMillis()}.jpg"
                                )
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                viewModel.setPhotoUri(uri)
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text(" Take Photo")
                        }
                    }

                    if (uiState.ocrText != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Text found in photo:",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            uiState.ocrText!!,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedButton(
                            onClick = { viewModel.useOcrText() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Use This")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    uiState.bestLocation?.let { location ->
                        Text(
                            "GPS Information",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text("Latitude: ${location.latitude}")
                        Text("Longitude: ${location.longitude}")
                        Text("Accuracy: ${String.format("%.1f", location.accuracy)} m")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.saveCapture(featureType) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving && uiState.name.isNotBlank()
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Capture")
                        }
                    }
                }
            }

            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
