package com.jokam.tempatsambung.ui.wallpaper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jokam.tempatsambung.R
import com.jokam.tempatsambung.data.model.WallpaperItem
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import com.jokam.tempatsambung.ui.components.RewardedAdDialog
import kotlinx.coroutines.launch

@Composable
fun WallpaperScreen(
    viewModel: WallpaperViewModel,
    onShowRewardedAd: (onEarned: () -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val adsConfig by RemoteConfigManager.adsConfig.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showAdDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val selected = uiState.selectedWallpaper
            if (selected != null) {
                viewModel.saveToGallery(
                    context, selected.fullUrl, selected.id,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_save_success))
                        }
                    },
                    onError = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_save_failed))
                        }
                    }
                )
            }
        } else {
            Toast.makeText(context, context.getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show()
        }
    }

    if (showAdDialog && pendingAction != null) {
        RewardedAdDialog(
            message = stringResource(R.string.rewarded_wallpaper_msg),
            onWatchAd = {
                val action = pendingAction
                pendingAction = null
                if (action != null) {
                    onShowRewardedAd { action() }
                }
            },
            onDismiss = {
                showAdDialog = false
                pendingAction = null
            }
        )
    }

    val runWithRewardedGate: (() -> Unit) -> Unit = { action ->
        if (adsConfig.isAdsEnabled && adsConfig.isRewardedWallpaperEnabled) {
            pendingAction = action
            showAdDialog = true
        } else {
            action()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.errorMessage != null) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.error_loading_wallpapers),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadWallpapers(forceRefresh = true) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.wallpapers, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(9f / 16f)
                                .clickable { viewModel.selectWallpaper(item) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(item.thumbUrl)
                                    .crossfade(true)
                                    .placeholder(R.drawable.bg_native_ad)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            // Fullscreen Preview Overlay
            val selected = uiState.selectedWallpaper
            if (selected != null) {
                BackHandler { viewModel.selectWallpaper(null) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.95f))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selected.fullUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Close Button
                    IconButton(
                        onClick = { viewModel.selectWallpaper(null) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.close),
                            tint = Color.White
                        )
                    }

                    // Progress indicator if downloading/saving
                    if (uiState.isActionInProgress) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Bottom action buttons: [Set as wallpaper] & [Save]
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                runWithRewardedGate {
                                    viewModel.setWallpaper(
                                        context, selected.fullUrl,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_set_success))
                                            }
                                        },
                                        onError = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_set_failed))
                                            }
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            enabled = !uiState.isActionInProgress
                        ) {
                            Icon(Icons.Default.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.set_wallpaper))
                        }

                        Button(
                            onClick = {
                                runWithRewardedGate {
                                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            viewModel.saveToGallery(
                                                context, selected.fullUrl, selected.id,
                                                onSuccess = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_save_success))
                                                    }
                                                },
                                                onError = {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_save_failed))
                                                    }
                                                }
                                            )
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                        }
                                    } else {
                                        viewModel.saveToGallery(
                                            context, selected.fullUrl, selected.id,
                                            onSuccess = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_save_success))
                                                }
                                            },
                                            onError = {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar(context.getString(R.string.wallpaper_save_failed))
                                                }
                                            }
                                        )
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            enabled = !uiState.isActionInProgress
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.save_wallpaper))
                        }
                    }
                }
            }
        }
    }
}
