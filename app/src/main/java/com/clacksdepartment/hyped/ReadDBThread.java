package com.clacksdepartment.hyped;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.clacksdepartment.hyped.FeedReaderContract.SQL_CREATE_ENTRIES_RELEASES;
import static com.clacksdepartment.hyped.FeedReaderContract.SQL_DELETE_ENTRIES_RELEASES;

class ReadDBThread extends AsyncTask<Void, Integer, Void> {

    private static final String TAG = "ReadDBThread";
    private SQLiteDatabase dbr;
    private SQLiteDatabase dbw;
    private RecyclerViewAdapter movieList;

    // It needs the DB, the mModifiedListAdapter and the load bar
    ReadDBThread(SQLiteDatabase dbr, SQLiteDatabase dbw, RecyclerViewAdapter movieList) {
        Log.d(TAG, "Initializing the thread that will read the DB");
        this.dbr = dbr;
        this.dbw = dbw;
        this.movieList = movieList;
    }

    @Override
    protected Void doInBackground(Void... v) {
        Log.d(TAG, "Starting DB read");

        Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

        ArrayList<Movie> releases = new ArrayList<>();
        ArrayList<Movie> theaters = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String today = dateFormat.format(calendar.getTime());

        String[] projection = {
                FeedReaderContract.FeedEntryReleases._ID,
                FeedReaderContract.FeedEntryReleases.COLUMN_REF,
                FeedReaderContract.FeedEntryReleases.COLUMN_TITLE,
                FeedReaderContract.FeedEntryReleases.COLUMN_COVER,
                FeedReaderContract.FeedEntryReleases.COLUMN_COVER_LINK,
                FeedReaderContract.FeedEntryReleases.COLUMN_SYNOPSIS,
                FeedReaderContract.FeedEntryReleases.COLUMN_TRAILER,
                FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE_STRING,
                FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE,
                FeedReaderContract.FeedEntryReleases.COLUMN_HYPE,
                FeedReaderContract.FeedEntryReleases.COLUMN_TYPE
        };

        Cursor cursor;

        try {
            cursor = dbr.query(
                    FeedReaderContract.FeedEntryReleases.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                                     // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE + " ASC"                                    // The sort order
            );
        }catch (Exception e){

            // query to obtain the names of all tables in your database
            Cursor c = dbw.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null);
            List<String> tables = new ArrayList<>();

            // iterate over the result set, adding every table name to a list
            while (c.moveToNext()) {
                tables.add(c.getString(0));
            }

            // call DROP TABLE on every table name
            for (String table : tables) {
                String dropQuery = "DROP TABLE IF EXISTS " + table;
                dbw.execSQL(dropQuery);
            }

            dbw.execSQL(SQL_DELETE_ENTRIES_RELEASES);
            dbw.execSQL(SQL_CREATE_ENTRIES_RELEASES);

            c.close();

            return null;
        }

        ContentValues values = new ContentValues();

        // Data for each movie:
        String link, title, synopsis, releaseDateString, releaseDate, trailer, coverLink;
        int type;
        boolean hype;
        byte[] coverByte;
        Bitmap coverBitmap;

        while (cursor.moveToNext()) {
            link = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_REF));
            title = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_TITLE));
            synopsis = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_SYNOPSIS));
            releaseDateString = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE_STRING));
            releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE));
            hype = (cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_HYPE)) == 1);
            type = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_TYPE));
            trailer = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_TRAILER));
            coverLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_COVER_LINK));
            coverByte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_COVER));

            coverBitmap = BitmapFactory.decodeByteArray(coverByte, 0, coverByte.length);

            boolean itIsInTheaters; //if it has already been released
            itIsInTheaters = today.compareToIgnoreCase(releaseDate) >= 0;

            if (type == 1) { // It is already on theaters
                theaters.add(new Movie(link,coverBitmap, coverLink, title, synopsis,
                        releaseDateString, releaseDate, hype));
            // It was initially on next releases, but it has been already released
            } else if (type == 2 && itIsInTheaters) {
                values.put(FeedReaderContract.FeedEntryReleases.COLUMN_TYPE, 1);

                dbw.update(FeedReaderContract.FeedEntryReleases.TABLE_NAME, values,
                        FeedReaderContract.FeedEntryReleases.COLUMN_REF + "='" +
                                link + "'", null);

                theaters.add(new Movie(link,coverBitmap, coverLink, title, synopsis,
                        releaseDateString, releaseDate, hype));

            } else { // Coming soon
                releases.add(new Movie(link,coverBitmap, coverLink, title, synopsis,
                        releaseDateString, releaseDate, hype));
            }

            values.clear();
            Log.d(TAG, "Found movie: " + title + ".");
        }

        cursor.close();

        Collections.sort(theaters, new Comparator<Movie>() {
            @Override
            public int compare(Movie p1, Movie p2) {
                return (p2.getReleaseDate()).compareToIgnoreCase(p1.getReleaseDate());
            }
        });

        movieList.addToTheatersList(theaters);
        movieList.addToReleasesList(releases);
        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        movieList.removeList();
    }

    // Update UI and remove load bar.
    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "DB read finished, updating interface.");
        movieList.updateInterface();
    }

}
