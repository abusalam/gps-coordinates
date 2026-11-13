package com.github.abusalam.gps_coordinates

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.abusalam.gps_coordinates.data.Coordinate
import com.github.abusalam.gps_coordinates.ui.theme.AccentBlue
import com.github.abusalam.gps_coordinates.ui.theme.AccentRed
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBackground
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBorder
import com.github.abusalam.gps_coordinates.ui.theme.PrimaryCyan
import com.github.abusalam.gps_coordinates.ui.theme.TextCyan
import com.github.abusalam.gps_coordinates.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WaypointDetailDialog(
    coordinate: Coordinate,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = remember(coordinate.timestamp) {
        val sdf = SimpleDateFormat("MMM dd 'at' hh:mm a", Locale.US)
        sdf.format(Date(coordinate.timestamp))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCardBackground,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Waypoint Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Waypoint Title & Subtitle
                Column {
                    Text(
                        text = coordinate.label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Saved $dateStr • Precision Lock",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }

                // Target Grid Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                        .background(DarkCardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridStep = 20.dp.toPx()
                        var x = 0f
                        while (x < size.width) {
                            drawLine(color = DarkCardBorder, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                            x += gridStep
                        }
                        var y = 0f
                        while (y < size.height) {
                            drawLine(color = DarkCardBorder, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                            y += gridStep
                        }

                        val center = Offset(size.width / 2, size.height / 2)
                        drawCircle(color = TextCyan, radius = 24.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
                        drawCircle(color = AccentRed, radius = 5.dp.toPx(), center = center)
                        drawLine(color = AccentRed, start = Offset(center.x - 12.dp.toPx(), center.y), end = Offset(center.x + 12.dp.toPx(), center.y), strokeWidth = 1.5.dp.toPx())
                        drawLine(color = AccentRed, start = Offset(center.x, center.y - 12.dp.toPx()), end = Offset(center.x, center.y + 12.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                    }

                    val latFormatted = String.format(Locale.US, "%.6f° N", Math.abs(coordinate.latitude))
                    val lonFormatted = String.format(Locale.US, "%.6f° W", Math.abs(coordinate.longitude))
                    Text(
                        text = "$latFormatted, $lonFormatted",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextCyan,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                    )
                }

                // Coordinate Formats Section
                Text(
                    text = "COORDINATE FORMATS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )

                // Decimal (DD) Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Decimal (DD)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                            Text(
                                "${coordinate.latitude}, ${coordinate.longitude}",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                            )
                        }
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DD", "${coordinate.latitude}, ${coordinate.longitude}")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied DD", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("COPY", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                        }
                    }
                }

                // Degrees Minutes Seconds (DMS) Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkCardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Degrees Minutes Seconds (DMS)", style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary))
                            Text(
                                LocationViewModel.formatDMS(coordinate.latitude, coordinate.longitude).removePrefix("DMS: "),
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontFamily = FontFamily.Monospace)
                            )
                        }
                        Button(
                            onClick = {
                                val dms = LocationViewModel.formatDMS(coordinate.latitude, coordinate.longitude)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("DMS", dms)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied DMS", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCardBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("COPY", style = MaterialTheme.typography.labelSmall.copy(color = Color.White))
                        }
                    }
                }

                // Primary Launch in Google Maps Button
                Button(
                    onClick = {
                        val uri = Uri.parse("geo:${coordinate.latitude},${coordinate.longitude}?q=${coordinate.latitude},${coordinate.longitude}(${Uri.encode(coordinate.label)})")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("LAUNCH IN GOOGLE MAPS", style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }

                // Share with Contacts Button
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Waypoint '${coordinate.label}': https://maps.google.com/?q=${coordinate.latitude},${coordinate.longitude}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Waypoint"))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkCardBackground),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkCardBorder)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("SHARE WITH CONTACTS", style = MaterialTheme.typography.labelLarge.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
            }
        },
        confirmButton = {}
    )
}
