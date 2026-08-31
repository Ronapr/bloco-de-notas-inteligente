package com.securenotes.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.securenotes.app.MainActivity
import com.securenotes.app.R
import com.securenotes.app.domain.model.Reminder
import com.securenotes.app.domain.settings.NotificationSound
import com.securenotes.app.domain.settings.SettingsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AndroidReminderScheduler(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) : ReminderScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private val channelMutex = Mutex()

    @Volatile
    private var currentChannelId: String? = null

    override fun schedule(reminderId: Long, atMillis: Long) {
        val pendingIntent = pendingIntent(reminderId)
        val canScheduleExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pendingIntent)
        }
    }

    override fun cancel(reminderId: Long) {
        alarmManager.cancel(pendingIntent(reminderId))
    }

    override suspend fun postNotification(reminder: Reminder) {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            Log.w(TAG, "Notification permission not granted, skipping post")
            return
        }

        val channelId = ensureChannel()

        val contentIntent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_NOTE_ID, reminder.noteId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        val pendingContentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = reminder.noteTitle.ifBlank {
            context.getString(R.string.lembrete)
        }
        val text = context.getString(
            R.string.lembrete_notificacao,
            formatDateTime(reminder.reminderAt)
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingContentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val desiredSound = desiredSoundUri()
        if (desiredSound != null) {
            builder.setSound(desiredSound)
        }
        val vibrationEnabled = try {
            settingsRepository.notificationVibrationEnabled.first()
        } catch (_: Exception) {
            true
        }
        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0L, 400L, 200L, 400L))
        }

        val notification = builder.build()

        try {
            NotificationManagerCompat.from(context).notify(reminder.id.toInt(), notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException posting notification", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification", e)
        }
    }

    /**
     * Garante que o canal de notificacoes existe e esta correto.
     * Retorna o ID do canal util.
     *
     * Usa Mutex para evitar race conditions entre refreshChannel e postNotification.
     */
    private suspend fun ensureChannel(): String {
        channelMutex.withLock {
            val existingId = currentChannelId
            if (existingId != null) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val existing = manager.getNotificationChannel(existingId)
                if (existing != null) {
                    return existingId
                }
                Log.w(TAG, "Channel $existingId not found, will recreate")
            }

            val desiredSound = desiredSoundUri()
            val desiredVibration = try {
                settingsRepository.notificationVibrationEnabled.first()
            } catch (_: Exception) {
                true
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            val currentVersion = try {
                settingsRepository.notificationChannelVersion.first()
            } catch (_: Exception) {
                INITIAL_VERSION
            }
            val currentId = channelId(currentVersion)
            val existing = manager.getNotificationChannel(currentId)

            if (existing != null &&
                uriMatch(existing.sound, desiredSound) &&
                existing.shouldVibrate() == desiredVibration
            ) {
                currentChannelId = currentId
                return currentId
            }

            val newVersion = if (existing != null) currentVersion + 1 else currentVersion
            val newId = channelId(newVersion)

            if (existing != null) {
                manager.deleteNotificationChannel(currentId)
            }

            createChannel(manager, newId, desiredSound, desiredVibration)

            try {
                settingsRepository.setNotificationChannelVersion(newVersion)
            } catch (_: Exception) {
            }
            currentChannelId = newId
            return newId
        }
    }

    override suspend fun refreshChannel(force: Boolean) {
        channelMutex.withLock {
            val desiredSound = desiredSoundUri()
            val desiredVibration = settingsRepository.notificationVibrationEnabled.first()

            val manager = context.getSystemService(NotificationManager::class.java)

            val currentVersion = settingsRepository.notificationChannelVersion.first()
            val currentId = channelId(currentVersion)
            val existing = manager.getNotificationChannel(currentId)

            val firstRun = currentVersion == INITIAL_VERSION
            val needsRecreate = force || firstRun ||
                existing == null ||
                !uriMatch(existing.sound, desiredSound) ||
                existing.shouldVibrate() != desiredVibration

            if (!needsRecreate) {
                currentChannelId = currentId
                return
            }

            val newVersion = currentVersion + 1
            val newId = channelId(newVersion)

            if (firstRun) {
                migrateLegacyChannels(manager)
            }

            if (manager.getNotificationChannel(currentId) != null) {
                manager.deleteNotificationChannel(currentId)
            }

            createChannel(manager, newId, desiredSound, desiredVibration)

            settingsRepository.setNotificationChannelVersion(newVersion)
            currentChannelId = newId
            Log.i(TAG, "Channel refreshed: $currentId -> $newId (sound=$desiredSound, vibration=$desiredVibration)")
        }
    }

    suspend fun currentChannelId(): String =
        channelId(settingsRepository.notificationChannelVersion.first())

    private fun createChannel(
        manager: NotificationManager,
        id: String,
        soundUri: Uri?,
        vibrationEnabled: Boolean
    ) {
        val channel = NotificationChannel(
            id,
            context.getString(R.string.canal_lembretes),
            NotificationManager.IMPORTANCE_HIGH
        )

        channel.description = context.getString(R.string.canal_lembretes)

        if (soundUri != null) {
            channel.setSound(
                soundUri,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        } else {
            channel.setSound(null, null)
        }

        channel.enableVibration(vibrationEnabled)
        if (vibrationEnabled) {
            channel.setVibrationPattern(longArrayOf(0L, 400L, 200L, 400L))
        }

        manager.createNotificationChannel(channel)
    }

    private fun migrateLegacyChannels(manager: NotificationManager) {
        legacyChannelIds.forEach { id ->
            manager.getNotificationChannel(id)?.let {
                manager.deleteNotificationChannel(id)
            }
        }
    }

    private suspend fun desiredSoundUri(): Uri? {
        val sound = try {
            settingsRepository.notificationSound.first()
        } catch (_: Exception) {
            NotificationSound.SYSTEM
        }
        return when (sound) {
            NotificationSound.SYSTEM ->
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            NotificationSound.RINGTONE ->
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            NotificationSound.SILENT -> null
            NotificationSound.CUSTOM -> {
                val customUri = try {
                    settingsRepository.customSoundUri.first()
                } catch (_: Exception) {
                    ""
                }
                if (customUri.isNotBlank()) {
                    try {
                        val parsed = Uri.parse(customUri)
                        if (parsed.toString().isNotEmpty()) parsed
                        else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    } catch (_: Exception) {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    }
                } else {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                }
            }
        }
    }

    private fun channelId(version: Int): String = "$CHANNEL_ID_PREFIX$version"

    private fun pendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_REMINDER_ID, reminderId)
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun formatDateTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    private fun uriMatch(a: Uri?, b: Uri?): Boolean {
        if (a == null && b == null) return true
        if (a == null || b == null) return false
        return a.toString() == b.toString()
    }

    companion object {
        private const val TAG = "ReminderScheduler"
        const val CHANNEL_ID_PREFIX = "reminders_v"
        const val INITIAL_VERSION = 2
        private val legacyChannelIds = setOf("reminders", "reminders_silent")
        const val ACTION_REMINDER = "com.securenotes.app.action.REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
