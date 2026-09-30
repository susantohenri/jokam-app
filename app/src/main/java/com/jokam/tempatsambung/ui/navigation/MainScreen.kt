package com.jokam.tempatsambung.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.google.android.gms.ads.nativead.NativeAd
import com.jokam.tempatsambung.R
import com.jokam.tempatsambung.ads.ConsentManager
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import com.jokam.tempatsambung.data.repository.PreferencesRepository
import com.jokam.tempatsambung.ui.components.BannerAdSection
import com.jokam.tempatsambung.ui.home.HomeScreen
import com.jokam.tempatsambung.ui.home.HomeViewModel
import com.jokam.tempatsambung.ui.pengurus.PengurusScreen
import com.jokam.tempatsambung.ui.pengurus.PengurusViewModel
import com.jokam.tempatsambung.ui.settings.SettingsScreen
import com.jokam.tempatsambung.ui.wallpaper.WallpaperScreen
import com.jokam.tempatsambung.ui.wallpaper.WallpaperViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    pengurusViewModel: PengurusViewModel,
    wallpaperViewModel: WallpaperViewModel,
    preferencesRepository: PreferencesRepository,
    consentManager: ConsentManager,
    onRequestLocationPermission: () -> Unit,
    onRequestEnableGps: () -> Unit,
    onShowRewardedAd: (onEarned: () -> Unit) -> Unit,
    nativeAds: List<NativeAd>
) {
    var selectedScreen by rememberSaveable { mutableStateOf(Screen.Home.route) }
    val adsConfig by RemoteConfigManager.adsConfig.collectAsState()
    val canRequestAds by consentManager.canRequestAdsState.collectAsState()

    val showBanner = adsConfig.isAdsEnabled &&
            adsConfig.isBannerEnabled &&
            canRequestAds &&
            selectedScreen != Screen.Settings.route &&
            !adsConfig.bannerAdUnitId.isNullOrEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Anchored Adaptive Banner Ad directly above BottomNavigation
                if (showBanner) {
                    BannerAdSection(adUnitId = adsConfig.bannerAdUnitId!!)
                }

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = selectedScreen == screen.route,
                            onClick = { selectedScreen = screen.route },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedScreen) {
                Screen.Home.route -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onRequestLocationPermission = onRequestLocationPermission,
                        onRequestEnableGps = onRequestEnableGps,
                        onShowRewardedAd = onShowRewardedAd,
                        nativeAds = nativeAds
                    )
                }
                Screen.Pengurus.route -> {
                    PengurusScreen(
                        viewModel = pengurusViewModel,
                        onShowRewardedAd = onShowRewardedAd,
                        nativeAds = nativeAds
                    )
                }
                Screen.Wallpaper.route -> {
                    WallpaperScreen(
                        viewModel = wallpaperViewModel,
                        onShowRewardedAd = onShowRewardedAd
                    )
                }
                Screen.Settings.route -> {
                    SettingsScreen(
                        preferencesRepository = preferencesRepository,
                        consentManager = consentManager
                    )
                }
            }
        }
    }
}
