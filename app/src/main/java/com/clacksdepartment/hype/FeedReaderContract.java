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
        public static final String TABLE_NAME = "ESTRENOS";
        public static final String COLUMN_REF = "Ref";
        public static final String COLUMN_TITULO = "Titulo";
        public static final String COLUMN_PORTADA = "Portada";
        public static final String COLUMN_PORTADA_ENLACE = "Portada_Enlace";
        public static final String COLUMN_SINOPSIS = "Sinopsis";
        public static final String COLUMN_TRAILER = "Trailer";
        public static final String COLUMN_ESTRENO_LETRAS = "Estreno_Letras";
        public static final String COLUMN_ESTRENO_FECHA = "Estreno_Fecha";
        public static final String COLUMN_HYPE = "Guardado";
        public static final String COLUMN_TIPO = "Tipo";
    }

    public static final String SQL_CREATE_ENTRIES_ESTRENOS =
            "CREATE TABLE " + FeedEntryEstrenos.TABLE_NAME + " (" +
                    FeedEntryEstrenos._ID + " INTEGER PRIMARY KEY," +
                    FeedEntryEstrenos.COLUMN_REF + " TEXT," +
                    FeedEntryEstrenos.COLUMN_TITULO + " TEXT," +
                    FeedEntryEstrenos.COLUMN_PORTADA + " BLOB," +
                    FeedEntryEstrenos.COLUMN_PORTADA_ENLACE+ " TEXT," +
                    FeedEntryEstrenos.COLUMN_SINOPSIS + " TEXT," +
                    FeedEntryEstrenos.COLUMN_TRAILER + " TEXT," +
                    FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS + " TEXT," +
                    FeedEntryEstrenos.COLUMN_ESTRENO_FECHA + " DATE," +
                    FeedEntryEstrenos.COLUMN_HYPE + " INTEGER," +
                    FeedEntryEstrenos.COLUMN_TIPO + " INTEGER)";

    public static final String SQL_DELETE_ENTRIES_ESTRENOS =
            "DROP TABLE IF EXISTS " + FeedEntryEstrenos.TABLE_NAME;

}