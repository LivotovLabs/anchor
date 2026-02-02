package io.anchorkmp.core

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Maps to Android's `Priority` constants.
 */
enum class AndroidPriority(val value: Int) {
    HIGH_ACCURACY(100),
    BALANCED(102),
    LOW_POWER(104),
    PASSIVE(105)
}

/**
 * Maps to iOS `kCLLocationAccuracy` constants.
 */
enum class IosAccuracy(val value: Double) {
    NAVIGATION(-2.0),
    BEST(-1.0),
    NEAREST_TEN_METERS(10.0),
    HUNDRED_METERS(100.0),
    KILOMETER(1000.0),
    THREE_KILOMETERS(3000.0)
}

/**
 * Maps to iOS `CLActivityType` constants.
 */
enum class IosActivityType {
    OTHER,
    AUTOMOTIVE_NAVIGATION,
    FITNESS,
    OTHER_NAVIGATION,
    AIRBORNE
}

enum class AnchorActivityType {
    STATIONARY,
    WALKING,
    RUNNING,
    AUTOMOTIVE,
    CYCLING,
    UNKNOWN
}

/**
 * The configuration for the Free/Core version.
 */
class AnchorConfig private constructor(
    val android: AndroidConfig,
    val ios: IosConfig,
    val minUpdateDistanceMeters: Double,
    val trackActivity: Boolean
) {

    data class AndroidConfig(
        val updateInterval: Duration,
        val priority: AndroidPriority,
        val notification: AnchorNotification = AnchorNotification()
    )

    data class IosConfig(
        val desiredAccuracy: IosAccuracy,
        val autoPause: Boolean,
        val activityType: IosActivityType
    )

    companion object {
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    class Builder {
        var minUpdateDistanceMeters: Double = 0.0
        var trackActivity: Boolean = false

        // Internal holders
        private var androidConfig = AndroidConfig(
            updateInterval = 10.seconds,
            priority = AndroidPriority.BALANCED
        )
        
        private var iosConfig = IosConfig(
            desiredAccuracy = IosAccuracy.BEST,
            autoPause = true,
            activityType = IosActivityType.OTHER
        )

        fun android(block: AndroidBuilder.() -> Unit) {
            val builder = AndroidBuilder(androidConfig)
            builder.block()
            androidConfig = builder.build()
        }

        fun ios(block: IosBuilder.() -> Unit) {
            val builder = IosBuilder(iosConfig)
            builder.block()
            iosConfig = builder.build()
        }

        // Used by Pro to load existing state
        fun loadFrom(config: AnchorConfig) {
            this.minUpdateDistanceMeters = config.minUpdateDistanceMeters
            this.trackActivity = config.trackActivity
            this.androidConfig = config.android
            this.iosConfig = config.ios
        }

        fun build() = AnchorConfig(androidConfig, iosConfig, minUpdateDistanceMeters, trackActivity)
    }

    class AndroidBuilder(defaults: AndroidConfig) {
        var updateInterval: Duration = defaults.updateInterval
        var priority: AndroidPriority = defaults.priority
        var notification: AnchorNotification = defaults.notification

        fun notification(block: AnchorNotificationBuilder.() -> Unit) {
            val builder = AnchorNotificationBuilder(notification)
            builder.block()
            notification = builder.build()
        }

        fun build() = AndroidConfig(updateInterval, priority, notification)
    }

    class AnchorNotificationBuilder(defaults: AnchorNotification) {
        var title: String = defaults.title
        var body: String = defaults.body
        var iconName: String = defaults.iconName
        var channelId: String = defaults.channelId
        var channelName: String = defaults.channelName

        fun build() = AnchorNotification(title, body, iconName, channelId, channelName)
    }

    class IosBuilder(defaults: IosConfig) {
        var desiredAccuracy: IosAccuracy = defaults.desiredAccuracy
        var autoPause: Boolean = defaults.autoPause
        var activityType: IosActivityType = defaults.activityType
        fun build() = IosConfig(desiredAccuracy, autoPause, activityType)
    }

    fun toBuilder(): Builder {
        val b = Builder()
        b.loadFrom(this)
        return b
    }
}

data class AnchorLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val bearing: Float? = null,
    val timestamp: Long,
    val activity: AnchorActivityType? = null
)
