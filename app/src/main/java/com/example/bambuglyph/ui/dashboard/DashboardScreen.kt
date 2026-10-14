package com.example.bambuglyph.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.bambuglyph.data.PreferencesManager
import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.glyph.GlyphMatrixRenderer
import com.example.bambuglyph.model.GlyphDeviceType
import com.example.bambuglyph.model.PrintState
import com.example.bambuglyph.model.PrinterConfig
import com.example.bambuglyph.service.PrinterForegroundService
import com.example.bambuglyph.theme.BorderDark
import com.example.bambuglyph.theme.CardDark
import com.example.bambuglyph.theme.NothingBlack
import com.example.bambuglyph.theme.NothingRed
import com.example.bambuglyph.theme.NothingWhite
import com.example.bambuglyph.theme.StatusAmber
import com.example.bambuglyph.theme.StatusBlue
import com.example.bambuglyph.theme.StatusGreen
import com.example.bambuglyph.theme.StatusRed
import com.example.bambuglyph.theme.TextMuted
import com.example.bambuglyph.theme.TextPrimary
import com.example.bambuglyph.theme.TextSecondary
import com.example.bambuglyph.ui.components.GlyphMatrixView
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToOnboarding: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val scope = rememberCoroutineScope()

    val status by PrinterRepository.status.collectAsState()
    val config by PrinterRepository.config.collectAsState()
    val isRunning by PrinterRepository.isServiceRunning.collectAsState()
    val matrixData by PrinterRepository.currentGlyphFrame.collectAsState()
    val matrixSize by PrinterRepository.currentMatrixSize.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var previewSizeOverride by remember { mutableStateOf<Int?>(null) }

    // 5-tap hidden demo mode toggle in header
    var headerTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }
    var demoUnlocked by remember { mutableStateOf(config.mockMode) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    val activeSize = previewSizeOverride ?: matrixSize
    // If override changed size, generate preview frame
    val displayFrame = if (previewSizeOverride != null && previewSizeOverride != matrixSize) {
        remember(status, activeSize) {
            GlyphMatrixRenderer.render(status, activeSize)
        }
    } else {
        matrixData
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = NothingBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastTapTime > 2000L) {
                            headerTapCount = 0
                        }
                        lastTapTime = now
                        headerTapCount++
                        if (headerTapCount >= 5) {
                            headerTapCount = 0
                            val newMode = !demoUnlocked
                            demoUnlocked = newMode
                            val updated = config.copy(mockMode = newMode)
                            prefs.savePrinterConfig(updated)
                            PrinterRepository.updateConfig(updated)
                            feedbackMsg = if (newMode) "DEMO MODE UNLOCKED" else "DEMO MODE DISABLED"
                            if (isRunning) {
                                PrinterForegroundService.stop(context)
                                PrinterForegroundService.start(context)
                            }
                        }
                    }
                ) {
                    Text(
                        text = "3D PRINTER",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = if (demoUnlocked) "DEMO SIMULATOR" else if (config.serialNumber.isNotBlank()) config.serialNumber else "MONITOR",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = if (demoUnlocked) NothingRed else TextMuted
                        )
                    )
                    if (feedbackMsg != null) {
                        Text(
                            text = feedbackMsg ?: "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = NothingRed,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status Badge
                    ConnectionBadge(
                        isConnected = status.isConnected,
                        isMock = demoUnlocked
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { showSettingsSheet = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .border(1.dp, BorderDark, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Live Glyph Matrix Visualizer
            GlyphMatrixView(
                matrixData = displayFrame,
                matrixSize = activeSize,
                label = if (activeSize == 25) "PHONE (3) • 25x25" else "PHONE (4a) PRO • 13x13"
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Matrix Toggle Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PREVIEW MATRIX: ",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                )

                SizePill(
                    label = "25x25",
                    isSelected = activeSize == 25,
                    onClick = { previewSizeOverride = 25 }
                )
                Spacer(modifier = Modifier.width(6.dp))
                SizePill(
                    label = "13x13",
                    isSelected = activeSize == 13,
                    onClick = { previewSizeOverride = 13 }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Print State & Progress Card
            PrintStateCard(status = status)

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Gauges (Temps)
            TelemetryRow(status = status)

            Spacer(modifier = Modifier.height(20.dp))

            // Background Service Start / Stop Button
            Button(
                onClick = {
                    if (isRunning) {
                        PrinterForegroundService.stop(context)
                    } else {
                        PrinterForegroundService.start(context)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) CardDark else NothingRed
                ),
                shape = RoundedCornerShape(8.dp),
                border = if (isRunning) ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StatusRed)
                ) else null
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isRunning) StatusRed else NothingBlack,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunning) "STOP BACKGROUND MONITORING" else "START BACKGROUND MONITORING",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isRunning) StatusRed else NothingBlack,
                        letterSpacing = 1.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isRunning)
                    "ACTIVE • Maintaining partial wake lock & MQTT socket when screen is locked."
                else
                    "STANDBY • Start background service to sync Glyph Matrix when screen is off.",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = if (isRunning) StatusGreen else TextMuted
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            // Demo / Test States Section (Hidden by default; unlocked by tapping header 5 times)
            if (demoUnlocked) {
                Spacer(modifier = Modifier.height(24.dp))
                DemoTestSection(
                    onSelectState = { state ->
                        if (!isRunning) {
                            PrinterForegroundService.start(context)
                        }
                        PrinterForegroundService.setDemoState(context, state)
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Settings Bottom Sheet
    if (showSettingsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = CardDark,
            contentColor = TextPrimary
        ) {
            SettingsSheetContent(
                currentConfig = config,
                onSave = { updated ->
                    prefs.savePrinterConfig(updated)
                    PrinterRepository.updateConfig(updated)
                    // If service running, restart it with new config
                    if (isRunning) {
                        PrinterForegroundService.stop(context)
                        PrinterForegroundService.start(context)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSettingsSheet = false
                    }
                },
                onRelaunchOnboarding = {
                    showSettingsSheet = false
                    onNavigateToOnboarding()
                }
            )
        }
    }
}

@Composable
private fun ConnectionBadge(isConnected: Boolean, isMock: Boolean) {
    val (color, text) = when {
        isMock -> StatusAmber to "DEMO"
        isConnected -> StatusGreen to "ONLINE"
        else -> TextMuted to "OFFLINE"
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
private fun PrintStateCard(status: com.example.bambuglyph.model.PrinterStatus) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // State Badge
                val (badgeColor, badgeText) = when (status.printState) {
                    PrintState.PREHEAT -> NothingRed to "PREHEAT [PH]"
                    PrintState.PRINTING -> StatusBlue to "PRINTING"
                    PrintState.PAUSED -> StatusAmber to "PAUSED [||]"
                    PrintState.COMPLETED -> StatusGreen to "COMPLETED [100% TICK]"
                    PrintState.FAILED -> StatusRed to "FAILED"
                    PrintState.IDLE -> TextMuted to "STANDBY"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        )
                    }
                }

                // Remaining Time
                Text(
                    text = if (status.printState == PrintState.PRINTING || status.printState == PrintState.PREHEAT)
                        "${status.remainingMinutes}m REMAINING"
                    else if (status.printState == PrintState.COMPLETED) "FINISHED"
                    else "--",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtask Name
            Text(
                text = if (status.subtaskName.isNotBlank()) status.subtaskName else "3d_model.gcode",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { (status.percent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = NothingRed,
                trackColor = NothingBlack
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROGRESS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted
                    )
                )
                Text(
                    text = "${status.percent}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun TelemetryRow(status: com.example.bambuglyph.model.PrinterStatus) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TelemetryCard(
            label = "NOZZLE TEMP",
            current = "${status.nozzleTemp.toInt()}°C",
            target = "${status.nozzleTargetTemp.toInt()}°C",
            modifier = Modifier.weight(1f)
        )
        TelemetryCard(
            label = "BED TEMP",
            current = "${status.bedTemp.toInt()}°C",
            target = "${status.bedTargetTemp.toInt()}°C",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TelemetryCard(label: String, current: String, target: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = current,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "/ $target",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun DemoTestSection(onSelectState: (PrintState) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "INTERACTIVE GLYPH TEST CONTROLS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed
                )
            )
            Text(
                text = "Tap any state to immediately display its dot-matrix LED pattern on rear hardware and simulator:",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoButton("PH", "PREHEAT", Modifier.weight(1f)) {
                    onSelectState(PrintState.PREHEAT)
                }
                DemoButton("75%", "PRINT", Modifier.weight(1f)) {
                    onSelectState(PrintState.PRINTING)
                }
                DemoButton("||", "PAUSE", Modifier.weight(1f)) {
                    onSelectState(PrintState.PAUSED)
                }
                DemoButton("TICK", "100%", Modifier.weight(1f)) {
                    onSelectState(PrintState.COMPLETED)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DemoButton("IDLE", "STANDBY", Modifier.weight(1f)) {
                    onSelectState(PrintState.IDLE)
                }
                DemoButton("ERR", "FAILED", Modifier.weight(1f)) {
                    onSelectState(PrintState.FAILED)
                }
            }
        }
    }
}

@Composable
private fun DemoButton(tag: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(NothingBlack)
            .border(1.dp, BorderDark, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tag,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed
                )
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun SizePill(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) NothingRed else NothingBlack)
            .border(1.dp, if (isSelected) NothingRed else BorderDark, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = if (isSelected) NothingWhite else TextSecondary
            )
        )
    }
}

@Composable
private fun SettingsSheetContent(
    currentConfig: PrinterConfig,
    onSave: (PrinterConfig) -> Unit,
    onRelaunchOnboarding: () -> Unit
) {
    var ip by remember { mutableStateOf(currentConfig.ipAddress) }
    var port by remember { mutableStateOf(currentConfig.port.toString()) }
    var serial by remember { mutableStateOf(currentConfig.serialNumber) }
    var code by remember { mutableStateOf(currentConfig.accessCode) }
    var deviceType by remember { mutableStateOf(currentConfig.deviceType) }
    var mockMode by remember { mutableStateOf(currentConfig.mockMode) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "PRINTER & HARDWARE SETTINGS",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("PRINTER IP", fontFamily = FontFamily.Monospace) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("PORT", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.weight(0.35f)
            )
            OutlinedTextField(
                value = serial,
                onValueChange = { serial = it },
                label = { Text("SERIAL", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.weight(0.65f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("ACCESS CODE", fontFamily = FontFamily.Monospace) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                onSave(
                    currentConfig.copy(
                        ipAddress = ip.trim(),
                        port = port.toIntOrNull() ?: 8883,
                        serialNumber = serial.trim(),
                        accessCode = code.trim(),
                        deviceType = deviceType,
                        mockMode = mockMode
                    )
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "APPLY SETTINGS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onRelaunchOnboarding,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "RELAUNCH ONBOARDING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary
                )
            )
        }
    }
}
