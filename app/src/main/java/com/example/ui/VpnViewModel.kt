package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.VpnConfig
import com.example.data.VpnDatabase
import com.example.data.VpnLog
import com.example.data.VpnRepository
import com.example.service.MyVpnService
import com.example.service.VpnState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

class VpnViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VpnRepository
    val configs: StateFlow<List<VpnConfig>>
    val activeConfig: StateFlow<VpnConfig?>
    val logs: StateFlow<List<VpnLog>>

    // Observe MyVpnService companion properties directly for real-time reactive UI
    val connectionState = MyVpnService.connectionState
    val downloadSpeed = MyVpnService.downloadSpeed
    val uploadSpeed = MyVpnService.uploadSpeed
    val durationSeconds = MyVpnService.durationSeconds

    // History for graphs
    private val _downloadSpeedHistory = MutableStateFlow<List<Long>>(List(30) { 0L })
    val downloadSpeedHistory: StateFlow<List<Long>> = _downloadSpeedHistory

    private val _uploadSpeedHistory = MutableStateFlow<List<Long>>(List(30) { 0L })
    val uploadSpeedHistory: StateFlow<List<Long>> = _uploadSpeedHistory

    private val _isPingTesting = MutableStateFlow(false)
    val isPingTesting: StateFlow<Boolean> = _isPingTesting

    init {
        val database = VpnDatabase.getDatabase(application)
        repository = VpnRepository(database.vpnDao())
        
        configs = repository.configs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
            
        activeConfig = repository.activeConfig
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
            
        logs = repository.logs
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Periodically record speeds for the traffic graph
        viewModelScope.launch {
            repository.addDefaultConfigsIfEmpty()
            
            downloadSpeed.combine(uploadSpeed) { dl, ul -> Pair(dl, ul) }
                .collectLatest { (dl, ul) ->
                    _downloadSpeedHistory.value = (_downloadSpeedHistory.value.drop(1) + dl)
                    _uploadSpeedHistory.value = (_uploadSpeedHistory.value.drop(1) + ul)
                }
        }
    }

    fun toggleVpnConnection(context: Context, prepareIntentNeeded: () -> Unit) {
        val active = activeConfig.value ?: return
        
        when (connectionState.value) {
            VpnState.DISCONNECTED -> {
                // Check if VPN service prepare is needed
                val prepareIntent = VpnService.prepare(context)
                if (prepareIntent != null) {
                    prepareIntentNeeded()
                } else {
                    startVpnService(context, active)
                }
            }
            VpnState.CONNECTED -> {
                stopVpnService(context)
            }
            else -> {}
        }
    }

    fun startVpnAfterPermissionApproved(context: Context) {
        val active = activeConfig.value ?: return
        startVpnService(context, active)
    }

    private fun startVpnService(context: Context, config: VpnConfig) {
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_CONNECT
            putExtra("server_id", config.id)
            putExtra("server_name", config.name)
        }
        context.startService(intent)
    }

    private fun stopVpnService(context: Context) {
        val intent = Intent(context, MyVpnService::class.java).apply {
            action = MyVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }

    fun selectActiveConfig(configId: Int, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setActiveConfig(configId)
            repository.insertLog("Switched active proxy configuration profile.", "INFO")
            
            // If currently connected, reconnect to the new server automatically
            if (connectionState.value == VpnState.CONNECTED) {
                stopVpnService(context)
                delay(600)
                configs.value.find { it.id == configId }?.let { newConfig ->
                    startVpnService(context, newConfig)
                }
            }
        }
    }

    fun addConfig(name: String, server: String, port: Int, type: String, uuid: String, pass: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val config = VpnConfig(
                name = name,
                server = server,
                port = port,
                type = type,
                uuid = uuid,
                password = pass
            )
            repository.insertConfig(config)
            repository.insertLog("Imported new $type configuration: $name", "INFO")
        }
    }

    fun deleteConfig(config: VpnConfig) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteConfig(config)
            repository.insertLog("Removed configuration profile: ${config.name}", "WARN")
        }
    }

    fun pingAllServers() {
        if (_isPingTesting.value) return
        _isPingTesting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLog("Starting complete server diagnostic ping test...", "INFO")
            val currentConfigs = configs.value
            for (config in currentConfigs) {
                // Simulate latency testing sequentially
                delay(300)
                val testPing = when {
                    config.server.contains("sg-node") -> Random.nextInt(25, 45)
                    config.server.contains("jp-node") -> Random.nextInt(50, 75)
                    config.server.contains("us-node") -> Random.nextInt(130, 160)
                    config.server.contains("de-node") -> Random.nextInt(165, 195)
                    else -> Random.nextInt(150, 220)
                }
                repository.updatePing(config.id, testPing)
                repository.insertLog("Checked latency of [${config.type}] ${config.name}: ${testPing}ms", "DEBUG")
            }
            _isPingTesting.value = false
            repository.insertLog("Server latency diagnostic completed successfully.", "INFO")
        }
    }

    fun clearLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearLogs()
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VpnViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return VpnViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
