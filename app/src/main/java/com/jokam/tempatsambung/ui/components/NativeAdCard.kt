package com.jokam.tempatsambung.ui.components

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.jokam.tempatsambung.R

@Composable
fun NativeAdCard(
    nativeAd: NativeAd,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context).inflate(R.layout.layout_native_ad, null, false) as NativeAdView

                val headlineView = view.findViewById<TextView>(R.id.ad_headline)
                val bodyView = view.findViewById<TextView>(R.id.ad_body)
                val iconView = view.findViewById<ImageView>(R.id.ad_app_icon)
                val mediaView = view.findViewById<MediaView>(R.id.ad_media)
                val callToActionView = view.findViewById<Button>(R.id.ad_call_to_action)

                view.headlineView = headlineView
                view.bodyView = bodyView
                view.iconView = iconView
                view.mediaView = mediaView
                view.callToActionView = callToActionView

                headlineView.text = nativeAd.headline ?: ""

                if (nativeAd.body == null) {
                    bodyView.visibility = View.GONE
                } else {
                    bodyView.visibility = View.VISIBLE
                    bodyView.text = nativeAd.body
                }

                if (nativeAd.icon == null) {
                    iconView.visibility = View.GONE
                } else {
                    iconView.visibility = View.VISIBLE
                    iconView.setImageDrawable(nativeAd.icon?.drawable)
                }

                if (nativeAd.mediaContent != null && nativeAd.mediaContent?.hasVideoContent() == true) {
                    mediaView.visibility = View.VISIBLE
                    mediaView.mediaContent = nativeAd.mediaContent
                } else {
                    mediaView.visibility = View.GONE
                }

                if (nativeAd.callToAction == null) {
                    callToActionView.visibility = View.GONE
                } else {
                    callToActionView.visibility = View.VISIBLE
                    callToActionView.text = nativeAd.callToAction
                }

                view.setNativeAd(nativeAd)
                view
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
