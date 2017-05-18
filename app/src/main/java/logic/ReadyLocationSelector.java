package logic;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import holders.LocationInfoHolder;

public class ReadyLocationSelector {
    private JSONArray mCitiesInfo;
    private JSONArray mFamousPlacesInfo;

    public ReadyLocationSelector() {
        LocationInfoHolder holder = LocationInfoHolder.getInstance();
        mCitiesInfo = holder.getCities();
        mFamousPlacesInfo = holder.getFamousPlaces();
    }

    private boolean isAlreadySelected(List<Integer> selectedLocationsIndexes, int index) {
        for (int i = 0; i < selectedLocationsIndexes.size(); i++) {
            if (selectedLocationsIndexes.get(i).equals(index))
                return true;
        }
        return false;
    }

    private LatLng getRandomPlace(JSONArray places, List<Integer> selectedIndexes) {
        Random random = new Random(System.currentTimeMillis());
        int placeIndex = random.nextInt(places.length());
        while (isAlreadySelected(selectedIndexes, placeIndex)) {
            placeIndex = random.nextInt(places.length());
        }
        selectedIndexes.add(placeIndex);

        LatLng placeLatLng = null;
        try {
            JSONArray place = places.getJSONArray(placeIndex);
            double lat = place.getDouble(0);
            double lng = place.getDouble(1);
            placeLatLng = new LatLng(lat, lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return placeLatLng;
    }

    public List<LatLng> selectPlaces(int numberOfPlaces, int mode) { //0 - cities, 1 - famous places
        List<LatLng> places = new ArrayList<>(numberOfPlaces);
        List<Integer> selectedPlacesIndexes = new ArrayList<>(numberOfPlaces);

        JSONArray placesToSelectFrom = new JSONArray();
        switch (mode) {
            case 0:
                placesToSelectFrom = mCitiesInfo;
                break;
            case 1:
                placesToSelectFrom = mFamousPlacesInfo;
                break;
            default:
                break;
        }

        for (int i = 0; i < numberOfPlaces; i++) {
            LatLng place = getRandomPlace(placesToSelectFrom, selectedPlacesIndexes);
            places.add(place);
        }

        return places;
    }
}
