package com.fontainment.app.presentation.desk

import android.widget.Toast
import com.fontainment.app.presentation.common.SpotifyWidgetHostView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fontainment.app.presentation.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DeskModeScreen(
    navController: NavController,
    viewModel: DeskViewModel
) {
    val context = LocalContext.current
    val burnInOffset by viewModel.burnInOffset.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val batteryPercentage by viewModel.batteryPercentage.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val ramUsagePercent by viewModel.ramUsagePercent.collectAsState()
    val deviceTemp by viewModel.deviceTemp.collectAsState()

    val hasActiveRoute by viewModel.hasActiveRoute.collectAsState()
    val destinationName by viewModel.destinationName.collectAsState()
    val currentNavInstruction by viewModel.currentNavInstruction.collectAsState()
    val distanceToTurnMeters by viewModel.distanceToTurnMeters.collectAsState()

    // Real-time notification listener link checks
    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val activePlayerPackage by viewModel.activePlayerPackage.collectAsState()

    // Custom dialog to guide notification setup and Spotify linking
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Decode Spotify cached album art if present
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

    // View layout configurations: 0: Split Grid (Hectic), 1: Minimal Apple Standby (Clean, default!)
    var isMinimalMode by remember { mutableStateOf(true) }
    var minimalPageIndex by remember { mutableStateOf(0) } // 0: Big Clock, 1: Media Player, 2: Map HUD

    // Smooth sweeping clock rendering frame ticker (60 FPS)
    var frameTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis {
                frameTime = System.currentTimeMillis()
            }
        }
    }

    val cal = Calendar.getInstance().apply { timeInMillis = frameTime }
    val hour = cal.get(Calendar.HOUR)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    
    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(frameTime))
    val dateString = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date(frameTime))

    // Interactive StandBy layouts cycling for the Grid panel
    var leftPanelLayoutIndex by remember { mutableStateOf(0) }
    var rightCard1Index by remember { mutableStateOf(0) }
    var rightCard2Index by remember { mutableStateOf(0) }

    // Shifting ambient background glow animation
    val infiniteTransition = rememberInfiniteTransition()
    val ambientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val ambientColor1 = Color(0xFF0F0C1B)
    val ambientColor2 = Color(0xFF07121C)
    val ambientColor3 = Color(0xFF080D09)

    // OLED efficient gradient mesh background
    val ambientBrush = Brush.linearGradient(
        colors = listOf(ambientColor1, ambientColor2, ambientColor3),
        start = Offset(
            (Math.sin(Math.toRadians(ambientOffset.toDouble())) * 400 + 400).toFloat(),
            (Math.cos(Math.toRadians(ambientOffset.toDouble())) * 400 + 400).toFloat()
        ),
        end = Offset(
            (Math.sin(Math.toRadians((ambientOffset + 180f).toDouble())) * 400 + 400).toFloat(),
            (Math.cos(Math.toRadians((ambientOffset + 180f).toDouble())) * 400 + 400).toFloat()
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Slowly shifting background glow (Only visible or bright in media player and grid modes)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientBrush)
                .blur(90.dp)
                .graphicsLayer(alpha = if (isMinimalMode && minimalPageIndex == 0) 0.15f else 0.45f)
        )

        // Screen overlay content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp)
        ) {
            // TOP FLOATING CONTROLS HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button
                IconButton(
                    onClick = { navController.navigate(Screen.DriveMode.route) },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                // Page Indicator Dots (Only in Minimal Mode)
                if (isMinimalMode) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..2) {
                            Box(
                                modifier = Modifier
                                    .size(if (i == minimalPageIndex) 8.dp else 6.dp)
                                    .clip(CircleShape)
                                    .background(if (i == minimalPageIndex) Color.Red else Color.White.copy(alpha = 0.3f))
                                    .clickable { minimalPageIndex = i }
                            )
                        }
                    }
                }

                // Layout Toggler
                IconButton(
                    onClick = { isMinimalMode = !isMinimalMode },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isMinimalMode) Icons.Default.GridView else Icons.Default.Fullscreen,
                        contentDescription = "Toggle Grid",
                        tint = Color.White
                    )
                }
            }

            // CENTRAL CROSSFADE CONTAINER
            Crossfade(
                targetState = isMinimalMode,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, bottom = 12.dp)
            ) { minimal ->
                if (minimal) {
                    // LAYOUT 1: APPLE STANDBY INSPIRED MINIMAL MODE
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable {
                                // Tap left/right edges to cycle pages easily
                                minimalPageIndex = (minimalPageIndex + 1) % 3
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Crossfade(targetState = minimalPageIndex) { page ->
                            when (page) {
                                0 -> {
                                    // PAGE 0: Apple Standby Huge Orange Clock
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = timeString,
                                            fontSize = 145.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFF4500), // Rich glowing orange-red
                                            lineHeight = 145.sp,
                                            letterSpacing = (-4).sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = dateString.uppercase(),
                                            color = Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 2.sp
                                        )
                                    }
                                }
                                1 -> {
                                    // PAGE 1: Spotify AppWidget Host (Falls back to custom player if Spotify not installed)
                                    SpotifyWidgetHostView(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 24.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Left Side Album Art Cover Poster
                                            Box(
                                                modifier = Modifier
                                                    .size(170.dp)
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(Color.White.copy(alpha = 0.03f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                                    .clickable {
                                                        try {
                                                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                                                            if (launchIntent != null) {
                                                                context.startActivity(launchIntent)
                                                            } else {
                                                                Toast.makeText(context, "Spotify is not installed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (bitmap != null) {
                                                    Image(
                                                        bitmap = bitmap.asImageBitmap(),
                                                        contentDescription = "Cover Art",
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0xFF1DB954)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = Color.Black, modifier = Modifier.size(54.dp))
                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text("LAUNCH SPOTIFY", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(36.dp))

                                            // Right Side Metadata & Controls
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight(),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = currentTrack.title,
                                                    color = Color.White,
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = currentTrack.artist.uppercase(),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(14.dp))

                                                // Seek slider and track times
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    val progress = if (currentTrack.durationMs > 0) currentTrack.progressMs.toFloat() / currentTrack.durationMs.toFloat() else 0f
                                                    Slider(
                                                        value = progress,
                                                        onValueChange = { viewModel.seekTo((it * currentTrack.durationMs).toLong()) },
                                                        colors = SliderDefaults.colors(
                                                            thumbColor = Color.White,
                                                            activeTrackColor = Color.White.copy(alpha = 0.6f),
                                                            inactiveTrackColor = Color.White.copy(alpha = 0.12f)
                                                        ),
                                                        modifier = Modifier.height(18.dp)
                                                    )
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        val elapsedSec = currentTrack.progressMs / 1000
                                                        val elapsed = String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)
                                                        val remaining = "-" + SimpleDateFormat("m:ss", Locale.getDefault()).format(Date(currentTrack.durationMs - currentTrack.progressMs))
                                                        Text(elapsed, color = Color.DarkGray, fontSize = 10.sp)
                                                        Text(remaining, color = Color.DarkGray, fontSize = 10.sp)
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                // Premium buttons controls
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(onClick = { showPermissionDialog = true }) {
                                                        Icon(
                                                            imageVector = if (!isNotificationAccessGranted) Icons.Default.Info else Icons.Default.VolumeUp,
                                                            contentDescription = "Access check",
                                                            tint = if (!isNotificationAccessGranted) Color.Red else Color.Gray,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(onClick = { viewModel.skipPrevious() }) {
                                                            Icon(Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White, modifier = Modifier.size(30.dp))
                                                        }
                                                        IconButton(
                                                            onClick = { viewModel.playPauseMusic() },
                                                            modifier = Modifier
                                                                .size(46.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.08f))
                                                        ) {
                                                            Icon(
                                                                imageVector = if (currentTrack.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                contentDescription = "PlayPause",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(28.dp)
                                                            )
                                                        }
                                                        IconButton(onClick = { viewModel.skipNext() }) {
                                                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(30.dp))
                                                        }
                                                    }

                                                    // Notification link status label
                                                    if (!isNotificationAccessGranted) {
                                                        Text(
                                                            text = "LINK",
                                                            color = Color.Red,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.clickable { showPermissionDialog = true }
                                                        )
                                                    } else {
                                                        Text(
                                                            text = activePlayerPackage?.substringAfterLast(".")?.uppercase() ?: "REAL",
                                                            color = Color.Gray,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // PAGE 2: World Map / Live Navigation HUD
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        if (hasActiveRoute) {
                                            // Navigation HUD Active
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Navigation,
                                                    contentDescription = "Nav direction",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(54.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = currentNavInstruction ?: "Drive to destination",
                                                    color = Color.White,
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                val distStr = if (distanceToTurnMeters > 1000) {
                                                    String.format(Locale.getDefault(), "In %.1f km", distanceToTurnMeters / 1000.0)
                                                } else {
                                                    "In $distanceToTurnMeters m"
                                                }
                                                Text(
                                                    text = "$distStr to $destinationName",
                                                    color = Color.Gray,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else {
                                            // Dotted World Map Graphic (Red outline matching Image 3)
                                            Canvas(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .graphicsLayer(alpha = 0.35f)
                                            ) {
                                                val worldDots = listOf(
                                                    Pair(0.1f, 0.2f), Pair(0.15f, 0.2f), Pair(0.2f, 0.2f), Pair(0.25f, 0.25f), Pair(0.3f, 0.28f), Pair(0.35f, 0.3f),
                                                    Pair(0.12f, 0.25f), Pair(0.18f, 0.28f), Pair(0.22f, 0.3f), Pair(0.28f, 0.32f), Pair(0.32f, 0.35f),
                                                    Pair(0.15f, 0.3f), Pair(0.2f, 0.33f), Pair(0.25f, 0.36f), Pair(0.28f, 0.4f),
                                                    Pair(0.18f, 0.38f), Pair(0.22f, 0.42f), Pair(0.26f, 0.46f),
                                                    Pair(0.42f, 0.12f), Pair(0.45f, 0.15f), Pair(0.48f, 0.16f),
                                                    Pair(0.32f, 0.55f), Pair(0.35f, 0.58f), Pair(0.37f, 0.62f),
                                                    Pair(0.34f, 0.65f), Pair(0.36f, 0.7f), Pair(0.38f, 0.73f), Pair(0.4f, 0.76f),
                                                    Pair(0.38f, 0.8f), Pair(0.39f, 0.84f), Pair(0.4f, 0.88f),
                                                    Pair(0.5f, 0.48f), Pair(0.52f, 0.52f), Pair(0.54f, 0.55f), Pair(0.56f, 0.58f),
                                                    Pair(0.52f, 0.62f), Pair(0.53f, 0.66f), Pair(0.55f, 0.7f), Pair(0.57f, 0.74f), Pair(0.58f, 0.78f),
                                                    Pair(0.48f, 0.25f), Pair(0.5f, 0.28f), Pair(0.52f, 0.26f), Pair(0.54f, 0.3f), Pair(0.56f, 0.32f),
                                                    Pair(0.50f, 0.34f), Pair(0.53f, 0.36f), Pair(0.56f, 0.38f),
                                                    Pair(0.58f, 0.42f), Pair(0.62f, 0.4f), Pair(0.65f, 0.38f), Pair(0.68f, 0.35f),
                                                    Pair(0.60f, 0.46f), Pair(0.64f, 0.44f), Pair(0.68f, 0.42f), Pair(0.72f, 0.4f),
                                                    Pair(0.76f, 0.38f), Pair(0.8f, 0.36f), Pair(0.84f, 0.34f), Pair(0.88f, 0.32f),
                                                    Pair(0.66f, 0.48f), Pair(0.70f, 0.46f), Pair(0.74f, 0.44f), Pair(0.78f, 0.42f),
                                                    Pair(0.82f, 0.4f), Pair(0.86f, 0.38f), Pair(0.9f, 0.36f),
                                                    Pair(0.70f, 0.52f), Pair(0.72f, 0.54f), Pair(0.71f, 0.57f),
                                                    Pair(0.78f, 0.48f), Pair(0.82f, 0.46f), Pair(0.86f, 0.45f), Pair(0.9f, 0.44f),
                                                    Pair(0.80f, 0.52f), Pair(0.84f, 0.5f), Pair(0.88f, 0.49f),
                                                    Pair(0.84f, 0.58f), Pair(0.87f, 0.6f), Pair(0.9f, 0.62f),
                                                    Pair(0.86f, 0.72f), Pair(0.89f, 0.7f), Pair(0.92f, 0.71f),
                                                    Pair(0.88f, 0.76f), Pair(0.91f, 0.75f), Pair(0.94f, 0.76f),
                                                    Pair(0.90f, 0.8f), Pair(0.93f, 0.81f)
                                                )

                                                worldDots.forEach { dot ->
                                                    drawCircle(
                                                        color = Color.Red,
                                                        radius = 3.dp.toPx(),
                                                        center = Offset(dot.first * size.width, dot.second * size.height)
                                                    )
                                                }

                                                // Draw a pulsing red city indicator for New Delhi
                                                drawCircle(
                                                    color = Color.Red,
                                                    radius = 8.dp.toPx(),
                                                    center = Offset(0.71f * size.width, 0.54f * size.height)
                                                )
                                            }

                                            // Text Info Overlays (New Delhi Clock, matching Image 3)
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(24.dp)
                                            ) {
                                                Text(
                                                    text = "New Delhi",
                                                    color = Color.Red,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = timeString,
                                                    color = Color.Red,
                                                    fontSize = 42.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // LAYOUT 2: SYSTEM SPLIT GRID DASHBOARD (Hectic Mode)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // LEFT PANEL: Customizable Large Clock / Watch Face Area
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(28.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                                .clickable { leftPanelLayoutIndex = (leftPanelLayoutIndex + 1) % 4 }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(targetState = leftPanelLayoutIndex) { layoutIndex ->
                                when (layoutIndex) {
                                    0 -> {
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text(
                                                text = timeString,
                                                fontSize = 94.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                lineHeight = 94.sp,
                                                letterSpacing = (-3).sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = dateString.uppercase(),
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            )
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CloudQueue, contentDescription = "Weather", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("22°C • Clear", color = Color.Gray, fontSize = 12.sp)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery", tint = if (isCharging) Color.Green else Color.Gray, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("$batteryPercentage%", color = Color.Gray, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                    1 -> {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(modifier = Modifier.size(140.dp)) {
                                                val dialPrimaryColor = MaterialTheme.colorScheme.primary
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val r = size.minDimension / 2
                                                    val center = Offset(size.width / 2, size.height / 2)
                                                    drawCircle(Color.White.copy(alpha = 0.05f), radius = r)
                                                    drawCircle(dialPrimaryColor, radius = r, style = Stroke(width = 2.dp.toPx()))
                                                    
                                                    // Dial lines
                                                    for (angle in 0..11) {
                                                        val rad = Math.toRadians((angle * 30).toDouble())
                                                        val start = Offset(
                                                            (center.x + (r - 10.dp.toPx()) * Math.sin(rad)).toFloat(),
                                                            (center.y - (r - 10.dp.toPx()) * Math.cos(rad)).toFloat()
                                                        )
                                                        val end = Offset(
                                                            (center.x + r * Math.sin(rad)).toFloat(),
                                                            (center.y - r * Math.cos(rad)).toFloat()
                                                        )
                                                        drawLine(Color.White.copy(alpha = 0.3f), start, end, strokeWidth = 2.dp.toPx())
                                                    }
                                                    
                                                    // Hour hand
                                                    val hrRad = Math.toRadians((hour * 30 + minute * 0.5).toDouble())
                                                    drawLine(
                                                        Color.White,
                                                        center,
                                                        Offset((center.x + r * 0.5 * Math.sin(hrRad)).toFloat(), (center.y - r * 0.5 * Math.cos(hrRad)).toFloat()),
                                                        strokeWidth = 4.dp.toPx()
                                                    )
                                                    
                                                    // Minute hand
                                                    val minRad = Math.toRadians((minute * 6).toDouble())
                                                    drawLine(
                                                        Color.White.copy(alpha = 0.8f),
                                                        center,
                                                        Offset((center.x + r * 0.7 * Math.sin(minRad)).toFloat(), (center.y - r * 0.7 * Math.cos(minRad)).toFloat()),
                                                        strokeWidth = 3.dp.toPx()
                                                    )
                                                    
                                                    // Center pin
                                                    drawCircle(dialPrimaryColor, radius = 5.dp.toPx())
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text("WORLD TIME ZONES", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            listOf(
                                                Pair("New York", "America/New_York"),
                                                Pair("London", "Europe/London"),
                                                Pair("Tokyo", "Asia/Tokyo")
                                            ).forEach { zone ->
                                                sdf.timeZone = TimeZone.getTimeZone(zone.second)
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(zone.first, color = Color.LightGray, fontSize = 13.sp)
                                                    Text(sdf.format(Date(frameTime)), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                    3 -> {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text("QUOTE OF THE DAY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                text = "\"The true engine of premium software design lies in simplicity and micro-details.\"",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                textAlign = TextAlign.Start
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("- Fontainment OS Design Lead", color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // RIGHT PANEL: Card 1 (Music) and Card 2 (System Stats/Nav)
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // CARD 1: Music Controls
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clickable { rightCard1Index = (rightCard1Index + 1) % 3 },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111215)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                    Crossfade(targetState = rightCard1Index) { index ->
                                        when (index) {
                                            0 -> {
                                                SpotifyWidgetHostView(
                                                    modifier = Modifier.fillMaxSize()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("SPOTIFY PLAYER", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                            Icon(Icons.Default.MusicNote, contentDescription = "Music", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                        }
                                                        
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(54.dp)
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(Color.White.copy(alpha = 0.03f))
                                                                    .clickable {
                                                                        try {
                                                                            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
                                                                            if (launchIntent != null) {
                                                                                context.startActivity(launchIntent)
                                                                            } else {
                                                                                Toast.makeText(context, "Spotify is not installed", Toast.LENGTH_SHORT).show()
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            e.printStackTrace()
                                                                        }
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                if (bitmap != null) {
                                                                    Image(
                                                                        bitmap = bitmap.asImageBitmap(),
                                                                        contentDescription = "Cover",
                                                                        contentScale = ContentScale.Crop,
                                                                        modifier = Modifier.fillMaxSize()
                                                                    )
                                                                } else {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .fillMaxSize()
                                                                            .background(Color(0xFF1DB954)),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(currentTrack.title.take(1), color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(currentTrack.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                Text(currentTrack.artist, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            }
                                                        }

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            val progress = if (currentTrack.durationMs > 0) currentTrack.progressMs.toFloat() / currentTrack.durationMs.toFloat() else 0f
                                                            Box(modifier = Modifier.weight(1f).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                                                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                                            }
                                                            Spacer(modifier = Modifier.width(10.dp))
                                                            Icon(
                                                                imageVector = if (currentTrack.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                contentDescription = "Play",
                                                                tint = Color.White,
                                                                modifier = Modifier.size(20.dp).clickable { viewModel.playPauseMusic() }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("CALENDAR EVENTS", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = Color.Gray, modifier = Modifier.size(12.dp))
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("• 09:30 AM: Weekly Standup", color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text("• 12:45 PM: Lunch Meeting", color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                }
                                            }
                                            2 -> {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("WEATHER", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                        Icon(Icons.Default.WbSunny, contentDescription = "Weather", tint = Color.Yellow, modifier = Modifier.size(12.dp))
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text("22°", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column {
                                                            Text("CLEAR SKY", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            Text("Wind: 8 km/h", color = Color.Gray, fontSize = 9.sp)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // CARD 2: Live Nav / System Stats
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .clickable { rightCard2Index = (rightCard2Index + 1) % 3 },
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF111215)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                            ) {
                                Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                                    Crossfade(targetState = rightCard2Index) { index ->
                                        when (index) {
                                            0 -> {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("SYSTEM STATUS", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Column {
                                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                Text("RAM CAPACITY", color = Color.Gray, fontSize = 10.sp)
                                                                Text("$ramUsagePercent%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                            Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                                                Box(modifier = Modifier.fillMaxWidth(ramUsagePercent / 100f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            1 -> {
                                                Column(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("BATTERY METRICS", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                        Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery", tint = Color.Green, modifier = Modifier.size(12.dp))
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text("BATTERY LEVEL", color = Color.Gray, fontSize = 11.sp)
                                                            Text("$batteryPercentage%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                            2 -> {
                                                if (hasActiveRoute) {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        verticalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text("LIVE NAVIGATION", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                            Icon(Icons.Default.Navigation, contentDescription = "Nav", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                                        }
                                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                            Text(destinationName ?: "Destination", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text(currentNavInstruction ?: "Driving", color = Color.LightGray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        val distStr = if (distanceToTurnMeters > 1000) {
                                                            String.format(Locale.getDefault(), "In %.1f km", distanceToTurnMeters / 1000.0)
                                                        } else {
                                                            "In $distanceToTurnMeters m"
                                                        }
                                                        Text(distStr, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .clip(RoundedCornerShape(16.dp))
                                                            .background(Color.White.copy(alpha = 0.02f))
                                                            .padding(10.dp)
                                                    ) {
                                                        Text("No active route", color = Color.Gray, fontSize = 11.sp, modifier = Modifier.align(Alignment.Center))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // OLED Protection text indicator
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("OLED PROTECTION ACTIVE", color = Color.DarkGray, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
