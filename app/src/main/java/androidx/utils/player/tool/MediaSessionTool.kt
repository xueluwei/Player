package androidx.utils.player.tool

import android.content.Context
import android.os.Bundle
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.utils.player.R
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture

@UnstableApi
object MediaSessionTool {
    const val SAVE_TO_FAVORITES = "save_to_favorites"

    private var mediaSession: MediaSession? = null
    private val cacheList = mutableListOf<MediaItem>()

    fun getMediaSession(): MediaSession? = mediaSession

    fun init(context: Context) {
        val player = ExoPlayer.Builder(context).build()
        val forwardingPlayer = object : ForwardingPlayer(player) { }
        mediaSession = MediaSession.Builder(context, forwardingPlayer)
            .setCallback(object : MediaSession.Callback {
                override fun onAddMediaItems(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    mediaItems: MutableList<MediaItem>
                ): ListenableFuture<MutableList<MediaItem>> {
                    cacheList.addAll(mediaItems)
                    return super.onAddMediaItems(mediaSession, controller, mediaItems)
                }

                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands =
                        MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                            .add(SessionCommand(SAVE_TO_FAVORITES, Bundle.EMPTY))
                            .build()
                    val favoriteButton = CommandButton.Builder()
                        .setDisplayName("Save to favorites")
                        .setIconResId(R.drawable.baseline_favorite_24)
                        .setSessionCommand(SessionCommand(SAVE_TO_FAVORITES, Bundle()))
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setCustomLayout(ImmutableList.of(favoriteButton))
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ): ListenableFuture<SessionResult> {
                    if (customCommand.customAction == SAVE_TO_FAVORITES) {
                        return Futures.immediateFuture(
                            SessionResult(SessionResult.RESULT_SUCCESS)
                        )
                    }
                    return super.onCustomCommand(session, controller, customCommand, args)
                }

                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val settable = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                    settable.set(MediaSession.MediaItemsWithStartPosition(cacheList, 0, 0))
                    return settable
                }
            })
            .build()
    }

    fun release() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
    }

}