package com.example.trm.placeyourguess;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Collections;

public class ScoreListAdapter extends ArrayAdapter<String> {

    private String boldName;
    private String[] names;
    private Integer[] scores;
    private Activity context;

    public ScoreListAdapter(Activity context, String boldName, String[] names, Integer[] scores) {
        super(context, R.layout.score_list_item, names);

        this.context = context;
        this.boldName = boldName;
        this.names = names;
        this.scores = scores;
        Arrays.sort(scores, Collections.reverseOrder());
    }

    @NonNull
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (view == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            view = inflater.inflate(R.layout.score_list_item, null, true);
            holder = new ViewHolder();
            holder.mTxtPlayerName = (TextView) view.findViewById(R.id.txt_playerName);
            holder.mTxtPlayerScore = (TextView) view.findViewById(R.id.txt_playerScore);

            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        holder.mTxtPlayerName.setText(names[position]);
        if (names[position].equals(boldName)) {
            holder.mTxtPlayerName.setTypeface(holder.mTxtPlayerName.getTypeface(), Typeface.BOLD);
            holder.mTxtPlayerScore.setTypeface(holder.mTxtPlayerScore.getTypeface(), Typeface.BOLD);
        }
        holder.mTxtPlayerScore.setText(Integer.toString(scores[position]));

        return view;
    }

    static class ViewHolder {
        private TextView mTxtPlayerName;
        private TextView mTxtPlayerScore;
    }
}
