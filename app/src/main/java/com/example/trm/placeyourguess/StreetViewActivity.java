package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback;
import com.google.android.gms.maps.StreetViewPanorama;
import com.google.android.gms.maps.SupportStreetViewPanoramaFragment;
import com.google.android.gms.maps.model.LatLng;

import static com.example.trm.placeyourguess.MapActivity.RESULT_KEY_DISTANCE;

public class StreetViewActivity extends AppCompatActivity {

    private FloatingActionButton mBtnSwitchToMap;

    private StreetViewPanorama mStreetViewPanorama;
    private LatLng mLocationCoords = new LatLng(37.765927, -122.449972);

    static final String TAG_LOCATION_COORDINATES = "TAG_LOCATION_COORDINATES";

    static final int REQ_MAP_ACTIVITY = 100;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_street_view);

        mBtnSwitchToMap = (FloatingActionButton) findViewById(R.id.btn_switchToMapView);
        mBtnSwitchToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StreetViewActivity.this, MapActivity.class);
                intent.putExtra(TAG_LOCATION_COORDINATES, new double[] {mLocationCoords.latitude, mLocationCoords.longitude});
                startActivityForResult(intent, REQ_MAP_ACTIVITY);
            }
        });

        SupportStreetViewPanoramaFragment streetViewPanoramaFragment =
                (SupportStreetViewPanoramaFragment) getSupportFragmentManager().findFragmentById(R.id.frag_streetview);

        streetViewPanoramaFragment.getStreetViewPanoramaAsync(new OnStreetViewPanoramaReadyCallback() {
                @Override
                public void onStreetViewPanoramaReady(StreetViewPanorama panorama) {
                    mStreetViewPanorama = panorama;
                    mStreetViewPanorama.setStreetNamesEnabled(Settings.isStreetNamesEnabled());
                    mStreetViewPanorama.setUserNavigationEnabled(true);
                    mStreetViewPanorama.setZoomGesturesEnabled(true);
                    mStreetViewPanorama.setPanningGesturesEnabled(true);

                    mStreetViewPanorama.setPosition(mLocationCoords);
                }
            });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_MAP_ACTIVITY:
                if (resultCode == RESULT_OK) {
                    Bundle res = data.getExtras();
                    float distance = res.getFloat(RESULT_KEY_DISTANCE); //TODO: calculate points based on that

                    mStreetViewPanorama.setPosition(new LatLng(40.758896, -73.985130));
                }
                break;
        }
    }

}
