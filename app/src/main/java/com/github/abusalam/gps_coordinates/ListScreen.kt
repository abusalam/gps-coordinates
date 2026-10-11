package com.github.abusalam.gps_coordinates

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.abusalam.gps_coordinates.data.Coordinate

@Composable
fun ListScreen(
    onLocationItemClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel()
) {
    val savedCoordinates by viewModel.savedCoordinates.collectAsState()
    val context = LocalContext.current

    var coordinateToDelete by remember { mutableStateOf<Coordinate?>(null) }
    var coordinateToEdit by remember { mutableStateOf<Coordinate?>(null) }
    var editLabelText by remember { mutableStateOf("") }
    
    var searchQuery by remember { mutableStateOf("") }
    val filteredCoordinates = remember(searchQuery, savedCoordinates) {
        if (searchQuery.isBlank()) {
            savedCoordinates
        } else {
            savedCoordinates.filter { 
                it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Locations") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
        item {
            ListItem(
                headlineContent = { Text("Current GPS Coordinates") },
                supportingContent = { Text("View & save current latitude and longitude") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLocationItemClick() }
            )
        }
        
        if (filteredCoordinates.isNotEmpty()) {
            item {
                HorizontalDivider()
                Text(
                    text = "Saved Locations",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(filteredCoordinates, key = { it.id }) { coord ->
                ListItem(
                    headlineContent = { Text(coord.label) },
                    supportingContent = { Text("${coord.latitude}, ${coord.longitude}") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { viewModel.togglePin(coord) }) {
                                Icon(
                                    imageVector = if (coord.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = if (coord.isPinned) "Unpin ${coord.label}" else "Pin ${coord.label}"
                                )
                            }
                            IconButton(onClick = {
                                coordinateToEdit = coord
                                editLabelText = coord.label
                            }) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit ${coord.label}")
                            }
                            IconButton(onClick = { coordinateToDelete = coord }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete ${coord.label}")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val uri = Uri.parse("geo:${coord.latitude},${coord.longitude}?q=${coord.latitude},${coord.longitude}(${Uri.encode(coord.label)})")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                )
            }
        }
    }

    coordinateToDelete?.let { coord ->
        AlertDialog(
            onDismissRequest = { coordinateToDelete = null },
            title = { Text("Delete Location") },
            text = { Text("Are you sure you want to delete '${coord.label}'?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteCoordinate(coord)
                    coordinateToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { coordinateToDelete = null }) { Text("Cancel") }
            }
        )
    }

    coordinateToEdit?.let { coord ->
        AlertDialog(
            onDismissRequest = { coordinateToEdit = null },
            title = { Text("Edit Label") },
            text = {
                OutlinedTextField(
                    value = editLabelText,
                    onValueChange = { editLabelText = it },
                    label = { Text("Location Label") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editLabelText.isNotBlank()) {
                        viewModel.updateCoordinateLabel(coord, editLabelText)
                    }
                    coordinateToEdit = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { coordinateToEdit = null }) { Text("Cancel") }
            }
        )
    }
}
