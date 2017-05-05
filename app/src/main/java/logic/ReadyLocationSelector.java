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

    private LatLng getRandomCity(List<Integer> selectedCitiesIndexes) {
        Random random = new Random(System.currentTimeMillis());
        int cityIndex = random.nextInt(mCitiesInfo.length());
        while (isAlreadySelected(selectedCitiesIndexes, cityIndex)) {
            cityIndex = random.nextInt(mCitiesInfo.length());
        }
        selectedCitiesIndexes.add(cityIndex);

        LatLng cityLatLng = null;
        try {
            JSONArray city = mCitiesInfo.getJSONArray(cityIndex);
            double lat = city.getDouble(0);
            double lng = city.getDouble(1);
            cityLatLng = new LatLng(lat, lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return cityLatLng;
    }

    private LatLng getRandomFamousPlace(List<Integer> selectedPlacesIndexes) {
        Random random = new Random(System.currentTimeMillis());
        int placeIndex = random.nextInt(mFamousPlacesInfo.length());
        while (isAlreadySelected(selectedPlacesIndexes, placeIndex)) {
            placeIndex = random.nextInt(mFamousPlacesInfo.length());
        }
        selectedPlacesIndexes.add(placeIndex);

        LatLng placeLatLng = null;
        try {
            JSONArray place = mFamousPlacesInfo.getJSONArray(placeIndex);
            double lat = place.getDouble(0);
            double lng = place.getDouble(1);
            placeLatLng = new LatLng(lat, lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return placeLatLng;
    }

    public List<LatLng> selectCities(int numberOfCities) {
        List<LatLng> cities = new ArrayList<>(numberOfCities);
        List<Integer> selectedCitiesIndexes = new ArrayList<>(numberOfCities);

        for (int i = 0; i < numberOfCities; i++) {
            LatLng city = getRandomCity(selectedCitiesIndexes);
            cities.add(city);
        }

        return cities;
    }

    public List<LatLng> selectFamousPlaces(int numberOfPlaces) {
        List<LatLng> places = new ArrayList<>(numberOfPlaces);
        List<Integer> selectedPlacesIndexes = new ArrayList<>(numberOfPlaces);

        for (int i = 0; i < numberOfPlaces; i++) {
            LatLng place = getRandomFamousPlace(selectedPlacesIndexes);
            places.add(place);
        }

        return places;
    }
}
