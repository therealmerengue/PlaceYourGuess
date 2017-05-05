package com.example.trm.placeyourguess;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

import logic.Calculator;

public class CustomLocationActivity extends AppCompatActivity {

    private SupportMapFragment mFragMap;
    private GoogleMap mMap;
    private Marker marker1, marker2;
    private LatLng defaultBound1 = new LatLng(0, 0);
    private LatLng defaultBound2 = new LatLng(20, 20);
    private Polygon mPolygon;

    final static String RESULT_KEY_LATITUDE_BOUNDS = "RESULT_KEY_LATITUDE_BOUNDS";
    final static String RESULT_KEY_LONGITUDE_BOUNDS = "RESULT_KEY_LONGITUDE_BOUNDS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_location);

        mFragMap = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frag_customLocationMap);
        mFragMap.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                UiSettings settings = mMap.getUiSettings();
                settings.setMapToolbarEnabled(false);
                settings.setCompassEnabled(false);
                settings.setRotateGesturesEnabled(false);
                settings.setIndoorLevelPickerEnabled(false);

                marker1 = mMap.addMarker(new MarkerOptions()
                        .position(defaultBound1)
                        .draggable(true)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                marker2 = mMap.addMarker(new MarkerOptions()
                        .draggable(true)
                        .position(defaultBound2)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

                mPolygon = mMap.addPolygon(new PolygonOptions()
                        .add(defaultBound1,
                            new LatLng(defaultBound1.latitude, defaultBound2.longitude),
                            defaultBound2,
                            new LatLng(defaultBound2.latitude, defaultBound1.longitude))
                        .strokeColor(Color.BLUE));

                mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                    @Override
                    public void onMarkerDragStart(Marker marker) {}

                    @Override
                    public void onMarkerDrag(Marker marker) {
                        ArrayList<LatLng> bounds = new ArrayList<>(4);
                        LatLng marker1Position = marker1.getPosition();
                        LatLng marker2Position = marker2.getPosition();

                        bounds.add(marker1Position);
                        bounds.add(new LatLng(marker1Position.latitude, marker2Position.longitude));
                        bounds.add(marker2Position);
                        bounds.add(new LatLng(marker2Position.latitude, marker1Position.longitude));

                        mPolygon.setPoints(bounds);
                    }

                    @Override
                    public void onMarkerDragEnd(Marker marker) {}
                });
            }
        });

        Button btnConfirmChoice = (Button) findViewById(R.id.btn_confirmLocationChoice);
        btnConfirmChoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //determine and send back bounds
                double[] latitudeBounds = new double[2];
                double[] longitudeBounds = new double[2];

                LatLng marker1Position = marker1.getPosition();
                LatLng marker2Position = marker2.getPosition();

                float distanceAcrossBounds = Calculator.measureDistance(marker1Position, marker2Position);
                if (distanceAcrossBounds < 5000) {
                    DecimalFormat df = new DecimalFormat("###.##");
                    String distanceStr = df.format(distanceAcrossBounds / 1000) + " km";
                    String label = "Selected area is too small. It's only " + distanceStr + " across. Choose a larger area (at least 10 km across).";
                    Toast.makeText(CustomLocationActivity.this, label, Toast.LENGTH_LONG).show();
                    return;
                }

                latitudeBounds[0] = marker1Position.latitude;
                latitudeBounds[1] = marker2Position.latitude;
                longitudeBounds[0] = marker1Position.longitude;
                longitudeBounds[1] = marker2Position.longitude;

                Arrays.sort(latitudeBounds); //0: min, 1: max
                Arrays.sort(longitudeBounds);

                Bundle resultData = new Bundle();
                resultData.putDoubleArray(RESULT_KEY_LATITUDE_BOUNDS, latitudeBounds);
                resultData.putDoubleArray(RESULT_KEY_LONGITUDE_BOUNDS, longitudeBounds);

                Intent resultIntent = new Intent();
                resultIntent.putExtras(resultData);

                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
}
