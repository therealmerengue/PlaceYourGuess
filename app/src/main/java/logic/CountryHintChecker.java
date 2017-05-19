package logic;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

public class CountryHintChecker {
    private LatLng latLng1, latLng2;
    private Context context;

    public CountryHintChecker(Context context, LatLng latLng1, LatLng latLng2) {
        this.context = context;
        this.latLng1 = latLng1;
        this.latLng2 = latLng2;
    }

    public void checkCountry() {
        new CountryChecker().execute();
    }

    private class CountryChecker extends AsyncTask<Void, Void, Boolean> {

        private static final String URL_START = "http://ws.geonames.org/countryCodeJSON?lat=";
        private static final String URL_MID = "&lng=";
        private static final String URL_END = "&username=demo";

        private boolean isResponseInvalid(JSONObject response) {
            return response.has("status");
        }

        private String getCountryCodeFromResponse(JSONObject response) {
            String countryCode = null;
            try {
                countryCode = response.getString("countryCode");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return countryCode;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            String URL1 = URL_START + Double.toString(latLng1.latitude) + URL_MID + Double.toString(latLng1.longitude) + URL_END;
            String URL2 = URL_START + Double.toString(latLng2.latitude) + URL_MID + Double.toString(latLng2.longitude) + URL_END;

            JSONObject response1 = LocationSelector.readJsonFromUrl(URL1);
            JSONObject response2 = LocationSelector.readJsonFromUrl(URL2);

            if (response1 == null || response2 == null || isResponseInvalid(response1) || isResponseInvalid(response2))
                return false;

            String countryCode1 = getCountryCodeFromResponse(response1);
            String countryCode2 = getCountryCodeFromResponse(response2);

            return !(countryCode1 == null || countryCode2 == null) && countryCode1.equals(countryCode2);
        }

        @Override
        protected void onPostExecute(Boolean isCountryCorrect) {
            super.onPostExecute(isCountryCorrect);

            String message = isCountryCorrect ? "Correct country." : "Wrong country.";

            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
