package com.example.trm.placeyourguess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;

import static com.example.trm.placeyourguess.MapActivity.RESULT_KEY_DISTANCE;

public class StreetViewActivity extends AppCompatActivity {

    private FloatingActionButton mBtnSwitchToMap;
    private TextView mTxtTotalScore;
    private TextView mTxtRoundsLeft;
    private TextView mTxtTimer;

    private StreetViewPanorama mStreetViewPanorama;
    private LatLng mLocationCoords = new LatLng(0, 0);

    private LocationSelector mLocationSelector;
    private static CountDownTimer mCountDownTimer;

    private int mTotalScore = 0;
    private int mRoundNumber = 1;
    private int mNumberOfRounds = 0;
    private int mTimerLimit = 0;
    private long mTimerLeft = -1;
    private String mCountryCode = "US";

    private boolean mSwitchToMapOnTimerEnd = true;

    static final String EXTRA_LOCATION_COORDINATES = "EXTRA_LOCATION_COORDINATES";
    static final String EXTRA_TIMER_LEFT = "EXTRA_TIMER_LEFT";

    static final int REQ_MAP_ACTIVITY = 100;

    static final String RESULT_KEY_SCORE = "SCORE";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);

        mBtnSwitchToMap = (FloatingActionButton) findViewById(R.id.btn_switchToMapView);
        mBtnSwitchToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapActivity();
            }
        });

        final Button btnChangeLocation = (Button) findViewById(R.id.btn_changeLocation);
        btnChangeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLocationSelector.switchPanorama("US");
            }
        });

        SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
                (SupportStreetViewPanoramaFragment) getSupportFragmentManager().findFragmentById(R.id.frag_streetview);

        streetViewPanoramaFragment.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
                @Override
                public void onStreetViewPanoramaReady(final StreetViewPanorama panorama) {
                    mStreetViewPanorama = panorama;

                    mStreetViewPanorama.setStreetNamesEnabled(Settings.isStreetNamesEnabled());
                    mStreetViewPanorama.setUserNavigationEnabled(true);
                    mStreetViewPanorama.setZoomGesturesEnabled(true);
                    mStreetViewPanorama.setPanningGesturesEnabled(true);

                    mLocationSelector = new LocationSelector(mStreetViewPanorama, StreetViewActivity.this);
                    mLocationSelector.switchPanorama("US");
                }
            });

        mTxtTotalScore = (TextView) findViewById(R.id.txt_Score);
        updateScoreTextview();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String numberOfRoundsStr = preferences.getString(getString(R.string.settings_numOfRounds), "5");
        mNumberOfRounds = Integer.parseInt(numberOfRoundsStr);

        mTxtRoundsLeft = (TextView) findViewById(R.id.txt_Roundsleft);
        updateRoundsLeftTextview();

        String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
        mTimerLimit = Integer.parseInt(timerLimitStr);

        mTxtTimer = (TextView) findViewById(R.id.txt_Timer);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_MAP_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    mRoundNumber++;
                    Bundle mapActivityResult = data.getExtras();
                    float distance = mapActivityResult.getFloat(RESULT_KEY_DISTANCE); //TODO: calculate points based on that instead of adding it straight to score
                    mTotalScore += Math.round(distance);

                    if (mRoundNumber <= mNumberOfRounds) {
                        updateScoreTextview();
                        mLocationSelector.switchPanorama(mCountryCode);
                        updateRoundsLeftTextview();
                    } else {
                        Bundle streetViewActivityResult = new Bundle();
                        streetViewActivityResult.putInt(RESULT_KEY_SCORE, mTotalScore);
                        Intent intent = new Intent();
                        intent.putExtras(streetViewActivityResult);
                        setResult(RESULT_OK, intent);

                        finish();
                    }
                }
                break;

            default:
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSwitchToMapOnTimerEnd = true; //to prevent having 2 MapActivities started
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSwitchToMapOnTimerEnd = false; //to prevent having 2 MapActivities started
    }

    public void setLocationCoords(LatLng coords) {
        mLocationCoords = coords;
    }

    private void updateScoreTextview() {
        String label = getResources().getString(R.string.score) + " " + mTotalScore;
        mTxtTotalScore.setText(label);
    }

    private void updateRoundsLeftTextview() {
        String label = getResources().getString(R.string.round) + " " + mRoundNumber + "/" + mNumberOfRounds;
        mTxtRoundsLeft.setText(label);
    }

    public static void cancelCountDownTimer() {
        if (mCountDownTimer != null)
            mCountDownTimer.cancel();
    }

    public void setupCountDownTimer() {
        if (mTimerLimit != -1) {
            String label = "Time left: " + mTimerLimit;
            mTxtTimer.setText(label);

            mCountDownTimer = new CountDownTimer(mTimerLimit * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    long timeLeft = millisUntilFinished / 1000;
                    mTxtTimer.setText("Time left:" + timeLeft);
                    mTimerLeft = timeLeft;
                }

                @Override
                public void onFinish() {
                    mTimerLeft = 0;
                    if (mSwitchToMapOnTimerEnd)
                        startMapActivity();
                }
            }.start();
        }
    }

    private void startMapActivity() {
        Intent intent = new Intent(StreetViewActivity.this, MapActivity.class);
        intent.putExtra(EXTRA_LOCATION_COORDINATES, new double[] {mLocationCoords.latitude, mLocationCoords.longitude});
        if (mTimerLeft != -1) {
            intent.putExtra(EXTRA_TIMER_LEFT, mTimerLeft);
        }
        startActivityForResult(intent, REQ_MAP_ACTIVITY);
    }
}
