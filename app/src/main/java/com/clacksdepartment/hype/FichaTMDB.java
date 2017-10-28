package com.clacksdepartment.hype;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Process;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;

public class FichaTMDB extends AsyncTask<Void,Integer,Void> {

    private static final String TAG = "FichaTMDB";
    private static final String apiKey = "8ac0d37839748f4647039ef00d859d13";
    private static final String preImagen = "https://image.tmdb.org/t/p/w640";
    private static final String preYoutube = "https://www.youtube.com/watch?v=/";

    // Valor de progreso resultante:
    private static final int progreso_POSTER = 0;
    private static final int progreso_ANO = 2;
    private static final int progreso_DURACION = 3;
    private static final int progreso_DIRECTORES = 4;
    private static final int progreso_REPARTO = 5;
    private static final int progreso_GENERO = 6;
    private static final int progreso_NOTA = 7;

    // Atributos
    private String id;
    private String ano;
    private String duracion;
    private Drawable portada;
    private List<String> director;
    private List <String> reparto;
    private List <String> genero;
    private String nota;
    private String votos;
    private String videoProvider;
    private String videoLink;

    // Vista a modificar
    private final View mView;

    FichaTMDB(String url, View view){
        String [] urlSplit = url.split("/");
        this.id = urlSplit[urlSplit.length-1];
        mView = view;

        director = new ArrayList<>();
        reparto = new ArrayList<>();
        genero = new ArrayList<>();

    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {

            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND + THREAD_PRIORITY_MORE_FAVORABLE);

            // Leo el HTML
            String basico = getHTML("https://api.themoviedb.org/3/movie/"+id+"?api_key="+apiKey+"&language=es-ES&append_to_response=videos,credits");

            // Aquí el parseo:
            JSONObject jObject;
            JSONArray jArray;

            jObject = new JSONObject(basico);

            String portadaUrl = preImagen + jObject.getString("poster_path");
            Log.d(TAG, portadaUrl);
            portada = loadImageFromURL(portadaUrl, portadaUrl);

            publishProgress(progreso_POSTER);

            duracion = jObject.getString("runtime") + " min";
            Log.d(TAG, duracion);

            publishProgress(progreso_DURACION);

            ano = jObject.getString("release_date").split("-")[0];
            Log.d(TAG, ano);

            publishProgress(progreso_ANO);

            nota = jObject.getString("vote_average");
            Log.d(TAG, nota);
            votos = jObject.getString("vote_count");
            Log.d(TAG, votos);

            publishProgress(progreso_NOTA);

            jArray = jObject.getJSONArray("genres");

            boolean mustContinue = true;
            int cont = 0;

            while (mustContinue){
                try{
                    genero.add(jArray.getJSONObject(cont).getString("name"));
                    cont++;
                }catch (Exception e){
                    mustContinue = false;
                }
            }

            publishProgress(progreso_GENERO);

            jArray = jObject.getJSONObject("credits").getJSONArray("cast");

            mustContinue = true;
            cont = 0;

            while (mustContinue){
                try{
                    reparto.add(jArray.getJSONObject(cont).getString("name"));
                    cont++;
                }catch (Exception e){
                    mustContinue = false;
                }
            }

            publishProgress(progreso_REPARTO);

            jArray = jObject.getJSONObject("credits").getJSONArray("crew");

            mustContinue = true;
            cont = 0;

            while (mustContinue){
                try{
                    if (jArray.getJSONObject(cont).getString("job").equalsIgnoreCase("director")) {
                        director.add(jArray.getJSONObject(cont).getString("name"));
                        Log.d(TAG, "Director encontrado: " + director.get(director.size()-1));
                    }
                    cont++;
                }catch (Exception e){
                    mustContinue = false;
                }
            }
            publishProgress(progreso_DIRECTORES);

            jArray = jObject.getJSONObject("videos").getJSONArray("results");
            videoProvider = jArray.getJSONObject(0).getString("site");
            if (videoProvider.equalsIgnoreCase("youtube")) {
                videoLink = preYoutube + jArray.getJSONObject(0).getString("key");
            }
            Log.d(TAG, videoLink);

        }catch (Exception e){
            Log.e(TAG, e.toString());
        }

        return null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        // se supone que la ficha ya está completa!
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        switch (values[0]){
            case progreso_POSTER:
                if (portada != null)
                    ((ImageView) mView.findViewById(R.id.ficha_poster)).setImageDrawable(portada);
                break;
            case progreso_ANO:
                if (!ano.equalsIgnoreCase("null"))
                    ((TextView) mView.findViewById(R.id.ficha_year)).setText(ano);
                else
                    ((TextView) mView.findViewById(R.id.ficha_year)).setText("N/A");
                break;
            case progreso_DURACION:
                if (!duracion.equalsIgnoreCase("0 min") && !duracion.equalsIgnoreCase("null min"))
                    ((TextView) mView.findViewById(R.id.ficha_duracion)).setText(duracion);
                else
                    ((TextView) mView.findViewById(R.id.ficha_duracion)).setText("N/A");
                break;
            case progreso_DIRECTORES:
                ((TextView) mView.findViewById(R.id.ficha_directores)).setText(director.toString().replace("[", "").replace("]", ""));
                break;
            case progreso_REPARTO:
                ((TextView) mView.findViewById(R.id.ficha_reparto)).setText(reparto.toString().replace("[", "").replace("]", ""));
                break;
            case progreso_GENERO:
                ((TextView) mView.findViewById(R.id.ficha_genero)).setText(genero.toString().replace("[", "").replace("]", ""));
                break;
            case progreso_NOTA:
                if (nota != null && !votos.equalsIgnoreCase("0")) {
                    ((TextView) mView.findViewById(R.id.ficha_nota)).setText(mView.getResources().getString(R.string.votes_structure, nota, votos));
                }else {
                    ((TextView) mView.findViewById(R.id.ficha_nota)).setText("N/A");
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @NonNull
    private String getHTML(String url) throws IOException {
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

    private Drawable loadImageFromURL(String url, String name) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(is, name);
        } catch (Exception e) {
            return null;
        }
    }


}
