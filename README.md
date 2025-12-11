# ⚓ Anchor: KMP Background Location

[![Status](https://img.shields.io/badge/status-in%20development-orange)](https://github.com/LivotovLabs/anchor)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-android%20|%20ios-lightgrey)](https://anchorkmp.io)

> 🚧 **Work In Progress:** Anchor is currently under active development.
> **[Watch this repository](https://github.com/LivotovLabs/anchor/subscription)** to get notified when v1.0.0 drops.

**Anchor** is a robust, battery-conscious background geolocation library built specifically for **Kotlin Multiplatform**.

We are building the first **native KMP** alternative to legacy wrappers. No Cordova bridges. No React Native JSON passing. Just pure Kotlin and Swift performance for modern Android & iOS apps.

[**Website**](https://anchorkmp.io) | [**Roadmap**](#-roadmap)

---

## ⚡ The Problem We Are Solving

Background location on mobile is broken. Existing libraries are often:
1.  **Expensive:** Charging $300+ per app for basic features.
2.  **Outdated:** Wrapping 5-year-old Objective-C code or reliance on hybrid bridges.
3.  **Fragile:** Failing to restart when the OS kills the background service.

**Anchor** solves this with a modern, reactive architecture built on Coroutines and Flow.

---

## 🔮 API Preview (Design Draft)

This is how you will use Anchor when v1.0 launches. We are designing for Type-Safety and clean architecture.

```kotlin
// 1. Configure
val config = AnchorConfig(
    interval = 10.seconds,        // Update frequency
    accuracy = Accuracy.HIGH,     // GPS Priority
    stopOnTerminate = false,      // Keep tracking if app is killed
    notification = NotificationConfig(
        title = "Tracking Active",
        text = "Recording your location..."
    )
)

// 2. Collect (No Callback Hell)
Anchor.start(config).collect { location ->
    println("Lat: ${location.latitude}, Lon: ${location.longitude}")
    println("Speed: ${location.speed}, Altitude: ${location.altitude}")
}
