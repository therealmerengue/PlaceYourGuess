package com.example.trm.placeyourguess;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import holders.LocationInfoHolder;

public class MainActivity extends AppCompatActivity {

    private Button mBtnSingleplayer;
    private Button mBtnMultiplayer;
    private Button mBtnTutorial;
    private FloatingActionButton mBtnSettings;

    //intent extras' tags
    static final String EXTRA_IS_SINGLEPLAYER = "IS_SINGLEPLAYER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnSingleplayer = (Button) findViewById(R.id.btn_singleplayer);
        mBtnSingleplayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new IsOnlineTask().execute(0);
            }
        });

        mBtnMultiplayer = (Button) findViewById(R.id.btn_multiplayer);
        mBtnMultiplayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new IsOnlineTask().execute(1);
            }
        });

        mBtnTutorial = (Button) findViewById(R.id.btn_tutorial);
        mBtnTutorial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        mBtnSettings = (FloatingActionButton) findViewById(R.id.btn_settings);
        mBtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        InputStream boxesStream = getResources().openRawResource(R.raw.boxes);
        LocationInfoHolder holder = LocationInfoHolder.getInstance();
        holder.loadBoxes(boxesStream);

        InputStream countriesStream = getResources().openRawResource(R.raw.codes);
        holder.loadCountries(countriesStream);

        InputStream citiesStream = getResources().openRawResource(R.raw.cities);
        holder.loadCities(citiesStream);

        InputStream famousStream = getResources().openRawResource(R.raw.famous);
        holder.loadFamousPlaces(famousStream);
    }

    class IsOnlineTask extends AsyncTask<Integer, Void, Boolean> {
        private int gameMode; //0 - SINGLE, 1 - MULTI

        private void showNoInternetAlertDialog(String title, String message) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle(title);
            alertDialog.setMessage(message);
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            alertDialog.show();
        }

        private boolean checkConnection() {
            try {
                int timeoutMs = 1500;

                Socket sock = new Socket();
                SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

                sock.connect(sockaddr, timeoutMs);
                sock.close();

                return true;
            } catch (IOException e) { return false; }
        }

        @Override
        protected Boolean doInBackground(Integer... params) {
            gameMode = params[0];
            return checkConnection();
        }

        @Override
        protected void onPostExecute(Boolean isOnline) {
            super.onPostExecute(isOnline);

            if (isOnline) {
                switch (gameMode) {
                    case 0:
                        Intent singleIntent = new Intent(MainActivity.this, LocationListActivity.class);
                        singleIntent.putExtra(EXTRA_IS_SINGLEPLAYER, true);
                        startActivity(singleIntent);
                        break;
                    case 1:
                        Intent multiIntent = new Intent(MainActivity.this, MultiplayerRoomsActivity.class);
                        startActivity(multiIntent);
                        break;
                    default:
                        break;
                }
            } else {
                showNoInternetAlertDialog("No internet connection", "Connect to the Internet to play the game.");
            }
        }
    }
}
