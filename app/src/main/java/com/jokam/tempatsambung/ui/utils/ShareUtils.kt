package com.jokam.tempatsambung.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.jokam.tempatsambung.R
import com.jokam.tempatsambung.data.model.Pengurus
import com.jokam.tempatsambung.data.model.Place
import com.jokam.tempatsambung.data.remote.RemoteConstants

object ShareUtils {
    fun copyToClipboard(context: Context, label: String, text: String, toastMessage: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    fun sharePlace(context: Context, place: Place) {
        val directionsUrl = "${RemoteConstants.GOOGLE_MAPS_DIR_BASE_URL}${place.lat},${place.lng}"
        val text = buildString {
            appendLine(place.name)
            appendLine(place.address)
            appendLine()
            append("Rute: $directionsUrl")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, place.name)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_place_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun sharePengurus(context: Context, pengurus: Pengurus) {
        val cleanDigits = pengurus.phone.filter { it.isDigit() }
        val formatted = if (cleanDigits.startsWith("0")) "62" + cleanDigits.substring(1) else cleanDigits
        val displayPhone = if (pengurus.phone.startsWith("62")) "+${pengurus.phone}" else pengurus.phone
        val waUrl = "${RemoteConstants.WHATSAPP_BASE_URL}$formatted"

        val text = buildString {
            appendLine(context.getString(R.string.share_pengurus_heading))
            appendLine("${pengurus.city}, ${pengurus.province}")
            appendLine("No. Telp: $displayPhone")
            append("WhatsApp: $waUrl")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${pengurus.city}, ${pengurus.province}")
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, context.getString(R.string.share_pengurus_title))
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
