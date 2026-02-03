# Usage

Once you have [setup](Setup.md) the project and defined your [configuration](Configuration.md), you are ready to use AnchorKMP.

## Initialization

You must initialize the `Anchor` singleton with your configuration before using any other features. This is typically done in your `Application` class or main entry point.

```kotlin
val config = AnchorConfig.build {
    // ... your configuration
}

Anchor.init(config)
```

## Permissions

AnchorKMP provides a unified API to check and request permissions.

### Checking Permissions

You can check the status of a specific permission scope non-blockingly:

```kotlin
val status = Anchor.checkPermission(PermissionScope.BACKGROUND)
if (status == PermissionStatus.GRANTED) {
    // Ready to track
}
```

Or check if the library is ready for the *current configuration*:

```kotlin
if (Anchor.isReady) {
    Anchor.startTracking()
}
```

### Requesting Permissions

To request permissions, call `requestPermission`. This is a **suspending** function that handles the UI flow (launching a transparent Activity on Android, or system dialogs on iOS) and returns the result.

```kotlin
scope.launch {
    // 1. Request Foreground (When In Use)
    val foregroundResult = Anchor.requestPermission(PermissionScope.FOREGROUND)
    
    if (foregroundResult == PermissionStatus.GRANTED) {
        // 2. Request Background (Always) if needed
        val backgroundResult = Anchor.requestPermission(PermissionScope.BACKGROUND)
        
        // 3. Request Notification permission (Android 13+)
        Anchor.requestPermission(PermissionScope.NOTIFICATIONS)
    }
}
```

### Permission Scopes
*   `FOREGROUND`: Access location while the app is in use.
*   `BACKGROUND`: Access location all the time.
*   `NOTIFICATIONS`: Permission to post notifications (Android).
*   `MOTION`: Activity Recognition / Motion usage.

## Tracking Location

### Continuous Updates

To receive continuous location updates, observe the `locationFlow`.

```kotlin
scope.launch {
    Anchor.locationFlow.collect { location ->
        println("New Location: ${location.latitude}, ${location.longitude}")
        
        if (location.activity != null) {
             println("Activity: ${location.activity}")
        }
    }
}
```

Start and stop the tracking engine:

```kotlin
// Start tracking (starts Service on Android / LocationManager on iOS)
Anchor.startTracking()

// Stop tracking
Anchor.stopTracking()
```

### Single Location

If you only need a one-time location fix:

```kotlin
val location = Anchor.getLocation()
println("Current location: $location")
```

## Runtime Reconfiguration

You can update the configuration while the app is running (even while tracking). The engine will automatically restart or adjust to apply the new settings.

```kotlin
Anchor.updateConfig {
    // Change to high accuracy mode
    android {
        priority = AndroidPriority.HIGH_ACCURACY
        updateInterval = 1.seconds
    }
    ios {
        desiredAccuracy = IosAccuracy.NAVIGATION
    }
}
```
