package com.bisariya.oxycarb

import android.content.Context
import okhttp3.Cache
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request as OkHttpRequest
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.File
import java.util.concurrent.TimeUnit

class AppDownloader(context: Context) : Downloader() {
    private val client = OkHttpClient.Builder()
        .cache(Cache(File(context.cacheDir, "http_cache"), 100L * 1024L * 1024L))
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    override fun execute(request: Request): Response {
        val builder = OkHttpRequest.Builder()
            .url(request.url())
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())

        request.headers().forEach { (key, values) ->
            if (key.equals("Accept-Encoding", ignoreCase = true)) return@forEach
            values.forEach { value ->
                builder.addHeader(key, value)
            }
        }

        val response = client.newCall(builder.build()).execute()
        val responseBody = response.body?.string() ?: ""
        val responseHeaders = response.headers.toMultimap()

        return Response(response.code, response.message, responseHeaders, responseBody, request.url())
    }
}
