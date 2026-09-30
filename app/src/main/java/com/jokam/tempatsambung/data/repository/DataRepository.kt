package com.jokam.tempatsambung.data.repository

import android.util.Log
import com.jokam.tempatsambung.data.model.Pengurus
import com.jokam.tempatsambung.data.model.Place
import com.jokam.tempatsambung.data.model.WallpaperItem
import com.jokam.tempatsambung.data.remote.RemoteConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class DataRepository {
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val placesMutex = Mutex()
    private val pengurusMutex = Mutex()
    private val wallpapersMutex = Mutex()

    // In-memory session caches
    private var cachedPlaces: List<Place>? = null
    private var cachedPengurus: List<Pengurus>? = null
    private var cachedWallpapers: List<WallpaperItem>? = null

    suspend fun getPlaces(forceRefresh: Boolean = false): Result<List<Place>> = withContext(Dispatchers.IO) {
        placesMutex.withLock {
            if (!forceRefresh && cachedPlaces != null) {
                return@withContext Result.success(cachedPlaces!!)
            }
            try {
                val request = Request.Builder().url(RemoteConstants.PLACES_URL).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                    val jsonArray = json.parseToJsonElement(body).jsonArray
                    val items = jsonArray.mapNotNull { element ->
                        runCatching { json.decodeFromJsonElement<Place>(element) }.getOrNull()
                    }
                    cachedPlaces = items
                    Result.success(items)
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error fetching places", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getPengurus(forceRefresh: Boolean = false): Result<List<Pengurus>> = withContext(Dispatchers.IO) {
        pengurusMutex.withLock {
            if (!forceRefresh && cachedPengurus != null) {
                return@withContext Result.success(cachedPengurus!!)
            }
            try {
                val request = Request.Builder().url(RemoteConstants.PENGURUS_URL).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                    val jsonArray = json.parseToJsonElement(body).jsonArray
                    val items = jsonArray.mapNotNull { element ->
                        runCatching { json.decodeFromJsonElement<Pengurus>(element) }.getOrNull()
                    }
                    cachedPengurus = items
                    Result.success(items)
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error fetching pengurus", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getWallpapers(forceRefresh: Boolean = false): Result<List<WallpaperItem>> = withContext(Dispatchers.IO) {
        wallpapersMutex.withLock {
            if (!forceRefresh && cachedWallpapers != null) {
                return@withContext Result.success(cachedWallpapers!!)
            }
            try {
                val request = Request.Builder().url(RemoteConstants.WALLPAPERS_URL).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${response.code}"))
                    }
                    val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                    val jsonArray = json.parseToJsonElement(body).jsonArray
                    val items = jsonArray.mapNotNull { element ->
                        runCatching { json.decodeFromJsonElement<WallpaperItem>(element) }.getOrNull()
                    }
                    cachedWallpapers = items
                    Result.success(items)
                }
            } catch (e: Exception) {
                Log.e("DataRepository", "Error fetching wallpapers", e)
                Result.failure(e)
            }
        }
    }
}
