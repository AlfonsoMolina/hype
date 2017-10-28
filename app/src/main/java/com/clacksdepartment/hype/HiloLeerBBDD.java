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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

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

        ArrayList<Pelicula> estrenos = new ArrayList<>();
        ArrayList<Pelicula> cartelera = new ArrayList<>();

        Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

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

        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA + " ASC"                                    // The sort order
        );

        ContentValues values = new ContentValues();

        //Datos de las películas:
        //link, título, sinopsis, estreno (letras), fecha e hype.
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
            String [] fecha = estreno_fecha.split("-");

            int difAno = Integer.parseInt(fecha[0]) - Calendar.getInstance().get(Calendar.YEAR);
            int difMes = Integer.parseInt(fecha[1]) - Calendar.getInstance().get(Calendar.MONTH) - 1;
            int difDia = Integer.parseInt(fecha[2]) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

            boolean futuro = false; //true si aun no se ha estrenado

            if (difAno == 0){
                if (difMes == 0){
                    if (difDia > 0){
                        futuro = true;
                    }
                }else if (difMes > 0){
                    futuro = true;
                }
            }else if (difAno > 0){
                futuro = true;
            }

            if (tipo == 1) { //Si es de cartelera
                cartelera.add(new Pelicula(enlace,portada_bitmap, portada_enlace, titulo, sinopsis,
                        estreno_letras, estreno_fecha, hype));


            } else if (tipo == 2 && !futuro) {  //Es de cartelera pero está mal catalogada
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_TIPO, 1);

                dbw.update(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, values,
                        FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + "= '" +
                                enlace + "'", null);

                cartelera.add(new Pelicula(enlace,portada_bitmap, portada_enlace, titulo, sinopsis,
                        estreno_letras, estreno_fecha, hype));

            } else { //Es de estrenos
                estrenos.add(new Pelicula(enlace,portada_bitmap, portada_enlace, titulo, sinopsis,
                        estreno_letras, estreno_fecha, hype));
            }

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
