package com.jokam.tempatsambung.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.jokam.tempatsambung.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    data object Home : Screen("home", R.string.tab_home, Icons.Default.Home)
    data object Pengurus : Screen("pengurus", R.string.tab_pengurus, Icons.AutoMirrored.Filled.Chat)
    data object Wallpaper : Screen("wallpaper", R.string.tab_wallpaper, Icons.Default.Image)
    data object Settings : Screen("settings", R.string.tab_settings, Icons.Default.Settings)

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Pengurus, Wallpaper, Settings)
    }
}
