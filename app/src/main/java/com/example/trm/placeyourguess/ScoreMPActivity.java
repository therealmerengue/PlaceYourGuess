package com.example.trm.placeyourguess;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import adapters.ScoreListAdapter;
import holders.SocketHolder;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ScoreMPActivity extends AppCompatActivity {

    private String mNickname;
    private int[] mScores;
    private String[] mNicknames;
    private int mScore;
    private String mRoomName;

    private Socket mSocket;

    private final String EVENT_SHOW_SCORES = "showScores";

    private final static String KEY_SAVED_STATE_NICKNAMES = "NICKNAMES";
    private final static String KEY_SAVED_STATE_SCORES = "SCORES";
    private final static String KEY_SAVED_STATE_NICKNAME = "NICKNAME";
    private final static String KEY_SAVED_STATE_ROOMNAME = "ROOMNAME";
    private final static String KEY_SAVED_STATE_SCORE = "SCORE";

    private Emitter.Listener onShowScoresListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("EVENT", "showScores");

            JSONArray playerScores = (JSONArray) args[0];
            int numOfPlayers = playerScores.length();
            mScores = new int[numOfPlayers];
            mNicknames = new String[numOfPlayers];

            for (int i = 0; i < numOfPlayers; i++) {
                try {
                    JSONObject playerScore = playerScores.getJSONObject(i);
                    String nickname = playerScore.getString("nickname");
                    int score = playerScore.getInt("score");

                    mNicknames[i] = nickname;
                    mScores[i] = score;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fillScoreList();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_mp);

        mSocket = SocketHolder.getInstance();

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            mScore = intent.getIntExtra(MultiplayerRoomActivity.EXTRA_FINAL_SCORE, 0);
            mNickname = intent.getStringExtra(MultiplayerRoomActivity.EXTRA_NICKNAME);
            mRoomName = intent.getStringExtra(MultiplayerRoomActivity.EXTRA_ROOM_NAME);

            JSONObject scoreInfo = new JSONObject();
            try {
                scoreInfo.put("score", mScore);
                scoreInfo.put("nickname", mNickname);
                scoreInfo.put("roomName", mRoomName);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mSocket.emit("sendScore", scoreInfo);
        } else {
            mScore = savedInstanceState.getInt(KEY_SAVED_STATE_SCORE);
            mNickname = savedInstanceState.getString(KEY_SAVED_STATE_NICKNAME);
            mRoomName = savedInstanceState.getString(KEY_SAVED_STATE_ROOMNAME);

            if (savedInstanceState.containsKey(KEY_SAVED_STATE_SCORES)) {
                mScores = savedInstanceState.getIntArray(KEY_SAVED_STATE_SCORES);
                mNicknames = savedInstanceState.getStringArray(KEY_SAVED_STATE_NICKNAMES);
                fillScoreList();
            }
        }

        mSocket.on(EVENT_SHOW_SCORES, onShowScoresListener);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(KEY_SAVED_STATE_SCORE, mScore);
        outState.putString(KEY_SAVED_STATE_ROOMNAME, mRoomName);
        outState.putString(KEY_SAVED_STATE_NICKNAME, mNickname);

        if (mNicknames != null)
            outState.putStringArray(KEY_SAVED_STATE_NICKNAMES, mNicknames);

        if (mScores != null)
            outState.putIntArray(KEY_SAVED_STATE_SCORES, mScores);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off(EVENT_SHOW_SCORES, onShowScoresListener);
    }

    private void fillScoreList() {
        ScoreListAdapter adapter = new ScoreListAdapter(ScoreMPActivity.this, mNickname, mNicknames, mScores);
        ListView scoresList = (ListView) findViewById(R.id.lv_scoreList);
        scoresList.setAdapter(adapter);
        scoresList.setClickable(false);
    }
}
