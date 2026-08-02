package com.fontainment.app.presentation.drive

import androidx.compose.animation.AnimatedVisibility
import com.fontainment.app.presentation.common.SpotifyWidgetHostView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fontainment.app.presentation.navigation.Screen
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom minimal dark styled JSON for maps
const val MapStyleDarkJson = """
[
  {
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#141519"
      }
    ]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [
      {
        "color": "#8a8d9a"
      }
    ]
  },
  {
    "elementType": "labels.text.stroke",
    "stylers": [
      {
        "color": "#141519"
      }
    ]
  },
  {
    "featureType": "administrative",
    "elementType": "geometry",
    "stylers": [
      {
        "visibility": "off"
      }
    ]
  },
  {
    "featureType": "road",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#2c2d35"
      }
    ]
  },
  {
    "featureType": "road.highway",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#e82127"
      },
      {
        "weight": 1.2
      }
    ]
  },
  {
    "featureType": "water",
    "elementType": "geometry",
    "stylers": [
      {
        "color": "#09090b"
      }
    ]
  }
]
"""

@Composable
fun DriveModeScreen(
    navController: NavController,
    viewModel: DriveViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val vehicleState by viewModel.vehicleState.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val musicVolume by viewModel.musicVolume.collectAsState()
    val assistantActive by viewModel.assistantActive.collectAsState()
    val assistantSpeechText by viewModel.assistantSpeechText.collectAsState()
    val themeName by viewModel.currentTheme.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    // New view states connected to viewmodel flows
    val currentLatLng by viewModel.currentLatLng.collectAsState()
    val mapMarkers by viewModel.mapMarkers.collectAsState()
    val activeCallState by viewModel.activeCallState.collectAsState()
    val callerName by viewModel.callerName.collectAsState()
    val callerNumber by viewModel.callerNumber.collectAsState()
    val dialpadText by viewModel.dialpadText.collectAsState()
    val recentCalls by viewModel.recentCalls.collectAsState()
    val favoriteContacts by viewModel.favoriteContacts.collectAsState()
    val activeCallTimeSeconds by viewModel.activeCallTimeSeconds.collectAsState()
    val playbackDevices by viewModel.playbackDevices.collectAsState()
    val selectedPlaybackDevice by viewModel.selectedPlaybackDevice.collectAsState()
    val equalizerBands by viewModel.equalizerBands.collectAsState()

    val routePoints by viewModel.currentRoutePoints.collectAsState()
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val activePlayerPackage by viewModel.activePlayerPackage.collectAsState()

    val currentNavInstruction by viewModel.currentNavInstruction.collectAsState()
    val distanceToTurnMeters by viewModel.distanceToTurnMeters.collectAsState()

    val bitmap = remember(currentTrack.albumArtUri) {
        if (currentTrack.albumArtUri != null && currentTrack.albumArtUri!!.startsWith("file://")) {
            try {
                val path = currentTrack.albumArtUri!!.removePrefix("file://")
                android.graphics.BitmapFactory.decodeFile(path)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showDevicesDialog by remember { mutableStateOf(false) }
    var showPhoneKeypadDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Toggle between Google Map and premium Hud Map styling fallback
    var mapHUDViewMode by remember { mutableStateOf(false) }

    // Toggle between standard split grid and ultra minimal full screen map layout
    var isMinimalMode by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Map camera management
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    // Keep camera focused on vehicle location or navigation destination
    androidx.compose.runtime.LaunchedEffect(currentLatLng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(currentLatLng, 16f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Main Top Content: Three Panels
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // LEFT PANEL: Interactive Google Map (Dynamic weight to expand to full screen!)
                Box(
                    modifier = Modifier
                        .weight(if (isMinimalMode) 12f else 4.2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF141519))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                ) {
                    if (mapHUDViewMode) {
                        // Premium Minimal HUD Map View Fallback
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height
                            
                            // Highways
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(0f, height * 0.45f)
                                    cubicTo(width * 0.3f, height * 0.3f, width * 0.6f, height * 0.75f, width, height * 0.6f)
                                },
                                color = primaryColor.copy(alpha = 0.2f),
                                style = Stroke(width = 24f)
                            )
                            // Local roads
                            drawPath(
                                path = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(width * 0.25f, 0f)
                                    lineTo(width * 0.55f, height)
                                    moveTo(width * 0.8f, 0f)
                                    lineTo(width * 0.4f, height)
                                },
                                color = Color.White.copy(alpha = 0.04f),
                                style = Stroke(width = 8f)
                            )
                            // Vehicle position
                            drawCircle(
                                color = primaryColor,
                                radius = 16f,
                                center = Offset(width * 0.44f, height * 0.54f)
                            )
                            drawCircle(
                                color = primaryColor.copy(alpha = 0.25f),
                                radius = 32f,
                                center = Offset(width * 0.44f, height * 0.54f)
                            )
                        }
                    } else {
                        // Real Google Maps Compose Component
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(
                                isMyLocationEnabled = false, // handled with mock marker or fine location
                                mapStyleOptions = MapStyleOptions(MapStyleDarkJson),
                                isBuildingEnabled = true
                            ),
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                compassEnabled = true
                            )
                        ) {
                            // Current Location vehicle indicator marker
                            Marker(
                                state = MarkerState(position = currentLatLng),
                                title = "Current Location",
                                snippet = vehicleState.currentRoadName
                            )

                            // Quick Action category and destination search markers
                            mapMarkers.forEach { marker ->
                                Marker(
                                    state = MarkerState(position = marker.position),
                                    title = marker.title,
                                    snippet = marker.snippet
                                )
                            }

                            if (routePoints.isNotEmpty()) {
                                Polyline(
                                    points = routePoints,
                                    color = primaryColor,
                                    width = 10f
                                )
                            }
                        }
                    }

                    // Turn-by-Turn Instruction Banner
                    if (currentNavInstruction != null && currentNavInstruction!!.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp)
                                .width(360.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xEE141519)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, primaryColor.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(primaryColor.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Navigation,
                                        contentDescription = "Turn icon",
                                        tint = primaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = currentNavInstruction!!,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val distStr = if (distanceToTurnMeters > 1000) {
                                        String.format(Locale.getDefault(), "In %.1f km", distanceToTurnMeters / 1000.0)
                                    } else {
                                        "In $distanceToTurnMeters m"
                                    }
                                    Text(
                                        text = distStr,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Consolidated top-right floating map controls
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Button 1: Layout toggle (Minimal Mode / Grid Dashboard)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .clickable { isMinimalMode = !isMinimalMode }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isMinimalMode) Icons.Default.GridView else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Layout",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Button 2: Map style toggle (HUD / satellite)
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .clickable { mapHUDViewMode = !mapHUDViewMode }
                                .padding(10.dp)
                        ) {
                            Icon(
                                imageVector = if (mapHUDViewMode) Icons.Default.DirectionsCar else Icons.Default.Map,
                                contentDescription = "Map Style Toggle",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Navigation Overlay Card (Glassmorphism design)
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .width(260.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = "Nav direction",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (mapMarkers.isNotEmpty()) "Navigate towards destination" else "Route active",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (mapMarkers.isNotEmpty()) mapMarkers.first().title else "Following " + vehicleState.currentRoadName,
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val etaTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + 900000))
                            Text("ETA: $etaTime", color = Color.White, fontSize = 12.sp)
                            Text("${Math.round((12.4 + (vehicleState.tripDistanceKm % 5.0)) * 10.0) / 10.0} km remaining", color = Color.Gray, fontSize = 11.sp)
                        }
                    }

                    // Floating GPS Search button & Speed Limit Indicator
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed Limit sign layout
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.White, RoundedCornerShape(10.dp))
                                .border(3.dp, Color.Red, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "80",
                                color = Color.Black,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))

                        // Category Quick Action Quick-Launch Button (triggers Coffee marker)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .clickable { viewModel.triggerQuickActionMarkers("coffee") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocalCafe, contentDescription = "Coffee Finder", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Search Address box
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(160.dp)
                                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                                .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                                .clickable { showSearchDialog = true }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Search...", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                    }
                    
                    // Current Road Label banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(vehicleState.currentRoadName.uppercase(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }

                    // Floating Speed and Music Cards for Minimal HUD Mode overlay
                    if (isMinimalMode) {
                        // Floating speedometer (bottom left)
                        Card(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                                .width(130.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${vehicleState.currentSpeedKmh}",
                                    color = Color.White,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Text(
                                    text = speedUnit,
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Floating music player widget overlay (bottom right)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp)
                                .width(320.dp)
                        ) {
                            SpotifyWidgetHostView(
                                title = currentTrack.title,
                                artist = if (!isNotificationAccessGranted) "Tap to link Spotify" else currentTrack.artist,
                                isPlaying = currentTrack.isPlaying,
                                albumArtBitmap = bitmap,
                                onPlayPauseClick = { viewModel.playPauseMusic() },
                                onPreviousClick = { viewModel.skipPrevious() },
                                onNextClick = { viewModel.skipNext() },
                                onWidgetClick = { showPermissionDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(84.dp)
                            )
                        }
                    }
                }

                // CENTER PANEL: Instrument Cluster & Rotating Compass
                if (!isMinimalMode) {
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                    // Clock and Date Header
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                        val dateString = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date())
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = dateString.uppercase(),
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Live Speed Cluster
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { viewModel.simulateIncomingCall() }
                    ) {
                        Text(
                            text = "${vehicleState.currentSpeedKmh}",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Light,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 80.sp
                        )
                        Text(
                            text = speedUnit,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Color.Gray
                        )
                    }

                    // Animated Rotating Compass Dial
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(135.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Circular compass dial drawn on Canvas
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .graphicsLayer(rotationZ = -vehicleState.compassHeadingDegrees),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val r = size.minDimension / 2
                                    val center = Offset(size.width / 2, size.height / 2)
                                    
                                    // Outer ring
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.05f),
                                        radius = r,
                                        style = Stroke(width = 2f)
                                    )
                                    
                                    // Cardinal Ticks
                                    drawLine(Color.Red, center - Offset(0f, r), center - Offset(0f, r - 8f), strokeWidth = 3f) // N
                                    drawLine(Color.White.copy(alpha = 0.4f), center + Offset(0f, r), center + Offset(0f, r - 8f), strokeWidth = 2f) // S
                                    drawLine(Color.White.copy(alpha = 0.4f), center - Offset(r, 0f), center - Offset(r - 8f, 0f), strokeWidth = 2f) // W
                                    drawLine(Color.White.copy(alpha = 0.4f), center + Offset(r, 0f), center + Offset(r - 8f, 0f), strokeWidth = 2f) // E
                                }
                                Text("N", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.align(Alignment.TopCenter))
                                Text("S", color = Color.Gray, fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomCenter))
                            }

                            // Telemetry Stats column
                            Column(
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("HEADING: ${vehicleState.compassDirection} • ${vehicleState.compassHeadingDegrees.toInt()}°", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("BAT: ${vehicleState.batteryPercentage}%", color = if (vehicleState.batteryPercentage < 20) Color.Red else Color.Green, fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TRIP: ${vehicleState.tripDistanceKm} km", color = Color.Gray, fontSize = 11.sp)
                                    Text("TIME: ${vehicleState.drivingTimeSeconds / 60}m", color = Color.Gray, fontSize = 11.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("TEMP: ${vehicleState.weatherTempCelsius}°${if (speedUnit == "KMH") "C" else "F"}", color = Color.White, fontSize = 11.sp)
                                    Text("NET: ${vehicleState.networkStrength}", color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

                // RIGHT PANEL: Premium Spotify Player with Equalizer & blurred background
                if (!isMinimalMode) {
                    Box(
                        modifier = Modifier
                            .weight(4.2f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    ) {
                    // Album art blurred background overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .blur(40.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SpotifyWidgetHostView(
                            title = currentTrack.title,
                            artist = if (!isNotificationAccessGranted) "Tap to link Spotify" else currentTrack.artist,
                            isPlaying = currentTrack.isPlaying,
                            albumArtBitmap = bitmap,
                            onPlayPauseClick = { viewModel.playPauseMusic() },
                            onPreviousClick = { viewModel.skipPrevious() },
                            onNextClick = { viewModel.skipNext() },
                            onWidgetClick = { showPermissionDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                        )
                    }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTTOM BAR: Persistent Automotive Dock navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dock Drive Home
                IconButton(onClick = { }) {
                    Icon(Icons.Default.DirectionsCar, "Drive", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                // Quick actions trigger map pins
                IconButton(onClick = { viewModel.triggerQuickActionMarkers("coffee") }) {
                    Icon(Icons.Default.LocalCafe, "Coffee Finder", tint = Color.White, modifier = Modifier.size(24.dp))
                }
                // Dock Button StandBy Desk Mode
                IconButton(onClick = { navController.navigate(Screen.DeskMode.route) }) {
                    Icon(Icons.Default.Home, "Desk Standby Mode", tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
                // Floating assistant speech mic trigger
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .clickable { viewModel.startVoiceAssistant() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, "Assistant", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                // Dock Phone Keypad dialing
                IconButton(onClick = { showPhoneKeypadDialog = true }) {
                    Icon(Icons.Default.Dialpad, "Phone Keypad Dialer", tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
                // Dock Settings configuration
                IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                    Icon(Icons.Default.Settings, "Settings Manager", tint = Color.Gray, modifier = Modifier.size(24.dp))
                }
            }
        }

        // VOICE ASSISTANT BREATHING FLOATING HUD OVERLAY
        AnimatedVisibility(
            visible = assistantActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val micScale by infiniteTransition.animateFloat(
                initialValue = 0.92f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(750),
                    repeatMode = RepeatMode.Reverse
                )
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.92f)),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(360.dp)
                    .padding(16.dp)
                    .shadow(16.dp, RoundedCornerShape(32.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Listening",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer(scaleX = micScale, scaleY = micScale)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "VOICE COMMAND ASSISTANT",
                        color = Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = assistantSpeechText,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // ACTIVE AND INCOMING PHONE CALL FULL SCREEN OVERLAY HUD
        AnimatedVisibility(
            visible = activeCallState != CallState.IDLE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(440.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16171A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Caller Profile initial avatar
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = callerName.take(2).uppercase(),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = callerName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = if (activeCallState == CallState.INCOMING) "Incoming Call..." else {
                                val minutes = activeCallTimeSeconds / 60
                                val seconds = activeCallTimeSeconds % 60
                                String.format("Connected • %02d:%02d", minutes, seconds)
                            },
                            color = if (activeCallState == CallState.INCOMING) MaterialTheme.colorScheme.primary else Color.Gray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        if (activeCallState == CallState.INCOMING) {
                            // Incoming Accept Decline Layout
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { viewModel.rejectCall() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                                    shape = CircleShape,
                                    modifier = Modifier.height(54.dp).width(140.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CallEnd, contentDescription = "Decline", tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Decline", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.acceptCall() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                    shape = CircleShape,
                                    modifier = Modifier.height(54.dp).width(140.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Call, contentDescription = "Accept", tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Accept", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Active Call Controls (Mute, Speaker, End)
                            var isMuted by remember { mutableStateOf(false) }
                            var isSpeakerOn by remember { mutableStateOf(false) }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    // Mute
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (isMuted) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                            .clickable { isMuted = !isMuted },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Mute", color = if (isMuted) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    // Speakerphone
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(CircleShape)
                                            .background(if (isSpeakerOn) Color.White.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                            .clickable { isSpeakerOn = !isSpeakerOn },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Speaker", color = if (isSpeakerOn) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.height(28.dp))
                                Button(
                                    onClick = { viewModel.endCall() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                                    shape = CircleShape,
                                    modifier = Modifier.height(54.dp).fillMaxWidth(0.8f)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("End Call", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // SPOTIFY AUDIO PLAYBACK DEVICES DIALOG
        AnimatedVisibility(
            visible = showDevicesDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showDevicesDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(360.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18191C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Connect to a device", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        playbackDevices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (device == selectedPlaybackDevice) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable {
                                        viewModel.setPlaybackDevice(device)
                                        showDevicesDialog = false
                                    }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(device, color = if (device == selectedPlaybackDevice) MaterialTheme.colorScheme.primary else Color.White, fontSize = 14.sp)
                                if (device == selectedPlaybackDevice) {
                                    Icon(Icons.Default.Star, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // FULL DIALER OUTGOING CALL KEYPAD OVERLAY
        AnimatedVisibility(
            visible = showPhoneKeypadDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showPhoneKeypadDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(550.dp)
                        .height(380.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16171A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        // Left Keypad Numbers Column
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Dialpad Number Display
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = dialpadText.ifEmpty { "Enter Number..." },
                                    color = if (dialpadText.isEmpty()) Color.DarkGray else Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (dialpadText.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "Backspace",
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clickable { viewModel.clearDialpad() }
                                    )
                                }
                            }

                            // Keypad numbers grid
                            val keys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("*", "0", "#")
                            )
                            keys.forEach { rowKeys ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rowKeys.forEach { key ->
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .clickable { viewModel.pressDialpadKey(key) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(key, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            
                            // Call Action Launch
                            Button(
                                onClick = {
                                    viewModel.makeCall(dialpadText)
                                    showPhoneKeypadDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Dial Out", tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Call", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Right Dialer Contacts & Recents Column
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("FAVORITES", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(favoriteContacts) { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.White.copy(alpha = 0.02f))
                                            .clickable {
                                                viewModel.makeCall(contact)
                                                showPhoneKeypadDialog = false
                                            }
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(contact, color = Color.White, fontSize = 13.sp)
                                        Icon(Icons.Default.Star, contentDescription = "Fav", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                            
                            Text("RECENT CALLS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(recentCalls) { log ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = "History", tint = Color.DarkGray, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(log, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // GEOGRAPHIC PLACES SEARCH DIALOG
        AnimatedVisibility(
            visible = showSearchDialog,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showSearchDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .width(440.dp)
                        .padding(16.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18191C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Search Destination",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        androidx.compose.material3.OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Enter address or landmark...", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.Gray
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { showSearchDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.searchPlaces(searchQuery)
                                        showSearchDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).height(46.dp)
                            ) {
                                Text("Navigate", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // SPOTIFY CONNECTIVITY AND INSTRUCTIONAL DIALOG
        if (showPermissionDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showPermissionDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.width(420.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16171A)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Spotify & Media Linking",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "To show real-time Spotify or system player cover art and track status, please complete the following steps:\n\n" +
                                   "1. Click **Grant Listener Access** below, find **Fontainment Media Integration** and toggle it ON.\n" +
                                   "2. If Spotify is closed or not playing, click **Launch Spotify** to trigger background sessions.",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Start
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    showPermissionDialog = false
                                    try {
                                        val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Failed to open settings", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("Grant Listener Access", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = {
                                    showPermissionDialog = false
                                    try {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        } else {
                                            Toast.makeText(context, "Spotify is not installed on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Unable to start Spotify", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Text("Launch Spotify App", color = Color.White)
                            }
                            
                            Button(
                                onClick = { showPermissionDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Text("Cancel", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}
