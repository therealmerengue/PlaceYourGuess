package holders;

import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

public final class LocationInfoHolder {

    private static LocationInfoHolder holder = null;
    private LocationInfoHolder() {}

    public static LocationInfoHolder getInstance() {
        return holder == null ? holder = new LocationInfoHolder() : holder;
    }

    private FeatureCollection mBoxesFeatureCollection;
    private JSONArray mCountries;
    private JSONArray mCities;

    public void loadBoxes(InputStream stream) {
        if (mBoxesFeatureCollection == null) {
            GeoJSONObject geoJSON = null;

            try {
                geoJSON = GeoJSON.parse(stream);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            mBoxesFeatureCollection = (FeatureCollection) geoJSON;
        }

        closeStream(stream);
    }

    public void loadCountries(InputStream stream) {
        try {
            String countriesJSONStr = IOUtils.toString(stream);
            mCountries = new JSONArray(countriesJSONStr);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        IOUtils.closeQuietly(stream);
    }

    public void loadCities(InputStream stream) {
        try {
            String citiesJSONStr = IOUtils.toString(stream);
            mCities = new JSONArray(citiesJSONStr);
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        IOUtils.closeQuietly(stream);
    }

    public FeatureCollection getBoxes() {
        if (mBoxesFeatureCollection == null) {
            throw new NullPointerException("Boxes not loaded.");
        } else {
            return mBoxesFeatureCollection;
        }
    }

    public JSONArray getCountries() {
        if (mCountries == null) {
            throw new NullPointerException("Countries not loaded.");
        } else {
            return mCountries;
        }
    }

    public JSONArray getCities() {
        if (mCities == null) {
            throw new NullPointerException("Cities not loaded.");
        } else {
            return mCities;
        }
    }

    private static void closeStream(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
