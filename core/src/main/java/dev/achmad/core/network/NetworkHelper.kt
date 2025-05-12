package dev.achmad.core.network

import android.annotation.SuppressLint
import android.content.Context
import dev.achmad.core.network.interceptor.IgnoreGzipInterceptor
import dev.achmad.core.network.interceptor.UncaughtExceptionInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.brotli.BrotliInterceptor
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

@SuppressLint("MissingPermission")
class NetworkHelper(
    private val context: Context,
    val isDebugBuild: Boolean,
) {

    val client: OkHttpClient = run {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .cache(
                Cache(
                    directory = File(context.cacheDir, "network_cache"),
                    maxSize = 5L * 1024 * 1024, // 5 MiB
                ),
            )
            .addInterceptor(UncaughtExceptionInterceptor())
            .addNetworkInterceptor(IgnoreGzipInterceptor())
            .addNetworkInterceptor(BrotliInterceptor)

        if (isDebugBuild) {
            val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addNetworkInterceptor(httpLoggingInterceptor)
        }

        builder.build()
    }

}
