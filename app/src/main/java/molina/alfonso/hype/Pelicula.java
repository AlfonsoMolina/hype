package molina.alfonso.hype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.net.URL;

/**
 * Created by Usuario on 02/07/2017.
 */

public class Pelicula {

    private String titulo;
    private Bitmap portada;
    private String sinopsis;
    private String estreno;
    private String fecha_estreno;
    private String enlace;
    private boolean isPressed;

    Pelicula (String l, String p, String t, String s, String e, String f, Boolean h){
        this.titulo = t;
        this.sinopsis = s;
        this.estreno = e;
        this.enlace = l;
        this.fecha_estreno = f;
        this.isPressed = h;

        try {
            URL url = new URL(p);
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            this.portada = Bitmap.createScaledBitmap(bmp, 50, 80, false);

        }catch (Exception ee){

        }
    }

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
        return estreno;
    }

    public String getEnlace() {
        return enlace;
    }

    public String getFecha_estreno() {
        return fecha_estreno;
    }
    
    public void setisPressed(boolean b) {
        this.isPressed = b;
    }

    public boolean getisPressed() {
        return isPressed;
    }
}

