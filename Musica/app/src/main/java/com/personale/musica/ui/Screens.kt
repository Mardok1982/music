package com.personale.musica.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage
import com.personale.musica.PlayerViewModel
import com.personale.musica.Song

@Composable
fun MusicaApp(vm: PlayerViewModel, permissionGranted: Boolean, onRequestPermission: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    BackHandler(enabled = expanded) { expanded = false }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Header(vm)
            when {
                !permissionGranted -> CenterMessage("Serve il permesso per leggere la tua musica", "Concedi", onRequestPermission)
                vm.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = TextDim) }
                vm.songs.isEmpty() -> CenterMessage("Nessun brano trovato sul telefono", "Riprova") { vm.loadSongs() }
                else -> SongList(vm, Modifier.weight(1f))
            }
            if (vm.hasMedia) MiniPlayer(vm) { expanded = true }
            Spacer(Modifier.navigationBarsPadding())
        }

        AnimatedVisibility(
            visible = expanded && vm.hasMedia,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) { FullPlayer(vm) { expanded = false } }
    }
}

@Composable
private fun Header(vm: PlayerViewModel) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Musica", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (vm.songs.isNotEmpty()) {
                FilledTonalButton(
                    onClick = vm::shuffleAll,
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface2, contentColor = TextMain)
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Casuale")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        TextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            placeholder = { Text("Cerca brani, artisti, album") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (vm.query.isNotEmpty()) IconButton({ vm.query = "" }) { Icon(Icons.Rounded.Close, null) }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SongList(vm: PlayerViewModel, modifier: Modifier) {
    val list = vm.filtered
    LazyColumn(modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 4.dp)) {
        item {
            Text(
                "${list.size} brani", color = TextDim, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(list, key = { it.id }) { song ->
            SongRow(song, isCurrent = vm.currentId == song.id.toString(), playing = vm.isPlaying) { vm.play(song) }
        }
    }
}

@Composable
private fun SongRow(song: Song, isCurrent: Boolean, playing: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Artwork(song.artUri, Modifier.size(52.dp), corner = 6.dp)
            if (isCurrent) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = .55f)),
                    Alignment.Center
                ) {
                    Icon(if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.Pause, null, tint = TextMain)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) Accent else TextMain, fontSize = 16.sp
            )
            Text(
                "${song.artist} • ${formatTime(song.durationMs)}",
                maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDim, fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun MiniPlayer(vm: PlayerViewModel, onExpand: () -> Unit) {
    val md = vm.metadata
    Column(Modifier.fillMaxWidth().background(Surface1).clickable(onClick = onExpand)) {
        LinearProgressIndicator(
            progress = { if (vm.duration > 0) vm.position.toFloat() / vm.duration else 0f },
            modifier = Modifier.fillMaxWidth().height(2.dp),
            color = TextMain, trackColor = Surface2,
        )
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(md?.artworkUri, Modifier.size(44.dp), corner = 4.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(md?.title?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                Text(md?.artist?.toString() ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextDim, fontSize = 13.sp)
            }
            IconButton(vm::previous) { Icon(Icons.Rounded.SkipPrevious, "Indietro") }
            IconButton(vm::togglePlay) {
                Icon(if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play/Pausa", Modifier.size(32.dp))
            }
            IconButton(vm::next) { Icon(Icons.Rounded.SkipNext, "Avanti") }
        }
    }
}

@Composable
private fun FullPlayer(vm: PlayerViewModel, onCollapse: () -> Unit) {
    val md = vm.metadata
    var dragging by remember { mutableStateOf<Float?>(null) }
    val progress = dragging ?: if (vm.duration > 0) vm.position.toFloat() / vm.duration else 0f

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Bg, Bg)))
            .clickable(enabled = false) {}
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onCollapse) { Icon(Icons.Rounded.KeyboardArrowDown, "Chiudi", Modifier.size(32.dp)) }
                Spacer(Modifier.weight(1f))
                Text(md?.albumTitle?.toString() ?: "", color = TextDim, fontSize = 13.sp, maxLines = 1)
                Spacer(Modifier.weight(1f))
                IconButton(vm::stop) { Icon(Icons.Rounded.Stop, "Stop") }
            }

            Spacer(Modifier.weight(1f))
            Artwork(md?.artworkUri, Modifier.fillMaxWidth().aspectRatio(1f), corner = 12.dp, iconSize = 96.dp)
            Spacer(Modifier.weight(1f))

            Column(Modifier.fillMaxWidth()) {
                Text(
                    md?.title?.toString() ?: "", fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(md?.artist?.toString() ?: "", fontSize = 16.sp, color = TextDim, maxLines = 1)
            }

            Spacer(Modifier.height(16.dp))
            Slider(
                value = progress,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { vm.seekTo((it * vm.duration).toLong()) }
                    dragging = null
                },
                colors = SliderDefaults.colors(thumbColor = TextMain, activeTrackColor = TextMain, inactiveTrackColor = Surface2),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatTime((progress * vm.duration).toLong()), color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(formatTime(vm.duration), color = TextDim, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                ToggleIcon(Icons.Rounded.Shuffle, vm.shuffle, vm::toggleShuffle)
                IconButton(vm::previous, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipPrevious, "Indietro", Modifier.size(40.dp)) }
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(TextMain).clickable(onClick = vm::togglePlay),
                    Alignment.Center
                ) {
                    Icon(
                        if (vm.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        "Play/Pausa", tint = Bg, modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(vm::next, Modifier.size(56.dp)) { Icon(Icons.Rounded.SkipNext, "Avanti", Modifier.size(40.dp)) }
                ToggleIcon(
                    if (vm.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    vm.repeatMode != Player.REPEAT_MODE_OFF, vm::cycleRepeat
                )
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ToggleIcon(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    IconButton(onClick) { Icon(icon, null, tint = if (active) TextMain else TextDim.copy(alpha = .5f)) }
}

@Composable
private fun Artwork(uri: Uri?, modifier: Modifier, corner: androidx.compose.ui.unit.Dp, iconSize: androidx.compose.ui.unit.Dp = 22.dp) {
    val shape = RoundedCornerShape(corner)
    val placeholder: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(Surface2), Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = TextDim, modifier = Modifier.size(iconSize))
        }
    }
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(shape),
        loading = { placeholder() },
        error = { placeholder() },
    )
}

@Composable
private fun CenterMessage(text: String, button: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = TextDim)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick) { Text(button) }
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(s / 60, s % 60)
}
