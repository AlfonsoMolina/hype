package molina.alfonso.hype;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import static molina.alfonso.hype.ListaModificadaAdapter.CARTELERA;
import static molina.alfonso.hype.ListaModificadaAdapter.ESTRENOS;

/**
 * Created by Clacks Department on 11/07/2017.
 *
 * Se conecta a FilmAffinity para descargar los próximos estrenos.
 */

// TODO: Actualizar también el resto de pelis

class HiloDescargas extends AsyncTask<SQLiteDatabase,Integer,Void> {

    /*
     * Declaración de variables
     */
    private static final String TAG = "HiloDescargarEstrenos";


    //Estoy hay que pasarlo a un array o algo
    private String[] meses_es = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

    private String[] meses_en = {"january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"};

    private String idioma = "es";   //Idioma de sinopsis y título. (Pendiente)
    private String pais = "es";     //Pais del que mirar los estrenos. (Pendiente)
    private int pagina = 1;         //Número de páginas de las 10 que se van a descargar.
    private SharedPreferences sharedPref;
    private boolean actFuerte;      //Si es True, se borra la bbdd y se cargan de nuevo.

    //Lista que guardará las películas
    private ListaModificadaAdapter lista;

    //Elementos del layout para la barra de carga
    private LinearLayout carga_barra;

    HiloDescargas(Context context, ListaModificadaAdapter lista, LinearLayout carga_barra, boolean act) {
        Log.d(TAG, "Inicializando el hilo encargado de descargar contenido de Filmaffinity");
        this.lista = lista;
        this.carga_barra = carga_barra;
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        this.actFuerte = act;
    }


     //Se muestra la barra de carga (y se pone en gris) y un mensaje.
    @Override
    protected void onPreExecute (){
        Log.d(TAG, "Actualizando UI antes de ejecutar el hilo");
        //Se coge el país elegido
        pais = sharedPref.getString("pref_pais", "");
        idioma = pais;
        if (pais.equalsIgnoreCase("uk") || pais.equalsIgnoreCase("us") || pais.equalsIgnoreCase("fr"))
            idioma = "en";

        if(actFuerte) {
            lista.eliminarLista();
            lista.noHayPelis();
        }
    }

    @Override
    protected Void doInBackground(SQLiteDatabase... db) {
        Log.d(TAG, "Comenzando descarga de estrenos");

        boolean estado = true; //Variable que utilizo para ejecutar dos veces el comando. El do primero, después cambia
                                //y se ejecuta el while, después cambia y sale.
                                //Primero hace Cartelera, despues Estrenos

        do {

            pagina = 1;
            publishProgress(0);
            //db[0] para leer db[1] para escribir
            String html;
            String dir;

            //Se prepara esto, para después usarlo para leer la bbdd y
            //ver si una película ya se ha guardado o es nueva

            String s_temp = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_REF :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_REF;
            String s_temp2 = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE;
            String s_temp3 = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO;
            String[] projection = {
                    s_temp, s_temp2, s_temp3
            };
            String selection = s_temp + " = ?";


            //Se van a descargar las 10 primeras páginas de estrenos de FILMAFFINITY
            while (pagina <= 10 && !isCancelled()) {
                try {
                    String modo = estado ? "new" : "upc";
                    dir = "https://m.filmaffinity.com/" + idioma + "/rdcat.php?id=" + modo + "_th_" + pais + "&page=" + pagina;
                    html = getHTML(dir);
                } catch (Exception ee) {
                    Log.d(TAG, "Error en la descarga de HTML.");
                    html = null;
                    this.cancel(true);
                }

                if (html != null) {

                    //Se parte el HTML en la división entre las películas
                    String[] peliculasHTML = html.split("-item\" href=\"");
                    String l;
                    String p;
                    String t;
                    String s;
                    String e;
                    String f;
                    byte[] p_byte;
                    Bitmap p_bitmap;
                    int ind;


                    Cursor cursor;

                    ContentValues values = new ContentValues();

                    //Se analizan los trozos de HTML correspondientes a cada película
                    for (int i = 1; i < peliculasHTML.length && !isCancelled(); i++) {

                        //Se utiliza en link para ver si ya está
                        l = peliculasHTML[i].substring(0, peliculasHTML[i].indexOf("\""));
                        String[] selectionArgs = {l};

                        if (estado) {

                            cursor = db[0].query(
                                    FeedReaderContract.FeedEntryCartelera.TABLE_NAME,                     // The table to query
                                    projection,                               // The columns to return
                                    selection,                                // The columns for the WHERE clause
                                    selectionArgs,                            // The values for the WHERE clause
                                    null,                                     // don't group the rows
                                    null,                                     // don't filter by row groups
                                    null                                    // The sort order
                            );
                        } else { // if (estado == ESTRENOS)
                            cursor = db[0].query(
                                    FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                                    projection,                               // The columns to return
                                    selection,                                // The columns for the WHERE clause
                                    selectionArgs,                            // The values for the WHERE clause
                                    null,                                     // don't group the rows
                                    null,                                     // don't filter by row groups
                                    null                                    // The sort order
                            );
                        }

                        //Si la película no está guardada (o si es una actualización fuerte), se añade
                        if (cursor.getCount() == 0 || actFuerte) {
                            //Se buscan y guardan los diferentes elementos
                            ind = peliculasHTML[i].indexOf("src=\"");
                            p = peliculasHTML[i].substring(ind + 5, peliculasHTML[i].indexOf("\"", ind + 5));

                            try {
                                URL url = new URL(p);
                                p_bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                p_bitmap = Bitmap.createScaledBitmap(p_bitmap, 50, 80, false);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                p_bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
                                p_byte = stream.toByteArray();

                            } catch (Exception ee) {
                                p_bitmap = Bitmap.createBitmap(50, 80, Bitmap.Config.ARGB_8888);
                                p_bitmap.eraseColor(Color.BLACK);
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                p_bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
                                p_byte = stream.toByteArray();

                                //TODO hacerlo más eficiente, directamente en stream
                            }

                            ind = peliculasHTML[i].indexOf("mc-title ft\">");
                            t = peliculasHTML[i].substring(ind + 13, peliculasHTML[i].indexOf("   ", ind + 13));
                            ind = peliculasHTML[i].indexOf("synop-text\">");

                            //La sinopsis acaba al final del campo o antes de (FILMAFFINITY) si es larga.
                            int ind2 = peliculasHTML[i].indexOf("</li>", ind + 12);
                            int ind3 = peliculasHTML[i].indexOf("(FILM", ind + 12);

                            if (ind3 > 0 && ind3 < ind2)
                                ind2 = ind3;

                            s = peliculasHTML[i].substring(ind + 12, ind2);
                            ind = peliculasHTML[i].indexOf("date\">");


                            if (ind == -1) {
                                e = "Sin confirmar";
                                f = "09/09/2099";

                            } else {
                                e = peliculasHTML[i].substring(ind + 6, peliculasHTML[i].indexOf("</span>", ind + 6));

                                //Ahora se mira la fecha para guardarla en el formato correcto.
                                ind2 = e.indexOf(", ");
                                String ee = "";
                                String fecha_dia = "";
                                String fecha_mes = "";
                                String fecha_ano = "";

                                if (ind2 > 0) {

                                    int m = 0;

                                    //Pasa el mes a número
                                    switch (idioma) {
                                        case "es":
                                        case "mx":
                                        case "ar":
                                        case "co":
                                        case "cl":
                                            ee = e.substring(e.indexOf(", ") + 2);
                                            fecha_dia = ee.substring(0, ee.indexOf(" "));
                                            fecha_mes = ee.substring(ee.indexOf("de ") + 3);

                                            if (fecha_dia.length() == 1)
                                                fecha_dia = '0' + fecha_dia;

                                            while (!fecha_mes.equalsIgnoreCase(meses_es[m++])) ;
                                            break;
                                        default:
                                            ee = e.substring(e.indexOf(", ") + 2);
                                            fecha_mes = ee.substring(0, ee.indexOf(" "));
                                            fecha_dia = ee.substring(ee.indexOf(" "));

                                            if (fecha_dia.length() == 1)
                                                fecha_dia = '0' + fecha_dia;

                                            while (!fecha_mes.equalsIgnoreCase(meses_en[m++])) ;
                                            break;
                                    }

                                    fecha_mes = "" + m;
                                    if (fecha_mes.length() == 1)
                                        fecha_mes = '0' + fecha_mes;

                                    f = "" + Calendar.getInstance().get(Calendar.YEAR) + '/' + fecha_mes + '/' + fecha_dia;

                                } else {
                                    fecha_dia = e.split("/")[0];
                                    fecha_mes = e.split("/")[1];
                                    fecha_ano = e.split("/")[2];

                                    switch (idioma) {
                                        case "es":
                                        case "mx":
                                        case "ar":
                                        case "co":
                                        case "cl":
                                            e = fecha_dia + " de " + meses_es[Integer.parseInt(fecha_mes) - 1];
                                            break;
                                        default:
                                            e = meses_en[Integer.parseInt(fecha_mes) - 1] + " " + fecha_dia;
                                            break;
                                    }

                                    if (fecha_dia.length() == 1)
                                        fecha_dia = '0' + fecha_dia;

                                    if (fecha_mes.length() == 1)
                                        fecha_mes = '0' + fecha_mes;

                                    f = "" + fecha_ano + '/' + fecha_mes + '/' + fecha_dia;

                                }

                            }
                            // Me cargo sangrías y cosas raras
                            s = s.trim();

                            if (estado) {

                                //Se añaden los valores que hemos cogido
                                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_REF, l);
                                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_TITULO, t);
                                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_PORTADA, p_byte);
                                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SINOPSIS, s);
                                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_ESTRENO, e);
                                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA, f);

                                String h = "F";
                                //Si es una act fuerte, primero se borra
                                if (actFuerte && cursor.getCount()>0){
                                    cursor.moveToFirst();
                                    values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE, 1);
                                    h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE));
                                    values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, h);

                                    String selection2 = FeedReaderContract.FeedEntryCartelera.COLUMN_REF + " LIKE ?";
                                    String[] selectionArgs2 = {l};
                                    db[0].delete(FeedReaderContract.FeedEntryCartelera.TABLE_NAME, selection2, selectionArgs2);

                                }else if (actFuerte){
                                    values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, false);
                                    values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE, 1);
                                } else {
                                    values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, false);
                                    values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE, 0);
                                }

                                //Y se insertan en la bbdd y en la mListaModificadaAdapter de películas de la mListaModificadaAdapter
                                db[1].insert(FeedReaderContract.FeedEntryCartelera.TABLE_NAME, null, values);
                                lista.addCartelera(new Pelicula(l, p_bitmap, t, s, e, f, h.equals("T")));


                            } else {
                                //Se añaden los valores que hemos cogido
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_REF, l);
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO, t);
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA, p_byte);
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS, s);
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, false);
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO, e);
                                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA, f);

                                String h = "F";
                                if (actFuerte && cursor.getCount()>0){
                                    cursor.moveToNext();
                                    h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE));
                                    values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, h);

                                    String selection2 = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + " LIKE ?";
                                    String[] selectionArgs2 = {l};
                                    db[0].delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, selection2, selectionArgs2);

                                }else{
                                    values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, false);
                                }

                                //Y se insertan en la bbdd y en la mListaModificadaAdapter de películas de la mListaModificadaAdapter
                                db[1].insert(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, values);
                                lista.addEstrenos(new Pelicula(l, p_bitmap, t, s, e, f, h.equals("T")));

                            }
                            values.clear();

                            Log.d(TAG, "Encontrada película: " + t + ".");
                        }
                        cursor.close();
                    }

                }

                //Después de leer cada página, se actualiza la interfaz con lo que llevamos
                //(Menos en la última, porque sería esto más el onPostExecution justo después,
                //y no queda bien)
                if (pagina < 10)
                    publishProgress(pagina);

                pagina++;
            }
            //Para la actualización fuerte de la cartelera hay que borrar a las antiguas. Para eso:
            //He marcado con SIGUE a las que he encontrado
            //Ahora borro las no marcadas
            //Cambio los valores SIGUE por 0
            if(actFuerte && estado){
                String selection2 = FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE + " LIKE ?";
                String[] selectionArgs2 = {"0"};
                db[0].delete(FeedReaderContract.FeedEntryCartelera.TABLE_NAME, selection2, selectionArgs2);

                //Y esto pone un 0 donde antes había un 1
                ContentValues values2 = new ContentValues();
                values2.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE, 0);

                /*String[] selectionArgs3 = { "1" };

                int count = db[1].update(
                        FeedReaderContract.FeedEntryCartelera.TABLE_NAME,
                        values2,
                        selection2,
                        selectionArgs3);*/

            }
            estado = !estado;
        }while(!estado);
        return null;
    }

    //Este método lee el HTML y lo convierte en String
    @NonNull
    private static String getHTML(String url) throws IOException {
        Log.d(TAG, "Obteniendo contenido HTML desde " + url);
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


    //En cada actualización añade un nuevo paso a la barra de progreso y
    //actualiza la interfaz con las películas hasta el momento
    @Override
    protected void onProgressUpdate(Integer... i) {
        Log.d(TAG, "Descarga al " + ((i[0]+1)*10) + "%");
        if (i[0] == 0){
            for(int j = 0; j < 9; j++)
                carga_barra.getChildAt(j).setBackgroundColor(Color.parseColor("#455a64"));
            carga_barra.setVisibility(View.VISIBLE);
        } else {
            carga_barra.getChildAt(i[0] - 1).setBackgroundColor(Color.parseColor("#37474f"));
            lista.setMaxPaginas();
            lista.notifyDataSetChanged();
            lista.actualizarInterfaz();
        }
    }

    //Le dice a la mListaModificadaAdapter que actualice las películas guardadas y esconde
    //la barra de progreso
    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "Descarga finalizada, actualizando interfaz");
        lista.notifyDataSetChanged();
        lista.setMaxPaginas();
        lista.actualizarInterfaz();
        lista.noHayPelis(); //Y esto de chanchullo para quitar el X
        carga_barra.setVisibility(View.GONE);
    }

    @Override
    protected void onCancelled(Void v){
        Log.d(TAG, "Descarga cancelada, actualizando interfaz");
        lista.notifyDataSetChanged();
        lista.setMaxPaginas();
        lista.actualizarInterfaz();
        lista.noHayPelis(); //Y esto de chanchullo para quitar el X
        carga_barra.setVisibility(View.GONE);
    }



}
