package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "omnibrowser_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val SEARCH_ENGINE_URL = stringPreferencesKey("search_engine_url")
        val HOME_PAGE = stringPreferencesKey("home_page")
        val HTTPS_ONLY = booleanPreferencesKey("https_only")
        val BLOCK_TRACKERS = booleanPreferencesKey("block_trackers")
        val BLOCK_THIRD_PARTY_COOKIES = booleanPreferencesKey("block_third_party_cookies")
        val BLOCK_POPUPS = booleanPreferencesKey("block_popups")
        val ASK_BEFORE_DOWNLOAD = booleanPreferencesKey("ask_before_download")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val DESKTOP_MODE_DEFAULT = booleanPreferencesKey("desktop_mode_default")
        val FONT_SIZE = intPreferencesKey("font_size")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val SAVE_PASSWORDS = booleanPreferencesKey("save_passwords")
        val CLOSE_TABS_ON_EXIT = booleanPreferencesKey("close_tabs_on_exit")
        val EXTENSIONS_ENABLED = booleanPreferencesKey("extensions_enabled")
        val AD_BLOCK_ENABLED = booleanPreferencesKey("ad_block_enabled")
        val SEARCH_SUGGESTIONS = booleanPreferencesKey("search_suggestions")
        val TOTAL_ADS_BLOCKED = intPreferencesKey("total_ads_blocked")
        val TOTAL_TRACKERS_BLOCKED = intPreferencesKey("total_trackers_blocked")
        val TOTAL_POPUPS_BLOCKED = intPreferencesKey("total_popups_blocked")
        val CATALOG_URL = stringPreferencesKey("catalog_url")
        val AUTO_UPDATE_EXTENSIONS = booleanPreferencesKey("auto_update_extensions")
        val LAST_CATALOG_FETCH_EPOCH = longPreferencesKey("last_catalog_fetch_epoch")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val CUSTOM_DNS_URL = stringPreferencesKey("custom_dns_url")
        val RESTORE_TABS_ON_STARTUP = booleanPreferencesKey("restore_tabs_on_startup")
        val HIDE_NAV_BAR = booleanPreferencesKey("hide_nav_bar")
        val LOCK_INCOGNITO_TABS = booleanPreferencesKey("lock_incognito_tabs")
    }

    val lockIncognitoTabs: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[LOCK_INCOGNITO_TABS] ?: false
    }

    suspend fun setLockIncognitoTabs(enabled: Boolean) {
        context.settingsDataStore.edit { it[LOCK_INCOGNITO_TABS] = enabled }
    }

    val hideNavBar: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[HIDE_NAV_BAR] ?: false
    }

    suspend fun setHideNavBar(enabled: Boolean) {
        context.settingsDataStore.edit { it[HIDE_NAV_BAR] = enabled }
    }

    val restoreTabsOnStartup: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[RESTORE_TABS_ON_STARTUP] ?: true
    }

    suspend fun setRestoreTabsOnStartup(enabled: Boolean) {
        context.settingsDataStore.edit { it[RESTORE_TABS_ON_STARTUP] = enabled }
    }

    val searchEngineUrl: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[SEARCH_ENGINE_URL] ?: "https://www.google.com/search?q="
    }

    val darkModeEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DARK_MODE_ENABLED] ?: true
    }

    val customDnsUrl: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[CUSTOM_DNS_URL] ?: ""
    }

    suspend fun customDnsUrl(): String = context.settingsDataStore.data.map { it[CUSTOM_DNS_URL] ?: "" }.first()

    val homePage: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[HOME_PAGE] ?: "omni://home"
    }

    val httpsOnly: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[HTTPS_ONLY] ?: true
    }

    val blockTrackers: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[BLOCK_TRACKERS] ?: true
    }

    val blockThirdPartyCookies: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[BLOCK_THIRD_PARTY_COOKIES] ?: true
    }

    val blockPopups: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[BLOCK_POPUPS] ?: true
    }

    val askBeforeDownload: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[ASK_BEFORE_DOWNLOAD] ?: true
    }

    val javascriptEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[JAVASCRIPT_ENABLED] ?: true
    }

    val desktopModeDefault: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[DESKTOP_MODE_DEFAULT] ?: false
    }

    val fontSize: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[FONT_SIZE] ?: 100
    }

    val darkMode: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: "system"
    }

    suspend fun darkModeMode(): String = context.settingsDataStore.data.map { it[DARK_MODE] ?: "system" }.first()

    val savePasswords: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SAVE_PASSWORDS] ?: false
    }

    val closeTabsOnExit: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[CLOSE_TABS_ON_EXIT] ?: false
    }

    val extensionsEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[EXTENSIONS_ENABLED] ?: true
    }

    val adBlockEnabled: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[AD_BLOCK_ENABLED] ?: true
    }

    val searchSuggestions: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[SEARCH_SUGGESTIONS] ?: true
    }

    val totalAdsBlocked: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[TOTAL_ADS_BLOCKED] ?: 0
    }

    val totalTrackersBlocked: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[TOTAL_TRACKERS_BLOCKED] ?: 0
    }

    val totalPopupsBlocked: Flow<Int> = context.settingsDataStore.data.map { preferences ->
        preferences[TOTAL_POPUPS_BLOCKED] ?: 0
    }

    val catalogUrl: Flow<String> = context.settingsDataStore.data.map { preferences ->
        preferences[CATALOG_URL] ?: "https://omnibrowser.app/extensions/index.json"
    }

    val autoUpdateExtensions: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[AUTO_UPDATE_EXTENSIONS] ?: true
    }

    val lastCatalogFetchEpoch: Flow<Long> = context.settingsDataStore.data.map { preferences ->
        preferences[LAST_CATALOG_FETCH_EPOCH] ?: 0L
    }

    // Setters
    suspend fun setSearchEngineUrl(url: String) {
        context.settingsDataStore.edit { it[SEARCH_ENGINE_URL] = url }
    }

    suspend fun setHomePage(url: String) {
        context.settingsDataStore.edit { it[HOME_PAGE] = url }
    }

    suspend fun setHttpsOnly(enabled: Boolean) {
        context.settingsDataStore.edit { it[HTTPS_ONLY] = enabled }
    }

    suspend fun setBlockTrackers(enabled: Boolean) {
        context.settingsDataStore.edit { it[BLOCK_TRACKERS] = enabled }
    }

    suspend fun setBlockThirdPartyCookies(enabled: Boolean) {
        context.settingsDataStore.edit { it[BLOCK_THIRD_PARTY_COOKIES] = enabled }
    }

    suspend fun setBlockPopups(enabled: Boolean) {
        context.settingsDataStore.edit { it[BLOCK_POPUPS] = enabled }
    }

    suspend fun setAskBeforeDownload(enabled: Boolean) {
        context.settingsDataStore.edit { it[ASK_BEFORE_DOWNLOAD] = enabled }
    }

    suspend fun setJavascriptEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[JAVASCRIPT_ENABLED] = enabled }
    }

    suspend fun setDesktopModeDefault(enabled: Boolean) {
        context.settingsDataStore.edit { it[DESKTOP_MODE_DEFAULT] = enabled }
    }

    suspend fun setFontSize(size: Int) {
        context.settingsDataStore.edit { it[FONT_SIZE] = size }
    }

    suspend fun setDarkMode(mode: String) {
        context.settingsDataStore.edit { it[DARK_MODE] = mode }
    }

    suspend fun setSavePasswords(enabled: Boolean) {
        context.settingsDataStore.edit { it[SAVE_PASSWORDS] = enabled }
    }

    suspend fun setCloseTabsOnExit(enabled: Boolean) {
        context.settingsDataStore.edit { it[CLOSE_TABS_ON_EXIT] = enabled }
    }

    suspend fun setExtensionsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[EXTENSIONS_ENABLED] = enabled }
    }

    suspend fun setAdBlockEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[AD_BLOCK_ENABLED] = enabled }
    }

    suspend fun setSearchSuggestions(enabled: Boolean) {
        context.settingsDataStore.edit { it[SEARCH_SUGGESTIONS] = enabled }
    }

    suspend fun incrementAdsBlocked(count: Int = 1) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[TOTAL_ADS_BLOCKED] ?: 0
            preferences[TOTAL_ADS_BLOCKED] = current + count
        }
    }

    suspend fun incrementTrackersBlocked(count: Int = 1) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[TOTAL_TRACKERS_BLOCKED] ?: 0
            preferences[TOTAL_TRACKERS_BLOCKED] = current + count
        }
    }

    suspend fun incrementPopupsBlocked(count: Int = 1) {
        context.settingsDataStore.edit { preferences ->
            val current = preferences[TOTAL_POPUPS_BLOCKED] ?: 0
            preferences[TOTAL_POPUPS_BLOCKED] = current + count
        }
    }

    suspend fun setCatalogUrl(url: String) {
        context.settingsDataStore.edit { it[CATALOG_URL] = url }
    }

    suspend fun setAutoUpdateExtensions(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_UPDATE_EXTENSIONS] = enabled }
    }

    suspend fun setLastCatalogFetchEpoch(epoch: Long) {
        context.settingsDataStore.edit { it[LAST_CATALOG_FETCH_EPOCH] = epoch }
    }

    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[DARK_MODE_ENABLED] = enabled }
    }

    suspend fun setCustomDnsUrl(url: String) {
        context.settingsDataStore.edit { it[CUSTOM_DNS_URL] = url }
    }
}
