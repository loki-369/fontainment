package com.fontainment.app.presentation.desk

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val burnInOffset by viewModel.burnInOffset.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val batteryPercentage by viewModel.batteryPercentage.collectAsState()
    val isCharging by viewModel.isCharging.collectAsState()
    val ramUsagePercent by viewModel.ramUsagePercent.collectAsState()
    val deviceTemp by viewModel.deviceTemp.collectAsState()

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
    val millis = cal.get(Calendar.MILLISECOND)

    val timeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(frameTime))
    val dateString = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date(frameTime))

    // Interactive StandBy layouts cycling
    var leftPanelLayoutIndex by remember { mutableStateOf(0) } // 0: Digital Clock, 1: Analog Clock, 2: World Clocks, 3: Quotes & News
    var rightCard1Index by remember { mutableStateOf(0) } // 0: Now Playing, 1: Calendar, 2: Weather Details
    var rightCard2Index by remember { mutableStateOf(0) } // 0: System Monitor, 1: Battery Health, 2: Photos Slideshow

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
            .background(Color.Black) // Strict OLED black backdrop
    ) {
        // Slowly shifting background glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientBrush)
                .blur(80.dp)
        )

        // Main StandBy Split Dashboard
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .offset(x = burnInOffset.first.dp, y = burnInOffset.second.dp) // Pixel shifting safety
        ) {
            // Top Back to Driving Launcher Button
            IconButton(
                onClick = { navController.navigate(Screen.DriveMode.route) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Drive Launcher", tint = Color.White)
            }

            // Central Layout Split Row
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 48.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT COLUMN: Customizable Large Clock / Watch Face Area
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
                                // 1. Oversized Digital Clock & Weather Widget
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CloudQueue, contentDescription = "Weather indicator", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("22°C • Clear", color = Color.Gray, fontSize = 12.sp)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery charging status", tint = if (isCharging) Color.Green else Color.Gray, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("$batteryPercentage%${if (isCharging) " (Charging)" else ""}", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                            1 -> {
                                // 2. Canvas Drawn Sweeping Analog Clock
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(modifier = Modifier.size(140.dp)) {
                                        val dialPrimaryColor = MaterialTheme.colorScheme.primary
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val r = size.minDimension / 2
                                            val center = Offset(size.width / 2, size.height / 2)
                                            
                                            // Dial ring border
                                            drawCircle(color = Color.White.copy(alpha = 0.05f), radius = r)
                                            drawCircle(color = Color.White.copy(alpha = 0.15f), radius = r, style = Stroke(width = 2f))
                                            
                                            // Ticks
                                            for (i in 0 until 12) {
                                                val tickAngle = i * 30 * (Math.PI / 180)
                                                val tickLen = if (i % 3 == 0) 12f else 6f
                                                val tickColor = if (i % 3 == 0) dialPrimaryColor else Color.Gray.copy(alpha = 0.5f)
                                                val start = Offset(
                                                    (center.x + (r - tickLen) * Math.sin(tickAngle)).toFloat(),
                                                    (center.y - (r - tickLen) * Math.cos(tickAngle)).toFloat()
                                                )
                                                val end = Offset(
                                                    (center.x + r * Math.sin(tickAngle)).toFloat(),
                                                    (center.y - r * Math.cos(tickAngle)).toFloat()
                                                )
                                                drawLine(color = tickColor, start = start, end = end, strokeWidth = 3f)
                                            }
                                            
                                            // Sweeping second calculation (fluid rotation)
                                            val sweepingSeconds = second + (millis / 1000f)
                                            val secondAngle = sweepingSeconds * 6 * (Math.PI / 180)
                                            val minuteAngle = (minute + second / 60f) * 6 * (Math.PI / 180)
                                            val hourAngle = ((hour % 12) + minute / 60f) * 30 * (Math.PI / 180)
                                            
                                            // Hands drawing
                                            // Hour hand
                                            drawLine(
                                                color = Color.White,
                                                start = center,
                                                end = Offset(
                                                    (center.x + (r * 0.5) * Math.sin(hourAngle)).toFloat(),
                                                    (center.y - (r * 0.5) * Math.cos(hourAngle)).toFloat()
                                                ),
                                                strokeWidth = 6f
                                            )
                                            // Minute hand
                                            drawLine(
                                                color = Color.White.copy(alpha = 0.8f),
                                                start = center,
                                                end = Offset(
                                                    (center.x + (r * 0.75) * Math.sin(minuteAngle)).toFloat(),
                                                    (center.y - (r * 0.75) * Math.cos(minuteAngle)).toFloat()
                                                ),
                                                strokeWidth = 4f
                                            )
                                            // Sweeping second hand (crimson/primary colored accent)
                                            drawLine(
                                                color = dialPrimaryColor,
                                                start = center,
                                                end = Offset(
                                                    (center.x + (r * 0.85) * Math.sin(secondAngle)).toFloat(),
                                                    (center.y - (r * 0.85) * Math.cos(secondAngle)).toFloat()
                                                ),
                                                strokeWidth = 2f
                                            )
                                            // Pivot Center pin
                                            drawCircle(color = Color.Black, radius = 6f)
                                            drawCircle(color = dialPrimaryColor, radius = 3f)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("ANALOG CLOCK", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                }
                            }
                            2 -> {
                                // 3. World Time Clocks List
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("WORLD TIME CLUSTERS", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    val formats = listOf(
                                        Pair("Cupertino (PST)", "America/Los_Angeles"),
                                        Pair("London (GMT)", "Europe/London"),
                                        Pair("Tokyo (JST)", "Asia/Tokyo")
                                    )
                                    
                                    formats.forEach { (cityName, zoneId) ->
                                        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
                                            timeZone = TimeZone.getTimeZone(zoneId)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(cityName, color = Color.LightGray, fontSize = 13.sp)
                                            Text(sdf.format(Date(frameTime)), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            3 -> {
                                // 4. Quote of the Day & News widget
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("QUOTE & INSIGHTS OF THE DAY", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
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
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("NEWS: Premium automotive OS update releases Desk Standby cluster integration.", color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                // RIGHT COLUMN: Split Carousel Widget Cards (Card 1 Top, Card 2 Bottom)
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // WIDGET CARD 1: Spotify Now Playing / Calendar / Weather Detailed
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
                                        // Spotify Now Playing Widget
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("NOW PLAYING", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                Icon(Icons.Default.MusicNote, contentDescription = "Music indicator", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                            }
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(
                                                                    MaterialTheme.colorScheme.primary,
                                                                    MaterialTheme.colorScheme.secondary
                                                                )
                                                            )
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(currentTrack.title.take(1), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(currentTrack.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(currentTrack.artist, color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }

                                            // Small progress bar and play pause triggers
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val progress = if (currentTrack.durationMs > 0) currentTrack.progressMs.toFloat() / currentTrack.durationMs.toFloat() else 0f
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(3.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.08f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(progress)
                                                            .fillMaxHeight()
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.primary)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Icon(
                                                    imageVector = if (currentTrack.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                    contentDescription = "Playback toggle",
                                                    tint = Color.White,
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clickable { viewModel.playPauseMusic() }
                                                )
                                            }
                                        }
                                    }
                                    1 -> {
                                        // Calendar Event List Widget
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
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("• 09:30 AM: Weekly Standup", color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("• 12:45 PM: Lunch Meeting", color = Color.White, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("• 04:00 PM: Vehicle Diagnostics Check", color = Color.Gray, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        // Detailed Weather Forecast Widget
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("WEATHER ANALYSIS", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                                Icon(Icons.Default.WbSunny, contentDescription = "Sunny Weather", tint = Color.Yellow, modifier = Modifier.size(12.dp))
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text("22°", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text("CLEAR SKY", color = Color.LightGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Text("Humidity: 45% • Wind: 8 km/h", color = Color.Gray, fontSize = 9.sp)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text("18:00 • 20°", color = Color.Gray, fontSize = 9.sp)
                                                Text("20:00 • 17°", color = Color.Gray, fontSize = 9.sp)
                                                Text("22:00 • 15°", color = Color.Gray, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // WIDGET CARD 2: System Monitor / Battery diagnostics / Photos Slideshow
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
                                        // System Monitor Widget
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("SYSTEM STATUS", color = Color.DarkGray, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                            
                                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                // RAM usage
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("RAM CAPACITY", color = Color.Gray, fontSize = 10.sp)
                                                        Text("$ramUsagePercent%", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                                        Box(modifier = Modifier.fillMaxWidth(ramUsagePercent / 100f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                                    }
                                                }

                                                // CPU Temperature
                                                Column {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text("CPU THERMALS", color = Color.Gray, fontSize = 10.sp)
                                                        Text("$deviceTemp°C", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Box(modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f))) {
                                                        Box(modifier = Modifier.fillMaxWidth(deviceTemp / 100f).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
                                                    }
                                                }
                                            }
                                            Text("UPTIME: ${cal.get(Calendar.HOUR_OF_DAY)}h ${cal.get(Calendar.MINUTE)}m", color = Color.DarkGray, fontSize = 9.sp)
                                        }
                                    }
                                    1 -> {
                                        // Battery Health & Charging Speeds Widget
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
                                                Icon(Icons.Default.BatteryChargingFull, contentDescription = "Battery info", tint = Color.Green, modifier = Modifier.size(12.dp))
                                            }
                                            
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("BATTERY HEALTH", color = Color.Gray, fontSize = 11.sp)
                                                    Text("94% (Good)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("VOLTAGE", color = Color.Gray, fontSize = 11.sp)
                                                    Text("3.87 V", color = Color.White, fontSize = 11.sp)
                                                }
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("CHARGE VELOCITY", color = Color.Gray, fontSize = 11.sp)
                                                    Text(if (isCharging) "18W Fast Charge" else "0W (Discharging)", color = if (isCharging) Color.Green else Color.Gray, fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                    2 -> {
                                        // Google Photos Slideshow Widget mockup
                                        // Uses rotating scenic gradient cards to mock slideshow visuals
                                        val gradientScenery = listOf(
                                            Pair("Alps Scenic Pass", Brush.linearGradient(listOf(Color(0xFF2196F3), Color(0xFF00BCD4)))),
                                            Pair("Sahara Desert Dunes", Brush.linearGradient(listOf(Color(0xFFFF9800), Color(0xFFFF5722)))),
                                            Pair("Pacific Coast Highway", Brush.linearGradient(listOf(Color(0xFF4CAF50), Color(0xFF009688))))
                                        )
                                        val photoIndex = ((cal.get(Calendar.SECOND) / 6) % gradientScenery.size).toInt()
                                        val photo = gradientScenery[photoIndex]
                                        
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(photo.second)
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Image, contentDescription = "Slideshow photo logo", tint = Color.White, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("PHOTOS", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                            Text(
                                                text = photo.first,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Small Pixel Shift Active Indicator
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
    }
}
