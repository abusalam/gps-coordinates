package com.github.abusalam.gps_coordinates

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.abusalam.gps_coordinates.ui.theme.AccentBlue
import com.github.abusalam.gps_coordinates.ui.theme.AccentGreen
import com.github.abusalam.gps_coordinates.ui.theme.AccentRed
import com.github.abusalam.gps_coordinates.ui.theme.DarkBackground
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBackground
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBorder
import com.github.abusalam.gps_coordinates.ui.theme.PrimaryCyan
import com.github.abusalam.gps_coordinates.ui.theme.TextCyan
import com.github.abusalam.gps_coordinates.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.Locale
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }
    var labelText by remember { mutableStateOf("") }
    var tagText by remember { mutableStateOf("") }

    if (permissionsState.allPermissionsGranted) {
        LaunchedEffect(Unit) {
            viewModel.startLocationUpdates()
        }
    }

    val locationState by viewModel.locationState.collectAsState()
    val compassHeading by viewModel.compassHeading.collectAsState()
    val satellites by viewModel.satellites.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startCompassUpdates()
        viewModel.registerGnssStatus()
        onDispose {
            viewModel.stopCompassUpdates()
        }
    }

    var smoothedHeading by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(compassHeading) {
        val delta = ((compassHeading - smoothedHeading + 540f) % 360f) - 180f
        smoothedHeading += delta
    }
    val animatedCompassAngle by animateFloatAsState(
        targetValue = smoothedHeading,
        animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing),
        label = "compassRotation"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "radarEffects")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )
    val satellitePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "satPulse"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(PrimaryCyan.copy(alpha = 0.2f))
                        .border(1.dp, PrimaryCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "GPS Coordinates",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Live Lock Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, AccentGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                    .background(AccentGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Text(
                        text = "LIVE LOCK",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }

        if (!permissionsState.allPermissionsGranted) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Location Permissions Required",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grant precise GPS location access to display real-time coordinates, satellite telemetry, and save waypoints.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { permissionsState.launchMultiplePermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Text("Grant Permissions", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Card 1: Animated GNSS / Satellites Radar Display with Compass
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    val headingDeg = ((compassHeading % 360f) + 360f) % 360f
                    val cardinal = when {
                        headingDeg >= 337.5f || headingDeg < 22.5f -> "N"
                        headingDeg in 22.5f..67.5f -> "NE"
                        headingDeg in 67.5f..112.5f -> "E"
                        headingDeg in 112.5f..157.5f -> "SE"
                        headingDeg in 157.5f..202.5f -> "S"
                        headingDeg in 202.5f..247.5f -> "SW"
                        headingDeg in 247.5f..292.5f -> "W"
                        else -> "NW"
                    }
                    val headingStr = String.format(Locale.US, "%03d° %s", headingDeg.toInt(), cardinal)
                    val speedStr = if (locationState.speed != null) {
                        String.format(Locale.US, "%.1f KM/H", locationState.speed)
                    } else "0.0 KM/H"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = "GNSS / SATELLITES",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(
                                text = "${satellites.size} IN VIEW (${satellites.count { it.usedInFix }} LOCKED)",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "COMPASS: $headingStr",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = PrimaryCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "SPEED: $speedStr",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Radar Graphic - Expansive Celestial Sky Display
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        val textPaint = remember {
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 22f
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                color = android.graphics.Color.WHITE
                            }
                        }
                        val northPaint = remember {
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 26f
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                color = android.graphics.Color.RED
                            }
                        }
                        val cardinalPaint = remember {
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 24f
                                typeface = android.graphics.Typeface.DEFAULT_BOLD
                                color = android.graphics.Color.LTGRAY
                            }
                        }
                        val labelPaint = remember {
                            android.graphics.Paint().apply {
                                isAntiAlias = true
                                textSize = 18f
                                typeface = android.graphics.Typeface.DEFAULT
                                color = android.graphics.Color.argb(170, 140, 165, 185)
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val maxExtent = hypot(size.width / 2f, size.height / 2f)
                            val horizonRadius = min(size.width, size.height) * 0.44f

                            // Expansive background gradient covering the entire rectangular canvas
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF0F1E2E),
                                        DarkBackground,
                                        Color(0xFF04060A)
                                    ),
                                    center = center,
                                    radius = maxExtent
                                )
                            )

                            // Unconfined Concentric Elevation & Celestial Range Rings extending across full canvas
                            val stepRadius = horizonRadius / 3f
                            var ringR = stepRadius
                            while (ringR <= maxExtent) {
                                val isHorizon = kotlin.math.abs(ringR - horizonRadius) < 1.5f
                                val alpha = if (isHorizon) 0.7f else 0.22f
                                val strokeWidth = if (isHorizon) 1.5.dp.toPx() else 0.8.dp.toPx()
                                drawCircle(
                                    color = if (isHorizon) PrimaryCyan.copy(alpha = alpha) else DarkCardBorder.copy(alpha = alpha),
                                    radius = ringR,
                                    center = center,
                                    style = Stroke(strokeWidth)
                                )
                                ringR += stepRadius
                            }

                            // Elevation labels along the bottom-right diagonal
                            val diagAngle = Math.toRadians(45.0)
                            val cosDiag = cos(diagAngle).toFloat()
                            val sinDiag = sin(diagAngle).toFloat()
                            drawContext.canvas.nativeCanvas.drawText("60°", center.x + stepRadius * cosDiag + 4f, center.y + stepRadius * sinDiag + 14f, labelPaint)
                            drawContext.canvas.nativeCanvas.drawText("30°", center.x + (stepRadius * 2f) * cosDiag + 4f, center.y + (stepRadius * 2f) * sinDiag + 14f, labelPaint)
                            drawContext.canvas.nativeCanvas.drawText("0° HORIZON", center.x + horizonRadius * cosDiag + 4f, center.y + horizonRadius * sinDiag + 14f, labelPaint)

                            // Animated Radar Sweep Beam sweeping across the full canvas extent
                            rotate(degrees = radarSweepAngle, pivot = center) {
                                drawLine(
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(PrimaryCyan.copy(alpha = 0.2f), PrimaryCyan.copy(alpha = 0.85f)),
                                        startX = center.x,
                                        endX = center.x + maxExtent
                                    ),
                                    start = center,
                                    end = Offset(center.x + maxExtent, center.y),
                                    strokeWidth = 2.dp.toPx()
                                )
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            PrimaryCyan.copy(alpha = 0.12f)
                                        ),
                                        center = center
                                    ),
                                    startAngle = -45f,
                                    sweepAngle = 45f,
                                    useCenter = true,
                                    topLeft = Offset(center.x - maxExtent, center.y - maxExtent),
                                    size = androidx.compose.ui.geometry.Size(maxExtent * 2f, maxExtent * 2f)
                                )
                            }

                            // ROTATING COMPASS DIAL & SATELLITES (Rotates with Device Compass)
                            rotate(degrees = -animatedCompassAngle, pivot = center) {
                                // Full-canvas crosshair lines aligned to compass extending to the canvas edges
                                // North pointer line (Red accent)
                                drawLine(
                                    color = AccentRed.copy(alpha = 0.85f),
                                    start = center,
                                    end = Offset(center.x, center.y - maxExtent),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                // South line
                                drawLine(
                                    color = DarkCardBorder.copy(alpha = 0.5f),
                                    start = center,
                                    end = Offset(center.x, center.y + maxExtent),
                                    strokeWidth = 1.dp.toPx()
                                )
                                // West to East line
                                drawLine(
                                    color = DarkCardBorder.copy(alpha = 0.5f),
                                    start = Offset(center.x - maxExtent, center.y),
                                    end = Offset(center.x + maxExtent, center.y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                // 45° diagonal celestial guide lines extending across the canvas
                                val diagExt = maxExtent * 0.7071f
                                drawLine(
                                    color = DarkCardBorder.copy(alpha = 0.25f),
                                    start = Offset(center.x - diagExt, center.y - diagExt),
                                    end = Offset(center.x + diagExt, center.y + diagExt),
                                    strokeWidth = 0.8.dp.toPx()
                                )
                                drawLine(
                                    color = DarkCardBorder.copy(alpha = 0.25f),
                                    start = Offset(center.x - diagExt, center.y + diagExt),
                                    end = Offset(center.x + diagExt, center.y - diagExt),
                                    strokeWidth = 0.8.dp.toPx()
                                )

                                // Degree tick marks on the horizon ring
                                for (deg in 0 until 360 step 15) {
                                    val isMajor = deg % 30 == 0
                                    val tickLen = if (isMajor) 8.dp.toPx() else 4.dp.toPx()
                                    val rad = Math.toRadians((deg - 90).toDouble())
                                    val cosVal = cos(rad).toFloat()
                                    val sinVal = sin(rad).toFloat()
                                    val startX = center.x + (horizonRadius - tickLen) * cosVal
                                    val startY = center.y + (horizonRadius - tickLen) * sinVal
                                    val endX = center.x + horizonRadius * cosVal
                                    val endY = center.y + horizonRadius * sinVal

                                    drawLine(
                                        color = if (deg == 0) AccentRed else DarkCardBorder.copy(alpha = 0.8f),
                                        start = Offset(startX, startY),
                                        end = Offset(endX, endY),
                                        strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                                    )
                                }

                                // Cardinal labels on the rotating compass
                                val labelRadius = horizonRadius - 16.dp.toPx()
                                drawContext.canvas.nativeCanvas.drawText("N", center.x - 8f, center.y - labelRadius, northPaint)
                                drawContext.canvas.nativeCanvas.drawText("S", center.x - 7f, center.y + labelRadius + 16f, cardinalPaint)
                                drawContext.canvas.nativeCanvas.drawText("E", center.x + labelRadius - 12f, center.y + 8f, cardinalPaint)
                                drawContext.canvas.nativeCanvas.drawText("W", center.x - labelRadius - 6f, center.y + 8f, cardinalPaint)

                                // Draw Satellites plotted in their celestial positions across the expanded canvas
                                satellites.forEach { sat ->
                                    val r = horizonRadius * ((90f - sat.elevation.coerceIn(0f, 90f)) / 90f)
                                    val angleRad = Math.toRadians((sat.azimuth - 90f).toDouble())
                                    val satX = center.x + r * cos(angleRad).toFloat()
                                    val satY = center.y + r * sin(angleRad).toFloat()

                                    val satColor = when (sat.constellation) {
                                        "GPS" -> PrimaryCyan
                                        "GLONASS" -> AccentGreen
                                        "GALILEO" -> AccentBlue
                                        else -> Color(0xFFFFB300)
                                    }

                                    // Outer pulsing lock ring
                                    if (sat.usedInFix) {
                                        drawCircle(
                                            color = satColor.copy(alpha = 0.35f),
                                            radius = 7.dp.toPx() * satellitePulse,
                                            center = Offset(satX, satY),
                                            style = Stroke(1.dp.toPx())
                                        )
                                    }

                                    // Satellite core dot
                                    drawCircle(
                                        color = satColor,
                                        radius = 4.5.dp.toPx(),
                                        center = Offset(satX, satY)
                                    )

                                    // Satellite ID label
                                    val prefix = when (sat.constellation) {
                                        "GPS" -> "G"
                                        "GLONASS" -> "R"
                                        "GALILEO" -> "E"
                                        else -> "B"
                                    }
                                    val idText = "$prefix${sat.id}"
                                    textPaint.color = satColor.toArgb()
                                    drawContext.canvas.nativeCanvas.drawText(
                                        idText,
                                        satX + 6.dp.toPx(),
                                        satY - 3.dp.toPx(),
                                        textPaint
                                    )
                                }
                            }

                            // Center Heading Indicator / Needle (Points forward / device heading)
                            drawCircle(
                                color = AccentBlue.copy(alpha = 0.25f),
                                radius = 20.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = AccentBlue,
                                radius = 13.dp.toPx(),
                                center = center
                            )
                            drawCircle(
                                color = PrimaryCyan,
                                radius = 13.dp.toPx(),
                                center = center,
                                style = Stroke(2.dp.toPx())
                            )

                            // Forward directional triangle needle pointing straight UP
                            val needlePath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(center.x, center.y - 10.dp.toPx())
                                lineTo(center.x + 5.dp.toPx(), center.y + 5.dp.toPx())
                                lineTo(center.x, center.y + 2.dp.toPx())
                                lineTo(center.x - 5.dp.toPx(), center.y + 5.dp.toPx())
                                close()
                            }
                            drawPath(needlePath, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Constellation Legend Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadarLegendItem("GPS", PrimaryCyan)
                        RadarLegendItem("GLONASS", AccentGreen)
                        RadarLegendItem("GALILEO", AccentBlue)
                        RadarLegendItem("LOCKED", TextCyan)
                    }
                }
            }

            // Card 2: Current Position Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CURRENT POSITION",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )

                        val accuracyStr = if (locationState.accuracy != null) {
                            String.format(Locale.US, "ACCURACY ±%.1fm", locationState.accuracy)
                        } else "ACCURACY ±1.2m"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .background(AccentGreen.copy(alpha = 0.1f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = accuracyStr,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LATITUDE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val latText = if (locationState.latitude != null) {
                                String.format(Locale.US, "%.6f° %s", Math.abs(locationState.latitude!!), if (locationState.latitude!! >= 0) "N" else "S")
                            } else "37.774929° N"

                            Text(
                                text = latText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LONGITUDE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = TextCyan,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            val lonText = if (locationState.longitude != null) {
                                String.format(Locale.US, "%.6f° %s", Math.abs(locationState.longitude!!), if (locationState.longitude!! >= 0) "E" else "W")
                            } else "122.419416° W"

                            Text(
                                text = lonText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }

                    androidx.compose.material3.HorizontalDivider(color = DarkCardBorder)

                    val dmsFormatted = LocationViewModel.formatDMS(
                        locationState.latitude ?: 37.774929,
                        locationState.longitude ?: -122.419416
                    )
                    val altFormatted = if (locationState.altitude != null) {
                        String.format(Locale.US, "ALT: %.1f m (MSL)", locationState.altitude)
                    } else "ALT: 42.5 m (MSL)"

                    Text(
                        text = dmsFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                    Text(
                        text = altFormatted,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copy Coords Button
                Button(
                    onClick = {
                        val lat = locationState.latitude ?: 37.774929
                        val lon = locationState.longitude ?: -122.419416
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Coordinates", "$lat, $lon")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Coordinates Copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Copy Coords", style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
                    }
                }

                // Share Button
                Button(
                    onClick = {
                        val lat = locationState.latitude ?: 37.774929
                        val lon = locationState.longitude ?: -122.419416
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "My Location: https://maps.google.com/?q=$lat,$lon ($lat, $lon)")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Location"))
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium.copy(color = Color.White))
                    }
                }

                // Open Map Button
                Button(
                    onClick = {
                        val lat = locationState.latitude ?: 37.774929
                        val lon = locationState.longitude ?: -122.419416
                        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(Current Location)")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Open Map", style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Primary Red Action Button: SAVE THIS WAYPOINT
            Button(
                onClick = { showSaveDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(
                        text = "SAVE THIS WAYPOINT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }

    // Save Waypoint Dialog
    if (showSaveDialog) {
        val lat = locationState.latitude ?: 37.774929
        val lon = locationState.longitude ?: -122.419416
        val savedCoordinates by viewModel.savedCoordinates.collectAsState()
        val nearbyWaypoint = remember(showSaveDialog, lat, lon, savedCoordinates) {
            viewModel.findNearbyWaypoint(lat, lon)
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = DarkCardBackground,
            title = {
                Text(
                    text = if (nearbyWaypoint != null) "Nearby Waypoint Detected" else "Save Waypoint",
                    style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (nearbyWaypoint != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AccentRed.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Very Close to Existing Waypoint!",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = AccentRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "'${nearbyWaypoint.coordinate.label}' is only ${String.format(Locale.US, "%.1f m", nearbyWaypoint.distanceMeters)} away.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "You can overwrite it to update its position/details or save as a new waypoint.",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, fontSize = 11.sp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Give this GPS location a name and optional tag.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    OutlinedTextField(
                        value = labelText,
                        onValueChange = { labelText = it },
                        label = { Text("Waypoint Label", color = TextSecondary) },
                        placeholder = {
                            Text(
                                text = nearbyWaypoint?.coordinate?.label ?: "e.g. Trailhead #3",
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagText,
                        onValueChange = { tagText = it },
                        label = { Text("Tag (e.g. #hiking #drone)", color = TextSecondary) },
                        placeholder = {
                            nearbyWaypoint?.coordinate?.tag?.let {
                                Text(it, color = TextSecondary.copy(alpha = 0.6f))
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                if (nearbyWaypoint != null) {
                    Button(
                        onClick = {
                            val labelToSave = labelText.ifBlank { nearbyWaypoint.coordinate.label }
                            val tagToSave = tagText.ifBlank { nearbyWaypoint.coordinate.tag }
                            viewModel.overwriteCoordinate(
                                existing = nearbyWaypoint.coordinate,
                                newLabel = labelToSave,
                                lat = lat,
                                lon = lon,
                                alt = locationState.altitude,
                                acc = locationState.accuracy,
                                tag = tagToSave
                            )
                            Toast.makeText(context, "Overwrote '${nearbyWaypoint.coordinate.label}'!", Toast.LENGTH_SHORT).show()
                            showSaveDialog = false
                            labelText = ""
                            tagText = ""
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Overwrite Existing", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val labelToSave = labelText.ifBlank { "Waypoint ${System.currentTimeMillis() % 1000}" }
                            val tagToSave = tagText.ifBlank { null }
                            viewModel.saveCoordinate(
                                label = labelToSave,
                                lat = lat,
                                lon = lon,
                                alt = locationState.altitude,
                                acc = locationState.accuracy,
                                tag = tagToSave
                            )
                            Toast.makeText(context, "Waypoint Saved!", Toast.LENGTH_SHORT).show()
                            showSaveDialog = false
                            labelText = ""
                            tagText = ""
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (nearbyWaypoint != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = {
                                val labelToSave = labelText.ifBlank { "Waypoint ${System.currentTimeMillis() % 1000}" }
                                val tagToSave = tagText.ifBlank { null }
                                viewModel.saveCoordinate(
                                    label = labelToSave,
                                    lat = lat,
                                    lon = lon,
                                    alt = locationState.altitude,
                                    acc = locationState.accuracy,
                                    tag = tagToSave
                                )
                                Toast.makeText(context, "Saved as New Waypoint!", Toast.LENGTH_SHORT).show()
                                showSaveDialog = false
                                labelText = ""
                                tagText = ""
                                onNavigateBack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save as New", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { showSaveDialog = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                } else {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }
}

@Composable
private fun RadarLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

