package androidx.utils.player.service

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.utils.player.tool.MediaSessionTool
import androidx.utils.player.tool.NotificationTool

@UnstableApi class PlaybackService : MediaSessionService() {

    companion object {
        private var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            when {
                isRunning -> { return }
                Build.VERSION.SDK_INT > Build.VERSION_CODES.O -> {
                    context.startForegroundService(intent)
                }
                else -> {
                    context.startService(intent)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        MediaSessionTool.init(this)
        setListener(object : Listener {
            override fun onForegroundServiceStartNotAllowedException() {
                NotificationTool.showNotification(this@PlaybackService)
            }
        })
        isRunning = true
        openNotification()
    }

    private fun openNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                startForeground(
                    NotificationTool.NOTIFICATION_ID,
                    NotificationTool.getNotificationBuilder(this).build()
                )
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    override fun onDestroy() {
        MediaSessionTool.release()
        super.onDestroy()
        isRunning = false
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return MediaSessionTool.getMediaSession()
    }

}