package com.example.trm.placeyourguess;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.Surface;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import adapters.LocationListAdapter;
import holders.LocationListItemsHolder;
import holders.SocketHolder;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import logic.Calculator;
import logic.LocationSelector;
import logic.ReadyLocationSelector;

import static com.example.trm.placeyourguess.CustomLocationActivity.RESULT_KEY_LATITUDE_BOUNDS;
import static com.example.trm.placeyourguess.CustomLocationActivity.RESULT_KEY_LONGITUDE_BOUNDS;
import static io.socket.client.Socket.EVENT_CONNECT;

public class LocationListActivity extends AppCompatActivity {

    private ListView mListviewCountries;

    private ProgressDialog mProgressDialog;

    private boolean mIsSingleplayer = true;
    private boolean mIsConnected = false;
    private int mNumOfRounds;

    private Socket mSocket;

    private GoogleApiClient mGoogleApiClient;
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Pair<LatLng, LatLng> boundingBox = locationToBoundingBox(location, 5);
            startGameWithCustomLocation(boundingBox);

            if (mLocationProgressDialog != null && mLocationProgressDialog.isShowing())
                mLocationProgressDialog.dismiss();

            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    };
    private ProgressDialog mLocationProgressDialog;

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

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LocationListActivity.this, "Connection lost.", Toast.LENGTH_LONG).show();
                    dismissProgressDialog(mProgressDialog);
                }
            });
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

            dismissProgressDialog(mProgressDialog);
        }
    };

    private Intent mStartGameIntent;

    public static final int REQ_STREET_VIEW_ACTIVITY = 101;
    static final int REQ_SCORE_SP_ACTIVITY = 102;
    static final int REQ_CUSTOM_LOCATION_ACTIVITY = 103;

    static final int PERMISSION_ACCESS_FINE_LOCATION = 101;

    public static final String EXTRA_LATITUDES = "LATITUDES";
    public static final String EXTRA_LONGITUDES = "LONGITUDES";
    public static final String EXTRA_SCORE = "EXTRA_SCORE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_country_list);

        mSocket = SocketHolder.getInstance();
        mIsConnected = mSocket.connected();
        mSocket.on(EVENT_CONNECT, onConnectListener)
                .on(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener)
                .on(Socket.EVENT_DISCONNECT, onDisconnectListener)
                .on(EVENT_START_SINGLEPLAYER_GAME, onStartSingleplayerGameListener);

        mIsSingleplayer = getIntent().getBooleanExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, true);

        LocationListAdapter adapter = new LocationListAdapter(this, LocationListItemsHolder.mCountryNames, LocationListItemsHolder.mImgIDs);
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
                    return;
                } else if (position == 2 || position == 3) {
                    if (!mIsSingleplayer) {
                        String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                        int timerLimit = Integer.parseInt(timerLimitStr);
                        boolean hintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);
                        selectedCountryCode = "ready";

                        JSONObject settings = getSettings(false, selectedCountryCode, timerLimit, hintsEnabled, null);

                        try {
                            if (position == 2)
                                settings.put("locationType", 1); //cities
                            else
                                settings.put("locationType", 2); //famous places
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        mSocket.emit("loadReadyLocations", settings);
                        Log.e("loadLocations", "host emits load city locations");

                        setResult(RESULT_OK);
                        finish();
                    } else {
                        ReadyLocationSelector selector = new ReadyLocationSelector();
                        List<LatLng> locations;
                        if (position == 2)
                            locations = selector.selectPlaces(mNumOfRounds, 0);
                        else
                            locations = selector.selectPlaces(mNumOfRounds, 1);

                        double[] latitudes = new double[mNumOfRounds];
                        double[] longitudes = new double[mNumOfRounds];

                        for (int i = 0; i < mNumOfRounds; i++) {
                            LatLng locLatLng = locations.get(i);
                            latitudes[i] = locLatLng.latitude;
                            longitudes[i] = locLatLng.longitude;
                        }

                        mStartGameIntent.putExtra(LocationListActivity.EXTRA_LATITUDES, latitudes);
                        mStartGameIntent.putExtra(LocationListActivity.EXTRA_LONGITUDES, longitudes);

                        startActivityForResult(mStartGameIntent, LocationListActivity.REQ_STREET_VIEW_ACTIVITY);
                        return;
                    }
                } else if (position == 4) { //neighbourhood mode
                    mGoogleApiClient = new GoogleApiClient.Builder(LocationListActivity.this)
                            .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(@Nullable Bundle bundle) {
                                    if (ActivityCompat.checkSelfPermission(LocationListActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        ActivityCompat.requestPermissions(LocationListActivity.this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                                                PERMISSION_ACCESS_FINE_LOCATION);
                                        return;
                                    }

                                    Location lastUserLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                                    if (lastUserLocation == null) {
                                        if (isLocationEnabled(LocationListActivity.this)) {
                                            LocationRequest locationRequest = LocationRequest.create()
                                                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                                                    .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                                                    .setFastestInterval(1000); // 1 second, in milliseconds
                                            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, mLocationListener);

                                            mLocationProgressDialog = new ProgressDialog(LocationListActivity.this);
                                            mLocationProgressDialog.setTitle("Retrieving your location...");
                                            mLocationProgressDialog.setMessage("Please wait");
                                            mLocationProgressDialog.setCancelable(false);
                                            mLocationProgressDialog.setCanceledOnTouchOutside(false);
                                            mLocationProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, mLocationListener);
                                                    unlockOrientation();
                                                }
                                            });
                                            mLocationProgressDialog.show();

                                            lockOrientation();
                                        }
                                        else {
                                            Toast.makeText(LocationListActivity.this, "Enable location.", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Pair<LatLng, LatLng> boundingBox = locationToBoundingBox(lastUserLocation, 5);
                                        startGameWithCustomLocation(boundingBox);
                                    }
                            }

                            @Override public void onConnectionSuspended(int i) {}
                        })
                        .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                            @Override public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}
                        })
                        .addApi(LocationServices.API)
                        .build();

                    mGoogleApiClient.connect();
                    return;
                }
                else  { //start streetViewActivity with country code.
                    selectedCountryCode = LocationListItemsHolder.mCountryCodes[position - 5];
                }

                if (!mIsSingleplayer) { //MULTIPLAYER
                    String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                    int timerLimit = Integer.parseInt(timerLimitStr);
                    boolean hintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);

                    mSocket.emit("loadLocations", getSettings(randomCountry, selectedCountryCode, timerLimit, hintsEnabled, null));
                    Log.e("loadLocations", "host emits load locations");

                    setResult(RESULT_OK);
                    finish();
                } else {
                    if (mIsConnected) { //SINGLEPLAYER
                        //get locations from socket
                        mSocket.emit("loadLocations", getSettings(randomCountry, selectedCountryCode, -1, false, null));
                        showProgressDialog();
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
                    double[] latitudeBounds = resultData.getDoubleArray(RESULT_KEY_LATITUDE_BOUNDS);
                    double[] longitudeBounds = resultData.getDoubleArray(RESULT_KEY_LONGITUDE_BOUNDS);

                    Pair<LatLng, LatLng> bounds = new Pair<>(new LatLng(latitudeBounds[0], longitudeBounds[0]),
                            new LatLng(latitudeBounds[1], longitudeBounds[1]));

                    startGameWithCustomLocation(bounds);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //if (mIsSingleplayer)
            //mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnectListener);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnectListener);
        mSocket.off("startSingleplayerGame", onStartSingleplayerGameListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // All good!
                } else {
                    Toast.makeText(this, "Your location is needed for this mode to work.", Toast.LENGTH_SHORT).show();
                }

                break;
        }
    }

    private Pair<LatLng, LatLng> locationToBoundingBox(Location location, double halfSideInKm) {
        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
        return Calculator.getBoundingBox(center, halfSideInKm);
    }

    private void startGameWithCustomLocation(Pair<LatLng, LatLng> bounds) {
        final String countryCode = "custom";

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LocationListActivity.this);

        if (!mIsSingleplayer) { //MULTIPLAYER
            String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
            int timerLimit = Integer.parseInt(timerLimitStr);
            boolean hintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);

            mSocket.emit("loadLocations", getSettings(false, countryCode, timerLimit, hintsEnabled, bounds));
            Log.e("loadLocations", "host emits load locations");

            setResult(RESULT_OK);
            finish();
        } else {
            if (mIsConnected) { //SINGLEPLAYER
                //get locations from socket
                mSocket.emit("loadLocations", getSettings(false, countryCode, -1, false, bounds));
                showProgressDialog();
            } else {
                //load locations on phone
                LocationSelector selector = new LocationSelector(LocationListActivity.this, mStartGameIntent, mNumOfRounds, false, countryCode);
                selector.selectLocations(bounds);
            }
        }
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

    private void showProgressDialog() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle("Loading locations...");
        mProgressDialog.setMessage("Please wait");
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
        lockOrientation();
    }

    private void dismissProgressDialog(ProgressDialog dialog) {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();

        unlockOrientation();
    }

    public void lockOrientation() {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch(rotation) {
            case Surface.ROTATION_180:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case Surface.ROTATION_270:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
            case Surface.ROTATION_0:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Surface.ROTATION_90:
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }

    public void unlockOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }
}