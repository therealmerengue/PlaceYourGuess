package com.example.trm.placeyourguess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DecimalFormat;

import logic.Calculator;

import static com.example.trm.placeyourguess.R.string.points;

public class MapActivity extends AppCompatActivity {

    private SupportMapFragment mFragMap;
    private GoogleMap mMap;
    private Marker mGuessedLocationMarker;
    private Marker mActualLocationMarker;
    private LatLng mPassedLocationCoords;

    private Button mBtnConfirm;
    private FloatingActionButton mBtnSwitchToStreetView;
    private FloatingActionButton mBtnHintDistance;
    private TextView mTxtRoundTimer;

    //score layout
    private LinearLayout mLayoutScore;
    private TextView mTxtPoints;
    private Button mBtnNextLocation;

    private CountDownTimer mRoundTimer;
    private boolean mGuessMade = false;
    private boolean mGuessConfirmed = false;
    private boolean mHintsEnabled = false;
    private int mScore;
    private float mCurrentMapZoom;
    private double[] mCurrentMapCenter = new double[2];

    //activity result variable keys
    final static String RESULT_KEY_SCORE = "SCORE";
    final static String RESULT_GUESSED_MARKER_LOCATION = "RESULT_GUESSED_MARKER_LOCATION";
    final static String RESULT_CURRENT_ZOOM = "RESULT_CURRENT_ZOOM";
    final static String RESULT_CURRENT_CENTER = "RESULT_CURRENT_CENTER";

    //SaveInstanceState variable keys
    private final static String KEY_SAVED_STATE_PASSED_LAT = "PASSED_LAT";
    private final static String KEY_SAVED_STATE_PASSED_LNG = "PASSED_LNG";
    private final static String KEY_SAVED_STATE_GUESSED_CONFIRMED = "KEY_SAVED_STATE_GUESSED_CONFIRMED";
    private final static String KEY_SAVED_STATE_HINTS_ENABLED = "HINTS_ENABLED";
    private final static String KEY_SAVED_STATE_PASSED_TIME_LEFT = "KEY_SAVED_STATE_PASSED_TIME_LEFT";
    private final static String KEY_SAVED_STATE_GUESSED_LOCATION = "KEY_SAVED_STATE_GUESSED_LOCATION";
    private final static String KEY_SAVED_STATE_CURRENT_ZOOM = "KEY_SAVED_STATE_CURRENT_ZOOM";
    private final static String KEY_SAVED_STATE_CURRENT_CENTER = "KEY_SAVED_STATE_CURRENT_CENTER";

    private float mGuessOffset;
    private long mPassedTimeLeft;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
        double[] passedCoords = new double[2];
        if (savedInstanceState == null) {
            passedCoords = intent.getDoubleArrayExtra(StreetViewActivity.EXTRA_LOCATION_COORDINATES);

            if (intent.hasExtra(MultiplayerRoomActivity.EXTRA_HINTS_ENABLED)) { //for multiplayer
                mHintsEnabled = intent.getBooleanExtra(MultiplayerRoomActivity.EXTRA_HINTS_ENABLED, false);
            } else { //for singleplayer
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
                mHintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);
            }
        } else {
            passedCoords[0] = savedInstanceState.getDouble(KEY_SAVED_STATE_PASSED_LAT);
            passedCoords[1] = savedInstanceState.getDouble(KEY_SAVED_STATE_PASSED_LNG);

            mHintsEnabled = savedInstanceState.getBoolean(KEY_SAVED_STATE_HINTS_ENABLED);

            mGuessConfirmed = savedInstanceState.getBoolean(KEY_SAVED_STATE_GUESSED_CONFIRMED);
        }
        mPassedLocationCoords = new LatLng(passedCoords[0], passedCoords[1]);

        mBtnSwitchToStreetView = (FloatingActionButton) findViewById(R.id.btn_switchToStreetView);
        mBtnSwitchToStreetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        //DISTANCE HINT
        if (mHintsEnabled) {
            mBtnHintDistance = (FloatingActionButton) findViewById(R.id.btn_hintDistance);
            mBtnHintDistance.setVisibility(View.VISIBLE);
            mBtnHintDistance.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mGuessedLocationMarker == null) {
                        Toast.makeText(MapActivity.this, "Place your guess to use this hint.", Toast.LENGTH_SHORT).show();
                    } else {
                        float distance = Calculator.measureDistance(mGuessedLocationMarker.getPosition(), mPassedLocationCoords) / 1000;
                        DecimalFormat df = new DecimalFormat("###.##");
                        Toast.makeText(MapActivity.this, "Your guess is " + df.format(distance) + " km off", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        mBtnConfirm = (Button) findViewById(R.id.btn_confirm);
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mGuessMade) {
                    mGuessMade = true;
                    mGuessConfirmed = true;

                    LatLng markerCoords = mGuessedLocationMarker.getPosition();
                    mGuessOffset = Calculator.measureDistance(markerCoords, mPassedLocationCoords);

                    PolylineOptions line = new PolylineOptions()
                            .add(mPassedLocationCoords, markerCoords)
                            .width(5)
                            .color(Color.BLUE);

                    mMap.addPolyline(line);

                    showResultOnMap();
                    showScore(mGuessOffset / 1000);

                    if (mRoundTimer != null) {
                        mRoundTimer.cancel();
                    }

                    StreetViewActivity.cancelCountDownTimer();
                } else {
                    mBtnNextLocation.callOnClick();
                }
            }
        });

        //SCORE LAYOUT
        mLayoutScore = (LinearLayout) findViewById(R.id.layout_score);
        mTxtPoints = (TextView) findViewById(R.id.txt_Points);

        mBtnNextLocation = (Button) findViewById(R.id.btn_nextLocation);
        mBtnNextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle resultData = new Bundle();
                resultData.putInt(RESULT_KEY_SCORE, mScore);
                Intent intent = new Intent();
                intent.putExtras(resultData);

                setResult(RESULT_OK, intent);

                if (mRoundTimer != null) {
                    mRoundTimer.cancel();
                }
                finish();
            }
        });

        //INITIALIZING TIMER
        if (savedInstanceState == null) {
            mPassedTimeLeft = intent.getLongExtra(StreetViewActivity.EXTRA_TIMER_LEFT, -1);
        } else {
            mPassedTimeLeft = savedInstanceState.getLong(KEY_SAVED_STATE_PASSED_TIME_LEFT);
        }
        initTimer();

        mFragMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frag_map);
        mFragMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                UiSettings settings = mMap.getUiSettings();
                settings.setMapToolbarEnabled(false);
                settings.setCompassEnabled(false);
                settings.setRotateGesturesEnabled(false);
                settings.setIndoorLevelPickerEnabled(false);

                if (savedInstanceState != null) {
                    mCurrentMapZoom = savedInstanceState.getFloat(KEY_SAVED_STATE_CURRENT_ZOOM);

                    if (savedInstanceState.containsKey(KEY_SAVED_STATE_GUESSED_LOCATION)) {
                        double[] guessedLocation = savedInstanceState.getDoubleArray(KEY_SAVED_STATE_GUESSED_LOCATION);
                        LatLng position = new LatLng(guessedLocation[0], guessedLocation[1]);
                        mGuessedLocationMarker = mMap.addMarker(new MarkerOptions().position(position));

                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, mCurrentMapZoom));
                        mBtnConfirm.setEnabled(true);
                    } else {
                        mCurrentMapCenter = savedInstanceState.getDoubleArray(KEY_SAVED_STATE_CURRENT_CENTER);
                        LatLng position = new LatLng(mCurrentMapCenter[0], mCurrentMapCenter[1]);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, mCurrentMapZoom));
                    }

                    if (mGuessConfirmed) {
                        mBtnConfirm.callOnClick();
                    }

                } else {
                    Intent intent = getIntent();
                    float zoom = 1f;
                    if (intent.hasExtra(StreetViewActivity.EXTRA_PREVIOUS_MAP_ZOOM)) {
                        zoom = intent.getFloatExtra(StreetViewActivity.EXTRA_PREVIOUS_MAP_ZOOM, 1f);
                    }
                    if (intent.hasExtra(StreetViewActivity.EXTRA_PREVIOUSLY_PLACED_MARKER)) {
                        LatLng position = positionArrayToLatLng(intent.getDoubleArrayExtra(StreetViewActivity.EXTRA_PREVIOUSLY_PLACED_MARKER));
                        mGuessedLocationMarker = mMap.addMarker(new MarkerOptions().position(position));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
                        mBtnConfirm.setEnabled(true);
                    } else if (intent.hasExtra(StreetViewActivity.EXTRA_PREVIOUS_MAP_CENTER)) {
                        LatLng position = positionArrayToLatLng(intent.getDoubleArrayExtra(StreetViewActivity.EXTRA_PREVIOUS_MAP_CENTER));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, zoom));
                    }
                }

                if (mPassedTimeLeft == 0 && mPassedTimeLeft != -1) {
                    showResultOnMap();
                } else {
                    mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng latLng) {
                            if (mGuessedLocationMarker != null) {
                                mGuessedLocationMarker.remove();
                            }
                            mGuessedLocationMarker = mMap.addMarker(new MarkerOptions().position(latLng));

                            mBtnConfirm.setEnabled(true);
                        }
                    });

                    mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                        @Override
                        public boolean onMarkerClick(Marker marker) {
                            if (mActualLocationMarker != null) {
                                mActualLocationMarker.showInfoWindow();
                            }
                            return true;
                        }
                    });

                    mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
                        @Override
                        public void onCameraMove() {
                            mCurrentMapZoom = mMap.getCameraPosition().zoom;
                            LatLng mapCenter = mMap.getCameraPosition().target;
                            mCurrentMapCenter[0] = mapCenter.latitude;
                            mCurrentMapCenter[1] = mapCenter.longitude;
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_SAVED_STATE_PASSED_TIME_LEFT, mPassedTimeLeft);

        outState.putBoolean(KEY_SAVED_STATE_GUESSED_CONFIRMED, mGuessConfirmed);

        outState.putDouble(KEY_SAVED_STATE_PASSED_LAT, mPassedLocationCoords.latitude);
        outState.putDouble(KEY_SAVED_STATE_PASSED_LNG, mPassedLocationCoords.longitude);

        outState.putDoubleArray(KEY_SAVED_STATE_CURRENT_CENTER, mCurrentMapCenter);
        outState.putFloat(KEY_SAVED_STATE_CURRENT_ZOOM, mCurrentMapZoom);

        if (mGuessedLocationMarker != null) {
            LatLng position = mGuessedLocationMarker.getPosition();
            double[] positionArray = new double[2];
            positionArray[0] = position.latitude;
            positionArray[1] = position.longitude;
            outState.putDoubleArray(KEY_SAVED_STATE_GUESSED_LOCATION, positionArray);
        }

        outState.putBoolean(KEY_SAVED_STATE_HINTS_ENABLED, mHintsEnabled);
    }

    @Override
    public void onBackPressed() {
        if (mGuessMade) {
            mBtnNextLocation.callOnClick();
        } else if (mGuessedLocationMarker != null) {
            Bundle resultData = new Bundle();
            LatLng position = mGuessedLocationMarker.getPosition();
            double[] positionArray = new double[2];
            positionArray[0] = position.latitude;
            positionArray[1] = position.longitude;
            resultData.putDoubleArray(RESULT_GUESSED_MARKER_LOCATION, positionArray);
            resultData.putFloat(RESULT_CURRENT_ZOOM, mCurrentMapZoom);

            finishActivityWithNoGuess(resultData);
        } else {
            Bundle resultData = new Bundle();
            resultData.putDoubleArray(RESULT_CURRENT_CENTER, mCurrentMapCenter);
            resultData.putFloat(RESULT_CURRENT_ZOOM, mCurrentMapZoom);
            finishActivityWithNoGuess(resultData);
        }
    }

    private void finishActivityWithNoGuess(Bundle resultData) {
        Intent intent = new Intent();
        intent.putExtras(resultData);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    private void showResultOnMap() {
        mActualLocationMarker = mMap.addMarker(new MarkerOptions()
                .position(mPassedLocationCoords)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        if (mGuessConfirmed) {
            String distanceSnippet;
            DecimalFormat df = new DecimalFormat("###.##");

            if (mGuessOffset < 1000) {
                distanceSnippet = df.format(mGuessOffset) + " m";
            } else {
                distanceSnippet = df.format(mGuessOffset / 1000) + " km";
            }

            mActualLocationMarker.setTitle("Distance");
            mActualLocationMarker.setSnippet(distanceSnippet);
            mActualLocationMarker.showInfoWindow();
        }

        if (mMap.getCameraPosition().zoom < 3) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(mPassedLocationCoords, 3f));
        } else {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(mPassedLocationCoords));
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() { //to prevent infoWindow from disappearing
            @Override
            public void onMapClick(LatLng latLng) {
                mActualLocationMarker.showInfoWindow();
            }
        });
    }

    private void showScore(float distance) {
        mLayoutScore.setVisibility(View.VISIBLE);
        if (mTxtRoundTimer != null)
            mTxtRoundTimer.setVisibility(View.GONE);
        if (mGuessConfirmed) {
            mScore = Calculator.calculatePoints(distance);
        } else {
            mScore = 0;
        }
        String pointsLabel = getString(points) + " " + mScore;

        mTxtPoints.setText(pointsLabel);
    }

    private void initTimer() {
        if (mPassedTimeLeft != -1) {
            mTxtRoundTimer = (TextView) findViewById(R.id.txt_RoundTimer);
            mTxtRoundTimer.setText(Long.toString(mPassedTimeLeft));

            if (mPassedTimeLeft == 0) {
                showScore(0);
                mGuessMade = true;
            } else {
                mRoundTimer = new CountDownTimer(mPassedTimeLeft * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        mPassedTimeLeft = millisUntilFinished / 1000;
                        mTxtRoundTimer.setText(Long.toString(mPassedTimeLeft));
                    }

                    @Override
                    public void onFinish() {
                        if (mGuessedLocationMarker != null) {
                            mBtnConfirm.callOnClick();
                        } else {
                            mGuessMade = true;
                            if (mMap != null) {
                                showResultOnMap();
                            }
                            mTxtRoundTimer.setText("0");
                            showScore(0);
                        }
                    }
                }.start();
            }
        }
    }

    private LatLng positionArrayToLatLng(double[] positionArray) {
        return new LatLng(positionArray[0], positionArray[1]);
    }
}
