package com.akylas.yolbiltest.ui.main;

import android.annotation.SuppressLint;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.akylas.yolbiltest.ui.main.constants.BaseSettings;
import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.core.MapPosVector;
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
import com.basarsoft.yolbil.navigation.NavigationCommand;
import com.basarsoft.yolbil.navigation.Navigation;
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

import java.util.ArrayList;
import java.util.List;
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
    private BlueDotDataSource blueDotDataSource = null;

    private NavigationResultVector navigationResults = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private static final long SIMULATION_STEP_MS = 1000L;
    private static final double SIMULATION_SPEED_KMH = 120.0;
    private Runnable simulationRunnable;
    private final List<MapPos> simulationPoints = new ArrayList<>();
    private int simulationIndex = 0;
    private boolean simulationRunning = false;
    private SimulationListener simulationListener;

    private TextView navigationInfoText;
    private boolean voiceGuidanceEnabled = true;
    private VoiceNarrator voiceNarrator;

    // UI tarafına kalan mesafe/süre yazabilmek için TextView referansı alır.
    public YolbilNavigationUsage(TextView navigationInfoText) {
        this.navigationInfoText = navigationInfoText;
    }
    // Harita üzerinde rota talep edip gerekli layer'ları hazırlar; NavigationResult döndürür.
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

        // SDK'nın varsayılan BlueDot ikonunu kullanabilmek için datasource'u init edip saklarız.
        blueDotDataSource = new BlueDotDataSource(new EPSG4326(), locationSource);
        blueDotDataSource.init();
        snapLocationSourceProxy = new LocationSourceSnapProxy(blueDotDataSource);
        snapLocationSourceProxy.setMaxSnapDistanceMeter(500);
        snapLocationSourceProxy.init();
        this.addLocationSourceToMap(mapView);

        bundle = this.getNavigationBundle(isOffline);
        applyVoiceGuidanceSetting();
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
            if (navigationResult.getPoints().size() >0){
                mapView.fitRouteOnMap(navigationResult.getPoints());

            }

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

    // Mavi nokta katmanını haritaya ekleyerek kullanıcı konumunun çizilmesini sağlar.
    void addLocationSourceToMap(MapView mapView) {
        if (blueDotDataSource == null && snapLocationSourceProxy != null) {
            blueDotDataSource = snapLocationSourceProxy.getBlueDotDataSource();
        }

        if (blueDotVectorLayer == null && blueDotDataSource != null) {
            blueDotVectorLayer = new VectorLayer(blueDotDataSource);
        }

        if (blueDotVectorLayer != null) {
            boolean layerExists = false;
            for (int i = 0; i < mapView.getLayers().count(); i++) {
                if (mapView.getLayers().get(i) == blueDotVectorLayer) {
                    layerExists = true;
                    break;
                }
            }
            if (!layerExists) {
                mapView.getLayers().add(blueDotVectorLayer);
            }
        }
    }

    // Harita üzerinde manuel konum güncellemesi yapmaya yarar (simülasyon/test için).
    void updateLocation(MapPos mapPos) {
        Location newLocation = new Location();
        newLocation.setCoordinate(mapPos);
        snapLocationSourceProxy.updateLocation(newLocation);
    }

    // Başlatılmış navigasyonu durdurup ilgili layer'ları temizler.
    void stopNavigation() {
        if (blueDotVectorLayer != null) mapView.getLayers().remove(blueDotVectorLayer);
        mapView.getLayers().removeAll(bundle.getLayers());
        bundle.stopNavigation();
        blueDotVectorLayer = null;
    }

    public interface SimulationListener {
        void onSimulationFinished();
    }

    public void setVoiceGuidanceEnabled(boolean enabled) {
        if (voiceGuidanceEnabled == enabled) {
            return;
        }
        voiceGuidanceEnabled = enabled;
        applyVoiceGuidanceSetting();
    }

    public synchronized boolean startSimulation(@Nullable SimulationListener listener) {
        if (simulationRunning) {
            Log.w(TAG, "startSimulation: Simulation already running");
            return false;
        }

        if (navigationResult == null || navigationResult.getPoints() == null || navigationResult.getPoints().size() <= 0) {
            Log.w(TAG, "startSimulation: No navigation result available");
            return false;
        }

        simulationPoints.clear();
        MapPosVector points = navigationResult.getPoints();
        List<MapPos> rawPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            rawPoints.add(points.get(i));
        }
        double speedMps = (SIMULATION_SPEED_KMH * 1000.0) / 3600.0;
        double spacingMeters = speedMps * (SIMULATION_STEP_MS / 1000.0);
        simulationPoints.addAll(resamplePolylineWgs84(rawPoints, spacingMeters));

        if (simulationPoints.size() < 2) {
            Log.w(TAG, "startSimulation: Not enough points to simulate");
            simulationPoints.clear();
            return false;
        }

        simulationListener = listener;
        simulationIndex = 0;
        simulationRunning = true;
        ensureDeviceOrientationFocus();
        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!simulationRunning) {
                    return;
                }

                if (simulationIndex >= simulationPoints.size()) {
                    stopSimulationInternal(true);
                    return;
                }

                MapPos mapPos = simulationPoints.get(simulationIndex++);
                double direction = Double.NaN;
                if (lastLocation != null && isCoordinateValid(lastLocation.getCoordinate())) {
                    direction = calculateBearing(lastLocation.getCoordinate(), mapPos);
                }
                if (locationSource != null) {
                    locationSource.sendMockLocation(mapPos);
                }
                if (lastLocation == null) {
                    lastLocation = new Location();
                }
                lastLocation.setCoordinate(mapPos);
                if (!Double.isNaN(direction)) {
                    lastLocation.setDirection(direction);
                }
                if (snapLocationSourceProxy != null) {
                    snapLocationSourceProxy.updateLocation(lastLocation);
                }
                
                followBlueDot(lastLocation, false);
                simulationHandler.postDelayed(this, SIMULATION_STEP_MS);
            }
        };
        simulationHandler.post(simulationRunnable);
        return true;
    }

    /**
     * İki nokta arasındaki coğrafi yöne (0-360 derece) ihtiyaç duyan simülasyon için bearing hesaplar.
     */
    private double calculateBearing(MapPos from, MapPos to) {
        double lat1 = Math.toRadians(from.getY());
        double lat2 = Math.toRadians(to.getY());
        double dLon = Math.toRadians(to.getX() - from.getX());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        bearing = (bearing + 360.0) % 360.0;
        return bearing;
    }

    public synchronized void stopSimulation() {
        stopSimulationInternal(true);
    }

    public boolean isSimulationRunning() {
        return simulationRunning;
    }

    private void stopSimulationInternal(boolean notifyListener) {
        if (!simulationRunning) {
            if (notifyListener && simulationListener != null) {
                simulationListener.onSimulationFinished();
            }
            simulationListener = null;
            return;
        }

        simulationRunning = false;
        simulationHandler.removeCallbacks(simulationRunnable);
        simulationRunnable = null;
        simulationPoints.clear();
        simulationIndex = 0;
        if (notifyListener && simulationListener != null) {
            simulationListener.onSimulationFinished();
        }
        simulationListener = null;
    }

    /**
     * MapView'in cihaz yön takip modunda olup olmadığını kontrol eder
     */
    private boolean ensureDeviceOrientationFocus() {
        if (mapView == null) {
            return false;
        }
        if (!mapView.isDeviceOrientationFocused()) {
            mapView.setDeviceOrientationFocused(true);
        }
        return true;
    }

    // YolbilNavigationBundle'ı çevrim içi/çevrim dışı ayarlarla hazırlar ve komut dinleyicilerini bağlar.
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

        if (voiceGuidanceEnabled) {
            VoiceNarrator narrator = ensureVoiceNarrator();
            if (narrator != null) {
                navigationBundleBuilder.setNarrator(narrator);
            }
        }
        return navigationBundleBuilder.build();
    }

    // YolbilNavigationBundle içindeki çizim katmanlarını MapView'a ekler.
    void addNavigationToMapLayers(MapView mapView) {
        mapView.getLayers().addAll(bundle.getLayers());
    }
    private boolean isFirstLocation = true;

    // Rota hazır olduğunda gerçek zamanlı navigasyonu başlatır ve haritayı kullanıcıya odaklar.
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

                    lastLocation = location;
                    //Bluedot takibi sağlanır
                    followBlueDot(location, isFirstLocation);
                    if (isFirstLocation) {
                        isFirstLocation = false;
                    }
                }
            });
        }
    }

    /**
     * Haritayı mevcut BlueDot konumuna kilitleyerek kullanıcının "Ortala" isteğini yerine getirir.
     * Önce snap proxy'den veri almaya çalışır
     */
    public boolean focusOnCurrentPosition() {
        if (!ensureDeviceOrientationFocus()) {
            return false;
        }
        boolean handled = followBlueDot(lastLocation, true);
        if (!handled && mapView != null && lastLocation != null && isCoordinateValid(lastLocation.getCoordinate())) {
            mapView.setFocusPos(lastLocation.getCoordinate(), 0.6f);
            mapView.setZoom(17, 0.6f);
            return true;
        }
        return handled;
    }

    /**
     * BlueDot snap koordinatlarını veya verilen lokasyonu kullanarak haritayı takip modunda tutar.
     * Yön bilgisi ile birlikte kamera rotasyonunu günceller
     */
    private boolean followBlueDot(@Nullable Location newLocation, boolean initialFocus) {
        if (mapView == null || !mapView.isDeviceOrientationFocused()) {
            return false;
        }
        MapPos focusPos = null;
        if (snapLocationSourceProxy != null) {
            focusPos = snapLocationSourceProxy.getShiftedCoordinate();
        }
        if (!isCoordinateValid(focusPos) && newLocation != null) {
            MapPos fallback = newLocation.getCoordinate();
            if (isCoordinateValid(fallback)) {
                focusPos = fallback;
            }
        }
        if (!isCoordinateValid(focusPos)) {
            return false;
        }
        mapView.setFocusPos(focusPos, 0.6f);
        if (snapLocationSourceProxy != null && snapLocationSourceProxy.getLastLocation() != null) {
            float direction = (float) snapLocationSourceProxy.getLastLocation().getDirection();
            mapView.setMapRotation(direction, 0.6f);
        }
        if (initialFocus) {
            mapView.setZoom(17, 0.6f);
            mapView.setTilt(45.0f, 0.6f);
        }
        return true;
    }

    // Haversine ile iki nokta arasındaki mesafeyi (metre) hesaplar.
    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Great-circle interpolasyonu ile iki nokta arasında f oranında ara nokta bulur.
    private MapPos interpolateGreatCircle(MapPos from, MapPos to, double fraction) {
        double phi1 = Math.toRadians(from.getY());
        double lambda1 = Math.toRadians(from.getX());
        double phi2 = Math.toRadians(to.getY());
        double lambda2 = Math.toRadians(to.getX());

        double sinPhi1 = Math.sin(phi1);
        double cosPhi1 = Math.cos(phi1);
        double sinPhi2 = Math.sin(phi2);
        double cosPhi2 = Math.cos(phi2);

        double delta = 2 * Math.asin(Math.sqrt(
                Math.sin((phi2 - phi1) / 2) * Math.sin((phi2 - phi1) / 2)
                        + Math.cos(phi1) * Math.cos(phi2)
                        * Math.sin((lambda2 - lambda1) / 2) * Math.sin((lambda2 - lambda1) / 2)
        ));
        if (delta == 0.0) {
            return new MapPos(from.getX(), from.getY());
        }

        double A = Math.sin((1 - fraction) * delta) / Math.sin(delta);
        double B = Math.sin(fraction * delta) / Math.sin(delta);

        double x = A * cosPhi1 * Math.cos(lambda1) + B * cosPhi2 * Math.cos(lambda2);
        double y = A * cosPhi1 * Math.sin(lambda1) + B * cosPhi2 * Math.sin(lambda2);
        double z = A * sinPhi1 + B * sinPhi2;

        double phi = Math.atan2(z, Math.sqrt(x * x + y * y));
        double lambda = Math.atan2(y, x);
        return new MapPos(Math.toDegrees(lambda), Math.toDegrees(phi));
    }

    // Polyline'ı verilen metre aralığında yeniden örnekler.
    private List<MapPos> resamplePolylineWgs84(List<MapPos> points, double spacingMeters) {
        if (points.size() < 2) {
            return new ArrayList<>(points);
        }
        List<MapPos> out = new ArrayList<>();
        MapPos prev = points.get(0);
        out.add(prev);
        double acc = 0.0;
        double nextTarget = spacingMeters;

        for (int i = 0; i < points.size() - 1; i++) {
            MapPos a = prev;
            MapPos b = points.get(i + 1);
            double segLen = haversineMeters(a.getY(), a.getX(), b.getY(), b.getX());
            MapPos start = a;
            while (acc + segLen >= nextTarget) {
                double remain = nextTarget - acc;
                double fraction = Math.min(Math.max(remain / segLen, 0.0), 1.0);
                MapPos np = interpolateGreatCircle(start, b, fraction);
                out.add(np);
                start = np;
                segLen = haversineMeters(start.getY(), start.getX(), b.getY(), b.getX());
                acc = nextTarget;
                nextTarget += spacingMeters;
            }
            acc += segLen;
            prev = b;
        }
        MapPos last = points.get(points.size() - 1);
        if (out.get(out.size() - 1).getX() != last.getX() || out.get(out.size() - 1).getY() != last.getY()) {
            out.add(last);
        }
        return out;
    }

    /**
     * Gelen MapPos'un gerçek bir koordinata karşılık gelip gelmediğini kontrol eder.
     */
    private boolean isCoordinateValid(@Nullable MapPos mapPos) {
        if (mapPos == null) {
            return false;
        }
        double x = mapPos.getX();
        double y = mapPos.getY();
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return false;
        }
        return !(Math.abs(x) < 1e-6 && Math.abs(y) < 1e-6);
    }

    // Sesli yönlendirmeyi çalışırken açıp kapatmak için navigator'a narrator atama/temizleme.
    private void applyVoiceGuidanceSetting() {
        if (bundle == null) {
            return;
        }
        try {
            Navigation navigation = bundle.getNavigation();
            if (navigation == null) {
                return;
            }
            if (voiceGuidanceEnabled) {
                VoiceNarrator narrator = ensureVoiceNarrator();
                navigation.setNarrator(narrator);
            } else {
                navigation.setNarrator(null);
            }
        } catch (Exception e) {
            Log.w(TAG, "Voice guidance toggle not applied at runtime: " + e.getMessage());
        }
    }

    // CommandVoices.zip paketini okuyup narrator'ı ilk ihtiyaçta yaratır, sonraki isteklerde aynı nesneyi döndürür.
    private VoiceNarrator ensureVoiceNarrator() {
        if (voiceNarrator == null) {
            try {
                voiceNarrator = new AssetsVoiceNarrator(new ZippedAssetPackage(AssetUtils.loadAsset("CommandVoices.zip")));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load narrator asset", e);
            }
        }
        return voiceNarrator;
    }

}
