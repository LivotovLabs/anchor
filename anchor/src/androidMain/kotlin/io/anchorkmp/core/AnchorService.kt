package io.anchorkmp.core

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat

internal class AnchorService : Service(), LocationListener {

    private val locationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val config = Anchor._configState.value
        val notifConfig = config?.android?.notification ?: AnchorNotification()

        val title = notifConfig.title
        val body = notifConfig.body
        val icon = resolveDrawableId(this, notifConfig.iconName)
        val channelId = notifConfig.channelId
        val channelName = notifConfig.channelName

        startForeground(NOTIFICATION_ID, createNotification(title, body, icon, channelId, channelName))
        startLocationUpdates(config)

        return START_STICKY
    }

    private fun createNotification(
        title: String,
        body: String,
        icon: Int,
        channelId: String,
        channelName: String
    ): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val validIcon = if (icon != 0) icon else android.R.drawable.ic_menu_mylocation

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(validIcon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(config: AnchorConfig?) {
        if (config == null) return

        val provider = when (config.android.priority) {
            AndroidPriority.HIGH_ACCURACY -> LocationManager.GPS_PROVIDER
            AndroidPriority.BALANCED -> LocationManager.NETWORK_PROVIDER
            AndroidPriority.LOW_POWER -> LocationManager.PASSIVE_PROVIDER
            AndroidPriority.PASSIVE -> LocationManager.PASSIVE_PROVIDER
        }

        locationManager.removeUpdates(this)
        locationManager.requestLocationUpdates(
            provider,
            config.android.updateInterval.inWholeMilliseconds,
            config.minUpdateDistanceMeters.toFloat(),
            this
        )
    }

    override fun onLocationChanged(androidLocation: android.location.Location) {
        val config = Anchor._configState.value
        (Anchor.engine as? AndroidLocationTrackerEngine)?.reportLocation(
            AnchorLocation(
                latitude = androidLocation.latitude,
                longitude = androidLocation.longitude,
                altitude = if (androidLocation.hasAltitude()) androidLocation.altitude else null,
                accuracy = if (androidLocation.hasAccuracy()) androidLocation.accuracy else null,
                speed = if (androidLocation.hasSpeed()) androidLocation.speed else null,
                bearing = if (androidLocation.hasBearing()) androidLocation.bearing else null,
                timestamp = androidLocation.time,
                activity = if (config?.trackActivity == true) {
                    (Anchor.engine as? AndroidLocationTrackerEngine)?.getCurrentActivity()
                } else null
            )
        )
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    override fun onDestroy() {
        locationManager.removeUpdates(this)
        super.onDestroy()
    }

    internal fun resolveDrawableId(context: Context, iconName: String): Int {
        val resId = context.resources.getIdentifier(
            iconName,
            "drawable",
            context.packageName
        )

        if (resId == 0) {
            // Fallback: If user forgot to add the icon, use a default from the library
            // or the app's launcher icon to prevent a crash.
            return R.drawable.anchor_notification_icon
        }

        return resId
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
