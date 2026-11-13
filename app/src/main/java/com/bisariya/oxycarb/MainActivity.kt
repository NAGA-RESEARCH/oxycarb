package com.bisariya.oxycarb

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

data class AppThemePalette(
    val name: String,
    val background: Color,
    val card: Color,
    val cardHover: Color,
    val accent: Color,
    val secondary: Color,
    val textPrimary: Color,
    val isLight: Boolean = false
)

val DeepDarkTheme = AppThemePalette(
    name = "Deep Dark",
    background = Color(0xFF0F050B),
    card = Color(0xFF200C16),
    cardHover = Color(0xFF331020),
    accent = Color(0xFFE83D7A),
    secondary = Color(0xFFC18A9F),
    textPrimary = Color(0xFFFFF4F8)
)

val CleanLightTheme = AppThemePalette(
    name = "Clean Light",
    background = Color(0xFFF4F5F7),
    card = Color(0xFFFFFFFF),
    cardHover = Color(0xFFE5E7EB),
    accent = Color(0xFFE83D7A),
    secondary = Color(0xFF6B7280),
    textPrimary = Color(0xFF111827),
    isLight = true
)

val MidnightBlueTheme = AppThemePalette(
    name = "Midnight Blue",
    background = Color(0xFF0B1325),
    card = Color(0xFF131D35),
    cardHover = Color(0xFF1C2B4D),
    accent = Color(0xFF3B82F6),
    secondary = Color(0xFF8D9EC6),
    textPrimary = Color(0xFFF0F4FF)
)

val CyberpunkGoldTheme = AppThemePalette(
    name = "Cyberpunk Gold",
    background = Color(0xFF120E00),
    card = Color(0xFF241C04),
    cardHover = Color(0xFF382C07),
    accent = Color(0xFFFFB800),
    secondary = Color(0xFFC7B27C),
    textPrimary = Color(0xFFFFFDEE)
)

val AppThemeList = listOf(DeepDarkTheme, CleanLightTheme, MidnightBlueTheme, CyberpunkGoldTheme)

val LocalAppTheme = compositionLocalOf { DeepDarkTheme }

@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(16.dp), alpha: Float = 0.5f): Modifier {
    val theme = LocalAppTheme.current
    val borderColor = if (theme.isLight) theme.secondary.copy(alpha = 0.15f) else theme.textPrimary.copy(alpha = 0.1f)
    return this.clip(shape)
        .background(theme.card.copy(alpha = alpha))
        .border(1.dp, borderColor, shape)
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Good Morning ☀️"
        in 12..16 -> "Good Afternoon 🌤️"
        in 17..21 -> "Good Evening 🌙"
        else -> "Late Night Vibes 🌌"
    }
}

@Composable
fun AppLogo(modifier: Modifier = Modifier.size(40.dp)) {
    val theme = LocalAppTheme.current
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = theme.accent,
            radius = radius * 0.88f,
            center = center,
            style = Stroke(width = radius * 0.18f)
        )

        drawCircle(
            color = if (theme.isLight) theme.secondary.copy(alpha = 0.7f) else theme.textPrimary.copy(alpha = 0.5f),
            radius = radius * 0.62f,
            center = center,
            style = Stroke(width = radius * 0.08f)
        )

        drawCircle(
            color = if (theme.isLight) theme.secondary.copy(alpha = 0.5f) else theme.textPrimary.copy(alpha = 0.35f),
            radius = radius * 0.45f,
            center = center,
            style = Stroke(width = radius * 0.06f)
        )

        drawCircle(
            color = theme.accent,
            radius = radius * 0.28f,
            center = center
        )

        drawCircle(
            color = if (theme.isLight) Color.White else theme.background,
            radius = radius * 0.12f,
            center = center
        )
    }
}

@Composable
fun FullScreenBackgroundVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val theme = LocalAppTheme.current
    val transition = rememberInfiniteTransition(label = "BackgroundVisualizerAnim")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barCount = 44
        val baseHeights = remember {
            List(barCount) { i ->
                val wave = sin(i * 0.35 + (i % 5)).toFloat()
                (0.25f + kotlin.math.abs(wave) * 0.75f).coerceIn(0.25f, 1.0f)
            }
        }

        baseHeights.forEachIndexed { index, baseHeight ->
            val animHeight by transition.animateFloat(
                initialValue = 0.15f,
                targetValue = baseHeight,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 420 + (index * 53) % 450,
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BarAnim$index"
            )

            val smoothHeight by animateFloatAsState(
                targetValue = if (isPlaying) animHeight else 0.12f,
                animationSpec = tween(durationMillis = 350, easing = LinearEasing),
                label = "SmoothH$index"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((260 * smoothHeight).dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                theme.accent.copy(alpha = 0.6f),
                                theme.secondary.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun MusicalBackgroundPattern(content: @Composable () -> Unit) {
    val theme = LocalAppTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "BgPatternAnimation")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.accent.copy(alpha = if (theme.isLight) 0.08f else 0.15f), Color.Transparent),
                    center = Offset(width * 0.85f, height * 0.12f),
                    radius = width * 0.75f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(theme.cardHover.copy(alpha = if (theme.isLight) 0.2f else 0.35f), Color.Transparent),
                    center = Offset(width * 0.15f, height * 0.85f),
                    radius = width * 0.85f
                )
            )

            val vinylCenter = Offset(width * 0.5f, height * 0.32f)
            listOf(140f, 240f, 340f, 440f, 540f).forEach { r ->
                drawCircle(
                    color = theme.accent.copy(alpha = 0.04f),
                    radius = r,
                    center = vinylCenter,
                    style = Stroke(width = 1.5f)
                )
            }

            val path1 = Path()
            val path2 = Path()

            val waveY1 = height * 0.22f
            val waveY2 = height * 0.68f

            path1.moveTo(0f, waveY1)
            path2.moveTo(0f, waveY2)

            var x = 0f
            while (x <= width) {
                val rad1 = (x * 0.015f + Math.toRadians(waveOffset.toDouble())).toFloat()
                val rad2 = (x * 0.012f - Math.toRadians(waveOffset.toDouble())).toFloat()

                val y1 = waveY1 + sin(rad1) * 35f
                val y2 = waveY2 + cos(rad2) * 45f

                path1.lineTo(x, y1)
                path2.lineTo(x, y2)
                x += 20f
            }

            drawPath(
                path = path1,
                color = theme.accent.copy(alpha = 0.06f),
                style = Stroke(width = 2f)
            )
            drawPath(
                path = path2,
                color = theme.secondary.copy(alpha = 0.05f),
                style = Stroke(width = 1.8f)
            )
        }

        content()
    }
}

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    var mediaController: MediaController? by mutableStateOf(null)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent { MusicHome(mediaController, viewModel) }
    }

    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
        }, MoreExecutors.directExecutor())
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}

object FavManager {
    fun getFavs(context: Context): List<TrackInfo> {
        val prefs = context.getSharedPreferences("oxycarb_favs", Context.MODE_PRIVATE)
        return prefs.getStringSet("saved_tracks", emptySet())?.mapNotNull {
            val p = it.split("::")
            if (p.size == 4) TrackInfo(p[0], p[1], p[2], p[3].toLongOrNull() ?: 0L) else null
        } ?: emptyList()
    }

    fun toggleFav(context: Context, track: TrackInfo) {
        val prefs = context.getSharedPreferences("oxycarb_favs", Context.MODE_PRIVATE)
        val set = prefs.getStringSet("saved_tracks", emptySet())?.toMutableSet() ?: mutableSetOf()
        val item = "${track.id}::${track.title}::${track.artist}::${track.durationMs}"
        if (set.any { it.startsWith("${track.id}::") }) {
            set.removeAll { it.startsWith("${track.id}::") }
        } else {
            set.add(item)
        }
        prefs.edit().putStringSet("saved_tracks", set).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicHome(player: MediaController?, viewModel: MainViewModel) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("oxycarb_settings", Context.MODE_PRIVATE)
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp >= 600

    var savedThemeName by rememberSaveable { mutableStateOf(prefs.getString("app_theme", "Deep Dark") ?: "Deep Dark") }
    val currentTheme = AppThemeList.firstOrNull { it.name == savedThemeName } ?: DeepDarkTheme

    var currentTab by rememberSaveable { mutableIntStateOf(0) }
    var searchCategory by rememberSaveable { mutableIntStateOf(0) }
    var historyCategory by rememberSaveable { mutableIntStateOf(0) }
    var selectedVibe by rememberSaveable { mutableStateOf("🔥 Trending") }

    val recentSongs by viewModel.recentSongs.collectAsState()
    val recentPodcasts by viewModel.recentPodcasts.collectAsState()
    val homeSuggestedSongs by viewModel.homeSuggestedSongs.collectAsState()
    val homeLoading by viewModel.homeLoading.collectAsState()
    val homeLoadingMore by viewModel.homeLoadingMore.collectAsState()

    val searchResultsSongs by viewModel.searchResultsSongs.collectAsState()
    val searchResultsPodcasts by viewModel.searchResultsPodcasts.collectAsState()
    val query by viewModel.currentQuery.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()

    var favoriteTracks by remember { mutableStateOf(FavManager.getFavs(context)) }
    var current by remember { mutableStateOf<TrackInfo?>(null) }
    var playing by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var streamLoading by remember { mutableStateOf(false) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeatModeState by rememberSaveable { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var dataSaver by remember { mutableStateOf(prefs.getBoolean("data_saver", false)) }
    var autoPlayEnabled by remember { mutableStateOf(prefs.getBoolean("auto_play", true)) }
    var selectedEqPreset by remember { mutableStateOf(prefs.getString("eq_preset", "Normal") ?: "Normal") }

    var sleepTimerMinutes by remember { mutableIntStateOf(0) }
    var sleepTimerJob by remember { mutableStateOf<Job?>(null) }
    var showCustomTimerDialog by remember { mutableStateOf(false) }
    var customSliderMinutes by remember { mutableFloatStateOf(60f) }

    val relatedQueue = remember { mutableStateListOf<TrackInfo>() }
    val playedHistoryStack = remember { mutableStateListOf<TrackInfo>() }

    val gridState = rememberLazyGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = gridState.layoutInfo.totalItemsCount
            lastVisible >= totalItems - 4 && totalItems > 0
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && currentTab == 0 && !homeLoading && !homeLoadingMore) {
            viewModel.loadMoreHomeFeed()
        }
    }

    fun togglePlayPause() {
        if (player != null) {
            if (player.isPlaying) {
                player.pause()
                playing = false
            } else {
                player.play()
                playing = true
            }
        }
    }

    fun startTrack(track: TrackInfo, isUserClick: Boolean = true, pushToStack: Boolean = true) {
        playbackJob?.cancel()
        playbackJob = coroutineScope.launch(Dispatchers.Main) {
            if (pushToStack && current != null && current?.id != track.id) {
                playedHistoryStack.add(current!!)
            }

            current = track
            playing = false
            currentPositionMs = 0L
            playbackError = null
            streamLoading = true

            viewModel.addToHistory(track)

            val streamUrl = YoutubeExtractor.getAudioUrl(track.id, dataSaver)
            if (!isActive) return@launch

            if (streamUrl == null) {
                streamLoading = false
                playbackError = "Could not extract audio stream for this track."
                return@launch
            }

            val metadata = MediaMetadata.Builder()
                .setTitle(track.title)
                .setArtist(track.artist)
                .build()

            val mediaItem = MediaItem.Builder()
                .setMediaId(track.id)
                .setUri(streamUrl)
                .setMediaMetadata(metadata)
                .build()

            player?.setMediaItem(mediaItem)
            player?.prepare()
            player?.playWhenReady = true
            streamLoading = false
            playing = true

            if (isUserClick) {
                coroutineScope.launch(Dispatchers.IO) {
                    val related = YoutubeExtractor.getRelatedTracks(track.id)
                    relatedQueue.clear()
                    relatedQueue.addAll(related)
                }
            }
        }
    }

    fun playNextRelatedTrack() {
        if (autoPlayEnabled && relatedQueue.isNotEmpty()) {
            val nextTrack = if (shuffle) relatedQueue.random() else relatedQueue.removeAt(0)
            startTrack(nextTrack, isUserClick = false, pushToStack = true)
        }
    }

    fun playPreviousTrack() {
        if (playedHistoryStack.isNotEmpty()) {
            val prevTrack = playedHistoryStack.removeAt(playedHistoryStack.size - 1)
            startTrack(prevTrack, isUserClick = false, pushToStack = false)
        } else {
            player?.seekTo(0L)
            currentPositionMs = 0L
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes = minutes
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = coroutineScope.launch {
                delay(minutes * 60 * 1000L)
                player?.pause()
                playing = false
                sleepTimerMinutes = 0
                Toast.makeText(context, "Sleep timer finished. Playback paused.", Toast.LENGTH_SHORT).show()
            }
            Toast.makeText(context, "Sleep timer set for $minutes minutes.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Sleep timer turned off.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(player) {
        fun syncFromPlayer() {
            if (player != null) {
                playing = player.isPlaying
                shuffle = player.shuffleModeEnabled
                repeatModeState = player.repeatMode
                val item = player.currentMediaItem
                if (item != null && item.mediaId.isNotBlank()) {
                    val title = item.mediaMetadata.title?.toString() ?: current?.title ?: "Unknown Song"
                    val artist = item.mediaMetadata.artist?.toString() ?: current?.artist ?: "Unknown Artist"
                    current = TrackInfo(
                        id = item.mediaId,
                        title = title,
                        artist = artist,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            }
        }

        syncFromPlayer()

        val listener = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                syncFromPlayer()
            }

            override fun onPlaybackStateChanged(state: Int) {
                syncFromPlayer()
                if (state == Player.STATE_ENDED) {
                    if (repeatModeState == Player.REPEAT_MODE_ONE && current != null) {
                        startTrack(current!!, isUserClick = false, pushToStack = false)
                    } else {
                        playNextRelatedTrack()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playing = isPlaying
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                playing = playWhenReady && player?.playbackState != Player.STATE_ENDED
            }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }

    LaunchedEffect(player, playing) {
        while (playing) {
            val pos = player?.currentPosition ?: 0L
            val dur = player?.duration ?: 0L
            if (dur > 0) {
                currentPositionMs = pos
                durationMs = dur
            }
            delay(250)
        }
    }

    BackHandler(enabled = showFullScreenPlayer) { showFullScreenPlayer = false }
    BackHandler(enabled = currentTab != 0 && !showFullScreenPlayer) { currentTab = 0 }

    CompositionLocalProvider(LocalAppTheme provides currentTheme) {
        val theme = LocalAppTheme.current

        MusicalBackgroundPattern {
            Row(Modifier.fillMaxSize()) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = theme.card.copy(alpha = 0.95f),
                        contentColor = theme.textPrimary
                    ) {
                        Spacer(Modifier.height(16.dp))
                        AppLogo(modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(24.dp))
                        NavigationRailItem(
                            icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            selected = currentTab == 0,
                            onClick = { currentTab = 0; showFullScreenPlayer = false },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                        )
                        NavigationRailItem(
                            icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                            label = { Text("Search") },
                            selected = currentTab == 1,
                            onClick = { currentTab = 1; showFullScreenPlayer = false },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                        )
                        NavigationRailItem(
                            icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
                            label = { Text("History") },
                            selected = currentTab == 2,
                            onClick = { currentTab = 2; showFullScreenPlayer = false },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                        )
                        NavigationRailItem(
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                            label = { Text("Settings") },
                            selected = currentTab == 3,
                            onClick = { currentTab = 3; showFullScreenPlayer = false },
                            colors = NavigationRailItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                        )
                    }
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column {
                            if (current != null && !showFullScreenPlayer) {
                                MiniPlayer(
                                    track = current!!,
                                    playing = playing,
                                    progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()) else 0f,
                                    loading = streamLoading,
                                    onPlayPause = { togglePlayPause() },
                                    onNext = { playNextRelatedTrack() },
                                    onClick = { showFullScreenPlayer = true }
                                )
                            }

                            if (!isWideScreen) {
                                NavigationBar(
                                    containerColor = theme.card.copy(alpha = 0.95f),
                                    contentColor = theme.textPrimary
                                ) {
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                                        label = { Text("Home") },
                                        selected = currentTab == 0,
                                        onClick = { currentTab = 0; showFullScreenPlayer = false },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                                        label = { Text("Search") },
                                        selected = currentTab == 1,
                                        onClick = { currentTab = 1; showFullScreenPlayer = false },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Rounded.History, contentDescription = "History") },
                                        label = { Text("History") },
                                        selected = currentTab == 2,
                                        onClick = { currentTab = 2; showFullScreenPlayer = false },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                                    )
                                    NavigationBarItem(
                                        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") },
                                        selected = currentTab == 3,
                                        onClick = { currentTab = 3; showFullScreenPlayer = false },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.accent, unselectedIconColor = theme.secondary, indicatorColor = theme.cardHover)
                                    )
                                }
                            }
                        }
                    }
                ) { padding ->
                    Box(Modifier.padding(padding).fillMaxSize()) {
                        val columns = if (isWideScreen) GridCells.Adaptive(320.dp) else GridCells.Fixed(1)

                        if (currentTab == 0) {
                            PullToRefreshBox(
                                isRefreshing = homeLoading,
                                onRefresh = { viewModel.loadHomeFeed(isRefresh = true) },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                LazyVerticalGrid(
                                    state = gridState,
                                    columns = columns,
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Spacer(Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isWideScreen) {
                                                AppLogo(modifier = Modifier.size(40.dp))
                                                Spacer(Modifier.width(12.dp))
                                            }
                                            Column(Modifier.weight(1f)) {
                                                Text("OXYCARB", color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                                Text("by AK Bisariya", color = theme.secondary, fontSize = 12.sp)
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
                                    }

                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(getGreeting(), color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(2.dp))
                                            Text("Select a vibe to start listening", color = theme.secondary, fontSize = 12.sp)
                                            Spacer(Modifier.height(12.dp))

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                val vibes = listOf(
                                                    "🔥 Trending" to "Trending Hindi Songs",
                                                    "🎧 Lofi Chill" to "Lofi Chill Beats Hindi",
                                                    "❤️ Romantic" to "Bollywood Romantic Songs",
                                                    "🕺 Party Hits" to "Punjabi Party Hits",
                                                    "✨ 90s Melodies" to "90s Bollywood Melodies",
                                                    "🎸 Acoustic" to "Romantic Acoustic Hindi"
                                                )
                                                items(vibes) { (label, vibeQuery) ->
                                                    FilterChip(
                                                        selected = selectedVibe == label,
                                                        onClick = {
                                                            selectedVibe = label
                                                            viewModel.loadHomeFeedByVibe(vibeQuery)
                                                        },
                                                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                                        colors = FilterChipDefaults.filterChipColors(
                                                            selectedContainerColor = theme.accent,
                                                            selectedLabelColor = theme.textPrimary,
                                                            containerColor = theme.card.copy(alpha = 0.5f),
                                                            labelColor = theme.secondary
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    if (homeLoading && homeSuggestedSongs.isEmpty()) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(color = theme.accent)
                                            }
                                        }
                                    } else if (homeSuggestedSongs.isEmpty()) {
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Text("No songs available right now. Swipe down to refresh!", color = theme.secondary, fontSize = 14.sp)
                                        }
                                    } else {
                                        items(items = homeSuggestedSongs, key = { it.id }) { song ->
                                            SongItem(song, current?.id == song.id, playing, favoriteTracks.any { it.id == song.id },
                                                { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                { startTrack(song) })
                                        }

                                        if (homeLoadingMore) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = theme.accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = columns,
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Spacer(Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!isWideScreen) {
                                            AppLogo(modifier = Modifier.size(40.dp))
                                            Spacer(Modifier.width(12.dp))
                                        }
                                        Column(Modifier.weight(1f)) {
                                            Text("OXYCARB", color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("by AK Bisariya", color = theme.secondary, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.height(16.dp))
                                }

                                when (currentTab) {
                                    1 -> { // SEARCH TAB
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column {
                                                OutlinedTextField(
                                                    value = query,
                                                    onValueChange = { viewModel.search(it.replace("\n", "")) },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                                                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                                        onSearch = {
                                                            viewModel.search(query)
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                        }
                                                    ),
                                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                                    placeholder = { Text("Search songs, podcasts, artists...", color = theme.secondary) },
                                                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = theme.accent) },
                                                    trailingIcon = {
                                                        if (query.isNotBlank()) {
                                                            IconButton(onClick = { viewModel.search("") }) {
                                                                Icon(Icons.Rounded.Clear, contentDescription = "Clear", tint = theme.secondary)
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedContainerColor = theme.card.copy(alpha = 0.5f), unfocusedContainerColor = theme.card.copy(alpha = 0.3f),
                                                        focusedTextColor = theme.textPrimary, unfocusedTextColor = theme.textPrimary, cursorColor = theme.accent,
                                                        focusedBorderColor = theme.accent.copy(alpha = 0.5f), unfocusedBorderColor = theme.textPrimary.copy(alpha = 0.1f)
                                                    )
                                                )

                                                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                    FilterChip(
                                                        selected = searchCategory == 0,
                                                        onClick = { searchCategory = 0 },
                                                        label = { Text("Songs") },
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                    FilterChip(
                                                        selected = searchCategory == 1,
                                                        onClick = { searchCategory = 1 },
                                                        label = { Text("Podcasts") }
                                                    )
                                                }
                                            }
                                        }

                                        if (searchLoading) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                    CircularProgressIndicator(color = theme.accent)
                                                }
                                            }
                                        } else if (searchError != null) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Icon(Icons.Rounded.WifiOff, contentDescription = null, tint = theme.accent, modifier = Modifier.size(48.dp))
                                                    Spacer(Modifier.height(8.dp))
                                                    Text(searchError!!, color = theme.textPrimary, fontSize = 14.sp)
                                                    Spacer(Modifier.height(16.dp))
                                                    Button(
                                                        onClick = { viewModel.retrySearch() },
                                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                                    ) {
                                                        Text("Retry Search")
                                                    }
                                                }
                                            }
                                        } else {
                                            val list = if (searchCategory == 0) searchResultsSongs else searchResultsPodcasts
                                            if (list.isEmpty() && query.isNotBlank()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) { Text("No results found.", color = theme.secondary) }
                                            } else {
                                                items(items = list, key = { it.id }) { song ->
                                                    SongItem(song, current?.id == song.id, playing, favoriteTracks.any { it.id == song.id },
                                                        { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        {
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            startTrack(song)
                                                        })
                                                }
                                            }
                                        }
                                    }

                                    2 -> { // HISTORY & FAVORITES TAB
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                                FilterChip(
                                                    selected = historyCategory == 0,
                                                    onClick = { historyCategory = 0 },
                                                    label = { Text("Listening History (24h)") },
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                                FilterChip(
                                                    selected = historyCategory == 1,
                                                    onClick = { historyCategory = 1 },
                                                    label = { Text("Favorite Songs (${favoriteTracks.size})") }
                                                )
                                            }
                                        }

                                        if (historyCategory == 0) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Text("Recently Played Songs", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (recentSongs.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) { Text("No recently played songs.", color = theme.secondary) }
                                            } else {
                                                items(items = recentSongs.map { it.toTrackInfo() }, key = { it.id }) { song ->
                                                    SongItem(song, current?.id == song.id, playing, favoriteTracks.any { it.id == song.id },
                                                        { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        { startTrack(song) })
                                                }
                                            }

                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Spacer(Modifier.height(16.dp))
                                                Text("Recently Played Podcasts", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (recentPodcasts.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) { Text("No recently played podcasts.", color = theme.secondary) }
                                            } else {
                                                items(items = recentPodcasts.map { it.toTrackInfo() }, key = { it.id }) { song ->
                                                    SongItem(song, current?.id == song.id, playing, favoriteTracks.any { it.id == song.id },
                                                        { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        { startTrack(song) })
                                                }
                                            }
                                        } else { // FAVORITES SECTION
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Text("Saved Favorite Songs", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (favoriteTracks.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) { Text("No favorite songs saved yet. Tap heart icon on any song to save!", color = theme.secondary) }
                                            } else {
                                                items(items = favoriteTracks, key = { it.id }) { song ->
                                                    SongItem(song, current?.id == song.id, playing, true,
                                                        { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        { startTrack(song) })
                                                }
                                            }
                                        }
                                    }

                                    3 -> { // SETTINGS TAB
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column {
                                                Text("Settings", color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(16.dp))
                                                Text("App Personalization", color = theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column(Modifier.fillMaxWidth().glass().padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Palette, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text("App Theme", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                        Text("Current: ${currentTheme.name}", color = theme.secondary, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(Modifier.height(12.dp))
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(AppThemeList) { appTheme ->
                                                        FilterChip(
                                                            selected = savedThemeName == appTheme.name,
                                                            onClick = {
                                                                savedThemeName = appTheme.name
                                                                prefs.edit().putString("app_theme", appTheme.name).apply()
                                                            },
                                                            label = { Text(appTheme.name, fontSize = 12.sp) }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column {
                                                Spacer(Modifier.height(8.dp))
                                                Text("Audio & Playback", color = theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Row(Modifier.fillMaxWidth().glass().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.DataSaverOn, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Data Saver Mode", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                    Text("Stream audio in lower quality to save data", color = theme.secondary, fontSize = 12.sp)
                                                }
                                                Switch(
                                                    checked = dataSaver,
                                                    onCheckedChange = {
                                                        dataSaver = it
                                                        prefs.edit().putBoolean("data_saver", it).apply()
                                                    }
                                                )
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Row(Modifier.fillMaxWidth().glass().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Auto-Play Related Songs", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                    Text("Automatically play next related song when current ends", color = theme.secondary, fontSize = 12.sp)
                                                }
                                                Switch(
                                                    checked = autoPlayEnabled,
                                                    onCheckedChange = {
                                                        autoPlayEnabled = it
                                                        prefs.edit().putBoolean("auto_play", it).apply()
                                                    }
                                                )
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column(Modifier.fillMaxWidth().glass().padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Equalizer, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text("Audio Equalizer Preset", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                        Text("Current: $selectedEqPreset", color = theme.secondary, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(Modifier.height(12.dp))
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(listOf("Normal", "Bass Boost", "Vocal", "Rock", "Pop")) { preset ->
                                                        FilterChip(
                                                            selected = selectedEqPreset == preset,
                                                            onClick = {
                                                                selectedEqPreset = preset
                                                                prefs.edit().putString("eq_preset", preset).apply()
                                                                Toast.makeText(context, "Equalizer set to $preset", Toast.LENGTH_SHORT).show()
                                                            },
                                                            label = { Text(preset, fontSize = 12.sp) }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column(Modifier.fillMaxWidth().glass().padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text("Sleep Timer", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                        Text(if (sleepTimerMinutes > 0) "Timer active: $sleepTimerMinutes minutes remaining" else "Off", color = theme.secondary, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(Modifier.height(12.dp))
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m", -1 to "Custom")) { (mins, label) ->
                                                        FilterChip(
                                                            selected = (mins != -1 && sleepTimerMinutes == mins) || (mins == -1 && sleepTimerMinutes > 60),
                                                            onClick = {
                                                                if (mins == -1) {
                                                                    showCustomTimerDialog = true
                                                                } else {
                                                                    setSleepTimer(mins)
                                                                }
                                                            },
                                                            label = { Text(label, fontSize = 12.sp) }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column {
                                                Spacer(Modifier.height(8.dp))
                                                Text("Storage & Data", color = theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(8.dp))
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .glass()
                                                    .clickable {
                                                        viewModel.clearHistory()
                                                        Toast.makeText(context, "Listening history cleared!", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Clear Listening History", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                    Text("Delete all stored 24h history from local database", color = theme.secondary, fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .glass()
                                                    .clickable {
                                                        try {
                                                            val cacheDir = File(context.cacheDir, "http_cache")
                                                            cacheDir.deleteRecursively()
                                                            Toast.makeText(context, "Audio cache cleared!", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Failed to clear cache", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Rounded.CleaningServices, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Column(Modifier.weight(1f)) {
                                                    Text("Clear Network & Audio Cache", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                    Text("Free up disk space by clearing cached stream files", color = theme.secondary, fontSize = 12.sp)
                                                }
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Column {
                                                Spacer(Modifier.height(8.dp))
                                                Text("Developer Social Handles", color = theme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                Spacer(Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth().glass().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceAround,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Button(
                                                        onClick = { uriHandler.openUri("https://youtube.com/@AK_Bisariya") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                                    ) {
                                                        Icon(Icons.Rounded.PlayArrow, contentDescription = "YouTube", modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("YouTube", fontSize = 12.sp)
                                                    }
                                                    Button(
                                                        onClick = { uriHandler.openUri("https://x.com/AK_Bisariya") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = theme.cardHover)
                                                    ) {
                                                        Icon(Icons.Rounded.Share, contentDescription = "X", tint = theme.accent, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("X (Twitter)", fontSize = 12.sp, color = theme.textPrimary)
                                                    }
                                                    Button(
                                                        onClick = { uriHandler.openUri("https://github.com/NAGA-RESEARCH") },
                                                        colors = ButtonDefaults.buttonColors(containerColor = theme.cardHover)
                                                    ) {
                                                        Icon(Icons.Rounded.Code, contentDescription = "GitHub", tint = theme.accent, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("GitHub", fontSize = 12.sp, color = theme.textPrimary)
                                                    }
                                                }
                                            }
                                        }

                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            Spacer(Modifier.height(8.dp))
                                            Column(
                                                modifier = Modifier.fillMaxWidth().glass().padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text("OXYCARB v1.1.9 (Universal)", color = theme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(Modifier.height(4.dp))
                                                Text("Crafted with ❤️ by AK Bisariya", color = theme.secondary, fontSize = 12.sp)
                                                Text("Engine: Media3 ExoPlayer & NewPipeExtractor", color = theme.secondary.copy(alpha = 0.7f), fontSize = 10.sp)
                                            }
                                            Spacer(Modifier.height(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (showFullScreenPlayer && current != null) {
                        FullScreenPlayer(
                            track = current!!,
                            playing = playing,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            shuffle = shuffle,
                            repeatMode = repeatModeState,
                            playbackSpeed = playbackSpeed,
                            isWideScreen = isWideScreen,
                            onClose = { showFullScreenPlayer = false },
                            onPlayPause = { togglePlayPause() },
                            onSkipPrevious = { playPreviousTrack() },
                            onSkipNext = { playNextRelatedTrack() },
                            onShuffleToggle = {
                                shuffle = !shuffle
                                player?.shuffleModeEnabled = shuffle
                            },
                            onRepeatToggle = {
                                repeatModeState = when (repeatModeState) {
                                    Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
                                    Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
                                    else -> Player.REPEAT_MODE_OFF
                                }
                                player?.repeatMode = repeatModeState
                            },
                            onSeek = { fraction ->
                                val target = (fraction * durationMs).toLong()
                                player?.seekTo(target)
                                currentPositionMs = target
                            },
                            onSpeedChange = {
                                playbackSpeed = it
                                player?.playbackParameters = PlaybackParameters(it)
                            }
                        )
                    }

                    if (showCustomTimerDialog) {
                        AlertDialog(
                            onDismissRequest = { showCustomTimerDialog = false },
                            containerColor = theme.card,
                            title = { Text("Custom Sleep Timer", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("Slide to set custom sleep timer:", color = theme.secondary, fontSize = 14.sp)
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "${customSliderMinutes.toInt()} Minutes",
                                        color = theme.accent,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Slider(
                                        value = customSliderMinutes,
                                        onValueChange = { customSliderMinutes = it },
                                        valueRange = 60f..120f,
                                        steps = 11,
                                        colors = SliderDefaults.colors(
                                            thumbColor = theme.accent,
                                            activeTrackColor = theme.accent,
                                            inactiveTrackColor = theme.cardHover
                                        )
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("60m", color = theme.secondary, fontSize = 12.sp)
                                        Text("120m", color = theme.secondary, fontSize = 12.sp)
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        setSleepTimer(customSliderMinutes.toInt())
                                        showCustomTimerDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                ) {
                                    Text("Set Timer")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCustomTimerDialog = false }) {
                                    Text("Cancel", color = theme.secondary)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: TrackInfo,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isFav: Boolean,
    onFavToggle: () -> Unit,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .glass(alpha = if (isCurrent) 0.6f else 0.3f)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(theme.cardHover),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isCurrent && isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = if (isCurrent) theme.accent else theme.secondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(song.title, color = if (isCurrent) theme.accent else theme.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = theme.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onFavToggle) {
            Icon(
                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = null,
                tint = if (isFav) theme.accent else theme.secondary
            )
        }
    }
}

@Composable
fun MiniPlayer(
    track: TrackInfo,
    playing: Boolean,
    progress: Float,
    loading: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit
) {
    val theme = LocalAppTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp), alpha = 0.95f)
            .clickable { onClick() }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = theme.accent,
            trackColor = theme.cardHover
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(theme.cardHover),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = theme.accent)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, color = theme.textPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(track.artist, color = theme.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (loading) {
                CircularProgressIndicator(color = theme.accent, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onPlayPause) {
                    Crossfade(targetState = playing, label = "PlayPauseTransition") { isPlaying ->
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = theme.textPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next", tint = theme.textPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
fun FullScreenPlayer(
    track: TrackInfo,
    playing: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeatMode: Int,
    playbackSpeed: Float,
    isWideScreen: Boolean,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit,
    onShuffleToggle: () -> Unit,
    onRepeatToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit
) {
    val theme = LocalAppTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "VinylRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        FullScreenBackgroundVisualizer(
            isPlaying = playing,
            modifier = Modifier.fillMaxWidth().height(320.dp).align(Alignment.BottomCenter)
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Close", tint = theme.textPrimary, modifier = Modifier.size(32.dp))
                }
                Text("NOW PLAYING", color = theme.secondary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(Modifier.width(48.dp))
            }

            Spacer(Modifier.height(16.dp))

            if (isWideScreen) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .clip(CircleShape)
                                .background(theme.cardHover)
                                .rotate(if (playing) rotationAngle else 0f)
                                .border(4.dp, theme.accent.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier.size(80.dp).clip(CircleShape).background(theme.card).border(2.dp, theme.accent.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp), tint = theme.accent)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(track.title, color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(track.artist, color = theme.secondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                        Spacer(Modifier.height(24.dp))

                        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                        Slider(
                            value = progress,
                            onValueChange = onSeek,
                            colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent, inactiveTrackColor = theme.cardHover)
                        )

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatTime(currentPositionMs), color = theme.secondary, fontSize = 12.sp)
                            Text(formatTime(durationMs), color = theme.secondary, fontSize = 12.sp)
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onSkipPrevious,
                                modifier = Modifier.size(48.dp).glass(CircleShape, alpha = 0.4f)
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Skip Previous", tint = theme.textPrimary, modifier = Modifier.size(28.dp))
                            }

                            IconButton(
                                onClick = onPlayPause,
                                modifier = Modifier.size(64.dp).background(theme.accent, CircleShape).shadow(10.dp, CircleShape, spotColor = theme.accent)
                            ) {
                                Crossfade(targetState = playing, label = "PlayPauseTransitionFullWide") { isPlaying ->
                                    Icon(
                                        if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = theme.textPrimary,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onSkipNext,
                                modifier = Modifier.size(48.dp).glass(CircleShape, alpha = 0.4f)
                            ) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Skip Next", tint = theme.textPrimary, modifier = Modifier.size(28.dp))
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glass(RoundedCornerShape(24.dp), alpha = 0.4f)
                                .padding(vertical = 6.dp, horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.clickable { onShuffleToggle() }.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffle) theme.accent else theme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (shuffle) "Shuffle On" else "Shuffle Off", color = if (shuffle) theme.accent else theme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(16.dp).background(theme.textPrimary.copy(alpha = 0.1f)))

                            Row(
                                modifier = Modifier.clickable { onRepeatToggle() }.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                                        Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                                        else -> Icons.Rounded.Repeat
                                    },
                                    contentDescription = "Repeat",
                                    tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    when (repeatMode) {
                                        Player.REPEAT_MODE_ONE -> "Repeat One"
                                        Player.REPEAT_MODE_ALL -> "Repeat All"
                                        else -> "Repeat Off"
                                    },
                                    color = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.secondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(theme.accent.copy(alpha = if (playing) 0.4f else 0.15f), Color.Transparent)
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .background(theme.cardHover)
                            .rotate(if (playing) rotationAngle else 0f)
                            .border(4.dp, theme.accent.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(80.dp).clip(CircleShape).background(theme.card).border(2.dp, theme.accent.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(40.dp), tint = theme.accent)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(track.title, color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(track.artist, color = theme.secondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

                Spacer(Modifier.height(20.dp))

                val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(thumbColor = theme.accent, activeTrackColor = theme.accent, inactiveTrackColor = theme.cardHover)
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(currentPositionMs), color = theme.secondary, fontSize = 12.sp)
                    Text(formatTime(durationMs), color = theme.secondary, fontSize = 12.sp)
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipPrevious,
                        modifier = Modifier.size(52.dp).glass(CircleShape, alpha = 0.4f)
                    ) {
                        Icon(Icons.Rounded.SkipPrevious, contentDescription = "Skip Previous", tint = theme.textPrimary, modifier = Modifier.size(28.dp))
                    }

                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(72.dp).background(theme.accent, CircleShape).shadow(12.dp, CircleShape, spotColor = theme.accent)
                    ) {
                        Crossfade(targetState = playing, label = "PlayPauseTransitionFull") { isPlaying ->
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = theme.textPrimary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onSkipNext,
                        modifier = Modifier.size(52.dp).glass(CircleShape, alpha = 0.4f)
                    ) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Skip Next", tint = theme.textPrimary, modifier = Modifier.size(28.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glass(RoundedCornerShape(24.dp), alpha = 0.4f)
                        .padding(vertical = 8.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clickable { onShuffleToggle() }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = "Shuffle", tint = if (shuffle) theme.accent else theme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (shuffle) "Shuffle On" else "Shuffle Off", color = if (shuffle) theme.accent else theme.secondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(theme.textPrimary.copy(alpha = 0.1f)))

                    Row(
                        modifier = Modifier.clickable { onRepeatToggle() }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                                Player.REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                                else -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat",
                            tint = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (repeatMode) {
                                Player.REPEAT_MODE_ONE -> "Repeat One"
                                Player.REPEAT_MODE_ALL -> "Repeat All"
                                else -> "Repeat Off"
                            },
                            color = if (repeatMode != Player.REPEAT_MODE_OFF) theme.accent else theme.secondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
