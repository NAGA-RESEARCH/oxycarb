package com.bisariya.oxycarb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object YoutubeExtractor {
    suspend fun getAudioUrl(videoId: String, dataSaver: Boolean = false): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor: StreamExtractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (!audioStreams.isNullOrEmpty()) {
                val selected = if (dataSaver) {
                    audioStreams.minByOrNull { it.averageBitrate }
                } else {
                    audioStreams.maxByOrNull { it.averageBitrate }
                }
                selected?.content
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
