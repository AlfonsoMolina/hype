package com.clacksdepartment.hype;

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
    public static class FeedEntryEstrenos implements BaseColumns {
        public static final String TABLE_NAME = "estrenos";
        public static final String COLUMN_REF = "Ref";
        public static final String COLUMN_TITULO = "Titulo";
        public static final String COLUMN_PORTADA = "Portada";
        public static final String COLUMN_SINOPSIS = "Sinopsis";
        public static final String COLUMN_ESTRENO = "Estreno";
        public static final String COLUMN_FECHA = "Fecha";
        public static final String COLUMN_CORTO = "Corto";
        public static final String COLUMN_HYPE = "Guardado";
    }

    public static final String SQL_CREATE_ENTRIES_ESTRENOS =
            "CREATE TABLE " + FeedEntryEstrenos.TABLE_NAME + " (" +
                    FeedEntryEstrenos._ID + " INTEGER PRIMARY KEY," +
                    FeedEntryEstrenos.COLUMN_REF + " TEXT," +
                    FeedEntryEstrenos.COLUMN_TITULO + " TEXT," +
                    FeedEntryEstrenos.COLUMN_PORTADA + " BLOB," +
                    FeedEntryEstrenos.COLUMN_SINOPSIS + " TEXT," +
                    FeedEntryEstrenos.COLUMN_ESTRENO + " TEXT," +
                    FeedEntryEstrenos.COLUMN_FECHA + " TEXT," +
                    FeedEntryEstrenos.COLUMN_CORTO + " TEXT," +
                    FeedEntryEstrenos.COLUMN_HYPE + " TEXT)";

    public static final String SQL_DELETE_ENTRIES_ESTRENOS =
            "DROP TABLE IF EXISTS " + FeedEntryEstrenos.TABLE_NAME;

    /* Inner class that defines the table contents */
    public static class FeedEntryCartelera implements BaseColumns {
        public static final String TABLE_NAME = "cartelera";
        public static final String COLUMN_REF = "Ref";
        public static final String COLUMN_TITULO = "Titulo";
        public static final String COLUMN_PORTADA = "Portada";
        public static final String COLUMN_SINOPSIS = "Sinopsis";
        public static final String COLUMN_ESTRENO = "Estreno";
        public static final String COLUMN_FECHA = "Fecha";
        public static final String COLUMN_HYPE = "Guardado";
        public static final String COLUMN_SIGUE = "Sigue";
    }

    public static final String SQL_CREATE_ENTRIES_CARTELERA =
            "CREATE TABLE " + FeedEntryCartelera.TABLE_NAME + " (" +
                    FeedEntryCartelera._ID + " INTEGER PRIMARY KEY," +
                    FeedEntryCartelera.COLUMN_REF + " TEXT," +
                    FeedEntryCartelera.COLUMN_TITULO + " TEXT," +
                    FeedEntryCartelera.COLUMN_PORTADA + " BLOB," +
                    FeedEntryCartelera.COLUMN_SINOPSIS + " TEXT," +
                    FeedEntryCartelera.COLUMN_ESTRENO + " TEXT," +
                    FeedEntryCartelera.COLUMN_FECHA + " TEXT," +
                    FeedEntryCartelera.COLUMN_HYPE + " TEXT," +
                    FeedEntryCartelera.COLUMN_SIGUE + " INTEGER(1))";

    public static final String SQL_DELETE_ENTRIES_CARTELERA =
            "DROP TABLE IF EXISTS " + FeedEntryCartelera.TABLE_NAME;

}