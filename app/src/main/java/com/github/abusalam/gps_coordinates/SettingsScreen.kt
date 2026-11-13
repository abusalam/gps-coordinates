package com.github.abusalam.gps_coordinates

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.content.FileProvider
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
import java.io.File

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val backupInfo by viewModel.backupInfo.collectAsState()
    val savedCoordinates by viewModel.savedCoordinates.collectAsState()

    var autoRestoreEnabled by remember { mutableStateOf(viewModel.isAutoRestoreEnabled) }
    var autoBackupEnabled by remember { mutableStateOf(viewModel.isAutoBackupEnabled) }
    var proximityAlertEnabled by remember { mutableStateOf(viewModel.isProximityAlertEnabled) }
    var proximityThreshold by remember { mutableFloatStateOf(viewModel.proximityThresholdMeters) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    // File picker for restoring backup from any location on device
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.let { stream ->
                    viewModel.restoreFromInputStream(stream, replaceExisting = false) { result ->
                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
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
            Text(
                text = "Settings & Backup",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        // Card 1: Device Backup Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (backupInfo.exists) AccentGreen.copy(alpha = 0.2f) else DarkCardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (backupInfo.exists) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (backupInfo.exists) AccentGreen else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (backupInfo.exists) "Device Backup Available" else "No Device Backup Found",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (backupInfo.exists) "Stored locally on this device" else "Create a backup to safeguard your waypoints",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }

                if (backupInfo.exists) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBackground)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Last Backup:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                            Text(
                                backupInfo.formattedDate,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextCyan, fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Saved Waypoints:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                            Text(
                                "${backupInfo.count} waypoints",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("File Size:", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                            Text(
                                backupInfo.fileSizeFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White)
                            )
                        }

                        if (backupInfo.filePath.isNotBlank()) {
                            Text(
                                text = "Path: ${backupInfo.filePath}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary
                                ),
                                maxLines = 2
                            )
                        }
                    }
                }

                // Backup and Restore Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.backupNow { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup Now", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier.weight(1f),
                        enabled = backupInfo.exists,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = PrimaryCyan,
                            disabledContentColor = TextSecondary
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (backupInfo.exists) PrimaryCyan else DarkCardBorder
                        )
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Secondary actions: Share & Pick File
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val backupFile = viewModel.getBackupFileForSharing()
                            if (backupFile != null && backupFile.exists()) {
                                shareBackupFile(context, backupFile)
                            } else {
                                Toast.makeText(context, "No backup file available to share. Please backup first.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Backup", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import File", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Card 2: Automation Preferences
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Automatic Backup & Restore",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                // Option 1: Auto-Restore
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Auto-restore if backup exists",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Automatically restores saved coordinates from device backup when the app opens and local data is empty.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Switch(
                        checked = autoRestoreEnabled,
                        onCheckedChange = { checked ->
                            autoRestoreEnabled = checked
                            viewModel.isAutoRestoreEnabled = checked
                            Toast.makeText(
                                context,
                                if (checked) "Auto-restore enabled" else "Auto-restore disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkCardBorder
                        )
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                // Option 2: Auto-Backup
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(
                            text = "Auto-backup on changes",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Silently keeps the device backup updated whenever waypoints are saved, edited, or deleted.",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }

                    Switch(
                        checked = autoBackupEnabled,
                        onCheckedChange = { checked ->
                            autoBackupEnabled = checked
                            viewModel.isAutoBackupEnabled = checked
                            Toast.makeText(
                                context,
                                if (checked) "Auto-backup enabled" else "Auto-backup disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkCardBorder
                        )
                    )
                }

                androidx.compose.material3.HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

                // Option 3: Proximity Alert & Overwrite
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "Proximity Alert & Overwrite",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Detects when saving a waypoint very close to an existing one and provides an option to overwrite.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }

                        Switch(
                            checked = proximityAlertEnabled,
                            onCheckedChange = { checked ->
                                proximityAlertEnabled = checked
                                viewModel.isProximityAlertEnabled = checked
                                Toast.makeText(
                                    context,
                                    if (checked) "Proximity alert enabled" else "Proximity alert disabled",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentBlue,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkCardBorder
                            )
                        )
                    }

                    if (proximityAlertEnabled) {
                        Text(
                            text = "Proximity Detection Radius:",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(10f, 25f, 50f, 100f).forEach { threshold ->
                                val isSelected = proximityThreshold == threshold
                                val label = if (threshold == 25f) "25m (Default)" else "${threshold.toInt()}m"
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        proximityThreshold = threshold
                                        viewModel.proximityThresholdMeters = threshold
                                    },
                                    label = { Text(label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentBlue,
                                        containerColor = DarkBackground
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Storage & Database Stats
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
                    Icon(Icons.Default.Storage, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Data & Storage Overview",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Database Waypoints:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(
                        "${savedCoordinates.size} waypoints",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Database Name:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text("coordinate_database", style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Android System Backup:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text("Enabled (Cloud & Device)", style = MaterialTheme.typography.bodySmall.copy(color = AccentGreen, fontWeight = FontWeight.Bold))
                }

                val appVersionName = remember(context) {
                    try {
                        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                        } else {
                            @Suppress("DEPRECATION")
                            context.packageManager.getPackageInfo(context.packageName, 0)
                        }
                        packageInfo.versionName ?: "1.2"
                    } catch (_: Exception) {
                        "1.2"
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("App Version:", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    Text(appVersionName, style = MaterialTheme.typography.bodySmall.copy(color = Color.White))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Restore confirmation dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Text(
                    text = "Restore Saved Waypoints?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Found backup containing ${backupInfo.count} waypoints from ${backupInfo.formattedDate}.",
                        color = Color.White
                    )
                    Text(
                        text = "• Merge: Adds any missing waypoints from the backup without deleting your current ones.\n• Replace All: Wipes existing list and replaces completely with backup.",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            containerColor = DarkCardBackground,
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.restoreBackup(replaceExisting = false) { result ->
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
                ) {
                    Text("Merge", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            showRestoreDialog = false
                            viewModel.restoreBackup(replaceExisting = true) { result ->
                                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                            }
                        }
                    ) {
                        Text("Replace All", color = AccentRed)
                    }

                    TextButton(onClick = { showRestoreDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            }
        )
    }
}

private fun shareBackupFile(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share GPS Coordinates Backup"))
    } catch (e: Exception) {
        Toast.makeText(context, "Could not share backup file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
