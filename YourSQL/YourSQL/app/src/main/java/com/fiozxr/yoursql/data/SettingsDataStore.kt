package com.fiozxr.yoursql.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yoursql_settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // Server settings
    val serverPort: Flow<Int> = dataStore.data.map { it[SERVER_PORT] ?: 5432 }
    val httpsEnabled: Flow<Boolean> = dataStore.data.map { it[HTTPS_ENABLED] ?: false }
    val defaultRateLimit: Flow<Int> = dataStore.data.map { it[DEFAULT_RATE_LIMIT] ?: 100 }

    // Security settings
    val ipAllowlist: Flow<List<String>> = dataStore.data.map {
        it[IP_ALLOWLIST]?.split(",")?.filter { ip -> ip.isNotBlank() } ?: emptyList()
    }

    // Backup settings
    val backupEnabled: Flow<Boolean> = dataStore.data.map { it[BACKUP_ENABLED] ?: false }
    val backupFrequency: Flow<String> = dataStore.data.map { it[BACKUP_FREQUENCY] ?: "daily" }
    val backupDestination: Flow<String> = dataStore.data.map { it[BACKUP_DESTINATION] ?: "local" }

    // Tunnel settings
    val cloudflareToken: Flow<String?> = dataStore.data.map { it[CLOUDFLARE_TOKEN] }
    val ngrokToken: Flow<String?> = dataStore.data.map { it[NGROK_TOKEN] }
    val tunnelActive: Flow<Boolean> = dataStore.data.map { it[TUNNEL_ACTIVE] ?: false }

    // Storage settings
    val storageQuota: Flow<Long> = dataStore.data.map { it[STORAGE_QUOTA] ?: (1L * 1024 * 1024 * 1024) } // 1GB

    // Theme settings
    val theme: Flow<String> = dataStore.data.map { it[THEME] ?: "system" }

    suspend fun setServerPort(port: Int) {
        dataStore.edit { it[SERVER_PORT] = port }
    }

    suspend fun setHttpsEnabled(enabled: Boolean) {
        dataStore.edit { it[HTTPS_ENABLED] = enabled }
    }

    suspend fun setDefaultRateLimit(limit: Int) {
        dataStore.edit { it[DEFAULT_RATE_LIMIT] = limit }
    }

    suspend fun setIpAllowlist(ips: List<String>) {
        dataStore.edit { it[IP_ALLOWLIST] = ips.joinToString(",") }
    }

    suspend fun setBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupFrequency(frequency: String) {
        dataStore.edit { it[BACKUP_FREQUENCY] = frequency }
    }

    suspend fun setBackupDestination(destination: String) {
        dataStore.edit { it[BACKUP_DESTINATION] = destination }
    }

    suspend fun setCloudflareToken(token: String?) {
        dataStore.edit {
            if (token != null) it[CLOUDFLARE_TOKEN] = token
            else it.remove(CLOUDFLARE_TOKEN)
        }
    }

    suspend fun setNgrokToken(token: String?) {
        dataStore.edit {
            if (token != null) it[NGROK_TOKEN] = token
            else it.remove(NGROK_TOKEN)
        }
    }

    suspend fun setTunnelActive(active: Boolean) {
        dataStore.edit { it[TUNNEL_ACTIVE] = active }
    }

    suspend fun setStorageQuota(quota: Long) {
        dataStore.edit { it[STORAGE_QUOTA] = quota }
    }

    suspend fun setTheme(themeValue: String) {
        dataStore.edit { it[THEME] = themeValue }
    }

    companion object {
        private val SERVER_PORT = intPreferencesKey("server_port")
        private val HTTPS_ENABLED = booleanPreferencesKey("https_enabled")
        private val DEFAULT_RATE_LIMIT = intPreferencesKey("default_rate_limit")
        private val IP_ALLOWLIST = stringPreferencesKey("ip_allowlist")
        private val BACKUP_ENABLED = booleanPreferencesKey("backup_enabled")
        private val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        private val BACKUP_DESTINATION = stringPreferencesKey("backup_destination")
        private val CLOUDFLARE_TOKEN = stringPreferencesKey("cloudflare_token")
        private val NGROK_TOKEN = stringPreferencesKey("ngrok_token")
        private val TUNNEL_ACTIVE = booleanPreferencesKey("tunnel_active")
        private val STORAGE_QUOTA = longPreferencesKey("storage_quota")
        private val THEME = stringPreferencesKey("theme")
    }
}
