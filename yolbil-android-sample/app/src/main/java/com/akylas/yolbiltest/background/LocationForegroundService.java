package com.akylas.yolbiltest.background;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.akylas.yolbiltest.MainActivity;
import com.akylas.yolbiltest.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.util.Locale;

public class LocationForegroundService extends Service {
    private static final String TAG = "LocationFgService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1011;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private ServiceGPSLocationSource gpsLocationSource;

    @Override
    public void onCreate() {
        super.onCreate();
        gpsLocationSource = ServiceGPSLocationSource.getInstance(getApplicationContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location = locationResult.getLastLocation();
                if (location == null) {
                    return;
                }
                gpsLocationSource.updateFromService(location);
                updateNotification(location.getLatitude(), location.getLongitude());
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            createNotificationChannelIfNeeded();
            startForeground(NOTIFICATION_ID, buildNotification(null, null));
            startLocationUpdates();
            return START_STICKY;
        } catch (SecurityException securityException) {
            Log.e(TAG, "FGS location başlatma izni reddedildi", securityException);
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Konum izni yok, servis durduruluyor");
            stopSelf();
            return;
        }

        try {
            LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                    .setMinUpdateIntervalMillis(1000)
                    .setWaitForAccurateLocation(false)
                    .build();
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (Exception e) {
            Log.e(TAG, "Konum dinleme başlatılamadı", e);
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        try {
            if (fusedLocationClient != null && locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        } catch (Exception e) {
            Log.w(TAG, "Konum dinleme durdurma hatası", e);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateNotification(double latitude, double longitude) {
        Notification notification = buildNotification(latitude, longitude);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, notification);
        }
    }

    private Notification buildNotification(@Nullable Double latitude, @Nullable Double longitude) {
        Intent activityIntent = new Intent(this, MainActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int pendingIntentFlags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, activityIntent, pendingIntentFlags);

        String contentText;
        if (latitude == null || longitude == null) {
            contentText = "Konum bekleniyor...";
        } else {
            contentText = String.format(Locale.US, "Lat: %.6f, Lon: %.6f", latitude, longitude);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Yolbil Konum Servisi")
                .setContentText(contentText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(contentText))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription("Arka planda konum takibi");
        notificationManager.createNotificationChannel(channel);
    }
}
