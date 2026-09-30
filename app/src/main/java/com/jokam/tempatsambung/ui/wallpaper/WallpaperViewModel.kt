package com.jokam.tempatsambung.ui.wallpaper

import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jokam.tempatsambung.data.model.WallpaperItem
import com.jokam.tempatsambung.data.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

data class WallpaperUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val wallpapers: List<WallpaperItem> = emptyList(),
    val selectedWallpaper: WallpaperItem? = null,
    val isActionInProgress: Boolean = false,
    val snackbarMessage: String? = null
)

class WallpaperViewModel(
    private val dataRepository: DataRepository
) : ViewModel() {

    private val httpClient = OkHttpClient()

    private val _uiState = MutableStateFlow(WallpaperUiState(isLoading = true))
    val uiState: StateFlow<WallpaperUiState> = _uiState.asStateFlow()

    init {
        loadWallpapers()
    }

    fun loadWallpapers(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val result = dataRepository.getWallpapers(forceRefresh)
            result.onSuccess { items ->
                _uiState.value = _uiState.value.copy(isLoading = false, wallpapers = items)
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = err.localizedMessage)
            }
        }
    }

    fun selectWallpaper(wallpaper: WallpaperItem?) {
        _uiState.value = _uiState.value.copy(selectedWallpaper = wallpaper)
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url).build()
            httpClient.newCall(req).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.e("WallpaperViewModel", "Error downloading wallpaper bitmap", e)
            null
        }
    }

    fun setWallpaper(context: Context, fullUrl: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionInProgress = true)
            val bitmap = downloadBitmap(fullUrl)
            if (bitmap != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val wm = WallpaperManager.getInstance(context)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                        } else {
                            wm.setBitmap(bitmap)
                        }
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(isActionInProgress = false)
                            onSuccess()
                        }
                    } catch (e: Exception) {
                        Log.e("WallpaperViewModel", "Error applying wallpaper", e)
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(isActionInProgress = false)
                            onError()
                        }
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                onError()
            }
        }
    }

    fun saveToGallery(context: Context, fullUrl: String, filename: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionInProgress = true)
            val bitmap = downloadBitmap(fullUrl)
            if (bitmap != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val saved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val resolver = context.contentResolver
                            val contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.jpg")
                                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Tempat Sambung")
                            }
                            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                            if (uri != null) {
                                resolver.openOutputStream(uri)?.use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                true
                            } else false
                        } else {
                            val dir = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                "Tempat Sambung"
                            )
                            if (!dir.exists()) dir.mkdirs()
                            val file = File(dir, "$filename.jpg")
                            FileOutputStream(file).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                            }
                            true
                        }

                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(isActionInProgress = false)
                            if (saved) onSuccess() else onError()
                        }
                    } catch (e: Exception) {
                        Log.e("WallpaperViewModel", "Error saving wallpaper", e)
                        withContext(Dispatchers.Main) {
                            _uiState.value = _uiState.value.copy(isActionInProgress = false)
                            onError()
                        }
                    }
                }
            } else {
                _uiState.value = _uiState.value.copy(isActionInProgress = false)
                onError()
            }
        }
    }
}
