package com.example.trm.placeyourguess;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import io.ably.lib.realtime.AblyRealtime;
import io.ably.lib.realtime.Channel;
import io.ably.lib.realtime.CompletionListener;
import io.ably.lib.types.AblyException;
import io.ably.lib.types.ClientOptions;
import io.ably.lib.types.ErrorInfo;
import io.ably.lib.types.Message;
import io.ably.lib.types.PresenceMessage;

public class MultiplayerActivity extends AppCompatActivity {

    private TextView mTxtChannelState;
    private EditText mEditChannel;
    private Button mBtnJoin;
    private Button mBtnLeave;

    private Channel mChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multiplayer);

        mTxtChannelState = (TextView) findViewById(R.id.txt_numOfPlayers);

        mEditChannel = (EditText) findViewById(R.id.edit_channelName);

        mBtnJoin = (Button) findViewById(R.id.btn_joinChannel);
        mBtnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ClientOptions clientOptions = new ClientOptions(getString(R.string.ably_key));
                    clientOptions.clientId = Long.toString(System.currentTimeMillis());
                    AblyRealtime realtime = new AblyRealtime(clientOptions);

                    final String channelName = mEditChannel.getText().toString();
                    //TODO: check for invalid channel names
                    if (channelName.length() == 0) {
                        Toast.makeText(MultiplayerActivity.this, "Enter channel name.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    mChannel = realtime.channels.get(channelName);
                    mChannel.presence.enter(null, new CompletionListener() {
                        @Override
                        public void onSuccess() {
                            try {
                                final PresenceMessage[] members = mChannel.presence.get();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mTxtChannelState.setText("Joined channel: " + channelName + ". "
                                                + getString(R.string.num_of_players_ready) + " " + Integer.toString(members.length));
                                        mBtnJoin.setEnabled(false);
                                        mBtnLeave.setEnabled(true);
                                    }
                                });
                                //TODO: check if player is first in the channel (first on members list) - if so he's host - what if host leaves?
                            } catch (AblyException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(ErrorInfo reason) {
                            Toast.makeText(MultiplayerActivity.this, "Failed to join channel.", Toast.LENGTH_LONG).show();
                        }
                    });

                    mChannel.subscribe(new Channel.MessageListener() {
                        @Override
                        public void onMessage(Message messages) {

                        }
                    });
                } catch (AblyException e) {
                    e.printStackTrace();
                }
            }
        });

        mBtnLeave = (Button) findViewById(R.id.btn_leaveChannel);
        mBtnLeave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    mChannel.presence.leave(new CompletionListener() {
                        @Override
                        public void onSuccess() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mTxtChannelState.setText(getString(R.string.not_joined));
                                    mBtnJoin.setEnabled(true);
                                    mBtnLeave.setEnabled(false);
                                }
                            });
                        }

                        @Override
                        public void onError(ErrorInfo reason) {
                            Toast.makeText(MultiplayerActivity.this, "Failed to leave channel.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (AblyException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
