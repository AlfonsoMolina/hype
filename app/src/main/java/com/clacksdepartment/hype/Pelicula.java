package com.clacksdepartment.hype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
    private String estrenoLetras;      //Viernes, 4 de febrero
    private String estrenoFecha;       //2018/02/04
    private String enlace;
    private String portadaEnlace;


    private String enlacePortada;
    private boolean hype;
    private int id;

    /*
     * Constructor
     */

    //link, portada, titulo, sinopsis, estreno (con letras), fecha (exacto), hay hype?
    Pelicula (String enlace, Bitmap portada, String portadaEnlace, String titulo, String sinopsis, String estrenoLetras, String estrenoFecha, Boolean hype){
        Log.v(TAG, "Objeto Pelicula construido");
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.estrenoLetras = estrenoLetras;
        this.enlace = enlace;
        this.estrenoFecha = estrenoFecha;
        this.hype = hype;
        this.portada = portada;
        this.portadaEnlace = portadaEnlace;

    }

    Pelicula (int id, String enlace, String portada, String titulo, String sinopsis, String estrenoLetras, String estrenoFecha, Boolean hype){
        Log.v(TAG, "Objeto Pelicula construido");
        this.titulo = titulo;
        this.sinopsis = sinopsis;
        this.estrenoLetras = estrenoLetras;
        this.enlace = enlace;
        this.estrenoFecha = estrenoFecha;
        this.hype = hype;
        this.enlacePortada = portada;
        this.id = id;

        //Descarga el bitmap de la portada cada vez que se coge la película de la bbdd.
        //Si no hay, se pone negro.

    }

    /*
     * Getters y setters
     */

    public String getEnlacePortada() {
        return enlacePortada;
    }

    public String getTitulo() {
        return titulo;
    }

    public Bitmap getPortada() {

        if (portada == null && enlacePortada != null){
                Bitmap p_bitmap;

                try {
                    URL url = new URL(enlacePortada);
                    p_bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                    p_bitmap = Bitmap.createScaledBitmap(p_bitmap, 50, 80, false);
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    p_bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);

                } catch (Exception ee) {
                    p_bitmap = Bitmap.createBitmap(50, 80, Bitmap.Config.ARGB_8888);
                    p_bitmap.eraseColor(Color.parseColor("#37474f"));
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    p_bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
                }
                portada = p_bitmap;
            }

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

    public int getId() {
        return id;
    }

}

