package adapters;

import android.app.Activity;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.trm.placeyourguess.R;

public class PlayerListAdapter extends ArrayAdapter<String> {

    private String boldName;
    private String[] players;
    private Activity context;

    public PlayerListAdapter(Activity context, String boldName, String[] players) {
        super(context, R.layout.player_list_item, players);
        this.context = context;
        this.boldName = boldName;
        this.players = players;
    }

    @NonNull
    public View getView(int position, View view, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        view = inflater.inflate(R.layout.player_list_item, null, true);

        TextView txtPlayer = (TextView) view.findViewById(R.id.txt_player);

        txtPlayer.setText(players[position]);
        if (players[position].equals(boldName)) {
            txtPlayer.setTypeface(txtPlayer.getTypeface(), Typeface.BOLD);
        }

        return view;
    }
}
