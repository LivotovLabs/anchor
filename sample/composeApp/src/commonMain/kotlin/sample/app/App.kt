package sample.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import io.anchorkmp.core.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@Serializable
data class StoredLocation(
    val lat: Double,
    val lon: Double,
    val timestamp: Long,
    val speed: Float?,
    val bearing: Float?,
    val activity: String?
)

class LocationManager {
    private val settings = Settings()
    private val json = Json { ignoreUnknownKeys = true }
    private val key = "locations"
    private val trackingKey = "tracking_enabled"

    var locations = mutableStateListOf<StoredLocation>()
        private set

    init {
        loadLocations()
        initAnchor()
    }
    
    fun shouldResumeTracking(): Boolean {
        return settings.getBoolean(trackingKey, false)
    }
    
    private fun initAnchor() {
        Anchor.init {
            android {
                updateInterval = 5.seconds
                priority = AndroidPriority.HIGH_ACCURACY
                notification {
                    title = "Demo Tracker"
                    body = "Location tracking by the sample app"
                }
            }
            ios {
                desiredAccuracy = IosAccuracy.BEST
                activityType = IosActivityType.AUTOMOTIVE_NAVIGATION
                autoPause = false
            }
            trackActivity = true
        }
    }

    private fun loadLocations() {
        val saved = settings.getStringOrNull(key)
        if (saved != null) {
            try {
                val list = json.decodeFromString<List<StoredLocation>>(saved)
                locations.clear()
                locations.addAll(list)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveLocation(loc: AnchorLocation) {
        val newLoc = StoredLocation(
            lat = loc.latitude, 
            lon = loc.longitude, 
            timestamp = loc.timestamp,
            speed = loc.speed,
            bearing = loc.bearing,
            activity = loc.activity?.name
        )
        locations.add(newLoc)
        persist()
    }
    
    fun clear() {
        locations.clear()
        persist()
    }
    
    private fun persist() {
        val str = json.encodeToString(locations.toList())
        settings[key] = str
    }

    suspend fun startTracking() {
        // Request permissions first
        Anchor.requestPermission(PermissionScope.NOTIFICATIONS)
        Anchor.requestPermission(PermissionScope.MOTION)
        
        val status = Anchor.requestPermission(PermissionScope.BACKGROUND)
        if (status == PermissionStatus.GRANTED) {
            Anchor.startTracking()
            settings.putBoolean(trackingKey, true)
        } else {
            println("Permission denied: $status")
            settings.putBoolean(trackingKey, false)
        }
    }

    suspend fun stopTracking() {
        Anchor.stopTracking()
        settings.putBoolean(trackingKey, false)
    }
}

enum class AppTab(val title: String, val icon: ImageVector) {
    Locations("Locations", Icons.AutoMirrored.Filled.List),
    Map("Map", Icons.Filled.LocationOn)
}

@Composable
fun App() {
    MaterialTheme {
        val scope = rememberCoroutineScope()
        val manager = remember { LocationManager() }
        var isTracking by remember { mutableStateOf(false) }
        var currentTab by remember { mutableStateOf(AppTab.Locations) }

        // Sync initial tracking state
        LaunchedEffect(Unit) {
            if (manager.shouldResumeTracking()) {
                manager.startTracking()
                isTracking = true
            } else {
                isTracking = Anchor.isTracking
            }
        }

        // Observe Anchor locations
        LaunchedEffect(Unit) {
            Anchor.locationFlow.collect { loc ->
                manager.saveLocation(loc)
            }
        }
        
        Scaffold(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars),
            topBar = {
                TopAppBar(
                    title = { Text("Anchor KMP Tracker") },
                    actions = {
                        if (isTracking) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        manager.stopTracking()
                                        isTracking = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red, contentColor = Color.White)
                            ) {
                                Text("Stop")
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        manager.startTracking()
                                        isTracking = true 
                                    }
                                }
                            ) {
                                Text("Start")
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = { manager.clear() },
                            enabled = manager.locations.isNotEmpty()
                        ) {
                            Text("Clear")
                        }
                    }
                )
            },
            bottomBar = {
                BottomNavigation {
                    AppTab.entries.forEach { tab ->
                        BottomNavigationItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title) },
                            selected = currentTab == tab,
                            onClick = { currentTab = tab }
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (currentTab) {
                    AppTab.Locations -> LocationsList(manager.locations)
                    AppTab.Map -> {
                        if (manager.locations.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No locations yet")
                            }
                        } else {
                            NativeMap(manager.locations, Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationsList(locations: List<StoredLocation>) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(locations.reversed()) { loc ->
            Card(elevation = 4.dp) {
                Column(Modifier.padding(16.dp)) {
                    val date = Instant.fromEpochMilliseconds(loc.timestamp)
                        .toLocalDateTime(TimeZone.currentSystemDefault())
                    
                    Text("Time: ${date.date} ${date.time.hour}:${date.time.minute}:${date.time.second}")
                    Text("Lat: ${loc.lat}, Lon: ${loc.lon}")
                    Text("Lon: ${loc.lon}")
                    if (loc.speed != null) Text("Speed: ${loc.speed} m/s")
                    if (loc.bearing != null) Text("Bearing: ${loc.bearing}°")
                    if (loc.activity != null) Text("Activity: ${loc.activity}")
                }
            }
        }
    }
}
