package com.yash.shortw

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yash.shortw.ui.theme.*

@Composable
fun HomeTab(onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Master Service", color = GhostWhite, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(if (AppState.isServiceRunning) "Running" else "Stopped", color = if (AppState.isServiceRunning) ElectricBlue else SoftGray)
            }
            Switch(
                checked = AppState.isServiceRunning,
                onCheckedChange = { 
                    if (it && !AppState.hasOverlayPermission && !AppState.hasAccessibilityPermission) {
                        Toast.makeText(context, "Overlay or Accessibility permission is required", Toast.LENGTH_SHORT).show()
                        return@Switch
                    }
                    AppState.isServiceRunning = it 
                    if (it) {
                        if (AppState.hasOverlayPermission) {
                            val intent = Intent(context, OverlayService::class.java)
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else if (AppState.hasAccessibilityPermission) {
                            Toast.makeText(context, "Using Accessibility Service Mode", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        if (AppState.hasOverlayPermission) {
                            context.stopService(Intent(context, OverlayService::class.java))
                        }
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = ElectricBlue, checkedTrackColor = ElectricBlue.copy(alpha = 0.3f))
            )
        }

        Text("App Configuration", color = HyperPink, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("app_selection") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Customize Slider Apps", color = GhostWhite)
                Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
            }
            HorizontalDivider(color = Obsidian)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("tool_selection") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Customize Tools", color = GhostWhite)
                Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
            }
            HorizontalDivider(color = Obsidian)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("control_selection") }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Customize Controls", color = GhostWhite)
                Icon(Icons.Rounded.ChevronRight, null, tint = SoftGray)
            }
        }
        
        Text("Legal & About", color = SoftGray, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
        ) {
            LegalMenuItem("Terms & Conditions") { onNavigate("terms") }
            HorizontalDivider(color = Obsidian)
            LegalMenuItem("Privacy Policy") { onNavigate("privacy") }
            HorizontalDivider(color = Obsidian)
            LegalMenuItem("Disclaimer") { onNavigate("disclaimer") }
            HorizontalDivider(color = Obsidian)
            LegalMenuItem("About") { onNavigate("about") }
        }
    }
}

@Composable
fun TriggerTab() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Edge Trigger Customization", color = NeonPurple, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Position", color = GhostWhite)
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Obsidian)
                ) {
                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 0); AppState.saveSettings(context) }
                            .background(if (AppState.settings.edgePosition == 0) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Left", color = GhostWhite) }
                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 1); AppState.saveSettings(context) }
                            .background(if (AppState.settings.edgePosition == 1) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Right", color = GhostWhite) }
                    Box(
                        modifier = Modifier
                            .clickable { AppState.settings = AppState.settings.copy(edgePosition = 2); AppState.saveSettings(context) }
                            .background(if (AppState.settings.edgePosition == 2) NeonPurple else Color.Transparent)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("Both", color = GhostWhite) }
                }
            }
            
            Column {
                Text("Thickness: ${AppState.settings.triggerThickness.toInt()}dp", color = GhostWhite)
                Slider(
                    value = AppState.settings.triggerThickness,
                    onValueChange = { AppState.settings = AppState.settings.copy(triggerThickness = it); AppState.saveSettings(context) },
                    valueRange = 2f..20f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
            }

            Column {
                Text("Height: ${AppState.settings.triggerHeight.toInt()}dp", color = GhostWhite)
                Slider(
                    value = AppState.settings.triggerHeight,
                    onValueChange = { AppState.settings = AppState.settings.copy(triggerHeight = it); AppState.saveSettings(context) },
                    valueRange = 50f..300f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
            }
            
            Column {
                
            Column {
                Text("Offset Y: ${AppState.settings.triggerOffsetY.toInt()}dp", color = GhostWhite)
                Slider(
                    value = AppState.settings.triggerOffsetY,
                    onValueChange = { AppState.settings = AppState.settings.copy(triggerOffsetY = it); AppState.saveSettings(context) },
                    valueRange = -500f..500f,
                    colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                )
            }
Text("Trigger Action", color = GhostWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Obsidian)
                ) {
                    val actions = listOf("Swipe", "Single", "Double")
                    actions.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { AppState.settings = AppState.settings.copy(triggerAction = index); AppState.saveSettings(context) }
                                .background(if (AppState.settings.triggerAction == index) NeonPurple else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = GhostWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            Column {
                Text("Trigger Mode", color = GhostWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Obsidian)
                ) {
                    val modes = listOf("Visual Slider", "Full Screen Edge")
                    modes.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { AppState.settings = AppState.settings.copy(triggerMode = index); AppState.saveSettings(context) }
                                .background(if (AppState.settings.triggerMode == index) NeonPurple else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = GhostWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
            
            Column {
                Text("Slider Color", color = GhostWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val colors = listOf(ElectricBlue, NeonPurple, HyperPink, Color(0xFFFFC107), Color(0xFF4CAF50), Color.White)
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(2.dp, if (AppState.settings.triggerColor == color) GhostWhite else Color.Transparent, CircleShape)
                                .clickable { AppState.settings = AppState.settings.copy(triggerColorArgb = color.toArgb(), triggerGradientColorArgb = null); AppState.saveSettings(context) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceTab() {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Panel Design", color = ElectricBlue, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("Design Style", color = GhostWhite)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Obsidian)
                ) {
                    val designs = listOf("Sci-Fi Gamer", "Minimal Sleek")
                    designs.forEachIndexed { index, name ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { AppState.settings = AppState.settings.copy(sidebarDesign = index); AppState.saveSettings(context) }
                                .background(if (AppState.settings.sidebarDesign == index) NeonPurple else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(name, color = GhostWhite, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        Text("Visual & Opacity", color = ElectricBlue, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("Sidebar Opacity", color = GhostWhite)
                Slider(
                    value = AppState.settings.sidebarOpacity,
                    onValueChange = { AppState.settings = AppState.settings.copy(sidebarOpacity = it); AppState.saveSettings(context) },
                    valueRange = 0.5f..1f,
                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                )
            }
            Column {
                Text("Default Window Opacity", color = GhostWhite)
                Slider(
                    value = AppState.settings.windowOpacity,
                    onValueChange = { AppState.settings = AppState.settings.copy(windowOpacity = it); AppState.saveSettings(context) },
                    valueRange = 0.3f..1f,
                    colors = SliderDefaults.colors(thumbColor = ElectricBlue, activeTrackColor = ElectricBlue)
                )
            }
        }
    }
}
