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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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

    // View layout configurations collected from ViewModel
    val uiMode by viewModel.uiMode.collectAsState()
    val clockTheme by viewModel.clockTheme.collectAsState()
    val backgroundStyle by viewModel.backgroundStyle.collectAsState()

    var minimalPageIndex by remember { mutableStateOf(0) } // 0: Clock, 1: Media Player, 2: Map HUD
    var showSettingsDrawer by remember { mutableStateOf(false) }

    // Smooth clock rendering frame ticker
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
        // Dynamic Backdrop Rendering
        when (backgroundStyle) {
            BackgroundStyle.DYNAMIC_BLUR -> {
                if (uiMode == UiMode.MINIMAL && minimalPageIndex == 1 && bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(80.dp)
                            .graphicsLayer(alpha = 0.45f)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ambientBrush)
                            .blur(90.dp)
                            .graphicsLayer(alpha = if (uiMode == UiMode.MINIMAL && minimalPageIndex == 0) 0.15f else 0.45f)
                    )
                }
            }
            BackgroundStyle.CHARCOAL_GREY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF16171A))
                )
            }
            BackgroundStyle.PITCH_BLACK -> {
                // OLED battery saving absolute black
            }
            BackgroundStyle.SUNSET_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE94057), Color(0xFFF27121), Color(0xFF8A2387))
                            )
                        )
                )
            }
            BackgroundStyle.OCEAN_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00c6ff), Color(0xFF0072ff))
                            )
                        )
                )
            }
            BackgroundStyle.FOREST_GRADIENT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF11998e), Color(0xFF38ef7d))
                            )
                        )
                )
            }
        }

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
                if (uiMode == UiMode.MINIMAL) {
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

                // Settings Cog Customization Dialog Trigger
                IconButton(
                    onClick = { showSettingsDrawer = true },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Customise layout",
                        tint = Color.White
                    )
                }
            }

            // CENTRAL CROSSFADE CONTAINER
            Crossfade(
                targetState = uiMode,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp, bottom = 12.dp)
            ) { mode ->
                when (mode) {
                    UiMode.MINIMAL -> {
                        // LAYOUT 1: APPLE STANDBY INSPIRED MINIMAL MODE
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    minimalPageIndex = (minimalPageIndex + 1) % 3
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Crossfade(targetState = minimalPageIndex) { page ->
                                when (page) {
                                    0 -> {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            DeskClock(
                                                hour = hour,
                                                minute = minute,
                                                second = second,
                                                clockTheme = clockTheme
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = if (clockTheme == ClockTheme.RETRO_GREEN) dateString.lowercase() else dateString.uppercase(),
                                                color = if (clockTheme == ClockTheme.RETRO_GREEN) Color(0xFF39FF14) else Color.LightGray,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            )
                                        }
                                    }
                                    1 -> {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 40.dp, vertical = 20.dp),
                                            horizontalArrangement = Arrangement.spacedBy(48.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(200.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color.White.copy(alpha = 0.03f))
                                                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
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
                                                        Icon(
                                                            Icons.Default.MusicNote,
                                                            contentDescription = "Music",
                                                            tint = Color.Black,
                                                            modifier = Modifier.size(56.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                                        contentDescription = "Volume status",
                                                        tint = Color.White.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    if (!isNotificationAccessGranted) {
                                                        Text(
                                                            text = "TAP TO LINK SPOTIFY",
                                                            color = Color(0xFF1DB954),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp,
                                                            modifier = Modifier.clickable { showPermissionDialog = true }
                                                        )
                                                    } else {
                                                        Text(
                                                            text = activePlayerPackage?.substringAfterLast(".")?.uppercase() ?: "SPOTIFY",
                                                            color = Color.White.copy(alpha = 0.4f),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            letterSpacing = 1.sp
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Column {
                                                    Text(
                                                        text = if (currentTrack.title.isBlank() || currentTrack.title == "Not Playing") "Not Playing" else currentTrack.title,
                                                        color = Color.White,
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = if (currentTrack.artist.isBlank() || currentTrack.artist == "Tap to connect Spotify") "Launch Spotify to play music" else currentTrack.artist,
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Normal,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.height(16.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    IconButton(
                                                        onClick = { viewModel.skipPrevious() },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.SkipPrevious,
                                                            contentDescription = "Prev",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(24.dp))
                                                    IconButton(
                                                        onClick = { viewModel.playPauseMusic() },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (currentTrack.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                            contentDescription = "Play/Pause",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(38.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(24.dp))
                                                    IconButton(
                                                        onClick = { viewModel.skipNext() },
                                                        modifier = Modifier.size(48.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.SkipNext,
                                                            contentDescription = "Next",
                                                            tint = Color.White,
                                                            modifier = Modifier.size(32.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(12.dp))

                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    val progress = if (currentTrack.durationMs > 0) currentTrack.progressMs.toFloat() / currentTrack.durationMs.toFloat() else 0f
                                                    Slider(
                                                        value = progress,
                                                        onValueChange = { viewModel.seekTo((it * currentTrack.durationMs).toLong()) },
                                                        colors = SliderDefaults.colors(
                                                            thumbColor = Color.White,
                                                            activeTrackColor = Color.White,
                                                            inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                                        ),
                                                        modifier = Modifier.height(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        val elapsedSec = currentTrack.progressMs / 1000
                                                        val elapsed = String.format("%d:%02d", elapsedSec / 60, elapsedSec % 60)
                                                        val durationSec = currentTrack.durationMs / 1000
                                                        val remainingSec = (currentTrack.durationMs - currentTrack.progressMs) / 1000
                                                        val remaining = if (durationSec > 0) String.format("-%d:%02d", remainingSec / 60, remainingSec % 60) else "-0:00"
                                                        Text(elapsed, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                        Text(remaining, color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (hasActiveRoute) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.85f)
                                                        .clip(RoundedCornerShape(24.dp))
                                                        .background(Color.White.copy(alpha = 0.03f))
                                                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                                        .padding(24.dp),
                                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("DRIVING DIRECTIONS", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                        Icon(Icons.Default.Navigation, contentDescription = "Navigation", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                    }
                                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text(destinationName ?: "Destination", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        Text(currentNavInstruction ?: "Live Guidance", color = Color.LightGray, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    }
                                                    val distStr = if (distanceToTurnMeters > 1000) {
                                                        String.format(Locale.getDefault(), "In %.1f km", distanceToTurnMeters / 1000.0)
                                                    } else {
                                                        "In $distanceToTurnMeters m"
                                                    }
                                                    Text(distStr, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(Icons.Default.Map, contentDescription = "Map HUD", tint = Color.Gray, modifier = Modifier.size(48.dp))
                                                    Text("Live Navigation Offline", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                    Text("Google Maps instructions sync automatically here", color = Color.DarkGray, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    UiMode.CUSTOMISED -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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
                                                DeskClock(
                                                    hour = hour,
                                                    minute = minute,
                                                    second = second,
                                                    clockTheme = ClockTheme.VECTOR_ANALOGUE,
                                                    modifier = Modifier.size(150.dp)
                                                )
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
                                                    Pair("Tokyo", "Asia/Tokyo"),
                                                    Pair("Sydney", "Australia/Sydney")
                                                ).forEach { zone ->
                                                    val format = sdf.apply { timeZone = TimeZone.getTimeZone(zone.second) }
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(zone.first, color = Color.LightGray, fontSize = 12.sp)
                                                        Text(format.format(Date(frameTime)), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                        3 -> {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text("SYSTEM HEALTH", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                listOf(
                                                    Triple("RAM UTILITY", "$ramUsagePercent%", ramUsagePercent / 100f),
                                                    Triple("CHIP TEMP", "$deviceTemp°C", deviceTemp / 100f),
                                                    Triple("BATTERY LIFE", "$batteryPercentage%", batteryPercentage / 100f)
                                                ).forEach { metric ->
                                                    Column {
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            Text(metric.first, color = Color.Gray, fontSize = 10.sp)
                                                            Text(metric.second, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                                            Box(modifier = Modifier.fillMaxWidth(metric.third).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(28.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Crossfade(targetState = rightCard1Index) { index ->
                                    when (index) {
                                        0 -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { rightCard1Index = (rightCard1Index + 1) % 3 },
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
                                        1 -> {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { rightCard1Index = (rightCard1Index + 1) % 3 },
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
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text("Next Meeting", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Text("Product Review @ 2:00 PM", color = Color.Gray, fontSize = 11.sp)
                                                }
                                                Text("Calendar synced", color = Color.DarkGray, fontSize = 10.sp)
                                            }
                                        }
                                        2 -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clickable { rightCard1Index = (rightCard1Index + 1) % 3 }
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                    UiMode.CLASSIC -> {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(28.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                DeskClock(
                                    hour = hour,
                                    minute = minute,
                                    second = second,
                                    clockTheme = ClockTheme.VECTOR_ANALOGUE
                                )
                            }
                            
                            Column(
                                modifier = Modifier
                                    .weight(0.9f)
                                    .fillMaxHeight()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("TEMP", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("$deviceTemp°C", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                        val progress = (deviceTemp / 100f).coerceIn(0f, 1f)
                                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White.copy(alpha = 0.6f)))
                                    }
                                }
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("BATTERY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("$batteryPercentage%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                        val progress = (batteryPercentage / 100f).coerceIn(0f, 1f)
                                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(if (isCharging) Color(0xFF39FF14) else Color.White))
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("RAM LIMIT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text("$ramUsagePercent%", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                        val progress = (ramUsagePercent / 100f).coerceIn(0f, 1f)
                                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(Color.White.copy(alpha = 0.6f)))
                                    }
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1.1f)
                                    .fillMaxHeight(),
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
            }

            // OLED Protection Indicator
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

        // SETTINGS CUSTOMISATION DRAWER OVERLAY
        if (showSettingsDrawer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { showSettingsDrawer = false },
                contentAlignment = Alignment.CenterEnd
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16171A)),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Text(
                            text = "Customisation",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // 1. UI MODE SELECTION
                        Column {
                            Text("INTERFACE STYLE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UiMode.values().forEach { mode ->
                                    val isSelected = uiMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                                            .border(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setUiMode(mode) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // 2. CLOCK THEME SELECTION
                        Column {
                            Text("STANDBY CLOCK THEME", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ClockTheme.values().forEach { theme ->
                                    val isSelected = clockTheme == theme
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .border(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setClockTheme(theme) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when(theme) {
                                                ClockTheme.ORANGE_STANDBY -> "Orange Standby"
                                                ClockTheme.MINIMALIST_WHITE -> "Minimalist White"
                                                ClockTheme.RETRO_GREEN -> "Retro Terminal Green"
                                                ClockTheme.VECTOR_ANALOGUE -> "Vector Analogue Clock"
                                            },
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF4500))
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. BACKGROUND SELECTION
                        Column {
                            Text("BACKGROUND SYSTEM", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                BackgroundStyle.values().forEach { style ->
                                    val isSelected = backgroundStyle == style
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .border(1.dp, if (isSelected) Color.White else Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setBackgroundStyle(style) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = when(style) {
                                                BackgroundStyle.DYNAMIC_BLUR -> "Dynamic Album Art Blur"
                                                BackgroundStyle.PITCH_BLACK -> "Pitch Black (OLED)"
                                                BackgroundStyle.CHARCOAL_GREY -> "Sleek Charcoal Solid"
                                                BackgroundStyle.SUNSET_GRADIENT -> "Sunset Gradient"
                                                BackgroundStyle.OCEAN_GRADIENT -> "Ocean Gradient"
                                                BackgroundStyle.FOREST_GRADIENT -> "Forest Gradient"
                                            },
                                            color = if (isSelected) Color.White else Color.Gray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Button(
                            onClick = { showSettingsDrawer = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Text("Apply and Save", color = Color.White, fontWeight = FontWeight.Bold)
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
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
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
