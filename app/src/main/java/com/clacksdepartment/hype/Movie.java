package com.clacksdepartment.hype;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 * Created by Clacks Department on 02/07/2017.
 */

public class Movie {

    private static final String TAG = "Movie";

    private String title;
    private Bitmap cover;
    private String synopsis;
    private String releaseDateString;       //Viernes, 4 de febrero
    private String releaseDate;             //2018/02/04
    private String link;
    private int type;
    private String coverLink;
    private boolean hype;
    private int id;

    //link, cover, title, synopsis, release date (letters), release date (detail), hype?
    Movie(String link, Bitmap cover, String coverLink, String title, String synopsis,
          String releaseDateString, String releaseDate, Boolean hype){
        Log.v(TAG, "Object Movie constructed");
        this.title = title;
        this.synopsis = synopsis;
        this.releaseDateString = releaseDateString;
        this.link = link;
        this.releaseDate = releaseDate;
        this.hype = hype;
        this.cover = cover;
        this.coverLink = coverLink;
    }

    Movie(int id, String link, String coverLink, String title, String synopsis,
          String releaseDateString, String releaseDate, Boolean hype){
        Log.v(TAG, "Object Movie constructed");
        this.title = title;
        this.synopsis = synopsis;
        this.releaseDateString = releaseDateString;
        this.link = link;
        this.releaseDate = releaseDate;
        this.hype = hype;
        this.coverLink = coverLink;
        this.id = id;
    }

    /*
     * Getters y setters
     */

    public String getTitle() {
        return title;
    }
    public String getCoverLink() {
        return coverLink;
    }
    public Bitmap getCover() {
        if (cover == null && coverLink != null){
            Bitmap p_bitmap;
            try {
                URL url = new URL(coverLink);
                p_bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                p_bitmap = Bitmap.createScaledBitmap(p_bitmap, 50, 80, false);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                p_bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);

            } catch (Exception ee) {
                Log.v(TAG, "Error get cover " + ee.getMessage());
                p_bitmap = Bitmap.createBitmap(50, 80, Bitmap.Config.ARGB_8888);
                p_bitmap.eraseColor(Color.parseColor("#37474f"));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                p_bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
            }
            cover = p_bitmap;
        }

        return cover;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getReleaseDateString() {
        return releaseDateString;
    }

    public String getLink() {
        return link;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setHype(boolean b) {
        Log.d(TAG, "Setting Hype=" + b + " in movie " + title);
        this.hype = b;
    }

    public boolean getHype() {
        return hype;
    }

    public int getId() {
        return id;
    }

}

