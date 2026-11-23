package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ai.configureAppCheck
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.NotesScreen
import com.example.ui.screens.PermissionsPrivacyScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.TextChatScreen
import com.example.ui.screens.VoiceAssistantScreen
import com.example.ui.state.ParmarViewModel
import com.example.ui.theme.CoolSlate
import com.example.ui.theme.DeepCobalt
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceWhite
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ParmarAiTheme
import com.example.ui.theme.VoidBlack

object ParmarRoutes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val VOICE = "voice"
    const val CHAT = "chat"
    const val NOTES = "notes"
    const val REMINDERS = "reminders"
    const val MEMORY = "memory"
    const val SETTINGS = "settings"
    const val PERMISSIONS = "permissions"
}

private data class NavTabItem(
    val route: String,
    val labelEn: String,
    val labelGu: String,
    val icon: ImageVector,
    val testTag: String
)

private val primaryTabs = listOf(
    NavTabItem(ParmarRoutes.HOME, "Home", "હોમ", Icons.Filled.Home, "nav_home"),
    NavTabItem(ParmarRoutes.VOICE, "Voice", "વોઇસ", Icons.Filled.GraphicEq, "nav_voice"),
    NavTabItem(ParmarRoutes.CHAT, "Chat", "ચેટ", Icons.Filled.Chat, "nav_chat"),
    NavTabItem(ParmarRoutes.NOTES, "Notes", "નોટ્સ", Icons.Filled.Description, "nav_notes"),
    NavTabItem(ParmarRoutes.REMINDERS, "Alarms", "રિમાઇન્ડર", Icons.Filled.Alarm, "nav_reminders")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Phase 1: Configure Firebase App Check with launch intent before UI/ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            ParmarAiTheme {
                ParmarRootApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureAppCheck(this, intent)
    }
}

@Composable
fun ParmarRootApp(
    viewModel: ParmarViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ParmarRoutes.SPLASH
    val context = LocalContext.current
    val isGu = uiState.settings.language == com.example.data.local.AssistantLanguage.GUJARATI

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshSystemStatus()
        if (granted) {
            viewModel.onOrbClicked()
        }
    }

    val adminLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshSystemStatus()
    }

    fun handleOrbAction() {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            viewModel.onOrbClicked()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun handleWakeStandbyAction() {
        val hasMic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasMic) {
            viewModel.startWakeWordMode()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun navigateTopLevel(route: String) {
        navController.navigate(route) {
            popUpTo(ParmarRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val showChrome = currentRoute != ParmarRoutes.SPLASH

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 640.dp

        Scaffold(
            containerColor = VoidBlack,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = IceWhite,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val tabLabel = if (isGu) tab.labelGu else tab.labelEn
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(imageVector = tab.icon, contentDescription = tabLabel)
                                },
                                label = { Text(tabLabel) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = DeepCobalt,
                                    unselectedIconColor = CoolSlate,
                                    unselectedTextColor = CoolSlate
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        contentColor = IceWhite
                    ) {
                        primaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val tabLabel = if (isGu) tab.labelGu else tab.labelEn
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(imageVector = tab.icon, contentDescription = tabLabel)
                                },
                                label = { Text(tabLabel) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = DeepCobalt,
                                    unselectedIconColor = CoolSlate,
                                    unselectedTextColor = CoolSlate
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = ParmarRoutes.SPLASH
                    ) {
                        composable(ParmarRoutes.SPLASH) {
                            SplashScreen(
                                onFinished = {
                                    navController.navigate(ParmarRoutes.HOME) {
                                        popUpTo(ParmarRoutes.SPLASH) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(ParmarRoutes.HOME) {
                            HomeScreen(
                                uiState = uiState,
                                onOrbClick = ::handleOrbAction,
                                onStartWakeStandby = ::handleWakeStandbyAction,
                                onStopListening = viewModel::stopAllListeningCompletely,
                                onQuickAction = viewModel::executeQuickAction,
                                onNavigateToVoice = { navigateTopLevel(ParmarRoutes.VOICE) },
                                onNavigateToChat = { navigateTopLevel(ParmarRoutes.CHAT) },
                                onNavigateToNotes = { navigateTopLevel(ParmarRoutes.NOTES) },
                                onNavigateToReminders = { navigateTopLevel(ParmarRoutes.REMINDERS) },
                                onNavigateToMemory = { navController.navigate(ParmarRoutes.MEMORY) },
                                onNavigateToSettings = { navController.navigate(ParmarRoutes.SETTINGS) },
                                onNavigateToPermissions = { navController.navigate(ParmarRoutes.PERMISSIONS) },
                                onDismissBanner = viewModel::clearBanners
                            )
                        }

                        composable(ParmarRoutes.VOICE) {
                            VoiceAssistantScreen(
                                uiState = uiState,
                                onOrbClick = ::handleOrbAction,
                                onStartWakeStandby = ::handleWakeStandbyAction,
                                onStopAll = viewModel::stopAllListeningCompletely,
                                onToggleLanguage = viewModel::updateLanguage,
                                onRunSampleVoiceCommand = { cmd ->
                                    viewModel.submitPrompt(cmd, speakReply = true)
                                },
                                onDismissBanner = viewModel::clearBanners
                            )
                        }

                        composable(ParmarRoutes.CHAT) {
                            TextChatScreen(
                                uiState = uiState,
                                onSendMessage = { msg -> viewModel.submitPrompt(msg, speakReply = false) },
                                onVoiceMicTap = ::handleOrbAction,
                                onStopListening = viewModel::stopAllListeningCompletely,
                                onApproveMemory = viewModel::approvePendingMemory,
                                onDismissMemory = viewModel::dismissPendingMemory,
                                onClearHistory = viewModel::clearChatHistory,
                                onDismissBanner = viewModel::clearBanners
                            )
                        }

                        composable(ParmarRoutes.NOTES) {
                            NotesScreen(
                                uiState = uiState,
                                onSaveNote = viewModel::saveNote,
                                onTogglePin = viewModel::toggleNotePin,
                                onDeleteNote = viewModel::deleteNote,
                                onStopListening = viewModel::stopAllListeningCompletely
                            )
                        }

                        composable(ParmarRoutes.REMINDERS) {
                            RemindersScreen(
                                uiState = uiState,
                                onSaveReminder = viewModel::saveReminder,
                                onToggleComplete = viewModel::toggleReminderComplete,
                                onDeleteReminder = viewModel::deleteReminder,
                                onOpenSystemAlarm = {
                                    viewModel.phoneControl.setSystemAlarm(7, 0, "PARMAR AI Alarm")
                                },
                                onStopListening = viewModel::stopAllListeningCompletely
                            )
                        }

                        composable(ParmarRoutes.MEMORY) {
                            MemoryScreen(
                                uiState = uiState,
                                onSaveMemory = viewModel::saveMemory,
                                onDeleteMemory = viewModel::deleteMemory,
                                onUnlockPrivate = viewModel::requestUnlockPrivateMemory,
                                onLockPrivate = viewModel::lockPrivateMemory,
                                onExportBackupJson = viewModel::exportBackupJson,
                                onImportBackupJson = viewModel::importBackupJson,
                                onStopListening = viewModel::stopAllListeningCompletely,
                                onDismissBanner = viewModel::clearBanners
                            )
                        }

                        composable(ParmarRoutes.SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onSaveWakeWord = viewModel::updateWakeWord,
                                onLanguageSelected = viewModel::updateLanguage,
                                onVoiceSpeedChanged = viewModel::updateVoiceSpeed,
                                onVoicePitchChanged = viewModel::updateVoicePitch,
                                onFemaleVoiceSelected = viewModel::updateFemaleVoice,
                                onTestFemaleVoice = viewModel::testFemaleVoice,
                                onAiCloudToggle = viewModel::updateAiCloudEnabled,
                                onOfflineModeToggle = viewModel::updateOfflineMode,
                                onBackgroundListeningToggle = viewModel::updateBackgroundListening,
                                onVoiceProfileToggle = viewModel::updateVoiceProfile,
                                onRequireDeviceAuthToggle = viewModel::updateRequireDeviceAuth,
                                onVolumeChanged = viewModel::setAssistantVolume,
                                onClearChatHistory = viewModel::clearChatHistory,
                                onStopListening = viewModel::stopAllListeningCompletely,
                                onDismissBanner = viewModel::clearBanners
                            )
                        }

                        composable(ParmarRoutes.PERMISSIONS) {
                            PermissionsPrivacyScreen(
                                uiState = uiState,
                                onBack = { navController.popBackStack() },
                                onRefreshStatus = viewModel::refreshSystemStatus,
                                onRequestDeviceAdmin = {
                                    adminLauncher.launch(viewModel.phoneControl.createDeviceAdminSetupIntent())
                                },
                                onOpenExactAlarmSettings = {
                                    viewModel.phoneControl.openExactAlarmSettings()
                                },
                                onOpenBatterySettings = {
                                    viewModel.phoneControl.openBatteryOptimizationSettings()
                                },
                                onStopListening = viewModel::stopAllListeningCompletely
                            )
                        }
                    }
                }
            }
        }
    }
}
