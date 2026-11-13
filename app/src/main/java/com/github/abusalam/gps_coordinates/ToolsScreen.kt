package com.github.abusalam.gps_coordinates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.abusalam.gps_coordinates.data.Coordinate
import com.github.abusalam.gps_coordinates.ui.theme.AccentBlue
import com.github.abusalam.gps_coordinates.ui.theme.AccentGreen
import com.github.abusalam.gps_coordinates.ui.theme.AccentRed
import com.github.abusalam.gps_coordinates.ui.theme.DarkBackground
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBackground
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBorder
import com.github.abusalam.gps_coordinates.ui.theme.PrimaryCyan
import com.github.abusalam.gps_coordinates.ui.theme.TextCyan
import com.github.abusalam.gps_coordinates.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

data class WaypointPoint(
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val sourceName: String = "Custom"
)

enum class PointSourceType {
    SAVED, CURRENT_GPS, MANUAL
}

@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val savedCoordinates by viewModel.savedCoordinates.collectAsState()
    val locationState by viewModel.locationState.collectAsState()

    // Ensure GPS updates are initiated so current location is available
    LaunchedEffect(Unit) {
        viewModel.startLocationUpdates()
    }

    var sourceAType by remember { mutableStateOf(PointSourceType.SAVED) }
    var sourceBType by remember { mutableStateOf(PointSourceType.SAVED) }

    var selectedCoordinateA by remember { mutableStateOf<Coordinate?>(null) }
    var selectedCoordinateB by remember { mutableStateOf<Coordinate?>(null) }

    var manualLatA by remember { mutableStateOf("") }
    var manualLonA by remember { mutableStateOf("") }
    var manualLatB by remember { mutableStateOf("") }
    var manualLonB by remember { mutableStateOf("") }

    var showPickerForPoint by remember { mutableStateOf<String?>(null) } // "A" or "B"
    var selectedDistanceUnit by remember { mutableIntStateOf(0) } // 0: km, 1: m, 2: mi, 3: NM, 4: ft

    // Set default initial selections if savedCoordinates is available
    LaunchedEffect(savedCoordinates) {
        if (selectedCoordinateA == null && savedCoordinates.isNotEmpty()) {
            selectedCoordinateA = savedCoordinates.firstOrNull()
        }
        if (selectedCoordinateB == null && savedCoordinates.size > 1) {
            selectedCoordinateB = savedCoordinates[1]
        }
    }

    // Resolve Point A
    val resolvedPointA: WaypointPoint? = remember(sourceAType, selectedCoordinateA, locationState, manualLatA, manualLonA) {
        when (sourceAType) {
            PointSourceType.SAVED -> selectedCoordinateA?.let {
                WaypointPoint(it.label, it.latitude, it.longitude, it.altitude, "Saved Waypoint")
            }
            PointSourceType.CURRENT_GPS -> {
                val lat = locationState.latitude
                val lon = locationState.longitude
                if (lat != null && lon != null) {
                    WaypointPoint("Current Location", lat, lon, locationState.altitude, "Live GPS")
                } else null
            }
            PointSourceType.MANUAL -> {
                val lat = manualLatA.toDoubleOrNull()
                val lon = manualLonA.toDoubleOrNull()
                if (lat != null && lon != null) {
                    WaypointPoint("Manual Point A", lat, lon, null, "Coordinates")
                } else null
            }
        }
    }

    // Resolve Point B
    val resolvedPointB: WaypointPoint? = remember(sourceBType, selectedCoordinateB, locationState, manualLatB, manualLonB) {
        when (sourceBType) {
            PointSourceType.SAVED -> selectedCoordinateB?.let {
                WaypointPoint(it.label, it.latitude, it.longitude, it.altitude, "Saved Waypoint")
            }
            PointSourceType.CURRENT_GPS -> {
                val lat = locationState.latitude
                val lon = locationState.longitude
                if (lat != null && lon != null) {
                    WaypointPoint("Current Location", lat, lon, locationState.altitude, "Live GPS")
                } else null
            }
            PointSourceType.MANUAL -> {
                val lat = manualLatB.toDoubleOrNull()
                val lon = manualLonB.toDoubleOrNull()
                if (lat != null && lon != null) {
                    WaypointPoint("Manual Point B", lat, lon, null, "Coordinates")
                } else null
            }
        }
    }

    // Calculation result
    val distanceCalculation = remember(resolvedPointA, resolvedPointB) {
        if (resolvedPointA != null && resolvedPointB != null) {
            val results = FloatArray(3)
            Location.distanceBetween(
                resolvedPointA.latitude, resolvedPointA.longitude,
                resolvedPointB.latitude, resolvedPointB.longitude,
                results
            )
            val distanceMeters = results[0]
            val initialBearing = results[1]
            val finalBearing = if (results.size > 2) results[2] else results[1]
            val altDiff = if (resolvedPointA.altitude != null && resolvedPointB.altitude != null) {
                resolvedPointB.altitude - resolvedPointA.altitude
            } else null

            CalculatedDistance(
                distanceMeters = distanceMeters,
                initialBearing = initialBearing,
                finalBearing = finalBearing,
                altitudeDifference = altDiff
            )
        } else null
    }

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
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "GPS Tools & Utilities",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
                Text(
                    text = "Measure distance, bearing & elevation between waypoints",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // Section: Distance Calculator Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Straighten,
                    contentDescription = null,
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Waypoint Distance Calculator",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // POINT A CARD
        PointSelectionCard(
            label = "Point A (Origin)",
            accentColor = AccentBlue,
            sourceType = sourceAType,
            onSourceTypeChanged = { sourceAType = it },
            resolvedPoint = resolvedPointA,
            onPickWaypoint = { showPickerForPoint = "A" },
            manualLat = manualLatA,
            manualLon = manualLonA,
            onManualLatChange = { manualLatA = it },
            onManualLonChange = { manualLonA = it },
            hasGpsFix = locationState.latitude != null
        )

        // Swap Button in between
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            OutlinedButton(
                onClick = {
                    val tempType = sourceAType
                    sourceAType = sourceBType
                    sourceBType = tempType

                    val tempCoord = selectedCoordinateA
                    selectedCoordinateA = selectedCoordinateB
                    selectedCoordinateB = tempCoord

                    val tempLat = manualLatA
                    val tempLon = manualLonA
                    manualLatA = manualLatB
                    manualLonA = manualLonB
                    manualLatB = tempLat
                    manualLonB = tempLon
                },
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Icon(Icons.Default.SwapVert, contentDescription = "Swap Points", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Swap Origin & Destination", style = MaterialTheme.typography.labelSmall)
            }
        }

        // POINT B CARD
        PointSelectionCard(
            label = "Point B (Destination)",
            accentColor = PrimaryCyan,
            sourceType = sourceBType,
            onSourceTypeChanged = { sourceBType = it },
            resolvedPoint = resolvedPointB,
            onPickWaypoint = { showPickerForPoint = "B" },
            manualLat = manualLatB,
            manualLon = manualLonB,
            onManualLatChange = { manualLatB = it },
            onManualLonChange = { manualLonB = it },
            hasGpsFix = locationState.latitude != null
        )

        // RESULTS SECTION
        if (distanceCalculation != null && resolvedPointA != null && resolvedPointB != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Measurement Results",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AccentGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "WGS84 Geodesic",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Unit selector tabs
                    val unitLabels = listOf("Kilometers", "Meters", "Miles", "Nautical Miles", "Feet")
                    TabRow(
                        selectedTabIndex = selectedDistanceUnit,
                        containerColor = DarkBackground,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedDistanceUnit]),
                                color = AccentBlue
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        unitLabels.forEachIndexed { index, _ ->
                            Tab(
                                selected = selectedDistanceUnit == index,
                                onClick = { selectedDistanceUnit = index },
                                text = {
                                    Text(
                                        text = when (index) {
                                            0 -> "km"
                                            1 -> "m"
                                            2 -> "mi"
                                            3 -> "NM"
                                            else -> "ft"
                                        },
                                        fontWeight = if (selectedDistanceUnit == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    // Primary Display
                    val primaryFormattedDistance = when (selectedDistanceUnit) {
                        0 -> String.format(Locale.US, "%.3f km", distanceCalculation.distanceMeters / 1000.0)
                        1 -> String.format(Locale.US, "%,.1f m", distanceCalculation.distanceMeters)
                        2 -> String.format(Locale.US, "%.3f mi", distanceCalculation.distanceMeters / 1609.344)
                        3 -> String.format(Locale.US, "%.3f NM", distanceCalculation.distanceMeters / 1852.0)
                        else -> String.format(Locale.US, "%,.1f ft", distanceCalculation.distanceMeters * 3.28084)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "STRAIGHT-LINE DISTANCE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextSecondary,
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = primaryFormattedDistance,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                color = TextCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    // Comprehensive Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MeasurementDataRow(
                            label = "Initial Bearing / Heading",
                            value = "${String.format(Locale.US, "%.1f°", distanceCalculation.bearingNormalized)} ${distanceCalculation.cardinalDirection}",
                            icon = Icons.Default.Explore
                        )

                        MeasurementDataRow(
                            label = "Final Bearing",
                            value = String.format(Locale.US, "%.1f°", distanceCalculation.finalBearingNormalized),
                            icon = Icons.Default.Explore
                        )

                        if (distanceCalculation.altitudeDifference != null) {
                            val sign = if (distanceCalculation.altitudeDifference >= 0) "+" else ""
                            MeasurementDataRow(
                                label = "Elevation Difference",
                                value = "$sign${String.format(Locale.US, "%.1f m", distanceCalculation.altitudeDifference)}",
                                icon = Icons.Default.Place
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                        MeasurementDataRow(
                            label = "Est. Walking Time (at 5 km/h)",
                            value = formatTravelTime(distanceCalculation.distanceMeters / 1000.0, 5.0),
                            icon = Icons.AutoMirrored.Filled.DirectionsWalk
                        )

                        MeasurementDataRow(
                            label = "Est. Driving Time (direct 60 km/h)",
                            value = formatTravelTime(distanceCalculation.distanceMeters / 1000.0, 60.0),
                            icon = Icons.Default.DirectionsCar
                        )
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val url = "https://www.google.com/maps/dir/?api=1&origin=${resolvedPointA.latitude},${resolvedPointA.longitude}&destination=${resolvedPointB.latitude},${resolvedPointB.longitude}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Maps", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        OutlinedButton(
                            onClick = {
                                val summary = """
                                    Distance Measurement:
                                    Point A: ${resolvedPointA.title} (${String.format(Locale.US, "%.6f, %.6f", resolvedPointA.latitude, resolvedPointA.longitude)})
                                    Point B: ${resolvedPointB.title} (${String.format(Locale.US, "%.6f, %.6f", resolvedPointB.latitude, resolvedPointB.longitude)})
                                    Distance: $primaryFormattedDistance (${String.format(Locale.US, "%.3f km / %.3f mi", distanceCalculation.distanceMeters / 1000.0, distanceCalculation.distanceMeters / 1609.344)})
                                    Initial Bearing: ${String.format(Locale.US, "%.1f°", distanceCalculation.bearingNormalized)} ${distanceCalculation.cardinalDirection}
                                """.trimIndent()

                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Distance Measurement", summary)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied measurement results", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Results", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Select Both Points to Calculate",
                        style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Choose saved waypoints, live GPS, or enter custom coordinates above to view distance, heading, and travel estimations.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Waypoint Selector Dialog
    if (showPickerForPoint != null) {
        val targetPoint = showPickerForPoint
        WaypointPickerDialog(
            title = if (targetPoint == "A") "Select Point A Waypoint" else "Select Point B Waypoint",
            waypoints = savedCoordinates,
            onDismiss = { showPickerForPoint = null },
            onSelect = { selectedCoord ->
                if (targetPoint == "A") {
                    selectedCoordinateA = selectedCoord
                    sourceAType = PointSourceType.SAVED
                } else {
                    selectedCoordinateB = selectedCoord
                    sourceBType = PointSourceType.SAVED
                }
                showPickerForPoint = null
            }
        )
    }
}

@Composable
fun PointSelectionCard(
    label: String,
    accentColor: Color,
    sourceType: PointSourceType,
    onSourceTypeChanged: (PointSourceType) -> Unit,
    resolvedPoint: WaypointPoint?,
    onPickWaypoint: () -> Unit,
    manualLat: String,
    manualLon: String,
    onManualLatChange: (String) -> Unit,
    onManualLonChange: (String) -> Unit,
    hasGpsFix: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }

            // Source Selector Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = sourceType == PointSourceType.SAVED,
                    onClick = { onSourceTypeChanged(PointSourceType.SAVED) },
                    label = { Text("Saved Waypoint", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        containerColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = sourceType == PointSourceType.CURRENT_GPS,
                    onClick = { onSourceTypeChanged(PointSourceType.CURRENT_GPS) },
                    label = { Text("Current GPS", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        containerColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                FilterChip(
                    selected = sourceType == PointSourceType.MANUAL,
                    onClick = { onSourceTypeChanged(PointSourceType.MANUAL) },
                    label = { Text("Custom", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        containerColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Content based on source type
            when (sourceType) {
                PointSourceType.SAVED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .clickable { onPickWaypoint() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = resolvedPoint?.title ?: "No waypoint selected",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (resolvedPoint != null) Color.White else TextSecondary
                                )
                            )
                            if (resolvedPoint != null) {
                                Text(
                                    text = String.format(Locale.US, "%.6f°, %.6f°", resolvedPoint.latitude, resolvedPoint.longitude),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextCyan,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = onPickWaypoint,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Select", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
                PointSourceType.CURRENT_GPS -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (hasGpsFix && resolvedPoint != null) {
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
                                Text("Live GPS Position Locked", style = MaterialTheme.typography.labelSmall.copy(color = AccentGreen, fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = String.format(Locale.US, "%.6f°, %.6f°", resolvedPoint.latitude, resolvedPoint.longitude),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextCyan,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            resolvedPoint.altitude?.let { alt ->
                                Text("Altitude: ${String.format(Locale.US, "%.1f m", alt)}", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AccentRed)
                                )
                                Text("Acquiring GPS fix...", style = MaterialTheme.typography.labelSmall.copy(color = AccentRed))
                            }
                            Text(
                                "Please ensure GPS and location permissions are enabled.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
                PointSourceType.MANUAL -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = manualLat,
                            onValueChange = onManualLatChange,
                            label = { Text("Latitude", fontSize = 11.sp, color = TextSecondary) },
                            placeholder = { Text("e.g. 37.7749", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = manualLon,
                            onValueChange = onManualLonChange,
                            label = { Text("Longitude", fontSize = 11.sp, color = TextSecondary) },
                            placeholder = { Text("e.g. -122.4194", color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryCyan,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeasurementDataRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}

@Composable
fun WaypointPickerDialog(
    title: String,
    waypoints: List<Coordinate>,
    onDismiss: () -> Unit,
    onSelect: (Coordinate) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery, waypoints) {
        if (searchQuery.isBlank()) waypoints
        else waypoints.filter {
            it.label.contains(searchQuery, ignoreCase = true) ||
            (it.tag != null && it.tag.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search waypoints...", color = TextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryCyan) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (filtered.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (waypoints.isEmpty()) "No saved waypoints found.\nSave waypoints in Live GPS first!" else "No matching waypoints found.",
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { coord ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkBackground)
                                    .clickable { onSelect(coord) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        coord.label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        String.format(Locale.US, "%.5f°, %.5f°", coord.latitude, coord.longitude),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextCyan,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }
                                coord.tag?.let { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkCardBorder)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("#$tag", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary, fontSize = 10.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkCardBackground,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

data class CalculatedDistance(
    val distanceMeters: Float,
    val initialBearing: Float,
    val finalBearing: Float,
    val altitudeDifference: Double? = null
) {
    val bearingNormalized: Float get() = ((initialBearing % 360f) + 360f) % 360f
    val finalBearingNormalized: Float get() = ((finalBearing % 360f) + 360f) % 360f
    val cardinalDirection: String
        get() {
            val b = bearingNormalized
            return when {
                b >= 337.5f || b < 22.5f -> "N"
                b in 22.5f..67.5f -> "NE"
                b in 67.5f..112.5f -> "E"
                b in 112.5f..157.5f -> "SE"
                b in 157.5f..202.5f -> "S"
                b in 202.5f..247.5f -> "SW"
                b in 247.5f..292.5f -> "W"
                else -> "NW"
            }
        }
}

private fun formatTravelTime(distanceKm: Double, speedKmH: Double): String {
    val hours = distanceKm / speedKmH
    val totalMinutes = (hours * 60).roundToInt()
    return if (totalMinutes < 60) {
        "~$totalMinutes min"
    } else {
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        "~${h}h ${m}m"
    }
}
