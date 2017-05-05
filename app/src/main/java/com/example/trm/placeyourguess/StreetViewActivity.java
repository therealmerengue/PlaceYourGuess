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
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

public class StreetViewActivity extends AppCompatActivity {

    private FloatingActionButton mBtnSwitchToMap;
    private FloatingActionButton mBtnHintStreetNames;
    private TextView mTxtTotalScore;
    private TextView mTxtRoundsLeft;
    private TextView mTxtTimer;
    private Button mBtnBackToStart;

    private StreetViewPanorama mStreetViewPanorama;

    private static CountDownTimer mCountDownTimer;

    private int mTotalScore = 0;
    private int mRoundNumber = 1;
    private long mTimerLeft = -1;
    private int mTimerLimit = -1;
    private boolean mHintsEnabled = false;
    private boolean mSwitchToMapOnTimerEnd = true;
    private int mNumberOfRounds;
    private double[] mLatitudes;
    private double[] mLongitudes;

    //multiplayer variables
    private boolean mIsSingleplayer = true;
    private boolean mIsHost = true;

    //intent extras' tags
    static final String EXTRA_LOCATION_COORDINATES = "EXTRA_LOCATION_COORDINATES";
    static final String EXTRA_TIMER_LEFT = "EXTRA_TIMER_LEFT";
    static final String EXTRA_HINTS_ENABLED = "EXTRA_HINTS_ENABLED";

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
    private final static String KEY_SAVED_STATE_LATITUDES = "LATITUDES";
    private final static String KEY_SAVED_STATE_LONGITUDES = "LONGITUDES";
    //multiplayer only
    private final static String KEY_SAVED_STATE_IS_SINGLEPLAYER = "IS_SINGLEPLAYER";
    private final static String KEY_SAVED_STATE_IS_HOST = "KEY_IS_HOST";
    private final static String KEY_SAVED_STATE_TIMER_LIMIT = "KEY_TIMER_LIMIT";
    private final static String KEY_SAVED_STATE_HINTS_ENABLED = "KEY_HINTS_ENABLED";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);

        mTxtTimer = (TextView) findViewById(R.id.txt_Timer);
        mTxtTotalScore = (TextView) findViewById(R.id.txt_Score);
        mTxtRoundsLeft = (TextView) findViewById(R.id.txt_Roundsleft);

        //btnSwitchToMap
        mBtnSwitchToMap = (FloatingActionButton) findViewById(R.id.btn_switchToMapView);
        mBtnSwitchToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startMapActivity();
            }
        });

        mBtnBackToStart = (Button) findViewById(R.id.btn_resetLocation);
        mBtnBackToStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mStreetViewPanorama != null) {
                    LatLng defaultPosition = new LatLng(mLatitudes[mRoundNumber - 1], mLongitudes[mRoundNumber - 1]);
                    mStreetViewPanorama.setPosition(defaultPosition);
                }
            }
        });

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            mIsSingleplayer = intent.getBooleanExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, true);
            if (!mIsSingleplayer) {
                mIsHost = intent.getBooleanExtra(MultiplayerRoomActivity.EXTRA_IS_HOST, true);

                mTimerLimit = intent.getIntExtra(MultiplayerRoomActivity.EXTRA_TIMER_LIMIT, -1);

                mHintsEnabled = intent.getBooleanExtra(MultiplayerRoomActivity.EXTRA_HINTS_ENABLED, false);
            } else {
                String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                mTimerLimit = Integer.parseInt(timerLimitStr);

                mHintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);
            }

            mLatitudes = intent.getDoubleArrayExtra(LocationListActivity.EXTRA_LATITUDES);
            mLongitudes = intent.getDoubleArrayExtra(LocationListActivity.EXTRA_LONGITUDES);

            setupCountDownTimer(true);
        } else {
            mIsSingleplayer = savedInstanceState.getBoolean(KEY_SAVED_STATE_IS_SINGLEPLAYER);
            if (!mIsSingleplayer) {
                mIsHost = savedInstanceState.getBoolean(KEY_SAVED_STATE_IS_HOST);

                mTimerLimit = savedInstanceState.getInt(KEY_SAVED_STATE_TIMER_LIMIT);

                mHintsEnabled = savedInstanceState.getBoolean(KEY_SAVED_STATE_HINTS_ENABLED);
            } else {
                String timerLimitStr = preferences.getString(getString(R.string.settings_timerLimit), "-1");
                mTimerLimit = Integer.parseInt(timerLimitStr);

                mHintsEnabled = preferences.getBoolean(getString(R.string.settings_hintsEnabled), false);
            }

            mLatitudes = savedInstanceState.getDoubleArray(KEY_SAVED_STATE_LATITUDES);
            mLongitudes = savedInstanceState.getDoubleArray(KEY_SAVED_STATE_LONGITUDES);

            mRoundNumber = savedInstanceState.getInt(KEY_SAVED_STATE_ROUND_NUMBER);
            mTimerLeft = savedInstanceState.getLong(KEY_SAVED_STATE_TIMER_VALUE);

            mTotalScore = savedInstanceState.getInt(KEY_SAVED_STATE_TOTAL_SCORE);

            setupCountDownTimer(false);
        }

        setupHintsButton();

        updateScoreTextview();

        //numberOfRounds
        mNumberOfRounds = mLatitudes.length;
        updateRoundsLeftTextview();

        //Street View panorama
        SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
                (SupportStreetViewPanoramaFragment) getSupportFragmentManager().findFragmentById(R.id.frag_streetview);
        streetViewPanoramaFragment.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
                @Override
                public void onStreetViewPanoramaReady(final StreetViewPanorama panorama) {
                    mStreetViewPanorama = panorama;

                    mStreetViewPanorama.setStreetNamesEnabled(false);
                    mStreetViewPanorama.setUserNavigationEnabled(true);
                    mStreetViewPanorama.setZoomGesturesEnabled(true);
                    mStreetViewPanorama.setPanningGesturesEnabled(true);

                    LatLng panoramaPosition = new LatLng(mLatitudes[mRoundNumber - 1], mLongitudes[mRoundNumber - 1]);
                    mStreetViewPanorama.setPosition(panoramaPosition);
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
                    int score = mapActivityResult.getInt(MapActivity.RESULT_KEY_SCORE);
                    mTotalScore += score;

                    if (mRoundNumber <= mNumberOfRounds) {
                        updateScoreTextview();
                        LatLng panoramaPosition = new LatLng(mLatitudes[mRoundNumber - 1], mLongitudes[mRoundNumber - 1]);
                        mStreetViewPanorama.setPosition(panoramaPosition);
                        updateRoundsLeftTextview();
                        setupCountDownTimer(true);
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
            StreetViewPanoramaLocation location = mStreetViewPanorama.getLocation();
            if (location != null) {
                LatLng position = mStreetViewPanorama.getLocation().position;
                outState.putDouble(KEY_SAVED_STATE_LOCATION_LAT, position.latitude);
                outState.putDouble(KEY_SAVED_STATE_LOCATION_LNG, position.longitude);
            }
        }

        //is singleplayer
        outState.putBoolean(KEY_SAVED_STATE_IS_SINGLEPLAYER, mIsSingleplayer);
        //if multiplayer
        if (!mIsSingleplayer) {
            outState.putBoolean(KEY_SAVED_STATE_IS_HOST, mIsHost);
            outState.putInt(KEY_SAVED_STATE_TIMER_LIMIT, mTimerLimit);
            outState.putBoolean(KEY_SAVED_STATE_HINTS_ENABLED, mHintsEnabled);
        }

        //current timer value
        outState.putLong(KEY_SAVED_STATE_TIMER_VALUE, mTimerLeft);

        //current round
        outState.putInt(KEY_SAVED_STATE_ROUND_NUMBER, mRoundNumber);

        //current score
        outState.putInt(KEY_SAVED_STATE_TOTAL_SCORE, mTotalScore);

        //location latlngs
        outState.putDoubleArray(KEY_SAVED_STATE_LATITUDES, mLatitudes);
        outState.putDoubleArray(KEY_SAVED_STATE_LONGITUDES, mLongitudes);
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
                    mTxtTimer.setText(Long.toString(timeLeft));
                    mTimerLeft = timeLeft;
                }

                @Override
                public void onFinish() {
                    mTimerLeft = 0;
                    mTxtTimer.setText("0");
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

        if (!mIsSingleplayer) {
            intent.putExtra(EXTRA_HINTS_ENABLED, mHintsEnabled);
        }

        startActivityForResult(intent, REQ_MAP_ACTIVITY);
    }

    private void setupHintsButton() {
        if (mHintsEnabled) {
            mBtnHintStreetNames = (FloatingActionButton) findViewById(R.id.btn_hintStreetNames);
            mBtnHintStreetNames.setVisibility(View.VISIBLE);
            mBtnHintStreetNames.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mStreetViewPanorama != null) {
                        mStreetViewPanorama.setStreetNamesEnabled(!mStreetViewPanorama.isStreetNamesEnabled());
                    }
                }
            });
        }
    }
}
