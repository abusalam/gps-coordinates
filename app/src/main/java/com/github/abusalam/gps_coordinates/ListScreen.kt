package com.github.abusalam.gps_coordinates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.PushPin
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun ListScreen(
    onLocationItemClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel()
) {
    val savedCoordinates by viewModel.savedCoordinates.collectAsState()
    val autoRestoreMessage by viewModel.autoRestoreMessage.collectAsState()
    val backupInfo by viewModel.backupInfo.collectAsState()
    val context = LocalContext.current

    var coordinateToDelete by remember { mutableStateOf<Coordinate?>(null) }
    var coordinateToEdit by remember { mutableStateOf<Coordinate?>(null) }
    var selectedCoordinateDetails by remember { mutableStateOf<Coordinate?>(null) }
    var editLabelText by remember { mutableStateOf("") }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filterCategories = listOf("All", "Pinned", "Outdoor", "Vehicles")

    val filteredCoordinates = remember(searchQuery, selectedFilter, savedCoordinates) {
        savedCoordinates.filter { coord ->
            val matchesQuery = searchQuery.isBlank() || 
                coord.label.contains(searchQuery, ignoreCase = true) ||
                (coord.tag != null && coord.tag.contains(searchQuery, ignoreCase = true))
            
            val matchesCategory = when (selectedFilter) {
                "Pinned" -> coord.isPinned
                "Outdoor" -> coord.tag?.contains("trail", ignoreCase = true) == true || coord.tag?.contains("outdoor", ignoreCase = true) == true || coord.label.contains("trail", ignoreCase = true)
                "Vehicles" -> coord.tag?.contains("car", ignoreCase = true) == true || coord.label.contains("car", ignoreCase = true) || coord.label.contains("parked", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesCategory
        }
    }

    val pinnedList = remember(filteredCoordinates) { filteredCoordinates.filter { it.isPinned } }
    val regularList = remember(filteredCoordinates) { filteredCoordinates.filter { !it.isPinned } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Saved Waypoints",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardBackground)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings & Backup",
                        tint = PrimaryCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Button(
                    onClick = onLocationItemClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Add New", style = MaterialTheme.typography.labelMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Auto-restore notification banner
        autoRestoreMessage?.let { msg ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Medium)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.dismissAutoRestoreMessage() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search waypoints or #tags...", color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = PrimaryCyan) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryCyan,
                unfocusedBorderColor = DarkCardBorder,
                focusedContainerColor = DarkCardBackground,
                unfocusedContainerColor = DarkCardBackground,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            items(filterCategories) { category ->
                val count = when (category) {
                    "All" -> savedCoordinates.size
                    "Pinned" -> savedCoordinates.count { it.isPinned }
                    else -> savedCoordinates.count { 
                        it.tag?.contains(category, ignoreCase = true) == true || 
                        it.label.contains(category, ignoreCase = true) 
                    }
                }
                val label = "$category ($count)"
                val isSelected = selectedFilter == category

                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = category },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentBlue,
                        containerColor = DarkCardBackground
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = DarkCardBorder,
                        selectedBorderColor = PrimaryCyan
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pinned Locations Section
            if (pinnedList.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(AccentRed)
                        )
                        Text(
                            text = "PINNED LOCATIONS (ALWAYS AT TOP)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentRed,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }

                items(pinnedList, key = { "pinned_${it.id}" }) { coord ->
                    WaypointCard(
                        coordinate = coord,
                        isPinnedSection = true,
                        onClick = { selectedCoordinateDetails = coord },
                        onTogglePin = { viewModel.togglePin(coord) },
                        onEdit = {
                            coordinateToEdit = coord
                            editLabelText = coord.label
                        },
                        onDelete = { coordinateToDelete = coord }
                    )
                }
            }

            // All Saved Locations Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(PrimaryCyan)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "MATCHING SEARCH RESULTS (${filteredCoordinates.size} FOUND)" else "ALL SAVED LOCATIONS (CHRONOLOGICAL)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }

            if (regularList.isEmpty() && pinnedList.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No Matching Waypoints" else "No Saved Waypoints",
                                style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (searchQuery.isNotBlank()) "Try a different search term or category filter." else "Use Live GPS to fetch and save your current coordinates.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            if (searchQuery.isBlank() && backupInfo.exists && backupInfo.count > 0) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    color = DarkCardBorder,
                                    thickness = 1.dp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Device Backup Available (${backupInfo.count} waypoints)",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = PrimaryCyan,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Text(
                                    text = "Last updated: ${backupInfo.formattedDate}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.restoreBackup(replaceExisting = false) { result ->
                                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore Now", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                    }

                                    OutlinedButton(
                                        onClick = onNavigateToSettings,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                                    ) {
                                        Text("Backup Settings", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                items(regularList, key = { "reg_${it.id}" }) { coord ->
                    WaypointCard(
                        coordinate = coord,
                        isPinnedSection = false,
                        onClick = { selectedCoordinateDetails = coord },
                        onTogglePin = { viewModel.togglePin(coord) },
                        onEdit = {
                            coordinateToEdit = coord
                            editLabelText = coord.label
                        },
                        onDelete = { coordinateToDelete = coord }
                    )
                }
            }
        }
    }

    // Waypoint Detail Dialog
    selectedCoordinateDetails?.let { coord ->
        WaypointDetailDialog(
            coordinate = coord,
            onDismiss = { selectedCoordinateDetails = null }
        )
    }

    // Delete Confirmation Dialog
    coordinateToDelete?.let { coord ->
        AlertDialog(
            onDismissRequest = { coordinateToDelete = null },
            containerColor = DarkCardBackground,
            title = { Text("Delete Waypoint", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete '${coord.label}'?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCoordinate(coord)
                        coordinateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { coordinateToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Label Dialog
    coordinateToEdit?.let { coord ->
        AlertDialog(
            onDismissRequest = { coordinateToEdit = null },
            containerColor = DarkCardBackground,
            title = { Text("Edit Label", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editLabelText,
                    onValueChange = { editLabelText = it },
                    label = { Text("Waypoint Label", color = TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editLabelText.isNotBlank()) {
                            viewModel.updateCoordinateLabel(coord, editLabelText)
                        }
                        coordinateToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                ) { Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { coordinateToEdit = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun WaypointCard(
    coordinate: Coordinate,
    isPinnedSection: Boolean,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = remember(coordinate.timestamp) {
        val sdf = SimpleDateFormat("MMM dd 'at' hh:mm a", Locale.US)
        sdf.format(Date(coordinate.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPinnedSection) AccentRed.copy(alpha = 0.6f) else DarkCardBorder
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = coordinate.label,
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f)
                )

                Row {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (coordinate.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = if (coordinate.isPinned) AccentRed else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Latitude & Longitude Readout
            val latStr = String.format(Locale.US, "%.6f° N", abs(coordinate.latitude))
            val lonStr = String.format(Locale.US, "%.6f° W", abs(coordinate.longitude))
            Text(
                text = "$latStr, $lonStr",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = TextCyan,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            )

            // Metadata / Subtitle
            val metaText = remember(coordinate) {
                val accText = if (coordinate.accuracy != null) " • Accuracy: ±${String.format(Locale.US, "%.1f", coordinate.accuracy)}m" else ""
                val tagText = if (coordinate.tag != null) " • Tag: ${coordinate.tag}" else ""
                "Saved: $dateStr$accText$tagText"
            }
            Text(
                text = metaText,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )

            // Quick Actions Row inside Card
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Button(
                    onClick = {
                        val uri = Uri.parse("geo:${coordinate.latitude},${coordinate.longitude}?q=${coordinate.latitude},${coordinate.longitude}(${Uri.encode(coordinate.label)})")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Open Maps", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Coordinates", "${coordinate.latitude}, ${coordinate.longitude}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("Copy", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                    }
                }
            }
        }
    }
}
