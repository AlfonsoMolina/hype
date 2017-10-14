package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Vicente on 12/10/2017.
 */

public class HiloDescargasTMDB extends AsyncTask<SQLiteDatabase,Integer,Void> {

    private static String TAG = "HiloDescargasTMDB";
    private static String preImagen = "https://image.tmdb.org/t/p/w640";
    private static String preLink = "https://www.themoviedb.org/movie/";
    private static final String apiKey = "8ac0d37839748f4647039ef00d859d13";

    private String idioma = "es-ES";   //Idioma de sinopsis y título. (Pendiente)
    private String pais = "ES";     //Pais del que mirar los estrenos. (Pendiente)

    private SharedPreferences sharedPref;
    private boolean actFuerte;      //Si es True, se borra la bbdd y se cargan de nuevo.

    //Lista que guardará las películas
    private RecyclerViewAdapter lista;

    //Elementos del layout para la barra de carga
    private LinearLayout carga_barra;

    private static final int INDEX_CARTELERA = 0;
    private static final int INDEX_ESTRENOS = 1;

    ArrayList <Pelicula> cartelera = new ArrayList<>();
    ArrayList <Pelicula> estrenos = new ArrayList<>();

    HiloDescargasTMDB(Context context, RecyclerViewAdapter lista, LinearLayout carga_barra, boolean act) {
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
            idioma = "en-US";

        // tiene que estar en uppercase!
        pais = pais.toUpperCase();

        if(actFuerte) {
            lista.eliminarLista();
            lista.mostrarNoPelis();
        }

    }
    @Override
    protected Void doInBackground(SQLiteDatabase... db) {

        // Descargando!
        cartelera = getPeliculas(INDEX_CARTELERA);
        estrenos = getPeliculas(INDEX_ESTRENOS);

        cartelera = ordenaPelis(cartelera, true);      // true para descendente
        //estrenos = ordenaPelis(estrenos, false);      // No hace falta, ya se obtienen ordenadas

        publishProgress(0);
        int totalProgress = cartelera.size() + estrenos.size();
        int actualProgress = 0;

        Cursor cursor;
        ContentValues values = new ContentValues();

        ArrayList<Pelicula> peliculasAtratar;

        boolean estado = true;

        do {

            if (estado) {
                peliculasAtratar = cartelera;
                Log.d(TAG, "Nº de películas de cartelera: " + cartelera.size());
            } else {
                peliculasAtratar = estrenos;
                Log.d(TAG, "Nº de películas de estrenos: " + estrenos.size());
            }

            //Se prepara esto, para después usarlo para leer la bbdd y
            //ver si una película ya se ha guardado o es nueva

            String s_temp = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_REF :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_REF;
            String s_temp2 = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE;
            String s_temp3 = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO;
            String s_temp4 = estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA :
                    FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA;

            String[] projection = {
                    s_temp, s_temp2, s_temp3, s_temp4
            };
            String selection = s_temp + " = ?";

            String l;
            String t;
            String s;
            String e;
            String f;
            byte[] p_byte;
            Bitmap p_bitmap;

            for (Pelicula peli:peliculasAtratar) {

                //Se utiliza en link para ver si ya está
                l = peli.getEnlace();

                String[] selectionArgs = {l};

                cursor = db[0].query(
                        estado ? FeedReaderContract.FeedEntryCartelera.TABLE_NAME : FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                        projection,                               // The columns to return
                        selection,                                // The columns for the WHERE clause
                        selectionArgs,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        estado ? FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA + " DESC" : FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA + " ASC"                                      // The sort order
                );

                //Si la película no está guardada (o si es una actualización fuerte), se añade
                if (cursor.getCount() == 0 || actFuerte) {
                    //Se buscan y guardan los diferentes elementos

                    t = peli.getTitulo();
                    p_byte = bitmapToByte(peli.getPortada());
                    s = peli.getSinopsis();
                    e = peli.getEstrenoLetras();
                    f = peli.getEstrenoFecha();
                    p_bitmap = peli.getPortada();

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
                        if (actFuerte && cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE, 1);
                            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE));
                            values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, h);

                            String selection2 = FeedReaderContract.FeedEntryCartelera.COLUMN_REF + " LIKE ?";
                            String[] selectionArgs2 = {l};
                            db[0].delete(FeedReaderContract.FeedEntryCartelera.TABLE_NAME, selection2, selectionArgs2);

                        } else if (actFuerte) {
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
                        if (actFuerte && cursor.getCount() > 0) {
                            cursor.moveToNext();
                            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE));
                            values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, h);

                            String selection2 = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + " LIKE ?";
                            String[] selectionArgs2 = {l};
                            db[0].delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, selection2, selectionArgs2);

                        } else {
                            values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, false);
                        }

                        //Y se insertan en la bbdd y en la mListaModificadaAdapter de películas de la mListaModificadaAdapter
                        db[1].insert(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, values);
                        lista.addEstrenos(new Pelicula(l, p_bitmap, t, s, e, f, h.equals("T")));

                    }
                    values.clear();

                    Log.d(TAG, "Encontrada película: " + t + ".");
                    actualProgress++;


                }
                publishProgress((int) Math.floor(actualProgress*10/totalProgress));
                cursor.close();
            }

            //Para la actualización fuerte de la cartelera hay que borrar a las antiguas. Para eso:
            //He marcado con SIGUE a las que he encontrado
            //Ahora borro las no marcadas
            //Cambio los valores SIGUE por 0
            if(actFuerte && estado){
                String selection2 = FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE + " LIKE ?";
                String[] selectionArgs2 = {"0"};
                if (!isCancelled())
                    db[0].delete(FeedReaderContract.FeedEntryCartelera.TABLE_NAME, selection2, selectionArgs2);

                //Y esto pone un 0 donde antes había un 1
                ContentValues values2 = new ContentValues();
                values2.put(FeedReaderContract.FeedEntryCartelera.COLUMN_SIGUE, 0);

                String[] selectionArgs3 = { "1" };

                int count = db[1].update(
                        FeedReaderContract.FeedEntryCartelera.TABLE_NAME,
                        values2,
                        selection2,
                        selectionArgs3);

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

    private ArrayList<Pelicula> getPeliculas (int TIPO){

        ArrayList<Pelicula> peliculas = new ArrayList<>();
        String urlJson;
        String tipo_texto;

        switch (TIPO){
            case INDEX_CARTELERA:
                urlJson = "https://api.themoviedb.org/3/movie/now_playing?api_key="+apiKey+"&sort_by=primary_release_date.desc";
                break;
            case INDEX_ESTRENOS:
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Date tomorrow = calendar.getTime();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String tomorrowAsString = dateFormat.format(tomorrow);

                urlJson = "https://api.themoviedb.org/3/discover/movie?api_key="+apiKey+"&sort_by=primary_release_date.asc&primary_release_date.gte="+tomorrowAsString;
                break;
            default:
                return null;
        }

        try {
            int numpages;
            JSONObject jObject;
            JSONArray pageresults;

            int page = 1;
            boolean ended;

            String texto;
            int id;
            String link;
            String title;
            String poster;
            String sinopsis;
            String date;
            String textdate;

            do {
                Log.d(TAG, urlJson);
                texto = getHTML(urlJson + "&language=" + idioma + "&page=" + page + "&region=" + pais);

                jObject = new JSONObject(texto);
                numpages = jObject.getInt("total_pages");
                pageresults = jObject.getJSONArray("results");
                ended = false;

                for (int results = 0;!ended;results++){
                    try {
                        Log.d(TAG, "-----------------");
                        id = pageresults.getJSONObject(results).getInt("id");
                        Log.d(TAG, "ID: " + id);
                        link = preLink + pageresults.getJSONObject(results).getString("id");
                        Log.d(TAG, "LINK: " + link);
                        title = pageresults.getJSONObject(results).getString("title");
                        Log.d(TAG, "Título: " + title);
                        poster = preImagen + pageresults.getJSONObject(results).getString("poster_path");
                        Log.d(TAG, "Portada: " + poster);
                        sinopsis = pageresults.getJSONObject(results).getString("overview");
                        Log.d(TAG, "Sinopsis: " + sinopsis);
                        date = pageresults.getJSONObject(results).getString("release_date");
                        Log.d(TAG, "Fecha: " + date);
                        textdate = getDateText(date);
                        Log.d(TAG, "Fecha en texto: " + textdate);

                        if (TIPO == INDEX_ESTRENOS || esCartelera(date)){
                            peliculas.add(new Pelicula(id, link, poster, title, sinopsis, textdate, date, false));
                        }else{
                            Log.d(TAG, "Saltando película supuesta cartelera que aún no está en cartelera");
                        }

                    }catch (Exception e){
                        ended = true;
                    }
                }
                page++;
            } while (page <= numpages);
        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        return peliculas;
    }

    private String getDateText (String date){
        String dateText;

        String[] meses_es = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};

        String[] meses_en = {"january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december"};

        String [] splitted = date.split("-");

        dateText = splitted[2] + " de " + meses_es[Integer.parseInt(splitted[1])-1] + " del " + splitted[0];

        return dateText;
    }

    private byte [] bitmapToByte (Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        return byteArray;
    }

    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "Descarga finalizada, actualizando interfaz");
        //lista.notifyDataSetChanged();

        lista.actualizarInterfaz();
        lista.quitarX(); //Y esto de chanchullo para quitar el X
        carga_barra.setVisibility(View.GONE);
    }

    @Override
    protected void onCancelled(Void v){
        Log.d(TAG, "Descarga cancelada, actualizando interfaz");
        //lista.notifyDataSetChanged();
        lista.actualizarInterfaz();
        lista.quitarX(); //Y esto de chanchullo para quitar el X
        lista.actualizarDatos();
        carga_barra.setVisibility(View.GONE);
    }

    private boolean esCartelera(String fecha){
        String [] f = fecha.split("-");

        int difAno = Integer.parseInt(f[0]) - Calendar.getInstance().get(Calendar.YEAR);
        int difMes = Integer.parseInt(f[1]) - Calendar.getInstance().get(Calendar.MONTH) - 1;
        int difDia = Integer.parseInt(f[2]) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        boolean futuro = false;

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
        return (!futuro);
    }

    @Override
    protected void onProgressUpdate(Integer... i) {
        Log.d(TAG, "Descarga al " + ((i[0]+1)*10) + "%");
        if (i[0] == -2)
            lista.mostrarNoPelis();
        else if (i[0] == 0){
            for(int j = 0; j < 10; j++)
                carga_barra.getChildAt(j).setBackgroundColor(Color.parseColor("#455a64"));
            carga_barra.setVisibility(View.VISIBLE);
            carga_barra.getChildAt(0).setBackgroundColor(Color.parseColor("#37474f"));
            lista.actualizarInterfaz();
        } else if(i[0]<10) {
            carga_barra.getChildAt(i[0]).setBackgroundColor(Color.parseColor("#37474f"));
            //lista.notifyDataSetChanged();
            lista.actualizarInterfaz();

        }
    }

    private ArrayList<Pelicula> ordenaPelis(ArrayList<Pelicula> pelis, boolean descendente){
        ArrayList<Pelicula> pelisOrdenadas = pelis;
        if(!descendente){
            Collections.sort(pelisOrdenadas, new Comparator<Pelicula>() {
                @Override
                public int compare(Pelicula p1, Pelicula p2) {
                    return (p1.getEstrenoFecha()).compareToIgnoreCase(p2.getEstrenoFecha());
                }
            });
        }else{
            Collections.sort(pelisOrdenadas, new Comparator<Pelicula>() {
                @Override
                public int compare(Pelicula p1, Pelicula p2) {
                    return (p2.getEstrenoFecha()).compareToIgnoreCase(p1.getEstrenoFecha());
                }
            });
        }
        return pelisOrdenadas;
    }

}
