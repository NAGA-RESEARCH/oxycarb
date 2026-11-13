package com.bisariya.oxycarb

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisariya.oxycarb.db.AppDatabase
import com.bisariya.oxycarb.db.TrackHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.ServiceList
import java.net.UnknownHostException

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val dao = db.historyDao()

    val recentSongs = dao.getRecentSongs().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
    val recentPodcasts = dao.getRecentPodcasts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val homeSuggestedSongs = MutableStateFlow<List<TrackInfo>>(emptyList())
    val homeLoading = MutableStateFlow(false)
    val homeLoadingMore = MutableStateFlow(false)

    val searchResultsSongs = MutableStateFlow<List<TrackInfo>>(emptyList())
    val searchResultsPodcasts = MutableStateFlow<List<TrackInfo>>(emptyList())

    val currentQuery = MutableStateFlow("")
    val searchLoading = MutableStateFlow(false)
    val searchError = MutableStateFlow<String?>(null)

    val currentTrack = MutableStateFlow<TrackInfo?>(null)

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
