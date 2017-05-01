package logic;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import holders.LocationInfoHolder;

public class CitySelector {
    private JSONArray mCitiesInfo;

    public CitySelector() {
        LocationInfoHolder holder = LocationInfoHolder.getInstance();
        mCitiesInfo = holder.getCities();
    }

    private boolean isAlreadySelected(List<Integer> selectedCitiesIndexes, int index) {
        for (int i = 0; i < selectedCitiesIndexes.size(); i++) {
            if (selectedCitiesIndexes.get(i).equals(index))
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

    public List<LatLng> selectCities(int numberOfCities) {
        List<LatLng> cities = new ArrayList<>(numberOfCities);
        List<Integer> selectedCitiesIndexes = new ArrayList<>(numberOfCities);

        for (int i = 0; i < numberOfCities; i++) {
            LatLng city = getRandomCity(selectedCitiesIndexes);
            cities.add(city);
        }

        return cities;
    }
}
