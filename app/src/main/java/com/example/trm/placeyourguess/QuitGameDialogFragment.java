package com.example.trm.placeyourguess;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import static android.app.Activity.RESULT_OK;

public class QuitGameDialogFragment extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Quit game");
        builder.setMessage("Do you want to quit the game?");

        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                quitGame();
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
                quitGame();
            }
        });

        return builder.create();
    }

    private void quitGame() {
        dismiss();

        StreetViewActivity activity = (StreetViewActivity) getActivity();
        Bundle resultData = new Bundle();
        resultData.putInt(StreetViewActivity.RESULT_KEY_SCORE, 0);

        Intent intent = new Intent();
        intent.putExtras(resultData);
        activity.setResult(RESULT_OK, intent);
        activity.finish();
    }
}
