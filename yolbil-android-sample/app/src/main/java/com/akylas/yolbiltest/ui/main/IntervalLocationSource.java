package com.akylas.yolbiltest.ui.main;

import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.core.Variant;
import com.basarsoft.yolbil.location.LocationBuilder;
import com.basarsoft.yolbil.location.LocationSource;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IntervalLocationSource extends LocationSource {
    public IntervalLocationSource() {
        final LocationBuilder locationBuilder = new LocationBuilder();
        final MapPos mapPos1 = new MapPos(32.645671741300916, 39.80168290242593);
        final MapPos mapPos2 = new MapPos(32.933479631618034, 40.01013954961311);
        locationBuilder.setAltitude(11);
        locationBuilder.setDirection(14);
        locationBuilder.setDirectionAccuracy(15);
        locationBuilder.setFloor(16);
        locationBuilder.setHorizontalAccuracy(17);
        locationBuilder.setSpeed(18);
        locationBuilder.setVerticalAccuracy(19);
        locationBuilder.setMetadata(new Variant("Bu veri GPS'den geldi :upside_down:"));

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(new Runnable() {
            private boolean pos = true;

            @Override
            public void run() {
                locationBuilder.setTimestamp(System.currentTimeMillis());
                if(pos) {
                    locationBuilder.setCoordinate(mapPos1);
                }else {
                    locationBuilder.setCoordinate(mapPos2);
                }
                pos = !pos;
                updateLocation(locationBuilder.build());
            }
        }, 0, 3, TimeUnit.SECONDS);
    }
}
