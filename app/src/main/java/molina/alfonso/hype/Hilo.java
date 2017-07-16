package molina.alfonso.hype;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Usuario on 11/07/2017.
 */

public class Hilo extends AsyncTask<SQLiteDatabase, String, ArrayList<Pelicula>> {

    private ListaModificadaAdapter lista;

    public Hilo(ListaModificadaAdapter lista) {
        this.lista = lista;
    }

    @Override
    protected ArrayList<Pelicula> doInBackground(SQLiteDatabase... db) {
        //db[0] para leer db[1] para escribir
        String html = "";
        ArrayList<Pelicula> peliculas = new ArrayList<>();
        String[] meses = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

        try {
            html = getHTML("https://m.filmaffinity.com/es/rdcat.php?id=upc_th_es");
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

                        int m = 0;

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

                    values.put(FeedReaderContract.FeedEntry.COLUMN_ESTRENO, e);
                    values.put(FeedReaderContract.FeedEntry.COLUMN_FECHA, f);

                    db[1].insert(FeedReaderContract.FeedEntry.TABLE_NAME, null, values);
                    peliculas.add(new Pelicula(l,p,t,s,e,f,false));
                    values.clear();

                }

                cursor.close();
            }

        }
        return peliculas;
    }


    @Override
    protected void onPostExecute(ArrayList<Pelicula> peliculas) {
        lista.add(peliculas);
        lista.notifyDataSetChanged();
        // log.setText(logtext);

    }

    @NonNull
    private static String getHTML(String url) throws IOException {
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
}
