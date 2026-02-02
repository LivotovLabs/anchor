package io.anchorkmp.core

data class AnchorNotification(
    val title: String = "Tracking Active",
    val body: String = "Location tracking is running",
    val iconName: String = "anchor_notification_icon",
    val channelId: String = "anchor_location_channel",
    val channelName: String = "Location Tracking"
)
