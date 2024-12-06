package com.example.yolbil_jetpack_sample

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Button
import androidx.lifecycle.ViewModel
import com.basarsoft.inavi.libs.sensormanager.SensorManager
import com.basarsoft.yolbil.core.MapPos
import com.basarsoft.yolbil.datasources.HTTPTileDataSource
import com.basarsoft.yolbil.layers.RasterTileLayer
import com.basarsoft.yolbil.layers.TileLoadListener
import com.basarsoft.yolbil.location.GPSLocationSource
import com.basarsoft.yolbil.location.Location
import com.basarsoft.yolbil.location.LocationListener
import com.basarsoft.yolbil.projections.EPSG4326
import com.basarsoft.yolbil.ui.MapView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject



@HiltViewModel
class YolbilViewModel @Inject constructor() : ViewModel() {

    // Define appCode and accId at the top for easier configuration
    private val appCode = "YOUR_APP_CODE"  // Replace with actual appCode.
    private val accId = "YOUR_ACCID"      // Replace with actual accId

    private var mapView: MapView? = null

    var gpsLocationSource: GPSLocationSource? = null
    var lastLocation: Location? = null
    var focusPos: Button? = null
    var startNavigation: android.widget.Button? = null
    var isLocationFound: Boolean = false
    private val navigationUsage:YolbilNavigationUsage  = YolbilNavigationUsage()
    var startPos:MapPos? = null
    var endPos:MapPos? = null

    @SuppressLint("MissingPermission")
    fun initializeMapView(mapView: MapView) {

        this.mapView = mapView
        this.startPos = MapPos(32.8597, 39.9334) // Kızılay, Ankara

        this.endPos = MapPos(32.8547, 39.9250)   // Anıtkabir, Ankara

        // Set map projection and initial position
        mapView.options.baseProjection = EPSG4326()
        mapView.setFocusPos(MapPos(32.836262, 39.960160), 0f)
        mapView.setZoom(17.0f, 0.0f)

        //IMPORTANT! HACK FOR COMPOSE
        Log.e("sensormanager", SensorManager.getModuleInfo())

        // Define tile data source using appCode and accId
        val tileDataSource = HTTPTileDataSource(
            0, 18, "https://bms.basarsoft.com.tr/service/api/v1/map/Default?appcode=$appCode&accid=$accId&x={x}&y={y}&z={zoom}"
        )

        // Create and add tile layer to MapView
        val rasterLayer = RasterTileLayer(tileDataSource)
        mapView.layers.add(rasterLayer)

        // Monitor tile load events
        rasterLayer.tileLoadListener = object : TileLoadListener() {
            override fun onVisibleTilesLoaded() {
                Log.d("YolbilViewModel", "Visible tiles loaded")
            }
        }

        gpsLocationSource = GPSLocationSource(mapView.context)
        gpsLocationSource!!.startLocationUpdates()
        gpsLocationSource!!.addListener(object : LocationListener() {
            override fun onLocationChange(location: Location) {
                if (!isLocationFound) {
                    lastLocation = location
                    createRoute()
                    isLocationFound = true
                }
                //mapViewObject.setDeviceOrientationFocused(true);
                //mapViewObject.setFocusPos(location.getCoordinate(), 1);
            }
        })

    }

    fun startNavigation(){
        mapView!!.isDeviceOrientationFocused = true
        navigationUsage.startNavigation()
    }
    fun createRoute() {
        // Check if dependencies are initialized

        if (!isLocationFound) {
            isLocationFound = true;
            val mapView = this.mapView
            if (mapView == null) {
                Log.e("YolbilViewModel", "MapView not initialized")
                return
            }

            //for offline routing
            val isOffline = false;
            // Use YolbilNavigationUsage to create and display the route
            val navigationResult = navigationUsage.fullExample(
                mapView,
                lastLocation!!.coordinate,
                this.endPos,
                isOffline,
                gpsLocationSource!!
            )

            if (navigationResult != null) {
                val routePoints = navigationResult.points

                // Check if `routePoints` has a size method or equivalent
                val pointsCount = try {
                    routePoints.size() // Replace with actual method if not `size()`
                } catch (e: Exception) {
                    Log.e("YolbilViewModel", "Error accessing route points size: ${e.message}")
                    return
                }

                Log.d(
                    "YolbilViewModel",
                    "Route created successfully with $pointsCount points"
                )

                // Further processing, such as displaying route details or instructions
            } else {
                Log.e("YolbilViewModel", "Failed to create route. NavigationResult is null.")
            }
        }
    }




}