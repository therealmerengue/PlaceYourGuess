package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketMultiplayerActivity extends AppCompatActivity {

    private Socket mSocket;

    private TextView mTxtChannelState;
    private EditText mEditChannel;
    private EditText mEditNickname;
    private Button mBtnJoin;
    private Button mBtnLeave;
    private Button mBtnStartGame;

    private String mJoinedChannelName;
    private String mNickname;
    private boolean mJoinedChannel = false;
    private boolean mIsHost = false;

    private Emitter.Listener playerCountChangeListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONArray playersList = (JSONArray) args[0];
            updateRoomList(playerListToString(playersList));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_multiplayer);

        mSocket = SocketHolder.getInstance();

        mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e("JOINED", "Joined server");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(SocketMultiplayerActivity.this, "Connected to server.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mJoinedChannel) {
                            emitLeaveRoom();
                        }
                        Toast.makeText(SocketMultiplayerActivity.this, "Disconnected from server.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).on("nominateHost", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                mIsHost = true;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mBtnStartGame.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).on("joinedRoom", playerCountChangeListener).on("playerLeft", playerCountChangeListener);
        mSocket.connect();

        mTxtChannelState = (TextView) findViewById(R.id.txt_channelState);
        mEditChannel = (EditText) findViewById(R.id.edit_channel);
        mEditNickname = (EditText) findViewById(R.id.edit_nickname);

        mBtnJoin = (Button) findViewById(R.id.btn_join);
        mBtnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String channelName = mEditChannel.getText().toString().trim();
                String nickname = mEditNickname.getText().toString().trim();
                //TODO: check for invalid channel names
                if (channelName.length() == 0) {
                    Toast.makeText(SocketMultiplayerActivity.this, "Enter channel name.", Toast.LENGTH_LONG).show();
                    return;
                }
                if (nickname.length() == 0) {
                    Toast.makeText(SocketMultiplayerActivity.this, "Enter nickname.", Toast.LENGTH_LONG).show();
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
                        mBtnStartGame.setVisibility(View.GONE);
                    }
                });
            }
        });

        mBtnStartGame = (Button) findViewById(R.id.btn_startGame);
        mBtnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SocketMultiplayerActivity.this, CountryListActivity.class);
                intent.putExtra(MainActivity.EXTRA_IS_SINGLEPLAYER, false);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mJoinedChannel) {
            mBtnLeave.callOnClick();
        }
        mSocket.disconnect();
        super.onBackPressed();
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
