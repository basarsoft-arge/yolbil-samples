package com.akylas.yolbiltest.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;
import android.util.Log;

import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.datasources.BlueDotDataSource;
import com.basarsoft.yolbil.layers.VectorLayer;
import com.basarsoft.yolbil.location.GPSLocationSource;
import com.basarsoft.yolbil.location.Location;
import com.basarsoft.yolbil.location.LocationBuilder;
import com.basarsoft.yolbil.location.LocationListener;
import com.basarsoft.yolbil.location.LocationSource;
import com.basarsoft.yolbil.location.LocationSourceSnapProxy;
import com.basarsoft.yolbil.navigation.AssetsVoiceNarrator;
import com.basarsoft.yolbil.navigation.CommandListener;
import com.basarsoft.yolbil.navigation.Narrator;
import com.basarsoft.yolbil.navigation.NavigationCommand;
import com.basarsoft.yolbil.navigation.VoiceNarrator;
import com.basarsoft.yolbil.navigation.YolbilNavigationBundle;
import com.basarsoft.yolbil.navigation.YolbilNavigationBundleBuilder;
import com.basarsoft.yolbil.projections.EPSG4326;
import com.basarsoft.yolbil.routing.NavigationResult;
import com.basarsoft.yolbil.routing.RoutingInstruction;
import com.basarsoft.yolbil.ui.MapClickInfo;
import com.basarsoft.yolbil.ui.MapEventListener;
import com.basarsoft.yolbil.ui.MapView;
import com.basarsoft.yolbil.utils.AssetUtils;
import com.basarsoft.yolbil.utils.ZippedAssetPackage;

public class YolbilNavigationUsage {
    private static final String TAG = "YolbilNavigationUsage";

    private LocationSourceSnapProxy snapLocationSourceProxy;
    private YolbilNavigationBundle bundle;

    @SuppressLint("MissingPermission")
    NavigationResult fullExample(MapView mapView, MapPos start, MapPos end, boolean isOffline) {
        // GPSLocationSource automatically implements Android specific GPS TODO iOS?
        GPSLocationSource locationSource = new GPSLocationSource(mapView.getContext());
        //locationSource.startLocationUpdates();
        //start = locationSource.getLastLocation().getCoordinate();
        mapView.setFocusPos(start, 0);
        locationSource.addListener(new LocationListener(){
            @Override
            public void onLocationChange(Location location) {
                //mapView.setFocusPos(location.getCoordinate(), 1);
            }
        });

        mapView.setMapEventListener(new MapEventListener() {

            @Override
            public void onMapClicked(MapClickInfo mapClickInfo) {
                locationSource.sendMockLocation(mapClickInfo.getClickPos());
            }
        });

        snapLocationSourceProxy = new LocationSourceSnapProxy(locationSource);
        snapLocationSourceProxy.setMaxSnapDistanceMeter(500);
        this.addLocationSourceToMap(mapView);
        bundle = this.getNavigationBundle(isOffline);
        this.addNavigationToMapLayers(mapView);
        NavigationResult navigationResult = null;
        if(isOffline)
            navigationResult = bundle.startOfflineNavigation(start, end);
        else
            navigationResult = bundle.startNavigation(start,end);
        snapLocationSourceProxy.setRoutingPoints(navigationResult.getPoints());

        for(int i = 0 ; i < navigationResult.getInstructions().size(); i++){
            RoutingInstruction rI = navigationResult.getInstructions().get(i);
            Log.e("Instruction", rI.getInstruction());
            Log.e("Instruction", rI.getAction().toString());
            rI.getGeometryTag().getObjectElement("commands").getArrayElement(0);
        }
        locationSource.sendMockLocation(start);
        return navigationResult;
    }

    void addLocationSourceToMap(MapView mapView) {
        BlueDotDataSource blueDotDataSource =
                new BlueDotDataSource(new EPSG4326(), snapLocationSourceProxy.getLocationSource());
        VectorLayer blueDotVectorLayer = new VectorLayer(blueDotDataSource);
        mapView.getLayers().add(blueDotVectorLayer);
    }

    void updateLocation(MapPos mapPos) {
        Location newLocation = new Location();
        newLocation.setCoordinate(mapPos);
        snapLocationSourceProxy.updateLocation(newLocation);
    }
    void stopNavigation(){
        bundle.stopNavigation();
    }

    YolbilNavigationBundle getNavigationBundle(boolean isOffline) {
        String baseUrl = "bms.basarsoft.com.tr";
        String accountId = "YOUR_ACC_ID";
        String appCode = "YOUR_APP_CODE";
        YolbilNavigationBundleBuilder navigationBundleBuilder = new YolbilNavigationBundleBuilder(
                baseUrl, accountId, appCode, snapLocationSourceProxy.getLocationSource()
        );
        navigationBundleBuilder.setOfflineEnabled(isOffline);
        navigationBundleBuilder.setBlueDotDataSourceEnabled(true);
        navigationBundleBuilder.setOfflineDataPath("/storage/emulated/0/yolbilxdata/TR.vtiles");
        // TODO
//        navigationBundleBuilder.setEndpoint();
        navigationBundleBuilder.setCommandListener(new CommandListener() {
            @Override
            public boolean onNavigationStarted() {
                Log.e(TAG, "onNavigationStarted: navigation started");
                return super.onNavigationStarted();
            }

            @Override
            public boolean onCommandReady(NavigationCommand command) {
                Log.e(TAG, "onCommandReady: " + command);
                return super.onCommandReady(command);
            }

            @Override
            public boolean onNavigationRecalculated(NavigationResult navigationResult) {
                Log.e(TAG, "onNavigationRecalculated: " + navigationResult);
                return super.onNavigationRecalculated(navigationResult);
            }
        });

        // Below implementation expects CommandVoices.zip to be in the Android assets folder.
        navigationBundleBuilder.setNarrator(
                new AssetsVoiceNarrator(
                        new ZippedAssetPackage(
                                AssetUtils.loadAsset("CommandVoices.zip"))));
        return navigationBundleBuilder.build();
    }

    void addNavigationToMapLayers(MapView mapView) {
        mapView.getLayers().addAll(bundle.getLayers());
    }

}
