package com.example.trm.placeyourguess;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ScoreSPActivity extends AppCompatActivity {

    static String RESULT_KEY_PLAY_AGAIN = "PLAY_AGAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score_sp);

        Intent intent = getIntent();
        int score = intent.getIntExtra(LocationListActivity.EXTRA_SCORE, 0);

        TextView mTxtScore = (TextView) findViewById(R.id.txt_finalScore);
        String scoreLabel = getString(R.string.final_score) + " " + Integer.toString(score);
        mTxtScore.setText(scoreLabel);

        Button mBtnPlayAgain = (Button) findViewById(R.id.btn_playAgain);
        mBtnPlayAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivityWithResult(true);
            }
        });

        Button mBtnBack = (Button) findViewById(R.id.btn_backToMenu);
        mBtnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivityWithResult(false);
            }
        });
    }

    private void finishActivityWithResult(boolean playAgain) {
        Bundle resultData = new Bundle();
        resultData.putBoolean(RESULT_KEY_PLAY_AGAIN, playAgain);

        Intent resultIntent = new Intent();
        resultIntent.putExtras(resultData);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}
