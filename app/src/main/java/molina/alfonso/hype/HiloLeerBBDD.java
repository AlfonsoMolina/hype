package molina.alfonso.hype;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by Usuario on 23/07/2017.
 * <p>
 * Hilo que lee la base de datos y carga la información de las películas en la lista
 */

public class HiloLeerBBDD extends AsyncTask<Void, Integer, Void> {

    private static final String TAG = "HiloLeerBBDD";

    private SQLiteDatabase db;
    private ListaModificadaAdapter lista;

    //Barra de progreso
    private LinearLayout carga_barra;
    private TextView carga_mensaje;

    private ArrayList<Pelicula> nuevasPelis = new ArrayList<>();

    //El constructor necesita la bbdd, la lista y la barra de progreso
    public HiloLeerBBDD(SQLiteDatabase db, ListaModificadaAdapter lista,
                        LinearLayout carga_barra, TextView carga_mensaje) {
        Log.d(TAG, "Inicializando el hilo encargado de leer la BBDD");
        this.db = db;
        this.lista = lista;
        this.carga_barra = carga_barra;
        this.carga_mensaje = carga_mensaje;
    }

    //Se muestra la barra de carga (y se pone en gris) y un mensaje.
    @Override
    protected void onPreExecute() {
        Log.d(TAG, "Actualizando UI antes de ejecutar el hilo");
        for (int i = 0; i < 9; i++)
            carga_barra.getChildAt(i).setBackgroundColor(Color.parseColor("#455a64"));
        carga_barra.setVisibility(View.VISIBLE);
        //carga_mensaje.setVisibility(View.VISIBLE);
    }

    @Override
    protected Void doInBackground(Void... v) {
        Log.d(TAG, "Comenzando lectura de la BBDD");

        Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND);

        //Se lee la bbdd y se guardan los elementos en cursor
        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_TITULO,
                FeedReaderContract.FeedEntry.COLUMN_PORTADA,
                FeedReaderContract.FeedEntry.COLUMN_REF,
                FeedReaderContract.FeedEntry.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntry.COLUMN_ESTRENO,
                FeedReaderContract.FeedEntry.COLUMN_FECHA,
                FeedReaderContract.FeedEntry.COLUMN_HYPE,
                FeedReaderContract.FeedEntry.COLUMN_CORTO
        };

        Cursor cursor = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntry.COLUMN_FECHA + " ASC"                                    // The sort order
        );

        //Habrá diez actualizaciones: 9 con la barra de progreso y la 10ª y última
        //marcador se usará para saber cuando se ha llegado a un décimo de los datos
        int marcador = cursor.getCount() / 10;

        //Datos de las películas:
        //link, título, sinopsis, estreno (letras, fecha, fecha corta e hype.
        String l, t, s, e, f, fc, h;
        byte[] p_byte;
        Bitmap p_bitmap;

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

        int cuenta_peliculas = 0;           //Número de películas analizadas hasta ahora
        int cuenta_actualizaciones = 0;     //Numero de actualizaciones hechas
        int marca_sig = marcador;           //Cuando lleguemos a marga_sig es hora de una actualización

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_FECHA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_TITULO)); //Esto para el log solo

            //Si fecha_hoy después que f, >0. i al revés, < 0
            //Si el estreno ya es pasado, se elimina
            if (fecha_hoy.compareTo(f) > 0) {
                String selection = FeedReaderContract.FeedEntry.COLUMN_REF + " LIKE ?";
                String[] selectionArgs = {t};
                db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
            } else {
                l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_REF));
                p_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_PORTADA));
                s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_SINOPSIS));
                e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_ESTRENO));
                h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_HYPE));
                fc = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_CORTO));

                p_bitmap = BitmapFactory.decodeByteArray(p_byte, 0, p_byte.length);

                nuevasPelis.add(new Pelicula(l, p_bitmap, t, s, e, f, fc, h.equalsIgnoreCase("T")));
            }
            cuenta_peliculas++;

            //Si se ha pasado un décimo de las películas...

            if (cuenta_peliculas >= marca_sig && cuenta_actualizaciones < 9) {
                lista.add(nuevasPelis);
                nuevasPelis.clear();
                publishProgress(cuenta_actualizaciones++);          //Se actualiza
                marca_sig = marcador * (cuenta_actualizaciones + 1);    //Se fija el siguiente marcador
            }
            Log.d(TAG, "Encontrada película: " + t + ".");
        }
        cursor.close();

        return null;
    }

    //Por cada décimo de los datos obtenidos, se avanza la barra de progreso y se actualiza la IU
    @Override
    protected void onProgressUpdate(Integer... i) {
        Log.d(TAG, "Lectura al " + ((i[0] + 1) * 10) + "%");
        carga_barra.getChildAt(i[0]).setBackgroundColor(Color.parseColor("#37474f"));
        lista.setMaxPaginas();
        lista.notifyDataSetChanged();
        lista.actualizarInterfaz();
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

        carga_mensaje.setVisibility(View.GONE);
        carga_barra.setVisibility(View.GONE);

    }

}
