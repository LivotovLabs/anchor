# Introduction

**AnchorKMP** is a powerful, battery-efficient Cross-Platform (KMP) location tracking library for Android and iOS. It provides a unified API to handle location updates, permissions, and background tracking with ease.

## Key Features

*   **Unified API**: Write your location logic once in Kotlin Common code.
*   **Background Tracking**: Built-in support for background location updates (Foreground Service on Android, Background Modes on iOS).
*   **Permissions Handling**: Simplified API for requesting and checking permissions (Foreground, Background, Notification, Motion).
*   **Battery Efficient**: Configurable update intervals, distance filters, and accuracy settings to balance performance and battery life.
*   **Customizable**: tailored configuration for specific platform needs (e.g., Android Notification customization, iOS Activity Type).
*   **Modern**: Built with Kotlin Coroutines and Flow.

## How it works

AnchorKMP abstracts away the complexities of `LocationManager` (Android) and `CLLocationManager` (iOS). It uses a singleton `Anchor` object to manage state and exposes a simple Flow-based API for location updates.

On **Android**, it automatically handles the Foreground Service lifecycle required for background tracking.
On **iOS**, it manages the standard location manager delegates and background capabilities.

## Getting Started

Check out the [Setup](Setup.md) guide to integrate AnchorKMP into your project.
