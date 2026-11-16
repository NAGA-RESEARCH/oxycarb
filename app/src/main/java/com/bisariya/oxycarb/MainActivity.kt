package com.bisariya.oxycarb

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as lazyGridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bisariya.oxycarb.db.PlaylistEntity
import com.bisariya.oxycarb.db.PlaylistTrackCrossRef
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
    val customPlaylists by viewModel.customPlaylists.collectAsState()
    val homeSuggestedSongs by viewModel.homeSuggestedSongs.collectAsState()
    val homeLoading by viewModel.homeLoading.collectAsState()
    val homeLoadingMore by viewModel.homeLoadingMore.collectAsState()

    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val selectedPlaylistTracks by viewModel.selectedPlaylistTracks.collectAsState()
    val playlistLoading by viewModel.playlistLoading.collectAsState()

    val searchResultsSongs by viewModel.searchResultsSongs.collectAsState()
    val searchResultsPodcasts by viewModel.searchResultsPodcasts.collectAsState()
    val query by viewModel.currentQuery.collectAsState()
    val searchLoading by viewModel.searchLoading.collectAsState()
    val searchError by viewModel.searchError.collectAsState()

    var favoriteTracks by remember { mutableStateOf(FavManager.getFavs(context)) }
    var downloadedTracks by remember { mutableStateOf(DownloadHelper.getDownloadedTracks(context)) }

    var current by remember { mutableStateOf<TrackInfo?>(null) }
    var playing by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }
    var streamLoading by remember { mutableStateOf(false) }
    var shuffle by rememberSaveable { mutableStateOf(false) }
    var repeatModeState by rememberSaveable { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var showFullScreenPlayer by rememberSaveable { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable { mutableStateOf(false) }
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var createPlaylistInput by rememberSaveable { mutableStateOf("") }
    var trackToAddToPlaylist by remember { mutableStateOf<TrackInfo?>(null) }

    var playbackError by remember { mutableStateOf<String?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var dataSaver by remember { mutableStateOf(prefs.getBoolean("data_saver", false)) }
    var selectedAudioQuality by remember { mutableStateOf(prefs.getString("audio_quality", "Balanced (96kbps)") ?: "Balanced (96kbps)") }
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

    fun startTrack(track: TrackInfo, mode: PlaybackMode = PlaybackMode.RADIO_AUTOPLAY, queue: List<TrackInfo> = emptyList(), isUserClick: Boolean = true, pushToStack: Boolean = true) {
        // Synchronous Main Thread Cleanup FIRST before launching coroutine (Zero Audio Overlap)
        player?.pause()
        player?.stop()
        player?.clearMediaItems()

        playbackJob?.cancel()

        if (pushToStack && current != null && current?.id != track.id) {
            playedHistoryStack.add(current!!)
        }

        if (isUserClick) {
            viewModel.playbackMode.value = mode
            if (mode == PlaybackMode.PLAYLIST_LOOP) {
                viewModel.playlistQueue.value = queue
            }
        }

        current = track
        playing = false
        currentPositionMs = 0L
        playbackError = null
        streamLoading = true

        viewModel.addToHistory(track)

        playbackJob = coroutineScope.launch(Dispatchers.Main) {
            val localUri = DownloadHelper.getLocalUri(context, track.id)
            val streamUrl = localUri ?: YoutubeExtractor.getAudioUrl(track.id, selectedAudioQuality)
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

            if (isUserClick && viewModel.playbackMode.value == PlaybackMode.RADIO_AUTOPLAY) {
                coroutineScope.launch(Dispatchers.IO) {
                    val related = YoutubeExtractor.getRelatedTracks(track.id)
                    relatedQueue.clear()
                    relatedQueue.addAll(related)
                }
            }
        }
    }

    fun playNextRelatedTrack() {
        if (viewModel.playbackMode.value == PlaybackMode.PLAYLIST_LOOP) {
            val queue = viewModel.playlistQueue.value
            if (queue.isNotEmpty()) {
                val currentIndex = queue.indexOfFirst { it.id == current?.id }
                val nextTrack = if (shuffle) {
                    queue.random()
                } else {
                    if (currentIndex != -1 && currentIndex < queue.size - 1) {
                        queue[currentIndex + 1]
                    } else {
                        queue.first()
                    }
                }
                startTrack(nextTrack, mode = PlaybackMode.PLAYLIST_LOOP, isUserClick = false, pushToStack = true)
            }
        } else {
            if (autoPlayEnabled && relatedQueue.isNotEmpty()) {
                val nextTrack = if (shuffle) relatedQueue.random() else relatedQueue.removeAt(0)
                startTrack(nextTrack, mode = PlaybackMode.RADIO_AUTOPLAY, isUserClick = false, pushToStack = true)
            }
        }
    }

    fun playPreviousTrack() {
        if (playedHistoryStack.isNotEmpty()) {
            val prevTrack = playedHistoryStack.removeAt(playedHistoryStack.size - 1)
            startTrack(prevTrack, mode = viewModel.playbackMode.value, isUserClick = false, pushToStack = false)
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

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.bisariya.oxycarb.ACTION_SKIP_NEXT" -> playNextRelatedTrack()
                    "com.bisariya.oxycarb.ACTION_SKIP_PREVIOUS" -> playPreviousTrack()
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction("com.bisariya.oxycarb.ACTION_SKIP_NEXT")
            addAction("com.bisariya.oxycarb.ACTION_SKIP_PREVIOUS")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
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

    val prefetchedSet = remember { mutableStateOf(mutableSetOf<String>()) }

    LaunchedEffect(player, playing) {
        while (playing) {
            val pos = player?.currentPosition ?: 0L
            val dur = player?.duration ?: 0L
            if (dur > 0) {
                currentPositionMs = pos
                durationMs = dur

                // Fast pre-fetch next track stream URL at 50% progress for instant transition (< 400ms)
                val progressFraction = pos.toFloat() / dur.toFloat()
                if (progressFraction >= 0.50f) {
                    val nextTrack = if (viewModel.playbackMode.value == PlaybackMode.PLAYLIST_LOOP) {
                        val queue = viewModel.playlistQueue.value
                        val currentIndex = queue.indexOfFirst { it.id == current?.id }
                        if (shuffle) queue.randomOrNull() else queue.getOrNull(currentIndex + 1) ?: queue.firstOrNull()
                    } else {
                        relatedQueue.firstOrNull()
                    }

                    if (nextTrack != null && !prefetchedSet.value.contains(nextTrack.id)) {
                        prefetchedSet.value.add(nextTrack.id)
                        coroutineScope.launch(Dispatchers.IO) {
                            val localUri = DownloadHelper.getLocalUri(context, nextTrack.id)
                            if (localUri == null) {
                                YoutubeExtractor.getAudioUrl(nextTrack.id, selectedAudioQuality)
                            }
                        }
                    }
                }
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
                            icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
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
                                        icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library") },
                                        label = { Text("Library") },
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
                                                lazyListItems(vibes) { vibePair ->
                                                    val label = vibePair.first
                                                    val vibeQuery = vibePair.second
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

                                            Spacer(Modifier.height(24.dp))
                                            Text("Curated Genre Playlists", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(12.dp))

                                            LazyRow(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                lazyListItems(viewModel.genrePlaylists) { playlist ->
                                                    PlaylistCard(playlist, theme) {
                                                        viewModel.loadPlaylist(playlist)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(16.dp))
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
                                        lazyGridItems(items = homeSuggestedSongs, key = { it.id }) { song ->
                                            SongItem(
                                                song = song,
                                                isCurrent = current?.id == song.id,
                                                isPlaying = playing,
                                                isFav = favoriteTracks.any { it.id == song.id },
                                                onFavToggle = { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                onClick = { startTrack(song, mode = PlaybackMode.RADIO_AUTOPLAY, isUserClick = true) },
                                                onPlayNext = { relatedQueue.add(0, song); Toast.makeText(context, "Added to Play Next", Toast.LENGTH_SHORT).show() },
                                                onAddToPlaylist = { trackToAddToPlaylist = song },
                                                onDownload = {
                                                    coroutineScope.launch {
                                                        DownloadHelper.downloadTrack(context, song, selectedAudioQuality, {}, { success ->
                                                            if (success) {
                                                                downloadedTracks = DownloadHelper.getDownloadedTracks(context)
                                                                Toast.makeText(context, "Downloaded ${song.title}", Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                            }
                                                        })
                                                    }
                                                }
                                            )
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
                                                lazyGridItems(items = list, key = { it.id }) { song ->
                                                    SongItem(
                                                        song = song,
                                                        isCurrent = current?.id == song.id,
                                                        isPlaying = playing,
                                                        isFav = favoriteTracks.any { it.id == song.id },
                                                        onFavToggle = { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        onClick = {
                                                            keyboardController?.hide()
                                                            focusManager.clearFocus()
                                                            startTrack(song, mode = PlaybackMode.RADIO_AUTOPLAY, isUserClick = true)
                                                        },
                                                        onPlayNext = { relatedQueue.add(0, song); Toast.makeText(context, "Added to Play Next", Toast.LENGTH_SHORT).show() },
                                                        onAddToPlaylist = { trackToAddToPlaylist = song },
                                                        onDownload = {
                                                            coroutineScope.launch {
                                                                DownloadHelper.downloadTrack(context, song, selectedAudioQuality, {}, { success ->
                                                                    if (success) {
                                                                        downloadedTracks = DownloadHelper.getDownloadedTracks(context)
                                                                        Toast.makeText(context, "Downloaded ${song.title}", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                })
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    2 -> { // LIBRARY TAB (HISTORY, FAVORITES, CUSTOM PLAYLISTS, DOWNLOADS)
                                        item(span = { GridItemSpan(maxLineSpan) }) {
                                            LazyRow(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                item { FilterChip(selected = historyCategory == 0, onClick = { historyCategory = 0 }, label = { Text("History (24h)") }) }
                                                item { FilterChip(selected = historyCategory == 1, onClick = { historyCategory = 1 }, label = { Text("Favorites (${favoriteTracks.size})") }) }
                                                item { FilterChip(selected = historyCategory == 2, onClick = { historyCategory = 2 }, label = { Text("Playlists (${customPlaylists.size})") }) }
                                                item { FilterChip(selected = historyCategory == 3, onClick = { historyCategory = 3 }, label = { Text("Downloads (${downloadedTracks.size})") }) }
                                            }
                                        }

                                        if (historyCategory == 0) {
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Text("Recently Played Songs", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (recentSongs.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) { Text("No recently played songs.", color = theme.secondary) }
                                            } else {
                                                val list = recentSongs.map { it.toTrackInfo() }
                                                lazyGridItems(items = list, key = { it.id }) { song ->
                                                    SongItem(
                                                        song = song,
                                                        isCurrent = current?.id == song.id,
                                                        isPlaying = playing,
                                                        isFav = favoriteTracks.any { it.id == song.id },
                                                        onFavToggle = { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        onClick = { startTrack(song, mode = PlaybackMode.RADIO_AUTOPLAY, isUserClick = true) }
                                                    )
                                                }
                                            }
                                        } else if (historyCategory == 1) { // FAVORITES
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Text("Saved Favorite Songs", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (favoriteTracks.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) { Text("No favorite songs saved yet.", color = theme.secondary) }
                                            } else {
                                                lazyGridItems(items = favoriteTracks, key = { it.id }) { song ->
                                                    SongItem(
                                                        song = song,
                                                        isCurrent = current?.id == song.id,
                                                        isPlaying = playing,
                                                        isFav = true,
                                                        onFavToggle = { FavManager.toggleFav(context, song); favoriteTracks = FavManager.getFavs(context) },
                                                        onClick = { startTrack(song, mode = PlaybackMode.RADIO_AUTOPLAY, isUserClick = true) }
                                                    )
                                                }
                                            }
                                        } else if (historyCategory == 2) { // CUSTOM PLAYLISTS
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("Your Custom Playlists", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                                    Button(
                                                        onClick = { showCreatePlaylistDialog = true },
                                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                                    ) {
                                                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Create", fontSize = 12.sp)
                                                    }
                                                }
                                            }

                                            if (customPlaylists.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    Text("No custom playlists created yet. Tap Create to add one!", color = theme.secondary)
                                                }
                                            } else {
                                                lazyGridItems(items = customPlaylists, key = { it.playlistId }) { playlist ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .glass()
                                                            .clickable {
                                                                viewModel.loadPlaylist(
                                                                    GenrePlaylist(
                                                                        id = playlist.playlistId.toString(),
                                                                        title = playlist.name,
                                                                        query = playlist.name
                                                                    )
                                                                )
                                                            }
                                                            .padding(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.PlaylistPlay, contentDescription = null, tint = theme.accent, modifier = Modifier.size(28.dp))
                                                        Spacer(Modifier.width(12.dp))
                                                        Column(Modifier.weight(1f)) {
                                                            Text(playlist.name, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                                                            Text("Custom Playlist", color = theme.secondary, fontSize = 12.sp)
                                                        }
                                                        IconButton(onClick = { viewModel.deleteCustomPlaylist(playlist.playlistId) }) {
                                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = theme.secondary)
                                                        }
                                                    }
                                                }
                                            }
                                        } else if (historyCategory == 3) { // OFFLINE DOWNLOADS
                                            item(span = { GridItemSpan(maxLineSpan) }) {
                                                Text("Downloaded Songs (Offline)", color = theme.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            }

                                            if (downloadedTracks.isEmpty()) {
                                                item(span = { GridItemSpan(maxLineSpan) }) {
                                                    Text("No downloaded songs. Tap download option on any song to save offline!", color = theme.secondary)
                                                }
                                            } else {
                                                lazyGridItems(items = downloadedTracks, key = { it.id }) { song ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .glass()
                                                            .clickable { startTrack(song, mode = PlaybackMode.RADIO_AUTOPLAY, isUserClick = true) }
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Rounded.OfflinePin, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                        Spacer(Modifier.width(12.dp))
                                                        Column(Modifier.weight(1f)) {
                                                            Text(song.title, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                            Text(song.artist, color = theme.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                        }
                                                        IconButton(
                                                            onClick = {
                                                                DownloadHelper.deleteDownload(context, song.id)
                                                                downloadedTracks = DownloadHelper.getDownloadedTracks(context)
                                                            }
                                                        ) {
                                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Download", tint = theme.secondary)
                                                        }
                                                    }
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
                                                    lazyListItems(AppThemeList) { appTheme ->
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
                                            Column(Modifier.fillMaxWidth().glass().padding(16.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = theme.accent, modifier = Modifier.size(24.dp))
                                                    Spacer(Modifier.width(12.dp))
                                                    Column {
                                                        Text("Audio Quality / Bitrate", color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                        Text("Current: $selectedAudioQuality", color = theme.secondary, fontSize = 12.sp)
                                                    }
                                                }
                                                Spacer(Modifier.height(12.dp))
                                                LazyRow(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    val options = listOf("Data Saver (48kbps)", "Balanced (96kbps)", "High Quality (128kbps+)")
                                                    lazyListItems(options) { quality ->
                                                        FilterChip(
                                                            selected = selectedAudioQuality == quality,
                                                            onClick = {
                                                                selectedAudioQuality = quality
                                                                prefs.edit().putString("audio_quality", quality).apply()
                                                                Toast.makeText(context, "Audio quality set to $quality", Toast.LENGTH_SHORT).show()
                                                            },
                                                            label = { Text(quality, fontSize = 12.sp) }
                                                        )
                                                    }
                                                }
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
                                                    val presets = listOf("Normal", "Bass Boost", "Vocal", "Rock", "Pop")
                                                    lazyListItems(presets) { preset ->
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
                                                    val timerOpts = listOf(0 to "Off", 15 to "15m", 30 to "30m", 60 to "60m", -1 to "Custom")
                                                    lazyListItems(timerOpts) { (mins, label) ->
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

                    if (selectedPlaylist != null) {
                        PlaylistDetailScreen(
                            playlist = selectedPlaylist!!,
                            tracks = selectedPlaylistTracks,
                            loading = playlistLoading,
                            theme = theme,
                            playing = playing,
                            currentTrack = current,
                            favoriteTracks = favoriteTracks,
                            onFavToggle = { track ->
                                FavManager.toggleFav(context, track)
                                favoriteTracks = FavManager.getFavs(context)
                            },
                            onClose = { viewModel.closePlaylist() },
                            onPlayAll = {
                                if (selectedPlaylistTracks.isNotEmpty()) {
                                    startTrack(selectedPlaylistTracks.first(), mode = PlaybackMode.PLAYLIST_LOOP, queue = selectedPlaylistTracks, isUserClick = true)
                                }
                            },
                            onShuffle = {
                                if (selectedPlaylistTracks.isNotEmpty()) {
                                    shuffle = true
                                    player?.shuffleModeEnabled = true
                                    startTrack(selectedPlaylistTracks.random(), mode = PlaybackMode.PLAYLIST_LOOP, queue = selectedPlaylistTracks, isUserClick = true)
                                }
                            },
                            onTrackClick = { track ->
                                startTrack(track, mode = PlaybackMode.PLAYLIST_LOOP, queue = selectedPlaylistTracks, isUserClick = true)
                            }
                        )
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
                            },
                            onOpenQueue = { showQueueSheet = true }
                        )
                    }

                    if (showQueueSheet) {
                        val activeQueue = if (viewModel.playbackMode.value == PlaybackMode.PLAYLIST_LOOP) {
                            viewModel.playlistQueue.value
                        } else {
                            relatedQueue
                        }
                        UpNextQueueSheet(
                            queue = activeQueue,
                            currentTrack = current,
                            playing = playing,
                            playbackMode = viewModel.playbackMode.value,
                            theme = theme,
                            onDismiss = { showQueueSheet = false },
                            onTrackSelect = { track -> startTrack(track, mode = viewModel.playbackMode.value, isUserClick = true) },
                            onRemoveTrack = { track ->
                                if (viewModel.playbackMode.value == PlaybackMode.PLAYLIST_LOOP) {
                                    viewModel.playlistQueue.value = viewModel.playlistQueue.value - track
                                } else {
                                    relatedQueue.remove(track)
                                }
                            }
                        )
                    }

                    if (showCreatePlaylistDialog) {
                        AlertDialog(
                            onDismissRequest = { showCreatePlaylistDialog = false },
                            containerColor = theme.card,
                            title = { Text("Create New Playlist", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
                            text = {
                                OutlinedTextField(
                                    value = createPlaylistInput,
                                    onValueChange = { createPlaylistInput = it },
                                    placeholder = { Text("e.g. Gym Motivation, Study", color = theme.secondary) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = theme.textPrimary, unfocusedTextColor = theme.textPrimary,
                                        cursorColor = theme.accent, focusedBorderColor = theme.accent, unfocusedBorderColor = theme.secondary
                                    )
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (createPlaylistInput.isNotBlank()) {
                                            viewModel.createCustomPlaylist(createPlaylistInput.trim())
                                            createPlaylistInput = ""
                                            showCreatePlaylistDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                ) {
                                    Text("Create")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCreatePlaylistDialog = false }) {
                                    Text("Cancel", color = theme.secondary)
                                }
                            }
                        )
                    }

                    if (trackToAddToPlaylist != null) {
                        AlertDialog(
                            onDismissRequest = { trackToAddToPlaylist = null },
                            containerColor = theme.card,
                            title = { Text("Add to Playlist", color = theme.textPrimary, fontWeight = FontWeight.Bold) },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    if (customPlaylists.isEmpty()) {
                                        Text("No playlists found. Create one in Library first!", color = theme.secondary)
                                    } else {
                                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                                            lazyListItems(items = customPlaylists, key = { it.playlistId }) { playlist ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.addTrackToCustomPlaylist(playlist.playlistId, trackToAddToPlaylist!!)
                                                            Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                                            trackToAddToPlaylist = null
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = theme.accent)
                                                    Spacer(Modifier.width(12.dp))
                                                    Text(playlist.name, color = theme.textPrimary, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { trackToAddToPlaylist = null }) {
                                    Text("Close", color = theme.secondary)
                                }
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
    onClick: () -> Unit,
    onPlayNext: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    val theme = LocalAppTheme.current
    var showMenu by remember { mutableStateOf(false) }

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
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = theme.secondary)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(theme.card)
            ) {
                if (onPlayNext != null) {
                    DropdownMenuItem(
                        text = { Text("Play Next", color = theme.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = theme.accent) },
                        onClick = { showMenu = false; onPlayNext() }
                    )
                }
                if (onAddToPlaylist != null) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = theme.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null, tint = theme.accent) },
                        onClick = { showMenu = false; onAddToPlaylist() }
                    )
                }
                if (onDownload != null) {
                    DropdownMenuItem(
                        text = { Text("Download Song", color = theme.textPrimary) },
                        leadingIcon = { Icon(Icons.Rounded.Download, contentDescription = null, tint = theme.accent) },
                        onClick = { showMenu = false; onDownload() }
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpNextQueueSheet(
    queue: List<TrackInfo>,
    currentTrack: TrackInfo?,
    playing: Boolean,
    playbackMode: PlaybackMode,
    theme: AppThemePalette,
    onDismiss: () -> Unit,
    onTrackSelect: (TrackInfo) -> Unit,
    onRemoveTrack: (TrackInfo) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = theme.card,
        contentColor = theme.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Up Next Queue", color = theme.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (playbackMode == PlaybackMode.PLAYLIST_LOOP) "Mode: Playlist Loop" else "Mode: Smart Radio Auto-Play",
                        color = theme.secondary,
                        fontSize = 12.sp
                    )
                }
                Text("${queue.size} Tracks", color = theme.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))

            if (queue.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No upcoming tracks in queue.", color = theme.secondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lazyListItems(items = queue, key = { it.id }) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .glass(alpha = 0.3f)
                                .clickable { onTrackSelect(track) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (currentTrack?.id == track.id && playing) Icons.Rounded.GraphicEq else Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = if (currentTrack?.id == track.id) theme.accent else theme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(track.title, color = if (currentTrack?.id == track.id) theme.accent else theme.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, color = theme.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { onRemoveTrack(track) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = theme.secondary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
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
    onSpeedChange: (Float) -> Unit,
    onOpenQueue: () -> Unit
) {
    val theme = LocalAppTheme.current
    var showLyrics by remember { mutableStateOf(false) }
    var lyricsLines by remember { mutableStateOf<List<LrcLine>?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }

    LaunchedEffect(track.id) {
        lyricsLoading = true
        lyricsLines = LrcLyricsHelper.fetchLyrics(track.title, track.artist)
        lyricsLoading = false
    }

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
                Row {
                    IconButton(onClick = { showLyrics = !showLyrics }) {
                        Icon(Icons.Rounded.Lyrics, contentDescription = "Lyrics", tint = if (showLyrics) theme.accent else theme.textPrimary, modifier = Modifier.size(24.dp))
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue", tint = theme.textPrimary, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .glass(alpha = 0.4f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (lyricsLoading) {
                        CircularProgressIndicator(color = theme.accent)
                    } else if (lyricsLines.isNullOrEmpty()) {
                        Text("No synced lyrics available for this track.", color = theme.secondary, fontSize = 14.sp)
                    } else {
                        val activeIndex = lyricsLines!!.indexOfLast { it.timeMs <= currentPositionMs }.coerceAtLeast(0)
                        val listState = rememberLazyListState()

                        LaunchedEffect(activeIndex) {
                            if (activeIndex >= 0) {
                                listState.animateScrollToItem(activeIndex)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            val lines = lyricsLines!!
                            lazyListItems(items = lines, key = { it.timeMs }) { line ->
                                val isCurrent = lines.indexOf(line) == activeIndex
                                Text(
                                    text = line.text,
                                    color = if (isCurrent) theme.accent else theme.textPrimary.copy(alpha = 0.5f),
                                    fontSize = if (isCurrent) 20.sp else 16.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.clickable {
                                        if (durationMs > 0) {
                                            onSeek(line.timeMs.toFloat() / durationMs.toFloat())
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (isWideScreen) {
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
                        Text(if (shuffle) "Shuffle On" else "Shuffle Off", color = if (shuffle) theme.accent else theme.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(playlist: GenrePlaylist, theme: AppThemePalette, onClick: () -> Unit) {
    val gradientColors = when (playlist.id) {
        "1" -> listOf(Color(0xFF4A00E0), Color(0xFF8E2DE2))
        "2" -> listOf(Color(0xFFf12711), Color(0xFFf5af19))
        "3" -> listOf(Color(0xFF000000), Color(0xFFe74c3c))
        "4" -> listOf(Color(0xFF11998e), Color(0xFF38ef7d))
        "5" -> listOf(Color(0xFFF3904F), Color(0xFF3B4371))
        "6" -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        else -> listOf(theme.accent, theme.secondary)
    }

    val icon = when (playlist.id) {
        "1" -> Icons.Rounded.Nightlife
        "2" -> Icons.Rounded.LocalFireDepartment
        "3" -> Icons.Rounded.FitnessCenter
        "4" -> Icons.Rounded.Album
        "5" -> Icons.Rounded.Headphones
        "6" -> Icons.Rounded.Code
        else -> Icons.Rounded.QueueMusic
    }

    Box(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradientColors))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            Column {
                Text(playlist.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text("20+ Tracks", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun PlaylistDetailScreen(
    playlist: GenrePlaylist,
    tracks: List<TrackInfo>,
    loading: Boolean,
    theme: AppThemePalette,
    playing: Boolean,
    currentTrack: TrackInfo?,
    favoriteTracks: List<TrackInfo>,
    onFavToggle: (TrackInfo) -> Unit,
    onClose: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTrackClick: (TrackInfo) -> Unit
) {
    BackHandler { onClose() }
    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = theme.textPrimary)
                }
                Spacer(Modifier.width(16.dp))
                Text(playlist.title, color = theme.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accent)
                }
            } else if (tracks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tracks found.", color = theme.secondary)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = onPlayAll, colors = ButtonDefaults.buttonColors(containerColor = theme.accent), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = theme.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Play All")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = onShuffle, colors = ButtonDefaults.buttonColors(containerColor = theme.cardHover), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, tint = theme.textPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("Shuffle")
                    }
                }
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    lazyListItems(items = tracks, key = { it.id }) { track ->
                        SongItem(
                            song = track,
                            isCurrent = currentTrack?.id == track.id,
                            isPlaying = playing,
                            isFav = favoriteTracks.any { it.id == track.id },
                            onFavToggle = { onFavToggle(track) },
                            onClick = { onTrackClick(track) }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
