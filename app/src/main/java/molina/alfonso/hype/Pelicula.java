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
    private String estrenoLetras;      //Viernes, 4 de febrero
    private String estrenoFecha;       //2018/02/04
    private String enlace;
    private boolean hype;

    /*
     * Constructor
     */

    //link, portada, titulo, sinopsis, estreno (con letras), fecha (exacto), hay hype?
    Pelicula (String enlace, Bitmap portada, String titulo, String sinopsis, String estrenoLetras, String estrenoFecha, Boolean hype){
        Log.v(TAG, "Objeto Pelicula construido");
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.estrenoLetras = estrenoLetras;
        this.enlace = enlace;
        this.estrenoFecha = estrenoFecha;
        this.hype = hype;
        this.portada = portada;

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

    public String getEstrenoLetras() {
        return estrenoLetras;
    }

    public String getEnlace() {
        return enlace;
    }

    public String getEstrenoFecha() {
        return estrenoFecha;
    }

    public void setHype(boolean b) {
        Log.d(TAG, "Marcando Hype=" + b + "en película " + titulo);
        this.hype = b;
    }

    public boolean getHype() {
        return hype;
    }

}

