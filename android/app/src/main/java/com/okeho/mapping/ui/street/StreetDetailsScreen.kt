package com.okeho.mapping.ui.street

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.okeho.mapping.domain.model.SurfaceType
import com.okeho.mapping.domain.model.TrafficDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreetDetailsScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: StreetMappingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Street Details") },
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
        ) {
            OutlinedTextField(
                value = uiState.streetName,
                onValueChange = { viewModel.updateStreetName(it) },
                label = { Text("Street name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Surface",
                style = MaterialTheme.typography.labelLarge
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.surfaceType == SurfaceType.PAVED,
                    onClick = { viewModel.updateSurfaceType(SurfaceType.PAVED) },
                    label = { Text("Paved") }
                )
                FilterChip(
                    selected = uiState.surfaceType == SurfaceType.UNPAVED,
                    onClick = { viewModel.updateSurfaceType(SurfaceType.UNPAVED) },
                    label = { Text("Unpaved") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Traffic direction",
                style = MaterialTheme.typography.labelLarge
            )
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.trafficDirection == TrafficDirection.ONE_WAY,
                    onClick = { viewModel.updateTrafficDirection(TrafficDirection.ONE_WAY) },
                    label = { Text("One-way") }
                )
                FilterChip(
                    selected = uiState.trafficDirection == TrafficDirection.TWO_WAY,
                    onClick = { viewModel.updateTrafficDirection(TrafficDirection.TWO_WAY) },
                    label = { Text("Two-way") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Points captured: ${uiState.points.size}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val street = viewModel.getStreet()
                    viewModel.markSaved()
                    onSaved()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.streetName.isNotBlank() && uiState.points.isNotEmpty()
            ) {
                Text("Save Street")
            }
        }
    }
}
