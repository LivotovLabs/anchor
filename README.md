<div align="center">
  <h1>⚓️ Anchor</h1>
  <h3>Background Geolocation for Kotlin Multiplatform</h3>
  
  <p>
    <b>Reliable. Battery-Conscious. Native Performance.</b>
  </p>

  [![Maven Central](https://img.shields.io/maven-central/v/io.anchorkmp/core?style=flat-square&color=blue)](https://central.sonatype.com/artifact/io.anchorkmp/core)
  [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](https://opensource.org/licenses/Apache-2.0)
  [![Platform](https://img.shields.io/badge/platform-android%20|%20ios-lightgrey?style=flat-square)](https://anchorkmp.io)
  [![Status](https://img.shields.io/badge/status-alpha-orange?style=flat-square)](https://github.com/LivotovLabs/anchor)

  <br/>
  
  <a href="https://anchorkmp.io"><strong>Website</strong></a> · 
  <a href="#-installation"><strong>Installation</strong></a> · 
  <a href="#-demo-application"><strong>Demo App</strong></a>
</div>

<br/>

> **Anchor** is a native, Kotlin-first background geolocation library for **Kotlin Multiplatform**, designed for high performance and seamless integration in modern Android and iOS applications.
>
> ⚠️ **Note:** The product is currently in its final stages of development and testing. The official **v1.0 release** is expected during **February 2026**.

---

## ✨ Features

- 🔋 **Always Background:** Architected specifically for reliable, long-running background tracking.
- 🎯 **Platform Tuned:** Granular control over Android Priority/Interval and iOS Accuracy/Activity Type.
- 🚀 **Modern API:** Built with Kotlin DSL, Coroutines, and Flow.
- 📱 **Cross-Platform:** Single shared API for Android and iOS.

---

## 📦 Installation

Add the dependency to your `commonMain` source set in `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation("io.anchorkmp:core:0.0.1")
}
```

---

## 🛠 Platform Setup

<details>
<summary><strong>🤖 Android Setup</strong></summary>

No manual initialization code is required. However, you must declare the foreground service type in your manifest if you are targeting Android 14+.

The library automatically includes the following permissions:
- `ACCESS_COARSE_LOCATION`
- `ACCESS_FINE_LOCATION`
- `ACCESS_BACKGROUND_LOCATION`
- `FOREGROUND_SERVICE_LOCATION`
- `POST_NOTIFICATIONS`
- `ACTIVITY_RECOGNITION`

</details>

<details>
<summary><strong>🍎 iOS Setup</strong></summary>

Add the following keys to your `Info.plist` (typically in `iosApp/iosApp/Info.plist`):

```xml
<!-- Location Permissions -->
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location to track your journey.</string>
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>We need your location to track your journey even in the background.</string>
<key>NSLocationAlwaysUsageDescription</key>
<string>We need your location to track your journey even in the background.</string>

<!-- Motion Permission (for Activity Recognition) -->
<key>NSMotionUsageDescription</key>
<string>We need access to motion data to detect if you are walking, running, or driving.</string>

<!-- Background Modes -->
<key>UIBackgroundModes</key>
<array>
    <string>location</string>
    <string>fetch</string>
    <string>processing</string>
</array>
```
</details>

---

## 🚀 Quick Start

### 1. Configuration

Initialize Anchor in your application startup logic.

```kotlin
import io.anchorkmp.core.*
import kotlin.time.Duration.Companion.seconds

Anchor.init {
    // Shared Options
    trackActivity = true            // Enable activity recognition (walking, driving, etc.)
    minUpdateDistanceMeters = 10.0  // Minimum distance before an update is triggered

    // Android Specifics
    android {
        updateInterval = 10.seconds          // Desired frequency of updates
        priority = AndroidPriority.BALANCED  // Balance between battery and accuracy
        
        notification {
            title = "Tracking Active"        // Persistent notification title
            body = "Location tracking is on" // Persistent notification body
            iconName = "ic_tracker"          // Drawable resource name
        }
    }
    
    // iOS Specifics
    ios {
        desiredAccuracy = IosAccuracy.BEST   // kCLLocationAccuracyBest
        autoPause = true                     // Allow system to pause updates to save battery
        activityType = IosActivityType.OTHER // CLActivityType
        displayBackgroundLocationIndicator = true // Show blue pill in status bar
    }
}
```

### 2. Permissions & Tracking

Anchor provides a simple coroutine-based API for permission management.

```kotlin
scope.launch {
    // 1. Request permissions (suspends until user decides)
    // Suggest asking for Notifications & Motion first
    Anchor.requestPermission(PermissionScope.NOTIFICATIONS)
    Anchor.requestPermission(PermissionScope.MOTION)

    // 2. Request Background Location
    val status = Anchor.requestPermission(PermissionScope.BACKGROUND)
    
    if (status == PermissionStatus.GRANTED) {
        // 3. Start Tracking
        Anchor.startTracking()
    } else {
        println("Permission denied")
    }
}
```

### 3. Observe Updates

```kotlin
scope.launch {
    Anchor.locationFlow.collect { location ->
        println("📍 Location: ${location.latitude}, ${location.longitude}")
        println("🏃 Activity: ${location.activity}")
    }
}
```

> **💡 Pro Tip:** For robust background tracking that survives process death, initialize Anchor and subscribe to updates in your `Application` class, not your Activity/UI. See [Best Practices](docs/Writerside/topics/Best-Practices.md).

---

## 📱 Demo Application

Check out the `sample/` directory for a complete Compose Multiplatform app demonstrating background tracking and native maps.

### Running on Android
1. Create `local.properties` in the project root.
2. Add your Google Maps API Key: `MAPS_API_KEY=AIzaSy...`
3. Run: `./gradlew :sample:composeApp:installDebug`

### Running on iOS
1. Open `sample/iosApp/iosApp.xcodeproj` in Xcode.
2. Select your target device and run. (Uses native Apple Maps, no key required).

---

<div align="center">
  <p>
    Licensed under <a href="LICENSE">Apache 2.0</a>.
    <br/>
    Built with ❤️ by <a href="https://github.com/LivotovLabs">Livotov Labs</a>.
  </p>
</div>
