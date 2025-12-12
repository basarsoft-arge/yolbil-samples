package com.akylas.yolbiltest.ui.main;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.basarsoft.yolbil.core.MapPos;
import com.basarsoft.yolbil.core.MapPosVector;
import com.basarsoft.yolbil.location.GPSLocationSource;
import com.basarsoft.yolbil.location.Location;
import com.basarsoft.yolbil.location.LocationSourceSnapProxy;
import com.basarsoft.yolbil.routing.NavigationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Navigasyon simülasyonuna ait tüm iş akışını kapsülleyerek {@link YolbilNavigationUsage}
 * sınıfının geri kalan entegrasyonlara odaklanmasını sağlar.
 */
final class NavigationSimulationHelper {

    // Simülasyonun ihtiyaç duyduğu verileri sağlayan ve sonuçları tüketen köprü arayüz.
    interface SimulationHost {
        @Nullable NavigationResult getNavigationResult();
        @Nullable Location getLastLocation();
        void updateLastLocation(Location location);
        @Nullable GPSLocationSource getLocationSource();
        @Nullable LocationSourceSnapProxy getSnapLocationSourceProxy();
        boolean ensureDeviceOrientationFocus();
        boolean followBlueDot(@Nullable Location location, boolean initialFocus);
    }

    private final SimulationHost host;
    private final String logTag;
    // Simülasyonda kullanılan ana iş parçacığındaki zamanlayıcı.
    private final Handler simulationHandler = new Handler(Looper.getMainLooper());
    private static final long SIMULATION_STEP_MS = 1000L;
    private static final double SIMULATION_SPEED_KMH = 100.0;

    private Runnable simulationRunnable;
    private final List<MapPos> simulationPoints = new ArrayList<>();
    private int simulationIndex = 0;
    private boolean simulationRunning = false;
    private YolbilNavigationUsage.SimulationListener simulationListener;

    NavigationSimulationHelper(String logTag, SimulationHost host) {
        this.logTag = logTag;
        this.host = host;
    }

    synchronized boolean startSimulation(@Nullable YolbilNavigationUsage.SimulationListener listener) {
        if (simulationRunning) {
            Log.w(logTag, "startSimulation: Simulation already running");
            return false;
        }

        NavigationResult navigationResult = host.getNavigationResult();
        if (navigationResult == null || navigationResult.getPoints() == null || navigationResult.getPoints().size() <= 0) {
            Log.w(logTag, "startSimulation: No navigation result available");
            return false;
        }

        simulationPoints.clear();
        MapPosVector points = navigationResult.getPoints();
        List<MapPos> rawPoints = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            rawPoints.add(points.get(i));
        }
        double speedMps = (SIMULATION_SPEED_KMH * 1000.0) / 3600.0;
        double spacingMeters = speedMps * (SIMULATION_STEP_MS / 1000.0);
        simulationPoints.addAll(resamplePolylineWgs84(rawPoints, spacingMeters));

        if (simulationPoints.size() < 2) { // Rota çok kısa ise simülasyon başlamasın.
            Log.w(logTag, "startSimulation: Not enough points to simulate");
            simulationPoints.clear();
            return false;
        }

        simulationListener = listener;
        simulationIndex = 0;
        simulationRunning = true;
        host.ensureDeviceOrientationFocus();
        simulationRunnable = new Runnable() {
            @Override
            public void run() {
                if (!simulationRunning) {
                    return;
                }

                if (simulationIndex >= simulationPoints.size()) {
                    stopSimulationInternal(true);
                    return;
                }

                MapPos mapPos = simulationPoints.get(simulationIndex++); // Bir sonraki ara noktayı işle.
                Location lastLocation = host.getLastLocation();
                double direction = Double.NaN;
                if (lastLocation != null && isCoordinateValid(lastLocation.getCoordinate())) {
                    direction = calculateBearing(lastLocation.getCoordinate(), mapPos);
                }
                GPSLocationSource locationSource = host.getLocationSource();
                if (locationSource != null) {
                    locationSource.sendMockLocation(mapPos);
                }
                Location updatedLocation = lastLocation;
                if (updatedLocation == null) {
                    updatedLocation = new Location();
                }
                updatedLocation.setCoordinate(mapPos);
                if (!Double.isNaN(direction)) {
                    updatedLocation.setDirection(direction);
                }
                host.updateLastLocation(updatedLocation);

                LocationSourceSnapProxy snapProxy = host.getSnapLocationSourceProxy();
                if (snapProxy != null) {
                    snapProxy.updateLocation(updatedLocation);
                }
                host.followBlueDot(updatedLocation, false); // Kamera takibini tetikle.
                simulationHandler.postDelayed(this, SIMULATION_STEP_MS);
            }
        };
        simulationHandler.post(simulationRunnable);
        return true;
    }

    synchronized void stopSimulation() {
        stopSimulationInternal(true);
    }

    synchronized boolean isSimulationRunning() {
        return simulationRunning;
    }

    private synchronized void stopSimulationInternal(boolean notifyListener) {
        if (!simulationRunning) {
            if (notifyListener && simulationListener != null) {
                simulationListener.onSimulationFinished();
            }
            simulationListener = null;
            return;
        }

        simulationRunning = false;
        simulationHandler.removeCallbacks(simulationRunnable);
        simulationRunnable = null;
        simulationPoints.clear();
        simulationIndex = 0;
        if (notifyListener && simulationListener != null) {
            simulationListener.onSimulationFinished();
        }
        simulationListener = null;
    }

    private double calculateBearing(MapPos from, MapPos to) {
        double lat1 = Math.toRadians(from.getY());
        double lat2 = Math.toRadians(to.getY());
        double dLon = Math.toRadians(to.getX() - from.getX());
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.toDegrees(Math.atan2(y, x));
        return (bearing + 360.0) % 360.0;
    }

    // Rota polyline'ını verilen aralığa göre yeniden örnekler.
    private List<MapPos> resamplePolylineWgs84(List<MapPos> points, double spacingMeters) {
        if (points.size() < 2) {
            return new ArrayList<>(points);
        }
        List<MapPos> out = new ArrayList<>();
        MapPos prev = points.get(0);
        out.add(prev);
        double acc = 0.0;
        double nextTarget = spacingMeters; // Bir sonraki noktanın hedef mesafesi.

        for (int i = 0; i < points.size() - 1; i++) {
            MapPos a = prev;
            MapPos b = points.get(i + 1);
            double segLen = haversineMeters(a.getY(), a.getX(), b.getY(), b.getX());
            MapPos start = a;

            while (acc + segLen >= nextTarget) {
                double remain = nextTarget - acc;
                double fraction = Math.min(Math.max(remain / segLen, 0.0), 1.0);
                MapPos np = interpolateGreatCircle(start, b, fraction);
                out.add(np);
                start = np;
                segLen = haversineMeters(start.getY(), start.getX(), b.getY(), b.getX());
                acc = nextTarget;
                nextTarget += spacingMeters;
            }
            acc += segLen;
            prev = b;
        }
        MapPos last = points.get(points.size() - 1);
        MapPos lastOut = out.get(out.size() - 1);
        if (lastOut.getX() != last.getX() || lastOut.getY() != last.getY()) {
            out.add(last);
        }
        return out;
    }

    // İki koordinat arasındaki mesafeyi metre cinsinden döndürür.
    private double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // Great-circle interpolasyonu ile ara nokta üretir.
    private MapPos interpolateGreatCircle(MapPos from, MapPos to, double fraction) {
        double phi1 = Math.toRadians(from.getY());
        double lambda1 = Math.toRadians(from.getX());
        double phi2 = Math.toRadians(to.getY());
        double lambda2 = Math.toRadians(to.getX());

        double sinPhi1 = Math.sin(phi1);
        double cosPhi1 = Math.cos(phi1);
        double sinPhi2 = Math.sin(phi2);
        double cosPhi2 = Math.cos(phi2);

        double delta = 2 * Math.asin(Math.sqrt(
                Math.sin((phi2 - phi1) / 2) * Math.sin((phi2 - phi1) / 2)
                        + Math.cos(phi1) * Math.cos(phi2)
                        * Math.sin((lambda2 - lambda1) / 2) * Math.sin((lambda2 - lambda1) / 2)
        ));
        if (delta == 0.0) {
            return new MapPos(from.getX(), from.getY());
        }

        double A = Math.sin((1 - fraction) * delta) / Math.sin(delta);
        double B = Math.sin(fraction * delta) / Math.sin(delta);

        double x = A * cosPhi1 * Math.cos(lambda1) + B * cosPhi2 * Math.cos(lambda2);
        double y = A * cosPhi1 * Math.sin(lambda1) + B * cosPhi2 * Math.sin(lambda2);
        double z = A * sinPhi1 + B * sinPhi2;

        double phi = Math.atan2(z, Math.sqrt(x * x + y * y));
        double lambda = Math.atan2(y, x);
        return new MapPos(Math.toDegrees(lambda), Math.toDegrees(phi));
    }

    private boolean isCoordinateValid(@Nullable MapPos mapPos) {
        if (mapPos == null) {
            return false;
        }
        double x = mapPos.getX();
        double y = mapPos.getY();
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return false;
        }
        return !(Math.abs(x) < 1e-6 && Math.abs(y) < 1e-6);
    }
}
