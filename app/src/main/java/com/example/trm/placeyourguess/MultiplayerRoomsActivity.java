package com.example.trm.placeyourguess;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static com.example.trm.placeyourguess.MultiplayerNewRoomActivity.EXTRA_ROOM_NAME;
import static com.example.trm.placeyourguess.MultiplayerNewRoomActivity.EXTRA_PLAYER_NAME;
import static com.example.trm.placeyourguess.MultiplayerNewRoomActivity.EXTRA_IS_HOST;

public class MultiplayerRoomsActivity extends AppCompatActivity {

    private TextView mTxtConnectionStatus;
    private TextView mTxtRooms;
    private FloatingActionButton mBtnRefresh;
    private EditText mEditNickname;
    private Button mBtnNewRoom;
    private Button mBtnReloadRoomList;
    private ListView mLvRooms;
    private RoomListAdapter mRoomListAdapter;

    static final String EXTRA_NICKNAME = "EXTRA_NICKNAME";

    private static Socket mSocket;

    private final String EVENT_RECEIVE_ROOM_LIST = "roomList";
    private final String EVENT_RELOAD_ROOM_LIST = "reloadRoomList";

    private Emitter.Listener onConnectListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTxtConnectionStatus.setText("Connected to server.");
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
                    mTxtConnectionStatus.setText("Connection error.");
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
                    mTxtConnectionStatus.setText("Disconnected from server.");
                    if (mTxtRooms.getVisibility() == View.VISIBLE)
                        mTxtRooms.setVisibility(View.GONE);
                    mLvRooms.setAdapter(null);
                }
            });
        }
    };
    private Emitter.Listener onRoomListReceivedListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTxtRooms.setVisibility(View.VISIBLE);
                }
            });

            JSONArray roomsInfo = (JSONArray) args[0];
            int numberOfRooms = roomsInfo.length();

            if (numberOfRooms != 0) {
                String[] roomNames = new String[numberOfRooms];
                String[] roomPlayerCounts = new String[numberOfRooms];

                for (int i = 0; i < numberOfRooms; i++) {
                    try {
                        JSONObject roomInfo = roomsInfo.getJSONObject(i);
                        roomNames[i] = roomInfo.getString("name");
                        roomPlayerCounts[i] = Integer.toString(roomInfo.getInt("numberOfPlayers"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                mRoomListAdapter = new RoomListAdapter(MultiplayerRoomsActivity.this, roomNames, roomPlayerCounts);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLvRooms.setAdapter(mRoomListAdapter);
                    }
                });
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLvRooms.setAdapter(null);
                        mTxtRooms.setText("No rooms - create new one.");
                    }
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_rooms);

        mTxtConnectionStatus = (TextView) findViewById(R.id.txt_connectionStatus);
        mTxtRooms = (TextView) findViewById(R.id.txt_rooms);

        mEditNickname = (EditText) findViewById(R.id.edit_playerName);

        mBtnRefresh = (FloatingActionButton) findViewById(R.id.btn_refresh);
        mBtnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTxtConnectionStatus.setText("Reconnecting");
                mSocket.connect();
            }
        });

        mBtnNewRoom = (Button) findViewById(R.id.btn_newRoom);
        mBtnNewRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nickname = mEditNickname.getText().toString().trim();
                if (!validateNickname(nickname))
                    return;

                Intent intent = new Intent(MultiplayerRoomsActivity.this, MultiplayerNewRoomActivity.class);
                intent.putExtra(EXTRA_NICKNAME, nickname);
                startActivity(intent);
            }
        });

        mBtnReloadRoomList = (Button) findViewById(R.id.btn_reloadList);
        mBtnReloadRoomList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocket.emit(EVENT_RELOAD_ROOM_LIST);
            }
        });

        mLvRooms = (ListView) findViewById(R.id.lv_roomsList);
        mLvRooms.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedRoomName = ((TextView) view.findViewById(R.id.txt_roomName)).getText().toString();
                String nickname = mEditNickname.getText().toString().trim();
                if (!validateNickname(nickname))
                    return;

                Intent intent = new Intent(MultiplayerRoomsActivity.this, MultiplayerRoomActivity.class);
                intent.putExtra(EXTRA_ROOM_NAME, selectedRoomName);
                intent.putExtra(EXTRA_PLAYER_NAME, nickname);
                intent.putExtra(EXTRA_IS_HOST, false);
                startActivity(intent);
            }
        });

        mSocket = SocketHolder.getInstance();

        mSocket.on(Socket.EVENT_CONNECT, onConnectListener)
            .on(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener)
            .on(Socket.EVENT_DISCONNECT, onDisconnectListener)
            .on(EVENT_RECEIVE_ROOM_LIST, onRoomListReceivedListener);
        if (!mSocket.connected())
            mSocket.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSocket.emit(EVENT_RELOAD_ROOM_LIST);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.off(Socket.EVENT_CONNECT, onConnectListener)
            .off(Socket.EVENT_CONNECT_ERROR, onConnectErrorListener)
            .off(Socket.EVENT_DISCONNECT, onDisconnectListener)
            .off(EVENT_RECEIVE_ROOM_LIST, onRoomListReceivedListener);
    }

    private boolean validateNickname(String nickname) {
        if (nickname.length() == 0) {
            Toast.makeText(MultiplayerRoomsActivity.this, "Enter your nickname", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
