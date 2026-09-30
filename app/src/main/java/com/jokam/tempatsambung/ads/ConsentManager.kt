package com.jokam.tempatsambung.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.jokam.tempatsambung.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ConsentManager(private val context: Context) {
    private val tag = "ConsentManager"
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private val _canRequestAdsState = MutableStateFlow(consentInformation.canRequestAds())
    val canRequestAdsState: StateFlow<Boolean> = _canRequestAdsState.asStateFlow()

    init {
        updatePrivacyOptionsRequirement()
    }

    fun canRequestAds(): Boolean = consentInformation.canRequestAds()

    fun gatherConsent(activity: Activity, onConsentGathered: () -> Unit) {
        val paramsBuilder = ConsentRequestParameters.Builder()

        if (BuildConfig.DEBUG) {
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                // Placeholder for developer test device hashed ID:
                .addTestDeviceHashedId("33BE2250B43518CCDA7DE426D04EE231")
                .build()
            paramsBuilder.setConsentDebugSettings(debugSettings)
        }

        val params = paramsBuilder.build()

        // If ads can already be requested from a previous session, initialize early
        if (consentInformation.canRequestAds()) {
            initializeMobileAdsIfPossible()
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(tag, "Consent form error: ${formError.errorCode}: ${formError.message}")
                    }
                    _canRequestAdsState.value = consentInformation.canRequestAds()
                    updatePrivacyOptionsRequirement()

                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsIfPossible()
                    }
                    onConsentGathered()
                }
            },
            { requestConsentError ->
                Log.w(tag, "Consent info update error: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                _canRequestAdsState.value = consentInformation.canRequestAds()
                updatePrivacyOptionsRequirement()
                if (consentInformation.canRequestAds()) {
                    initializeMobileAdsIfPossible()
                }
                onConsentGathered()
            }
        )
    }

    private fun initializeMobileAdsIfPossible() {
        if (isMobileAdsInitializeCalled.compareAndSet(false, true)) {
            Log.d(tag, "Initializing MobileAds off main thread...")
            CoroutineScope(Dispatchers.IO).launch {
                MobileAds.initialize(context) { status ->
                    Log.d(tag, "MobileAds initialized: $status")
                }
            }
        }
    }

    private fun updatePrivacyOptionsRequirement() {
        val required = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        _isPrivacyOptionsRequired.value = required
    }

    fun showPrivacyOptionsForm(activity: Activity, onDismiss: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(tag, "Error showing privacy options form: ${formError.message}")
            }
            updatePrivacyOptionsRequirement()
            _canRequestAdsState.value = consentInformation.canRequestAds()
            onDismiss()
        }
    }
}
