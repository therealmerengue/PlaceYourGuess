package com.example.trm.placeyourguess;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.util.Pair;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;

public class LocationSelector {
    private FeatureCollection mBoxesFeatureCollection;
    private JSONArray mCountriesInfo;
    private StreetViewPanorama mPanorama;
    private StreetViewActivity mContextActivity;
    private ProgressDialog mProgressDialog;

    private String mCountryCode;

    private static final String URL_BEGINNING = "http://maps.google.com/cbk?output=json&hl=en&ll=";
    private static final String URL_END = "&radius=100000&cb_client=maps_sv&v=4";

    public LocationSelector(StreetViewPanorama panorama, Context context) {
        mPanorama = panorama;
        mContextActivity = (StreetViewActivity) context;

        BoundingBoxesHolder bbHolder = BoundingBoxesHolder.getInstance();
        mBoxesFeatureCollection = bbHolder.getBoxes();
        mCountriesInfo = bbHolder.getCountries();
    }

    public void switchPanorama(String countryCode) {
        mCountryCode = countryCode;
        Pair<LatLng, LatLng> bounds = getBounds(countryCode);
        new ImageChecker().execute(bounds.first, bounds.second);
    }

    private Pair<LatLng, LatLng> getBounds(String countryCode) {
        Feature countryFeature = findCountryFeature(countryCode);
        return getBounds(countryFeature);
    }

    private Pair<LatLng, LatLng> getBounds(Feature countryFeature) {
        double maxLat = 0, minLat = 0, maxLng = 0, minLng = 0;
        try {
            JSONObject jsonFeature = countryFeature.toJSON();
            JSONObject geometry = jsonFeature.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates").getJSONArray(0);
            JSONArray southWest = coordinates.getJSONArray(0);
            JSONArray northEast = coordinates.getJSONArray(2);
            maxLat = northEast.getDouble(1);
            minLat = southWest.getDouble(1);
            maxLng = northEast.getDouble(0);
            minLng = southWest.getDouble(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return new Pair<>(new LatLng(minLat, minLng), new LatLng(maxLat, maxLng));
    }

    private LatLng getRandomLatLng(Pair<LatLng, LatLng> bounds) {

        LatLng southWest = bounds.first;
        LatLng northEast = bounds.second;

        double maxLat = northEast.latitude;
        double minLat = southWest.latitude;
        double maxLng = northEast.longitude;
        double minLng = southWest.longitude;


        Random random = new Random(System.currentTimeMillis());
        double lat = minLat + (maxLat - minLat) * random.nextDouble();
        double lng = minLng + (maxLng - minLng) * random.nextDouble();

        return new LatLng(lat, lng);
    }

    private Feature findCountryFeature(String countryCode) {
        List<Feature> features = mBoxesFeatureCollection.getFeatures();

        for (Feature feature : features) {
            String code = null;
            JSONObject jsonFeature;
            try {
                jsonFeature = feature.toJSON();
                JSONObject properties = jsonFeature.getJSONObject("properties");
                code = properties.getString("iso3166");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (countryCode.equals(code)) {
                return feature;
            }
        }

        return null;
    }

    private String readAll(Reader rd) {
        StringBuilder sb = new StringBuilder();
        int cp;
        try {
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private JSONObject readJsonFromUrl(String url)  {
        InputStream is = null;

        try {
            is = new URL(url).openStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            return new JSONObject(jsonText);
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private String getCountryCode(String countryName) {
        for (int i = 0; i < mCountriesInfo.length(); i++) {
            try {
                JSONObject country = mCountriesInfo.getJSONObject(i);
                String name = country.getString("name");
                if (name.equals(countryName)) {
                    return country.getString("alpha-2");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    private class ImageChecker extends AsyncTask<LatLng, Void, JSONObject> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = ProgressDialog.show(mContextActivity, "", "Please Wait", false);
        }

        @Override
        protected JSONObject doInBackground(LatLng... params) {
            LatLng southWest = params[0];
            LatLng northEast = params[1];

            JSONObject response = new JSONObject();
            String locationCountryCode = null;

            while (response == null || response.length() == 0
                    || locationCountryCode == null || !locationCountryCode.equals(mCountryCode)) {
                LatLng randomLocation = getRandomLatLng(new Pair<>(southWest, northEast));
                String url = URL_BEGINNING + randomLocation.latitude + "," + randomLocation.longitude + URL_END;
                response = readJsonFromUrl(url);
                if (response == null || response.length() == 0)
                    continue;

                try {
                    JSONObject location = response.getJSONObject("Location");
                    String locationCountryName = location.getString("country");
                    locationCountryCode = getCountryCode(locationCountryName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            double lat = 0, lng = 0;

            try {
                JSONObject location = result.getJSONObject("Location");
                lat = location.getDouble("lat");
                lng = location.getDouble("lng");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            LatLng location = new LatLng(lat, lng);
            mPanorama.setPosition(location);
            mContextActivity.setupCountDownTimer(true);
            mProgressDialog.dismiss();
        }
    }
}
