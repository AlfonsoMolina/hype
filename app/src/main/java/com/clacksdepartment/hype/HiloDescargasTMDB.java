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

import static com.clacksdepartment.hype.FeedReaderContract.FeedEntryEstrenos;

class HiloDescargasTMDB extends AsyncTask<SQLiteDatabase,Integer,Void> {

    private static final String TAG = "HiloDescargasTMDB";
    private static final String preImagen = "https://image.tmdb.org/t/p/w160";
    private static final String preLink = "https://www.themoviedb.org/movie/";
    private static final String apiKey = "8ac0d37839748f4647039ef00d859d13";

    private String idioma = "es-ES";   //Idioma de sinopsis y título. (Pendiente)
    private String pais = "ES";     //Pais del que mirar los estrenos. (Pendiente)

    private SharedPreferences sharedPref;

    //Lista que guardará las películas
    private RecyclerViewAdapter lista;

    //Elementos del layout para la barra de carga
    private LinearLayout carga_barra;

    private static final int INDEX_CARTELERA = 1;
    private static final int INDEX_ESTRENOS = 2;

    private static final int MAX_RESULTS_PER_SECTION = 200;

    HiloDescargasTMDB(Context context, RecyclerViewAdapter lista, LinearLayout carga_barra) {
        Log.d(TAG, "Inicializando el hilo encargado de descargar contenido de Filmaffinity");
        this.lista = lista;
        this.carga_barra = carga_barra;
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    //Se muestra la barra de carga (y se pone en gris) y un mensaje.
    @Override
    protected void onPreExecute (){
        Log.d(TAG, "Actualizando UI antes de ejecutar el hilo");
        //Se coge el país elegido
        pais = sharedPref.getString("pref_pais", "");
        idioma = "es-ES";
        if (pais.equalsIgnoreCase("uk") || pais.equalsIgnoreCase("us") || pais.equalsIgnoreCase("fr"))
            idioma = "en-US";

        // tiene que estar en uppercase!
        pais = pais.toUpperCase();

    }
    @Override
    protected Void doInBackground(SQLiteDatabase... db) {

        // Descargando!
        ArrayList<Pelicula> cartelera = getPeliculas(INDEX_CARTELERA);
        ArrayList<Pelicula> estrenos = getPeliculas(INDEX_ESTRENOS);

        cartelera = ordenaPelis(cartelera, true);      // true para descendente

        publishProgress(0);

        if (cartelera == null || estrenos == null){
            return null;
        }

        int totalProgress = cartelera.size() + estrenos.size();
        int actualProgress = 0;

        Cursor cursor;
        ContentValues values = new ContentValues();

        ArrayList<Pelicula> peliculasAtratar;

        boolean estado = true; //Se hace dos veces, una con carteleray otra con estrenos

        //Cambio el tipo a -1 de las peliculas en cartelera (1) y estrenos (2)
        values.put(FeedEntryEstrenos.COLUMN_TIPO, -1);

        String clause = FeedEntryEstrenos.COLUMN_TIPO + "='1' OR " +
                FeedEntryEstrenos.COLUMN_TIPO + "='2'";
        db[1].update(FeedEntryEstrenos.TABLE_NAME, values,
                clause, null);


        String[] projection = {
                //FeedEntryEstrenos._ID,
                //FeedEntryEstrenos.COLUMN_REF,
                FeedEntryEstrenos.COLUMN_TITULO,
                //FeedEntryEstrenos.COLUMN_PORTADA,
                FeedEntryEstrenos.COLUMN_PORTADA_ENLACE,
                FeedEntryEstrenos.COLUMN_SINOPSIS,
                //FeedEntryEstrenos.COLUMN_TRAILER,
                FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS,
                FeedEntryEstrenos.COLUMN_ESTRENO_FECHA,
                FeedEntryEstrenos.COLUMN_HYPE,
                FeedEntryEstrenos.COLUMN_TIPO
        };

        do {

            if (estado) {
                peliculasAtratar = cartelera;
                Log.d(TAG, "Nº de películas de cartelera: " + cartelera.size());
            } else {
                peliculasAtratar = estrenos;
                Log.d(TAG, "Nº de películas de estrenos: " + estrenos.size());
            }

            String whereClauseColumns = FeedEntryEstrenos.COLUMN_REF + " = ?";

            String enlace;
            String titulo;
            String sinopsis;
            String estreno_letras;
            String estreno_fecha;
            String portada_enlace;
            byte[] portada_byte;
            Bitmap portada_bitmap;

            for (Pelicula peli:peliculasAtratar) {

                //Se utiliza en link para ver si ya está
                enlace = peli.getEnlace();

                String[] whereClauseValues = {enlace};

                cursor = db[0].query(
                        FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                        projection,                               // The columns to return
                        whereClauseColumns,                                // The columns for the WHERE clause
                        whereClauseValues,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        FeedEntryEstrenos.COLUMN_ESTRENO_FECHA + (estado ? " DESC" : " ASC")                                      // The sort order
                );

                //Si la película no está guardada se añade
                if (cursor.getCount() == 0) {
                    //Se buscan y guardan los diferentes elementos

                    titulo = peli.getTitulo();
                    portada_enlace = peli.getPortadaEnlace();
                    sinopsis = peli.getSinopsis();
                    estreno_letras = peli.getEstrenoLetras();
                    estreno_fecha = peli.getEstrenoFecha();
                    portada_bitmap = peli.getPortada();
                    portada_byte = bitmapToByte(portada_bitmap);


                    values.put(FeedEntryEstrenos.COLUMN_REF, enlace);
                    values.put(FeedEntryEstrenos.COLUMN_TITULO, titulo);
                    values.put(FeedEntryEstrenos.COLUMN_PORTADA, portada_byte);
                    values.put(FeedEntryEstrenos.COLUMN_PORTADA_ENLACE, portada_enlace);
                    values.put(FeedEntryEstrenos.COLUMN_SINOPSIS, sinopsis);
                    values.put(FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS, estreno_letras);
                    values.put(FeedEntryEstrenos.COLUMN_ESTRENO_FECHA, estreno_fecha);
                    values.put(FeedEntryEstrenos.COLUMN_TIPO, estado?1:2);

                    db[1].insert(FeedEntryEstrenos.TABLE_NAME, null, values);

/*
                    if (estado) {
                        //Si es una act fuerte, primero se borra
                        if (actFuerte && cursor.getCount() > 0) {
                            cursor.moveToFirst();
                            values.put(column_sigue, 1);
                            hype = cursor.getString(cursor.getColumnIndexOrThrow(column_hype));
                            values.put(column_hype, hype);

                            String whereClauseColumns2 = column_ref + " LIKE ?";
                            String[] whereClauseValues_2 = {link};
                            db[0].delete(table_name, whereClauseColumns2, whereClauseValues_2);

                        } else if (actFuerte) {
                            values.put(column_hype, false);
                            values.put(column_sigue, 1);
                        } else {
                            values.put(column_hype, false);
                            values.put(column_sigue, 0);
                        }

                        //Y se insertan en la bbdd y en la mListaModificadaAdapter de películas de la mListaModificadaAdapter
                        db[1].insert(table_name, null, values);
                        lista.addCartelera(new Pelicula(link, p_bitmap, title, sinopsis, estreno_letras, estreno_fecha, hype.equals("T")));

                    } else {
                        values.put(column_hype, false);

                        if (actFuerte && cursor.getCount() > 0) {
                            cursor.moveToNext();
                            hype = cursor.getString(cursor.getColumnIndexOrThrow(column_hype));
                            values.put(column_hype, hype);

                            String selection2 = column_ref + " LIKE ?";
                            String[] selectionArgs2 = {link};
                            db[0].delete(table_name, selection2, selectionArgs2);
                        } else {
                            values.put(column_hype, false);
                        }

                        //Y se insertan en la bbdd y en la mListaModificadaAdapter de películas de la mListaModificadaAdapter
                        db[1].insert(table_name, null, values);
                        lista.addEstrenos(new Pelicula(link, p_bitmap, title, sinopsis, estreno_letras, estreno_fecha, hype.equals("T")));

                    }
                    */
                    values.clear();
                    Log.d(TAG, "Encontrada película: " + titulo + ".");

                //Si ya está se modifica
                }else {
                    cursor.moveToFirst();
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryEstrenos.COLUMN_TITULO)).equalsIgnoreCase(peli.getTitulo())) {
                        values.put(FeedEntryEstrenos.COLUMN_TITULO, peli.getTitulo());
                    }
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryEstrenos.COLUMN_SINOPSIS)).equalsIgnoreCase(peli.getSinopsis())) {
                        values.put(FeedEntryEstrenos.COLUMN_SINOPSIS, peli.getSinopsis());
                    }
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryEstrenos.COLUMN_ESTRENO_FECHA)).equalsIgnoreCase(peli.getEstrenoFecha())) {
                        values.put(FeedEntryEstrenos.COLUMN_ESTRENO_FECHA, peli.getEstrenoFecha());
                        values.put(FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS, peli.getEstrenoLetras());
                    }
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryEstrenos.COLUMN_PORTADA_ENLACE)).equalsIgnoreCase(peli.getPortadaEnlace())) {
                        values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA, bitmapToByte(peli.getPortada()));
                        values.put(FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS, peli.getEstrenoLetras());
                    }
                    values.put(FeedEntryEstrenos.COLUMN_TIPO, estado?1:2);

                    Log.d(TAG, "Actualizando película " + peli.getTitulo());
                        // Si ha cambiado algo de lo de arriba, actualizo la portada, por si ha cambiado...
                        // TODO: COMPARAR HASH O ALGO?
                    db[1].update(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, values, FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + "= '" + peli.getEnlace() + "'", null);
                    values.clear();

                }

                actualProgress++;
                publishProgress((int) Math.floor(actualProgress*10/totalProgress));
                cursor.close();
            }

            //Se eliminan las películas que no hayan aparecido
            String selection2 = FeedEntryEstrenos.COLUMN_TIPO + "<'0'";

            if (!isCancelled())
                db[0].delete(FeedEntryEstrenos.TABLE_NAME, selection2, null);

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

        String staticUrl;

        JSONObject jObject;
        JSONArray pageresults;

        int page = 1;
        int numpages;

        int totalresults = 0;

        boolean hasEnded;

        String response;

        // Datos a recopilar:
        int id;
        String link;
        String title;
        String poster;
        String sinopsis;
        String date;
        String textdate;

        switch (TIPO){
            case INDEX_CARTELERA:
                staticUrl = "https://api.themoviedb.org/3/movie/now_playing?api_key="+apiKey+"&sort_by=primary_release_date.desc";
                break;
            case INDEX_ESTRENOS:
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_YEAR, 1);
                Date tomorrow = calendar.getTime();
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                String tomorrowAsString = dateFormat.format(tomorrow);
                staticUrl = "https://api.themoviedb.org/3/discover/movie?api_key="+apiKey+"&sort_by=primary_release_date.asc&primary_release_date.gte="+tomorrowAsString;
                break;
            default:
                return null;
        }

        try {

            do {
                Log.d(TAG, staticUrl);
                response = getHTML(staticUrl + "&language=" + idioma + "&page=" + page + "&region=" + pais);

                jObject = new JSONObject(response);
                numpages = jObject.getInt("total_pages");
                pageresults = jObject.getJSONArray("results");
                hasEnded = false;

                for (int results = 0;!hasEnded && (totalresults<MAX_RESULTS_PER_SECTION);results++){
                    try {
                        id = pageresults.getJSONObject(results).getInt("id");
                        link = preLink + pageresults.getJSONObject(results).getString("id");
                        title = pageresults.getJSONObject(results).getString("title");
                        poster = preImagen + pageresults.getJSONObject(results).getString("poster_path");
                        sinopsis = pageresults.getJSONObject(results).getString("overview");
                        date = pageresults.getJSONObject(results).getString("release_date");
                        textdate = getDateText(date);
                        if (TIPO == INDEX_ESTRENOS && !esCartelera(date)){
                            peliculas.add(new Pelicula(id, link, poster, title, sinopsis, textdate, date, false));
                        }else if (TIPO == INDEX_CARTELERA && esCartelera(date)){
                            peliculas.add(new Pelicula(id, link, poster, title, sinopsis, textdate, date, false));
                        }else{
                            Log.d(TAG, "Saltando película en sección incorrecta");
                        }
                        totalresults++;
                    }catch (Exception e){
                        hasEnded = true;
                    }
                }
                page++;
            } while ((page <= numpages) && (totalresults < MAX_RESULTS_PER_SECTION));
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

        if (idioma.equalsIgnoreCase("es-ES")) {
            dateText = splitted[2] + " de " + meses_es[Integer.parseInt(splitted[1]) - 1] + " del " + splitted[0];
        }else{
            dateText = splitted[2] + " " + meses_en[Integer.parseInt(splitted[1]) - 1] + " " + splitted[0];
        }
        return dateText;
    }

    private byte [] bitmapToByte (Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
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
        if(!descendente){
            Collections.sort(pelis, new Comparator<Pelicula>() {
                @Override
                public int compare(Pelicula p1, Pelicula p2) {
                    return (p1.getEstrenoFecha()).compareToIgnoreCase(p2.getEstrenoFecha());
                }
            });
        }else{
            Collections.sort(pelis, new Comparator<Pelicula>() {
                @Override
                public int compare(Pelicula p1, Pelicula p2) {
                    return (p2.getEstrenoFecha()).compareToIgnoreCase(p1.getEstrenoFecha());
                }
            });
        }
        return pelis;
    }

}
