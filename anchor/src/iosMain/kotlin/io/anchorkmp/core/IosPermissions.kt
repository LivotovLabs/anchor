package io.anchorkmp.core

import platform.CoreLocation.*
import platform.CoreMotion.*
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.ExperimentalForeignApi

internal actual fun platformPermissionCheck(scope: PermissionScope): PermissionStatus {
    if (scope == PermissionScope.MOTION) {
        val status = CMMotionActivityManager.authorizationStatus()
        return mapMotionStatus(status)
    }
    
    val status = CLLocationManager.authorizationStatus()
    return mapIosStatus(status, scope)
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun platformPermissionRequest(scope: PermissionScope): PermissionStatus = suspendCoroutine { cont ->
    val currentStatus = platformPermissionCheck(scope)
    if (currentStatus == PermissionStatus.GRANTED) {
        cont.resume(PermissionStatus.GRANTED)
        return@suspendCoroutine
    }
    
    if (scope == PermissionScope.MOTION) {
        val manager = CMMotionActivityManager()
        val now = NSDate()
        manager.queryActivityStartingFromDate(now, now, NSOperationQueue.mainQueue) { _, error ->
            // This callback is invoked after user decision (or immediately if already decided)
            // Error code 105 (CMErrorMotionActivityNotAuthorized) indicates denial
            val newStatus = CMMotionActivityManager.authorizationStatus()
            cont.resume(mapMotionStatus(newStatus))
        }
        return@suspendCoroutine
    }
    
    val locationManager = CLLocationManager()
    
    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didChangeAuthorizationStatus: CLAuthorizationStatus) {
            if (didChangeAuthorizationStatus == kCLAuthorizationStatusNotDetermined) return
            
            // Capture result
            val result = mapIosStatus(didChangeAuthorizationStatus, scope)
            
            // Clean up
            manager.delegate = null
            PermissionRequestHolder.activeDelegate = null // Release strong ref
            
            cont.resume(result)
        }
    }
    
    // Hold strong reference to delegate
    PermissionRequestHolder.activeDelegate = delegate
    locationManager.delegate = delegate

    if (scope == PermissionScope.BACKGROUND) {
        locationManager.requestAlwaysAuthorization()
    } else {
        locationManager.requestWhenInUseAuthorization()
    }
}

private object PermissionRequestHolder {
    var activeDelegate: Any? = null
}

private fun mapIosStatus(status: CLAuthorizationStatus, scope: PermissionScope): PermissionStatus {
    return when (status) {
        kCLAuthorizationStatusAuthorizedAlways -> PermissionStatus.GRANTED
        kCLAuthorizationStatusAuthorizedWhenInUse -> {
            if (scope == PermissionScope.BACKGROUND) PermissionStatus.DENIED else PermissionStatus.GRANTED
        }
        kCLAuthorizationStatusDenied -> PermissionStatus.PERMANENTLY_DENIED
        kCLAuthorizationStatusRestricted -> PermissionStatus.PERMANENTLY_DENIED
        else -> PermissionStatus.NOT_DETERMINED
    }
}

private fun mapMotionStatus(status: CMAuthorizationStatus): PermissionStatus {
    return when (status) {
        CMAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
        CMAuthorizationStatusDenied -> PermissionStatus.PERMANENTLY_DENIED
        CMAuthorizationStatusRestricted -> PermissionStatus.PERMANENTLY_DENIED
        CMAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
        else -> PermissionStatus.NOT_DETERMINED
    }
}
