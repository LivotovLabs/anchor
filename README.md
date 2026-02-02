# Anchor KMP Core

Anchor KMP Core is a Kotlin Multiplatform library for efficient and customizable background geolocation tracking on Android and iOS.

## Features

*   **Cross-Platform:** Shared API for Android and iOS.
*   **Always Background:** Designed specifically for reliable background location updates.
*   **Platform Specific Tuning:** Granular control over Android's Priority and iOS's Accuracy and Activity types.
*   **Modern API:** Built with Kotlin Coroutines and Flow.

## Installation

Add the dependency to your common module's `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation("io.anchorkmp:core:1.0.0")
}
```

## Setup

### Android

No manual setup is required. The library includes the necessary permissions and initializes automatically.

> **Note:** You must still request the location permissions (and `ACTIVITY_RECOGNITION` on Android 10+) from the user at runtime before starting tracking.

### iOS

1.  **Info.plist:** Add usage descriptions to `iosApp/iosApp/Info.plist`.

    ```xml
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>We need your location to track your journey.</string>
    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
    <string>We need your location to track your journey even in the background.</string>
    <key>NSMotionUsageDescription</key>
    <string>We need access to motion data to detect if you are walking, running, or driving.</string>
    <key>UIBackgroundModes</key>
    <array>
        <string>location</string>
    </array>
    ```

## Usage

### 1. Configure and Initialize

Initialize `Anchor` with your configuration, typically in your application startup.

```kotlin
import io.anchorkmp.core.*
import kotlin.time.Duration.Companion.seconds

val config = AnchorConfig.build {
    trackActivity = true
    minUpdateDistanceMeters = 10.0

    android {
        updateInterval = 10.seconds
        priority = AndroidPriority.HIGH_ACCURACY
        
        notification {
            title = "Tracking Active"
            body = "We are tracking your location"
            iconName = "my_custom_icon"
        }
    }
    
    ios {
        desiredAccuracy = IosAccuracy.BEST
        autoPause = true
        activityType = IosActivityType.OTHER
    }
}

Anchor.init(config)
```

### 2. Check and Request Permissions

Anchor provides a clean coroutine-based API for handling permissions.

```kotlin
// In your ViewModel or coroutine scope
scope.launch {
    // Check if everything is ready based on your config
    if (Anchor.isReady) {
        Anchor.startTracking()
    } else {
        // Request permissions (suspends until user decides)
        val result = Anchor.requestPermission(PermissionScope.BACKGROUND)
        
        if (result == PermissionStatus.GRANTED) {
            Anchor.startTracking()
        } else {
            println("Permission denied")
            // Handle denial (e.g. show settings button if PERMANENTLY_DENIED)
        }
    }
}
```

### 3. Control Tracking

```kotlin
// Start tracking
scope.launch {
    Anchor.startTracking()
}

// Observe updates
scope.launch {
    Anchor.locationFlow.collect { location ->
        // location is AnchorLocation
        println("New Location: ${location.latitude}, ${location.longitude}")
    }
}

// Update configuration at runtime
scope.launch {
    Anchor.updateConfig {
        android {
            updateInterval = 5.seconds // Change interval
        }
    }
}

// Stop tracking
scope.launch {
    Anchor.stopTracking()
}
```

### 4. One-shot Location

If you only need the user's current location once without starting continuous tracking:

```kotlin
scope.launch {
    try {
        val location = Anchor.getLocation()
        println("Current Location: ${location.latitude}, ${location.longitude}")
    } catch (e: Exception) {
        println("Failed to get location: ${e.message}")
    }
}
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
