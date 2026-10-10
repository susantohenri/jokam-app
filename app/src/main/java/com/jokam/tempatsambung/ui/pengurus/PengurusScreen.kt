package com.jokam.tempatsambung.ui.pengurus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd
import com.jokam.tempatsambung.R
import com.jokam.tempatsambung.data.model.Pengurus
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import com.jokam.tempatsambung.data.remote.RemoteConstants
import com.jokam.tempatsambung.ui.components.NativeAdCard
import com.jokam.tempatsambung.ui.components.RewardedAdDialog
import com.jokam.tempatsambung.ui.utils.ShareUtils

fun launchWhatsApp(context: Context, phone: String) {
    val cleanDigits = phone.filter { it.isDigit() }
    val formatted = if (cleanDigits.startsWith("0")) "62" + cleanDigits.substring(1) else cleanDigits
    val url = "${RemoteConstants.WHATSAPP_BASE_URL}$formatted"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, context.getString(R.string.error_open_whatsapp), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PengurusScreen(
    viewModel: PengurusViewModel,
    onShowRewardedAd: (onEarned: () -> Unit) -> Unit,
    nativeAds: List<NativeAd>,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val adsConfig by RemoteConfigManager.adsConfig.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var adDialogMessage by remember { mutableStateOf<String?>(null) }

    if (adDialogMessage != null && pendingAction != null) {
        RewardedAdDialog(
            message = adDialogMessage!!,
            onWatchAd = {
                val action = pendingAction
                pendingAction = null
                adDialogMessage = null
                if (action != null) {
                    onShowRewardedAd { action() }
                }
            },
            onDismiss = {
                adDialogMessage = null
                pendingAction = null
            }
        )
    }

    val runWithRewardedGate: (Boolean, String, () -> Unit) -> Unit = { isGateEnabled, dialogMsg, action ->
        if (adsConfig.isAdsEnabled && isGateEnabled) {
            pendingAction = action
            adDialogMessage = dialogMsg
        } else {
            action()
        }
    }

    val onContactClick: (Pengurus) -> Unit = { p ->
        runWithRewardedGate(
            adsConfig.isRewardedContactEnabled,
            context.getString(R.string.rewarded_contact_msg)
        ) {
            launchWhatsApp(context, p.phone)
        }
    }

    val onCopyClick: (Pengurus) -> Unit = { p ->
        runWithRewardedGate(
            adsConfig.isRewardedCopyEnabled,
            context.getString(R.string.rewarded_copy_msg)
        ) {
            ShareUtils.copyToClipboard(
                context = context,
                label = "Phone",
                text = p.phone,
                toastMessage = context.getString(R.string.phone_copied)
            )
        }
    }

    val onShareClick: (Pengurus) -> Unit = { p ->
        runWithRewardedGate(
            adsConfig.isRewardedShareEnabled,
            context.getString(R.string.rewarded_share_msg)
        ) {
            ShareUtils.sharePengurus(context, p)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(R.string.error_loading_pengurus),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.loadData(forceRefresh = true) }) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else {
            val canShowNativeAds = adsConfig.isAdsEnabled && adsConfig.isNativeEnabled && nativeAds.isNotEmpty()

            // Search Bar for Pengurus
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.search_pengurus_placeholder),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.clear_search),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (uiState.pengurusList.isEmpty()) {
                    item(key = "empty_pengurus") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) {
                                    stringResource(R.string.empty_search_pengurus, uiState.searchQuery)
                                } else {
                                    stringResource(R.string.empty_pengurus)
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                itemsIndexed(
                    items = uiState.pengurusList,
                    key = { _, item -> item.id }
                ) { index, item ->
                    PengurusItemCard(
                        pengurus = item,
                        onCopyClick = { onCopyClick(item) },
                        onShareClick = { onShareClick(item) },
                        onMessageClick = { onContactClick(item) }
                    )

                    // Native ad interleaving: slot after 3rd item (index == 2), then every 8th item, max 3
                    val isNativeSlot = (index == 2 || (index > 2 && (index - 2) % 8 == 0))
                    val slotIndex = if (index >= 2) (index - 2) / 8 else -1
                    if (isNativeSlot && canShowNativeAds && slotIndex in 0..2 && slotIndex < nativeAds.size) {
                        NativeAdCard(nativeAd = nativeAds[slotIndex])
                    }
                }
            }
        }
    }
}
