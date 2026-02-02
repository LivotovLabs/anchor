# ⚓ Anchor: KMP Background Geolocation

[![Status](https://img.shields.io/badge/status-in%20development-orange)](https://github.com/LivotovLabs/anchor)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-android%20|%20ios-lightgrey)](https://anchorkmp.io)

> 🚧 **Work In Progress:** Anchor is currently under active development.
> **[Watch this repository](https://github.com/LivotovLabs/anchor/subscription)** to get notified when v1.0.0 drops.

**Anchor** is a robust, battery-conscious background geolocation library built specifically for **Kotlin Multiplatform**.

We are building the first **native KMP** alternative to legacy wrappers. No Cordova bridges. No React Native JSON passing. 
Just pure Kotlin and Swift performance for modern Android & iOS apps.

[**Website**](https://anchorkmp.io) | [**Roadmap**](#-roadmap)


## Features

*   **Cross-Platform:** Shared API for Android and iOS.
*   **Always Background:** Designed specifically for reliable background location updates.
*   **Platform Specific Tuning:** Granular control over Android's Priority and iOS's Accuracy and Activity types.
*   **Modern API:** Built with Kotlin DSL, Coroutines and Flow.

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

> **Note:** You must still request the location permissions (and `ACTIVITY_RECOGNITION` on Android 10+, and `POST_NOTIFICATIONS` on Android 13+) from the user at runtime before starting tracking.

### iOS

1.  **Info.plist:** Add usage descriptions to `iosApp/iosApp/Info.plist`.

    ```xml
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>We need your location to track your journey.</string>
    <key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
    <string>We need your location to track your journey even in the background.</string>
    <key>NSLocationAlwaysUsageDescription</key>
    <string>We need your location to track your journey even in the background.</string>
    <key>NSMotionUsageDescription</key>
    <string>We need access to motion data to detect if you are walking, running, or driving.</string>
    <key>UIBackgroundModes</key>
    <array>
        <string>location</string>
        <string>fetch</string>
        <string>processing</string>
    </array>
    ```

## Usage

### 1. Configure and Initialize
... (omitted for brevity) ...
### 2. Check and Request Permissions

Anchor provides a clean coroutine-based API for handling permissions.

```kotlin
// In your ViewModel or coroutine scope
scope.launch {
    // Request basic permissions (suspends until user decides)
    Anchor.requestPermission(PermissionScope.NOTIFICATIONS)
    Anchor.requestPermission(PermissionScope.MOTION)

    // Check if everything is ready based on your config
    if (Anchor.isReady) {
        Anchor.startTracking()
    } else {
        // Request background location
        val result = Anchor.requestPermission(PermissionScope.BACKGROUND)
        
        if (result == PermissionStatus.GRANTED) {
            Anchor.startTracking()
        } else {
            println("Permission denied")
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

## Demo Application

This repository includes a sample Compose Multiplatform application in the `sample/` directory that demonstrates background tracking, activity detection, and native map integration.

### Android Setup (Google Maps)

The Android sample uses Google Maps. To provide your API Key:

1. Create or open `local.properties` in the project root directory.
2. Add your Google Maps API Key:
   ```properties
   MAPS_API_KEY=AIzaSy...your_key...
   ```
3. Run the application:
   ```bash
   ./gradlew :sample:composeApp:installDebug
   ```

### iOS Setup (Apple Maps)

The iOS sample uses native Apple Maps (MapKit) and requires no additional API keys.

1. Open `sample/iosApp/iosApp.xcodeproj` in Xcode.
2. Select your target device or simulator.
3. Build and Run.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
