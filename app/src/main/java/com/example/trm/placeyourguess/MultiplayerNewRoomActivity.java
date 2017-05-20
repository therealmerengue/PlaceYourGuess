package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import holders.SocketHolder;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MultiplayerNewRoomActivity extends AppCompatActivity {

    private EditText mEditRoomName;
    private Button mBtnCreateNewRoom;

    private String mNewRoomName;
    private String mPlayerName;

    private static Socket mSocket;

    static final String EVENT_CREATE_ROOM = "createRoom";
    private final String EVENT_ROOM_ALREADY_EXISTS = "roomAlreadyExists";
    private final String EVENT_ROOM_CREATED = "roomCreated";

    static final String EXTRA_ROOM_NAME = "EXTRA_ROOM_NAME";
    static final String EXTRA_PLAYER_NAME = "EXTRA_PLAYER_NAME";
    static final String EXTRA_IS_HOST = "EXTRA_IS_HOST";

    private final int REQ_MULTIPLAYER_ROOM_ACTIVITY = 101;

    private Emitter.Listener onRoomAlreadyExistsListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JoinExistingRoomDialogFragment fragment = JoinExistingRoomDialogFragment.newInstance(mNewRoomName, mPlayerName);
                    fragment.show(getSupportFragmentManager(), "TAG_JOIN_EXISTING_ROOM");
                }
            });
        }
    };
    private Emitter.Listener onRoomCreatedListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Intent intent = new Intent(MultiplayerNewRoomActivity.this, MultiplayerRoomActivity.class);

            intent.putExtra(EXTRA_ROOM_NAME, mNewRoomName);
            intent.putExtra(EXTRA_PLAYER_NAME, mPlayerName);
            intent.putExtra(EXTRA_IS_HOST, true);
            startActivityForResult(intent, REQ_MULTIPLAYER_ROOM_ACTIVITY);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer_new_room);

        Intent intent = getIntent();
        mPlayerName = intent.getStringExtra(MultiplayerRoomsActivity.EXTRA_NICKNAME);

        mEditRoomName = (EditText) findViewById(R.id.edit_roomName);

        mBtnCreateNewRoom = (Button) findViewById(R.id.btn_createNewRoom);
        mBtnCreateNewRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = mEditRoomName.getText().toString().trim();

                if (roomName.length() == 0) {
                    Toast.makeText(MultiplayerNewRoomActivity.this, "Enter room name.", Toast.LENGTH_SHORT).show();
                    return;
                }

                mNewRoomName = roomName;

                JSONObject newRoomInfo = new JSONObject();
                try {
                    newRoomInfo.put("roomName", mNewRoomName);
                    newRoomInfo.put("hostName", mPlayerName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                mSocket.emit(EVENT_CREATE_ROOM, newRoomInfo);
            }
        });

        mSocket = SocketHolder.getInstance();
        mSocket.on(EVENT_ROOM_ALREADY_EXISTS, onRoomAlreadyExistsListener)
            .on(EVENT_ROOM_CREATED, onRoomCreatedListener);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_MULTIPLAYER_ROOM_ACTIVITY:
                finish();
                break;
            default: break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mSocket.off(EVENT_ROOM_ALREADY_EXISTS, onRoomAlreadyExistsListener)
            .off(EVENT_ROOM_CREATED, onRoomCreatedListener);
    }
}
