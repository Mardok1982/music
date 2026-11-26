package com.personale.musica

import android.app.Application
import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var query by mutableStateOf("")

    var metadata by mutableStateOf<MediaMetadata?>(null); private set
    var hasMedia by mutableStateOf(false); private set
    var isPlaying by mutableStateOf(false); private set
    var position by mutableLongStateOf(0L); private set
    var duration by mutableLongStateOf(0L); private set
    var shuffle by mutableStateOf(false); private set
    var repeatMode by mutableIntStateOf(Player.REPEAT_MODE_OFF); private set
    var currentId by mutableStateOf<String?>(null); private set

    val filtered: List<Song>
        get() = if (query.isBlank()) songs else songs.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }

    private var controller: MediaController? = null
    private val future = MediaController.Builder(
        app, SessionToken(app, ComponentName(app, PlaybackService::class.java))
    ).buildAsync()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync(player)
    }

    init {
        future.addListener({
            controller = future.get().also {
                it.addListener(listener)
                sync(it)
            }
        }, ContextCompat.getMainExecutor(app))

        viewModelScope.launch {
            while (isActive) {
                controller?.let {
                    position = it.currentPosition
                    duration = it.duration.coerceAtLeast(0)
                }
                delay(500)
            }
        }
    }

    private fun sync(p: Player) {
        hasMedia = p.mediaItemCount > 0 && p.playbackState != Player.STATE_IDLE
        metadata = p.currentMediaItem?.mediaMetadata
        currentId = p.currentMediaItem?.mediaId
        isPlaying = p.isPlaying
        shuffle = p.shuffleModeEnabled
        repeatMode = p.repeatMode
        duration = p.duration.coerceAtLeast(0)
    }

    fun loadSongs() {
        viewModelScope.launch {
            loading = true
            songs = withContext(Dispatchers.IO) { MusicRepository.loadSongs(getApplication()) }
            loading = false
        }
    }

    /** Avvia la lista (filtrata) partendo dal brano scelto. */
    fun play(song: Song) {
        val list = filtered
        val c = controller ?: return
        c.setMediaItems(list.map { it.toMediaItem() }, list.indexOf(song).coerceAtLeast(0), 0L)
        c.prepare()
        c.play()
    }

    fun shuffleAll() {
        val c = controller ?: return
        if (songs.isEmpty()) return
        c.shuffleModeEnabled = true
        c.setMediaItems(songs.map { it.toMediaItem() }, songs.indices.random(), 0L)
        c.prepare()
        c.play()
    }

    fun togglePlay() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun next() { controller?.seekToNext() }
    fun previous() { controller?.seekToPrevious() }
    fun seekTo(ms: Long) { controller?.seekTo(ms) }
    fun stop() {
        val c = controller ?: return
        c.pause(); c.seekTo(0); c.stop()
    }
    fun toggleShuffle() {
        val c = controller ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }
    fun cycleRepeat() {
        val c = controller ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
    }
}
