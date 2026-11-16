package com.bisariya.oxycarb

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisariya.oxycarb.db.AppDatabase
import com.bisariya.oxycarb.db.PlaylistEntity
import com.bisariya.oxycarb.db.PlaylistTrackCrossRef
import com.bisariya.oxycarb.db.TrackHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.ServiceList
import java.net.UnknownHostException

enum class PlaybackMode {
    RADIO_AUTOPLAY,
    PLAYLIST_LOOP
}

data class GenrePlaylist(
    val id: String,
    val title: String,
    val query: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.historyDao()
    private val playlistDao = db.playlistDao()

    val recentSongs = dao.getRecentSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val recentPodcasts = dao.getRecentPodcasts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val customPlaylists = playlistDao.getAllPlaylists().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val playbackMode = MutableStateFlow(PlaybackMode.RADIO_AUTOPLAY)
    val playlistQueue = MutableStateFlow<List<TrackInfo>>(emptyList())

    val homeSuggestedSongs = MutableStateFlow<List<TrackInfo>>(emptyList())
    val homeLoading = MutableStateFlow(false)
    val homeLoadingMore = MutableStateFlow(false)

    val recentSearches = MutableStateFlow(listOf("Arijit Singh", "Lofi Beats", "Punjabi Party"))

    val searchResultsSongs = MutableStateFlow<List<TrackInfo>>(emptyList())
    val searchResultsPodcasts = MutableStateFlow<List<TrackInfo>>(emptyList())

    val currentQuery = MutableStateFlow("")
    val searchLoading = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)

    val currentTrack = MutableStateFlow<TrackInfo?>(null)

    val genrePlaylists = listOf(
        GenrePlaylist("1", "Lofi & Late Night Chill", "Lofi chill beats"),
        GenrePlaylist("2", "Trending Bollywood / Hindi Hits", "Trending Bollywood Hits 2025"),
        GenrePlaylist("3", "Workout & Phonk Energy", "Workout Phonk Gym Motivation"),
        GenrePlaylist("4", "90s Retro Melodies", "90s Bollywood Retro Songs"),
        GenrePlaylist("5", "Acoustic & Indie Unplugged", "Acoustic Indie Unplugged Hindi"),
        GenrePlaylist("6", "Deep Focus & Coding Beats", "Deep Focus Coding Beats Cyberpunk")
    )

    val selectedPlaylist = MutableStateFlow<GenrePlaylist?>(null)
    val selectedPlaylistTracks = MutableStateFlow<List<TrackInfo>>(emptyList())
    val playlistLoading = MutableStateFlow(false)

    fun triggerAutoBackup(context: Context, favorites: List<TrackInfo>) {
        val prefs = context.getSharedPreferences("oxycarb_settings", Context.MODE_PRIVATE)
        val isBackupEnabled = prefs.getBoolean("backup_enabled", false)
        if (!isBackupEnabled) return

        viewModelScope.launch(Dispatchers.IO) {
            val playlists = customPlaylists.value
            val playlistBackups = playlists.map { p ->
                val tracks = playlistDao.getTracksForPlaylistSync(p.playlistId).map { it.toTrackInfo() }
                PlaylistBackupModel(name = p.name, tracks = tracks)
            }
            BackupVaultHelper.saveBackupToDocuments(context, favorites, playlistBackups)
        }
    }

    fun performManualBackup(context: Context, favorites: List<TrackInfo>, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = customPlaylists.value
            val playlistBackups = playlists.map { p ->
                val tracks = playlistDao.getTracksForPlaylistSync(p.playlistId).map { it.toTrackInfo() }
                PlaylistBackupModel(name = p.name, tracks = tracks)
            }
            val success = BackupVaultHelper.saveBackupToDocuments(context, favorites, playlistBackups)
            viewModelScope.launch(Dispatchers.Main) {
                onComplete(success)
            }
        }
    }

    fun restoreBackup(context: Context, onResult: (Boolean, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val backup = BackupVaultHelper.readBackupFromDocuments(context)
            if (backup != null) {
                var restoredCount = 0

                if (backup.favorites.isNotEmpty()) {
                    val currentFavs = FavManager.getFavs(context).toMutableList()
                    val existingIds = currentFavs.map { it.id }.toSet()
                    val newFavs = backup.favorites.filter { it.id !in existingIds }
                    for (f in newFavs) {
                        FavManager.toggleFav(context, f)
                        restoredCount++
                    }
                }

                for (pBackup in backup.playlists) {
                    val pId = playlistDao.createPlaylist(PlaylistEntity(name = pBackup.name))
                    for (track in pBackup.tracks) {
                        playlistDao.addTrackToPlaylist(
                            PlaylistTrackCrossRef(
                                playlistId = pId,
                                trackId = track.id,
                                title = track.title,
                                artist = track.artist,
                                durationMs = track.durationMs
                            )
                        )
                        restoredCount++
                    }
                }

                viewModelScope.launch(Dispatchers.Main) {
                    onResult(true, restoredCount)
                }
            } else {
                viewModelScope.launch(Dispatchers.Main) {
                    onResult(false, 0)
                }
            }
        }
    }

    fun addRecentSearch(q: String) {
        if (q.isBlank()) return
        val current = recentSearches.value.toMutableList()
        current.remove(q)
        current.add(0, q)
        recentSearches.value = current.take(5)
    }

    fun removeRecentSearch(q: String) {
        val current = recentSearches.value.toMutableList()
        current.remove(q)
        recentSearches.value = current
    }

    fun playGenreCardRadio(playlist: GenrePlaylist, onTrackReady: (TrackInfo, List<TrackInfo>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getSearchExtractor(playlist.query)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                val results = items.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }
                if (results.isNotEmpty()) {
                    val seed = results.first()
                    val related = YoutubeExtractor.getRelatedTracks(seed.id)
                    viewModelScope.launch(Dispatchers.Main) {
                        onTrackReady(seed, related)
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun createCustomPlaylist(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.createPlaylist(PlaylistEntity(name = name))
        }
    }

    fun deleteCustomPlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.deletePlaylist(playlistId)
        }
    }

    fun addTrackToCustomPlaylist(playlistId: Long, track: TrackInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.addTrackToPlaylist(
                PlaylistTrackCrossRef(
                    playlistId = playlistId,
                    trackId = track.id,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs
                )
            )
        }
    }

    fun removeTrackFromCustomPlaylist(playlistId: Long, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    fun getTracksForCustomPlaylist(playlistId: Long) = playlistDao.getTracksForPlaylist(playlistId)

    fun loadPlaylist(playlist: GenrePlaylist) {
        selectedPlaylist.value = playlist
        selectedPlaylistTracks.value = emptyList()
        playlistLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getSearchExtractor(playlist.query)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                val results = items.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }
                selectedPlaylistTracks.value = results
            } catch (e: Exception) {
                // Ignore
            } finally {
                playlistLoading.value = false
            }
        }
    }

    fun closePlaylist() {
        selectedPlaylist.value = null
    }

    private val homeQueryPool = listOf(
        "Trending Hindi Songs",
        "Arijit Singh Latest Hits",
        "Bollywood Romantic Songs",
        "Punjabi Party Hits",
        "Lofi Chill Beats Hindi",
        "Coke Studio All Time Hits",
        "Top Global Hits",
        "90s Bollywood Melodies",
        "Indie Pop India",
        "Shreya Ghoshal Best",
        "Bollywood Dance Party Songs",
        "Romantic Acoustic Hindi"
    )

    fun loadHomeFeed(isRefresh: Boolean = false) {
        if (isRefresh) {
            homeLoading.value = true
        } else if (homeSuggestedSongs.value.isEmpty()) {
            homeLoading.value = true
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query = homeQueryPool.random()
                val extractor = ServiceList.YouTube.getSearchExtractor(query)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                val results = items.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }

                if (isRefresh || homeSuggestedSongs.value.isEmpty()) {
                    homeSuggestedSongs.value = results.shuffled().distinctBy { it.id }
                } else {
                    val existingIds = homeSuggestedSongs.value.map { it.id }.toSet()
                    val newUnique = results.filter { it.id !in existingIds }
                    homeSuggestedSongs.value = homeSuggestedSongs.value + newUnique
                }
            } catch (e: Exception) {
                // Ignore silent load errors
            } finally {
                homeLoading.value = false
            }
        }
    }

    fun loadHomeFeedByVibe(query: String) {
        homeLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getSearchExtractor(query)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                val results = items.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }
                homeSuggestedSongs.value = results.shuffled().distinctBy { it.id }
            } catch (e: Exception) {
                // Ignore
            } finally {
                homeLoading.value = false
            }
        }
    }

    fun loadMoreHomeFeed() {
        if (homeLoading.value || homeLoadingMore.value) return
        homeLoadingMore.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query = homeQueryPool.random()
                val extractor = ServiceList.YouTube.getSearchExtractor(query)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                val results = items.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }

                val existingIds = homeSuggestedSongs.value.map { it.id }.toSet()
                val newUnique = results.filter { it.id !in existingIds }
                homeSuggestedSongs.value = homeSuggestedSongs.value + newUnique
            } catch (e: Exception) {
                // Ignore
            } finally {
                homeLoadingMore.value = false
            }
        }
    }

    fun search(query: String) {
        currentQuery.value = query
        if (query.isBlank()) {
            searchResultsSongs.value = emptyList()
            searchResultsPodcasts.value = emptyList()
            searchLoading.value = false
            searchError.value = null
            return
        }

        addRecentSearch(query)

        searchLoading.value = true
        searchError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val extractor = ServiceList.YouTube.getSearchExtractor(query)
                extractor.fetchPage()
                val items = extractor.initialPage.items.filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                val results = items.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }

                val songs = results.filter { it.durationMs < 1800000L } // Less than 30 mins
                val podcasts = results.filter { it.durationMs >= 1800000L }

                searchResultsSongs.value = songs
                searchResultsPodcasts.value = podcasts
            } catch (e: UnknownHostException) {
                searchError.value = "Internet connection error. Please check your network and try again."
            } catch (e: Exception) {
                searchError.value = "Failed to load results. Please try again."
            } finally {
                searchLoading.value = false
            }
        }
    }

    fun retrySearch() {
        if (currentQuery.value.isNotBlank()) {
            search(currentQuery.value)
        }
    }

    fun addToHistory(track: TrackInfo) {
        viewModelScope.launch {
            val isPodcast = track.durationMs >= 1800000L
            dao.insertTrack(TrackHistoryEntity(track.id, track.title, track.artist, track.durationMs, isPodcast))
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            dao.clearAllHistory()
        }
    }

    init {
        loadHomeFeed(isRefresh = false)
        viewModelScope.launch {
            // Delete history older than 24 hours
            dao.deleteOldHistory(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
        }
    }
}
