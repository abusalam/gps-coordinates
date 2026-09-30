package com.github.abusalam.gps_coordinates

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    viewModel: LocationViewModel = viewModel()
) {
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val context = LocalContext.current

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
                Button(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Coordinates", "${locationState.latitude}, ${locationState.longitude}")
                    clipboard.setPrimaryClip(clip)
                }) {
                    Text("Copy to Clipboard")
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
