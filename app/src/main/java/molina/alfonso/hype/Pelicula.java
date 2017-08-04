package molina.alfonso.hype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.net.URL;

/**
 * Created by Clacks Department on 02/07/2017.
 */

public class Pelicula {

    /*
     * Declaración de variables
     */

    private static final String TAG = "Pelicula";

    private String titulo;
    private Bitmap portada;
    private String sinopsis;
    private String estreno_letras;      //Viernes, 4 de febrero
    private String estreno_fecha;       //2018/02/04
    private String estreno_corto;       //4 feb
    private String enlace;
    private boolean isHyped;

    /*
     * Constructor
     */

    //link, portada, titulo, sinopsis, estreno (con letras), fecha (exacto), fecha corta, hay hype?
    Pelicula (String l, String p, String t, String s, String e, String f, String fc,Boolean h){
        Log.v(TAG, "Objeto Pelicula construido");
        this.titulo = t;
        this.sinopsis = s;
        this.estreno_letras = e;
        this.enlace = l;
        this.estreno_fecha = f;
        this.estreno_corto = fc;
        this.isHyped = h;

        //Descarga el bitmap de la portada cada vez que se coge la película de la bbdd.
        //Si no hay, se pone negro.
        try {
            URL url = new URL(p);
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            this.portada = Bitmap.createScaledBitmap(bmp, 50, 80, false);

        }catch (Exception ee){
            Bitmap bmp = Bitmap.createBitmap(50, 80, Bitmap.Config.ARGB_8888);
            bmp.eraseColor(Color.BLACK);
            this.portada = bmp;
        }
    }

    /*
     * Getters y setters
     */

    public String getTitulo() {
        return titulo;
    }

    public Bitmap getPortada() {
        return portada;
    }

    public String getSinopsis() {
        return sinopsis;
    }

    public String getEstreno() {
        return estreno_letras;
    }

    public String getEnlace() {
        return enlace;
    }

    public String getFecha_estreno() {
        return estreno_fecha;
    }

    public String getEstreno_corto() {
        return estreno_corto;
    }
    
    public void setisHyped (boolean b) {
        Log.d(TAG, "Marcando Hype=" + b + "en película " + titulo);
        this.isHyped = b;
    }

    public boolean getisHyped() {
        return isHyped;
    }

}

