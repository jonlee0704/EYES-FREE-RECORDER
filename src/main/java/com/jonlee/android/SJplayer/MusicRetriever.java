package com.jonlee.android.SJplayer;

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.ContentUris;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 *
 * TODO Need to think about if this class is needed in case ContentResolver will be used inside of Recorder class.
 * TODO Some possibility of usecases like random song selection, re-encoding double-byte TAGs.
 */
public class MusicRetriever {
    final String TAG = "MusicRetriever";

    ContentResolver mContentResolver;
    Cursor cur;

    // the items (songs) we have queried
    List<Item> mItems = new ArrayList<Item>();

    //TODO After getting into Artist navigation mode(UP_RIGHT/BOTTOM_LEFT),
    // some quick Alphabetical navigation/search feature required.
    // HashTable, Artist:Index
    Hashtable mArtistTable = new Hashtable();

    Random mRandom = new Random();

    public MusicRetriever(ContentResolver cr) {
        mContentResolver = cr;
    }

    public int getSongCount(){
        return mItems.size();
    }

    public void previous(){
        if(cur.isFirst())
            cur.moveToLast();
        else
            cur.moveToPrevious();
    }

    public void next(){
        //Log.i(TAG, "getCurrentItem:" + cur.isBeforeFirst() + ":" + cur.isAfterLast() + ":"+ cur.isFirst() + ":" + cur.isLast());

        if(cur.isLast())
            cur.moveToFirst();
        else
            cur.moveToNext();
        //Log.i(TAG, "Next:" + cur.getPosition());
    }

    public String getSongTitle(){
        return mItems.get(cur.getPosition()).getTitle();
    }

    public Cursor getCursor(){
        return this.cur;
    }

    /**
     * TODO In this virtual directory mode, more navigation mode needs to be supported.
     */
    public void moveNextArtist(){
    }

    /**
     * Loads music data. This method may take long, so be sure to call it asynchronously without
     * blocking the main thread.
     */
    public void prepare() {
        Uri uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Log.i(TAG, "Querying media...");
        Log.i(TAG, "URI: " + uri.toString());

        // Perform a query on the content resolver. The URI we're passing specifies that we
        // want to query for all audio media on external storage (e.g. SD card)
        cur = mContentResolver.query(uri, null,
                MediaStore.Audio.Media.IS_MUSIC + " = 1", null, null);
        Log.i(TAG, "Query finished. " + (cur == null ? "Returned NULL." : "Returned a cursor."));

        if (cur == null) {
            // Query failed...
            Log.e(TAG, "Failed to retrieve music: cursor is null :-(");
            return;
        }
        if (!cur.moveToFirst()) {
            // Nothing to query. There is no music on the device. How boring.
            Log.e(TAG, "Failed to move cursor to first row (no query results).");
            return;
        }

        Log.i(TAG, "Listing...");

        // retrieve the indices of the columns where the ID, title, etc. of the song are
        int artistColumn = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        int titleColumn = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
        int albumColumn = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        int trackNumber = cur.getColumnIndex(MediaStore.Audio.Media.TRACK);
        int durationColumn = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
        int idColumn = cur.getColumnIndex(MediaStore.Audio.Media._ID);

//        Log.i(TAG, "Title column index: " + String.valueOf(titleColumn));
//        Log.i(TAG, "ID column index: " + String.valueOf(titleColumn));

        // add each song to mItems
        do {
            //Log.i(TAG, "ID: " + cur.getString(idColumn) + " Title: " + cur.getString(titleColumn));
            mItems.add(new Item(
                    cur.getLong(idColumn),
                    cur.getString(artistColumn),
                    cur.getString(titleColumn),
                    cur.getString(albumColumn),
                    cur.getString(trackNumber),
                    cur.getLong(durationColumn)));
            //Artist navigation
            this.mArtistTable.put(cur.getString(artistColumn), cur.getPosition());
        } while (cur.moveToNext());

        //After adding all musics, then move to the first again.
        cur.moveToFirst();

        Log.i(TAG, "Done querying media. MusicRetriever is ready.");
    }

    public Uri getCurrentUri(){
        Log.i(TAG,"currentUri:" + mItems.get(0).getURI()+" getColumnCount:"+cur.getColumnCount() + ":" + this.getSongTitle());
        return mItems.get(cur.getPosition()).getURI();
    }

    public Item getCurrentItem(){
        Log.i(TAG, "musicRetriever.getSongCount():" + getSongCount());
        return mItems.get(cur.getPosition());
    }

    public ContentResolver getContentResolver() {
        return mContentResolver;
    }

    /** Returns a random Item. If there are no items available, returns null. */
    public Item getRandomItem() {
        if (mItems.size() <= 0) return null;
        return mItems.get(mRandom.nextInt(mItems.size()));
    }

    /**
     * Gets the corresponding path to a file from the given content:// URI
     */
    public String getFilePathFromContentUri() {
        String filePath;
        String[] filePathColumn = {MediaStore.MediaColumns.DATA};

        Cursor cursor = mContentResolver.query(mItems.get(cur.getPosition()).getURI(), filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        filePath = cursor.getString(columnIndex);
        cursor.close();
        return filePath;
    }

    public static class Item {
        long id;
        String artist;
        String title;
        String album;
        String trackNumber;
        long duration;

        public Item(long id, String artist, String title, String album, String trackNumber, long duration) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
            this.trackNumber = trackNumber;
        }

        public long getId() {
            return id;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public String getTrackNumber() { return this.trackNumber; }

        public long getDuration() {
            return duration;
        }

        public Uri getURI() {
            return ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
    }
}