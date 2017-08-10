package molina.alfonso.hype;

import android.provider.BaseColumns;

/**
 * Created by Usuario on 11/07/2017.
 */

public final class FeedReaderContract {

    /*
     * Declaración de variables
     */

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private FeedReaderContract() {}

    /* Inner class that defines the table contents */
    public static class FeedEntry implements BaseColumns {
        public static final String TABLE_NAME = "peliculas";
        public static final String COLUMN_REF = "Ref";
        public static final String COLUMN_TITULO = "Titulo";
        public static final String COLUMN_PORTADA = "Portada";
        public static final String COLUMN_SINOPSIS = "Sinopsis";
        public static final String COLUMN_ESTRENO = "Estreno";
        public static final String COLUMN_FECHA = "Fecha";
        public static final String COLUMN_CORTO = "Corto";
        public static final String COLUMN_HYPE = "Guardado";
    }

    public static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + FeedEntry.TABLE_NAME + " (" +
                    FeedEntry._ID + " INTEGER PRIMARY KEY," +
                    FeedEntry.COLUMN_REF + " TEXT," +
                    FeedEntry.COLUMN_TITULO + " TEXT," +
                    FeedEntry.COLUMN_PORTADA + " BLOB," +
                    FeedEntry.COLUMN_SINOPSIS + " TEXT," +
                    FeedEntry.COLUMN_ESTRENO + " TEXT," +
                    FeedEntry.COLUMN_FECHA + " TEXT," +
                    FeedEntry.COLUMN_CORTO + " TEXT," +
                    FeedEntry.COLUMN_HYPE + " TEXT)";

    public static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + FeedEntry.TABLE_NAME;




}