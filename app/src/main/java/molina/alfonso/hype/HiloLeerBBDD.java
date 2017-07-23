package molina.alfonso.hype;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Usuario on 23/07/2017.
 */

public class HiloLeerBBDD extends AsyncTask<Void,Integer,Void> {

    private static final String TAG = "HiloLeerBBDD";

    private SQLiteDatabase db;
    private ListaModificadaAdapter lista;
    private LinearLayout carga_barra;
    private TextView carga_mensaje;

    public HiloLeerBBDD (SQLiteDatabase db, ListaModificadaAdapter lista, LinearLayout carga_barra, TextView carga_mensaje) {
        Log.d(TAG, "HiloLeerBBDD");
        this.db = db;
        this.lista = lista;
        this.carga_barra = carga_barra;
        this.carga_mensaje = carga_mensaje;
    }

    @Override
    protected Void doInBackground(Void... v) {
        Log.d(TAG, "doInBackground");
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
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
                FeedReaderContract.FeedEntry.COLUMN_FECHA+" ASC"                                    // The sort order
        );

        int cuenta = cursor.getCount()/9;
        String l, p, t, s, e, f, h, fc;

        String year = "" + Calendar.getInstance().get(Calendar.YEAR);
        int month_i = Calendar.getInstance().get(Calendar.MONTH)+1;
        int day_i = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        String month;
        String day;

        if (month_i < 10)
            month = "0" + month_i;
        else
            month = ""+month_i;

        if (day_i < 10)
            day = "0" + day_i;
        else
            day = "" + day_i;

        String fecha_hoy = year + '/' + month + '/' + day;

        int i = 0;
        int j = 0;
        int cuenta_temp = cuenta;
        while(cursor.moveToNext()) {
            l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_REF));
            p = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_PORTADA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_TITULO));
            s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_SINOPSIS));
            e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_ESTRENO));
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_FECHA));
            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_HYPE));
            fc = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_CORTO));


            //Si fecha_hoy después que f, >0
            // si al revés, < 0
            if(fecha_hoy.compareTo(f)>0) {
                String selection = FeedReaderContract.FeedEntry.COLUMN_REF + " LIKE ?";
                // Specify arguments in placeholder order.
                String[] selectionArgs = { l };
                // Issue SQL statement.
                db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
            }else
                lista.add(new Pelicula(l,p,t,s,e,f,fc,h.equalsIgnoreCase("T")));

            i++;
            if(i >= cuenta_temp){
                publishProgress(j++);
                cuenta_temp = cuenta*(j+1);
            }

        }
        cursor.close();

        return null;
    }

    @Override
    protected void onPreExecute (){
        Log.d(TAG, "onPreExecute");
        for(int i = 0; i < 9; i++)
            carga_barra.getChildAt(i).setBackgroundColor(Color.GRAY);
        carga_barra.setVisibility(View.VISIBLE);
        carga_mensaje.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onProgressUpdate(Integer... i) {
        Log.d(TAG, "onProgressUpdate");
        carga_barra.getChildAt(i[0]).setBackgroundColor(Color.GREEN);
        lista.setMaxPaginas();
        lista.notifyDataSetChanged();
        lista.actualizarInterfaz();
    }

    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "onPostExecute");
        lista.setMaxPaginas();
        lista.notifyDataSetChanged();
        lista.actualizarInterfaz();
        lista.noHayPelis();
        carga_mensaje.setVisibility(View.GONE);
        carga_barra.setVisibility(View.GONE);

    }
}
