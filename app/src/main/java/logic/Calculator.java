package logic;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public final class Calculator {
    public static int calculatePoints(float distance) {
        int points = 0;

        if (distance <= 1) {
            points = Math.round(-100f * distance + 1000f);
        } else if (distance <= 10) {
            points = Math.round((-100f / 9f) * distance + 900f + 100f / 9f);
        } else if (distance <= 100) {
            points = Math.round((-10f / 9f) * distance + 800f + 100 / 9f);
        } else if (distance <= 200) {
            points = Math.round(-2f * distance + 900f);
        } else if (distance <= 2000) {
            points = Math.round((-5f / 18f) * distance + 500f + 500f / 9f);
        }

        return points;
    }

    public static float measureDistance(LatLng latLng1, LatLng latLng2) {
        Location location1 = new Location("");
        location1.setLatitude(latLng1.latitude);
        location1.setLongitude(latLng1.longitude);

        Location location2 = new Location("");
        location2.setLatitude(latLng2.latitude);
        location2.setLongitude(latLng2.longitude);

        return location1.distanceTo(location2);
    }
}
