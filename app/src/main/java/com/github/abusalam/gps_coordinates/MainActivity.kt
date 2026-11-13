package com.github.abusalam.gps_coordinates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import com.github.abusalam.gps_coordinates.ui.theme.AccentBlue
import com.github.abusalam.gps_coordinates.ui.theme.DarkBackground
import com.github.abusalam.gps_coordinates.ui.theme.DarkCardBorder
import com.github.abusalam.gps_coordinates.ui.theme.GPSCoordinatesTheme
import com.github.abusalam.gps_coordinates.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GPSCoordinatesTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainAppScreen() {
    val backStack = rememberNavBackStack(ListRoute)
    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

    // Observe active route for tab highlight
    val currentRoute = backStack.lastOrNull() ?: ListRoute

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        bottomBar = {
            CustomBottomNavigationBar(
                currentRoute = currentRoute,
                onTabSelected = { tab ->
                    when (tab) {
                        "Live GPS" -> {
                            if (currentRoute != LocationDetailRoute) {
                                backStack.add(LocationDetailRoute)
                            }
                        }
                        "Saved" -> {
                            if (currentRoute != ListRoute) {
                                if (backStack.size > 1) {
                                    backStack.removeAt(backStack.lastIndex)
                                } else {
                                    backStack.add(ListRoute)
                                }
                            }
                        }
                        "Tools" -> {
                            if (currentRoute != ToolsRoute) {
                                backStack.add(ToolsRoute)
                            }
                        }
                        "Settings" -> {
                            if (currentRoute != SettingsRoute) {
                                backStack.add(SettingsRoute)
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            sceneStrategies = listOf(listDetailStrategy),
            entryProvider = entryProvider {
                entry<ListRoute>(
                    metadata = ListDetailSceneStrategy.listPane(
                        detailPlaceholder = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Select an item to view details", color = TextSecondary)
                            }
                        }
                    )
                ) {
                    ListScreen(
                        onLocationItemClick = { backStack.add(LocationDetailRoute) },
                        onNavigateToSettings = { backStack.add(SettingsRoute) }
                    )
                }
                entry<LocationDetailRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    LocationDetailScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
                entry<ToolsRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    ToolsScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
                entry<SettingsRoute>(
                    metadata = ListDetailSceneStrategy.detailPane()
                ) {
                    SettingsScreen(
                        onNavigateBack = {
                            if (backStack.size > 1) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    )
                }
            }
        )
    }
}

@Composable
fun CustomBottomNavigationBar(
    currentRoute: NavKey,
    onTabSelected: (String) -> Unit
) {
    val selectedTab = when (currentRoute) {
        LocationDetailRoute -> "Live GPS"
        ToolsRoute -> "Tools"
        SettingsRoute -> "Settings"
        else -> "Saved"
    }

    val tabs = listOf(
        TabItem("Live GPS", Icons.Default.LocationOn),
        TabItem("Saved", Icons.Default.Bookmark),
        TabItem("Tools", Icons.Default.Build),
        TabItem("Settings", Icons.Default.Settings)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
    ) {
        androidx.compose.material3.HorizontalDivider(color = DarkCardBorder, thickness = 1.dp)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.title == selectedTab

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab.title) }
                        .padding(vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) AccentBlue else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (isSelected) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: ImageVector)
