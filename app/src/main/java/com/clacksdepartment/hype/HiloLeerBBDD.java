package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static com.clacksdepartment.hype.FeedReaderContract.SQL_CREATE_ENTRIES_ESTRENOS;
import static com.clacksdepartment.hype.FeedReaderContract.SQL_DELETE_ENTRIES_ESTRENOS;

/**
 * Created by Usuario on 23/07/2017.
 * <p>
 * Hilo que lee la base de datos y carga la información de las películas en la mListaModificadaAdapter
 */

class HiloLeerBBDD extends AsyncTask<Void, Integer, Void> {

    private static final String TAG = "HiloLeerBBDD";

    //db[0] para leer db[1] para escribir

    private SQLiteDatabase dbr;
    private SQLiteDatabase dbw;
    private RecyclerViewAdapter lista;

    //El constructor necesita la bbdd, la mListaModificadaAdapter y la barra de progreso
    public HiloLeerBBDD(SQLiteDatabase dbr, SQLiteDatabase dbw, RecyclerViewAdapter lista) {
        Log.d(TAG, "Inicializando el hilo encargado de leer la BBDD");
        this.dbr = dbr;
        this.dbw = dbw;
        this.lista = lista;
    }
    @Override
    protected Void doInBackground(Void... v) {
        Log.d(TAG, "Comenzando lectura de la BBDD");

        Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

        ArrayList<Pelicula> estrenos = new ArrayList<>();
        ArrayList<Pelicula> cartelera = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String today = dateFormat.format(calendar.getTime());


        //Y ahora los estrenos

        //Se lee la bbdd y se guardan los elementos en cursor
        String[] projection = {
                FeedReaderContract.FeedEntryEstrenos._ID,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_REF,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA_ENLACE,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TRAILER,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TIPO
        };

        Cursor cursor;

        try {
            cursor = dbr.query(
                    FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                    projection,                               // The columns to return
                    null,                                // The columns for the WHERE clause
                    null,                                     // The values for the WHERE clause
                    null,                                     // don't group the rows
                    null,                                     // don't filter by row groups
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA + " ASC"                                    // The sort order
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

            dbw.execSQL(SQL_DELETE_ENTRIES_ESTRENOS);
            dbw.execSQL(SQL_CREATE_ENTRIES_ESTRENOS);

            c.close();

            return null;
        }

        ContentValues values = new ContentValues();

        //Datos de las películas:
        String enlace, titulo, sinopsis, estreno_letras, estreno_fecha, trailer, portada_enlace;
        int tipo;
        boolean hype;
        byte[] portada_byte;
        Bitmap portada_bitmap;

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            enlace = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_REF));
            titulo = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO));
            sinopsis = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS));
            estreno_letras = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS));
            estreno_fecha = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA));
            hype = (cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE)) == 1);
            tipo = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_TIPO));
            trailer = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_TRAILER));
            portada_enlace = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA_ENLACE));
            portada_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA));

            portada_bitmap = BitmapFactory.decodeByteArray(portada_byte, 0, portada_byte.length);

            boolean esCartelera; //true si aun no se ha estrenado

            esCartelera = today.compareToIgnoreCase(estreno_fecha) >= 0;

            if (tipo == 1) { //Si es de cartelera
                cartelera.add(new Pelicula(enlace,portada_bitmap, portada_enlace, titulo, sinopsis,
                        estreno_letras, estreno_fecha, hype));


            } else if (tipo == 2 && esCartelera) {  //Es de cartelera pero está mal catalogada
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_TIPO, 1);

                dbw.update(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, values,
                        FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + "='" +
                                enlace + "'", null);

                cartelera.add(new Pelicula(enlace,portada_bitmap, portada_enlace, titulo, sinopsis,
                        estreno_letras, estreno_fecha, hype));

            } else { //Es de estrenos
                estrenos.add(new Pelicula(enlace,portada_bitmap, portada_enlace, titulo, sinopsis,
                        estreno_letras, estreno_fecha, hype));
            }

            values.clear();
            Log.d(TAG, "Encontrada película: " + titulo + ".");
        }

        cursor.close();

        Collections.sort(cartelera, new Comparator<Pelicula>() {
            @Override
            public int compare(Pelicula p1, Pelicula p2) {
                return (p2.getEstrenoFecha()).compareToIgnoreCase(p1.getEstrenoFecha());
            }
        });

        lista.addCartelera(cartelera);
        lista.addEstrenos(estrenos);
        return null;
    }

    //Se actualiza la IU y se oculta la barra de progreso.
    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "Lectura finalizada, actualizando interfaz");
        //lista.notifyDataSetChanged();
        lista.actualizarInterfaz();

        //Se comprueba si no hay películas (primera ejecución), para mostrar un emnsaje.
   //     lista.noHayPelis();
    }

}
