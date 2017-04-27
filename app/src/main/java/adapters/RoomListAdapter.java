package adapters;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.trm.placeyourguess.R;

public class RoomListAdapter extends ArrayAdapter {

    private Activity context;
    private String[] roomNames;
    private String[] playerCountsInRooms;

    public RoomListAdapter(Activity context, String[] roomNames, String[] playerCountsInRooms) {
        super(context, R.layout.room_list_item, roomNames);

        this.context = context;
        this.roomNames = roomNames;
        this.playerCountsInRooms = playerCountsInRooms;
    }

    @NonNull
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;

        if (view == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            view = inflater.inflate(R.layout.room_list_item, null, true);
            holder = new ViewHolder();
            holder.mTxtRoomName = (TextView) view.findViewById(R.id.txt_roomName);
            holder.mTxtNumberOfPlayers = (TextView) view.findViewById(R.id.txt_numberOfPlayers);

            view.setTag(holder);
        }
        else {
            holder = (ViewHolder) view.getTag();
        }

        holder.mTxtRoomName.setText(roomNames[position]);
        holder.mTxtNumberOfPlayers.setText(playerCountsInRooms[position]);

        return view;
    }

    static class ViewHolder {
        private TextView mTxtRoomName;
        private TextView mTxtNumberOfPlayers;
    }
}
