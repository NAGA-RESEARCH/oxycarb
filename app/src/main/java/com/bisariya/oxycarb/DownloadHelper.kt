package com.bisariya.oxycarb

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

sealed class DownloadStatus {
    object Idle : DownloadStatus()
    data class Downloading(val progress: Float) : DownloadStatus()
    object Downloaded : DownloadStatus()
    object Failed : DownloadStatus()
}

object DownloadHelper {
    private const val PREF_NAME = "oxycarb_downloads"
    private const val KEY_TRACKS = "downloaded_tracks"
    private val gson = Gson()
    private val client = OkHttpClient()

    val downloadStateMap = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())

    fun getDownloadedTracks(context: Context): List<TrackInfo> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TRACKS, "[]") ?: "[]"
        val type = object : TypeToken<List<TrackInfo>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun isDownloaded(context: Context, trackId: String): Boolean {
        val file = File(context.filesDir, "downloads/$trackId.m4a")
        return file.exists() && file.length() > 0
    }

    fun getLocalUri(context: Context, trackId: String): String? {
        val file = File(context.filesDir, "downloads/$trackId.m4a")
        return if (file.exists() && file.length() > 0) {
            file.toURI().toString()
        } else {
            null
        }
    }

    suspend fun downloadTrack(
        context: Context,
        track: TrackInfo,
        audioQuality: String,
        onProgress: (Float) -> Unit,
        onComplete: (Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            updateStatus(track.id, DownloadStatus.Downloading(0f))
            val streamUrl = YoutubeExtractor.getAudioUrl(track.id, audioQuality)
            if (streamUrl == null) {
                updateStatus(track.id, DownloadStatus.Failed)
                withContext(Dispatchers.Main) { onComplete(false) }
                return@withContext
            }

            val request = Request.Builder().url(streamUrl).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                updateStatus(track.id, DownloadStatus.Failed)
                withContext(Dispatchers.Main) { onComplete(false) }
                return@withContext
            }

            val downloadsDir = File(context.filesDir, "downloads")
            if (!downloadsDir.exists()) downloadsDir.mkdirs()

            val targetFile = File(downloadsDir, "${track.id}.m4a")
            val body = response.body ?: run {
                updateStatus(track.id, DownloadStatus.Failed)
                withContext(Dispatchers.Main) { onComplete(false) }
                return@withContext
            }

            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (totalBytes > 0) {
                            val progress = downloadedBytes.toFloat() / totalBytes.toFloat()
                            updateStatus(track.id, DownloadStatus.Downloading(progress))
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            val currentDownloads = getDownloadedTracks(context).toMutableList()
            if (currentDownloads.none { it.id == track.id }) {
                currentDownloads.add(track)
                val json = gson.toJson(currentDownloads)
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_TRACKS, json)
                    .apply()
            }

            updateStatus(track.id, DownloadStatus.Downloaded)
            withContext(Dispatchers.Main) { onComplete(true) }
        } catch (e: Exception) {
            Log.e("DownloadHelper", "Download failed for ${track.id}", e)
            updateStatus(track.id, DownloadStatus.Failed)
            withContext(Dispatchers.Main) { onComplete(false) }
        }
    }

    private fun updateStatus(trackId: String, status: DownloadStatus) {
        val current = downloadStateMap.value.toMutableMap()
        current[trackId] = status
        downloadStateMap.value = current
    }

    fun deleteDownload(context: Context, trackId: String) {
        val file = File(context.filesDir, "downloads/$trackId.m4a")
        if (file.exists()) file.delete()

        val currentDownloads = getDownloadedTracks(context).toMutableList()
        currentDownloads.removeAll { it.id == trackId }
        val json = gson.toJson(currentDownloads)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRACKS, json)
            .apply()

        updateStatus(trackId, DownloadStatus.Idle)
    }
}
