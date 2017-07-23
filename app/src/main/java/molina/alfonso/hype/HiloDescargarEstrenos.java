package molina.alfonso.hype;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Clacks Department on 11/07/2017.
 */

public class HiloDescargarEstrenos extends AsyncTask<SQLiteDatabase,Integer,ArrayList<Pelicula>> {

    /*
     * Declaración de variables
     */
    private static final String TAG = "HiloDescargarEstrenos";

    private String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private String[] meses_corto = {"ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic"};
    private String idioma = "es";
    private String pais = "es";
    private int pagina = 1;

    private ListaModificadaAdapter lista;
    private LinearLayout carga_barra;
    private TextView carga_mensaje;

    public HiloDescargarEstrenos(ListaModificadaAdapter lista, LinearLayout carga_barra, TextView carga_mensaje) {
        Log.d(TAG, "HiloDescargarEstrenos");
        this.lista = lista;
        this.carga_barra = carga_barra;
        this.carga_mensaje = carga_mensaje;
     }

    @Override
    protected ArrayList<Pelicula> doInBackground(SQLiteDatabase... db) {
        Log.d(TAG, "doInBackground");
        //db[0] para leer db[1] para escribir
        String html = "";
        String dir = "";
        ArrayList<Pelicula> peliculas = new ArrayList<>();

        while (pagina <= 10) {
            try {
                dir = "https://m.filmaffinity.com/" + idioma + "/rdcat.php?id=upc_th_" + pais + "&page=" + pagina;
                html = getHTML(dir);
            } catch (IOException ee) {
                html = null;
            }

            if (html != null) {

                String[] peliculasHTML = html.split("-item\" href=\"");
                String l;
                String p;
                String t;
                String s;
                String e;
                String f;
                String fc;
                Boolean h;
                int ind;

                String[] projection = {
                        FeedReaderContract.FeedEntry.COLUMN_REF
                };

                // Filter results WHERE "title" = 'My Title'
                String selection = FeedReaderContract.FeedEntry.COLUMN_REF + " = ?";
                Cursor cursor;

                ContentValues values = new ContentValues();

                for (int i = 1; i < peliculasHTML.length; i++) {
                    l = peliculasHTML[i].substring(0, peliculasHTML[i].indexOf("\""));

                    String[] selectionArgs = {l};

                    cursor = db[0].query(
                            FeedReaderContract.FeedEntry.TABLE_NAME,                     // The table to query
                            projection,                               // The columns to return
                            selection,                                // The columns for the WHERE clause
                            selectionArgs,                            // The values for the WHERE clause
                            null,                                     // don't group the rows
                            null,                                     // don't filter by row groups
                            null                                    // The sort order
                    );

                    //Si la película no está guardada, se añade
                    //TODO que la edite con nueva info, sin borrar si se ha guardado
                    if (cursor.getCount() == 0) {

                        ind = peliculasHTML[i].indexOf("src=\"");
                        p = peliculasHTML[i].substring(ind + 5, peliculasHTML[i].indexOf("\"", ind + 5));
                        ind = peliculasHTML[i].indexOf("mc-title ft\">");
                        t = peliculasHTML[i].substring(ind + 13, peliculasHTML[i].indexOf("   ", ind + 13));
                        ind = peliculasHTML[i].indexOf("synop-text\">");
                        s = peliculasHTML[i].substring(ind + 12, peliculasHTML[i].indexOf("</li>", ind + 12));
                        ind = peliculasHTML[i].indexOf("date\">");
                        e = peliculasHTML[i].substring(ind + 6, peliculasHTML[i].indexOf("</span>", ind + 6));

                        values.put(FeedReaderContract.FeedEntry.COLUMN_REF, l);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_TITULO, t);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_PORTADA, p);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_SINOPSIS, s);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_HYPE, false);

                        int ind2 = e.indexOf(", ");
                        String ee = "";
                        String fecha_dia = "";
                        String fecha_mes = "";
                        String fecha_ano = "";
                        if (ind2 > 0) {
                            ee = e.substring(e.indexOf(", ") + 2);
                            fecha_dia = ee.substring(0, ee.indexOf(" "));
                            fecha_mes = ee.substring(ee.indexOf("de ") + 3);

                            if (fecha_dia.length() == 1)
                                fecha_dia = '0' + fecha_dia;

                            int m = 0;

                            //Pasa el mes a número
                            while (!fecha_mes.matches(meses[m++])) ;

                            fecha_mes = "" + m;
                            if (fecha_mes.length() == 1)
                                fecha_mes = '0' + fecha_mes;

                            f = "" + Calendar.getInstance().get(Calendar.YEAR) + '/' + fecha_mes + '/' + fecha_dia;
                        } else {
                            fecha_dia = e.split("/")[0];
                            fecha_mes = e.split("/")[1];
                            fecha_ano = e.split("/")[2];

                            e = fecha_dia + " de " + meses[Integer.parseInt(fecha_mes) - 1];

                            if (fecha_dia.length() == 1)
                                fecha_dia = '0' + fecha_dia;

                            if (fecha_mes.length() == 1)
                                fecha_mes = '0' + fecha_mes;

                            f = "" + fecha_ano + '/' + fecha_mes + '/' + fecha_dia;

                        }

                        fc = fecha_dia + " " + meses_corto[Integer.parseInt(fecha_mes)-1];

                        values.put(FeedReaderContract.FeedEntry.COLUMN_CORTO, fc);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_ESTRENO, e);
                        values.put(FeedReaderContract.FeedEntry.COLUMN_FECHA, f);


                        db[1].insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                        peliculas.add(new Pelicula(l, p, t, s, e, f, fc, false));
                        values.clear();

                    }

                    cursor.close();
                }

            }
            if (pagina<10)
                publishProgress(pagina);
            pagina++;
        }
        return peliculas;
    }


    @Override
    protected void onPostExecute(ArrayList<Pelicula> peliculas) {
        Log.d(TAG, "onPostExecute");
        lista.add(peliculas);
        lista.notifyDataSetChanged();
        lista.actualizarBBDD();
        carga_mensaje.setText("Actualizando...");
        carga_mensaje.setVisibility(View.GONE);
        carga_barra.setVisibility(View.GONE);

    }

    @NonNull
    private static String getHTML(String url) throws IOException {
        Log.d(TAG, "getHTML");
        // Build and set timeout values for the request.
        URLConnection connection = (new URL(url)).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.connect();

        // Read and store the result line by line then return the entire string.
        InputStream in = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder html = new StringBuilder();
        for (String line; (line = reader.readLine()) != null; ) {
            html.append(line);
        }
        in.close();

        return html.toString();
    }

    @Override
    protected void onProgressUpdate(Integer... i) {
        //TODO código del onProgressUpdate (HiloDescargarEstrenos Principal)
        carga_barra.getChildAt(i[0]-1).setBackgroundColor(Color.GREEN);
    }

    @Override
    protected void onPreExecute (){
        for(int i = 0; i < 10; i++)
            carga_barra.getChildAt(i).setBackgroundColor(Color.GRAY);
        carga_barra.setVisibility(View.VISIBLE);
        carga_mensaje.setVisibility(View.VISIBLE);
    }
}
