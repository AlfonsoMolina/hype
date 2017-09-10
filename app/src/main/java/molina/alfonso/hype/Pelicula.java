package molina.alfonso.hype;

import android.graphics.Bitmap;
import android.util.Log;

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

    //link, portada, titulo, sinopsis, estreno (con letras), fecha (exacto), hay hype?
    Pelicula (String l, Bitmap p, String t, String s, String e, String f, Boolean h){
        Log.v(TAG, "Objeto Pelicula construido");
        this.titulo = t;
        this.sinopsis = s;
        this.estreno_letras = e;
        this.enlace = l;
        this.estreno_fecha = f;
        this.isHyped = h;
        this.portada = p;

        //Descarga el bitmap de la portada cada vez que se coge la película de la bbdd.
        //Si no hay, se pone negro.

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

