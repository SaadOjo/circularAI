package com.example.circularai

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import com.mapbox.geojson.Point
import com.mapbox.geojson.Point.fromLngLat
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location

class map_fragment : Fragment(R.layout.fragment_map) {

    lateinit var mapView:MapView

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView = view.findViewById<MapView>(R.id.mapView)
        mapView?.getMapboxMap()?.loadStyleUri(Style.MAPBOX_STREETS,
            object : Style.OnStyleLoaded {
                override fun onStyleLoaded(style: Style) {
                    mapView.location.updateSettings {
                        enabled = true
                        pulsingEnabled = true
                    }
                }
            })
        mapView.location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)

        val annotationApi = mapView?.annotations
        val circleAnnotationManager = annotationApi?.createCircleAnnotationManager()

        // Add the resulting circle to the map.





        circleAnnotationManager?.create(add_point(fromLngLat(12.47746472271569, 41.86886108617344)))
        circleAnnotationManager?.create(add_point(fromLngLat(12.476025981538717, 41.89546142333354)))
        circleAnnotationManager?.create(add_point(fromLngLat(12.526087842521806, 41.88615795070312)))
        circleAnnotationManager?.create(add_point(fromLngLat(12.49244518056611, 41.90179692675658)))

        circleAnnotationManager?.create(add_point(fromLngLat(29.012303842239632, 41.068197827883125)))
        circleAnnotationManager?.create(add_point(fromLngLat(29.012475503606193, 41.071085455865855)))
        circleAnnotationManager?.create(add_point(fromLngLat(29.02866891724044, 41.10781465884691)))
        circleAnnotationManager?.create(add_point(fromLngLat(29.020334827846952, 41.1049425685896)))


    }
    fun add_point(p:Point): CircleAnnotationOptions {
        val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
            .withPoint(p)
            // Style the circle that will be added to the map.
            .withCircleRadius(8.0)
            .withCircleColor("#ee4e8b")
            .withCircleStrokeWidth(2.0)
            .withCircleStrokeColor("#ffffff")

        return circleAnnotationOptions
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView?.onDestroy()

    }

}
