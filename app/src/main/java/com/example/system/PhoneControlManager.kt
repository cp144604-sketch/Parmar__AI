package com.example.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.AlarmClock
import android.provider.Settings
import com.example.data.local.AssistantLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppLaunchInfo(
    val label: String,
    val packageName: String
)

data class ControlResult(
    val success: Boolean,
    val messageGu: String,
    val messageEn: String
)

class PhoneControlManager(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val devicePolicyManager =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn: StateFlow<Boolean> = _isFlashlightOn.asStateFlow()

    private val adminComponent = ComponentName(context, ParmarDeviceAdminReceiver::class.java)

    private val gujaratiAppAliases = mapOf(
        "વોટ્સએપ" to "whatsapp",
        "વૉટ્સઍપ" to "whatsapp",
        "યુટ્યુબ" to "youtube",
        "કેમેરા" to "camera",
        "કૅમેરા" to "camera",
        "ફોન" to "phone",
        "ડાયલર" to "phone",
        "સેટિંગ્સ" to "settings",
        "સેટિંગ" to "settings",
        "ક્રોમ" to "chrome",
        "જીમેલ" to "gmail",
        "મેપ્સ" to "maps",
        "કેલ્ક્યુલેટર" to "calculator",
        "ઘડિયાળ" to "clock",
        "ગેલેરી" to "photos"
    )

    fun setFlashlight(enabled: Boolean): ControlResult {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && lensFacing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId == null) {
                ControlResult(
                    success = false,
                    messageGu = "માફ કરશો Sir, આ ડિવાઇસમાં ફ્લેશલાઇટ સપોર્ટ ઉપલબ્ધ નથી.",
                    messageEn = "Sorry Sir, flashlight hardware is not available on this device."
                )
            } else {
                cameraManager.setTorchMode(cameraId, enabled)
                _isFlashlightOn.value = enabled
                if (enabled) {
                    ControlResult(
                        success = true,
                        messageGu = "Sir, ફ્લેશલાઇટ ચાલુ કરી દીધી છે.",
                        messageEn = "Sir, I have turned on the flashlight."
                    )
                } else {
                    ControlResult(
                        success = true,
                        messageGu = "Sir, ફ્લેશલાઇટ બંધ કરી દીધી છે.",
                        messageEn = "Sir, I have turned off the flashlight."
                    )
                }
            }
        } catch (e: Exception) {
            ControlResult(
                success = false,
                messageGu = "Sir, ફ્લેશલાઇટ નિયંત્રિત કરવામાં મુશ્કેલી આવી: ${e.localizedMessage ?: "Error"}",
                messageEn = "Sir, could not toggle flashlight: ${e.localizedMessage ?: "Unsupported"}"
            )
        }
    }

    fun getInstalledLauncherApps(): List<AppLaunchInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = context.packageManager
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }
        return resolveInfos
            .map { info ->
                AppLaunchInfo(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun openAppByName(rawQuery: String): ControlResult {
        val cleaned = rawQuery.trim().lowercase(Locale.ROOT)
        if (cleaned.isEmpty()) {
            return ControlResult(
                false,
                "Sir, કઈ એપ ખોલવી છે તે જણાવો.",
                "Sir, please tell me which app to open."
            )
        }

        val mappedQuery = gujaratiAppAliases.entries
            .firstOrNull { cleaned.contains(it.key) }?.value ?: cleaned

        val apps = getInstalledLauncherApps()
        val match = apps.firstOrNull { it.label.lowercase(Locale.ROOT) == mappedQuery }
            ?: apps.firstOrNull { it.label.lowercase(Locale.ROOT).contains(mappedQuery) }
            ?: apps.firstOrNull { mappedQuery.contains(it.label.lowercase(Locale.ROOT)) }
            ?: apps.firstOrNull { it.packageName.lowercase(Locale.ROOT).contains(mappedQuery) }

        if (match == null) {
            return ControlResult(
                false,
                "Sir, '$rawQuery' નામની એપ ઇન્સ્ટોલ થયેલી મળી નથી.",
                "Sir, I could not find an installed app matching '$rawQuery'."
            )
        }

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ControlResult(
                    true,
                    "Sir, ${match.label} ખોલી રહ્યો છું.",
                    "Sir, opening ${match.label}."
                )
            } else {
                ControlResult(
                    false,
                    "Sir, ${match.label} લોન્ચ કરી શકાય તેમ નથી.",
                    "Sir, ${match.label} cannot be launched directly."
                )
            }
        } catch (e: Exception) {
            ControlResult(
                false,
                "Sir, એપ ખોલવામાં એરર આવી.",
                "Sir, failed to open ${match.label}: ${e.localizedMessage}"
            )
        }
    }

    fun getCurrentDateTimeText(language: AssistantLanguage): String {
        val now = Date()
        return if (language == AssistantLanguage.GUJARATI) {
            val timeStr = SimpleDateFormat("hh:mm a", Locale("gu", "IN")).format(now)
            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("gu", "IN")).format(now)
            "Sir, અત્યારે સમય $timeStr થયો છે અને આજે તારીખ $dateStr છે."
        } else {
            val timeStr = SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(now)
            val dateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(now)
            "Sir, the current time is $timeStr on $dateStr."
        }
    }

    fun getAssistantVolumePercent(): Int {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return ((current.toFloat() / max) * 100f).toInt().coerceIn(0, 100)
    }

    fun setVolumePercent(percent: Int): ControlResult {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((percent.coerceIn(0, 100) / 100f) * max).toInt().coerceIn(0, max)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, AudioManager.FLAG_SHOW_UI)
        return ControlResult(
            true,
            "Sir, વોલ્યુમ $percent% પર સેટ કર્યું છે.",
            "Sir, volume set to $percent%."
        )
    }

    fun adjustVolume(direction: String): ControlResult {
        val dir = direction.lowercase(Locale.ROOT)
        return when {
            dir.contains("up") || dir.contains("increase") || dir.contains("વધાર") -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                ControlResult(true, "Sir, અવાજ વધારી દીધો છે.", "Sir, volume increased.")
            }
            dir.contains("down") || dir.contains("decrease") || dir.contains("ઘટાડ") || dir.contains("ઓછ") -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                ControlResult(true, "Sir, અવાજ ઓછો કરી દીધો છે.", "Sir, volume decreased.")
            }
            dir.contains("mute") || dir.contains("બંઘ") || dir.contains("મ્યુટ") -> {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_MUTE,
                    AudioManager.FLAG_SHOW_UI
                )
                ControlResult(true, "Sir, અવાજ મ્યુટ કર્યો છે.", "Sir, audio muted.")
            }
            else -> {
                val pct = dir.filter { it.isDigit() }.toIntOrNull()
                if (pct != null) setVolumePercent(pct)
                else ControlResult(true, "Sir, હાલનું વોલ્યુમ ${getAssistantVolumePercent()}% છે.", "Sir, current volume is ${getAssistantVolumePercent()}%.")
            }
        }
    }

    fun openBluetoothSettings(): ControlResult {
        return launchSettingsIntent(
            Intent(Settings.ACTION_BLUETOOTH_SETTINGS),
            "Sir, બ્લૂટૂથ સેટિંગ્સ ખોલી રહ્યો છું.",
            "Sir, opening Bluetooth settings."
        )
    }

    fun openWifiSettings(): ControlResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        return launchSettingsIntent(
            intent,
            "Sir, Wi-Fi અને નેટવર્ક સેટિંગ્સ ખોલી રહ્યો છું.",
            "Sir, opening Wi-Fi & network settings."
        )
    }

    fun openSystemSettings(): ControlResult {
        return launchSettingsIntent(
            Intent(Settings.ACTION_SETTINGS),
            "Sir, ફોન સેટિંગ્સ ખોલી રહ્યો છું.",
            "Sir, opening phone settings."
        )
    }

    fun openBatteryOptimizationSettings(): ControlResult {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        return launchSettingsIntent(
            intent,
            "Sir, બેટરી ઓપ્ટિમાઇઝેશન સેટિંગ્સ ખોલી રહ્યો છું.",
            "Sir, opening battery optimization settings."
        )
    }

    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        } else true
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else true
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    fun setSystemAlarm(hour: Int, minute: Int, message: String): ControlResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
                putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
                putExtra(AlarmClock.EXTRA_MESSAGE, message.ifBlank { "PARMAR AI Alarm" })
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ControlResult(
                true,
                "Sir, ${String.format(Locale.US, "%02d:%02d", hour, minute)} નો એલાર્મ સેટ કરી રહ્યો છું.",
                "Sir, setting alarm for ${String.format(Locale.US, "%02d:%02d", hour, minute)}."
            )
        } catch (e: Exception) {
            ControlResult(
                false,
                "Sir, ક્લોક એપ ઉપલબ્ધ નથી.",
                "Sir, unable to open system alarm clock: ${e.localizedMessage}"
            )
        }
    }

    fun scheduleLocalReminderNotification(reminderId: Long, title: String, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val intent = Intent(context, ParmarReminderReceiver::class.java).apply {
            putExtra(ParmarReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ParmarReminderReceiver.EXTRA_REMINDER_TITLE, title)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    fun cancelLocalReminderNotification(reminderId: Long) {
        val intent = Intent(context, ParmarReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            alarmManager.cancel(pending)
        }
    }

    fun isDeviceAdminActive(): Boolean = devicePolicyManager.isAdminActive(adminComponent)

    fun createDeviceAdminSetupIntent(): Intent {
        return Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Grant explicit Device Admin permission only if you want PARMAR AI to lock the screen when you say Lock Phone."
            )
        }
    }

    fun lockScreenNow(): ControlResult {
        return if (isDeviceAdminActive()) {
            try {
                devicePolicyManager.lockNow()
                ControlResult(true, "Sir, ફોન લોક કરી રહ્યો છું.", "Sir, locking your screen.")
            } catch (e: SecurityException) {
                ControlResult(
                    false,
                    "Sir, સ્ક્રીન લોક પરવાનગી સક્રિય નથી.",
                    "Sir, screen lock permission is not active."
                )
            }
        } else {
            ControlResult(
                false,
                "Sir, ફોન લોક કરવા માટે Permissions & Privacy માં જઈને Device Admin ચાલુ કરવું જરૂરી છે.",
                "Sir, please enable Device Admin in Permissions & Privacy to allow screen locking."
            )
        }
    }

    private fun launchSettingsIntent(intent: Intent, msgGu: String, msgEn: String): ControlResult {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ControlResult(true, msgGu, msgEn)
        } catch (e: Exception) {
            ControlResult(
                false,
                "Sir, આ સેટિંગ્સ પેજ ખોલી શકાયું નથી.",
                "Sir, unable to open settings: ${e.localizedMessage}"
            )
        }
    }
}
