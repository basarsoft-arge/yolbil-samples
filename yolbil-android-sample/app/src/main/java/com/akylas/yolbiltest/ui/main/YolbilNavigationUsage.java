package com.akylas.yolbiltest.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.akylas.yolbiltest.ui.main.constants.BaseSettings;
import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.datasources.BlueDotDataSource;
import com.basarsoft.yolbil.graphics.Color;
import com.basarsoft.yolbil.layers.VectorLayer;
import com.basarsoft.yolbil.location.GPSLocationSource;
import com.basarsoft.yolbil.location.Location;
import com.basarsoft.yolbil.location.LocationBuilder;
import com.basarsoft.yolbil.location.LocationListener;
import com.basarsoft.yolbil.location.LocationSource;
import com.basarsoft.yolbil.location.LocationSourceSnapProxy;
import com.basarsoft.yolbil.navigation.AssetsVoiceNarrator;
import com.basarsoft.yolbil.navigation.CommandListener;
import com.basarsoft.yolbil.navigation.CommandListenerModuleJNI;
import com.basarsoft.yolbil.navigation.Narrator;
import com.basarsoft.yolbil.navigation.NavigationCommand;
import com.basarsoft.yolbil.navigation.RouteType;
import com.basarsoft.yolbil.navigation.VoiceNarrator;
import com.basarsoft.yolbil.navigation.YolbilNavigationBundle;
import com.basarsoft.yolbil.navigation.YolbilNavigationBundleBuilder;
import com.basarsoft.yolbil.projections.EPSG4326;
import com.basarsoft.yolbil.routing.NavigationResult;
import com.basarsoft.yolbil.routing.RoutingInstruction;
import com.basarsoft.yolbil.styles.LineEndType;
import com.basarsoft.yolbil.styles.LineJoinType;
import com.basarsoft.yolbil.styles.LineStyle;
import com.basarsoft.yolbil.styles.LineStyleBuilder;
import com.basarsoft.yolbil.ui.MapClickInfo;
import com.basarsoft.yolbil.ui.MapEventListener;
import com.basarsoft.yolbil.ui.MapView;
import com.basarsoft.yolbil.utils.AssetUtils;
import com.basarsoft.yolbil.utils.ZippedAssetPackage;
import com.basarsoft.yolbil.routing.NavigationResultVector;

import java.util.Locale;

public class YolbilNavigationUsage {
    private static final String TAG = "YolbilNavigationUsage";

    private MapView mapView;
    private LocationSourceSnapProxy snapLocationSourceProxy;
    private YolbilNavigationBundle bundle;
    private GPSLocationSource locationSource;
    private Location lastLocation = null;
    private NavigationResult navigationResult;
    private VectorLayer blueDotVectorLayer = null;

    private NavigationResultVector navigationResults = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private TextView navigationInfoText;

    public YolbilNavigationUsage(TextView navigationInfoText) {
        this.navigationInfoText = navigationInfoText;
    }
    @SuppressLint("MissingPermission")
    NavigationResult fullExample(MapView mapView, MapPos start, MapPos end, boolean isOffline, GPSLocationSource locationSource) {
        this.mapView = mapView;
        this.locationSource = locationSource;
        lastLocation = new Location();
        lastLocation.setCoordinate(start);

        mapView.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapClicked(MapClickInfo mapClickInfo) {
                locationSource.sendMockLocation(mapClickInfo.getClickPos());
            }
        });

        BlueDotDataSource blueDotDataSource = new BlueDotDataSource(new EPSG4326(), locationSource);
        snapLocationSourceProxy = new LocationSourceSnapProxy(blueDotDataSource);
        snapLocationSourceProxy.setMaxSnapDistanceMeter(500);
        this.addLocationSourceToMap(mapView);

        bundle = this.getNavigationBundle(isOffline);
        this.addNavigationToMapLayers(mapView);


        if (bundle != null) {
            navigationResults = bundle.startNavigation(start, end);
        } else {
            navigationResults = null;
        }
        if (navigationResults != null && navigationResults.size() > 0) {
            navigationResult = navigationResults.get(0);
        } else {
            navigationResult = null;
            Log.e(TAG, "Rota çizilemedi! navigationResults boş döndü.");
             return null; // rotasız devam edilmez
        }

        if (navigationResult != null) {
            snapLocationSourceProxy.setRoutingPoints(navigationResult.getPoints());
            mapView.fitRouteOnMap(navigationResult.getPoints());

            for (int i = 0; i < navigationResult.getInstructions().size(); i++) {
                RoutingInstruction rI = navigationResult.getInstructions().get(i);
                Log.e("Instruction", rI.getInstruction());
                Log.e("Instruction", rI.getAction().toString());
                rI.getGeometryTag().getObjectElement("commands").getArrayElement(0);
            }
        } else {
            Log.e(TAG, "NavigationResult is null! Navigation could not start.");
        }

        locationSource.sendMockLocation(start);
        return navigationResult;
    }

    void addLocationSourceToMap(MapView mapView) {
        BlueDotDataSource blueDotDataSource = new BlueDotDataSource(new EPSG4326(), snapLocationSourceProxy.getBlueDotDataSource().getLocationSource());
        blueDotVectorLayer = new VectorLayer(blueDotDataSource);
        mapView.getLayers().add(blueDotVectorLayer);
    }

    void updateLocation(MapPos mapPos) {
        Location newLocation = new Location();
        newLocation.setCoordinate(mapPos);
        snapLocationSourceProxy.updateLocation(newLocation);
    }

    void stopNavigation() {
        if (blueDotVectorLayer != null) mapView.getLayers().remove(blueDotVectorLayer);
        mapView.getLayers().removeAll(bundle.getLayers());
        bundle.stopNavigation();
    }

    YolbilNavigationBundle getNavigationBundle(boolean isOffline) {
        String baseUrl = BaseSettings.INSTANCE.getBASE_URL();
        String accountId = BaseSettings.INSTANCE.getAccountId();
        String appCode = BaseSettings.INSTANCE.getAppCode();
        YolbilNavigationBundleBuilder navigationBundleBuilder = new YolbilNavigationBundleBuilder(baseUrl, accountId, appCode, snapLocationSourceProxy.getBlueDotDataSource().getLocationSource(), RouteType.Car);
        navigationBundleBuilder.setOfflineEnabled(isOffline);
        navigationBundleBuilder.setBlueDotDataSourceEnabled(true);
        navigationBundleBuilder.setOfflineDataPath("/storage/emulated/0/yolbilxdata/TR.vtiles");
        //optional
        navigationBundleBuilder.setRequestEndpoint("/Service/api/v1/Routing/RouteAdvance");
        //custom line style set (optional)
        LineStyleBuilder lineStyleBuilder = new LineStyleBuilder();
        lineStyleBuilder.setColor(new Color((short) 148, (short) 148, (short) 148, (short) 100));
        lineStyleBuilder.setLineJoinType(LineJoinType.LINE_JOIN_TYPE_ROUND);
        lineStyleBuilder.setLineEndType(LineEndType.LINE_END_TYPE_ROUND);
        lineStyleBuilder.setWidth(5);
        LineStyle completedLineStyle = lineStyleBuilder.buildStyle();
        navigationBundleBuilder.setCompleteRouteLineStyle(completedLineStyle);

        // TODO
        //navigationBundleBuilder.setEndpoint();
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
            public boolean onNavigationWillRecalculate(){
                Log.e(TAG, "onNavigationWillRecalculate");
                return super.onNavigationWillRecalculate();
            }

            @Override
            public boolean onNavigationRecalculated(NavigationResult navigationResult) {
                Log.e(TAG, "onNavigationRecalculated: " + navigationResult);
                return super.onNavigationRecalculated(navigationResult);
            }

// Konum burada dinleniyor ve kalan mesafe , kalan süre değerleri alınıyor
            @Override
            public boolean onLocationChanged(NavigationCommand command) {
                double distanceToCommand = command.getTotalDistanceToCommand();
                double remainingTime = command.getRemainingTimeInSec();

                final String infoText = String.format(Locale.getDefault(), "Kalan Mesafe: %.1f m\nKalan Süre: %.0f sn", distanceToCommand, remainingTime);

                Log.e("NAV_INFO", infoText);
                if (navigationInfoText != null) {
                    mainHandler.post(() -> navigationInfoText.setText(infoText));
                } else {
                    Log.e("NAVIGATION", "navigationInfoText = ull");
                }

            return super.onLocationChanged(command);
            }
        }
        );

        // Below implementation expects CommandVoices.zip to be in the Android assets folder.
        navigationBundleBuilder.setNarrator(new AssetsVoiceNarrator(new ZippedAssetPackage(AssetUtils.loadAsset("CommandVoices.zip"))));
        return navigationBundleBuilder.build();
    }

    void addNavigationToMapLayers(MapView mapView) {
        mapView.getLayers().addAll(bundle.getLayers());
    }
    private boolean isFirstLocation = true;

    @SuppressLint("MissingPermission")
    void startNavigation() {
        if (navigationResult != null && mapView != null) {
            bundle.beginNavigation(navigationResult);
            mapView.setDeviceOrientationFocused(true);
            mapView.setFocusPos(lastLocation.getCoordinate(), 1.0f);
            mapView.setZoom(17, 1.0f);

            locationSource.addListener(new LocationListener() {
                @Override
                public void onLocationChange(Location location) {
                    if (isFirstLocation) {
                        mapView.setFocusPos(location.getCoordinate(), 1.0f);
                        mapView.setZoom(17, 1.0f);
                        mapView.setDeviceOrientationFocused(true);
                        isFirstLocation = false;
                    }
                }
            });
        }
    }

}
