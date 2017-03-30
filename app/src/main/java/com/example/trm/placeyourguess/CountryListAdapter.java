package com.example.trm.placeyourguess;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CountryListAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final String[] itemNames;
    private final Integer[] imgID;

    public CountryListAdapter(Activity context, String[] itemNames, Integer[] imgIDs) {
        super(context, R.layout.list_item, itemNames);

        this.context = context;
        this.itemNames = itemNames;
        this.imgID = imgIDs;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.list_item, null, true);

        TextView txtCountryName = (TextView) rowView.findViewById(R.id.txt_countryName);
        ImageView ivFlag = (ImageView) rowView.findViewById(R.id.iv_flag);

        txtCountryName.setText(itemNames[position]);
        ivFlag.setImageResource(imgID[position]);
        return rowView;
    }
}
