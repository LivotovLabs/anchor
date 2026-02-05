package sample.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.UIKit.UIColor
import platform.UIKit.UILabel
import platform.UIKit.UIFont
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun NativeMap(locations: List<StoredLocation>, modifier: Modifier) {
    if (locations.isEmpty()) {
        return
    }

    val lastLocation = locations.last()
    val center = CLLocationCoordinate2DMake(lastLocation.lat, lastLocation.lon)
    
    val mapDelegate = remember { MapDelegate() }
    
    UIKitView(
        factory = {
            val mapView = MKMapView()
            mapView.delegate = mapDelegate
            mapView.setRegion(
                MKCoordinateRegionMake(center, MKCoordinateSpanMake(0.01, 0.01)),
                animated = true
            )
            mapView
        },
        modifier = modifier,
        update = { mapView ->
            println("NativeMap update: ${locations.size} locations")
            mapView.removeAnnotations(mapView.annotations)
            locations.forEach { loc ->
                val color = when (loc.activity) {
                    "STATIONARY" -> UIColor.grayColor
                    "WALKING" -> UIColor.greenColor
                    "RUNNING" -> UIColor.yellowColor
                    "AUTOMOTIVE" -> UIColor.blueColor
                    "CYCLING" -> UIColor.orangeColor
                    "UNKNOWN" -> UIColor.redColor
                    else -> UIColor.redColor
                }
                
                val date = Instant.fromEpochMilliseconds(loc.timestamp)
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                val timeStr = "${date.hour}:${date.minute}:${date.second}"
                // Simple formatting manually since String.format is not available in common/native easily
                // or requires importing platform.Foundation.NSString
                val speedStr = loc.speed?.let { "${(it * 3.6 * 10).toInt() / 10.0} km/h" } ?: "N/A"
                val bearingStr = loc.bearing?.let { "${(it * 10).toInt() / 10.0}°" } ?: "N/A"
                val activityStr = loc.activity ?: "Unknown"
                val detailedText = "Speed: $speedStr\nBearing: $bearingStr\nTime: $timeStr"

                val annotation = AnchorAnnotation(color, detailedText)
                annotation.setCoordinate(CLLocationCoordinate2DMake(loc.lat, loc.lon))
                annotation.setTitle(activityStr)
                mapView.addAnnotation(annotation)
            }
            if (locations.isNotEmpty()) {
                mapView.showAnnotations(mapView.annotations, animated = true)
            }
        }
    )
}

class AnchorAnnotation(val color: UIColor, val detailedText: String) : MKPointAnnotation()

class MapDelegate : NSObject(), MKMapViewDelegateProtocol {
    override fun mapView(mapView: MKMapView, viewForAnnotation: MKAnnotationProtocol): MKAnnotationView? {
        if (viewForAnnotation is AnchorAnnotation) {
            val identifier = "AnchorMarker"
            var view = mapView.dequeueReusableAnnotationViewWithIdentifier(identifier) as? MKMarkerAnnotationView
            
            if (view == null) {
                view = MKMarkerAnnotationView(annotation = viewForAnnotation, reuseIdentifier = identifier)
                view.canShowCallout = true
            } else {
                view.annotation = viewForAnnotation
            }
            
            view.markerTintColor = viewForAnnotation.color
            view.clusteringIdentifier = null // Explicitly disable clustering
            view.displayPriority = MKFeatureDisplayPriorityRequired
            // view.collisionMode = 2L as MKAnnotationViewCollisionMode // Causing crash
            
            val label = UILabel()
            label.numberOfLines = 0
            label.text = viewForAnnotation.detailedText
            label.font = UIFont.systemFontOfSize(12.0)
            view.detailCalloutAccessoryView = label
            
            return view
        }
        return null
    }
}