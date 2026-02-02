package io.anchorkmp.core

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume

internal actual fun createTrackerEngine(): LocationTrackerEngine = AndroidLocationTrackerEngine()

internal class AndroidLocationTrackerEngine : LocationTrackerEngine {
    private val _isTracking = MutableStateFlow(false)
    override val isTracking: Boolean get() = _isTracking.value
    
    private val context by lazy { AnchorContext.getContext() }
    
    private val locationManager by lazy { 
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager 
    }
    
    private val activityRecognitionClient by lazy {
        ActivityRecognition.getClient(context)
    }
    
    private val _locationFlow = MutableSharedFlow<AnchorLocation>(replay = 1)
    override val locationFlow: Flow<AnchorLocation> = _locationFlow
    
    private val currentActivity = AtomicReference<AnchorActivityType?>(null)
    
    private val activityReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && ActivityRecognitionResult.hasResult(intent)) {
                val result = ActivityRecognitionResult.extractResult(intent)
                result?.mostProbableActivity?.let { detectedActivity ->
                    currentActivity.set(mapActivity(detectedActivity))
                }
            }
        }
    }
    
    private var activityPendingIntent: PendingIntent? = null

    internal fun reportLocation(location: AnchorLocation) {
        _locationFlow.tryEmit(location)
    }

    internal fun getCurrentActivity(): AnchorActivityType? = currentActivity.get()

    @SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
    override suspend fun startTracking(config: AnchorConfig) {
        if (config.trackActivity) {
            startActivityUpdates(config)
        }
        
        val intent = Intent(context, AnchorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        _isTracking.value = true
    }
    
    @SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
    private fun startActivityUpdates(config: AnchorConfig) {
        stopActivityUpdates()
        
        val intent = Intent("io.anchorkmp.core.ACTION_ACTIVITY_UPDATE")
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        activityPendingIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(activityReceiver, IntentFilter("io.anchorkmp.core.ACTION_ACTIVITY_UPDATE"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(activityReceiver, IntentFilter("io.anchorkmp.core.ACTION_ACTIVITY_UPDATE"))
        }
        
        activityRecognitionClient.requestActivityUpdates(
            config.android.updateInterval.inWholeMilliseconds,
            activityPendingIntent!!
        )
    }
    
    private fun stopActivityUpdates() {
        activityPendingIntent?.let {
            activityRecognitionClient.removeActivityUpdates(it)
            try {
                context.unregisterReceiver(activityReceiver)
            } catch (e: IllegalArgumentException) { }
            activityPendingIntent = null
        }
    }

    override suspend fun stopTracking() {
        stopActivityUpdates()
        val intent = Intent(context, AnchorService::class.java)
        context.stopService(intent)
        _isTracking.value = false
    }

    override suspend fun reconfigure(config: AnchorConfig) {
        if (_isTracking.value) {
            // Restarting service to apply new config
            startTracking(config)
        }
    }
    
    @SuppressLint("MissingPermission")
    override suspend fun requestSingleLocation(config: AnchorConfig): AnchorLocation {
        if (isTracking) {
            return locationFlow.first()
        }
        
        return suspendCancellableCoroutine { cont ->
            val provider = when(config.android.priority) {
                AndroidPriority.HIGH_ACCURACY -> LocationManager.GPS_PROVIDER
                AndroidPriority.BALANCED -> LocationManager.NETWORK_PROVIDER
                AndroidPriority.LOW_POWER -> LocationManager.PASSIVE_PROVIDER
                AndroidPriority.PASSIVE -> LocationManager.PASSIVE_PROVIDER
            }
            
            val listener = object : LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    locationManager.removeUpdates(this)
                    if (cont.isActive) {
                        cont.resume(
                            AnchorLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                altitude = if (location.hasAltitude()) location.altitude else null,
                                accuracy = if (location.hasAccuracy()) location.accuracy else null,
                                speed = if (location.hasSpeed()) location.speed else null,
                                bearing = if (location.hasBearing()) location.bearing else null,
                                timestamp = location.time,
                                activity = if (config.trackActivity) currentActivity.get() else null
                            )
                        )
                    }
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            
            cont.invokeOnCancellation {
                locationManager.removeUpdates(listener)
            }
        }
    }
    
    private fun mapActivity(detected: DetectedActivity): AnchorActivityType {
        return when(detected.type) {
            DetectedActivity.IN_VEHICLE -> AnchorActivityType.AUTOMOTIVE
            DetectedActivity.ON_BICYCLE -> AnchorActivityType.CYCLING
            DetectedActivity.ON_FOOT, DetectedActivity.WALKING -> AnchorActivityType.WALKING
            DetectedActivity.RUNNING -> AnchorActivityType.RUNNING
            DetectedActivity.STILL -> AnchorActivityType.STATIONARY
            else -> AnchorActivityType.UNKNOWN
        }
    }
}