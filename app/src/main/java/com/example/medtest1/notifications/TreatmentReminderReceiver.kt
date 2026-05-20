package com.example.medtest1.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.medtest1.R

class TreatmentReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val customSoundUri = intent?.getStringExtra(EXTRA_CUSTOM_SOUND_URI).orEmpty()
        val customTrimMs = intent?.getLongExtra(EXTRA_CUSTOM_SOUND_TRIM_MS, 10_000L) ?: 10_000L
        val hasCustomSound = customSoundUri.isNotBlank()
        val channelId = createChannel(
            context = context,
            customSoundUri = if (hasCustomSound) customSoundUri else null
        )
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Напоминание о лечении"
        val body = intent?.getStringExtra(EXTRA_BODY) ?: "Пора принять лекарство."
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (hasCustomSound) {
            val uri = Uri.parse(customSoundUri)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                notificationBuilder.setSound(uri)
            } else {
                // On Android O+ channels play full sounds, so we play only the first chunk manually.
                notificationBuilder.setSilent(true)
                playFirstAudioChunk(context, uri, customTrimMs.coerceAtMost(10_000L))
            }
        }

        val notification = notificationBuilder.build()
        NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun createChannel(context: Context, customSoundUri: String?): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return CHANNEL_ID
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (customSoundUri.isNullOrBlank()) CHANNEL_ID else "${CHANNEL_ID}_custom"
        val channelName = if (customSoundUri.isNullOrBlank()) {
            "Напоминания о лечении"
        } else {
            "Напоминания о лечении (свой звук)"
        }
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        if (!customSoundUri.isNullOrBlank()) {
            channel.setSound(
                Uri.parse(customSoundUri),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        manager.createNotificationChannel(channel)
        return channelId
    }

    private fun playFirstAudioChunk(context: Context, uri: Uri, maxDurationMs: Long) {
        runCatching {
            val player = MediaPlayer().apply {
                setDataSource(context, uri)
                prepare()
                start()
            }
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching {
                    if (player.isPlaying) player.stop()
                    player.release()
                }
            }, maxDurationMs.coerceAtLeast(1L))
        }
    }

    companion object {
        const val CHANNEL_ID = "treatment_reminders"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_CUSTOM_SOUND_URI = "extra_custom_sound_uri"
        const val EXTRA_CUSTOM_SOUND_TRIM_MS = "extra_custom_sound_trim_ms"
    }
}
