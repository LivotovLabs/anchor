package io.anchorkmp.core

enum class PermissionScope {
    /** Only while the app is open (Android: Fine/Coarse, iOS: WhenInUse) */
    FOREGROUND,
    
    /** Continuous tracking (Android: Background, iOS: Always) */
    BACKGROUND,
    
    /** Activity Recognition (Android: Activity Recognition, iOS: Motion) */
    MOTION
}

enum class PermissionStatus {
    /** Ready to track */
    GRANTED,
    
    /** User rejected, but we can ask again */
    DENIED,
    
    /** User rejected "Don't ask again" or system restricted. Needs Settings. */
    PERMANENTLY_DENIED,
    
    /** (iOS mostly) User hasn't made a choice yet */
    NOT_DETERMINED
}

internal expect fun platformPermissionCheck(scope: PermissionScope): PermissionStatus
internal expect suspend fun platformPermissionRequest(scope: PermissionScope): PermissionStatus
