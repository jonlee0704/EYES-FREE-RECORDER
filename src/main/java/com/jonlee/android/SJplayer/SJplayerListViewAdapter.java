package com.jonlee.android.SJplayer;

import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.w3c.dom.Text;

public class SJplayerListViewAdapter extends RecyclerView.Adapter<SJplayerListViewAdapter.MyViewHolder> {


    private static String  TAG = "SJplayerListViewAdapter";
    private BackupFileResult[] mDataset;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        // each data item is just a string in this case
        public View listView;


        private String mItem;

        public MyViewHolder(View view) {
            super(view);
            view.setOnClickListener(this);
            listView = view;
        }
//
//        public void setItem(String item) {
//            mItem = item;
//            mTextView.setText(item);
//        }

        @Override
        public void onClick(View view) {
            Log.i(SJplayerListViewAdapter.TAG, "onClick ");
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public SJplayerListViewAdapter(BackupFileResult[] myDataset) {
        mDataset = myDataset;

    }

    // Create new views (invoked by the layout manager)
    @Override
    public SJplayerListViewAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                     int viewType) {
        // create a new view
        View listItem = (View) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.image_list_item, parent, false);
        MyViewHolder vh = new MyViewHolder(listItem);
        return vh;
    }



    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        BackupFileResult item = mDataset[position];
        TextView nameView = holder.listView.findViewById(R.id.textView_filename);
        TextView descView = holder.listView.findViewById(R.id.textView_description);
        ImageView statusView = holder.listView.findViewById(R.id.imageView_status);

        nameView.setText(item.getFileName());
        descView.setText(item.getBackupResultDescription());
        statusView.setImageResource(item.getStatusImage());
        holder.listView.setContentDescription("Folder " + item.getFileName() + ", " + item.getBackupResultDescription());
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
