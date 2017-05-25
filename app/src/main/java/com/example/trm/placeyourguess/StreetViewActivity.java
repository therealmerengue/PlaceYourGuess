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
import android.widget.LinearLayout;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.StreetViewPanoramaLocation;

public class StreetViewActivity extends AppCompatActivity {

    private FloatingActionButton mBtnSwitchToMap;
    private FloatingActionButton mBtnHintStreetNames;
    private StrokedTextView mTxtTotalScore;
    private StrokedTextView mTxtRoundsLeft;
    private StrokedTextView mTxtTimer;
    private Button mBtnBackToStart;
    private LinearLayout mLayoutHints;

    private StreetViewPanorama mStreetViewPanorama;

    private static CountDownTimer mCountDownTimer;

    private int mTotalScore = 0;
    private int mRoundNumber = 1;
    private long mTimerLeft = -1;
    private int mTimerLimit = -1;
    private boolean mHintsEnabled = false;
    private boolean mSwitchToMapOnTimerEnd = true;
    private boolean mMapActivityStarted = false;
    private int mNumberOfRounds;
    private double[] mLatitudes;
    private double[] mLongitudes;

    //for centering map and placing marker if user comes back from MapActivity without making a guess
    private double[] mPreviouslyPlacedMarkerPosition;
    private double[] mPreviousMapCenter;
    private float mPreviousMapZoom;

    //for coming back to the same location on StreetView if user comes back from MapActivity without making a guess
    private double[] mPreviousPanoramaPosition;
    private boolean mMapActivityResultCancelled = false;

    //multiplayer variables
    private boolean mIsSingleplayer = true;
    private boolean mIsHost = true;

    //intent extras' tags
    static final String EXTRA_LOCATION_COORDINATES = "EXTRA_LOCATION_COORDINATES";
    static final String EXTRA_TIMER_LEFT = "EXTRA_TIMER_LEFT";
    static final String EXTRA_HINTS_ENABLED = "EXTRA_HINTS_ENABLED";
    static final String EXTRA_PREVIOUSLY_PLACED_MARKER = "EXTRA_PREVIOUSLY_PLACED_MARKER";
    static final String EXTRA_PREVIOUS_MAP_CENTER = "EXTRA_PREVIOUS_MAP_CENTER";
    static final String EXTRA_PREVIOUS_MAP_ZOOM = "EXTRA_PREVIOUS_MAP_ZOOM";

    //startActivity request codes
    static final int REQ_MAP_ACTIVITY = 100;

    //activity result variable keys
    static final String RESULT_KEY_SCORE = "SCORE";

    //SaveInstanceState variable keys
    private final static String KEY_SAVED_STATE_MAP_ACTIVITY_STARTED = "KEY_SAVED_STATE_MAP_ACTIVITY_STARTED";
    private final static String KEY_SAVED_STATE_PANORAMA_LAT_LNG = "PANORAMA_LAT_LNG";
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

        mTxtTimer = (StrokedTextView) findViewById(R.id.txt_Timer);
        mTxtTotalScore = (StrokedTextView) findViewById(R.id.txt_Score);
        mTxtRoundsLeft = (StrokedTextView) findViewById(R.id.txt_Roundsleft);

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
            mTimerLeft = mTimerLimit;

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

            mMapActivityStarted = savedInstanceState.getBoolean(KEY_SAVED_STATE_MAP_ACTIVITY_STARTED);

            if (savedInstanceState.containsKey(KEY_SAVED_STATE_PANORAMA_LAT_LNG)) {
                mPreviousPanoramaPosition = savedInstanceState.getDoubleArray(KEY_SAVED_STATE_PANORAMA_LAT_LNG);
            }

            setupCountDownTimer(false);
        }

        setupHints();

        updateScoreTextview();

        //numberOfRounds
        mNumberOfRounds = mLatitudes.length;
        updateRoundsLeftTextview();

        //Street View panorama
        if (!mMapActivityStarted) {
            if (savedInstanceState == null)
                initPanoramaFragment(new LatLng(mLatitudes[mRoundNumber - 1], mLongitudes[mRoundNumber - 1]));
            else if (savedInstanceState.containsKey(KEY_SAVED_STATE_PANORAMA_LAT_LNG))
                initPanoramaFragment(new LatLng(mPreviousPanoramaPosition[0], mPreviousPanoramaPosition[1]));
        }
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

                    mPreviouslyPlacedMarkerPosition = null;
                    mPreviousMapCenter = null;
                    mPreviousMapZoom = 1f;

                    if (mRoundNumber <= mNumberOfRounds) {
                        updateScoreTextview();
                        initPanoramaFragment(new LatLng(mLatitudes[mRoundNumber - 1], mLongitudes[mRoundNumber - 1]));
                        mMapActivityStarted = false;
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
                } else if (resultCode == RESULT_CANCELED) {
                    mMapActivityResultCancelled = true;
                    mMapActivityStarted = false;
                    if (data != null) {
                        Bundle result = data.getExtras();
                        mPreviousMapZoom = result.getFloat(MapActivity.RESULT_CURRENT_ZOOM);
                        if (result.containsKey(MapActivity.RESULT_GUESSED_MARKER_LOCATION)) {
                            mPreviouslyPlacedMarkerPosition = result.getDoubleArray(MapActivity.RESULT_GUESSED_MARKER_LOCATION);
                        } else if (result.containsKey(MapActivity.RESULT_CURRENT_CENTER)) {
                            mPreviousMapCenter = result.getDoubleArray(MapActivity.RESULT_CURRENT_CENTER);
                        }
                    }
                    setupCountDownTimer(false);
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

        if (mMapActivityResultCancelled && mPreviousPanoramaPosition != null) {
            initPanoramaFragment(new LatLng(mPreviousPanoramaPosition[0], mPreviousPanoramaPosition[1]));
            mMapActivityResultCancelled = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSwitchToMapOnTimerEnd = false;

        if (mStreetViewPanorama != null && mStreetViewPanorama.getLocation() != null) {
            LatLng position = mStreetViewPanorama.getLocation().position;
            mPreviousPanoramaPosition = new double[] {position.latitude, position.longitude};
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //current Street View location
        if (mStreetViewPanorama != null) {
            StreetViewPanoramaLocation location = mStreetViewPanorama.getLocation();
            if (location != null) {
                LatLng position = mStreetViewPanorama.getLocation().position;
                outState.putDoubleArray(KEY_SAVED_STATE_PANORAMA_LAT_LNG, new double[] {position.latitude, position.longitude});
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

        outState.putBoolean(KEY_SAVED_STATE_MAP_ACTIVITY_STARTED, mMapActivityStarted);
    }

    @Override
    public void onBackPressed() {
        QuitGameDialogFragment quitGameDialogFragment = new QuitGameDialogFragment();
        quitGameDialogFragment.show(getSupportFragmentManager(), "TAG_QUIT_FRAGMENT");
    }

    private void initPanoramaFragment(final LatLng panoramaPosition) {
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

                mStreetViewPanorama.setPosition(panoramaPosition);
            }
        });
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

            long startValue = mTimerLimit;
            if (!newTimer) {
                startValue = mTimerLeft;
            }

            mTxtTimer.setText(Long.toString(startValue));
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

        if (mStreetViewPanorama == null || mStreetViewPanorama.getLocation() == null) return;

        LatLng panoramaLocation = mStreetViewPanorama.getLocation().position;
        intent.putExtra(EXTRA_LOCATION_COORDINATES, new double[] {panoramaLocation.latitude, panoramaLocation.longitude});

        if (mTimerLeft != -1) {
            intent.putExtra(EXTRA_TIMER_LEFT, mTimerLeft);
        }

        if (!mIsSingleplayer) {
            intent.putExtra(EXTRA_HINTS_ENABLED, mHintsEnabled);
        }

        if (mPreviouslyPlacedMarkerPosition != null) {
            intent.putExtra(EXTRA_PREVIOUSLY_PLACED_MARKER, mPreviouslyPlacedMarkerPosition);
            intent.putExtra(EXTRA_PREVIOUS_MAP_ZOOM, mPreviousMapZoom);
        }

        if (mPreviousMapCenter != null) {
            intent.putExtra(EXTRA_PREVIOUS_MAP_CENTER, mPreviousMapCenter);
            intent.putExtra(EXTRA_PREVIOUS_MAP_ZOOM, mPreviousMapZoom);
        }

        mMapActivityStarted = true;

        startActivityForResult(intent, REQ_MAP_ACTIVITY);
    }

    private void setupHints() {
        if (mHintsEnabled) {
            mLayoutHints = (LinearLayout) findViewById(R.id.layout_hints_sv);
            mLayoutHints.setVisibility(View.VISIBLE);

            mBtnHintStreetNames = (FloatingActionButton) findViewById(R.id.btn_hintStreetNames);
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
