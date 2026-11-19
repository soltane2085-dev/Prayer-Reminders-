package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.NearbyMosque
import com.example.ui.theme.CelestialGrey
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.DeepSkyBlue
import com.example.ui.theme.GoldenSun
import com.example.ui.theme.GreenEmerald
import com.example.ui.theme.PureDawnLight
import com.example.ui.translation.Translation
import com.example.viewmodel.PrayerViewModel
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import java.util.Locale

@Composable
fun MosqueLocatorScreen(
    viewModel: PrayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appLang by viewModel.appLang.collectAsStateWithLifecycle()
    val t = { key: String -> Translation.get(key, appLang) }

    val nearbyMosques by viewModel.nearbyMosques.collectAsStateWithLifecycle()
    val selectedMosque by viewModel.selectedMosque.collectAsStateWithLifecycle()
    val isLoadingMosques by viewModel.isLoadingMosques.collectAsStateWithLifecycle()
    val mosqueLocatorError by viewModel.mosqueLocatorError.collectAsStateWithLifecycle()
    val mosqueMissingApiKey by viewModel.mosqueMissingApiKey.collectAsStateWithLifecycle()
    val searchRadiusMeters by viewModel.mosqueSearchRadiusMeters.collectAsStateWithLifecycle()
    val centerCoords by viewModel.mosqueCenterCoordinates.collectAsStateWithLifecycle()
    val isUsingGps by viewModel.isMosqueUsingGps.collectAsStateWithLifecycle()

    BackHandler {
        onClose()
    }

    var hasLocationPermission by remember { mutableStateOf(viewModel.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = fine || coarse
        if (fine || coarse) {
            viewModel.loadNearbyMosques(useCurrentGps = true)
        } else {
            val msg = when (appLang) {
                "AR" -> "تم رفض صلاحية الموقع. يتم البحث حول المدينة المختارة حالياً."
                "FR" -> "Autorisation GPS refusée. Recherche autour de la ville active."
                else -> "Location permission denied. Searching around active city."
            }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.loadNearbyMosques(useCurrentGps = false)
        }
    }

    LaunchedEffect(Unit) {
        if (!viewModel.hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Header
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mosque_locator_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = t("mosque_locator_title"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = t("mosque_locator_subtitle"),
                                fontSize = 11.sp,
                                color = CelestialGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = {
                                if (viewModel.hasLocationPermission()) {
                                    viewModel.loadNearbyMosques(useCurrentGps = true)
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mosque_locator_gps_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = t("mosque_gps_locate"),
                                tint = if (isUsingGps) GreenEmerald else DeepSkyBlue
                            )
                        }
                        IconButton(
                            onClick = {
                                viewModel.loadNearbyMosques(useCurrentGps = viewModel.hasLocationPermission())
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("mosque_locator_refresh_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = t("mosque_refresh"),
                                tint = GoldenSun
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Radius filter chips & location source badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val radii = listOf(2000.0 to "2 km", 5000.0 to "5 km", 10000.0 to "10 km")
                        radii.forEach { (radiusVal, label) ->
                            val selected = searchRadiusMeters == radiusVal
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.updateMosqueRadius(radiusVal) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoldenSun.copy(alpha = 0.2f),
                                    selectedLabelColor = GoldenSun
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mosque_locator_radius_${radiusVal.toInt()}")
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Surface(
                            color = (if (isUsingGps) GreenEmerald else DeepSkyBlue).copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = if (isUsingGps) t("mosque_gps_badge") else t("mosque_city_badge"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUsingGps) GreenEmerald else DeepSkyBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Adaptive Content Area (Side-by-side on wide screens, stacked on phones)
            BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                val isWide = maxWidth >= 700.dp

                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                        ) {
                            MosqueMapPane(
                                centerCoords = centerCoords,
                                searchRadiusMeters = searchRadiusMeters,
                                nearbyMosques = nearbyMosques,
                                selectedMosque = selectedMosque,
                                hasLocationPermission = hasLocationPermission,
                                missingApiKey = mosqueMissingApiKey,
                                errorMsg = mosqueLocatorError,
                                onSelectMosque = { viewModel.selectMosque(it) }
                            )
                        }
                        MosqueListPane(
                            nearbyMosques = nearbyMosques,
                            selectedMosque = selectedMosque,
                            isLoadingMosques = isLoadingMosques,
                            mosqueMissingApiKey = mosqueMissingApiKey,
                            mosqueLocatorError = mosqueLocatorError,
                            appLang = appLang,
                            t = t,
                            onSelectMosque = { viewModel.selectMosque(it) },
                            onRetry = {
                                viewModel.loadNearbyMosques(useCurrentGps = viewModel.hasLocationPermission())
                            },
                            onOpenDirections = { mosque ->
                                openDirectionsIntent(context, mosque)
                            },
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.52f)
                        ) {
                            MosqueMapPane(
                                centerCoords = centerCoords,
                                searchRadiusMeters = searchRadiusMeters,
                                nearbyMosques = nearbyMosques,
                                selectedMosque = selectedMosque,
                                hasLocationPermission = hasLocationPermission,
                                missingApiKey = mosqueMissingApiKey,
                                errorMsg = mosqueLocatorError,
                                onSelectMosque = { viewModel.selectMosque(it) }
                            )
                        }
                        MosqueListPane(
                            nearbyMosques = nearbyMosques,
                            selectedMosque = selectedMosque,
                            isLoadingMosques = isLoadingMosques,
                            mosqueMissingApiKey = mosqueMissingApiKey,
                            mosqueLocatorError = mosqueLocatorError,
                            appLang = appLang,
                            t = t,
                            onSelectMosque = { viewModel.selectMosque(it) },
                            onRetry = {
                                viewModel.loadNearbyMosques(useCurrentGps = viewModel.hasLocationPermission())
                            },
                            onOpenDirections = { mosque ->
                                openDirectionsIntent(context, mosque)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(0.48f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MosqueMapPane(
    centerCoords: Pair<Double, Double>,
    searchRadiusMeters: Double,
    nearbyMosques: List<NearbyMosque>,
    selectedMosque: NearbyMosque?,
    hasLocationPermission: Boolean,
    missingApiKey: Boolean,
    errorMsg: String?,
    onSelectMosque: (NearbyMosque) -> Unit
) {
    val context = LocalContext.current
    if (missingApiKey) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldenSun.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = GoldenSun,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "MAPS_API_KEY",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PureDawnLight
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMsg ?: "Configure a valid MAPS_API_KEY in the AI Studio Secrets panel to enable in-app Google Maps & Places search.",
                        fontSize = 12.sp,
                        color = CelestialGrey,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            openGoogleMapsNearbyMosques(context, centerCoords.first, centerCoords.second)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenEmerald),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Google Maps", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    var mapsSdkReady by remember { mutableStateOf(false) }
    var mapsSdkUnavailableReason by remember { mutableStateOf<String?>(null) }
    var isMapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(missingApiKey) {
        if (missingApiKey) {
            mapsSdkReady = false
            return@LaunchedEffect
        }
        try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            )
            val manifestKey = appInfo.metaData?.getString("com.google.android.geo.API_KEY")?.trim()
            if (manifestKey.isNullOrBlank() || !manifestKey.any { it.isLetter() } || manifestKey.length < 16) {
                mapsSdkReady = false
                mapsSdkUnavailableReason = errorMsg
                    ?: "Google Maps API key (MAPS_API_KEY) is missing or invalid in AndroidManifest."
                return@LaunchedEffect
            }

            val playStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context)
            if (playStatus != ConnectionResult.SUCCESS) {
                mapsSdkReady = false
                mapsSdkUnavailableReason = "Google Play Services unavailable on this device (code: $playStatus)."
                return@LaunchedEffect
            }
            val initResult = MapsInitializer.initialize(context.applicationContext)
            if (initResult != ConnectionResult.SUCCESS) {
                mapsSdkReady = false
                mapsSdkUnavailableReason = "Google Maps initializer failed (code: $initResult)."
                return@LaunchedEffect
            }

            // Pre-flight MapView creation so any Play Services/API-key exception is caught cleanly before Compose mounts GoogleMap.
            val probeView = MapView(context)
            probeView.onCreate(null)
            probeView.onDestroy()

            mapsSdkReady = true
            mapsSdkUnavailableReason = null
        } catch (t: Throwable) {
            mapsSdkReady = false
            mapsSdkUnavailableReason = t.localizedMessage ?: "Google Maps SDK failed to initialize."
        }
    }

    if (mapsSdkUnavailableReason != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GoldenSun.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = GoldenSun,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mapsSdkUnavailableReason ?: "Google Maps unavailable.",
                        fontSize = 12.sp,
                        color = CelestialGrey,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val webUri = Uri.parse(
                                "https://www.google.com/maps/search/mosque/@${centerCoords.first},${centerCoords.second},14z"
                            )
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            } catch (_: Throwable) {
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenEmerald),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Google Maps", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    if (!mapsSdkReady) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GoldenSun)
        }
        return
    }

    val centerLatLng = remember(centerCoords) { LatLng(centerCoords.first, centerCoords.second) }
    val targetLatLng = remember(selectedMosque, centerLatLng) {
        selectedMosque?.let { LatLng(it.latitude, it.longitude) } ?: centerLatLng
    }

    val zoomLevel = when {
        searchRadiusMeters <= 2500.0 -> 14.2f
        searchRadiusMeters <= 6000.0 -> 13.0f
        else -> 12.0f
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(centerLatLng, zoomLevel)
    }

    LaunchedEffect(targetLatLng, searchRadiusMeters, isMapLoaded) {
        if (!isMapLoaded) return@LaunchedEffect
        val targetZoom = if (selectedMosque != null) 15.0f else zoomLevel
        try {
            cameraPositionState.animate(
                update = CameraUpdateFactory.newLatLngZoom(targetLatLng, targetZoom),
                durationMs = 550
            )
        } catch (_: Throwable) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(targetLatLng, targetZoom)
        }
    }

    val mapProperties = remember(hasLocationPermission) {
        MapProperties(isMyLocationEnabled = hasLocationPermission)
    }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = false,
            compassEnabled = true
        )
    }

    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mosque_locator_map"),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = uiSettings,
        onMapLoaded = { isMapLoaded = true }
    ) {
        Circle(
            center = centerLatLng,
            radius = searchRadiusMeters,
            fillColor = DeepSkyBlue.copy(alpha = 0.08f),
            strokeColor = DeepSkyBlue.copy(alpha = 0.55f),
            strokeWidth = 3f
        )

        if (!hasLocationPermission) {
            Marker(
                state = rememberMarkerState(key = "center_origin", position = centerLatLng),
                title = "Search Center",
                icon = safeMarkerIcon(isMapLoaded, BitmapDescriptorFactory.HUE_AZURE)
            )
        }

        nearbyMosques.forEach { mosque ->
            val isSelected = mosque.id == selectedMosque?.id
            val markerState = rememberMarkerState(
                key = mosque.id,
                position = LatLng(mosque.latitude, mosque.longitude)
            )
            Marker(
                state = markerState,
                title = mosque.name,
                snippet = mosque.address,
                icon = safeMarkerIcon(
                    isMapLoaded = isMapLoaded,
                    hue = if (isSelected) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE
                ),
                onClick = {
                    onSelectMosque(mosque)
                    false
                }
            )
        }
    }
}

private fun safeMarkerIcon(isMapLoaded: Boolean, hue: Float): BitmapDescriptor? {
    if (!isMapLoaded) return null
    return try {
        BitmapDescriptorFactory.defaultMarker(hue)
    } catch (_: Throwable) {
        null
    }
}

@Composable
private fun MosqueListPane(
    nearbyMosques: List<NearbyMosque>,
    selectedMosque: NearbyMosque?,
    isLoadingMosques: Boolean,
    mosqueMissingApiKey: Boolean,
    mosqueLocatorError: String?,
    appLang: String,
    t: (String) -> String,
    onSelectMosque: (NearbyMosque) -> Unit,
    onRetry: () -> Unit,
    onOpenDirections: (NearbyMosque) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (isLoadingMosques) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = GoldenSun,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = t("mosque_loading_nearby"),
                    fontSize = 13.sp,
                    color = PureDawnLight,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (!mosqueLocatorError.isNullOrBlank() && !mosqueMissingApiKey) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CoralAlert.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mosqueLocatorError,
                        fontSize = 12.sp,
                        color = CoralAlert,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAlert),
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                    ) {
                        Text(t("mosque_refresh"), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        if (!isLoadingMosques && mosqueLocatorError.isNullOrBlank() && nearbyMosques.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = CelestialGrey,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = t("mosque_none_nearby"),
                        fontSize = 13.sp,
                        color = CelestialGrey,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(nearbyMosques, key = { it.id }) { mosque ->
                    val isSelected = mosque.id == selectedMosque?.id
                    val distanceText = formatDistance(mosque.distanceMeters, t)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectMosque(mosque) }
                            .testTag("mosque_card_${mosque.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) GreenEmerald else MaterialTheme.colorScheme.outlineVariant
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        color = (if (isSelected) GreenEmerald else GoldenSun).copy(alpha = 0.16f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = null,
                                    tint = if (isSelected) GreenEmerald else GoldenSun,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mosque.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureDawnLight,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (mosque.address.isNotBlank()) {
                                    Text(
                                        text = mosque.address,
                                        fontSize = 11.sp,
                                        color = CelestialGrey,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = distanceText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepSkyBlue
                                    )
                                    val ratingVal = mosque.rating
                                    if (ratingVal != null && ratingVal > 0.0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = GoldenSun,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            val countSuffix = mosque.userRatingCount?.let { " ($it)" } ?: ""
                                            Text(
                                                text = String.format(Locale.US, "%.1f%s", ratingVal, countSuffix),
                                                fontSize = 11.sp,
                                                color = GoldenSun,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onOpenDirections(mosque) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenEmerald.copy(alpha = 0.2f),
                                    contentColor = GreenEmerald
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("mosque_directions_${mosque.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Directions,
                                    contentDescription = t("mosque_directions"),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = t("mosque_directions"),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDistance(distanceMeters: Double, t: (String) -> String): String {
    return if (distanceMeters < 1000.0) {
        t("mosque_meters_away").replace("{dist}", distanceMeters.toInt().toString())
    } else {
        val km = String.format(Locale.US, "%.1f", distanceMeters / 1000.0)
        t("mosque_km_away").replace("{dist}", km)
    }
}

private fun openDirectionsIntent(context: android.content.Context, mosque: NearbyMosque) {
    val encodedName = Uri.encode(mosque.name)
    val geoUri = Uri.parse("geo:${mosque.latitude},${mosque.longitude}?q=${mosque.latitude},${mosque.longitude}($encodedName)")
    val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
    try {
        context.startActivity(mapIntent)
    } catch (e: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${mosque.latitude},${mosque.longitude}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: Throwable) {
        }
    }
}

private fun openGoogleMapsNearbyMosques(context: android.content.Context, lat: Double, lon: Double) {
    val geoUri = Uri.parse("geo:$lat,$lon?q=mosque")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, geoUri))
    } catch (_: Throwable) {
        val webUri = Uri.parse("https://www.google.com/maps/search/mosque/@$lat,$lon,14z")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (_: Throwable) {
        }
    }
}
