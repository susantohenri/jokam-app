package com.jokam.tempatsambung

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.jokam.tempatsambung.ads.AdsManager
import com.jokam.tempatsambung.ads.ConsentManager
import com.jokam.tempatsambung.data.remote.RemoteConfigManager
import com.jokam.tempatsambung.data.repository.DataRepository
import com.jokam.tempatsambung.data.repository.PreferencesRepository
import com.jokam.tempatsambung.ui.home.HomeViewModel
import com.jokam.tempatsambung.ui.home.LocationStatus
import com.jokam.tempatsambung.ui.navigation.MainScreen
import com.jokam.tempatsambung.ui.pengurus.PengurusViewModel
import com.jokam.tempatsambung.ui.theme.TempatSambungTheme
import com.jokam.tempatsambung.ui.theme.ThemeMode
import com.jokam.tempatsambung.ui.wallpaper.WallpaperViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var consentManager: ConsentManager
    private lateinit var adsManager: AdsManager
    private lateinit var dataRepository: DataRepository
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var homeViewModel: HomeViewModel
    private lateinit var pengurusViewModel: PengurusViewModel
    private lateinit var wallpaperViewModel: WallpaperViewModel

    // Permission launcher for location
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            checkGpsAndRequestLocation()
        } else {
            homeViewModel.onLocationPermissionDenied()
        }
    }

    // ResolvableApiException GPS dialog launcher
    private val gpsResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            fetchDeviceLocation()
        } else {
            homeViewModel.onGpsDisabled()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize dependencies (single module, no DI)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        consentManager = ConsentManager(this)
        adsManager = AdsManager(this, consentManager)
        dataRepository = DataRepository()
        preferencesRepository = PreferencesRepository(this)

        homeViewModel = HomeViewModel(dataRepository, preferencesRepository)
        pengurusViewModel = PengurusViewModel(dataRepository)
        wallpaperViewModel = WallpaperViewModel(dataRepository)

        // Request remote config & UMP Consent
        lifecycleScope.launch {
            RemoteConfigManager.fetchAdsConfig()
            consentManager.gatherConsent(this@MainActivity) {
                adsManager.preloadRewardedAd()
                adsManager.loadNativeAds(3)
            }
        }

        // Check initial location state
        checkLocationPermission()

        setContent {
            val themeMode by preferencesRepository.themeModeFlow.collectAsState(initial = ThemeMode.SYSTEM)
            val nativeAds by adsManager.nativeAds.collectAsState()

            TempatSambungTheme(themeMode = themeMode) {
                MainScreen(
                    homeViewModel = homeViewModel,
                    pengurusViewModel = pengurusViewModel,
                    wallpaperViewModel = wallpaperViewModel,
                    preferencesRepository = preferencesRepository,
                    consentManager = consentManager,
                    onRequestLocationPermission = {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        )
                    },
                    onRequestEnableGps = {
                        checkGpsAndRequestLocation()
                    },
                    onShowRewardedAd = { onEarned ->
                        adsManager.showRewardedAd(this@MainActivity, onEarned)
                    },
                    nativeAds = nativeAds
                )
            }
        }
    }

    private fun checkLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            checkGpsAndRequestLocation()
        } else {
            homeViewModel.onLocationPermissionDenied()
        }
    }

    private fun checkGpsAndRequestLocation() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000
        ).build()

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .build()

        val settingsClient = LocationServices.getSettingsClient(this)
        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener {
                fetchDeviceLocation()
            }
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        gpsResolutionLauncher.launch(intentSenderRequest)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.e(tag, "Failed to start resolution for GPS", sendEx)
                        homeViewModel.onGpsDisabled()
                    }
                } else {
                    homeViewModel.onGpsDisabled()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun fetchDeviceLocation() {
        // Fallback timer if GPS takes too long (~8s)
        lifecycleScope.launch {
            delay(8000)
            if (homeViewModel.uiState.value.locationStatus is LocationStatus.Checking) {
                homeViewModel.onLocationUnavailable()
                pengurusViewModel.updateLocationStatus(LocationStatus.LocationUnavailable)
            }
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    homeViewModel.onLocationReceived(location)
                    pengurusViewModel.updateLocationStatus(
                        LocationStatus.HasLocation(location.latitude, location.longitude)
                    )
                } else {
                    // Try lastLocation if currentLocation was null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            homeViewModel.onLocationReceived(lastLoc)
                            pengurusViewModel.updateLocationStatus(
                                LocationStatus.HasLocation(lastLoc.latitude, lastLoc.longitude)
                            )
                        } else {
                            homeViewModel.onLocationUnavailable()
                            pengurusViewModel.updateLocationStatus(LocationStatus.LocationUnavailable)
                        }
                    }.addOnFailureListener {
                        homeViewModel.onLocationUnavailable()
                        pengurusViewModel.updateLocationStatus(LocationStatus.LocationUnavailable)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w(tag, "Could not obtain device location", e)
                homeViewModel.onLocationUnavailable()
                pengurusViewModel.updateLocationStatus(LocationStatus.LocationUnavailable)
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        adsManager.destroyNativeAds()
    }
}
