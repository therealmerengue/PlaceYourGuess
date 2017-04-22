package com.example.trm.placeyourguess;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class LocationListAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final String[] itemNames;
    private final Integer[] imgID;

    public LocationListAdapter(Activity context, String[] itemNames, Integer[] imgIDs) {
        super(context, R.layout.country_list_item, itemNames);

        this.context = context;
        this.itemNames = itemNames;
        this.imgID = imgIDs;
    }

    @NonNull
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;

        if (view == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            view = inflater.inflate(R.layout.country_list_item, null, true);
            holder = new ViewHolder();
            holder.mTxtCountryName = (TextView) view.findViewById(R.id.txt_countryName);
            holder.mIvFlag = (ImageView) view.findViewById(R.id.iv_flag);

            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        holder.mTxtCountryName.setText(itemNames[position]);
        holder.mIvFlag.setImageResource(imgID[position]);

        return view;
    }

    static class ViewHolder {
        private TextView mTxtCountryName;
        private ImageView mIvFlag;
    }
}
