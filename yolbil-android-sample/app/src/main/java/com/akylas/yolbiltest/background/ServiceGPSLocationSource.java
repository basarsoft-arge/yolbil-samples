package com.akylas.yolbiltest.background;

import android.content.Context;
import android.os.Build;

import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.core.Variant;
import com.basarsoft.yolbil.location.GPSLocationSource;
import com.basarsoft.yolbil.location.LocationBuilder;

/**
 * Uygulama ve foreground servis tarafından ortak kullanılan GPSLocationSource.
 * Platform konumunu foreground servis toplar ve SDK listener'larına bu nesne ile yayınlar.
 */
public final class ServiceGPSLocationSource extends GPSLocationSource {
    private static volatile ServiceGPSLocationSource instance;

    private ServiceGPSLocationSource(Context context) {
        super(context.getApplicationContext());
    }

    public static ServiceGPSLocationSource getInstance(Context context) {
        if (instance == null) {
            synchronized (ServiceGPSLocationSource.class) {
                if (instance == null) {
                    instance = new ServiceGPSLocationSource(context);
                }
            }
        }
        return instance;
    }

    @Override
    public void startLocationUpdates() {
    }

    @Override
    public void stopLocationUpdates() {
    }

    public void updateFromService(android.location.Location androidLocation) {
        if (androidLocation == null) {
            return;
        }

        LocationBuilder builder = new LocationBuilder();
        builder.setCoordinate(new MapPos(
                androidLocation.getLongitude(),
                androidLocation.getLatitude()
        ));
        builder.setTimestamp(androidLocation.getTime());
        String provider = androidLocation.getProvider() != null
                ? androidLocation.getProvider()
                : "service";
        builder.setMetadata(new Variant(provider));

        if (androidLocation.hasAltitude()) {
            builder.setAltitude(androidLocation.getAltitude());
        }
        if (androidLocation.hasBearing()) {
            builder.setDirection(androidLocation.getBearing());
        }
        if (androidLocation.hasSpeed()) {
            builder.setSpeed(androidLocation.getSpeed());
        }
        if (androidLocation.hasAccuracy()) {
            builder.setHorizontalAccuracy(androidLocation.getAccuracy());
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (androidLocation.hasVerticalAccuracy()) {
                builder.setVerticalAccuracy(androidLocation.getVerticalAccuracyMeters());
            }
            if (androidLocation.hasBearingAccuracy()) {
                builder.setDirectionAccuracy(androidLocation.getBearingAccuracyDegrees());
            }
        }

        updateLocation(builder.build());
    }
}
