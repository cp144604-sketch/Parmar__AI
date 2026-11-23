package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.ParmarGlassCard
import com.example.ui.components.ParmarTelemetryHeader
import com.example.ui.state.ParmarUiState
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.IceWhite
import com.example.ui.theme.RoyalElectricBlue
import com.example.ui.theme.VoidBlack

@Composable
fun PermissionsPrivacyScreen(
    uiState: ParmarUiState,
    onBack: () -> Unit,
    onRefreshStatus: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onStopListening: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onRefreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onRefreshStatus() }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { onRefreshStatus() }

    val health = uiState.permissionHealth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        ParmarTelemetryHeader(
            title = "Permissions & Privacy",
            voiceState = uiState.voiceState,
            wakeWord = uiState.settings.wakeWord,
            isOnline = uiState.isNetworkConnected,
            isOfflineModeEnabled = uiState.settings.offlineModeEnabled,
            onStopListening = onStopListening,
            trailingActions = {
                IconButton(onClick = onBack, modifier = Modifier.testTag("permissions_back_btn")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = IceWhite)
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.CenterHorizontally),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ParmarGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = ElectricCyan.copy(alpha = 0.55f)
                ) {
                    Text("TRANSPARENT PRIVACY ARCHITECTURE", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• No secret recording or continuous microphone audio upload.\n" +
                            "• Wake word \"${uiState.settings.wakeWord}\" uses on-device speech recognition preference.\n" +
                            "• Notes, Reminders, and Memories stay encrypted inside local device storage.\n" +
                            "• AI queries use Firebase App Check attested Gemini only when online and approved.\n" +
                            "• Screen locking & security controls never bypass Android PIN/Biometrics.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = IceWhite
                    )
                }
            }

            item {
                PermissionRowCard(
                    title = "Microphone Access (RECORD_AUDIO)",
                    description = "Required for DIAMOND wake-word listening and Gujarati/English voice commands. When background listening is ON, a visible persistent notification is always displayed.",
                    isGranted = health.hasMicPermission,
                    buttonLabel = if (health.hasMicPermission) "Granted" else "Grant Mic",
                    enabled = !health.hasMicPermission,
                    testTag = "grant_mic_btn",
                    onAction = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                )
            }

            item {
                PermissionRowCard(
                    title = "Notifications (POST_NOTIFICATIONS)",
                    description = "Required on Android 13–15+ (OPPO A3 5G) to show ongoing microphone service controls and fire scheduled local reminders.",
                    isGranted = health.hasNotificationPermission,
                    buttonLabel = if (health.hasNotificationPermission) "Granted" else "Allow Alerts",
                    enabled = !health.hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                    testTag = "grant_notif_btn",
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            item {
                PermissionRowCard(
                    title = "Exact Reminder Alarms",
                    description = "Ensures local reminders notify on time on Android 12–15+ without OS deferral.",
                    isGranted = health.canScheduleExactAlarms,
                    buttonLabel = if (health.canScheduleExactAlarms) "Allowed" else "Open Alarm Access",
                    enabled = !health.canScheduleExactAlarms,
                    testTag = "grant_alarm_btn",
                    onAction = onOpenExactAlarmSettings
                )
            }

            item {
                PermissionRowCard(
                    title = "Device Admin (Optional Screen Lock)",
                    description = "Allows PARMAR AI to lock the screen ONLY when you explicitly say \"Lock Phone\" or tap Lock. Requires your manual Android approval.",
                    isGranted = health.isDeviceAdminEnabled,
                    buttonLabel = if (health.isDeviceAdminEnabled) "Enabled" else "Enable Lock Admin",
                    enabled = !health.isDeviceAdminEnabled,
                    testTag = "grant_admin_btn",
                    onAction = onRequestDeviceAdmin
                )
            }

            item {
                ParmarGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Text("OPPO A3 5G / COLOROS BATTERY GUIDE", style = MaterialTheme.typography.labelLarge, color = ElectricCyan)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Android 15 and ColorOS restrict background microphone services during deep sleep. For reliable screen-off \"${uiState.settings.wakeWord}\" standby:\n" +
                            "1. Set Battery usage to Unrestricted / Don't Optimize.\n" +
                            "2. Lock PARMAR AI in Recent Apps if desired.\n" +
                            "3. Note: Some hardware DSPs restrict microphone wake when screen is locked unless foreground service is active.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoolSlate
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onOpenBatterySettings,
                        modifier = Modifier.testTag("battery_opt_btn")
                    ) {
                        Text(
                            text = if (health.isIgnoringBatteryOptimizations) "Battery Unrestricted ✓" else "Open Battery Optimization Settings",
                            color = ElectricCyan
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRowCard(
    title: String,
    description: String,
    isGranted: Boolean,
    buttonLabel: String,
    enabled: Boolean,
    testTag: String,
    onAction: () -> Unit
) {
    ParmarGlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (isGranted) EmeraldActive.copy(alpha = 0.55f) else AmberWarning.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    contentDescription = null,
                    tint = if (isGranted) EmeraldActive else AmberWarning
                )
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite
                )
            }
            Button(
                onClick = onAction,
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RoyalElectricBlue,
                    disabledContainerColor = EmeraldActive.copy(alpha = 0.18f)
                ),
                modifier = Modifier.testTag(testTag)
            ) {
                Text(buttonLabel, color = IceWhite)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = description, style = MaterialTheme.typography.bodyMedium, color = CoolSlate)
    }
}
