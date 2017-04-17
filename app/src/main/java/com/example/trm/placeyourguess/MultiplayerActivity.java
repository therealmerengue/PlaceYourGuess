package com.example.trm.placeyourguess;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.example.trm.placeyourguess.CountryListActivity.EXTRA_LATITUDES;
import static com.example.trm.placeyourguess.CountryListActivity.EXTRA_LONGITUDES;

public class MultiplayerActivity extends AppCompatActivity {

    private Socket mSocket;
    private final String EVENT_NOMINATE_HOST = "nominateHost";
    private final String EVENT_START_MULTIPLAYER_GAME = "startMultiplayerGame";
    private final String EVENT_JOINED_ROOM = "joinedRoom";
    private final String EVENT_PLAYER_LEFT = "playerLeft";
    private final String EVENT_PLAYER_JOINED = "playerJoined";
    private Emitter.Listener onConnectListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MultiplayerActivity.this, "Connected to server.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private Emitter.Listener onConnectErrorListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MultiplayerActivity.this, "Unable to connect to server.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private Emitter.Listener onDisconnectListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mJoinedChannel) {
                        emitLeaveRoom();
                    }
                    Toast.makeText(MultiplayerActivity.this, "Disconnected from server.", Toast.LENGTH_SHORT).show();
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
    private Emitter.Listener onStartMultiplayerGameListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject settings = (JSONObject) args[0];
            JSONArray locations = null;
            int timerLimit = -1;
            try {
                locations = settings.getJSONArray("locations");
                timerLimit = settings.getInt("timerLimit");
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

            //TODO: this used to work with old StreetViewActivity, now probably doesn't work
            Intent intent = new Intent(MultiplayerActivity.this, StreetViewActivity.class);
            intent.putExtra(EXTRA_TIMER_LIMIT, timerLimit);
            intent.putExtra(EXTRA_IS_HOST, mIsHost);
            intent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, false);
            intent.putExtra(EXTRA_LATITUDES, latitudes);
            intent.putExtra(EXTRA_LONGITUDES, longitudes);

            startActivity(intent);
        }
    };
    private Emitter.Listener playerCountChangeListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONArray playersList = (JSONArray) args[0];
            updateRoomList(playerListToString(playersList));
            mPlayersInRoom = playersList.length();
        }
    };

    private TextView mTxtChannelState;
    private EditText mEditChannel;
    private EditText mEditNickname;
    private Button mBtnJoin;
    private Button mBtnLeave;
    private Button mBtnStartGame;
    private Button mBtnSettings;
    private LinearLayout mLayoutHostControls;

    private String mJoinedChannelName;
    private String mNickname;
    private boolean mJoinedChannel = false;
    private boolean mIsHost = false;
    private int mPlayersInRoom = 1;

    //intent extras' tags
    static final String EXTRA_IS_HOST = "IS_HOST";
    static final String EXTRA_TIMER_LIMIT = "EXTRA_TIMER_LIMIT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        mSocket = SocketHolder.getInstance();

        mSocket.on(Socket.EVENT_CONNECT, onConnectListener)
                .on(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener)
                .on(Socket.EVENT_DISCONNECT, onDisconnectListener)
                .on(EVENT_NOMINATE_HOST, onNominateHostListener)
                .on(EVENT_START_MULTIPLAYER_GAME, onStartMultiplayerGameListener)
                .on(EVENT_JOINED_ROOM, playerCountChangeListener)
                .on(EVENT_PLAYER_LEFT, playerCountChangeListener)
                .on(EVENT_PLAYER_JOINED, playerCountChangeListener);
        if (!mSocket.connected())
            mSocket.connect();

        mTxtChannelState = (TextView) findViewById(R.id.txt_channelState);
        mEditChannel = (EditText) findViewById(R.id.edit_channel);
        mEditNickname = (EditText) findViewById(R.id.edit_nickname);
        mLayoutHostControls = (LinearLayout) findViewById(R.id.layout_hostControls);

        mBtnJoin = (Button) findViewById(R.id.btn_join);
        mBtnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channelName = mEditChannel.getText().toString().trim();
                String nickname = mEditNickname.getText().toString().trim();
                //TODO: check for invalid channel names
                if (channelName.length() == 0) {
                    Toast.makeText(MultiplayerActivity.this, "Enter channel name.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (nickname.length() == 0) {
                    Toast.makeText(MultiplayerActivity.this, "Enter nickname.", Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject joinInfo = new JSONObject();
                try {
                    joinInfo.put("room", channelName);
                    joinInfo.put("nickname", nickname);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mJoinedChannelName = channelName;
                mNickname = nickname;
                mJoinedChannel = true;
                mSocket.emit("joinRoom", joinInfo);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnJoin.setEnabled(false);
                        mBtnLeave.setEnabled(true);
                        mEditNickname.setEnabled(false);
                        mEditChannel.setEnabled(false);
                    }
                });
            }
        });

        mBtnLeave = (Button) findViewById(R.id.btn_leave);
        mBtnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                emitLeaveRoom();

                mJoinedChannel = false;
                mIsHost = false; //won't hurt even if player wasn't host to begin with

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnJoin.setEnabled(true);
                        mBtnLeave.setEnabled(false);
                        mTxtChannelState.setText(getString(R.string.not_joined));
                        mEditNickname.getText().clear();
                        mEditChannel.getText().clear();
                        mEditChannel.setEnabled(true);
                        mEditNickname.setEnabled(true);
                        mLayoutHostControls.setVisibility(View.GONE);
                    }
                });
            }
        });

        mBtnStartGame = (Button) findViewById(R.id.btn_startGame);
        mBtnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPlayersInRoom > 1) {
                    Intent intent = new Intent(MultiplayerActivity.this, CountryListActivity.class);
                    intent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, false);
                    intent.putExtra(EXTRA_IS_HOST, mIsHost);
                    startActivity(intent);
                } else {
                    Toast.makeText(MultiplayerActivity.this, "Minimum 2 players required to start the game.", Toast.LENGTH_LONG).show();
                }
            }
        });

        mBtnSettings = (Button) findViewById(R.id.btn_switchToSettings);
        mBtnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MultiplayerActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mJoinedChannel) {
            mBtnLeave.callOnClick();
        }

        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off(Socket.EVENT_CONNECT, onConnectListener)
                .off(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener)
                .off(Socket.EVENT_DISCONNECT, onDisconnectListener)
                .off(EVENT_NOMINATE_HOST, onNominateHostListener)
                .off(EVENT_START_MULTIPLAYER_GAME, onStartMultiplayerGameListener)
                .off(EVENT_JOINED_ROOM, playerCountChangeListener)
                .off(EVENT_PLAYER_LEFT, playerCountChangeListener)
                .off(EVENT_PLAYER_JOINED, playerCountChangeListener);
    }

    private void emitLeaveRoom() {
        JSONObject leaveInfo = new JSONObject();
        try {
            leaveInfo.put("room", mJoinedChannelName);
            leaveInfo.put("nickname", mNickname);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("leaveRoom", leaveInfo);
    }

    private String playerListToString(JSONArray playersList) {
        String listStr = mNickname + ", you've joined room: " + mJoinedChannelName + ". Players: \n";
        for (int i = 0; i < playersList.length(); i++) {
            try {
                listStr += playersList.getString(i) + "\n";
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return listStr;
    }

    private void updateRoomList(final String channelStateStr) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTxtChannelState.setText(channelStateStr);
            }
        });
    }
}
