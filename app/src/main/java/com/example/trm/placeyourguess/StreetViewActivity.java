package com.example.trm.placeyourguess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
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

    private static StreetViewPanorama mStreetViewPanorama;

    private LocationSelector mLocationSelector;
    private static CountDownTimer mCountDownTimer;

    private int mTotalScore = 0;
    private int mRoundNumber = 1;
    private int mNumberOfRounds = 0;
    private long mTimerLeft = -1;
    private int mTimerLimit = -1;
    private String mCountryCode = "US";
    private boolean mPickRandomCountry = false;
    private boolean mSwitchToMapOnTimerEnd = true;

    //multiplayer variables
    private boolean mIsSingleplayer = true;
    private boolean mIsHost = true;

    //intent extras' tags
    static final String EXTRA_LOCATION_COORDINATES = "EXTRA_LOCATION_COORDINATES";
    static final String EXTRA_TIMER_LEFT = "EXTRA_TIMER_LEFT";

    //startActivity request codes
    static final int REQ_MAP_ACTIVITY = 100;

    //activity result variable keys
    static final String RESULT_KEY_SCORE = "SCORE";

    //SaveInstanceState variable keys
    private final static String KEY_SAVED_STATE_LOCATION_LAT = "LOCATION_LAT";
    private final static String KEY_SAVED_STATE_LOCATION_LNG = "LOCATION_LNG";
    private final static String KEY_SAVED_STATE_TIMER_VALUE = "TIMER_VALUE";
    private final static String KEY_SAVED_STATE_ROUND_NUMBER = "ROUND_NUMBER";
    private final static String KEY_SAVED_STATE_TOTAL_SCORE = "TOTAL_SCORE";
    private final static String KEY_SAVED_STATE_COUNTRY_CODE = "COUNTRY_CODE";
    private final static String KEY_SAVED_STATE_RANDOM_COUNTRY = "RANDOM_COUNTRY";
    //multiplayer only
    private final static String KEY_SAVED_STATE_IS_SINGLEPLAYER = "IS_SINGLEPLAYER";
    private final static String KEY_SAVED_STATE_IS_HOST = "KEY_IS_HOST";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            mIsSingleplayer = intent.getBooleanExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, true);
            if (!mIsSingleplayer) {
                mIsHost = intent.getBooleanExtra(MultiplayerActivity.EXTRA_IS_HOST, true);
            }

            mPickRandomCountry = intent.getBooleanExtra(CountryListActivity.EXTRA_RANDOM_COUNTRY, false);
            if (mPickRandomCountry) {
                mCountryCode = CountryListActivity.getRandomCode();
            } else {
                mCountryCode = intent.getStringExtra(CountryListActivity.EXTRA_SELECTED_COUNTRY_CODE);
            }
        } else {
            mIsSingleplayer = savedInstanceState.getBoolean(KEY_SAVED_STATE_IS_SINGLEPLAYER);
            if (!mIsSingleplayer) {
                mIsHost = savedInstanceState.getBoolean(KEY_SAVED_STATE_IS_HOST);
            }

            mPickRandomCountry = savedInstanceState.getBoolean(KEY_SAVED_STATE_RANDOM_COUNTRY);
            if (mPickRandomCountry) {
                mCountryCode = CountryListActivity.getRandomCode();
            } else {
                mCountryCode = savedInstanceState.getString(KEY_SAVED_STATE_COUNTRY_CODE);
            }
        }

        //btnSwitchToMap
        mBtnSwitchToMap = (FloatingActionButton) findViewById(R.id.btn_switchToMapView);
        mBtnSwitchToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapActivity();
            }
        });

        //totalScore
        if (savedInstanceState != null) {
            mTotalScore = savedInstanceState.getInt(KEY_SAVED_STATE_TOTAL_SCORE);
        }
        mTxtTotalScore = (TextView) findViewById(R.id.txt_Score);
        updateScoreTextview();

        //numberOfRounds
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String numberOfRoundsStr = preferences.getString(getString(R.string.settings_numOfRounds), "5");
        mNumberOfRounds = Integer.parseInt(numberOfRoundsStr);

        //roundNumber
        if (savedInstanceState != null) {
            mRoundNumber = savedInstanceState.getInt(KEY_SAVED_STATE_ROUND_NUMBER);
        }
        mTxtRoundsLeft = (TextView) findViewById(R.id.txt_Roundsleft);
        updateRoundsLeftTextview();

        //timer
        mTxtTimer = (TextView) findViewById(R.id.txt_Timer);
        String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
        mTimerLimit = Integer.parseInt(timerLimitStr);
        if (savedInstanceState != null) {
            mTimerLeft = savedInstanceState.getLong(KEY_SAVED_STATE_TIMER_VALUE);
            setupCountDownTimer(false);
        }

        //Street View panorama
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

                    if (savedInstanceState == null) {
                        mLocationSelector.switchPanorama(mCountryCode); //switches Street View panorama and starts count down timer
                    } else {
                        LatLng panoramaPosition = new LatLng(savedInstanceState.getDouble(KEY_SAVED_STATE_LOCATION_LAT),
                                        savedInstanceState.getDouble(KEY_SAVED_STATE_LOCATION_LNG));
                        mStreetViewPanorama.setPosition(panoramaPosition);
                    }
                }
            });

        //btnChangeLocation
        final Button btnChangeLocation = (Button) findViewById(R.id.btn_changeLocation);
        btnChangeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLocationSelector.switchPanorama(mCountryCode);
            }
        });
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
                        if (mLocationSelector == null) {
                            mLocationSelector = new LocationSelector(mStreetViewPanorama, this);
                        }
                        if (mPickRandomCountry) {
                            mCountryCode = CountryListActivity.getRandomCode();
                        }
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
        mSwitchToMapOnTimerEnd = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //current Street View location
        if (mStreetViewPanorama != null) {
            LatLng position = mStreetViewPanorama.getLocation().position;
            outState.putDouble(KEY_SAVED_STATE_LOCATION_LAT, position.latitude);
            outState.putDouble(KEY_SAVED_STATE_LOCATION_LNG, position.longitude);
        }

        //is singleplayer
        outState.putBoolean(KEY_SAVED_STATE_IS_SINGLEPLAYER, mIsSingleplayer);
        //if multiplayer - is host
        if (!mIsSingleplayer) {
            outState.putBoolean(KEY_SAVED_STATE_IS_HOST, mIsHost);
        }

        //current timer value
        outState.putLong(KEY_SAVED_STATE_TIMER_VALUE, mTimerLeft);

        //current round
        outState.putInt(KEY_SAVED_STATE_ROUND_NUMBER, mRoundNumber);

        //current score
        outState.putInt(KEY_SAVED_STATE_TOTAL_SCORE, mTotalScore);

        //choose random country?
        outState.putBoolean(KEY_SAVED_STATE_RANDOM_COUNTRY, mPickRandomCountry);

        //country code
        outState.putString(KEY_SAVED_STATE_COUNTRY_CODE, mCountryCode);
    }

    @Override
    public void onBackPressed() {
        QuitGameDialogFragment quitGameDialogFragment = new QuitGameDialogFragment();
        quitGameDialogFragment.show(getSupportFragmentManager(), "TAG_QUIT_FRAGMENT");
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

    public void setupCountDownTimer(boolean newTimer) {
        if (mTimerLimit != -1) {
            String label = "Time left: ";

            long startValue;
            if (newTimer) {
                startValue = mTimerLimit;
                label += mTimerLimit;
            } else {
                startValue = mTimerLeft;
                label += mTimerLeft;
            }

            mTxtTimer.setText(label);
            cancelCountDownTimer();
            mCountDownTimer = new CountDownTimer(startValue * 1000, 1000) {
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
        LatLng panoramaLocation = mStreetViewPanorama.getLocation().position;
        intent.putExtra(EXTRA_LOCATION_COORDINATES, new double[] {panoramaLocation.latitude, panoramaLocation.longitude});
        if (mTimerLeft != -1) {
            intent.putExtra(EXTRA_TIMER_LEFT, mTimerLeft);
        }
        startActivityForResult(intent, REQ_MAP_ACTIVITY);
    }
}
