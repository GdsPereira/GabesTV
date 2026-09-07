package com.gabestv.iptv.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritesDataStore by preferencesDataStore(name = "gabestv_favorites")

/**
 * Persists favorite channel IDs using Jetpack DataStore Preferences.
 * Each physical device gets its own favorites list keyed by ANDROID_ID,
 * so a TV, phone, and tablet each maintain independent favorites.
 */
@Singleton
class FavoritesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val deviceId: String by lazy {
        Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "default"
    }

    private val favoritesKey get() = stringSetPreferencesKey("favorites_$deviceId")

    /** Reactive stream of favorite channel IDs for this device */
    val favoriteChannelIds: Flow<Set<String>> = context.favoritesDataStore.data
        .map { prefs -> prefs[favoritesKey] ?: emptySet() }

    /** Atomically adds or removes a channel from favorites */
    suspend fun toggleFavorite(channelId: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[favoritesKey] ?: emptySet()
            prefs[favoritesKey] = if (channelId in current) {
                current - channelId
            } else {
                current + channelId
            }
        }
    }

    /** Replaces the entire favorites set */
    suspend fun setFavorites(ids: Set<String>) {
        context.favoritesDataStore.edit { prefs ->
            prefs[favoritesKey] = ids
        }
    }
}
