package com.jonlee.android.SJplayer;

import android.content.Context;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class SJBackupFileAdapter extends ArrayAdapter<BackupFileResult> {

    private Context mContext;
    private BackupFileResult[] backupFileResults;

    public SJBackupFileAdapter(@NonNull Context context, BackupFileResult[] list) {
        super(context, 0 , list);
        mContext = context;
        backupFileResults = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View listItem = convertView;
        if(listItem == null)
            listItem = LayoutInflater.from(mContext).inflate(R.layout.image_list_item,parent,false);

        BackupFileResult currentFile = backupFileResults[position];

        ImageView image = (ImageView)listItem.findViewById(R.id.imageView_status);
        image.setImageResource(currentFile.getStatusImage());

        TextView name = (TextView) listItem.findViewById(R.id.textView_filename);
        name.setText(currentFile.getFileName());

        TextView release = (TextView) listItem.findViewById(R.id.textView_description);
        String desc = "[" + currentFile.getParentFolderName() + "] " + currentFile.getBackupResultDescription();
        release.setText(desc);

        listItem.setContentDescription("Shared file " + currentFile.getFileName() + ", " + desc);

        return listItem;
    }
}
