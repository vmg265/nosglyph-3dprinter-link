package com.example.bambuglyph.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bambuglyph.data.PreferencesManager
import com.example.bambuglyph.data.PrinterRepository
import com.example.bambuglyph.model.GlyphDeviceType
import com.example.bambuglyph.model.PrinterConfig
import com.example.bambuglyph.theme.BorderDark
import com.example.bambuglyph.theme.CardDark
import com.example.bambuglyph.theme.NothingBlack
import com.example.bambuglyph.theme.NothingRed
import com.example.bambuglyph.theme.NothingWhite
import com.example.bambuglyph.theme.StatusGreen
import com.example.bambuglyph.theme.TextMuted
import com.example.bambuglyph.theme.TextPrimary
import com.example.bambuglyph.theme.TextSecondary

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var currentStep by remember { mutableIntStateOf(0) }

    // Configuration state
    var ipAddress by remember { mutableStateOf("192.168.1.100") }
    var port by remember { mutableStateOf("8883") }
    var serialNumber by remember { mutableStateOf("01P00A000000000") }
    var accessCode by remember { mutableStateOf("12345678") }
    var selectedDeviceType by remember { mutableStateOf(GlyphDeviceType.AUTO_DETECT) }

    // Permission states
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val isIgnoringBatteryOptimizations = remember {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    var batteryExemptGranted by remember { mutableStateOf(isIgnoringBatteryOptimizations) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NothingBlack)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Step Header Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GLYPH MONITOR // ONBOARDING",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = NothingRed
                )
            )

            Text(
                text = "STEP ${currentStep + 1} OF 3",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Progress indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 0..2) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(
                            if (i <= currentStep) NothingRed else BorderDark
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        when (currentStep) {
            0 -> WelcomeStep(onNext = { currentStep = 1 })
            1 -> PermissionsStep(
                hasNotification = hasNotificationPermission,
                hasBatteryExempt = batteryExemptGranted,
                onRequestNotification = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onRequestBattery = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                        batteryExemptGranted = true
                    } catch (_: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                onNext = { currentStep = 2 }
            )
            2 -> DeviceConfigStep(
                ip = ipAddress,
                onIpChange = { ipAddress = it },
                port = port,
                onPortChange = { port = it },
                serial = serialNumber,
                onSerialChange = { serialNumber = it },
                accessCode = accessCode,
                onAccessCodeChange = { accessCode = it },
                deviceType = selectedDeviceType,
                onDeviceTypeChange = { selectedDeviceType = it },
                onComplete = {
                    val config = PrinterConfig(
                        ipAddress = ipAddress.trim(),
                        port = port.toIntOrNull() ?: 8883,
                        serialNumber = serialNumber.trim(),
                        accessCode = accessCode.trim(),
                        deviceType = selectedDeviceType
                    )
                    prefs.savePrinterConfig(config)
                    prefs.isOnboardingCompleted = true
                    PrinterRepository.updateConfig(config)
                    onComplete()
                }
            )
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column {
        Text(
            text = "REAR MATRIX MONITOR",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = TextPrimary
            )
        )
        Text(
            text = "PRINT STATUS TELEMETRY",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                color = NothingRed
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Synchronize live telemetry from your 3D printer directly onto the rear Glyph Matrix of your Nothing Phone (3) and Nothing Phone (4a) Pro.",
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        FeatureCard(
            icon = Icons.Default.Layers,
            title = "BACKGROUND AND SCREEN-OFF SYNC",
            desc = "Maintains persistent MQTT telemetry when your screen is locked. Real-time updates without battery drain."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            icon = Icons.Default.Print,
            title = "DYNAMIC GLYPH PATTERNS",
            desc = "Preheat indicator (PH), real-time print time countdown, paused print symbol, and 100% completion tick with breathing effect."
        )

        Spacer(modifier = Modifier.height(12.dp))

        FeatureCard(
            icon = Icons.Default.Settings,
            title = "NATIVE GLYPH TOY SUPPORT",
            desc = "Integrated with Nothing OS Glyph Toy Carousel and AOD (Always-On Display) service."
        )

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "CONTINUE TO PERMISSIONS",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun PermissionsStep(
    hasNotification: Boolean,
    hasBatteryExempt: Boolean,
    onRequestNotification: () -> Unit,
    onRequestBattery: () -> Unit,
    onNext: () -> Unit
) {
    Column {
        Text(
            text = "SYSTEM PERMISSIONS",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "REQUIRED FOR PERSISTENT MONITORING",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Notification Permission Card
        PermissionItemCard(
            icon = Icons.Default.Notifications,
            title = "NOTIFICATIONS",
            desc = "Required by Android 14+ for foreground services to display live print progress in the status bar.",
            isGranted = hasNotification,
            onAction = onRequestNotification,
            actionText = "GRANT"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Battery Optimization Card
        PermissionItemCard(
            icon = Icons.Default.BatteryAlert,
            title = "BATTERY OPTIMIZATION EXEMPTION",
            desc = "Prevents Nothing OS from suspending the background MQTT connection when the phone screen turns off.",
            isGranted = hasBatteryExempt,
            onAction = onRequestBattery,
            actionText = "EXEMPT"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Glyph SDK Info Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CardDark)
                .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = StatusGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "NOTHING GLYPH MATRIX SDK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "com.nothing.ketchum.permission.ENABLE is declared in manifest for direct rear LED grid rendering.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "CONTINUE TO DEVICE SETUP",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
private fun DeviceConfigStep(
    ip: String,
    onIpChange: (String) -> Unit,
    port: String,
    onPortChange: (String) -> Unit,
    serial: String,
    onSerialChange: (String) -> Unit,
    accessCode: String,
    onAccessCodeChange: (String) -> Unit,
    deviceType: GlyphDeviceType,
    onDeviceTypeChange: (GlyphDeviceType) -> Unit,
    onComplete: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }

    Column {
        Text(
            text = "PRINTER & HARDWARE",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        Text(
            text = "PRINTER CREDENTIALS",
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hardware Selection
        Text(
            text = "TARGET NOTHING HARDWARE",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlyphDeviceType.entries.forEach { type ->
                val isSelected = deviceType == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NothingRed else CardDark)
                        .border(1.dp, if (isSelected) NothingRed else BorderDark, RoundedCornerShape(6.dp))
                        .clickable { onDeviceTypeChange(type) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (type) {
                            GlyphDeviceType.PHONE_3 -> "PHONE (3)\n25x25"
                            GlyphDeviceType.PHONE_4A_PRO -> "4A PRO\n13x13"
                            GlyphDeviceType.AUTO_DETECT -> "AUTO\nDETECT"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) NothingWhite else TextSecondary,
                            lineHeight = 14.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Printer Network Credentials
        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            label = { Text("PRINTER IP ADDRESS", fontFamily = FontFamily.Monospace) },
            placeholder = { Text("192.168.1.100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = port,
                onValueChange = onPortChange,
                label = { Text("PORT", fontFamily = FontFamily.Monospace) },
                singleLine = true,
                modifier = Modifier.weight(0.35f),
                colors = textFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = serial,
                onValueChange = onSerialChange,
                label = { Text("SERIAL NUMBER", fontFamily = FontFamily.Monospace) },
                placeholder = { Text("01P00A000000000") },
                singleLine = true,
                modifier = Modifier.weight(0.65f),
                colors = textFieldColors()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = accessCode,
            onValueChange = onAccessCodeChange,
            label = { Text("LAN ACCESS CODE", fontFamily = FontFamily.Monospace) },
            placeholder = { Text("8-digit code from screen") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = textFieldColors(),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onComplete,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "COMPLETE SETUP & START",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
private fun FeatureCard(icon: ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NothingBlack)
                .border(1.dp, BorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = NothingWhite, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 18.sp)
            )
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    desc: String,
    isGranted: Boolean,
    onAction: () -> Unit,
    actionText: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CardDark)
            .border(1.dp, if (isGranted) StatusGreen.copy(alpha = 0.5f) else BorderDark, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isGranted) StatusGreen.copy(alpha = 0.2f) else NothingBlack)
                .border(1.dp, if (isGranted) StatusGreen else BorderDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isGranted) Icons.Default.Check else icon,
                contentDescription = null,
                tint = if (isGranted) StatusGreen else NothingWhite,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary, lineHeight = 16.sp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
            Text(
                text = "ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = StatusGreen
                )
            )
        } else {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(4.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(NothingRed))
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = NothingRed
                    )
                )
            }
        }
    }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = NothingRed,
    unfocusedBorderColor = BorderDark,
    focusedLabelColor = NothingRed,
    unfocusedLabelColor = TextMuted,
    cursorColor = NothingRed,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)
