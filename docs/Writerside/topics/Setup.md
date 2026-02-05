# Setup

This guide covers the installation and configuration required to use AnchorKMP in your Kotlin Multiplatform project.

## Gradle Dependency

Add the `anchor` dependency to your `commonMain` source set in `build.gradle.kts`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("io.anchorkmp:anchor:1.0.0") // Replace with latest version
        }
    }
}
```

## Android Setup

AnchorKMP handles most Android manifests automatically via manifest merging. It declares the necessary permissions and services for you.

### Permissions

The library includes the following permissions in its manifest:
*   `ACCESS_COARSE_LOCATION`
*   `ACCESS_FINE_LOCATION`
*   `ACCESS_BACKGROUND_LOCATION`
*   `FOREGROUND_SERVICE`
*   `FOREGROUND_SERVICE_LOCATION`
*   `POST_NOTIFICATIONS` (Android 13+)
*   `ACTIVITY_RECOGNITION`

<note>
You generally do not need to add these manually to your app's manifest unless you need to strictly remove some of them.
</note>

### Context Initialization

AnchorKMP uses an `AnchorInitProvider` to automatically retrieve the `Application` context on startup. You do not need to manually initialize the library with a context.

## iOS Setup

For iOS, you must configure your `Info.plist` and enable specific capabilities to allow location tracking.

### Info.plist Keys

Add the following keys to your `iosApp/iosApp/Info.plist` (or wherever your plist is located) to explain why your app needs location access. The system shows these strings to the user in the permission dialog.

```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>We need your location to show where you are on the map.</string>

<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>We need your location to track your journey even when the app is in the background.</string>

<key>NSLocationAlwaysUsageDescription</key>
<string>We need your location to track your journey in the background.</string>

<key>NSMotionUsageDescription</key>
<string>We use motion detection to optimize battery usage when you are stationary.</string>
```

### Background Modes

If you plan to use background location tracking:

1.  Open your project in **Xcode**.
2.  Select your Target.
3.  Go to the **Signing &amp; Capabilities** tab.
4.  Click **+ Capability** and add **Background Modes**.
5.  Check **Location updates**.

This allows the app to receive location events while suspended or in the background.
