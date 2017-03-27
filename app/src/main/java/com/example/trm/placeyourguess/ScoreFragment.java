package com.example.trm.placeyourguess;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

public class ScoreFragment extends Fragment {

    private float mPassedDistance;

    private TextView mTxtDistance;

    private OnFragmentInteractionListener mListener;

    public ScoreFragment() {}

    public static ScoreFragment newInstance() {
        return new ScoreFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_score, container, false);

        mTxtDistance = (TextView) fragmentView.findViewById(R.id.txt_Distance);

        FloatingActionButton btnNextLocation = (FloatingActionButton) fragmentView.findViewById(R.id.btn_nextLocation);
        btnNextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onScoreFragmentInteraction();
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void setDistance(float distance) {
        mPassedDistance = distance;

        if (mTxtDistance != null) {
            String label = "Distance: " + Float.toString(mPassedDistance);
            mTxtDistance.setText(label);
        }
    }

    public interface OnFragmentInteractionListener {
        void onScoreFragmentInteraction();
    }
}
