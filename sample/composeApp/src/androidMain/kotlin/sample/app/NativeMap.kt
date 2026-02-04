package sample.app

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
actual fun NativeMap(locations: List<StoredLocation>, modifier: Modifier) {
    if (locations.isEmpty()) {
        return
    }

    val cameraPositionState = rememberCameraPositionState()
    var isMapLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(locations, isMapLoaded) {
        if (locations.isNotEmpty() && isMapLoaded) {
            val builder = LatLngBounds.builder()
            locations.forEach { builder.include(LatLng(it.lat, it.lon)) }
            val bounds = builder.build()
            
            try {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        onMapLoaded = { isMapLoaded = true }
    ) {
        locations.forEach { location ->
            val hue = when (location.activity) {
                "STATIONARY" -> 270f // Violet (Proxy for Gray)
                "WALKING" -> BitmapDescriptorFactory.HUE_GREEN
                "RUNNING" -> BitmapDescriptorFactory.HUE_YELLOW
                "AUTOMOTIVE" -> BitmapDescriptorFactory.HUE_BLUE
                "CYCLING" -> BitmapDescriptorFactory.HUE_ORANGE
                "UNKNOWN" -> BitmapDescriptorFactory.HUE_RED
                else -> BitmapDescriptorFactory.HUE_RED
            }
            
            val date = Instant.fromEpochMilliseconds(location.timestamp)
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val timeStr = "${date.date} ${date.time.hour}:${date.time.minute}:${date.time.second}"
            val speedStr = location.speed?.let { "%.1f km/h".format(it * 3.6) } ?: "N/A"
            val bearingStr = location.bearing?.let { "%.1f°".format(it) } ?: "N/A"
            val activityStr = location.activity ?: "Unknown"

            MarkerInfoWindow(
                state = MarkerState(position = LatLng(location.lat, location.lon)),
                icon = BitmapDescriptorFactory.defaultMarker(hue)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Text(
                        text = activityStr,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(text = "Speed: $speedStr", color = Color.Black)
                    Text(text = "Bearing: $bearingStr", color = Color.Black)
                    Text(text = "Time: $timeStr", color = Color.Black)
                }
            }
        }
    }
}
