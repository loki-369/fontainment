package com.fontainment.app.presentation.desk

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DeskClock(
    hour: Int,
    minute: Int,
    second: Int,
    clockTheme: ClockTheme,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        when (clockTheme) {
            ClockTheme.ORANGE_STANDBY -> {
                val timeString = String.format("%02d:%02d", hour, minute)
                Text(
                    text = timeString,
                    fontSize = 135.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF4500), // Glowing Orange-Red
                    lineHeight = 135.sp,
                    letterSpacing = (-4).sp
                )
            }
            ClockTheme.MINIMALIST_WHITE -> {
                val timeString = String.format("%02d:%02d", hour, minute)
                Text(
                    text = timeString,
                    fontSize = 135.sp,
                    fontWeight = FontWeight.ExtraLight,
                    color = Color.White,
                    lineHeight = 135.sp,
                    letterSpacing = (-2).sp
                )
            }
            ClockTheme.RETRO_GREEN -> {
                val blink = second % 2 == 0
                val colon = if (blink) ":" else " "
                val timeString = String.format("%02d%s%02d", hour, colon, minute)
                Text(
                    text = timeString,
                    fontSize = 120.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF39FF14), // Glowing neon green
                    lineHeight = 120.sp,
                    letterSpacing = 2.sp
                )
            }
            ClockTheme.VECTOR_ANALOGUE -> {
                Canvas(modifier = Modifier.size(240.dp)) {
                    val center = this.center
                    val radius = size.minDimension / 2
                    
                    // Draw transparent background ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.02f),
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    // Draw outer ticks
                    for (i in 0 until 12) {
                        val angle = i * 30 * (Math.PI / 180)
                        val isQuarter = i % 3 == 0
                        val tickLength = if (isQuarter) 14.dp.toPx() else 8.dp.toPx()
                        val start = Offset(
                            (center.x + (radius - tickLength) * Math.sin(angle)).toFloat(),
                            (center.y - (radius - tickLength) * Math.cos(angle)).toFloat()
                        )
                        val end = Offset(
                            (center.x + radius * Math.sin(angle)).toFloat(),
                            (center.y - radius * Math.cos(angle)).toFloat()
                        )
                        drawLine(
                            color = if (isQuarter) Color.White else Color.Gray.copy(alpha = 0.4f),
                            start = start,
                            end = end,
                            strokeWidth = if (isQuarter) 3.dp.toPx() else 1.5f.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    
                    // Hour hand (thick white hand)
                    val hourAngle = ((hour % 12) + minute / 60f) * 30 * (Math.PI / 180)
                    drawLine(
                        color = Color.White,
                        start = center,
                        end = Offset(
                            (center.x + (radius * 0.45f) * Math.sin(hourAngle)).toFloat(),
                            (center.y - (radius * 0.45f) * Math.cos(hourAngle)).toFloat()
                        ),
                        strokeWidth = 5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Minute hand (slightly longer and thinner)
                    val minuteAngle = (minute + second / 60f) * 6 * (Math.PI / 180)
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = center,
                        end = Offset(
                            (center.x + (radius * 0.7f) * Math.sin(minuteAngle)).toFloat(),
                            (center.y - (radius * 0.7f) * Math.cos(minuteAngle)).toFloat()
                        ),
                        strokeWidth = 3.5f.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Second hand (thin glowing red/orange second hand)
                    val secondAngle = second * 6 * (Math.PI / 180)
                    drawLine(
                        color = Color(0xFFFF4500),
                        start = center,
                        end = Offset(
                            (center.x + (radius * 0.8f) * Math.sin(secondAngle)).toFloat(),
                            (center.y - (radius * 0.8f) * Math.cos(secondAngle)).toFloat()
                        ),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Central cap details
                    drawCircle(color = Color(0xFFFF4500), radius = 5.dp.toPx(), center = center)
                    drawCircle(color = Color.Black, radius = 2.dp.toPx(), center = center)
                }
            }
        }
    }
}
