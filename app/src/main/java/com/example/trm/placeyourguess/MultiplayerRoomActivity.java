package com.example.trm.placeyourguess;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import adapters.PlayerListAdapter;
import holders.SocketHolder;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MultiplayerRoomActivity extends AppCompatActivity {

    private TextView mTxtRoomName;
    private Button mBtnLeaveRoom;
    private Button mBtnGameSettings;
    private Button mBtnStartGame;
    private ListView mLvPlayers;
    private LinearLayout mLayoutHostControls;

    private PlayerListAdapter mPlayerListAdapter;
    private static String mPlayerName;
    private static String mRoomName;
    private boolean mIsHost;

    static final String EXTRA_IS_HOST = "EXTRA_IS_HOST";
    static final String EXTRA_HINTS_ENABLED = "EXTRA_HINTS_ENABLED";
    static final String EXTRA_TIMER_LIMIT = "EXTRA_TIMER_LIMIT";
    static final String EXTRA_FINAL_SCORE = "EXTRA_FINAL_SCORE";
    static final String EXTRA_ROOM_NAME = "EXTRA_ROOM_NAME";
    static final String EXTRA_NICKNAME = "EXTRA_NICKNAME";

    static final int REQ_STREET_VIEW_ACTIVITY = 101;

    private static Socket mSocket;

    private final String EVENT_JOIN_EXISTING_ROOM = "joinExistingRoom";
    private final String EVENT_PLAYER_JOINED = "playerJoined";
    private final String EVENT_REQUEST_PLAYER_LIST = "requestPlayerList";
    private final static String EVENT_LEAVE_ROOM = "leaveRoom";
    private final String EVENT_PLAYER_LEFT = "playerLeft";
    private final String EVENT_NOMINATE_HOST = "nominateHost";
    private final String EVENT_ROOM_NOT_EXISTS = "roomNotExists";
    private final String EVENT_START_MULTIPLAYER_GAME = "startMultiplayerGame";
    private final String EVENT_NICKNAME_ALREADY_TAKEN = "nicknameAlreadyTaken";

    private Emitter.Listener onReloadListListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONArray players = (JSONArray) args[0];
            int numberOfPlayers = players.length();
            String[] playersArray = new String[numberOfPlayers];

            for (int i = 0; i < numberOfPlayers; i++) {
                try {
                    playersArray[i] = players.getString(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            mPlayerListAdapter = new PlayerListAdapter(MultiplayerRoomActivity.this, mPlayerName, playersArray);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLvPlayers.setAdapter(mPlayerListAdapter);
                }
            });
        }
    };

    private Emitter.Listener onNominateHostListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            mIsHost = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLayoutHostControls.setVisibility(View.VISIBLE);
                }
            });
        }
    };

    private Emitter.Listener onRoomNotExistsListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MultiplayerRoomActivity.this, "Room does not exist anymore - reating a new one with the same name.", Toast.LENGTH_LONG).show();
                    mLayoutHostControls.setVisibility(View.VISIBLE);
                }
            });

            //create a new room with the same name as the previous one
            JSONObject newRoomInfo = new JSONObject();
            try {
                newRoomInfo.put("roomName", mRoomName);
                newRoomInfo.put("hostName", mPlayerName);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mSocket.emit(MultiplayerNewRoomActivity.EVENT_CREATE_ROOM, newRoomInfo);
            mSocket.emit(EVENT_REQUEST_PLAYER_LIST, mRoomName);

            mIsHost = true;
        }
    };

    private Emitter.Listener onStartMultiplayerGame = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject settings = (JSONObject) args[0];
            JSONArray locations = null;
            int timerLimit = -1;
            boolean hintsEnabled = false;
            try {
                locations = settings.getJSONArray("locations");
                timerLimit = settings.getInt("timerLimit");
                hintsEnabled = settings.getBoolean("hintsEnabled");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            int numOfRounds = locations.length();
            double[] latitudes = new double[numOfRounds];
            double[] longitudes = new double[numOfRounds];

            for (int i = 0; i < numOfRounds; i++) {
                try {
                    JSONObject location = locations.getJSONObject(i);
                    latitudes[i] = location.getDouble("lat");
                    longitudes[i] = location.getDouble("lng");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            Log.e("startMultiplayerGame", "Starting multiplayer game");

            Intent intent = new Intent(MultiplayerRoomActivity.this, StreetViewActivity.class);
            intent.putExtra(EXTRA_TIMER_LIMIT, timerLimit);
            intent.putExtra(EXTRA_HINTS_ENABLED, hintsEnabled);
            intent.putExtra(EXTRA_IS_HOST, mIsHost);
            intent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, false);
            intent.putExtra(LocationListActivity.EXTRA_LATITUDES, latitudes);
            intent.putExtra(LocationListActivity.EXTRA_LONGITUDES, longitudes);

            startActivityForResult(intent, REQ_STREET_VIEW_ACTIVITY);
        }
    };

    private Emitter.Listener onNicknameAlreadyTakenListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MultiplayerRoomActivity.this, "Nickname already taken by another player in that room.", Toast.LENGTH_LONG).show();
                }
            });

            stopService(new Intent(MultiplayerRoomActivity.this, OnClearFromRecentService.class));
            MultiplayerRoomActivity.this.finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_room);

        startService(new Intent(this, OnClearFromRecentService.class));

        mSocket = SocketHolder.getInstance();
        mSocket.on(EVENT_PLAYER_JOINED, onReloadListListener)
            .on(EVENT_PLAYER_LEFT, onReloadListListener)
            .on(EVENT_NOMINATE_HOST, onNominateHostListener)
            .on(EVENT_ROOM_NOT_EXISTS, onRoomNotExistsListener)
            .on(EVENT_START_MULTIPLAYER_GAME, onStartMultiplayerGame)
            .on(EVENT_NICKNAME_ALREADY_TAKEN, onNicknameAlreadyTakenListener);

        Intent intent = getIntent();
        mRoomName = intent.getStringExtra(MultiplayerNewRoomActivity.EXTRA_ROOM_NAME);
        mPlayerName = intent.getStringExtra(MultiplayerNewRoomActivity.EXTRA_PLAYER_NAME);
        mIsHost = intent.getBooleanExtra(MultiplayerNewRoomActivity.EXTRA_IS_HOST, false);

        if (!mIsHost) {
            JSONObject joinRoomInfo = new JSONObject();
            try {
                joinRoomInfo.put("roomName", mRoomName);
                joinRoomInfo.put("playerName", mPlayerName);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mSocket.emit(EVENT_JOIN_EXISTING_ROOM, joinRoomInfo);
        } else {
            mSocket.emit(EVENT_REQUEST_PLAYER_LIST, mRoomName);
        }

        mTxtRoomName = (TextView) findViewById(R.id.txt_joinedRoomName);
        mTxtRoomName.setText("Room: " + mRoomName);

        mLvPlayers = (ListView) findViewById(R.id.lv_playersInRoom);
        mLayoutHostControls = (LinearLayout) findViewById(R.id.layout_roomHostControls);
        if (mIsHost) {
            mLayoutHostControls.setVisibility(View.VISIBLE);
        }

        mBtnStartGame = (Button) findViewById(R.id.btn_startMultiplayerGame);
        mBtnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLvPlayers.getAdapter().getCount() > 1) {
                    Intent intent = new Intent(MultiplayerRoomActivity.this, LocationListActivity.class);
                    intent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, false);
                    intent.putExtra(EXTRA_IS_HOST, mIsHost);
                    startActivity(intent);
                } else {
                    Toast.makeText(MultiplayerRoomActivity.this, "Minimum 2 players required to start the game.", Toast.LENGTH_LONG).show();
                }
            }
        });

        mBtnGameSettings = (Button) findViewById(R.id.btn_changeSettings);
        mBtnGameSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MultiplayerRoomActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        mBtnLeaveRoom = (Button) findViewById(R.id.btn_leaveRoom);
        mBtnLeaveRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emitLeaveRoom();
                setResult(RESULT_CANCELED);
                stopService(new Intent(MultiplayerRoomActivity.this, OnClearFromRecentService.class));
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_STREET_VIEW_ACTIVITY:
                if (data != null) {
                    Bundle resultData = data.getExtras();
                    int score = resultData.getInt(StreetViewActivity.RESULT_KEY_SCORE);

                    Toast.makeText(this, Integer.toString(score), Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(this, ScoreMPActivity.class);
                    intent.putExtra(EXTRA_FINAL_SCORE, score);
                    intent.putExtra(EXTRA_ROOM_NAME, mRoomName);
                    intent.putExtra(EXTRA_NICKNAME, mPlayerName);
                    startActivity(intent);
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.off(EVENT_PLAYER_JOINED, onReloadListListener)
            .off(EVENT_PLAYER_LEFT, onReloadListListener)
            .off(EVENT_NOMINATE_HOST, onNominateHostListener)
            .off(EVENT_ROOM_NOT_EXISTS, onRoomNotExistsListener)
            .off(EVENT_START_MULTIPLAYER_GAME, onStartMultiplayerGame)
            .off(EVENT_NICKNAME_ALREADY_TAKEN, onNicknameAlreadyTakenListener);
    }

    @Override
    public void onBackPressed() {
        emitLeaveRoom();
        stopService(new Intent(MultiplayerRoomActivity.this, OnClearFromRecentService.class));
        super.onBackPressed();
    }

    private static void emitLeaveRoom() {
        JSONObject leaveInfo = new JSONObject();
        try {
            leaveInfo.put("room", mRoomName);
            leaveInfo.put("nickname", mPlayerName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit(EVENT_LEAVE_ROOM, leaveInfo);
    }

    public static class OnClearFromRecentService extends Service {

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            Log.d("ClearFromRecentService", "Service Started");
            return START_NOT_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            Log.d("ClearFromRecentService", "Service Destroyed");
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            Log.e("ClearFromRecentService", "END");

            if (!mSocket.connected())
                mSocket.connect();
            emitLeaveRoom();

            mSocket.disconnect();
            stopSelf();
        }
    }
}
