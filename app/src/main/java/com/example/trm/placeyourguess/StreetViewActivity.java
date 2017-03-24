package com.example.trm.placeyourguess;

import android.content.Intent;
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

    private StreetViewPanorama mStreetViewPanorama;
    private LatLng mLocationCoords = new LatLng(0, 0);

    private LocationSelector mLocationSelector;

    private int mTotalScore = 0;
    private int mRoundNumber = 1;
    private String mCountryCode = "US";

    static final String EXTRA_LOCATION_COORDINATES = "EXTRA_LOCATION_COORDINATES";

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
                Intent intent = new Intent(StreetViewActivity.this, MapActivity.class);
                intent.putExtra(EXTRA_LOCATION_COORDINATES, new double[] {mLocationCoords.latitude, mLocationCoords.longitude});
                startActivityForResult(intent, REQ_MAP_ACTIVITY);
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

        mTxtRoundsLeft = (TextView) findViewById(R.id.txt_Roundsleft);
        updateRoundsLeftTextview();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_MAP_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    mRoundNumber++;
                    if (mRoundNumber <= Settings.getNumOfRounds()) {
                        Bundle resultData = data.getExtras();
                        float distance = resultData.getFloat(RESULT_KEY_DISTANCE); //TODO: calculate points based on that instead of adding it straight to score
                        mTotalScore += Math.round(distance);
                        updateScoreTextview();
                        mLocationSelector.switchPanorama(mCountryCode);
                        updateRoundsLeftTextview();
                    } else {
                        Bundle resultData = new Bundle();
                        resultData.putInt(RESULT_KEY_SCORE, mTotalScore);
                        Intent intent = new Intent();
                        intent.putExtras(resultData);
                        setResult(RESULT_OK, intent);

                        finish();
                    }
                }
                break;

            default:
                break;
        }
    }

    public void setLocationCoords(LatLng coords) {
        mLocationCoords = coords;
    }

    private void updateScoreTextview() {
        String label = getResources().getString(R.string.score) + " " + mTotalScore;
        mTxtTotalScore.setText(label);
    }

    private void updateRoundsLeftTextview() {
        String label = getResources().getString(R.string.round) + " " + mRoundNumber + "/" + Settings.getNumOfRounds();
        mTxtRoundsLeft.setText(label);
    }
}
