package com.okeho.mapping.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.okeho.mapping.data.remote.SupabaseClient
import com.okeho.mapping.domain.model.SyncStatus
import com.okeho.mapping.domain.repository.CaptureRepository
import com.okeho.mapping.data.remote.CaptureDto
import com.okeho.mapping.data.remote.StreetDto
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onCapturePoint: () -> Unit,
    onMapStreet: () -> Unit,
    onRecords: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val isSyncing = remember { mutableStateOf(false) }

    val hasLocationPermission = remember {
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
        hasLocationPermission.value = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission.value) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", android.content.Context.MODE_PRIVATE)
        )
    }

    val mapView = remember { mutableStateOf<MapView?>(null) }

    fun doSync() {
        scope.launch {
            isSyncing.value = true
            try {
                withContext(Dispatchers.IO) {
                    val captureRepo = com.okeho.mapping.di.SyncHelper.captureRepository
                    val streetRepo = com.okeho.mapping.di.SyncHelper.streetRepository

                    val pendingCaptures = captureRepo.getPendingCaptures()
                    var syncedCaptures = 0
                    for (capture in pendingCaptures) {
                        try {
                            val dto = CaptureDto(
                                id = capture.id,
                                user_id = capture.userId.ifBlank { "anonymous" },
                                name = capture.name,
                                feature_type = capture.featureType.name,
                                latitude = capture.latitude,
                                longitude = capture.longitude,
                                accuracy = capture.accuracy,
                                photo_url = capture.photoUrl,
                                ocr_text = capture.ocrText,
                                sync_status = "synced"
                            )
                            SupabaseClient.getClient().from("captures").insert(dto)
                            captureRepo.updateSyncStatus(capture.id, SyncStatus.SYNCED.name)
                            syncedCaptures++
                        } catch (e: Exception) {
                            android.util.Log.e("Sync", "Failed to sync capture ${capture.id}", e)
                            captureRepo.updateSyncStatus(capture.id, SyncStatus.FAILED.name)
                        }
                    }

                    val pendingStreets = streetRepo.getPendingStreets()
                    var syncedStreets = 0
                    for (street in pendingStreets) {
                        try {
                            val dto = StreetDto(
                                id = street.id,
                                user_id = street.userId.ifBlank { "anonymous" },
                                name = street.name,
                                surface_type = street.surfaceType.name,
                                traffic_direction = street.trafficDirection.name,
                                points_captured = street.points.size,
                                sync_status = "synced"
                            )
                            SupabaseClient.getClient().from("streets").insert(dto)
                            streetRepo.updateSyncStatus(street.id, SyncStatus.SYNCED.name)
                            syncedStreets++
                        } catch (e: Exception) {
                            android.util.Log.e("Sync", "Failed to sync street ${street.id}", e)
                            streetRepo.updateSyncStatus(street.id, SyncStatus.FAILED.name)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Synced $syncedCaptures captures, $syncedStreets streets",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("Sync", "Sync failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Sync failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } finally {
                isSyncing.value = false
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Okeho Mapping",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.List, contentDescription = null) },
                    label = { Text("View Records") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onRecords()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Route, contentDescription = null) },
                    label = { Text("Map Street") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onMapStreet()
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.MyLocation, contentDescription = null) },
                    label = { Text("My Location") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        mapView.value?.let { map ->
                            map.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()?.let { overlay ->
                                overlay.enableFollowLocation()
                                overlay.myLocation?.let { loc ->
                                    map.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                                }
                            }
                        }
                    }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                    label = { Text(if (isSyncing.value) "Syncing..." else "Sync Now") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        doSync()
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Okeho Mapping") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            },
            floatingActionButton = {
                Column(horizontalAlignment = Alignment.End) {
                    SmallFloatingActionButton(
                        onClick = onMapStreet,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Route, contentDescription = "Map Street")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    FloatingActionButton(onClick = onCapturePoint) {
                        Icon(Icons.Default.Add, contentDescription = "Capture Point")
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(16.0)
                            controller.setCenter(GeoPoint(8.03, 3.70))

                            val locationOverlay = MyLocationNewOverlay(
                                GpsMyLocationProvider(ctx),
                                this
                            )
                            locationOverlay.enableMyLocation()
                            locationOverlay.enableFollowLocation()
                            overlays.add(locationOverlay)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { map ->
                        mapView.value = map
                    }
                )

                SmallFloatingActionButton(
                    onClick = {
                        mapView.value?.let { map ->
                            map.overlays.filterIsInstance<MyLocationNewOverlay>().firstOrNull()?.let { overlay ->
                                overlay.myLocation?.let { loc ->
                                    map.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
                                } ?: run {
                                    Toast.makeText(context, "Waiting for GPS...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                }
            }
        }
    }
}
