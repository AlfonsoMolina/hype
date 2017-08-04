package molina.alfonso.hype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Vicente on 04/08/2017.
 */

public class Ficha extends AsyncTask<Void,Integer,Void> {

    private static final String TAG = "Ficha";

    // Atributos
    private String UrlFA;
    private String ano;
    private String duracion;
    private String portadaUrl;
    private Bitmap portada;
    private List <String> director;
    private List <String> reparto;
    private List <String> productora;
    private List <String> genero;
    private String sinopsis;

    // Vista a modificar
    private final View mView;

    // Regex de cada cosa a leer:
    private static final String regex_POSTER = "<a id=\"main-poster\" href=\"#\">(.*?)<img itemprop=\"image\" src=\"(.*?)\"";
    private static final String regex_SINOPSIS = "<dd itemprop=\"description\">(.*?)</dd>";
    private static final String regex_ANO = "itemprop=\"datePublished\">(.*?)<";
    private static final String regex_DURACION = "itemprop=\"duration\">(.*?)<";
    private static final String regex_DIRECTORES = "<(.*?)itemprop=\"director\"(.*?)itemprop=\"name\">(.*?)<";
    private static final String regex_REPARTO = "<(.*?)itemprop=\"actor\"(.*?)itemprop=\"name\">(.*?)<";
    private static final String regex_GENERO = "<(.*?)itemprop=\"genre\">(.*?)<";

    // Grupo a almacenar de cada regex
    private static final int grupo_POSTER = 2;
    private static final int grupo_SINOPSIS = 1;
    private static final int grupo_ANO = 1;
    private static final int grupo_DURACION = 1;
    private static final int grupo_DIRECTORES = 3;
    private static final int grupo_REPARTO = 3;
    private static final int grupo_GENERO = 2;

    // Valor de progreso resultante:
    private static final int progreso_POSTER = 0;
    private static final int progreso_SINOPSIS = 1;
    private static final int progreso_ANO = 2;
    private static final int progreso_DURACION = 3;
    private static final int progreso_DIRECTORES = 4;
    private static final int progreso_REPARTO = 5;
    private static final int progreso_GENERO = 6;

    Ficha (String url, View view){
        this.UrlFA = url;
        mView = view;

        director = new ArrayList<>();
        reparto = new ArrayList<>();
        productora = new ArrayList<>();
        genero = new ArrayList<>();

    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            // Setup inicial
            Pattern pattern;
            Matcher matcher;

            // Leo el HTML
            String contenido = getHTML(UrlFA);

            // Aquí el parseo:

            // Saco el poster
            pattern = Pattern.compile(regex_POSTER);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                portadaUrl = matcher.group(grupo_POSTER);
                Log.d(TAG, matcher.group(grupo_POSTER));

                try {
                    URL url = new URL(portadaUrl);
                    Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    this.portada = Bitmap.createScaledBitmap(bmp, 654, 868, false);
                    publishProgress(progreso_POSTER);

                }catch (Exception ee) {
                    Bitmap bmp = Bitmap.createBitmap(654, 868, Bitmap.Config.ARGB_8888);
                    bmp.eraseColor(Color.BLACK);
                    this.portada = bmp;
                }

            }

            // Saco la sinopsis
            pattern = Pattern.compile(regex_SINOPSIS);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                sinopsis = matcher.group(grupo_SINOPSIS);
                Log.d(TAG, matcher.group(grupo_SINOPSIS));
                publishProgress(progreso_SINOPSIS);
            }

            // Saco el año:
            pattern = Pattern.compile(regex_ANO);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                ano = matcher.group(grupo_ANO);
                Log.d(TAG, matcher.group(grupo_ANO));
                publishProgress(progreso_ANO);
            }

            // Saco la duracion:
            pattern = Pattern.compile(regex_DURACION);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                duracion = matcher.group(grupo_DURACION);
                Log.d(TAG, matcher.group(grupo_DURACION));
                publishProgress(progreso_DURACION);
            }

            // Saco los directores
            pattern = Pattern.compile(regex_DIRECTORES);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                director.add(matcher.group(grupo_DIRECTORES));
            }
            publishProgress(progreso_DIRECTORES);
            Log.d(TAG, director.toString());

            // Saco el reparto
            pattern = Pattern.compile(regex_REPARTO);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                reparto.add(matcher.group(grupo_REPARTO));
            }
            publishProgress(progreso_REPARTO);
            Log.d(TAG, reparto.toString());

            // Saco el genero
            pattern = Pattern.compile(regex_GENERO);
            matcher = pattern.matcher(contenido);

            while (matcher.find()) {
                genero.add(matcher.group(grupo_GENERO));
            }
            publishProgress(progreso_GENERO);
            Log.d(TAG, genero.toString());

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
                ((ImageView) mView.findViewById(R.id.ficha_poster)).setImageBitmap(portada);
                break;
            case progreso_SINOPSIS:
                ((TextView) mView.findViewById(R.id.ficha_sinopsis)).setText(sinopsis.replace("(FILMAFFINITY",""));
                break;
            case progreso_ANO:
                ((TextView) mView.findViewById(R.id.ficha_year)).setText(ano);
                break;
            case progreso_DURACION:
                ((TextView) mView.findViewById(R.id.ficha_duracion)).setText(duracion);
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

    public String getUrlFA() {
        return UrlFA;
    }

    public String getAno() {
        return ano;
    }

    public String getDuracion() {
        return duracion;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public List<String> getDirector() {
        return director;
    }

    public List<String> getReparto() {
        return reparto;
    }

    public List<String> getProductora() {
        return productora;
    }

    public List<String> getGenero() {
        return genero;
    }

    public String getSinopsis() {
        return sinopsis;
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
}
