package logic;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Pair;

import com.cocoahero.android.geojson.Feature;
import com.cocoahero.android.geojson.FeatureCollection;
import com.example.trm.placeyourguess.LocationListActivity;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import holders.BoundingBoxesHolder;
import holders.LocationInfoHolder;

public class LocationSelector {
    private FeatureCollection mBoxesFeatureCollection;
    private JSONArray mCountriesInfo;
    private ProgressDialog mProgressDialog;

    private LocationListActivity mContextActivity;
    private String mCountryCode;
    private int mNumOfLocations;
    private boolean mRandomLocation;
    private Intent mStartGameIntent;

    private static final String URL_BEGINNING = "http://maps.google.com/cbk?output=json&hl=en&ll=";
    private static final String URL_END = "&radius=10000&cb_client=maps_sv&v=4";

    public LocationSelector(Context context, Intent startGameIntent, int numberOfLocations, boolean randomLocation, String countryCode) {
        mNumOfLocations = numberOfLocations;
        mContextActivity = (LocationListActivity) context;
        mRandomLocation = randomLocation;
        mCountryCode = countryCode;
        mStartGameIntent = startGameIntent;

        BoundingBoxesHolder bbHolder = BoundingBoxesHolder.getInstance();
        mBoxesFeatureCollection = bbHolder.getBoxes();
        mCountriesInfo = bbHolder.getCountries();
    }

    public void selectLocations() {
        if (!mRandomLocation) {
            Pair<LatLng, LatLng> bounds = getBounds(mCountryCode);
            new ImageChecker().execute(bounds.first, bounds.second);
        } else {
            new ImageChecker().execute();
        }
    }

    public void selectLocations(Pair<LatLng, LatLng> bounds) {
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

    private boolean isAlreadySelected(List<LatLng> locations, LatLng location) {
        for (LatLng loc : locations) {
            if (loc.equals(location))
                return true;
        }
        return false;
    }

    private class ImageChecker extends AsyncTask<LatLng, Void, List<LatLng>> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = ProgressDialog.show(mContextActivity, "", "Please Wait", false);
        }

        @Override
        protected List<LatLng> doInBackground(LatLng... params) {
            LatLng southWest = null, northEast = null;
            if (!mRandomLocation) {
                southWest = params[0];
                northEast = params[1];
            }

            List<LatLng> locations = new ArrayList<>(mNumOfLocations);

            for (int i = 0; i < mNumOfLocations; i++) {
                JSONObject response = new JSONObject();
                String locationCountryCode = null;

                if (mRandomLocation) {
                    mCountryCode = LocationInfoHolder.getRandomCode();
                    Pair<LatLng, LatLng> bounds = getBounds(mCountryCode);
                    southWest = bounds.first;
                    northEast = bounds.second;
                }

                while (response == null || response.length() == 0 || locationCountryCode == null) {
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

                    if (!mRandomLocation && !mCountryCode.equals("custom") && !mCountryCode.equals(locationCountryCode)) {
                        response = null;
                    }
                }

                try {
                    JSONObject location = response.getJSONObject("Location");
                    double lat = location.getDouble("lat");
                    double lng = location.getDouble("lng");
                    LatLng latLng = new LatLng(lat, lng);
                    boolean alreadySelected = isAlreadySelected(locations, latLng);
                    if (!alreadySelected) {
                        locations.add(latLng);
                    } else {
                        i--;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            return locations;
        }

        @Override
        protected void onPostExecute(List<LatLng> result) {
            super.onPostExecute(result);

            double[] latitudes = new double[result.size()];
            double[] longitudes = new double[result.size()];

            for (int i = 0; i < result.size(); i++) {
                latitudes[i] = result.get(i).latitude;
                longitudes[i] = result.get(i).longitude;
            }

            mStartGameIntent.putExtra(LocationListActivity.EXTRA_LATITUDES, latitudes);
            mStartGameIntent.putExtra(LocationListActivity.EXTRA_LONGITUDES, longitudes);

            mContextActivity.startActivityForResult(mStartGameIntent, LocationListActivity.REQ_STREET_VIEW_ACTIVITY);
            mProgressDialog.dismiss();
        }
    }
}
