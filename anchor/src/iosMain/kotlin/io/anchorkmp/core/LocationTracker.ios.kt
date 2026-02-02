package io.anchorkmp.core

import platform.CoreLocation.*
import platform.CoreMotion.*
import platform.Foundation.*
import platform.darwin.NSObject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.cinterop.useContents
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.concurrent.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal actual fun createTrackerEngine(): LocationTrackerEngine = IosLocationTrackerEngine()

@OptIn(ExperimentalForeignApi::class)
internal class IosLocationTrackerEngine : LocationTrackerEngine {
    private val locationManager = CLLocationManager()
    private val motionActivityManager = CMMotionActivityManager()
    private val delegate = LocationDelegate()
    
    private val _locationFlow = MutableSharedFlow<AnchorLocation>(replay = 1)
    override val locationFlow: Flow<AnchorLocation> = _locationFlow
    
    private var _isTracking = false
    override val isTracking: Boolean get() = _isTracking
    
    private val currentActivity = AtomicReference<AnchorActivityType?>(null)
    private val operationQueue = NSOperationQueue()

    init {
        locationManager.delegate = delegate
    }

    override suspend fun startTracking(config: AnchorConfig) {
        applyConfig(config)
        
        if (!_isTracking) {
             _isTracking = true
             locationManager.startUpdatingLocation()
        }
    }
    
    private fun applyConfig(config: AnchorConfig) {
        locationManager.desiredAccuracy = config.ios.desiredAccuracy.value
        
        locationManager.distanceFilter = if (config.minUpdateDistanceMeters == 0.0) kCLDistanceFilterNone else config.minUpdateDistanceMeters
        
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.showsBackgroundLocationIndicator = config.ios.displayBackgroundLocationIndicator
        locationManager.pausesLocationUpdatesAutomatically = config.ios.autoPause
        locationManager.activityType = when(config.ios.activityType) {
            IosActivityType.OTHER -> CLActivityTypeOther
            IosActivityType.AUTOMOTIVE_NAVIGATION -> CLActivityTypeAutomotiveNavigation
            IosActivityType.FITNESS -> CLActivityTypeFitness
            IosActivityType.OTHER_NAVIGATION -> CLActivityTypeOtherNavigation
            IosActivityType.AIRBORNE -> CLActivityTypeAirborne
        }
        
        if (config.trackActivity && CMMotionActivityManager.isActivityAvailable()) {
            motionActivityManager.startActivityUpdatesToQueue(operationQueue) { activity ->
                activity?.let {
                    currentActivity.value = mapActivity(it)
                }
            }
        } else {
            motionActivityManager.stopActivityUpdates()
            currentActivity.value = null
        }
    }

    override suspend fun stopTracking() {
        locationManager.stopUpdatingLocation()
        motionActivityManager.stopActivityUpdates()
        _isTracking = false
    }
    
    override suspend fun reconfigure(config: AnchorConfig) {
        applyConfig(config)
    }
    
    override suspend fun requestSingleLocation(config: AnchorConfig): AnchorLocation {
        if (isTracking) {
            return locationFlow.first()
        }
        
        return suspendCoroutine { cont ->
            val oneShotManager = CLLocationManager()
            oneShotManager.desiredAccuracy = config.ios.desiredAccuracy.value
            
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                    val locations = didUpdateLocations as List<CLLocation>
                    locations.lastOrNull()?.let { iosLocation ->
                        manager.delegate = null
                        OneShotRequestHolder.remove(this)
                        cont.resume(
                            AnchorLocation(
                                latitude = iosLocation.coordinate.useContents { latitude },
                                longitude = iosLocation.coordinate.useContents { longitude },
                                altitude = iosLocation.altitude,
                                accuracy = iosLocation.horizontalAccuracy.toFloat(),
                                speed = iosLocation.speed.toFloat(),
                                bearing = iosLocation.course.toFloat(),
                                timestamp = (iosLocation.timestamp.timeIntervalSince1970 * 1000).toLong(),
                                activity = currentActivity.value
                            )
                        )
                    }
                }
                
                override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                    manager.delegate = null
                    OneShotRequestHolder.remove(this)
                    cont.resumeWithException(RuntimeException("Location request failed: ${didFailWithError.localizedDescription}"))
                }
            }
            
            OneShotRequestHolder.add(delegate)
            oneShotManager.delegate = delegate
            oneShotManager.requestLocation()
        }
    }
    
    private fun mapActivity(activity: CMMotionActivity): AnchorActivityType {
        return when {
            activity.automotive -> AnchorActivityType.AUTOMOTIVE
            activity.cycling -> AnchorActivityType.CYCLING
            activity.running -> AnchorActivityType.RUNNING
            activity.walking -> AnchorActivityType.WALKING
            activity.stationary -> AnchorActivityType.STATIONARY
            else -> AnchorActivityType.UNKNOWN
        }
    }
    
    private inner class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val locations = didUpdateLocations as List<CLLocation>
            locations.lastOrNull()?.let { iosLocation ->
                _locationFlow.tryEmit(
                    AnchorLocation(
                        latitude = iosLocation.coordinate.useContents { latitude },
                        longitude = iosLocation.coordinate.useContents { longitude },
                        altitude = iosLocation.altitude,
                        accuracy = iosLocation.horizontalAccuracy.toFloat(),
                        speed = iosLocation.speed.toFloat(),
                        bearing = iosLocation.course.toFloat(),
                        timestamp = (iosLocation.timestamp.timeIntervalSince1970 * 1000).toLong(),
                        activity = currentActivity.value
                    )
                )
            }
        }
        
        override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
            // Handle error
        }
    }
}

private object OneShotRequestHolder {
    private val delegates = mutableSetOf<Any>()
    
    fun add(delegate: Any) {
        delegates.add(delegate)
    }
    
    fun remove(delegate: Any) {
        delegates.remove(delegate)
    }
}