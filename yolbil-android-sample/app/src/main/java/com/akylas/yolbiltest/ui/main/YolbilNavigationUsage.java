package com.akylas.yolbiltest.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.akylas.yolbiltest.R;
import com.akylas.yolbiltest.ui.main.constants.BaseSettings;
import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.datasources.BlueDotDataSource;
import com.basarsoft.yolbil.graphics.Color;
import com.basarsoft.yolbil.layers.VectorLayer;
import com.basarsoft.yolbil.location.GPSLocationSource;
import com.basarsoft.yolbil.location.Location;
import com.basarsoft.yolbil.location.LocationListener;
import com.basarsoft.yolbil.location.LocationSource;
import com.basarsoft.yolbil.location.LocationSourceSnapProxy;
import com.basarsoft.yolbil.navigation.AssetsVoiceNarrator;
import com.basarsoft.yolbil.navigation.CommandListener;
import com.basarsoft.yolbil.navigation.NavigationCommand;
import com.basarsoft.yolbil.navigation.Navigation;
import com.basarsoft.yolbil.navigation.RouteType;
import com.basarsoft.yolbil.navigation.TurnCommandEnum;
import com.basarsoft.yolbil.navigation.TurnCommandEnumVector;
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
    private int totalInstructionCount = 0;
    private VectorLayer blueDotVectorLayer = null;
    private BlueDotDataSource blueDotDataSource = null;

    private NavigationResultVector navigationResults = null;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final NavigationSimulationHelper simulationHelper;

    private final NavigationInfoCardView navigationInfoCardView;
    private final Context context;
    private boolean voiceGuidanceEnabled = true;
    private VoiceNarrator voiceNarrator;
    private String lastRouteMessage = null;
    private String currentManifestText = "";
    private String currentCommandName = "";
    private String currentDirectionText = "";
    private int currentCommandIconRes = R.drawable.go_straight;
    private int currentStaticIndex = -1;
    private String currentTotalDistanceText = "";
    private String currentTotalTimeText = "";
    private int currentSpeedKmh = 0;
    private int currentSpeedLimitKmh = 0;
    private boolean mockGpsEnabled = false;

    // UI tarafındaki NavigationInfoCard bileşenini güncelleyebilmek için referans alır.
    public YolbilNavigationUsage(NavigationInfoCardView navigationInfoCardView, Context context) {
        this.navigationInfoCardView = navigationInfoCardView;
        this.context = context.getApplicationContext();
        this.currentManifestText = this.context.getString(R.string.navigation_card_default_manifest);
        this.currentDirectionText = this.context.getString(R.string.keep_going_straight);
        this.currentTotalDistanceText = this.context.getString(R.string.navigation_total_distance_placeholder);
        this.currentTotalTimeText = this.context.getString(R.string.navigation_total_time_placeholder);
        this.simulationHelper = new NavigationSimulationHelper(TAG, new NavigationSimulationHelper.SimulationHost() {
            @Nullable
            @Override
            public NavigationResult getNavigationResult() {
                return navigationResult;
            }

            @Nullable
            @Override
            public Location getLastLocation() {
                return lastLocation;
            }

            @Override
            public void updateLastLocation(Location location) {
                lastLocation = location;
            }

            @Nullable
            @Override
            public GPSLocationSource getLocationSource() {
                return locationSource;
            }

            @Nullable
            @Override
            public LocationSourceSnapProxy getSnapLocationSourceProxy() {
                return snapLocationSourceProxy;
            }

            @Override
            public boolean ensureDeviceOrientationFocus() {
                return YolbilNavigationUsage.this.ensureDeviceOrientationFocus();
            }

            @Override
            public boolean followBlueDot(@Nullable Location location, boolean initialFocus) {
                return YolbilNavigationUsage.this.followBlueDot(location, initialFocus);
            }
        });
    }
    // Harita üzerinde rota talep edip gerekli layer'ları hazırlar; NavigationResult döndürür.
    @SuppressLint("MissingPermission")
    NavigationResult fullExample(MapView mapView, MapPos start, MapPos end, boolean isOffline, GPSLocationSource locationSource, BlueDotDataSource sharedBlueDotDataSource) {
        if (start == null || end == null) {
            lastRouteMessage = "Başlangıç veya varış noktası eksik.";
            Log.e(TAG, "fullExample: start or end is null");
            return null;
        }
        this.mapView = mapView;
        this.locationSource = locationSource;
        lastLocation = new Location();
        lastLocation.setCoordinate(start);

        mapView.setMapEventListener(new MapEventListener() {
            @Override
            public void onMapClicked(MapClickInfo mapClickInfo) {
                if (mockGpsEnabled) {
                    locationSource.sendMockLocation(mapClickInfo.getClickPos());
                }
            }
        });

        // Tüm navigasyonlar shared BlueDotDataSource üzerinden devam eder.
        if (sharedBlueDotDataSource == null) {
            Log.e(TAG, "fullExample: blue dot data source missing");
            return null;
        }
        blueDotDataSource = sharedBlueDotDataSource;
        if (snapLocationSourceProxy != null) {
            snapLocationSourceProxy.delete();
        }
        snapLocationSourceProxy = new LocationSourceSnapProxy(blueDotDataSource);
        snapLocationSourceProxy.init();
        snapLocationSourceProxy.setMaxSnapDistanceMeter(50.0);
        mapView.post(() -> {
            if (snapLocationSourceProxy != null && mapView != null) {
                snapLocationSourceProxy.setmoveToHeadingDistance(50.0, mapView.getWidth(), mapView.getHeight());
            }
        });
        this.addLocationSourceToMap(mapView);

        bundle = this.getNavigationBundle(isOffline);
        applyVoiceGuidanceSetting();
        this.addNavigationToMapLayers(mapView);


        if (bundle != null) {
            try {
                navigationResults = bundle.startNavigation(start, end);
            } catch (Exception startEx) {
                Log.e(TAG, "startNavigation failed", startEx);
                lastRouteMessage = startEx.getMessage();
                navigationResults = null;
            }
        } else {
            lastRouteMessage = lastRouteMessage != null ? lastRouteMessage : "Navigasyon paketi oluşturulamadı.";
            Log.e(TAG, "fullExample: getNavigationBundle returned null");
            navigationResults = null;
        }
        if (navigationResults != null && navigationResults.size() > 0) {
            navigationResult = navigationResults.get(0);
            if (navigationResult != null && navigationResult.getInstructions() != null) {
                totalInstructionCount = (int) navigationResult.getInstructions().size();
            } else {
                totalInstructionCount = 0;
            }
        } else {
            navigationResult = null;
            totalInstructionCount = 0;
            Log.e(TAG, "Rota çizilemedi! navigationResults boş döndü.");
             return null; // rotasız devam edilmez
        }

        if (navigationResult != null) {
            snapLocationSourceProxy.setRoutingPoints(navigationResult.getPoints());
            int pointCount = (int) navigationResult.getPoints().size();
            if (pointCount == 0) {
                lastRouteMessage = "Rota noktası boş döndü.";
                Log.e(TAG, "Navigation points empty; skipping fitRouteOnMap/beginNavigation.");
                navigationResult = null;
                return null;
            }
            try {
                mapView.fitRouteOnMap(navigationResult.getPoints());
            } catch (Exception fitEx) {
                lastRouteMessage = fitEx.getMessage();
                Log.e(TAG, "fitRouteOnMap failed", fitEx);
                return null;
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

        try {
            locationSource.sendMockLocation(start);
        } catch (Exception mockEx) {
            lastRouteMessage = mockEx.getMessage();
            Log.e(TAG, "sendMockLocation failed", mockEx);
        }
        return navigationResult;
    }

    // Mavi nokta katmanını haritaya ekleyerek kullanıcı konumunun çizilmesini sağlar.
    void addLocationSourceToMap(MapView mapView) {
        if (mapView == null) {
            Log.w(TAG, "addLocationSourceToMap: mapView is null");
            return;
        }
        if (blueDotDataSource == null) {
            if (snapLocationSourceProxy != null) {
                blueDotDataSource = snapLocationSourceProxy.getBlueDotDataSource();
            } else {
                Log.w(TAG, "addLocationSourceToMap: blueDotDataSource is null and snap proxy unavailable");
                return;
            }
        }
        if (blueDotVectorLayer == null) {
            blueDotVectorLayer = new VectorLayer(blueDotDataSource);
        }

        for (int i = 0; i < mapView.getLayers().count(); i++) {
            if (mapView.getLayers().get(i) instanceof VectorLayer) {
                VectorLayer existingLayer = (VectorLayer) mapView.getLayers().get(i);
                if (existingLayer == blueDotVectorLayer || existingLayer.getDataSource() == blueDotDataSource) {
                    return;
                }
            }
        }
        mapView.getLayers().add(blueDotVectorLayer);
    }

    // Harita üzerinde manuel konum güncellemesi yapmaya yarar (simülasyon/test için).
    void updateLocation(MapPos mapPos) {
        Location newLocation = new Location();
        newLocation.setCoordinate(mapPos);
        snapLocationSourceProxy.updateLocation(newLocation);
    }

    // Başlatılmış navigasyonu durdurup ilgili layer'ları temizler.
    void stopNavigation() {
        if (mapView != null) {
            try {
                if (blueDotVectorLayer != null) {
                    mapView.getLayers().remove(blueDotVectorLayer);
                }
            } catch (Exception removeBlueDotEx) {
                Log.e(TAG, "BlueDot layer remove failed", removeBlueDotEx);
            }
            try {
                if (bundle != null) {
                    mapView.getLayers().removeAll(bundle.getLayers());
                }
            } catch (Exception removeRouteEx) {
                Log.e(TAG, "Route layers remove failed", removeRouteEx);
            }
        }
        try {
            if (bundle != null) {
                bundle.stopNavigation();
            }
        } catch (Exception stopEx) {
            Log.e(TAG, "bundle.stopNavigation failed", stopEx);
        }
        blueDotVectorLayer = null;
        if (navigationInfoCardView != null) {
            navigationInfoCardView.reset();
        }
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

    public void setMockGpsEnabled(boolean enabled) {
        mockGpsEnabled = enabled;
    }

    public synchronized boolean startSimulation(@Nullable SimulationListener listener) {
        return simulationHelper.startSimulation(listener);
    }

    public synchronized void stopSimulation() {
        simulationHelper.stopSimulation();
    }

    public boolean isSimulationRunning() {
        return simulationHelper.isSimulationRunning();
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
        if (snapLocationSourceProxy == null || snapLocationSourceProxy.getBlueDotDataSource() == null || snapLocationSourceProxy.getBlueDotDataSource().getLocationSource() == null) {
            lastRouteMessage = "Konum kaynağı oluşturulamadı.";
            Log.e(TAG, "getNavigationBundle: snapSource is null");
            return null;
        }
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
// Anlık konumun dinlenip komutların doldudurulduğu kısım
            @Override
            public boolean onCommandReady(NavigationCommand command) {
                if (command == null) {
                    return super.onCommandReady(command);
                }
                currentStaticIndex = (int) command.getStaticManifestCommandIndex();
                currentManifestText = resolveManifestText(command);
                String firstCommandName = extractFirstCommandName(command.getCommands());
                updateDirectionState(firstCommandName);
                currentSpeedLimitKmh = (int) Math.round(command.getSpeedLimit());
                final String distanceText = NavigationCommandFormatter.formatDistance(command.getDistanceToCommand());
                currentTotalDistanceText = NavigationCommandFormatter.formatTotalDistance(command.getTotalDistanceToCommand()); // manifest üstündeki toplam mesafe yazısı
                currentTotalTimeText = NavigationCommandFormatter.formatTotalTime(command.getRemainingTimeInSec());
                final Integer nextIconRes = resolveNextCommandIcon(command.getNextCommands());
                final boolean showNext = shouldShowNextCommand(currentCommandName, nextIconRes, currentStaticIndex);

                postNavigationCardUpdate(currentManifestText, currentDirectionText, distanceText, currentCommandIconRes, nextIconRes, showNext, currentTotalDistanceText, currentTotalTimeText);
                return super.onCommandReady(command);
            }

            @Override
            public boolean onNavigationWillRecalculate(){
                Log.e(TAG, "onNavigationWillRecalculate");
                return super.onNavigationWillRecalculate();
            }
// Farklı bir yola geçildiğinde rotanın yeniden hesaplandığı kısım.
            @Override
            public boolean onNavigationRecalculated(NavigationResult navigationResult) {
                Log.e(TAG, "onNavigationRecalculated: updating map for new route");
                if (navigationResult == null) {
                    return super.onNavigationRecalculated(navigationResult);
                }
                YolbilNavigationUsage.this.navigationResult = navigationResult;
                totalInstructionCount =
                        navigationResult.getInstructions() != null ? (int) navigationResult.getInstructions().size() : 0;

                if (snapLocationSourceProxy != null) {
                    snapLocationSourceProxy.setRoutingPoints(navigationResult.getPoints());
                }

                if (mapView != null && navigationResult.getPoints() != null && navigationResult.getPoints().size() > 0) {
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (mapView == null) {
                                return;
                            }
                        //    mapView.fitRouteOnMap(navigationResult.getPoints());
                            if (snapLocationSourceProxy != null) {
                                snapLocationSourceProxy.setmoveToHeadingDistance(50.0, mapView.getWidth(), mapView.getHeight());
                                MapPos focus = snapLocationSourceProxy.getShiftedCoordinate();
                                if (focus != null) {
                                    mapView.setFocusPos(focus, 0.6f);
                                }
                            }
                        }
                    });
                }
                return super.onNavigationRecalculated(navigationResult);
            }

            @Override
            public boolean onNavigationStopped() {
                if (navigationInfoCardView != null) {
                    navigationInfoCardView.reset();
                }
                return super.onNavigationStopped();
            }


// Konum burada dinleniyor ve kalan mesafe , kalan süre değerleri alınıyor
            @Override
            public boolean onLocationChanged(NavigationCommand command) {
                if (command == null || navigationInfoCardView == null) {
                    return super.onLocationChanged(command);
                }
                double distanceToCommand = command.getDistanceToCommand();
                final String distanceText = NavigationCommandFormatter.formatDistance(distanceToCommand);
                currentTotalDistanceText = NavigationCommandFormatter.formatTotalDistance(command.getTotalDistanceToCommand());
                currentTotalTimeText = NavigationCommandFormatter.formatTotalTime(command.getRemainingTimeInSec());

                final String manifestFromLocation = resolveManifestText(command);
                if (!TextUtils.isEmpty(manifestFromLocation) && !TextUtils.equals(manifestFromLocation, currentManifestText)) {
                    currentManifestText = manifestFromLocation;
                }

                String nextCommandName = extractFirstCommandName(command.getCommands());
                if (!TextUtils.isEmpty(nextCommandName) && !TextUtils.equals(nextCommandName, currentCommandName)) {
                    currentStaticIndex = (int) command.getStaticManifestCommandIndex();
                    updateDirectionState(nextCommandName);
                } else if (TextUtils.isEmpty(nextCommandName) && command.getDistanceToCommand() > 300) {
                    currentStaticIndex = (int) command.getStaticManifestCommandIndex();
                    updateDirectionState("");
                }
                currentSpeedLimitKmh = (int) Math.round(command.getSpeedLimit());

                final Integer nextIconRes = resolveNextCommandIcon(command.getNextCommands());
                final boolean showNext = shouldShowNextCommand(currentCommandName, nextIconRes, currentStaticIndex);

                postNavigationCardUpdate(
                        currentManifestText,
                        currentDirectionText,
                        distanceText,
                        currentCommandIconRes,
                        nextIconRes,
                        showNext,
                        currentTotalDistanceText,
                        currentTotalTimeText
                );

                return super.onLocationChanged(command);
            }
        });

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
        if (bundle == null) {
            return;
        }
        try {
            mapView.getLayers().addAll(bundle.getLayers());
        } catch (Exception layerEx) {
            lastRouteMessage = layerEx.getMessage();
            Log.e(TAG, "Failed to add navigation layers", layerEx);
        }
    }
    private boolean isFirstLocation = true;

    // Rota hazır olduğunda gerçek zamanlı navigasyonu başlatır ve haritayı kullanıcıya odaklar.
    @SuppressLint("MissingPermission")
    void startNavigation() {
        if (navigationResult != null && mapView != null && bundle != null) {
            if (navigationResult.getPoints().size() == 0) {
                lastRouteMessage = "Rota noktası boş, navigasyon başlatılmadı.";
                Log.e(TAG, "startNavigation: nav.points empty");
                return;
            }
            try {
                bundle.beginNavigation(navigationResult);
            } catch (Exception beginEx) {
                lastRouteMessage = beginEx.getMessage();
                Log.e(TAG, "beginNavigation failed", beginEx);
                return;
            }
            mapView.setDeviceOrientationFocused(true);
            mapView.setFocusPos(lastLocation.getCoordinate(), 1.0f);
            mapView.setZoom(19, 1.0f);

            locationSource.addListener(new LocationListener() {
                @Override
            public void onLocationChange(Location location) {

                lastLocation = location;
                //Bluedot takibi sağlanır
                followBlueDot(location, isFirstLocation);
                updateSpeedFromLocation(location);


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
        if (!handled && mapView != null && lastLocation != null) {
            MapPos coordinate = lastLocation.getCoordinate();
            if (coordinate != null) {
                mapView.setFocusPos(coordinate, 0.6f);
                mapView.setZoom(17, 0.6f);
                return true;
            }
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

        mapView.setFocusPos(focusPos, 0.6f);
        if (snapLocationSourceProxy != null && snapLocationSourceProxy.getLastLocation() != null) {
            float direction = (float) snapLocationSourceProxy.getLastLocation().getDirection();
            mapView.setMapRotation(direction, 0.6f);
        }
        if (initialFocus) {
            mapView.setZoom(18, 0.6f);
            mapView.setTilt(45.0f, 0.6f);
        }
        return true;
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


    /** TurnCommand listesinden ilk komutu string olarak döndürür (ikon metni için kullanılır). */
    @Nullable
    private String extractFirstCommandName(@Nullable TurnCommandEnumVector commands) {
        if (commands == null || commands.size() == 0) {
            return "";
        }
        TurnCommandEnum enumValue = commands.get(0);
        if (enumValue == null) {
            return "";
        }
        return enumValue.toString();
    }

    /** “Ardından” bölümünde hangi ikonun gösterileceğini belirler. */
    @Nullable
    private Integer resolveNextCommandIcon(@Nullable TurnCommandEnumVector nextCommands) {
        if (nextCommands == null || nextCommands.size() == 0) {
            return null;
        }
        TurnCommandEnum nextEnum = nextCommands.get(0);
        if (nextEnum == null) {
            return null;
        }
        return NavigationCommandFormatter.getIconForCommand(nextEnum.toString());
    }

    /** Son komutta veya manifest dizisinin sonunda “Ardından” kutusunu gizlemek için kullanılır. */
    private boolean shouldShowNextCommand(@Nullable String primaryCommandName, @Nullable Integer nextIconRes, int staticIndex) {
        if (nextIconRes == null) {
            return false;
        }
        String normalized = primaryCommandName == null ? "" : primaryCommandName.toUpperCase(Locale.US);
        if (TextUtils.isEmpty(normalized)) {
            return false;
        }
        boolean isFinalCmd = normalized.contains("WILL_REACH_YOUR_DESTINATION")
                || normalized.contains("REACHED_YOUR_DESTINATION")
                || normalized.contains("DESTINATION");
        boolean isAtEnd = totalInstructionCount > 0 && staticIndex >= (totalInstructionCount - 1);
        return !isFinalCmd && !isAtEnd;
    }

    /** nextRoadName boşsa statik manifest listesinden sokak adı bularak kart başlığını belirler. */
    private String resolveManifestText(NavigationCommand command) {
        String manifestText = command.getNextRoadName();
        String resolved = NavigationCommandFormatter.resolveManifestText(context, manifestText);
        if (!TextUtils.isEmpty(manifestText)) {
            return manifestText;
        }
        if (navigationResult != null && navigationResult.getInstructions() != null && navigationResult.getInstructions().size() > 0) {
            int index = (int) command.getStaticManifestCommandIndex();
            if (index < 0) {
                index = 0;
            }
            long size = navigationResult.getInstructions().size();
            if (index >= size) {
                index = (int) (size - 1);
            }
            RoutingInstruction instruction = navigationResult.getInstructions().get(index);
            if (instruction != null && !TextUtils.isEmpty(instruction.getStreetName())) {
                return instruction.getStreetName();
            }
        }
        return resolved;
    }

    /** NavigationInfoCard tek noktadan günceller. */
    private void postNavigationCardUpdate(
            final String manifestText,
            final String directionText,
            final String distanceText,
            final int iconRes,
            @Nullable final Integer nextIconRes,
            final boolean showNext,
            final String totalDistanceText,
            final String totalTimeText
    ) {
        if (navigationInfoCardView == null) {
            return;
        }
        mainHandler.post(() -> {
            navigationInfoCardView.updatePrimaryInfo(manifestText, directionText, distanceText, iconRes);
            navigationInfoCardView.updateSummaryInfo(totalDistanceText, totalTimeText);
            boolean isSpeeding = currentSpeedLimitKmh > 0 && currentSpeedKmh > currentSpeedLimitKmh;
            navigationInfoCardView.updateSpeedInfo(currentSpeedKmh, currentSpeedLimitKmh, isSpeeding);
            if (showNext && nextIconRes != null) {
                navigationInfoCardView.showNextCommand(nextIconRes, context.getString(R.string.next_direction_label));
            } else {
                navigationInfoCardView.hideNextCommand();
            }
        });
    }


    /** GPS hızını Kalman filtresinden geçirip km/s olarak saklar. */
    private void updateSpeedFromLocation(@Nullable Location location) {
        if (location == null) {
            return;
        }
        double speedValue = location.getSpeed();
        float filtered = SpeedUtils.updateFilteredSpeed((float) speedValue);
        int kmh = Math.max(0, Math.round(filtered));
        if (kmh != currentSpeedKmh) {
            currentSpeedKmh = kmh;
            refreshSpeedRowAsync();
        }
    }

    /** UI thread üzerinde hız kutusunu günceller. */
    private void refreshSpeedRowAsync() {
        if (navigationInfoCardView == null) {
            return;
        }
        final int speed = currentSpeedKmh;
        final int limit = currentSpeedLimitKmh;
        final boolean isSpeeding = limit > 0 && speed > limit;
        mainHandler.post(() -> navigationInfoCardView.updateSpeedInfo(speed, limit, isSpeeding));
    }

    /** Kartın manifest/direction state'ini günceller (setDynamicManifestCommand eşleniği). */
    private void updateDirectionState(@Nullable String commandName) {
        String safeName = commandName == null ? "" : commandName;
        currentCommandName = safeName;
        currentDirectionText = NavigationCommandFormatter.getDirectionText(context, safeName);
        currentCommandIconRes = NavigationCommandFormatter.getIconForCommand(safeName);
    }
}
