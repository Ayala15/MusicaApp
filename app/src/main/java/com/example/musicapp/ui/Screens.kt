package com.example.musicapp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.musicapp.data.Artist
import com.example.musicapp.data.AuthRepository
import com.example.musicapp.data.FavoritesRepository
import com.example.musicapp.data.Track
import com.example.musicapp.data.User
import com.example.musicapp.player.PlayerManager
import com.example.musicapp.player.PlayerState

private enum class Section(val label: String, val title: String) {
    DESCUBRIR("Descubrir", "Descubre"),
    BUSCAR("Buscar", "Busca"),
    GUARDADAS("Guardadas", "Tu lista")
}

@Composable
fun MusicApp(user: User, vm: MusicViewModel = viewModel()) {
    val s = vm.state
    val player by PlayerManager.state.collectAsStateWithLifecycle()
    val favorites by FavoritesRepository.items.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.DESCUBRIR) }
    var expanded by remember { mutableStateOf(false) }

    val currentId = player.track?.id
    val favIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val hasPlayer = player.track != null
    val showNow = expanded && hasPlayer

    BackHandler(enabled = section == Section.BUSCAR && s.results != null) { vm.clearResults() }
    BackHandler(enabled = showNow) { expanded = false }

    Box(Modifier.fillMaxSize().background(Palette.Cream)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (hasPlayer) 112.dp else 24.dp)
        ) {
            item { TopBar(user, section) { section = it } }
            when (section) {
                Section.DESCUBRIR -> discover(s, vm, currentId, favIds) {
                    vm.openArtist(it)
                    section = Section.BUSCAR
                }
                Section.BUSCAR -> search(s, vm, currentId, favIds)
                Section.GUARDADAS -> saved(favorites, currentId, favIds)
            }
        }

        if (hasPlayer) {
            PillPlayer(
                p = player,
                onOpen = { expanded = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        AnimatedVisibility(
            visible = showNow,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            NowPlaying(
                p = player,
                isFav = currentId != null && currentId in favIds,
                onClose = { expanded = false }
            )
        }
    }
}

/* Secciones */

private fun LazyListScope.discover(
    s: UiState,
    vm: MusicViewModel,
    currentId: Long?,
    favIds: Set<Long>,
    onArtist: (Artist) -> Unit
) {
    when {
        s.loading -> item { LoadingBlock() }
        s.error != null && s.chartTracks.isEmpty() -> item { ErrorBlock(s.error, vm::loadHome) }
        else -> {
            val artists = s.featured + s.artists.filter { a -> s.featured.none { it.id == a.id } }
            if (artists.isNotEmpty()) {
                item { SectionTitle("Artistas") }
                item { ArtistCards(artists, onArtist) }
            }
            item { RecsHeader(s.recsLoading, vm::loadRecommendations) }
            when {
                s.recs.isNotEmpty() -> item { RecsRow(s.recs, currentId) }
                s.recsLoading -> item {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                        color = Palette.Accent,
                        trackColor = Palette.Line
                    )
                }
                s.recsError -> item {
                    Text(
                        "No se pudieron cargar las recomendaciones. Toca ↻ para reintentar.",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        color = Palette.Muted,
                        fontSize = 13.sp
                    )
                }
            }
            item { SectionTitle("Top del mundo") }
            trackList(s.chartTracks, currentId, favIds)
        }
    }
}

private fun LazyListScope.search(
    s: UiState,
    vm: MusicViewModel,
    currentId: Long?,
    favIds: Set<Long>
) {
    item { SearchField(s.query, vm::onQueryChange) }
    val results = s.results
    if (results == null) {
        item { SectionTitle("Prueba con") }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vm.featuredNames) { name ->
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, Palette.Ink, RoundedCornerShape(50))
                            .clickable { vm.onQueryChange(name) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(name, color = Palette.Ink, fontSize = 14.sp)
                    }
                }
            }
        }
    } else {
        item { ResultsHeader(s.resultsTitle, vm::clearResults) }
        if (s.searching) {
            item { LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                color = Palette.Accent,
                trackColor = Palette.Line
            ) }
        } else if (results.isEmpty()) {
            item { Text("Sin resultados", Modifier.padding(24.dp), color = Palette.Muted) }
        }
        trackList(results, currentId, favIds)
    }
}

private fun LazyListScope.saved(favorites: List<Track>, currentId: Long?, favIds: Set<Long>) {
    if (favorites.isEmpty()) {
        item {
            Text(
                "Aún no guardas nada.\nToca el corazón en cualquier canción.",
                modifier = Modifier.padding(24.dp),
                color = Palette.Muted,
                fontSize = 15.sp
            )
        }
    } else {
        trackList(favorites, currentId, favIds)
    }
}

private fun LazyListScope.trackList(tracks: List<Track>, currentId: Long?, favIds: Set<Long>) {
    itemsIndexed(tracks) { i, t ->
        TrackRow(i + 1, t, tracks, t.id == currentId, t.id in favIds)
    }
}

/* Piezas */

@Composable
private fun TopBar(user: User, section: Section, onSection: (Section) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val dark = LocalDark.current
    Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Hola, ${user.name.trim().substringBefore(' ')}", color = Palette.Muted, fontSize = 14.sp)
                Text(
                    section.title,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 40.sp,
                    color = Palette.Ink
                )
            }
            IconButton(onClick = { ThemeSettings.set(!dark) }) {
                Icon(
                    if (dark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar entre modo claro y oscuro",
                    tint = Palette.Ink
                )
            }
            Box {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Palette.Ink)
                        .clickable { menu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        user.name.trim().firstOrNull()?.uppercase() ?: "?",
                        color = Palette.Cream,
                        fontWeight = FontWeight.Bold
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("@${user.username}", color = Palette.Muted, fontSize = 12.sp) },
                        onClick = {},
                        enabled = false
                    )
                    DropdownMenuItem(
                        text = { Text("Cerrar sesión") },
                        onClick = {
                            menu = false
                            AuthRepository.logout()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Section.values().forEach { sec ->
                val selected = sec == section
                Column(
                    Modifier.clickable { onSection(sec) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        sec.label,
                        color = if (selected) Palette.Ink else Palette.Muted,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (selected) Palette.Accent else Color.Transparent)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        fontFamily = SerifFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        color = Palette.Ink
    )
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        placeholder = { Text("Canciones, artistas…", color = Palette.Muted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Palette.Muted) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar", tint = Palette.Ink)
                }
            }
        },
        singleLine = true,
        colors = minimalFieldColors()
    )
}

@Composable
private fun ResultsHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.padding(start = 16.dp, end = 24.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Palette.Ink)
        }
        Text(
            title,
            fontFamily = SerifFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Palette.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Palette.Accent)
    }
}

@Composable
private fun ErrorBlock(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, textAlign = TextAlign.Center, color = Palette.Ink)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Ink, contentColor = Palette.Cream)
        ) { Text("Reintentar") }
    }
}

@Composable
private fun ArtistCards(artists: List<Artist>, onClick: (Artist) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(artists) { a ->
            Box(
                Modifier
                    .width(140.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Palette.Card)
                    .clickable { onClick(a) }
            ) {
                AsyncImage(
                    model = a.pictureBig ?: a.pictureMedium,
                    contentDescription = a.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(0.45f to Color.Transparent, 1f to Color(0xCC141414)))
                )
                Text(
                    a.name,
                    color = Color.White,
                    fontFamily = SerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun TrackRow(index: Int, track: Track, queue: List<Track>, isCurrent: Boolean, isFav: Boolean) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { PlayerManager.playQueue(queue, track) }
                .padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "%02d".format(index),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isCurrent) Palette.Accent else Palette.Muted,
                modifier = Modifier.width(34.dp)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (isCurrent) Palette.Accent else Palette.Ink
                )
                Text(
                    track.artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                    color = Palette.Muted
                )
            }
            IconButton(onClick = { FavoritesRepository.toggle(track) }) {
                Icon(
                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Guardar",
                    tint = if (isFav) Palette.Accent else Palette.Muted
                )
            }
        }
        Box(
            Modifier
                .padding(start = 58.dp, end = 24.dp)
                .fillMaxWidth()
                .height(1.dp)
                .background(Palette.Line)
        )
    }
}

@Composable
private fun RecsHeader(loading: Boolean, onRefresh: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Para ti",
                fontFamily = SerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Palette.Ink
            )
            Text("Recomendado por ReccoBeats", color = Palette.Muted, fontSize = 12.sp)
        }
        IconButton(onClick = onRefresh, enabled = !loading) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Nuevas recomendaciones",
                tint = if (loading) Palette.Line else Palette.Ink
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun RecsRow(tracks: List<Track>, currentId: Long?) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(tracks) { t ->
            Column(
                Modifier
                    .width(150.dp)
                    .clickable { PlayerManager.playQueue(tracks, t) }
            ) {
                AsyncImage(
                    model = t.album?.coverBig ?: t.album?.coverMedium,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Palette.Card)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    t.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (t.id == currentId) Palette.Accent else Palette.Ink
                )
                Text(
                    t.artist.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = Palette.Muted
                )
            }
        }
    }
}

/* Reproductor */

@Composable
private fun PillPlayer(p: PlayerState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val progress = if (p.durationMs > 0) (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) else 0f
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(32.dp),
        color = Palette.Ink,
        shadowElevation = 8.dp
    ) {
        Column(Modifier.clickable(onClick = onOpen)) {
            Row(
                Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = p.track?.album?.coverMedium,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Palette.Muted)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        p.track?.title ?: "",
                        color = Palette.Cream,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        p.track?.artist?.name ?: "",
                        color = Palette.Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Palette.Accent)
                        .clickable { PlayerManager.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (p.isBuffering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (p.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir o pausar",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
            Box(
                Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Palette.Cream.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .height(2.dp)
                        .background(Palette.Accent)
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun NowPlaying(p: PlayerState, isFav: Boolean, onClose: () -> Unit) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val track = p.track ?: return
    val progress = if (p.durationMs > 0) (p.positionMs.toFloat() / p.durationMs).coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { } },
        color = Palette.Cream
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Palette.Ink)
                }
                Spacer(Modifier.weight(1f))
                Text("Sonando ahora", color = Palette.Muted, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            AsyncImage(
                model = track.album?.coverBig ?: track.album?.coverMedium,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Palette.Card)
            )
            Spacer(Modifier.height(28.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        track.title,
                        fontFamily = SerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Palette.Ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(track.artist.name, color = Palette.Muted, fontSize = 16.sp)
                }
                IconButton(onClick = { FavoritesRepository.toggle(track) }) {
                    Icon(
                        if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Guardar",
                        tint = if (isFav) Palette.Accent else Palette.Muted
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Slider(
                value = if (dragging) dragValue else progress,
                onValueChange = {
                    dragging = true
                    dragValue = it
                },
                onValueChangeFinished = {
                    PlayerManager.seekTo((dragValue * p.durationMs).toLong())
                    dragging = false
                },
                colors = SliderDefaults.colors(
                    thumbColor = Palette.Ink,
                    activeTrackColor = Palette.Accent,
                    inactiveTrackColor = Palette.Line
                )
            )
            Row(Modifier.fillMaxWidth()) {
                Text(formatMs(p.positionMs), color = Palette.Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(formatMs(p.durationMs), color = Palette.Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = PlayerManager::previous, enabled = p.hasPrev) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = if (p.hasPrev) Palette.Ink else Palette.Line,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Palette.Accent)
                        .clickable { PlayerManager.togglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    if (p.isBuffering) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (p.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Reproducir o pausar",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                IconButton(onClick = PlayerManager::next, enabled = p.hasNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = if (p.hasNext) Palette.Ink else Palette.Line,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatMs(ms: Long): String = "%d:%02d".format(ms / 60000, (ms / 1000) % 60)
