package com.akylas.yolbiltest.application;

import com.basarsoft.yolbil.core.MapPos;

public final class AppSession {
    private static MapPos lastKnowLocation;
    private static Double lastLon;
    private static Double lastLat;

    private AppSession() {
    }

    public static void setLastKnowLocation(MapPos mapPos) {
        lastKnowLocation = mapPos;
    }

    public static MapPos getLastKnowLocation() {
        return lastKnowLocation;
    }

    public static void setLastKnowLocationRaw(double lon, double lat) {
        lastLon = lon;
        lastLat = lat;
    }

    public static MapPos getLastKnowLocationBackground() {
        if (lastKnowLocation != null) {
            return lastKnowLocation;
        }
        if (lastLon != null && lastLat != null) {
            return new MapPos(lastLon, lastLat);
        }
        return null;
    }
}
