package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by Usuario on 23/07/2017.
 * <p>
 * Hilo que lee la base de datos y carga la información de las películas en la mListaModificadaAdapter
 */

public class HiloLeerBBDD extends AsyncTask<Void, Integer, Void> {

    private static final String TAG = "HiloLeerBBDD";

    //db[0] para leer db[1] para escribir

    private SQLiteDatabase dbr;
    private SQLiteDatabase dbw;
    private ListaModificadaAdapter lista;

    //El constructor necesita la bbdd, la mListaModificadaAdapter y la barra de progreso
    public HiloLeerBBDD(SQLiteDatabase dbr, SQLiteDatabase dbw, ListaModificadaAdapter lista) {
        Log.d(TAG, "Inicializando el hilo encargado de leer la BBDD");
        this.dbr = dbr;
        this.dbw = dbw;
        this.lista = lista;
    }

    @Override
    protected Void doInBackground(Void... v) {
        Log.d(TAG, "Comenzando lectura de la BBDD");

        ArrayList<Pelicula> estrenos = new ArrayList<>();
        ArrayList<Pelicula> cartelera = new ArrayList<>();

        Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

        //Primero se carga la cartelera

        //Se lee la bbdd y se guardan los elementos en cursor
        String[] projection = {
                FeedReaderContract.FeedEntryCartelera._ID,
                FeedReaderContract.FeedEntryCartelera.COLUMN_TITULO,
                FeedReaderContract.FeedEntryCartelera.COLUMN_PORTADA,
                FeedReaderContract.FeedEntryCartelera.COLUMN_REF,
                FeedReaderContract.FeedEntryCartelera.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntryCartelera.COLUMN_ESTRENO,
                FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA,
                FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE
        };

        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntryCartelera.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA + " DESC"                                    // The sort order
        );

        //Datos de las películas:
        //link, título, sinopsis, estreno (letras, fecha e hype.
        String l, t, s, e, f, h;
        byte[] p_byte;
        Bitmap p_bitmap;

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_TITULO)); //Esto para el log solo
            l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_REF));
            p_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_PORTADA));
            s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_SINOPSIS));
            e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_ESTRENO));
            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE));

            p_bitmap = BitmapFactory.decodeByteArray(p_byte, 0, p_byte.length);

            cartelera.add(new Pelicula(l, p_bitmap, t, s, e, f, h.equalsIgnoreCase("T")));

            Log.d(TAG, "Encontrada película: " + t + ".");

        }

        cursor.close();

        //Y ahora los estrenos

        //Se lee la bbdd y se guardan los elementos en cursor
        String[] projection2 = {
                FeedReaderContract.FeedEntryEstrenos._ID,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_REF,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_CORTO
        };

        cursor = dbr.query(
                FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                projection2,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA + " ASC"                                    // The sort order
        );

        //Se coge el día de hoy
        String year = "" + Calendar.getInstance().get(Calendar.YEAR);
        int month_i = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int day_i = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        String month;
        String day;

        if (month_i < 10)
            month = "0" + month_i;
        else
            month = "" + month_i;

        if (day_i < 10)
            day = "0" + day_i;
        else
            day = "" + day_i;

        String fecha_hoy = year + '/' + month + '/' + day;

        ContentValues values = new ContentValues();

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO)); //Esto para el log solo
            l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_REF));
            p_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA));
            s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS));
            e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO));
            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE));

            p_bitmap = BitmapFactory.decodeByteArray(p_byte, 0, p_byte.length);

            if (fecha_hoy.compareTo(f) >= 0) {
                String selection = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + " LIKE ?";
                String[] selectionArgs = {l};
                dbr.delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, selection, selectionArgs);

                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_REF, l);
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO, t);
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA, p_byte);
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS, s);
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, h);
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO, e);
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA, f);

                dbw.insert(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, values);

                cartelera.add(new Pelicula(l, p_bitmap, t, s, e, f, h.equalsIgnoreCase("T")));

            } else {
                estrenos.add(new Pelicula(l, p_bitmap, t, s, e, f, h.equalsIgnoreCase("T")));
            }

            Log.d(TAG, "Encontrada película: " + t + ".");
        }
        lista.addCartelera(cartelera);
        lista.addEstrenos(estrenos);
        return null;
    }

    //Se actualiza la IU y se oculta la barra de progreso.
    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "Lectura finalizada, actualizando interfaz");
        lista.setMaxPaginas();
        lista.notifyDataSetChanged();
        lista.actualizarInterfaz();

        //Se comprueba si no hay películas (primera ejecución), para mostrar un emnsaje.
        lista.noHayPelis();
    }

}
