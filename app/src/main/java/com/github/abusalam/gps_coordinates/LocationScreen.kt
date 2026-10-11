package com.github.abusalam.gps_coordinates

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

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
    var labelText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (permissionsState.allPermissionsGranted) {
            LaunchedEffect(Unit) {
                viewModel.startLocationUpdates()
            }
            val locationState by viewModel.locationState.collectAsState()

            if (locationState.latitude != null && locationState.longitude != null) {
                Text(
                    text = "Latitude: ${locationState.latitude}\nLongitude: ${locationState.longitude}",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = labelText,
                    onValueChange = { labelText = it },
                    label = { Text("Location Label (e.g. Home, Park)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        val labelToSave = labelText.ifBlank { "Saved Location" }
                        viewModel.saveCoordinate(labelToSave, locationState.latitude!!, locationState.longitude!!)
                        Toast.makeText(context, "Location Saved", Toast.LENGTH_SHORT).show()
                        labelText = ""
                        onNavigateBack()
                    }) {
                        Text("Save")
                    }

                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Coordinates", "${locationState.latitude}, ${locationState.longitude}")
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to Clipboard", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Copy")
                    }

                    Button(onClick = {
                        val labelForMap = labelText.ifBlank { "Location" }
                        val uri = Uri.parse("geo:${locationState.latitude},${locationState.longitude}?q=${locationState.latitude},${locationState.longitude}(${Uri.encode(labelForMap)})")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    }) {
                        Text("Map")
                    }
                }
            } else {
                Text(
                    text = "Fetching location...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            val textToShow = if (permissionsState.shouldShowRationale) {
                "We need location permissions to fetch your GPS coordinates. Please grant them."
            } else {
                "Location permissions are required to display GPS coordinates. Please grant the permissions."
            }
            Text(
                text = textToShow,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant Permissions")
            }
        }
    }
}
