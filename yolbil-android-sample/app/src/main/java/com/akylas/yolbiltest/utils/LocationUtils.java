package com.akylas.yolbiltest.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import com.akylas.yolbiltest.application.AppSession;
import com.basarsoft.yolbil.core.MapPos;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.FusedLocationProviderClient;

public class LocationUtils {
    private static final String TAG = "LocationUtils";

    private final  FusedLocationProviderClient fusedLocationClient;
    private final LocationRequest locationRequest;
    private final LocationCallback locationCallback;

    public LocationUtils(Context context) {
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context.getApplicationContext());
        this.locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setWaitForAccurateLocation(false)
                .build();
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) {
                    return;
                }
                Location androidLocation = result.getLastLocation();
                if (androidLocation == null) {
                    return;
                }
                AppSession.setLastKnowLocation(new MapPos(androidLocation.getLongitude(), androidLocation.getLatitude()));
            }
        };
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.w(TAG, "startLocationUpdates: missing location permission", e);
        }
    }

    public void stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        } catch (Exception e) {
            Log.w(TAG, "stopLocationUpdates failed", e);
        }
    }
}
