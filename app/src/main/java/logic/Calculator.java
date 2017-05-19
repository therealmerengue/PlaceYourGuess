package logic;

import android.location.Location;
import android.util.Pair;

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

    // Semi-axes of WGS-84 geoidal reference
    private static final double WGS84_a = 6378137.0; // Major semiaxis [m]
    private static final double WGS84_b = 6356752.3; // Minor semiaxis [m]

    // 'halfSideInKm' is the half length of the bounding box you want in kilometers.
    public static Pair<LatLng, LatLng> getBoundingBox(LatLng point, double halfSideInKm)
    {
        // Bounding box surrounding the point at given coordinates,
        // assuming local approximation of Earth surface as a sphere
        // of radius given by WGS84
        double lat = deg2Rad(point.latitude);
        double lon = deg2Rad(point.longitude);
        double halfSide = 1000 * halfSideInKm;

        // Radius of Earth at given latitude
        double radius = WGS84EarthRadius(lat);
        // Radius of the parallel at given latitude
        double pradius = radius * Math.cos(lat);

        double latMin = lat - halfSide / radius;
        double latMax = lat + halfSide / radius;
        double lonMin = lon - halfSide / pradius;
        double lonMax = lon + halfSide / pradius;

        return new Pair<>(new LatLng(rad2Deg(latMin), rad2Deg(lonMin)), new LatLng(rad2Deg(latMax), rad2Deg(lonMax)));
    }

    // degrees to radians
    private static double deg2Rad(double degrees)
    {
        return Math.PI * degrees / 180.0;
    }

    // radians to degrees
    private static double rad2Deg(double radians)
    {
        return 180.0 * radians / Math.PI;
    }

    // Earth radius at a given latitude, according to the WGS-84 ellipsoid [m]
    private static double WGS84EarthRadius(double lat)
    {
        // http://en.wikipedia.org/wiki/Earth_radius
        double An = WGS84_a * WGS84_a * Math.cos(lat);
        double Bn = WGS84_b * WGS84_b * Math.sin(lat);
        double Ad = WGS84_a * Math.cos(lat);
        double Bd = WGS84_b * Math.sin(lat);
        return Math.sqrt((An * An + Bn * Bn) / (Ad * Ad + Bd * Bd));
    }
}
