package molina.alfonso.hype;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import static molina.alfonso.hype.FeedReaderContract.SQL_CREATE_ENTRIES_CARTELERA;
import static molina.alfonso.hype.FeedReaderContract.SQL_CREATE_ENTRIES_ESTRENOS;
import static molina.alfonso.hype.FeedReaderContract.SQL_DELETE_ENTRIES_ESTRENOS;

/**
 * Created by Usuario on 11/07/2017.
 */

public class FeedReaderDbHelper extends SQLiteOpenHelper {

    /*
     * Declaración de variables
     */

    private static final String TAG = "FeedReaderDbHelper";

    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Peliculas.db";

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.d(TAG, "FeedReaderDbHelper");
    }
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "onCreate");
        db.execSQL(SQL_CREATE_ENTRIES_ESTRENOS);
        db.execSQL(SQL_CREATE_ENTRIES_CARTELERA);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        Log.d(TAG, "onUpgrade");
        db.execSQL(SQL_DELETE_ENTRIES_ESTRENOS);
        db.execSQL(SQL_CREATE_ENTRIES_CARTELERA);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "onDowngrade");
        onUpgrade(db, oldVersion, newVersion);
    }
}