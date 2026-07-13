package com.yash.shortw

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yash.shortw.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDashboard(onNavigate: (String) -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Short Panel", color = GhostWhite, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Obsidian)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = GhostWhite
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Obsidian,
                        selectedTextColor = ElectricBlue,
                        indicatorColor = ElectricBlue,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.TouchApp, contentDescription = "Trigger") },
                    label = { Text("Trigger") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Obsidian,
                        selectedTextColor = NeonPurple,
                        indicatorColor = NeonPurple,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Palette, contentDescription = "Appearance") },
                    label = { Text("Appearance") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Obsidian,
                        selectedTextColor = HyperPink,
                        indicatorColor = HyperPink,
                        unselectedIconColor = SoftGray,
                        unselectedTextColor = SoftGray
                    )
                )
            }
        },
        containerColor = Obsidian
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> HomeTab(onNavigate)
                1 -> TriggerTab()
                2 -> AppearanceTab()
            }
        }
    }
}
