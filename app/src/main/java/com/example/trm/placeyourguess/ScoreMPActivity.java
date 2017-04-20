package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ScoreMPActivity extends AppCompatActivity {

    private String mNickname;

    private Socket mSocket;

    private final String EVENT_SHOW_SCORES = "showScores";

    private Emitter.Listener onShowScoresListener = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            Log.e("EVENT", "showScores");

            JSONArray playerScores = (JSONArray) args[0];
            int numOfPlayers = playerScores.length();
            final Integer scores[] = new Integer[numOfPlayers];
            final String nicknames[] = new String[numOfPlayers];

            for (int i = 0; i < numOfPlayers; i++) {
                try {
                    JSONObject playerScore = playerScores.getJSONObject(i);
                    String nickname = playerScore.getString("nickname");
                    int score = playerScore.getInt("score");

                    nicknames[i] = nickname;
                    scores[i] = score;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ScoreListAdapter adapter = new ScoreListAdapter(ScoreMPActivity.this, mNickname, nicknames, scores);
                    ListView scoresList = (ListView) findViewById(R.id.lv_scoreList);
                    scoresList.setAdapter(adapter);
                    scoresList.setClickable(false);
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_mp);

        mSocket = SocketHolder.getInstance();

        Intent intent = getIntent();
        int score = intent.getIntExtra(MultiplayerActivity.EXTRA_FINAL_SCORE, 0);
        mNickname = intent.getStringExtra(MultiplayerActivity.EXTRA_NICKNAME);
        String roomName = intent.getStringExtra(MultiplayerActivity.EXTRA_ROOM_NAME);

        JSONObject scoreInfo = new JSONObject();
        try {
            scoreInfo.put("score", score);
            scoreInfo.put("nickname", mNickname);
            scoreInfo.put("roomName", roomName);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("sendScore", scoreInfo);

        mSocket.on(EVENT_SHOW_SCORES, onShowScoresListener);
        if (!mSocket.connected())
            mSocket.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.off(EVENT_SHOW_SCORES, onShowScoresListener);
    }
}
