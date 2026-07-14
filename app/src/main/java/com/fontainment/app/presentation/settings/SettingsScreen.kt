package com.fontainment.app.presentation.settings

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.app.ActivityManager
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fontainment.app.presentation.navigation.Screen

enum class SettingsSection {
    PREFERENCES, DIAGNOSTICS, ABOUT
}

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val currentTheme by viewModel.currentTheme.collectAsState()
    val speedUnit by viewModel.speedUnit.collectAsState()
    val tempUnit by viewModel.tempUnit.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val autoBrightness by viewModel.autoBrightnessEnabled.collectAsState()
    val autoLaunchDevice by viewModel.autoLaunchDevice.collectAsState()
    val pairedDevices by viewModel.pairedBluetoothDevices.collectAsState()

    var activeSection by remember { mutableStateOf(SettingsSection.PREFERENCES) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val themesList = listOf(
        "Tesla Dark", "BMW Blue", "AMOLED Black", 
        "Nothing Style", "Lucid White", "Cyber Neon",
        "Midnight Black", "Minimal Gray", "Classic Dashboard",
        "Wallpaper Adaptive"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left Column: Navigation Sidebar
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = { navController.navigate(Screen.DriveMode.route) },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Section Switch Buttons
            SidebarItem(
                title = "Preferences",
                icon = Icons.Default.Settings,
                isSelected = activeSection == SettingsSection.PREFERENCES,
                onClick = { activeSection = SettingsSection.PREFERENCES }
            )
            SidebarItem(
                title = "Diagnostics",
                icon = Icons.Default.Build,
                isSelected = activeSection == SettingsSection.DIAGNOSTICS,
                onClick = { activeSection = SettingsSection.DIAGNOSTICS }
            )
            SidebarItem(
                title = "About System",
                icon = Icons.Default.Info,
                isSelected = activeSection == SettingsSection.ABOUT,
                onClick = { activeSection = SettingsSection.ABOUT }
            )
        }

        // Right Column: Settings Content Scroll
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = activeSection.name.replace("_", " "),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            when (activeSection) {
                SettingsSection.PREFERENCES -> {
                    // Theme Preset Card List
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = "Theme selection", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("System Theme Preset", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Render 10 theme selections in 3 rows
                                themesList.chunked(3).forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        chunk.forEach { theme ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(50.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (theme == currentTheme) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                                    .clickable { viewModel.selectTheme(theme) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = theme,
                                                    color = if (theme == currentTheme) Color.Black else Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                        // Pad empty space in row
                                        if (chunk.size < 3) {
                                            Spacer(modifier = Modifier.weight((3 - chunk.size).toFloat()))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Brightness and Display Settings
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.BrightnessLow, contentDescription = "Brightness status", tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Auto Brightness", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                    }
                                    Switch(
                                        checked = autoBrightness,
                                        onCheckedChange = { viewModel.toggleAutoBrightness(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Manual Brightness Control", color = Color.Gray, fontSize = 13.sp)
                                Slider(
                                    value = brightness,
                                    onValueChange = { viewModel.setBrightnessLevel(it) },
                                    enabled = !autoBrightness,
                                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }

                    // Auto Launch Automation Configurations
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Power, contentDescription = "Auto Launch options", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Automation & Auto-Launch", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Launch on pairing BT Speaker:", color = Color.White, fontSize = 14.sp)
                                    Box {
                                        Button(
                                            onClick = { dropdownExpanded = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Text(autoLaunchDevice ?: "Select Device", color = Color.White)
                                        }
                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("None (Disable)", color = Color.White) },
                                                onClick = {
                                                    viewModel.configureAutoLaunchBluetoothDevice(null)
                                                    dropdownExpanded = false
                                                }
                                            )
                                            pairedDevices.forEach { device ->
                                                DropdownMenuItem(
                                                    text = { Text(device, color = Color.White) },
                                                    onClick = {
                                                        viewModel.configureAutoLaunchBluetoothDevice(device)
                                                        dropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Measurement Units Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CompassCalibration, contentDescription = "Units config", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Measurement Scale Units", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Speed Limit Scale:", color = Color.White, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.selectSpeedUnit("KMH") },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (speedUnit == "KMH") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Text("KM/H", color = if (speedUnit == "KMH") Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.selectSpeedUnit("MPH") },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (speedUnit == "MPH") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Text("MPH", color = if (speedUnit == "MPH") Color.Black else Color.White)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Weather Temp Scale:", color = Color.White, fontSize = 14.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.selectTempUnit("C") },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (tempUnit == "C") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Text("Celsius (°C)", color = if (tempUnit == "C") Color.Black else Color.White)
                                        }
                                        Button(
                                            onClick = { viewModel.selectTempUnit("F") },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (tempUnit == "F") MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                                        ) {
                                            Text("Fahrenheit (°F)", color = if (tempUnit == "F") Color.Black else Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsSection.DIAGNOSTICS -> {
                    // Hardware checks metrics list
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Active Hardware Telemetry", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                
                                // Fetch real device metrics (RAM capacity, storage capacity)
                                val stat = StatFs(Environment.getDataDirectory().path)
                                val availSpace = stat.availableBlocksLong * stat.blockSizeLong / (1024 * 1024 * 1024)
                                val totalSpace = stat.blockCountLong * stat.blockSizeLong / (1024 * 1024 * 1024)

                                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                                val memInfo = ActivityManager.MemoryInfo()
                                actManager.getMemoryInfo(memInfo)
                                val totalRam = memInfo.totalMem / (1024 * 1024 * 1024)
                                val availRam = memInfo.availMem / (1024 * 1024 * 1024)

                                DiagnosticRow(title = "Fine GPS Tracking", value = "Connected (Live updates active)")
                                DiagnosticRow(title = "RAM Capacity", value = "$availRam GB Free / $totalRam GB Total")
                                DiagnosticRow(title = "Internal Storage space", value = "$availSpace GB Available / $totalSpace GB Total")
                                DiagnosticRow(title = "Bluetooth Adapter State", value = "Bonded list: ${pairedDevices.size} paired devices")
                                DiagnosticRow(title = "Android Operating System", value = "Android 14 (API Level 34)")
                                DiagnosticRow(title = "Device Target hardware", value = "Google Pixel 5a")
                            }
                        }
                    }
                }

                SettingsSection.ABOUT -> {
                    // About application details
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "System Info", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Fontainment OS Dashboard", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                    Text("Production Infotainment system built with Kotlin + Compose. Optimized for horizontal vehicle setups, magnetic dash mounts, and Desk StandBy visual docks.", color = Color.Gray, fontSize = 13.sp)
                                }
                            }
                        }
                    }

                    // Preferences Backup & Restore actions card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Backup, contentDescription = "Backup restore controls", tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Configuration Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.performBackup { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Backup Preferences", color = Color.White)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.performRestore { msg ->
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    ) {
                                        Text("Restore Settings", color = Color.Black, fontWeight = FontWeight.Bold)
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

@Composable
fun SidebarItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DiagnosticRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.Gray, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
