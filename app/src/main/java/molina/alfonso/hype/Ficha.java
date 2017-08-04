package molina.alfonso.hype;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Vicente on 04/08/2017.
 */

public class Ficha {

    static final String TAG = "Ficha";

    private String UrlFA;
    private String titulo;
    private String ano;
    private String duracion;
    private String pais;
    private String portadaUrl;
    private String director;
    private String guion;
    private String musica;
    private String fotografia;
    private String reparto;
    private String productora;
    private String genero;
    private String grupos;
    private String sinopsis;

    private final View mView;

    Ficha (String url, View view){
        this.UrlFA = url;
        mView = view;
        parseContent();
    }

    private void parseContent (){
        AsyncTask task = new AsyncTask() {

            String sinopsis;
            String director;
            String imagen_url;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Object[] objects) {

                try {
                    String contenido = getHTML(UrlFA);
                    // Aquí el parseo

                    // Saco la sinopsis
                    Pattern pattern = Pattern.compile("<dt>Sinopsis</dt>(.*?)<dd itemprop=\"description\">(.*?)</dd>");
                    Matcher matcher = pattern.matcher(contenido);

                    while (matcher.find()) {
                        sinopsis = matcher.group(2);
                        Log.d(TAG, matcher.group(2));
                    }

                    pattern = Pattern.compile("<dt id=\"full-director\">Director</dt>(.*?)<span itemprop=\"name\">(.*?)</span>");
                    matcher = pattern.matcher(contenido);

                    while (matcher.find()) {
                        director = matcher.group(2);
                        Log.d(TAG, matcher.group(2));
                    }

                    pattern = Pattern.compile("<a id=\"main-poster\" href=\"#\">(.*?)<img itemprop=\"image\" src=\"(.*?)\"");
                    matcher = pattern.matcher(contenido);

                    while (matcher.find()) {
                        imagen_url = matcher.group(2);
                        Log.d(TAG, matcher.group(2));
                    }

                }catch (Exception e){
                    Log.e(TAG, e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                ((TextView) mView.findViewById(R.id.ficha_sinopsis)).setText(sinopsis);
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
        };

        task.execute();
    }

    public String getUrlFA() {
        return UrlFA;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAno() {
        return ano;
    }

    public String getDuracion() {
        return duracion;
    }

    public String getPais() {
        return pais;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public String getDirector() {
        return director;
    }

    public String getGuion() {
        return guion;
    }

    public String getMusica() {
        return musica;
    }

    public String getFotografia() {
        return fotografia;
    }

    public String getReparto() {
        return reparto;
    }

    public String getProductora() {
        return productora;
    }

    public String getGenero() {
        return genero;
    }

    public String getGrupos() {
        return grupos;
    }

    public String getSinopsis() {
        return sinopsis;
    }

}
