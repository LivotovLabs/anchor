# Best Practices

## Handling Process Death on Android

One of the most critical aspects of background location tracking on Android is handling **process death**.

### The Problem

When your app is in the background, Android may kill your application process to reclaim resources. However, if you are running a **Foreground Service** (which AnchorKMP does), the system will keep the Service alive (or restart it), but your Activities and UI-related objects (ViewControllers, ViewModels, Composables) will be destroyed.

This leads to a situation where:
1.  The Anchor Service is running and fetching locations.
2.  Your UI (and any collectors inside it) is dead.
3.  **Result:** Location updates are happening, but your app is not saving or processing them.

### The Solution: Application-Scope Monitoring

To ensure you never miss an update, you must initialize Anchor and subscribe to `locationFlow` in a component that survives as long as the application process itself. The best place for this is your `Application` class (Android) or `App` struct (iOS).

#### 1. Create a Singleton Manager
Create a shared object that manages initialization and data persistence.

```kotlin
// commonMain
object LocationManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun monitor() {
        // 1. Initialize Anchor
        Anchor.init { /* ... config ... */ }
        
        // 2. Start collecting immediately
        scope.launch {
            Anchor.locationFlow.collect { location ->
                // Save to DB / Send to API
                // This runs even if UI is dead
                Repository.save(location)
            }
        }
        
        // 3. Resume tracking if needed
        // If the app was killed while tracking, we should ensure the engine is started.
        if (shouldBeTracking()) {
             scope.launch {
                 if (Anchor.isReady) Anchor.startTracking()
             }
        }
    }
}
```

#### 2. Initialize in Platform Entry Points

**Android (`Application.onCreate`):**

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        LocationManager.monitor()
    }
}
```

**iOS (`App.init`):**

```swift
@main
struct iOSApp: App {
    init() {
        LocationManager.shared.monitor()
    }
    // ...
}
```

### UI vs. Background Logic

With this pattern, your architecture becomes cleaner:

*   **Background Layer (LocationManager)**: The single source of truth for *writing* data. It runs independently of the UI.
*   **UI Layer (Compose/SwiftUI)**: A passive observer that *reads* data from the Repository/Database. It does not control the lifecycle of the location collection directly.
