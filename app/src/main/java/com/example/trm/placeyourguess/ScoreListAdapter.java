package com.example.trm.placeyourguess;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ScoreListAdapter extends ArrayAdapter<String> {

    private String[] names;
    private Integer[] scores;
    private Activity context;

    public ScoreListAdapter(Activity context, String[] names, Integer[] scores) {
        super(context, R.layout.score_list_item, names);

        this.context = context;
        this.names = names;
        this.scores = scores;
    }

    @NonNull
    public View getView(int position, View view, ViewGroup parent) {
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
        holder.mTxtPlayerScore.setText(Integer.toString(scores[position]));

        return view;
    }

    static class ViewHolder {
        private TextView mTxtPlayerName;
        private TextView mTxtPlayerScore;
    }
}
