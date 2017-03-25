package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private Button btnSingleplayer;
    private FloatingActionButton btnSettings;

    private static final int REQ_STREET_ACTIVITY = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSingleplayer = (Button) findViewById(R.id.btn_singleplayer);
        btnSingleplayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, StreetViewActivity.class);
                startActivityForResult(intent, REQ_STREET_ACTIVITY);
            }
        });

        btnSettings = (FloatingActionButton) findViewById(R.id.btn_settings);
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        InputStream boxesStream = getResources().openRawResource(R.raw.boxes);
        BoundingBoxesHolder bbHolder = BoundingBoxesHolder.getInstance();
        bbHolder.loadBoxes(boxesStream);

        InputStream countriesStream = getResources().openRawResource(R.raw.codes);
        bbHolder.loadCountries(countriesStream);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_STREET_ACTIVITY:
                Bundle resultData = data.getExtras();
                int score = resultData.getInt(StreetViewActivity.RESULT_KEY_SCORE);
                Toast.makeText(this, Integer.toString(score), Toast.LENGTH_LONG).show(); //TODO: replace with screen presenting score
                break;
        }
    }
}
