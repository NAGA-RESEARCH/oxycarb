package com.bisariya.oxycarb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.StreamExtractor
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.concurrent.ConcurrentHashMap

class DownloaderImpl private constructor() : Downloader() {
    private val client = okhttp3.OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
            .header("Accept-Language", "en-US,en;q=0.9")

        for ((headerName, headerValueList) in headers) {
            if (headerValueList != null && headerValueList.isNotEmpty()) {
                builder.header(headerName, headerValueList[0])
            }
        }

        if (httpMethod.equals("POST", ignoreCase = true)) {
            val body = okhttp3.RequestBody.create(null, dataToSend ?: ByteArray(0))
            builder.post(body)
        } else {
            builder.get()
        }

        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string() ?: ""
        val responseHeaders = mutableMapOf<String, List<String>>()
        for (name in response.headers.names()) {
            responseHeaders[name] = response.headers.values(name)
        }

        return Response(response.code, response.message, responseHeaders, responseBody, request.url())
    }

    companion object {
        private var instance: DownloaderImpl? = null
        fun getInstance(): DownloaderImpl {
            if (instance == null) {
                instance = DownloaderImpl()
            }
            return instance!!
        }
    }
}

object YoutubeExtractor {
    private data class CachedStream(
        val url: String,
        val timestamp: Long
    )

    private val cacheTtlMs = 2 * 60 * 60 * 1000L // 2-Hour TTL
    private val streamUrlCache = ConcurrentHashMap<String, CachedStream>()

    init {
        try {
            NewPipe.init(DownloaderImpl.getInstance())
        } catch (e: Exception) {
            Log.e("OxycarbPlayer", "Failed to initialize NewPipe DownloaderImpl", e)
        }
    }

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

    suspend fun getAudioUrl(videoId: String, quality: String = "Balanced (96kbps)"): String? = withContext(Dispatchers.IO) {
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
                val sortedStreams = audioStreams.sortedWith(
                    compareByDescending<org.schabi.newpipe.extractor.stream.AudioStream> {
                        val formatName = it.format?.name?.lowercase() ?: ""
                        when {
                            it.itag == 140 -> 4
                            formatName.contains("m4a") || formatName.contains("mp4a") -> 3
                            formatName.contains("aac") -> 2
                            formatName.contains("opus") || formatName.contains("webm") -> 1
                            else -> 0
                        }
                    }.thenByDescending {
                        when {
                            quality.contains("Data Saver") -> -it.averageBitrate
                            quality.contains("High Quality") -> it.averageBitrate
                            else -> -kotlin.math.abs(it.averageBitrate - 96)
                        }
                    }
                )

                var selectedUrl: String? = null
                for (stream in sortedStreams) {
                    if (!stream.content.isNullOrBlank()) {
                        selectedUrl = stream.content
                        break
                    }
                }

                if (selectedUrl == null) {
                    for (stream in audioStreams) {
                        if (!stream.content.isNullOrBlank()) {
                            selectedUrl = stream.content
                            break
                        }
                    }
                }

                if (selectedUrl != null) {
                    streamUrlCache[cacheKey] = CachedStream(selectedUrl, System.currentTimeMillis())
                    Log.d("OxycarbPlayer", "Successfully extracted audio stream URL for $videoId")
                } else {
                    Log.e("OxycarbPlayer", "No valid stream URL content found for $videoId")
                }
                selectedUrl
            } else {
                Log.e("OxycarbPlayer", "audioStreams list is empty or null for $videoId")
                null
            }
        } catch (e: Exception) {
            Log.e("OxycarbPlayer", "NewPipeExtractor failed for videoId: $videoId", e)
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
            Log.e("OxycarbPlayer", "Failed to get related tracks for videoId: $videoId", e)
            emptyList()
        }
    }
}
