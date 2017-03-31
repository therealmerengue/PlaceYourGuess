package com.example.trm.placeyourguess;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.CompletionListener;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.ErrorInfo;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

import static io.ably.lib.types.ProtocolMessage.Action.presence;

public class MainActivity extends AppCompatActivity {

    private Button mBtnSingleplayer;
    private Button mBtnMultiplayer;
    private FloatingActionButton mBtnSettings;

    private Channel mChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnSingleplayer = (Button) findViewById(R.id.btn_singleplayer);
        mBtnSingleplayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnline()) {
                    Intent intent = new Intent(MainActivity.this, CountryListActivity.class);
                    startActivity(intent);
                } else {
                    showNoInternetAlertDialog("No internet connection", "Connect to the Internet to play the game.");
                }
            }
        });

        mBtnMultiplayer = (Button) findViewById(R.id.btn_multiplayer);
        mBtnMultiplayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOnline()) {
                    Intent intent = new Intent(MainActivity.this, MultiplayerActivity.class);
                    startActivity(intent);
                } else {
                    showNoInternetAlertDialog("No internet connection", "Connect to the Internet to play the game.");
                }
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
        BoundingBoxesHolder bbHolder = BoundingBoxesHolder.getInstance();
        bbHolder.loadBoxes(boxesStream);

        InputStream countriesStream = getResources().openRawResource(R.raw.codes);
        bbHolder.loadCountries(countriesStream);
    }

    private boolean isOnline() {
        Runtime runtime = Runtime.getRuntime();
        try {
            Process ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8");
            int exitValue = ipProcess.waitFor();
            return exitValue == 0;
        }
        catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showNoInternetAlertDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
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
}
