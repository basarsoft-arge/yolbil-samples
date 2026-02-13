package com.akylas.yolbiltest.background;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class LocationWatchdogWorker extends Worker {
    public LocationWatchdogWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context appContext = getApplicationContext();
        if (!LocationServiceStarter.hasLocationPermission(appContext)) {
            return Result.success();
        }

        if (!LocationServiceStarter.isServiceRunning(appContext)) {
            LocationServiceStarter.startServiceFromBackgroundIfPossible(appContext);
        }
        return Result.success();
    }
}
