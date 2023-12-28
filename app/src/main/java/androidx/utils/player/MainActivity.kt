package androidx.utils.player

import android.app.Notification
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.utils.player.service.PlaybackService
import androidx.utils.player.tool.MediaSessionTool
import androidx.utils.player.tool.NotificationTool
import androidx.utils.player.ui.theme.PlayerTheme
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import coil.size.Size
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

@UnstableApi class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val handler = Handler(Looper.getMainLooper())
    private val mediaController: MediaController?
        get() = controllerFuture?.get()
    private val items = mutableListOf<MediaItem>()
    private val currentItemIndex = MutableLiveData(0)
    private val isMediaPlaying = MutableLiveData(false)
    private val currentProgress = MutableLiveData(0f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlaybackService.start(this)
        items.addAll(generateItems())
        setContent {
            PlayerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentItem = currentItemIndex.observeAsState(0)
                    val currentMediaMetadata = items[currentItem.value].mediaMetadata
                    val isPlaying = isMediaPlaying.observeAsState(false)
                    val currentProgress = currentProgress.observeAsState(0f)
                    Column {
                        Column(
                            Modifier
                                .wrapContentSize()
                                .padding(8.dp)
                        ) {
                            Row {
                                WebImage(modifier = Modifier.size(64.dp), url = currentMediaMetadata.artworkUri!!.toString())
                                Column(
                                    modifier = Modifier
                                        .wrapContentSize(Alignment.CenterStart)
                                        .padding(8.dp)
                                ) {
                                    Text(text = "Title:${currentMediaMetadata.title}")
                                    Text(text = "Artist:${currentMediaMetadata.artist}")
                                }
                            }

                        }
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = currentProgress.value,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Button(onClick = { if (isPlaying.value) mediaController?.pause() else mediaController?.play() }) {
                                Text(text = if (isPlaying.value) "Pause" else "Play")
                            }
                            Spacer(modifier = Modifier.padding(8.dp))
                            Button(onClick = { mediaController?.seekToNextMediaItem() }) {
                                Text(text = "Next")
                            }
                            Spacer(modifier = Modifier.padding(8.dp))
                            Button(onClick = { mediaController?.seekToPreviousMediaItem() }) {
                                Text(text = "Previous")
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture!!.addListener(
            {
                controllerFuture?.get()?.let { mediaController ->
                    mediaController.setMediaItems(items, 0, 0)
                    mediaController.prepare()
                    mediaController.addListener(object : Player.Listener {

                        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                            super.onMediaItemTransition(mediaItem, reason)
                            val id = mediaItem?.mediaId ?: return
                            val newIndex = items.map { it.mediaId }.indexOfFirst { it == id }
                            currentItemIndex.value = if (newIndex < 0) 0 else newIndex
                        }

                        override fun onEvents(player: Player, events: Player.Events) {
                            if (
                                events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) ||
                                events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                            ) {
                                if (player.playWhenReady) {
                                    addPositionChangeListener(player) {
                                        currentProgress.value = it.toFloat() / player.duration
                                    }
                                }
                            }
                        }

                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            isMediaPlaying.value = isPlaying
                        }

                    })
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    override fun onStop() {
        super.onStop()
        removePositionChangeListener()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }

    private fun generateItems(): List<MediaItem> {
        val itemList = mutableListOf<MediaItem>()
        for (i in 0..10) {
            itemList.add(
                MediaItem.Builder()
                    .setMediaId("media-${i}")
                    .setUri("https://storage.googleapis.com/exoplayer-test-media-0/play.mp3")
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setArtist("David BowieR")
                            .setTitle("Item_${i}")
                            .setArtworkUri(Uri.parse("https://upload.wikimedia.org/wikipedia/commons/4/41/Sunflower_from_Silesia2.jpg"))
                            .build()
                    )
                    .build()
            )
        }
        return itemList
    }

    private fun addPositionChangeListener(player: Player, listener: (Long) -> Unit) {
        checkPlaybackPosition(player, listener)
    }

    private fun removePositionChangeListener() {
        handler.removeCallbacksAndMessages(null)
    }

    private fun checkPlaybackPosition(
        player: Player,
        listener: (Long) -> Unit
    ) {
        handler.postDelayed(
            {
                listener.invoke(player.currentPosition)
                checkPlaybackPosition(player, listener)
            },
            20
        )
    }

}

@Composable
fun WebImage(
    modifier: Modifier,
    url: String
) {
    val context = LocalContext.current
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(Size.ORIGINAL)
            .scale(Scale.FIT)
            .crossfade(true)
            .build()
    )
    Box(modifier = modifier) {
        Image(
            painter,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "Image:$url",
            contentScale = ContentScale.Crop
        )
    }
}
