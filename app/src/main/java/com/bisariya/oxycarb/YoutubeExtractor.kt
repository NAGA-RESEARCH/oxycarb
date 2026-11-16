package com.bisariya.oxycarb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.ConcurrentHashMap

object YoutubeExtractor {
    private data class CachedStream(
        val url: String,
        val timestamp: Long
    )

    private val cacheTtlMs = 2 * 60 * 60 * 1000L // 2-Hour TTL
    private val streamUrlCache = ConcurrentHashMap<String, CachedStream>()

    fun getCachedUrl(videoId: String, quality: String): String? {
        val cacheKey = "${videoId}_$quality"
        val cached = streamUrlCache[cacheKey]
        if (cached != null) {
            if (System.currentTimeMillis() - cached.timestamp < cacheTtlMs) {
                return cached.url
            } else {
                streamUrlCache.remove(cacheKey)
            }
        }
        return null
    }

    suspend fun getAudioUrl(videoId: String, quality: String = "Balanced"): String? = withContext(Dispatchers.IO) {
        val cacheKey = "${videoId}_$quality"
        val cachedUrl = getCachedUrl(videoId, quality)
        if (cachedUrl != null) {
            return@withContext cachedUrl
        }

        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val selected = when (quality) {
                    "Data Saver (48kbps)" -> audioStreams.minByOrNull { it.averageBitrate }
                    "High Quality (128kbps+)" -> audioStreams.maxByOrNull { it.averageBitrate }
                    else -> {
                        // Balanced (96kbps) or default
                        audioStreams.minByOrNull { kotlin.math.abs(it.averageBitrate - 96) }
                            ?: audioStreams.maxByOrNull { it.averageBitrate }
                    }
                }

                val selectedUrl = selected?.content ?: audioStreams.firstOrNull()?.content
                if (selectedUrl != null) {
                    streamUrlCache[cacheKey] = CachedStream(selectedUrl, System.currentTimeMillis())
                }
                selectedUrl
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("YoutubeExtractor", "NewPipeExtractor failed for $videoId", e)
            null
        }
    }

    suspend fun getRelatedTracks(videoId: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()

            val relatedItems = extractor.relatedItems?.items?.filterIsInstance<StreamInfoItem>() ?: emptyList()
            relatedItems.map { TrackInfo(it.url.substringAfter("v="), it.name, it.uploaderName, it.duration * 1000L) }
        } catch (e: Exception) {
            Log.e("YoutubeExtractor", "Failed to get related for $videoId", e)
            emptyList()
        }
    }
}
