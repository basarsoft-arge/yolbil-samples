package com.akylas.yolbiltest.ui.main;

import android.widget.Button;

import androidx.annotation.Nullable;

import com.basarsoft.yolbil.datasources.HTTPTileDataSource;
import com.basarsoft.yolbil.datasources.YBOfflineStoredDataSource;
import com.basarsoft.yolbil.core.StringVector;
import com.basarsoft.yolbil.layers.RasterTileLayer;
import com.basarsoft.yolbil.ui.MapView;

/**
 * Google ve Basarsoft taban katmanlarını hazır tutup, butonlarla seçilen altlığı haritaya
 * ekleyen yardımcı sınıf.
 */

public class BaseMapSwitchController {

    private enum BaseLayerType {
        GOOGLE,
        BASARSOFT
    }

    private final MapView mapView;
    private final String appCode;
    private final String accId;

    private Button googleButton;
    private Button basarsoftButton;

    private RasterTileLayer basarsoftLayer;
    private RasterTileLayer googleLayer;
    private BaseLayerType activeLayer = BaseLayerType.BASARSOFT;

    public BaseMapSwitchController(MapView mapView, String appCode, String accId) {
        this.mapView = mapView;
        this.appCode = appCode;
        this.accId = accId;
    }

    // Seçim butonlarını kontrolöre bağlar ve tıklama dinleyicilerini tanımlar.
    public void bindButtons(@Nullable Button googleButton, @Nullable Button basarsoftButton) {
        this.googleButton = googleButton;
        this.basarsoftButton = basarsoftButton;

        if (googleButton != null) {
            googleButton.setOnClickListener(v -> showGoogleLayer());
        }
        if (basarsoftButton != null) {
            basarsoftButton.setOnClickListener(v -> showBasarsoftLayer());
        }
        updateButtonState();
    }

    // Başlangıçta varsayılan olarak Basarsoft altlığını gösterir.
    public void initializeDefaultLayer() {
        showBasarsoftLayer();
    }

    // Google altlık katmanını hazırlar ve haritada görünür hale getirir.
    public void showGoogleLayer() {
        ensureGoogleLayer();
        applyBaseLayer(googleLayer, BaseLayerType.GOOGLE);
    }

    // Basarsoft altlık katmanını hazırlar ve haritada görünür hale getirir.
    public void showBasarsoftLayer() {
        ensureBasarsoftLayer();
        applyBaseLayer(basarsoftLayer, BaseLayerType.BASARSOFT);
    }

    // Basarsoft katmanı henüz oluşturulmadıysa ilgili veri kaynağını ve layer'ı kurar.
    private void ensureBasarsoftLayer() {
        if (basarsoftLayer != null) {
            return;
        }
        final String tileUrl = "https://bms.basarsoft.com.tr/service/api/v1/map/Default"
                + "?appcode=" + appCode
                + "&accid=" + accId
                + "&x={x}&y={y}&z={zoom}";

        HTTPTileDataSource httpTileDataSource = new HTTPTileDataSource(0, 18, tileUrl);
        StringVector subdomains = new StringVector();
        subdomains.add("1");
        subdomains.add("2");
        subdomains.add("3");
        httpTileDataSource.setSubdomains(subdomains);

        YBOfflineStoredDataSource offlineDataSource =
                new YBOfflineStoredDataSource(httpTileDataSource, "/storage/emulated/0/.basarsoft.cachetile.db");
        offlineDataSource.setCacheOnlyMode(false);
        basarsoftLayer = new RasterTileLayer(offlineDataSource);
    }

    // Google katmanı henüz oluşturulmadıysa HTTP veri kaynağı ve raster katmanı hazırlar.
    private void ensureGoogleLayer() {
        if (googleLayer != null) {
            return;
        }
        final HTTPTileDataSource googleTileDataSource = new HTTPTileDataSource(
                0,
                18,
                "https://mt0.google.com/vt/lyrs=m&hl=tr&scale=4&apistyle=s.t%3A2%7Cs.e%3Al%7Cp.v%3Aoff&x={x}&y={y}&z={zoom}"
        );
        YBOfflineStoredDataSource googleCache =
                new YBOfflineStoredDataSource(googleTileDataSource, "/storage/emulated/0/.google.cachetile.db");
        googleCache.setCacheOnlyMode(false);
        googleLayer = new RasterTileLayer(googleCache);
    }

    // Aktif katmanı kaldırıp yeni seçilen katmanı ekler, buton durumlarını günceller.
    private void applyBaseLayer(@Nullable RasterTileLayer targetLayer, BaseLayerType targetType) {
        if (mapView == null || targetLayer == null) {
            return;
        }
        removeLayer(googleLayer);
        removeLayer(basarsoftLayer);
        mapView.getLayers().insert(0, targetLayer);
        activeLayer = targetType;
        updateButtonState();
    }

    // Haritada ekli olan raster katmanını güvenli şekilde kaldırır.
    private void removeLayer(@Nullable RasterTileLayer layer) {
        if (mapView == null || layer == null) {
            return;
        }
        try {
            mapView.getLayers().remove(layer);
        } catch (Exception ignored) {
        }
    }

    private void updateButtonState() {
        if (googleButton != null) {
            googleButton.setEnabled(activeLayer != BaseLayerType.GOOGLE);
        }
        if (basarsoftButton != null) {
            basarsoftButton.setEnabled(activeLayer != BaseLayerType.BASARSOFT);
        }
    }
}
