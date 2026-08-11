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
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.okeho.mapping.ui.components.SignOutConfirmDialog
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
    val viewModel: DashboardViewModel = hiltViewModel()

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
    val accountState by viewModel.accountState.collectAsState()
    val showSignOutConfirm = remember { mutableStateOf(false) }

    // Refresh when the drawer opens rather than once at composition, so the
    // unsynced count in the confirm dialog is current at the moment it matters.
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) viewModel.refreshAccount()
    }

    if (showSignOutConfirm.value) {
        SignOutConfirmDialog(
            pendingCount = accountState.pendingCount,
            onConfirm = {
                showSignOutConfirm.value = false
                scope.launch { drawerState.close() }
                viewModel.signOut()
            },
            onDismiss = { showSignOutConfirm.value = false }
        )
    }

    fun doSync() {
        isSyncing.value = true
        viewModel.sync { msg ->
            isSyncing.value = false
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        "Okeho Mapping",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    accountState.name?.let { name ->
                        Text(name, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        accountState.email ?: "Not signed in",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
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
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onSettings()
                    }
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    label = { Text("Sign Out") },
                    selected = false,
                    onClick = { showSignOutConfirm.value = true }
                )
                Spacer(modifier = Modifier.height(8.dp))
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
