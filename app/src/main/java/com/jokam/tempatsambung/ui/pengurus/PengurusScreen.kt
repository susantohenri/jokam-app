package com.jokam.tempatsambung.ui.pengurus

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd
import com.jokam.tempatsambung.R
import com.jokam.tempatsambung.data.model.Pengurus
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import com.jokam.tempatsambung.ui.components.NativeAdCard
import com.jokam.tempatsambung.ui.components.RewardedAdDialog

fun launchWhatsApp(context: Context, phone: String) {
    val cleanDigits = phone.filter { it.isDigit() }
    val formatted = if (cleanDigits.startsWith("0")) "62" + cleanDigits.substring(1) else cleanDigits
    val url = "https://wa.me/$formatted"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Could not open WhatsApp", Toast.LENGTH_SHORT).show()
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

    var pendingPengurusForContact by remember { mutableStateOf<Pengurus?>(null) }
    var showAdDialog by remember { mutableStateOf(false) }

    if (showAdDialog && pendingPengurusForContact != null) {
        RewardedAdDialog(
            message = stringResource(R.string.rewarded_contact_msg),
            onWatchAd = {
                val target = pendingPengurusForContact
                pendingPengurusForContact = null
                if (target != null) {
                    onShowRewardedAd {
                        launchWhatsApp(context, target.phone)
                    }
                }
            },
            onDismiss = {
                showAdDialog = false
                pendingPengurusForContact = null
            }
        )
    }

    val onContactClick: (Pengurus) -> Unit = { p ->
        if (adsConfig.isAdsEnabled && adsConfig.isRewardedContactEnabled) {
            pendingPengurusForContact = p
            showAdDialog = true
        } else {
            launchWhatsApp(context, p.phone)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.errorMessage != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.error_loading_pengurus),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { viewModel.loadData(forceRefresh = true) }) {
                    Text(stringResource(R.string.retry))
                }
            }
        } else {
            val canShowNativeAds = adsConfig.isAdsEnabled && adsConfig.isNativeEnabled && nativeAds.isNotEmpty()
            var adIndex = 0

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = uiState.pengurusList,
                    key = { _, item -> item.id }
                ) { index, item ->
                    PengurusItemCard(
                        pengurus = item,
                        onContactClick = { onContactClick(item) }
                    )

                    // Native ad interleaving: slot after 3rd item (index == 2), then every 8th item, max 3
                    val isNativeSlot = (index == 2 || (index > 2 && (index - 2) % 8 == 0))
                    if (isNativeSlot && canShowNativeAds && adIndex < 3 && adIndex < nativeAds.size) {
                        NativeAdCard(nativeAd = nativeAds[adIndex])
                        adIndex++
                    }
                }
            }
        }
    }
}
