package com.personale.musica.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import coil.compose.SubcomposeAsyncImage
import com.personale.musica.Playlist
import com.personale.musica.PlayerViewModel
import com.personale.musica.Song

// =====================================================================
//  Struttura principale
// =====================================================================

@Composable
fun MusicaApp(vm: PlayerViewModel, permissionGranted: Boolean, onRequestPermission: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }          // 0 = Brani, 1 = Playlist
    var openPlaylist by rememberSaveable { mutableStateOf<Long?>(null) }
    var addToPlaylist by remember { mutableStateOf<List<Long>?>(null) }

    BackHandler(enabled = expanded) { expanded = false }
    BackHandler(enabled = !expanded && openPlaylist != null) { openPlaylist = null }

    Box(Modifier.fillMaxSize().background(Bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                val playlist = openPlaylist?.let { vm.playlist(it) }
                when {
                    !permissionGranted ->
                        CenterMessage("Serve il permesso per leggere la tua musica", "Concedi", onRequestPermission)

                    playlist != null ->
                        PlaylistDetail(vm, playlist, onBack = { openPlaylist = null })

                    else -> Column(Modifier.fillMaxSize()) {
                        Header(vm, tab, onTab = { tab = it })
                        when {
                            vm.loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                                CircularProgressIndicator(color = TextDim)
                            }
                            tab == 0 && vm.songs.isEmpty() ->
                                CenterMessage("Nessun brano trovato sul telefono", "Riprova") { vm.loadSongs() }
                            tab == 0 ->
                                SongList(vm, onAddToPlaylist = { addToPlaylist = listOf(it.id) })
                            else ->
                                PlaylistsTab(vm, onOpen = { openPlaylist = it })
                        }
                    }
                }
            }
            if (vm.hasMedia) MiniPlayer(vm) { expanded = true }
            Spacer(Modifier.navigationBarsPadding())
        }

        AnimatedVisibility(
            visible = expanded && vm.hasMedia,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
        ) { FullPlayer(vm) { expanded = false } }

        addToPlaylist?.let { ids ->
            AddToPlaylistDialog(vm, ids, onDismiss = { addToPlaylist = null })
        }
    }
}

@Composable
private fun Header(vm: PlayerViewModel, tab: Int, onTab: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Musica", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (vm.songs.isNotEmpty()) {
                FilledTonalButton(
                    onClick = { vm.playQueue(vm.songs, 0, shuffled = true) },
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface2, contentColor = TextMain)
                ) {
                    Icon(Icons.Rounded.Shuffle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Casuale")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Brani", tab == 0) { onTab(0) }
            Chip("Playlist", tab == 1) { onTab(1) }
        }
        if (tab == 0) {
            Spacer(Modifier.height(10.dp))
            SearchField(vm.query, { vm.query = it }, "Cerca brani, artisti, album")
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) TextMain else Surface2)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (selected) Bg else TextMain, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = {
            if (value.isNotEmpty()) IconButton({ onChange("") }) { Icon(Icons.Rounded.Close, null) }
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

// =====================================================================
//  Scheda Brani
// =====================================================================

@Composable
private fun SongList(vm: PlayerViewModel, onAddToPlaylist: (Song) -> Unit) {
    val list = vm.filtered
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        item {
            Text(
                "${list.size} brani", color = TextDim, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(list, key = { it.id }) { song ->
            SongRow(
                song,
                isCurrent = vm.currentId == song.id.toString(),
                playing = vm.isPlaying,
                onClick = { vm.play(song) },
            ) {
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "Opzioni", tint = TextDim) }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Aggiungi a playlist") },
                            leadingIcon = { Icon(Icons.Rounded.Add, null) },
                            onClick = { menu = false; onAddToPlaylist(song) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SongRow(
    song: Song,
    isCurrent: Boolean,
    playing: Boolean,
    onClick: (() -> Unit)?,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
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
        trailing()
    }
}

// =====================================================================
//  Scheda Playlist
// =====================================================================

@Composable
private fun PlaylistsTab(vm: PlayerViewModel, onOpen: (Long) -> Unit) {
    var creating by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
        item {
            Row(
                Modifier.fillMaxWidth().clickable { creating = true }.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(Surface2), Alignment.Center) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("Nuova playlist", fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (vm.playlists.isEmpty()) {
            item {
                Text(
                    "Non hai ancora playlist.\nCreane una, oppure dalla scheda Brani usa ⋮ → Aggiungi a playlist.",
                    color = TextDim, fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        items(vm.playlists, key = { it.id }) { p ->
            val songs = vm.songsOf(p)
            Row(
                Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Artwork(songs.firstOrNull()?.artUri, Modifier.size(56.dp), corner = 6.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${songs.size} brani", color = TextDim, fontSize = 13.sp)
                }
            }
        }
    }

    if (creating) {
        NameDialog(
            title = "Nuova playlist", initial = "", confirm = "Crea",
            onConfirm = { name -> creating = false; onOpen(vm.createPlaylist(name)) },
            onDismiss = { creating = false }
        )
    }
}

@Composable
private fun PlaylistDetail(vm: PlayerViewModel, p: Playlist, onBack: () -> Unit) {
    val songs = vm.songsOf(p)
    var editing by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // barra in alto
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Indietro") }
            Spacer(Modifier.weight(1f))
            if (songs.size > 1 || editing) {
                TextButton({ editing = !editing }) {
                    Icon(if (editing) Icons.Rounded.Check else Icons.Rounded.SwapVert, null, Modifier.size(18.dp), tint = TextMain)
                    Spacer(Modifier.width(6.dp))
                    Text(if (editing) "Fatto" else "Riordina", color = TextMain)
                }
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "Opzioni") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rinomina") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; renaming = true }
                    )
                    DropdownMenuItem(
                        text = { Text("Elimina playlist") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = { menu = false; deleting = true }
                    )
                }
            }
        }

        // intestazione
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(songs.firstOrNull()?.artUri, Modifier.size(96.dp), corner = 8.dp, iconSize = 40.dp)
            Spacer(Modifier.width(16.dp))
            Column {
                Text(p.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    "${songs.size} brani • ${formatTime(songs.sumOf { it.durationMs })}",
                    color = TextDim, fontSize = 13.sp
                )
            }
        }

        // pulsanti
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { vm.playQueue(songs, 0) },
                enabled = songs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = TextMain, contentColor = Bg),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Riproduci")
            }
            FilledTonalButton(
                onClick = { vm.playQueue(songs, 0, shuffled = true) },
                enabled = songs.isNotEmpty(),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface2, contentColor = TextMain),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(Icons.Rounded.Shuffle, "Casuale", Modifier.size(20.dp))
            }
            FilledTonalButton(
                onClick = { picking = true },
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = Surface2, contentColor = TextMain),
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("Aggiungi")
            }
        }

        if (editing) {
            Text(
                "Usa le frecce per spostare i brani, la X per toglierli dalla playlist.",
                color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (songs.isEmpty()) {
            Text(
                "Playlist vuota. Premi \"Aggiungi\" per scegliere i brani.",
                color = TextDim, modifier = Modifier.padding(16.dp)
            )
        }

        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
            itemsIndexed(songs, key = { _, s -> s.id }) { i, song ->
                SongRow(
                    song,
                    isCurrent = vm.currentId == song.id.toString(),
                    playing = vm.isPlaying,
                    onClick = if (editing) null else ({ vm.playQueue(songs, i) }),
                ) {
                    if (editing) {
                        IconButton({ vm.moveInPlaylist(p.id, i, i - 1) }, enabled = i > 0) {
                            Icon(Icons.Rounded.KeyboardArrowUp, "Su",
                                tint = if (i > 0) TextMain else TextDim.copy(alpha = .3f))
                        }
                        IconButton({ vm.moveInPlaylist(p.id, i, i + 1) }, enabled = i < songs.lastIndex) {
                            Icon(Icons.Rounded.KeyboardArrowDown, "Giù",
                                tint = if (i < songs.lastIndex) TextMain else TextDim.copy(alpha = .3f))
                        }
                        IconButton({ vm.removeFromPlaylist(p.id, i) }) {
                            Icon(Icons.Rounded.Close, "Togli", tint = TextDim)
                        }
                    } else {
                        Text(
                            "${i + 1}", color = TextDim, fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    }

    if (renaming) {
        NameDialog(
            title = "Rinomina playlist", initial = p.name, confirm = "Salva",
            onConfirm = { vm.renamePlaylist(p.id, it); renaming = false },
            onDismiss = { renaming = false }
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("Eliminare \"${p.name}\"?") },
            text = { Text("I brani restano sul telefono, viene cancellata solo la playlist.") },
            confirmButton = {
                TextButton({ deleting = false; onBack(); vm.deletePlaylist(p.id) }) { Text("Elimina", color = Accent) }
            },
            dismissButton = { TextButton({ deleting = false }) { Text("Annulla", color = TextMain) } },
            containerColor = Surface2,
        )
    }
    if (picking) {
        SongPickerDialog(
            vm = vm,
            exclude = p.songIds.toSet(),
            onConfirm = { ids -> vm.addToPlaylist(p.id, ids); picking = false },
            onDismiss = { picking = false }
        )
    }
}

// =====================================================================
//  Finestre di dialogo
// =====================================================================

@Composable
private fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text, onValueChange = { text = it },
                placeholder = { Text("Nome") }, singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TextMain, cursorColor = TextMain
                )
            )
        },
        confirmButton = {
            TextButton({ onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirm, color = TextMain) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Annulla", color = TextDim) } },
        containerColor = Surface2,
    )
}

@Composable
private fun AddToPlaylistDialog(vm: PlayerViewModel, songIds: List<Long>, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var creating by remember { mutableStateOf(false) }

    if (creating) {
        NameDialog(
            title = "Nuova playlist", initial = "", confirm = "Crea",
            onConfirm = { name ->
                vm.createPlaylist(name, songIds)
                Toast.makeText(context, "Aggiunto a \"${name.trim()}\"", Toast.LENGTH_SHORT).show()
                onDismiss()
            },
            onDismiss = onDismiss
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi a playlist") },
        text = {
            Column(Modifier.heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .clickable { creating = true }.padding(vertical = 12.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(12.dp))
                    Text("Nuova playlist", fontWeight = FontWeight.Medium)
                }
                vm.playlists.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .clickable {
                                vm.addToPlaylist(p.id, songIds)
                                Toast.makeText(context, "Aggiunto a \"${p.name}\"", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.MusicNote, null, tint = TextDim)
                        Spacer(Modifier.width(12.dp))
                        Text(p.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (songIds.all { it in p.songIds }) Icon(Icons.Rounded.Check, "Già presente", tint = TextDim)
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Chiudi", color = TextDim) } },
        containerColor = Surface2,
    )
}

/**
 * Selettore a schermo intero: tocchi i brani nell'ordine in cui li vuoi,
 * il numero accanto indica la posizione con cui verranno aggiunti.
 */
@Composable
private fun SongPickerDialog(
    vm: PlayerViewModel,
    exclude: Set<Long>,
    onConfirm: (List<Long>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val selected = remember { mutableStateListOf<Long>() }
    val available = remember(vm.songs, exclude) { vm.songs.filterNot { it.id in exclude } }
    val shown = if (query.isBlank()) available else available.filter {
        it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Bg, contentColor = TextMain) {
            Column {
                Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onDismiss) { Icon(Icons.Rounded.Close, "Chiudi") }
                    Text("Aggiungi brani", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { onConfirm(selected.toList()) },
                        enabled = selected.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TextMain, contentColor = Bg),
                        modifier = Modifier.padding(end = 8.dp)
                    ) { Text(if (selected.isEmpty()) "Aggiungi" else "Aggiungi (${selected.size})") }
                }
                Box(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    SearchField(query, { query = it }, "Cerca")
                }
                Text(
                    "Tocca i brani nell'ordine in cui li vuoi.",
                    color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.id }) { song ->
                        val pos = selected.indexOf(song.id)
                        SongRow(
                            song, isCurrent = false, playing = false,
                            onClick = { if (pos >= 0) selected.remove(song.id) else selected.add(song.id) },
                        ) {
                            Box(
                                Modifier.padding(horizontal = 12.dp).size(28.dp).clip(CircleShape)
                                    .then(
                                        if (pos >= 0) Modifier.background(TextMain)
                                        else Modifier.border(2.dp, TextDim, CircleShape)
                                    ),
                                Alignment.Center
                            ) {
                                if (pos >= 0) Text("${pos + 1}", color = Bg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
//  Lettore (mini e a schermo intero)
// =====================================================================

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

// =====================================================================
//  Componenti comuni
// =====================================================================

@Composable
private fun ToggleIcon(icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    IconButton(onClick) { Icon(icon, null, tint = if (active) TextMain else TextDim.copy(alpha = .5f)) }
}

@Composable
private fun Artwork(uri: Uri?, modifier: Modifier, corner: Dp, iconSize: Dp = 22.dp) {
    val placeholder: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(Surface2), Alignment.Center) {
            Icon(Icons.Rounded.MusicNote, null, tint = TextDim, modifier = Modifier.size(iconSize))
        }
    }
    if (uri == null) {
        Box(modifier.clip(RoundedCornerShape(corner))) { placeholder() }
        return
    }
    SubcomposeAsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner)),
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
        OutlinedButton(onClick) { Text(button, color = TextMain) }
    }
}

private fun formatTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    val h = s / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, (s % 3600) / 60, s % 60)
    else "%d:%02d".format(s / 60, s % 60)
}
