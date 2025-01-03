package com.example.yolbil_jetpack_sample

import android.annotation.SuppressLint
import android.util.Log
import com.basarsoft.yolbil.core.MapPos
import com.basarsoft.yolbil.datasources.BlueDotDataSource
import com.basarsoft.yolbil.layers.VectorLayer
import com.basarsoft.yolbil.location.GPSLocationSource
import com.basarsoft.yolbil.location.Location
import com.basarsoft.yolbil.location.LocationListener
import com.basarsoft.yolbil.location.LocationSourceSnapProxy
import com.basarsoft.yolbil.navigation.CommandListener
import com.basarsoft.yolbil.navigation.NavigationCommand
import com.basarsoft.yolbil.navigation.YolbilNavigationBundle
import com.basarsoft.yolbil.navigation.YolbilNavigationBundleBuilder
import com.basarsoft.yolbil.projections.EPSG4326
import com.basarsoft.yolbil.routing.NavigationResult
import com.basarsoft.yolbil.ui.MapView


class YolbilNavigationUsage {

    val TAG: String = "YolbilNavigationUsage"

    private var mapView: MapView? = null
    private var snapLocationSourceProxy: LocationSourceSnapProxy? = null
    private var bundle: YolbilNavigationBundle? = null
    private var locationSource: GPSLocationSource? = null
    private var lastLocation: Location? = null
    private var navigationResult: NavigationResult? = null
    private val blueDotVectorLayer: VectorLayer? = null

    @SuppressLint("MissingPermission")
    fun fullExample(
        mapView: MapView,
        start: MapPos?,
        end: MapPos?,
        isOffline: Boolean,
        locationSource: GPSLocationSource
    ): NavigationResult? {
        // Initialize location source
        this.mapView = mapView

        mapView.setFocusPos(start, 0f)

        // GPSLocationSource automatically implements Android specific GPS TODO iOS?
        this.locationSource = locationSource
        locationSource.addListener(object : LocationListener() {
            override fun onLocationChange(location: Location) {
                // Update map focus position if necessary
            }
        })

       /* mapView.mapEventListener = object : MapEventListener() {
            override fun onMapClicked(mapClickInfo: MapClickInfo) {
                locationSource.sendMockLocation(mapClickInfo.clickPos)
            }
        }*/

        snapLocationSourceProxy = LocationSourceSnapProxy(locationSource)
        snapLocationSourceProxy?.setMaxSnapDistanceMeter(500.0)
        lastLocation = Location()
        lastLocation!!.setCoordinate(start)
        this.addLocationSourceToMap(mapView)
        bundle = this.getNavigationBundle(isOffline)
        this.addNavigationToMapLayers(mapView)

        val localBundle = bundle // Use a local variable for null safety

        if (localBundle != null) {
            navigationResult = if (isOffline) {
                localBundle.startOfflineNavigation(start, end)
            } else {
                localBundle.startNavigation(start, end)
            }

            snapLocationSourceProxy?.setRoutingPoints(navigationResult!!.points)

            for (i in 0 until navigationResult!!.instructions.size()) {
                val rI = navigationResult!!.instructions[i.toInt()]
                Log.e("Instruction", rI.instruction)
                Log.e("Instruction", rI.action.toString())
                rI.geometryTag.getObjectElement("commands").getArrayElement(0)
            }
            locationSource.sendMockLocation(start)
        } else {
            Log.e(TAG, "Navigation bundle is null")
        }
        mapView.fitRouteOnMap(navigationResult!!.getPoints());

        return navigationResult
    }

    fun addLocationSourceToMap(mapView: MapView) {
        val blueDotDataSource = BlueDotDataSource(EPSG4326(), snapLocationSourceProxy?.locationSource)
        val blueDotVectorLayer = VectorLayer(blueDotDataSource)
        mapView.layers.add(blueDotVectorLayer)
    }

    fun updateLocation(mapPos: MapPos?) {
        val newLocation = Location()
        newLocation.coordinate = mapPos
        snapLocationSourceProxy?.updateLocation(newLocation)
    }

    fun stopNavigation() {
        if (blueDotVectorLayer != null) mapView!!.layers.remove(blueDotVectorLayer)
        mapView!!.layers.removeAll(bundle!!.layers)
        bundle!!.stopNavigation()
    }

    fun getNavigationBundle(isOffline: Boolean): YolbilNavigationBundle {
        val baseUrl = "bms.basarsoft.com.tr"
        val accountId = "YOUR_ACC_ID"
        val appCode = "YOUR_APP_CODE"
        val navigationBundleBuilder = YolbilNavigationBundleBuilder(
            baseUrl, accountId, appCode, snapLocationSourceProxy?.locationSource
        )

        navigationBundleBuilder.setBlueDotDataSourceEnabled(true)
        navigationBundleBuilder.setOfflineEnabled(isOffline)
        navigationBundleBuilder.setOfflineDataPath("/storage/emulated/0/yolbilxdata/TR.vtiles")
        navigationBundleBuilder.setCommandListener(object : CommandListener() {
            override fun onNavigationStarted(): Boolean {
                Log.e(TAG, "onNavigationStarted: navigation started")
                return super.onNavigationStarted()
            }

            override fun onCommandReady(command: NavigationCommand): Boolean {
                Log.e(TAG, "onCommandReady: $command")
                return super.onCommandReady(command)
            }

            override fun onNavigationRecalculated(navigationResult: NavigationResult): Boolean {
                Log.e(TAG, "onNavigationRecalculated: $navigationResult")
                return super.onNavigationRecalculated(navigationResult)
            }
        })


        return navigationBundleBuilder.build()
    }

    fun addNavigationToMapLayers(mapView: MapView) {
        bundle?.layers?.let { mapView.layers.addAll(it) }
    }

    @SuppressLint("MissingPermission")
    fun startNavigation() {
        if (navigationResult != null && mapView != null) {
            bundle!!.beginNavigation(navigationResult)
            mapView!!.setDeviceOrientationFocused(true)
            mapView!!.setFocusPos(lastLocation!!.getCoordinate() , 1.0f)
            mapView!!.setZoom(17f, 1.0f)
            locationSource?.addListener(object : LocationListener() {
                override fun onLocationChange(location: Location) {
                    mapView!!.setDeviceOrientationFocused(true)
                    mapView!!.setZoom(17f, 1.0f)
                    mapView!!.setFocusPos(location.coordinate, 1f)
                }
            })
        }
    }
}