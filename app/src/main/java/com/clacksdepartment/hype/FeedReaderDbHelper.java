package com.clacksdepartment.hype;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static com.clacksdepartment.hype.FeedReaderContract.SQL_CREATE_ENTRIES_RELEASES;
import static com.clacksdepartment.hype.FeedReaderContract.SQL_DELETE_ENTRIES_RELEASES;


public class FeedReaderDbHelper extends SQLiteOpenHelper {

    private static final String TAG = "FeedReaderDbHelper";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "HypeMovies.db";

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "FeedReaderDbHelper");
    }
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(SQL_CREATE_ENTRIES_RELEASES);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        Log.d(TAG, "onUpgrade");
        db.execSQL(SQL_DELETE_ENTRIES_RELEASES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade");
        onUpgrade(db, oldVersion, newVersion);
    }
}