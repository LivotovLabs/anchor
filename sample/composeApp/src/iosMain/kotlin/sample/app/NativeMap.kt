package sample.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKCoordinateRegionMake
import platform.MapKit.MKCoordinateSpanMake
import platform.MapKit.MKMapView
import platform.MapKit.MKPointAnnotation

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeMap(locations: List<StoredLocation>, modifier: Modifier) {
    if (locations.isEmpty()) {
        return
    }

    val lastLocation = locations.last()
    val center = CLLocationCoordinate2DMake(lastLocation.lat, lastLocation.lon)
    
    UIKitView(
        factory = {
            val mapView = MKMapView()
            mapView.setRegion(
                MKCoordinateRegionMake(center, MKCoordinateSpanMake(0.01, 0.01)),
                animated = true
            )
            mapView
        },
        modifier = modifier,
        update = { mapView ->
            mapView.removeAnnotations(mapView.annotations)
            locations.forEach { loc ->
                val annotation = MKPointAnnotation()
                annotation.setCoordinate(CLLocationCoordinate2DMake(loc.lat, loc.lon))
                annotation.setTitle("Location")
                annotation.setSubtitle("Time: ${loc.timestamp}")
                mapView.addAnnotation(annotation)
            }
            if (locations.isNotEmpty()) {
                mapView.showAnnotations(mapView.annotations, animated = true)
            }
        }
    )
}
