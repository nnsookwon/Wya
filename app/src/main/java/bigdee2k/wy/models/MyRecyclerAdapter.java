package bigdee2k.wy.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import bigdee2k.wy.R;

import static android.content.Context.MODE_PRIVATE;
import static com.facebook.FacebookSdk.getApplicationContext;

/**
 * Created by Nate on 11/11/17.
 */

public class MyRecyclerAdapter extends RecyclerView.Adapter<MyRecyclerAdapter.ViewHolder> {
    private static String TAG = "MyRecyclerAdapter";
    public ArrayList<FacebookFriend> facebookFriends;
    private static RecyclerViewClickListener itemListener;
    private LayoutInflater inflater;
    private Context context;
    SharedPreferences prefs;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case

        public ImageView picture;
        public ImageView block;
        public TextView name;
        public View view;
        public ViewHolder(View v) {
            super(v);
            view = v;
            block = (ImageView) v.findViewById(R.id.block);
            picture = (ImageView) v.findViewById(R.id.friend_list_picture);
            name = (TextView) v.findViewById(R.id.friend_list_name);
        }
        public void putXOverPicture(){
            block.setVisibility(View.VISIBLE);
        }

        public void removeXOverPicture(){
            block.setVisibility(View.INVISIBLE);
        }
    }



    public interface RecyclerViewClickListener {
        void recyclerViewListClicked(View v, int position);
        void recyclerViewListLongClicked(View v, int position);
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyRecyclerAdapter(ArrayList<FacebookFriend> myDataset, Context context) {
        facebookFriends = myDataset;
        this.context = context;

        prefs = context.getSharedPreferences("wya_pref", MODE_PRIVATE);
    }

    public void setItemListener(RecyclerViewClickListener listener) {
        itemListener = listener;
    }

    public void setFriendsList(ArrayList<FacebookFriend> list) {
        facebookFriends = list;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyRecyclerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.recycler_view_row, parent, false);

        ViewHolder viewHolder = new ViewHolder(v);
        Log.d(TAG, "onCreateViewHolder");

        return viewHolder;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Log.d(TAG, "onBindViewHolder");
        holder.name.setText(facebookFriends.get(position).getUserName());
        Picasso.with(getApplicationContext())
                .load(facebookFriends.get(position).getPhotoUrl()).into(holder.picture);
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemListener.recyclerViewListClicked(v, holder.getPosition());
            }
        });
        holder.view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                itemListener.recyclerViewListLongClicked(v, holder.getPosition());
                return true;
            }
        });

        // friend is blocked
        if (prefs.getBoolean("block_" + facebookFriends.get(position).getId(), false)) {
            holder.putXOverPicture();
        }
        else {
            holder.removeXOverPicture();
        }



    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        Log.d(TAG, "list size: " + facebookFriends.size());
        return facebookFriends.size();
    }
}
