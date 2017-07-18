package molina.alfonso.hype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    private String estreno_letras;
    private String estreno_fecha;
    private String estreno_corto;
    private String enlace;
    private boolean isPressed;

    /*
     * Constructor
     */

    Pelicula (String l, String p, String t, String s, String e, String f, String fc,Boolean h){
        Log.d(TAG, "Pelicula");
        this.titulo = t;
        this.sinopsis = s;
        this.estreno_letras = e;
        this.enlace = l;
        this.estreno_fecha = f;
        this.estreno_corto = fc;
        this.isPressed = h;

        try {
            URL url = new URL(p);
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            this.portada = Bitmap.createScaledBitmap(bmp, 50, 80, false);

        }catch (Exception ee){

        }
    }

    /*
     * Getters y setters
     */

    public String getTitulo() {
        Log.d(TAG, "getTitulo");
        return titulo;
    }

    public Bitmap getPortada() {
        Log.d(TAG, "getPortada");
        return portada;
    }

    public String getSinopsis() {
        Log.d(TAG, "getSinopsis");
        return sinopsis;
    }

    public String getEstreno() {
        Log.d(TAG, "getEstreno");
        return estreno_letras;
    }

    public String getEnlace() {
        Log.d(TAG, "getEnlace");
        return enlace;
    }

    public String getFecha_estreno() {
        Log.d(TAG, "getFecha_estreno");
        return estreno_fecha;
    }

    public String getEstreno_corto() {
        Log.d(TAG, "getEstreno_corto");
        return estreno_corto;
    }
    
    public void setisPressed(boolean b) {
        Log.d(TAG, "setisPressed");
        this.isPressed = b;
    }

    public boolean getisPressed() {
        Log.d(TAG, "getisPressed");
        return isPressed;
    }
}

