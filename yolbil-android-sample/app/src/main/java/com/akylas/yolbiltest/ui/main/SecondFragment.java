package com.akylas.yolbiltest.ui.main;
import org.json.JSONArray;
import org.json.JSONException;
import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.akylas.yolbiltest.ui.main.constants.BaseSettings;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.akylas.yolbiltest.R;
import com.basarsoft.yolbil.components.Options;

import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.core.MapPosVector;
import com.basarsoft.yolbil.core.StringVector;
import com.basarsoft.yolbil.datasources.HTTPTileDataSource;
import com.basarsoft.yolbil.datasources.LocalVectorDataSource;
import com.basarsoft.yolbil.datasources.YBOfflineStoredDataSource;
import com.basarsoft.yolbil.graphics.Color;
import com.basarsoft.yolbil.layers.RasterTileLayer;
import com.basarsoft.yolbil.layers.VectorLayer;
import com.basarsoft.yolbil.location.GPSLocationSource;
import com.basarsoft.yolbil.location.Location;
import com.basarsoft.yolbil.location.LocationBuilder;
import com.basarsoft.yolbil.location.LocationListener;
import com.basarsoft.yolbil.location.LocationSource;
import com.basarsoft.yolbil.navigation.AssetsVoiceNarrator;
import com.basarsoft.yolbil.projections.EPSG4326;
import com.basarsoft.yolbil.routing.NavigationResult;
import com.basarsoft.yolbil.routing.RoutingInstructionVector;
import com.basarsoft.yolbil.styles.LineStyleBuilder;
import com.basarsoft.yolbil.styles.MarkerStyle;
import com.basarsoft.yolbil.styles.MarkerStyleBuilder;
import com.basarsoft.yolbil.ui.DeviceOrientationFocusedListener;
import com.basarsoft.yolbil.ui.MapView;
import com.basarsoft.yolbil.utils.YolbilDownloadManager;
import com.basarsoft.yolbil.vectorelements.Marker;
import java.io.File;
import java.util.HashMap;
import java.util.Map;


public class SecondFragment extends Fragment {
    private final String TAG = "SecondFragment";
    private ProgressDialog progressDialog;
    private YolbilDownloadManager downloadManager;

    private MapPos silverBlocks;
    private MapPos touchPos;
    private LocationSource locationSource;
    private MapView mapViewObject;
    Switch offlineSwitch;
    MapPosVector navigationVector;
    LocalVectorDataSource vectorDataSourceMarker;
    LineStyleBuilder lineStyleBuilder;
    VectorLayer vectorLayerLine;
    VectorLayer vectorLayerMarker;
    YolbilNavigationUsage usage;
    GPSLocationSource gpsLocationSource;
    Location lastLocation = null;
    Button focusPos,startNavigation;
    private TextView navigationInfoText;
    private View routeLoadingOverlay;
    private TextView routeLoadingText;
    boolean isLocationFound = false;

    String accId = BaseSettings.INSTANCE.getAccountId();
    String appCode = BaseSettings.INSTANCE.getAppCode();

    private static final int REQUEST_LOCATION_PERMISSION = 1001;

    AssetsVoiceNarrator commandPlayer;
    public static SecondFragment newInstance() {
        return new SecondFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.second_fragment, container, false);
        offlineSwitch = view.findViewById(R.id.offlineSwitch);
        focusPos = view.findViewById(R.id.button2);
        navigationInfoText = view.findViewById(R.id.navigationInfoText);
        startNavigation = view.findViewById(R.id.button3);
        routeLoadingOverlay = view.findViewById(R.id.routeLoadingOverlay);
        routeLoadingText = view.findViewById(R.id.routeLoadingText);
        focusPos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (lastLocation != null) {
                        mapViewObject.setFocusPos(lastLocation.getCoordinate(), 1.0f);
                        mapViewObject.setZoom(17, 1.0f);
                    }
                    mapViewObject.setDeviceOrientationFocused(true);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });

        startNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(usage != null ){
                    usage.startNavigation();
                }
            }
        });
        offlineSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

                if(lastLocation != null) {
                    isLocationFound = false;
                    if (usage != null) {
                        usage.stopNavigation();
                    }

                    if (b) {
                        initOnline(lastLocation.getCoordinate(), new MapPos(32.814785, 39.923197), false);
                    } else {
                        initOnline(lastLocation.getCoordinate(), new MapPos(32.814785, 39.923197), false);
                    }
                    offlineSwitch.setChecked(b);
                }
            }
        });
        Button modeBtn = view.findViewById(R.id.modeButton);

        mapViewObject = view.findViewById(R.id.mapView);
        modeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mapViewObject.isDeviceOrientationFocused()) {
                    mapViewObject.setDeviceOrientationFocused(true);
                }
            }
        });

        String tileUrl = "https://bms.basarsoft.com.tr/service/api/v1/map/Default"
                + "?appcode=" + appCode
                + "&accid=" + accId
                + "&x={x}&y={y}&z={zoom}";

        silverBlocks = new MapPos(32.836262, 39.960160);
        final HTTPTileDataSource httpTileDataSource = new HTTPTileDataSource(0, 18, tileUrl);
        final StringVector subdomains = new StringVector();
        subdomains.add("1");
        subdomains.add("2");
        subdomains.add("3");
        httpTileDataSource.setSubdomains(subdomains);

        //FOR OFFLINE STORAGE VIEWED TILES
        YBOfflineStoredDataSource ybOfflineStoredDataSource = new YBOfflineStoredDataSource(httpTileDataSource, "/storage/emulated/0/.cachetile.db");

        //ONLY OFFLINE USAGE
        //ybOfflineStoredDataSource.setCacheOnlyMode(true);

        final RasterTileLayer rasterlayer = new RasterTileLayer(ybOfflineStoredDataSource);

        //FOR DOWNLOAD SELECTED BOUNDARY
        /*
        MapBounds bounds = new MapBounds(new MapPos(32.836262, 39.960160), new MapPos(32.836262, 39.960160));
        ybOfflineStoredDataSource.startDownloadArea(bounds, 0, 10, new TileDownloadListener() {
            @Override
            public void onDownloadProgress(float progress) {
                Log.e("progress", String.valueOf(progress));
            }

            @Override
            public void onDownloadCompleted() {
                Log.e("onDownloadCompleted", "onDownloadCompleted");

            }
        });*/

        Options options = mapViewObject.getOptions();

        options.setBaseProjection(new EPSG4326());
        mapViewObject.getLayers().add(rasterlayer);

        LocationBuilder locationBuilder = new LocationBuilder();
        locationBuilder.setCoordinate(silverBlocks);
        locationBuilder.setHorizontalAccuracy(1);
        locationBuilder.setDirectionAccuracy(1);

        mapViewObject.setDeviceOrientationFocused(true);


        mapViewObject.setDeviceOrientationFocusedListener(new DeviceOrientationFocusedListener() {
            @Override
            public void onDeviceOrientationFocusedChanged(boolean isDeviceOrientationFocusedChanged) {
                Log.e("isDeviceOrientationFocusedChanged: ", String.valueOf(isDeviceOrientationFocusedChanged));
            }
        });

        mapViewObject.setFocusPos(silverBlocks, 0);
        //mapViewObject.setZoom(17, 0);

        //DownloadManager downloadmanager = (DownloadManager) getActivity().getSystemService(Context.DOWNLOAD_SERVICE);
        //YolbilUtil.downloadYolbilData(downloadmanager);
        //getActivity().registerReceiver(onDownloadComplete,new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        /*rasterlayer.setTileLoadListener( new TileLoadListener(){
            @Override
            public void onVisibleTilesLoaded() {
                Log.e("minx", ""+ mapViewObject.getMapBounds().getMin().getX());
                Log.e("miny", ""+ mapViewObject.getMapBounds().getMin().getY());

                Log.e("maxx", ""+ mapViewObject.getMapBounds().getMax().getX());
                Log.e("maxy", ""+ mapViewObject.getMapBounds().getMax().getY());
                super.onVisibleTilesLoaded();
            }
        });*/
        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        try {
            if(lastLocation != null) {
                isLocationFound = false;
                if (offlineSwitch.isChecked()) {
                    initOnline(lastLocation.getCoordinate(),new MapPos(32.814785, 39.923197), false);

                } else {
                    initOnline(lastLocation.getCoordinate(),new MapPos(32.814785, 39.923197), false);
                }
            }
        }catch (Exception e){
            Log.e("valhalla", e.getMessage());
        }

        if (hasLocationPermission()) {
            startLocationUpdates();
        } else {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

        mapViewObject.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                try {
                    if(usage != null){
                        usage.stopNavigation();
                    }
                    //initOnline(touchPos, offlineSwitch.isChecked());
                }catch (Exception e){
                    Log.e("fail","new route");
                }
                return false;
            }
        });
        /*
         */


        File destinationFile = new File("/storage/emulated/0/yolbilxdata/", "TR.vtiles");
        String downloadUrl = "DOWNLOAD_URL";

        // ProgressDialog ayarları
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Dosya indiriliyor...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100); // Yüzdelik olarak gösterilecek

        // Dosya indirme işlemini başlat
        //startDownload(destinationFile, downloadUrl);

        //THIS MUST BE SET FOR CUSTOM DOMAIN
        downloadManager = new YolbilDownloadManager("DOMAIN");

        /*downloadManager.checkVersion("/storage/emulated/0/yolbilxdata/", new YolbilDownloadManager.VersionListener(){

            @Override
            public void onVersionUpToDate() {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "onVersionUpToDate", Toast.LENGTH_LONG).show()
                    ;
                });
                //initOnline(new MapPos(30.927677, 37.326687),true);
            }

            @Override
            public void onVersionOutdated() {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "onVersionOutdated", Toast.LENGTH_LONG).show();
                });
                startDownload(destinationFile, downloadUrl);
            }

            @Override
            public void onError(String error) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "onError: " + error, Toast.LENGTH_LONG).show();
                });
                startDownload(destinationFile, downloadUrl);
            }
        });*/

        sendAutoSuggestionRequest();
        mapViewObject.setFocusPos(new MapPos(34.12908547029324,39.45037125619312),0.0f);
        mapViewObject.setZoom(4,0.0f);

    }

    // Yolbil servisinden örnek bir adres önerisi isteği gönderip sonucu loglar.
    private void sendAutoSuggestionRequest() {
        String url = "https://bms.basarsoft.com.tr/Service/api/v1/AutoSuggestion/Search"
                + "?accId=" +accId
                + "&appCode="+appCode
                + "&words=atatürk"
                + "&limit=10"
                + "&lat=0"
                + "&lon=0"
                + "&type=4"
                + "&uk=false";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            for (int i = 0; i < response.length(); i++) {
                                JSONArray innerArray = response.getJSONArray(i);

                                String fullAddress = innerArray.getString(0); // Adres
                                String city = innerArray.getString(6); // Şehir

                                Log.d(TAG, "Full Address: " + fullAddress);
                                Log.d(TAG, "City: " + city);
                            }
                        } catch (JSONException e) {
                            Log.e(TAG, "JSON Parsing Error: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Error: " + error.toString());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("accept", "text/plain");
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        requestQueue.add(jsonArrayRequest);
    }
    // Offline veri paketini indirirken kullanıcıya süreç hakkında bilgi verir.
    private void startDownload(File destinationFile, String downloadUrl) {
        // İndirilen dosyanın kaydedileceği yer

        // ProgressDialog'u göster
        getActivity().runOnUiThread(() -> progressDialog.show());

        // YolbilDownloadManager'ı kullanarak dosya indirme işlemini başlat
        downloadManager.downloadFile(downloadUrl, destinationFile, new YolbilDownloadManager.DownloadListener() {
            @Override
            public void onProgress(int progress) {
                // ProgressDialog'u güncelle
                getActivity().runOnUiThread(() -> progressDialog.setProgress(progress));
            }

            @Override
            public void onSuccess(File downloadedFile) {
                // İndirme başarılı, ProgressDialog'u kapat
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "İndirme tamamlandı: " + downloadedFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    //initOnline(new MapPos(30.927677, 37.326687),true);
                });
            }

            @Override
            public void onError(String errorMessage, YolbilDownloadManager.DownloadError errorType) {
                getActivity().runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "İndirme hatası: " + errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /*
        private void addSpatialiteLayer(MapView mapView, String tableName, int minZoom, int maxZoom, MBVectorTileDecoder vectorTileDecoder, SpatialiteDatabase database, String... properties) {
            final SpatialiteVectorTileDataSource dataSource = new SpatialiteVectorTileDataSource(database);
            StringVector propertiesVector = new StringVector();
            for(String property: properties) {
                propertiesVector.add(property);
            }
            dataSource.createSpatialiteLayer(tableName, tableName, minZoom, maxZoom, propertiesVector);
            try {
                dataSource.disableLayer("test");
            }catch (RuntimeException ex) {
                ex.printStackTrace();
            }
            final VectorTileLayer layer = new VectorTileLayer(dataSource, vectorTileDecoder);
            layer.setLabelRenderOrder(VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST);
            mapView.getLayers().add(layer);
        }

        private Pair<VectorTileLayer, SpatialiteVectorTileDataSource> spatialiteLayer(String layerName, String tableName, int minZoom, int maxZoom, MBVectorTileDecoder decoder, SpatialiteDatabase spatialiteDatabase) {
            final SpatialiteVectorTileDataSource dataSource = new SpatialiteVectorTileDataSource(spatialiteDatabase);
            dataSource.createSpatialiteLayer(layerName, tableName, minZoom, maxZoom);
            final VectorTileLayer layer = new VectorTileLayer(dataSource, decoder);
            layer.setLabelRenderOrder(VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST);
            return new Pair<>(layer, dataSource);
        }

        private Pair<VectorTileLayer, SpatialiteVectorTileDataSource> spatialiteLayer(String layerName, String tableName, int minZoom, int maxZoom, MBVectorTileDecoder decoder, SpatialiteDatabase spatialiteDatabase, String... properties) {
            final SpatialiteVectorTileDataSource dataSource = new SpatialiteVectorTileDataSource(spatialiteDatabase);
            StringVector propertiesVector = new StringVector();
            for(String property: properties) {
                propertiesVector.add(property);
            }
            dataSource.createSpatialiteLayer(layerName, tableName, minZoom, maxZoom, propertiesVector);
            final VectorTileLayer layer = new VectorTileLayer(dataSource, decoder);
            layer.setLabelRenderOrder(VectorTileRenderOrder.VECTOR_TILE_RENDER_ORDER_LAST);
            return new Pair<>(layer, dataSource);
        }
        */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    public Animator onCreateAnimator(int transit, boolean enter, int nextAnim) {
        Animator result = super.onCreateAnimator(transit, enter, nextAnim);
        Log.d(TAG, "onCreateAnimator " + result);
        return result;
    }
    // Kullanıcı konumu ile hedef arasındaki rotayı hesaplayıp harita katmanlarını günceller.
    public void initOnline(MapPos from, MapPos mapPosTo, boolean isOffline) {
        if (!isLocationFound) {
            isLocationFound = true;
            if (usage != null) {
                mapViewObject.getLayers().remove(vectorLayerMarker);
                vectorDataSourceMarker.clear();
            }

            MarkerStyleBuilder markerStyleBuilder = new MarkerStyleBuilder();
            markerStyleBuilder.setColor(new Color(0xffff5031));

            markerStyleBuilder.setSize(30);
            MarkerStyle sharedMarkerStyle = markerStyleBuilder.buildStyle();

            Marker fromMarker = new Marker(from, sharedMarkerStyle);
            Marker toMarker = new Marker(mapPosTo, sharedMarkerStyle);

            vectorDataSourceMarker = new LocalVectorDataSource(new EPSG4326());
            //vectorDataSourceMarker.add(fromMarker);
            vectorDataSourceMarker.add(toMarker);

            vectorLayerMarker = new VectorLayer(vectorDataSourceMarker);
            mapViewObject.getLayers().add(vectorLayerMarker);
            if (usage == null) {
                usage = new YolbilNavigationUsage(navigationInfoText);
            }

            toggleRouteLoading(true, getString(R.string.route_loading_text));
            final MapPos startPos = from;
            final MapPos destinationPos = mapPosTo;
            final boolean offline = isOffline;

            routeLoadingOverlay.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        // Yolbil servisinden rota isteğini gönderip NavigationResult bekler.
                        NavigationResult navigationResult = usage.fullExample(mapViewObject, startPos, destinationPos, offline, gpsLocationSource);

                        if (navigationResult != null) {
                            RoutingInstructionVector instructions = navigationResult.getInstructions();
                            Log.e(TAG, "onViewCreated: nav result: " + navigationResult);
                            for (int i = 0; i < instructions.size(); i++) {
                                Log.e(TAG, "onViewCreated: " + instructions.get(i).toString());
                            }
                        }
                    } finally {
                        toggleRouteLoading(false, null);
                    }
                }
            });
        }
    }

    private void toggleRouteLoading(final boolean show, @Nullable final String message) {
        if (routeLoadingOverlay == null || getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                routeLoadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
                if (message != null && routeLoadingText != null) {
                    routeLoadingText.setText(message);
                }
            }
        });
    }

    // Hem ince hem de kaba konum izinlerini kontrol eder.
    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // GPSLocationSource'u devreye alıp gelen konuma göre ilk rotayı tetikler.
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (gpsLocationSource == null) {
            gpsLocationSource = new GPSLocationSource(getActivity());
        }
        gpsLocationSource.startLocationUpdates();
        gpsLocationSource.addListener(new LocationListener() {
            @Override
            public void onLocationChange(Location location) {
                lastLocation = location;
                if (!isLocationFound) {
                    initOnline(location.getCoordinate(), new MapPos(43.74083, 37.57444), false);
                    isLocationFound = true;
                }
            }
        });
    }

    // İzin isteği geri dönüşünü ele alır ve gerekirse kullanıcıyı bilgilendirir.
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(getContext(), "Konum izni olmadan navigasyon başlatılamaz", Toast.LENGTH_LONG).show();
            }
        }
    }

}
