package com.bisariya.oxycarb

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class TrackInfo(val id: String, val title: String, val artist: String, val durationMs: Long)

object YoutubeApi {
    suspend fun searchMusic(query: String): List<TrackInfo> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<TrackInfo>()
        try {
            val url = URL("https://music.youtube.com/youtubei/v1/search")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("context", JSONObject().apply {
                    put("client", JSONObject().apply {
                        put("clientName", "WEB_REMIX")
                        put("clientVersion", "1.20231214.00.00")
                    })
                })
                put("query", query)
            }

            conn.outputStream.use { it.write(payload.toString().toByteArray()) }

            if (conn.responseCode == 200) {
                val response = conn.inputStream.bufferedReader().readText()
                val json = JSONObject(response)

                // Parse tracks
                val contents = json.optJSONObject("contents")
                    ?.optJSONObject("tabbedSearchResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")

                if (contents != null) {
                    for (i in 0 until contents.length()) {
                        val obj = contents.optJSONObject(i) ?: continue
                        val items = obj.optJSONObject("itemSectionRenderer")?.optJSONArray("contents")
                            ?: obj.optJSONObject("musicShelfRenderer")?.optJSONArray("contents")
                            ?: obj.optJSONObject("musicCardShelfRenderer")?.optJSONArray("contents")
                            ?: continue

                        for (j in 0 until items.length()) {
                            val item = items.optJSONObject(j)?.optJSONObject("musicResponsiveListItemRenderer") ?: continue
                            val videoId = item.optJSONObject("playlistItemData")?.optString("videoId") ?: continue
                            val flexColumns = item.optJSONArray("flexColumns") ?: continue

                            val titleRun = flexColumns.optJSONObject(0)
                                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")
                                ?.optJSONArray("runs")
                                ?.optJSONObject(0)
                            val title = titleRun?.optString("text") ?: "Unknown Title"

                            var artist = "Unknown Artist"
                            var durationStr = "0:00"
                            val metaRuns = flexColumns.optJSONObject(1)
                                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                                ?.optJSONObject("text")
                                ?.optJSONArray("runs")

                            if (metaRuns != null) {
                                val parts = mutableListOf<String>()
                                for (k in 0 until metaRuns.length()) {
                                    val text = metaRuns.optJSONObject(k)?.optString("text") ?: ""
                                    if (text.isNotBlank() && text != " • ") parts.add(text)
                                }
                                if (parts.size >= 1) artist = parts[0]
                                if (parts.size >= 3) durationStr = parts.last()
                            }

                            val durParts = durationStr.split(":")
                            val durationMs = if (durParts.size == 2) {
                                (durParts[0].toLongOrNull() ?: 0) * 60000 + (durParts[1].toLongOrNull() ?: 0) * 1000
                            } else if (durParts.size == 3) {
                                (durParts[0].toLongOrNull() ?: 0) * 3600000 + (durParts[1].toLongOrNull() ?: 0) * 60000 + (durParts[2].toLongOrNull() ?: 0) * 1000
                            } else 0L

                            tracks.add(TrackInfo(videoId, title, artist, durationMs))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("YoutubeApi", "Search failed", e)
        }
        tracks
    }
}
