package com.example.trm.placeyourguess;

import android.os.CountDownTimer;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapActivity extends AppCompatActivity {

    private SupportMapFragment mFragMap;
    private GoogleMap mMap;
    private Marker mGuessedLocationMarker;
    private LatLng mPassedLocationCoords;

    private Button mBtnConfirm;
    private FloatingActionButton mBtnSwitchToStreetView;
    private TextView mTxtRoundTimer;

    //score layout
    private LinearLayout mLayoutScore;
    private TextView mTxtDistance;
    private TextView mTxtPoints;
    private TextView mTxtTotalScore;
    private FloatingActionButton mBtnNextLocation;

    private CountDownTimer mRoundTimer;
    private long mTimerValue;
    private boolean mGuessMade = false;

    //activity result variable keys
    final static String RESULT_KEY_DISTANCE = "DISTANCE";

    //SaveInstanceState variable keys
    private final static String KEY_SAVED_STATE_TIMER_VALUE = "TIMER";
    private final static String KEY_SAVED_STATE_PASSED_LAT = "PASSED_LAT";
    private final static String KEY_SAVED_STATE_PASSED_LNG = "PASSED_LNG";

    private float mGuessOffset;
    private long mPassedTimeLeft;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        Intent intent = getIntent();
        double[] passedCoords = new double[2];
        if (savedInstanceState == null) {
            passedCoords = intent.getDoubleArrayExtra(StreetViewActivity.EXTRA_LOCATION_COORDINATES);
        } else {
            passedCoords[0] = savedInstanceState.getDouble(KEY_SAVED_STATE_PASSED_LAT);
            passedCoords[1] = savedInstanceState.getDouble(KEY_SAVED_STATE_PASSED_LNG);
        }
        mPassedLocationCoords = new LatLng(passedCoords[0], passedCoords[1]);

        mBtnSwitchToStreetView = (FloatingActionButton) findViewById(R.id.btn_switchToStreetView);
        mBtnSwitchToStreetView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mBtnConfirm = (Button) findViewById(R.id.btn_confirm);
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGuessMade = true;
                LatLng markerCoords = mGuessedLocationMarker.getPosition();

                Location markerLocation = new Location("");
                markerLocation.setLatitude(markerCoords.latitude);
                markerLocation.setLongitude(markerCoords.longitude);

                Location passedLocation = new Location("");
                passedLocation.setLatitude(mPassedLocationCoords.latitude);
                passedLocation.setLongitude(mPassedLocationCoords.longitude);

                mGuessOffset = markerLocation.distanceTo(passedLocation);

                PolylineOptions line = new PolylineOptions()
                        .add(mPassedLocationCoords, markerCoords)
                        .width(5)
                        .color(Color.BLUE);

                mMap.addPolyline(line);

                disableMapControls();
                showScore(mGuessOffset);

                if (mRoundTimer != null) {
                    mRoundTimer.cancel();
                }

                StreetViewActivity.cancelCountDownTimer();
            }
        });

        //SCORE FRAGMENT
        mLayoutScore = (LinearLayout) findViewById(R.id.layout_score);

        mTxtDistance = (TextView) findViewById(R.id.txt_Distance);
        mTxtPoints = (TextView) findViewById(R.id.txt_Points);
        mTxtTotalScore = (TextView) findViewById(R.id.txt_TotalScore);

        mBtnNextLocation = (FloatingActionButton) findViewById(R.id.btn_nextLocation);
        mBtnNextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle resultData = new Bundle();
                resultData.putFloat(RESULT_KEY_DISTANCE, mGuessOffset);
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
            mPassedTimeLeft = savedInstanceState.getLong(KEY_SAVED_STATE_TIMER_VALUE);
        }
        initTimer();

        mFragMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frag_map);
        mFragMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;

                if (mPassedTimeLeft == 0) {
                    disableMapControls();
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
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_SAVED_STATE_TIMER_VALUE, mTimerValue);

        outState.putDouble(KEY_SAVED_STATE_PASSED_LAT, mPassedLocationCoords.latitude);
        outState.putDouble(KEY_SAVED_STATE_PASSED_LNG, mPassedLocationCoords.longitude);
    }

    @Override
    public void onBackPressed() {
        if (mGuessMade) {
            mBtnNextLocation.callOnClick();
        } else {
            super.onBackPressed();
        }
    }

    private void disableMapControls() {  //TODO: add score
        mMap.addMarker(new MarkerOptions()
                .position(mPassedLocationCoords)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        mMap.setOnMapClickListener(null);
        mBtnConfirm.setEnabled(false);
    }

    private void showScore(float distance) {
        mLayoutScore.setVisibility(View.VISIBLE);
        String distanceLabel = getString(R.string.distance) + Float.toString(distance);
        String pointsLabel = getString(R.string.points); //TODO: add points
        String totalScoreLabel = getString(R.string.total_score); //TODO: add total score

        mTxtDistance.setText(distanceLabel);
        mTxtPoints.setText(pointsLabel);
        mTxtTotalScore.setText(totalScoreLabel);
    }

    private void initTimer() {
        if (mPassedTimeLeft != -1) {
            mTxtRoundTimer = (TextView) findViewById(R.id.txt_RoundTimer);
            mTxtRoundTimer.setText("Time left: " + mPassedTimeLeft);

            if (mPassedTimeLeft == 0) {
                showScore(0);
                mGuessMade = true;
            } else {
                mRoundTimer = new CountDownTimer(mPassedTimeLeft * 1000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        mTxtRoundTimer.setText("Time left: " + millisUntilFinished / 1000);
                        mTimerValue = millisUntilFinished / 1000;
                    }

                    @Override
                    public void onFinish() {
                        mGuessMade = true;
                        if (mMap != null) {
                            disableMapControls();
                        }
                        mTxtRoundTimer.setText("Time left: " + 0);
                        showScore(0);
                    }
                }.start();
            }
        }
    }
}
