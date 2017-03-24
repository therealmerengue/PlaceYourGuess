package com.example.trm.placeyourguess;

import com.cocoahero.android.geojson.FeatureCollection;
import com.cocoahero.android.geojson.GeoJSON;
import com.cocoahero.android.geojson.GeoJSONObject;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

public final class BoundingBoxesHolder {

    private static BoundingBoxesHolder holder = null;
    private BoundingBoxesHolder() {}

    public static BoundingBoxesHolder getInstance() {
        return holder == null ? holder = new BoundingBoxesHolder() : holder;
    }

    private FeatureCollection mBoxesFeatureCollection = null;
    private JSONArray mCountries = null;

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

    private static void closeStream(InputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
