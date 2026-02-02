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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

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
            Marker(
                state = MarkerState(position = LatLng(location.lat, location.lon)),
                title = "Location",
                snippet = "Time: ${location.timestamp}"
            )
        }
    }
}
