package com.example.trm.placeyourguess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static io.socket.client.Socket.EVENT_CONNECT;

public class CountryListActivity extends AppCompatActivity {

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

    static final int REQ_STREET_VIEW_ACTIVITY = 101;
    static final int REQ_SCORE_SP_ACTIVITY = 102;

    static final String EXTRA_LATITUDES = "LATITUDES";
    static final String EXTRA_LONGITUDES = "LONGITUDES";
    static final String EXTRA_SCORE = "EXTRA_SCORE";

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

        CountryListAdapter adapter = new CountryListAdapter(this, CountryInfoHolder.mCountryNames, CountryInfoHolder.mImgIDs);
        mListviewCountries = (ListView) findViewById(R.id.lv_countryList);
        mListviewCountries.setAdapter(adapter);

        mListviewCountries.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                boolean randomCountry = false;

                mStartGameIntent = new Intent(CountryListActivity.this, StreetViewActivity.class);
                mStartGameIntent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, mIsSingleplayer);
                String selectedCountryCode = null;

                if (position != 0) { //start streetViewActivity with country code.
                    selectedCountryCode = CountryInfoHolder.mCountryCodes[position - 1];
                } else {
                    randomCountry = true;
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(CountryListActivity.this);
                String numberOfRoundsStr = preferences.getString(getString(R.string.settings_numOfRounds), "5");
                mNumOfRounds = Integer.parseInt(numberOfRoundsStr);

                if (!mIsSingleplayer) { //MULTIPLAYER
                    String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                    int timerLimit = Integer.parseInt(timerLimitStr);

                    mSocket.emit("loadLocations", getSettings(randomCountry, selectedCountryCode, timerLimit));
                    Log.e("loadLocations", "host emits load locations");

                    finish();
                } else {
                    if (mIsConnected) { //SINGLEPLAYER
                        //get locations from socket
                        mSocket.emit("loadLocations", getSettings(randomCountry, selectedCountryCode, -1));
                    } else {
                        //load locations on phone
                        LocationSelector selector = new LocationSelector(CountryListActivity.this, mStartGameIntent, mNumOfRounds, randomCountry, selectedCountryCode);
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

            //case REQ_SCORE_MP_ACTIVITY - the same in MultiplayerActivity for non host players.
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

    private JSONObject getSettings(boolean randomCountry, String countryCode, int timerLimit) {
        JSONObject settings = new JSONObject();

        try {
            settings.put("isSingleplayer", mIsSingleplayer);
            if (!mIsSingleplayer) {
                settings.put("timerLimit", timerLimit);
            }
            settings.put("numberOfRounds", mNumOfRounds);
            settings.put("randomCountry", randomCountry);
            if (!randomCountry) {
                settings.put("countryCode", countryCode);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return settings;
    }
}