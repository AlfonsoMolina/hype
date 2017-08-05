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
    private static final String regex_POSTER = "<a id=\"main-poster\" href=\"#\">\\p{Blank}*?<img itemprop=\"image\" src=\"(\\p{ASCII}*?)\"";
    private static final String regex_SINOPSIS = "<dd itemprop=\"description\">([\\p{Alnum}\\p{Punct}\\p{Blank}]*?)<";
    private static final String regex_ANO = "itemprop=\"datePublished\">(\\p{Digit}*?)<";
    private static final String regex_DURACION = "itemprop=\"duration\">([\\p{Alnum}\\p{Punct}\\s]*?)<";
    private static final String regex_DIRECTORES = "itemprop=\"director\" itemscope itemtype=\"http://schema.org/Person\".*?itemprop=\"name\">([\\p{Alnum}\\p{Punct}\\s]*?)<";
    private static final String regex_REPARTO = "itemprop=\"actor\" itemscope itemtype=\"http://schema.org/Person\".*?itemprop=\"name\">([\\p{Alnum}\\p{Punct}\\s]*?)<";
    private static final String regex_GENERO = "itemprop=\"genre\">([\\p{Alnum}\\s\\p{Punct}]*?)<";

    // Grupo a almacenar de cada regex
    private static final int grupo_POSTER = 1;
    private static final int grupo_SINOPSIS = 1;
    private static final int grupo_ANO = 1;
    private static final int grupo_DURACION = 1;
    private static final int grupo_DIRECTORES = 1;
    private static final int grupo_REPARTO = 1;
    private static final int grupo_GENERO = 1;

    // Valor de progreso resultante:
    private static final int progreso_POSTER = 0;
    private static final int progreso_SINOPSIS = 1;
    private static final int progreso_ANO = 2;
    private static final int progreso_DURACION = 3;
    private static final int progreso_DIRECTORES = 4;
    private static final int progreso_REPARTO = 5;
    private static final int progreso_GENERO = 6;

    // Patterns y matchers:
    private ArrayList <Pattern> vPattern;

    Ficha (String url, View view){
        this.UrlFA = url;
        mView = view;

        director = new ArrayList<>();
        reparto = new ArrayList<>();
        productora = new ArrayList<>();
        genero = new ArrayList<>();

        // Precompilado de patterns:

        vPattern = new ArrayList<>();

        vPattern.add(Pattern.compile(regex_POSTER));
        vPattern.add(Pattern.compile(regex_SINOPSIS));
        vPattern.add(Pattern.compile(regex_ANO));
        vPattern.add(Pattern.compile(regex_DURACION));
        vPattern.add(Pattern.compile(regex_DIRECTORES));
        vPattern.add(Pattern.compile(regex_REPARTO));
        vPattern.add(Pattern.compile(regex_GENERO));

    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            // Setup inicial
            Matcher matcher;

            // Leo el HTML
            String contenido = getHTML(UrlFA);

            // Aquí el parseo:

            for (int i=0; i<vPattern.size(); i++){

                matcher = vPattern.get(i).matcher(contenido);

                switch (i){
                    case progreso_POSTER:
                        while (matcher.find()) {
                            portadaUrl = matcher.group(grupo_POSTER);
                            Log.d(TAG, matcher.group(grupo_POSTER));
                            try {
                                URL url = new URL(portadaUrl);
                                Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                                this.portada = Bitmap.createScaledBitmap(bmp, 654, 868, false);
                            }catch (Exception ee) {
                                Bitmap bmp = Bitmap.createBitmap(654, 868, Bitmap.Config.ARGB_8888);
                                bmp.eraseColor(Color.BLACK);
                                this.portada = bmp;
                            }

                        }
                        break;
                    case progreso_SINOPSIS:
                        sinopsis = "No disponible";
                        while (matcher.find()) {
                            sinopsis = matcher.group(grupo_SINOPSIS);
                            Log.d(TAG, matcher.group(grupo_SINOPSIS));
                        }
                        break;
                    case progreso_ANO:
                        ano = "No disponible";
                        while (matcher.find()) {
                            ano = matcher.group(grupo_ANO);
                            Log.d(TAG, matcher.group(grupo_ANO));
                        }
                        break;
                    case progreso_DURACION:
                        duracion = "No disponible";
                        while (matcher.find()) {
                            duracion = matcher.group(grupo_DURACION);
                            Log.d(TAG, matcher.group(grupo_DURACION));
                        }
                        break;
                    case progreso_DIRECTORES:
                        while (matcher.find()) {
                            director.add(matcher.group(grupo_DIRECTORES));
                        }
                        Log.d(TAG, director.toString());
                        break;
                    case progreso_REPARTO:
                        while (matcher.find()) {
                            reparto.add(matcher.group(grupo_REPARTO));
                        }
                        Log.d(TAG, reparto.toString());
                        break;
                    case progreso_GENERO:
                        while (matcher.find()) {
                            genero.add(matcher.group(grupo_GENERO));
                        }
                        Log.d(TAG, genero.toString());
                        break;
                    default:
                        break;
                }
                publishProgress(i);
            }
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
                ((TextView) mView.findViewById(R.id.ficha_sinopsis)).setText(sinopsis.replace("(FILMAFFINITY)","").replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "\'").replace("&lt;","<").replace("&gt;",">").replace("&nbsp;", " "));
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
