package io.anchorkmp.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal interface LocationTrackerEngine {
    val locationFlow: Flow<AnchorLocation>
    val isTracking: Boolean
    
    suspend fun startTracking(config: AnchorConfig)
    suspend fun stopTracking()
    suspend fun reconfigure(config: AnchorConfig)
    suspend fun requestSingleLocation(config: AnchorConfig): AnchorLocation
}

internal expect fun createTrackerEngine(): LocationTrackerEngine

object Anchor {
    private val configMutex = Mutex()
    internal val _configState = MutableStateFlow<AnchorConfig?>(null)
    internal val engine by lazy { createTrackerEngine() }

    fun init(config: AnchorConfig) {
        if (_configState.value != null) {
            println("Anchor is already initialized. Ignoring new init call.")
            return
        }
        _configState.value = config
    }

    fun init(block: AnchorConfig.Builder.() -> Unit) {
        init(AnchorConfig.build(block))
    }

    val locationFlow: Flow<AnchorLocation>
        get() = engine.locationFlow
        
    val isTracking: Boolean
        get() = engine.isTracking

    suspend fun startTracking() {
        val config = _configState.value ?: throw IllegalStateException("Anchor.init() must be called before startTracking.")
        engine.startTracking(config)
    }

    suspend fun stopTracking() {
        engine.stopTracking()
    }
    
    suspend fun getLocation(): AnchorLocation {
        val config = _configState.value ?: throw IllegalStateException("Anchor.init() must be called before getLocation.")
        return engine.requestSingleLocation(config)
    }

    suspend fun updateConfig(block: AnchorConfig.Builder.() -> Unit) {
        configMutex.withLock {
            val currentConfig = _configState.value 
                ?: throw IllegalStateException("Anchor.init() must be called before updating config.")

            val builder = currentConfig.toBuilder()
            builder.block()
            val newConfig = builder.build()
            _configState.value = newConfig

            engine.reconfigure(newConfig)
        }
    }

    /**
     * Checks if the library has all permissions required by the CURRENT configuration.
     * e.g., if Config has `stopOnTerminate = false`, this checks for BACKGROUND permission.
     */
    val isReady: Boolean 
        get() = checkConfigPermissions()

    /**
     * Non-blocking check of current status.
     */
    fun checkPermission(scope: PermissionScope): PermissionStatus {
        return platformPermissionCheck(scope)
    }

    /**
     * Suspending function that handles the UI flow.
     * On Android: Launches a transparent Activity internally.
     * On iOS: Calls CLLocationManager request methods.
     * 
     * Returns result only after user makes a choice.
     */
    suspend fun requestPermission(scope: PermissionScope): PermissionStatus {
        return platformPermissionRequest(scope)
    }
    
    // Internal helper to match config to scope
    private fun checkConfigPermissions(): Boolean {
        val config = _configState.value ?: return false
        
        // If config implies background usage (e.g. service enabled), check Background
        // Otherwise check Foreground
        // Note: Logic simplified as we assume background tracking by default for this lib
        // but we can check if notification is set for Android which implies foreground service which implies background logic often
        val requiredScope = if (config.android.notification != null) {
            PermissionScope.BACKGROUND
        } else {
            PermissionScope.FOREGROUND
        }
        
        return checkPermission(requiredScope) == PermissionStatus.GRANTED
    }
}