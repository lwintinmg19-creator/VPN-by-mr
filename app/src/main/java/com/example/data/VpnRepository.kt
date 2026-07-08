package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class VpnRepository(private val vpnDao: VpnDao) {

    val configs: Flow<List<VpnConfig>> = vpnDao.getAllConfigs()
    val activeConfig: Flow<VpnConfig?> = vpnDao.getActiveConfig()
    val logs: Flow<List<VpnLog>> = vpnDao.getRecentLogs()

    suspend fun insertConfig(config: VpnConfig) {
        vpnDao.insertConfig(config)
    }

    suspend fun updateConfig(config: VpnConfig) {
        vpnDao.updateConfig(config)
    }

    suspend fun setActiveConfig(configId: Int) {
        vpnDao.setActiveConfig(configId)
    }

    suspend fun updatePing(configId: Int, ping: Int) {
        vpnDao.updatePing(configId, ping)
    }

    suspend fun deleteConfig(config: VpnConfig) {
        vpnDao.deleteConfig(config)
    }

    suspend fun insertLog(message: String, level: String = "INFO") {
        vpnDao.insertLog(VpnLog(message = message, level = level))
    }

    suspend fun clearLogs() {
        vpnDao.clearLogs()
    }

    suspend fun addDefaultConfigsIfEmpty() {
        val currentConfigs = vpnDao.getAllConfigs().firstOrNull()
        if (currentConfigs.isNullOrEmpty()) {
            val defaults = listOf(
                VpnConfig(
                    name = "🇸🇬 Singapore Premium-VLESS [Fastest]",
                    server = "sg-node.pyaephyohanvpn.net",
                    port = 443,
                    type = "VLESS",
                    uuid = "7a412b18-f213-4a1e-821f-bc87e07a3bf2",
                    isActive = true,
                    ping = 32
                ),
                VpnConfig(
                    name = "🇺🇸 US West Trojan-HighSpeed",
                    server = "us-node.pyaephyohanvpn.net",
                    port = 443,
                    type = "Trojan",
                    password = "pyaephyohanpass",
                    isActive = false,
                    ping = 142
                ),
                VpnConfig(
                    name = "🇯🇵 Tokyo Hysteria2-Gaming",
                    server = "jp-node.pyaephyohanvpn.net",
                    port = 8443,
                    type = "Hysteria2",
                    password = "gaming_pyaephyohan",
                    isActive = false,
                    ping = 58
                ),
                VpnConfig(
                    name = "🇩🇪 Germany Shadowsocks-Secure",
                    server = "de-node.pyaephyohanvpn.net",
                    port = 8388,
                    type = "Shadowsocks",
                    password = "chacha20-ietf-poly1305:pyaephyohankey",
                    isActive = false,
                    ping = 175
                ),
                VpnConfig(
                    name = "🇬🇧 London TUIC-Bypass",
                    server = "uk-node.pyaephyohanvpn.net",
                    port = 10443,
                    type = "TUIC",
                    uuid = "8f31b619-a102-4dbe-a82f-2bc30a7b9ef8",
                    isActive = false,
                    ping = 160
                )
            )
            for (config in defaults) {
                vpnDao.insertConfig(config)
            }
            vpnDao.insertLog(VpnLog(message = "Initialized default secure proxy configurations.", level = "INFO"))
        }
    }
}
