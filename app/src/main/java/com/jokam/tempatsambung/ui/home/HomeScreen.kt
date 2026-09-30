package com.jokam.tempatsambung.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.nativead.NativeAd
import com.jokam.tempatsambung.R
import com.jokam.tempatsambung.data.model.Place
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import com.jokam.tempatsambung.ui.components.NativeAdCard
import com.jokam.tempatsambung.ui.components.RewardedAdDialog

import com.jokam.tempatsambung.data.remote.RemoteConstants

fun launchDirectionsIntent(context: Context, lat: Double, lng: Double) {
    val uri = Uri.parse("${RemoteConstants.GOOGLE_MAPS_DIR_BASE_URL}$lat,$lng")
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        `package` = "com.google.android.apps.maps"
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(fallbackIntent)
        } catch (_: Exception) {
            Toast.makeText(context, context.getString(R.string.error_open_map), Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onRequestLocationPermission: () -> Unit,
    onRequestEnableGps: () -> Unit,
    onShowRewardedAd: (onEarned: () -> Unit) -> Unit,
    nativeAds: List<NativeAd>,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favoritesFlow.collectAsState()
    val adsConfig by RemoteConfigManager.adsConfig.collectAsState()
    val context = LocalContext.current

    var pendingPlaceForDirections by remember { mutableStateOf<Place?>(null) }
    var showAdDialog by remember { mutableStateOf(false) }

    if (showAdDialog && pendingPlaceForDirections != null) {
        RewardedAdDialog(
            message = stringResource(R.string.rewarded_route_msg),
            onWatchAd = {
                val target = pendingPlaceForDirections
                pendingPlaceForDirections = null
                if (target != null) {
                    onShowRewardedAd {
                        launchDirectionsIntent(context, target.lat, target.lng)
                    }
                }
            },
            onDismiss = {
                showAdDialog = false
                pendingPlaceForDirections = null
            }
        )
    }

    val onDirectionsClick: (Place) -> Unit = { place ->
        if (adsConfig.isAdsEnabled && adsConfig.isRewardedRouteEnabled) {
            pendingPlaceForDirections = place
            showAdDialog = true
        } else {
            launchDirectionsIntent(context, place.lat, place.lng)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Location Permission & GPS Banners
        when (uiState.locationStatus) {
            is LocationStatus.PermissionNeeded -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.location_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestLocationPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(stringResource(R.string.allow_location))
                        }
                    }
                }
            }
            is LocationStatus.GpsDisabled -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.location_off_rationale),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestEnableGps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(stringResource(R.string.enable_location))
                        }
                    }
                }
            }
            else -> {}
        }

        // City Autocomplete (shown if location is not locked or user wants to search city)
        if (uiState.locationStatus !is LocationStatus.HasLocation) {
            var dropdownExpanded by remember { mutableStateOf(false) }
            val hasSuggestions = uiState.citySuggestions.isNotEmpty()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = {
                        viewModel.onSearchQueryChanged(it)
                        dropdownExpanded = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.search_city_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                if (hasSuggestions && dropdownExpanded) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        uiState.citySuggestions.forEach { suggestion ->
                            DropdownMenuItem(
                                text = { Text(suggestion) },
                                onClick = {
                                    viewModel.selectCitySuggestion(suggestion)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.locationStatus is LocationStatus.CitySelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.active_city_label,
                            (uiState.locationStatus as LocationStatus.CitySelected).cityName
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.clearSelectedCity() }
                    ) {
                        Text(
                            text = stringResource(R.string.clear_city),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }

        // Content Area
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.error_loading_places),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.loadPlaces(forceRefresh = true) }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // Favorites Section at top
                if (uiState.favoritePlaces.isNotEmpty()) {
                    item(key = "header_favorites") {
                        Text(
                            text = stringResource(R.string.favorites_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(
                        items = uiState.favoritePlaces,
                        key = { _, item -> "fav_${item.place.id}" }
                    ) { _, item ->
                        PlaceItemCard(
                            place = item.place,
                            distanceLabel = item.displayLabel,
                            isFavorite = true,
                            onToggleFavorite = { viewModel.toggleFavorite(item.place.id) },
                            onOpenDirections = { onDirectionsClick(item.place) }
                        )
                    }

                    item(key = "divider_favorites") {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // Empty state if both favorites and main are empty
                if (uiState.favoritePlaces.isEmpty() && uiState.mainPlaces.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_places),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Main Places List with Interleaved Native Ads
                // Policy Section 14: first slot after 3rd item (index == 2), then every 8th item, max 3
                val canShowNativeAds = adsConfig.isAdsEnabled && adsConfig.isNativeEnabled && nativeAds.isNotEmpty()

                itemsIndexed(
                    items = uiState.mainPlaces,
                    key = { _, item -> item.place.id }
                ) { index, item ->
                    PlaceItemCard(
                        place = item.place,
                        distanceLabel = item.displayLabel,
                        isFavorite = favorites.contains(item.place.id),
                        onToggleFavorite = { viewModel.toggleFavorite(item.place.id) },
                        onOpenDirections = { onDirectionsClick(item.place) }
                    )

                    // Interleave native ad deterministically
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
