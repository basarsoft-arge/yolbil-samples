package com.akylas.yolbiltest.ui.main;

import com.akylas.yolbiltest.ui.main.constants.BaseSettings;
import com.basarsoft.yolbil.core.StringVector;
import com.basarsoft.yolbil.datasources.HTTPTileDataSource;
import com.basarsoft.yolbil.datasources.YBOfflineStoredDataSource;
import com.basarsoft.yolbil.layers.RasterTileLayer;
import com.basarsoft.yolbil.ui.MapView;

public class BaseMapSwitchController {

    private final MapView mapView;
    private final String appCode;
    private final String accId;
    private RasterTileLayer basarsoftLayer;

    public BaseMapSwitchController(MapView mapView, String appCode, String accId) {
        this.mapView = mapView;
        this.appCode = appCode;
        this.accId = accId;
    }

    // Varsayilan olarak sadece Basarsoft raster altligi kullanilir.
    public void initializeDefaultLayer() {
        ensureBasarsoftLayer();
        applyBaseLayer(basarsoftLayer);
    }

    private void ensureBasarsoftLayer() {
        if (basarsoftLayer != null) {
            return;
        }
        final String tileUrl = BaseSettings.INSTANCE.getBaseRasterTileUrl(appCode, accId);

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

    private void applyBaseLayer(RasterTileLayer targetLayer) {
        if (mapView == null || targetLayer == null) {
            return;
        }
        removeLayer(basarsoftLayer);
        mapView.getLayers().insert(0, targetLayer);
    }

    private void removeLayer(RasterTileLayer layer) {
        if (mapView == null || layer == null) {
            return;
        }
        try {
            mapView.getLayers().remove(layer);
        } catch (Exception ignored) {
        }
    }
}
