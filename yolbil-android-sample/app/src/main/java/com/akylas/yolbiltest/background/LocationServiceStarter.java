package com.akylas.yolbiltest.background;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LocationServiceStarter {
    public static final String WATCHDOG_WORK_NAME = "location-service-watchdog";

    private LocationServiceStarter() {
    }

    public static void startServiceIfPossible(Context context) {
        if (!hasLocationPermission(context)) {
            return;
        }
        startService(context);
    }

    public static void startServiceFromBackgroundIfPossible(Context context) {
        if (!hasLocationPermission(context)) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startService(context);
    }

    public static void startService(Context context) {
        Context appContext = context.getApplicationContext();
        Intent serviceIntent = new Intent(appContext, LocationForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(appContext, serviceIntent);
        } else {
            appContext.startService(serviceIntent);
        }
    }

    public static void scheduleWatchdog(Context context) {
        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(LocationWatchdogWorker.class, 15, TimeUnit.MINUTES)
                        .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(
                        WATCHDOG_WORK_NAME,
                        ExistingPeriodicWorkPolicy.REPLACE,
                        workRequest
                );
    }

    public static boolean hasLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isServiceRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }

        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo serviceInfo : services) {
            if (LocationForegroundService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
