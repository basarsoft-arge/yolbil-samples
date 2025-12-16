package com.akylas.yolbiltest.ui.main;

class KalmanFilter {
    private final float q;
    private final float r;
    private float p = 1f;
    private float x = 0f;

    KalmanFilter(float processNoise, float measurementNoise) {
        this.q = processNoise;
        this.r = measurementNoise;
    }

    float filter(float measurement) {
        float pPrime = p + q;
        float k = pPrime / (pPrime + r);
        x += k * (measurement - x);
        p = (1 - k) * pPrime;
        return x;
    }
}

public final class SpeedUtils {
    private static final KalmanFilter SPEED_FILTER = new KalmanFilter(0.1f, 1f);

    private SpeedUtils() {
    }

    public static float updateFilteredSpeed(float speedMetersPerSecond) {
        float speedKmh = speedMetersPerSecond * 3.6f;
        return SPEED_FILTER.filter(speedKmh);
    }
}
