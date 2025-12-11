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
2.  **Outdated:** Wrapping 5-year-old Objective-C code or relying on hybrid bridges.
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
```

---

## 🗺️ Roadmap to v1.0

We are building in public. Here is our progress:

- [x] **Project Setup & Architecture** (Gradle, KMP, CI/CD)
- [x] **Android Core** (FusedLocationProvider + Foreground Service)
- [x] **iOS Core** (CLLocationManager + Background Modes)
- [x] **Permission Handler** (Unified API for Android/iOS)
- [ ] **Pro Features** (SQLite Buffering & Auto-Sync)
- [ ] **Documentation Website**
- [ ] **Maven Central Release**

---

## 💎 The Business Model (Transparency)

We believe in sustainable open source.

1.  **Anchor Core (Apache 2.0):** Free, Open Source. Unlimited real-time tracking, background service management, and permission handling.
2.  **Anchor Pro (Commercial):** Will include "heavy lifting" features like built-in SQLite persistence, Auto-Sync to HTTP endpoints, and Motion Activity Intelligence to save battery.

---

## 📩 Get Updates

*   ⭐ **Star this repo** to show support and boost visibility.
*   👀 **Watch this repo** to be notified of the Beta release.
*   Visit **[anchorkmp.io](https://anchorkmp.io)** to sign up for the waitlist.

---

### License
Copyright © 2025 **[LivotovLabs](https://labs.livotov.eu)**.

Licensed under the **Apache License, Version 2.0** (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. S
