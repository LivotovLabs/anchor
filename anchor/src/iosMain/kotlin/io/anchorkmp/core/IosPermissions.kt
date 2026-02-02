package io.anchorkmp.core

import platform.CoreLocation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.cinterop.ExperimentalForeignApi

internal actual fun platformPermissionCheck(scope: PermissionScope): PermissionStatus {
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
