package com.example.trm.placeyourguess;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import static android.app.Activity.RESULT_OK;
import static com.example.trm.placeyourguess.MultiplayerNewRoomActivity.EXTRA_PLAYER_NAME;

public class JoinExistingRoomDialogFragment extends DialogFragment {

    private static final String KEY_ROOM_NAME = "KEY_ROOM_NAME";
    private static final String KEY_PLAYER_NAME = "KEY_PLAYER_NAME";

    private String roomName;
    private String playerName;

    public static JoinExistingRoomDialogFragment newInstance(String roomName, String playerName) {
        JoinExistingRoomDialogFragment fragment = new JoinExistingRoomDialogFragment();

        Bundle args = new Bundle();
        args.putString(KEY_ROOM_NAME, roomName);
        args.putString(KEY_PLAYER_NAME, playerName);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        roomName = args.getString(KEY_ROOM_NAME);
        playerName = args.getString(KEY_PLAYER_NAME);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Join existing room");
        builder.setMessage("Room already exists - do you want to join it?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();

                Intent intent = new Intent(getContext(), MultiplayerRoomActivity.class);
                intent.putExtra(MultiplayerNewRoomActivity.EXTRA_ROOM_NAME, roomName);
                intent.putExtra(MultiplayerNewRoomActivity.EXTRA_PLAYER_NAME, playerName);
                intent.putExtra(MultiplayerNewRoomActivity.EXTRA_IS_HOST, false);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dismiss();
            }
        });

        return builder.create();
    }
}
