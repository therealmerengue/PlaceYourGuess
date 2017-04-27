package com.example.trm.placeyourguess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import adapters.LocationListAdapter;
import holders.LocationInfoHolder;
import holders.SocketHolder;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import logic.LocationSelector;

import static io.socket.client.Socket.EVENT_CONNECT;

public class LocationListActivity extends AppCompatActivity {

    private ListView mListviewCountries;

    private boolean mIsSingleplayer = true;
    private boolean mIsConnected = false;
    private int mNumOfRounds;

    private Socket mSocket;

    private final String EVENT_START_SINGLEPLAYER_GAME = "startSingleplayerGame";

    private Emitter.Listener onConnectListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("SOCK", "EVENT_CONNECT");
            mIsConnected = true;
        }
    };
    private Emitter.Listener onDisconnectListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mIsConnected = false;
        }
    };
    private Emitter.Listener onConnectErrorListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("SOCK", "EVENT_CONNECT_ERROR");
            mIsConnected = false;
        }
    };
    private Emitter.Listener onStartSingleplayerGameListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONArray locations = (JSONArray) args[0];
            int numOfRounds = locations.length();
            double[] latitudes = new double[numOfRounds];
            double[] longitudes = new double[numOfRounds];

            for (int i = 0; i < numOfRounds; i++) {
                try {
                    JSONObject location = locations.getJSONObject(i);
                    latitudes[i] = location.getDouble("lat");
                    longitudes[i] = location.getDouble("lng");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Log.e("SERV", "Starting game");
            mStartGameIntent.putExtra(EXTRA_LATITUDES, latitudes);
            mStartGameIntent.putExtra(EXTRA_LONGITUDES, longitudes);
            startActivityForResult(mStartGameIntent, REQ_STREET_VIEW_ACTIVITY);
        }
    };

    private Intent mStartGameIntent;

    public static final int REQ_STREET_VIEW_ACTIVITY = 101;
    static final int REQ_SCORE_SP_ACTIVITY = 102;
    static final int REQ_CUSTOM_LOCATION_ACTIVITY = 103;

    public static final String EXTRA_LATITUDES = "LATITUDES";
    public static final String EXTRA_LONGITUDES = "LONGITUDES";
    public static final String EXTRA_SCORE = "EXTRA_SCORE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_list);

        mSocket = SocketHolder.getInstance();
        mSocket.on(EVENT_CONNECT, onConnectListener)
                .on(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener)
                .on(Socket.EVENT_DISCONNECT, onDisconnectListener)
                .on(EVENT_START_SINGLEPLAYER_GAME, onStartSingleplayerGameListener);
        if (!mSocket.connected())
            mSocket.connect();

        mIsSingleplayer = getIntent().getBooleanExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, true);

        LocationListAdapter adapter = new LocationListAdapter(this, LocationInfoHolder.mCountryNames, LocationInfoHolder.mImgIDs);
        mListviewCountries = (ListView) findViewById(R.id.lv_countryList);
        mListviewCountries.setAdapter(adapter);

        mListviewCountries.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean randomCountry = false;

                mStartGameIntent = new Intent(LocationListActivity.this, StreetViewActivity.class);
                mStartGameIntent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, mIsSingleplayer);
                String selectedCountryCode = null;

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LocationListActivity.this);
                String numberOfRoundsStr = preferences.getString(getString(R.string.settings_numOfRounds), "5");
                mNumOfRounds = Integer.parseInt(numberOfRoundsStr);

                if (position == 0) {
                    randomCountry = true;
                } else if (position == 1) {
                    Intent customLocationIntent = new Intent(LocationListActivity.this, CustomLocationActivity.class);
                    startActivityForResult(customLocationIntent, REQ_CUSTOM_LOCATION_ACTIVITY);
                    return; //return at the end? <- yes
                } else  { //start streetViewActivity with country code.
                    selectedCountryCode = LocationInfoHolder.mCountryCodes[position - 2];
                }

                if (!mIsSingleplayer) { //MULTIPLAYER
                    String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                    int timerLimit = Integer.parseInt(timerLimitStr);
                    boolean hintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);

                    mSocket.emit("loadLocations", getSettings(randomCountry, selectedCountryCode, timerLimit, hintsEnabled, null));
                    Log.e("loadLocations", "host emits load locations");

                    finish();
                } else {
                    if (mIsConnected) { //SINGLEPLAYER
                        //get locations from socket
                        mSocket.emit("loadLocations", getSettings(randomCountry, selectedCountryCode, -1, false, null));
                    } else {
                        //load locations on phone
                        LocationSelector selector = new LocationSelector(LocationListActivity.this, mStartGameIntent, mNumOfRounds, randomCountry, selectedCountryCode);
                        selector.selectLocations();
                    }
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_STREET_VIEW_ACTIVITY:
                if (data != null) {
                    Bundle resultData = data.getExtras();
                    int score = resultData.getInt(StreetViewActivity.RESULT_KEY_SCORE);

                    if (mIsSingleplayer) {
                        Intent intent = new Intent(this, ScoreSPActivity.class);
                        intent.putExtra(EXTRA_SCORE, score);
                        startActivityForResult(intent, REQ_SCORE_SP_ACTIVITY);
                    }
                }
                break;

            case REQ_SCORE_SP_ACTIVITY:
                if (data != null) {
                    Bundle resultData = data.getExtras();
                    boolean playAgain = resultData.getBoolean(ScoreSPActivity.RESULT_KEY_PLAY_AGAIN);
                    if (!playAgain) {
                        finish();
                    }
                }
                break;

            case REQ_CUSTOM_LOCATION_ACTIVITY:
                if (data != null) {
                    Bundle resultData = data.getExtras();
                    double[] latitudeBounds = resultData.getDoubleArray(CustomLocationActivity.RESULT_KEY_LATITUDE_BOUNDS);
                    double[] longitudeBounds = resultData.getDoubleArray(CustomLocationActivity.RESULT_KEY_LONGITUDE_BOUNDS);

                    Pair<LatLng, LatLng> bounds = new Pair<>(new LatLng(latitudeBounds[0], longitudeBounds[0]),
                            new LatLng(latitudeBounds[1], longitudeBounds[1]));

                    final String countryCode = "custom";

                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LocationListActivity.this);

                    if (!mIsSingleplayer) { //MULTIPLAYER
                        String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                        int timerLimit = Integer.parseInt(timerLimitStr);
                        boolean hintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);

                        mSocket.emit("loadLocations", getSettings(false, countryCode, timerLimit, hintsEnabled, bounds));
                        Log.e("loadLocations", "host emits load locations");

                        finish();
                    } else {
                        if (mIsConnected) { //SINGLEPLAYER
                            //get locations from socket
                            mSocket.emit("loadLocations", getSettings(false, countryCode, -1, false, bounds));
                        } else {
                            //load locations on phone
                            LocationSelector selector = new LocationSelector(LocationListActivity.this, mStartGameIntent, mNumOfRounds, false, countryCode);
                            selector.selectLocations(bounds);
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsSingleplayer)
            mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnectListener);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnectListener);
        mSocket.off("startSingleplayerGame", onStartSingleplayerGameListener);
    }

    private JSONObject getSettings(boolean randomCountry, String countryCode, int timerLimit, boolean hintsEnabled, Pair<LatLng, LatLng> bounds) {
        JSONObject settings = new JSONObject();

        try {
            settings.put("isSingleplayer", mIsSingleplayer);
            if (!mIsSingleplayer) {
                settings.put("timerLimit", timerLimit);
                settings.put("hintsEnabled", hintsEnabled);
            }
            settings.put("numberOfRounds", mNumOfRounds);
            settings.put("randomCountry", randomCountry);

            if (!randomCountry) {
                settings.put("countryCode", countryCode);

                if (countryCode.equals("custom")) {
                    LatLng minimums = bounds.first;
                    LatLng maximums = bounds.second;

                    settings.put("minLat", minimums.latitude);
                    settings.put("maxLat", maximums.latitude);
                    settings.put("minLng", minimums.longitude);
                    settings.put("maxLng", maximums.longitude);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return settings;
    }
}