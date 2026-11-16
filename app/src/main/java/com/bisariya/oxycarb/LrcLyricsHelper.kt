package com.bisariya.oxycarb

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

data class LrcLine(val timeMs: Long, val text: String)

object LrcLyricsHelper {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchLyrics(trackTitle: String, trackArtist: String): List<LrcLine>? = withContext(Dispatchers.IO) {
        try {
            val cleanTitle = trackTitle.replace(Regex("\\[.*?\\]|\\(.*?\\)|(?:official|video|audio|lyric).*?", RegexOption.IGNORE_CASE), "").trim()
            val cleanArtist = trackArtist.replace(Regex("- Topic|VEVO", RegexOption.IGNORE_CASE), "").trim()

            val queryUrl = "https://lrclib.net/api/get?track_name=${URLEncoder.encode(cleanTitle, "UTF-8")}&artist_name=${URLEncoder.encode(cleanArtist, "UTF-8")}"

            val request = Request.Builder()
                .url(queryUrl)
                .header("User-Agent", "Oxycarb/1.1.9 (https://github.com/NAGA-RESEARCH/oxycarb)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val json = response.body?.string() ?: return@withContext null
                val map = gson.fromJson(json, Map::class.java)
                val syncedLyrics = map["syncedLyrics"] as? String
                if (!syncedLyrics.isNullOrEmpty()) {
                    return@withContext parseLrc(syncedLyrics)
                }
            }
        } catch (e: Exception) {
            Log.e("Lyrics", "Failed to fetch lyrics", e)
        }
        return@withContext null
    }

    private fun parseLrc(lrc: String): List<LrcLine> {
        val lines = lrc.lines()
        val result = mutableListOf<LrcLine>()
        val regex = Regex("\\[(\\d+):(\\d+\\.\\d+)\\](.*)")

        for (line in lines) {
            val match = regex.find(line)
            if (match != null) {
                val min = match.groupValues[1].toLong()
                val sec = match.groupValues[2].toDouble()
                val text = match.groupValues[3].trim()
                val timeMs = (min * 60 * 1000) + (sec * 1000).toLong()
                if (text.isNotEmpty()) {
                    result.add(LrcLine(timeMs, text))
                }
            }
        }
        return result.sortedBy { it.timeMs }
    }
}
