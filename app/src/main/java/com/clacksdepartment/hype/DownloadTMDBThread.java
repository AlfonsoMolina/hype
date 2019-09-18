package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import androidx.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static com.clacksdepartment.hype.FeedReaderContract.FeedEntryReleases;

class DownloadTMDBThread extends AsyncTask<SQLiteDatabase,Integer,Void> {

    private static final String TAG = "DownloadTMDBThread";
    private static final String preImage = "https://image.tmdb.org/t/p/w154";
    private static final String preLink = "https://www.themoviedb.org/movie/";
    private static final String apiKey = "8ac0d37839748f4647039ef00d859d13";

    private String language = "es-ES";  // Language for title and synopsis
    private String country = "ES";      // Country for theaters and releases

    private SharedPreferences sharedPref;

    // List to stare the movies
    private RecyclerViewAdapter movieList;

    private WeakReference<LinearLayout> loadBar;
    private static final int INDEX_THEATER = 1;
    private static final int INDEX_RELEASES = 2;

    private static final int MAX_RESULTS_PER_SECTION = 200;

    DownloadTMDBThread(Context context, RecyclerViewAdapter movieList, LinearLayout loadBar) {
        Log.d(TAG, "Starting thread to download TMDB content.");
        this.movieList = movieList;
        this.loadBar = new WeakReference<>(loadBar);
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // Display load bar (and set gray) and a message.
    @Override
    protected void onPreExecute (){
        Log.d(TAG, "Updating UI before executing the thread.");
        country = sharedPref.getString("pref_country", "");
        country = country.toUpperCase();

        // Language is spanish, unless the country is one of the non-spanish supported countries.
        language = "es-ES";
        if (country.equalsIgnoreCase("uk") || country.equalsIgnoreCase("us")
                || country.equalsIgnoreCase("fr")) {
            language = "en-US";
        }
    }

    @Override
    protected Void doInBackground(SQLiteDatabase... db) {

        publishProgress(0);

        // Downloading
        ArrayList<Movie> theaters = getMovies(INDEX_THEATER);
        ArrayList<Movie> releases = getMovies(INDEX_RELEASES);

        if (theaters == null || releases == null){
            return null;
        }

        int totalProgress = theaters.size() + releases.size();
        int currentProgress = 0;

        Cursor cursor;
        ContentValues values = new ContentValues();

        // Not all movies will be stored in the db.
        ArrayList<Movie> filteredMovies;

        boolean status = true; // This loop will be iterated twice, for theaters and for releases

        // Change the type to -1 of movies in theaters (1) and releases (2).
        // It will be updated later. At the end, any movie still at -1 is removed.
        values.put(FeedReaderContract.FeedEntryReleases.COLUMN_TYPE, -1);

        String clause = FeedReaderContract.FeedEntryReleases.COLUMN_TYPE + "='1' OR " +
                FeedReaderContract.FeedEntryReleases.COLUMN_TYPE + "='2'";
        db[1].update(FeedEntryReleases.TABLE_NAME, values,
                clause, null);

        String[] projection = {
                //FeedEntryReleases._ID,
                //FeedEntryReleases.COLUMN_REF,
                FeedReaderContract.FeedEntryReleases.COLUMN_TITLE,
                //FeedEntryReleases.COLUMN_COVER,
                FeedReaderContract.FeedEntryReleases.COLUMN_COVER_LINK,
                FeedEntryReleases.COLUMN_SYNOPSIS,
                //FeedEntryReleases.COLUMN_TRAILER,
                FeedEntryReleases.COLUMN_RELEASE_DATE_STRING,
                FeedEntryReleases.COLUMN_RELEASE_DATE,
                FeedEntryReleases.COLUMN_HYPE,
                FeedEntryReleases.COLUMN_TYPE
        };

        do {

            if (status) {
                filteredMovies = theaters;
                Log.d(TAG, "Nº of movies in theaters: " + theaters.size());
            } else {
                filteredMovies = releases;
                Log.d(TAG, "Nº of movies to be released: " + releases.size());
            }

            String whereClauseColumns = FeedEntryReleases.COLUMN_REF + " = ?";
            String[] whereClauseValues = new String[1];

            String link;
            String title;
            String synopsis;
            String releaseDateString;
            String releaseDate;
            String coverLink;
            byte[] coverByte;
            Bitmap coverBitmap;


            for (Movie movie:filteredMovies) {

                // We use the link to check if it is already stored
                link = movie.getLink();
                whereClauseValues[0] = link;
                cursor = db[0].query(
                        FeedEntryReleases.TABLE_NAME,                     // The table to query
                        projection,                               // The columns to return
                        whereClauseColumns,                                // The columns for the WHERE clause
                        whereClauseValues,                            // The values for the WHERE clause
                        null,                                     // don't group the rows
                        null,                                     // don't filter by row groups
                        FeedEntryReleases.COLUMN_RELEASE_DATE + (status ? " DESC" : " ASC")                                      // The sort order
                );

                // If it is not stored, we add it
                if (cursor.getCount() == 0) {
                    title = movie.getTitle();
                    coverLink = movie.getCoverLink();
                    synopsis = movie.getSynopsis();
                    releaseDateString = movie.getReleaseDateString();
                    releaseDate = movie.getReleaseDate();
                    coverBitmap = movie.getCover();
                    coverByte = bitmapToByte(coverBitmap);

                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_REF, link);
                    values.put(FeedEntryReleases.COLUMN_TITLE, title);
                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_COVER, coverByte);
                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_COVER_LINK, coverLink);
                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_SYNOPSIS, synopsis);
                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE_STRING, releaseDateString);
                    values.put(FeedEntryReleases.COLUMN_RELEASE_DATE, releaseDate);
                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_TYPE, status?1:2);

                    db[1].insert(FeedReaderContract.FeedEntryReleases.TABLE_NAME, null, values);

                    values.clear();
                    Log.d(TAG, "Adding movie: " + title + ".");

                // If it is stored, update it.
                }else {
                    // Update the elements only if it has changed (less DB load I guess?).
                    cursor.moveToFirst();
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryReleases.COLUMN_TITLE)).equalsIgnoreCase(movie.getTitle())) {
                        values.put(FeedReaderContract.FeedEntryReleases.COLUMN_TITLE, movie.getTitle());
                    }
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryReleases.COLUMN_SYNOPSIS)).equalsIgnoreCase(movie.getSynopsis())) {
                        values.put(FeedEntryReleases.COLUMN_SYNOPSIS, movie.getSynopsis());
                    }
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryReleases.COLUMN_RELEASE_DATE)).equalsIgnoreCase(movie.getReleaseDate())) {
                        values.put(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE, movie.getReleaseDate());
                        values.put(FeedEntryReleases.COLUMN_RELEASE_DATE_STRING, movie.getReleaseDateString());
                    }
                    if(!cursor.getString(cursor.getColumnIndexOrThrow(FeedEntryReleases.COLUMN_COVER_LINK)).equalsIgnoreCase(movie.getCoverLink())) {
                        values.put(FeedReaderContract.FeedEntryReleases.COLUMN_COVER, bitmapToByte(movie.getCover()));
                        values.put(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE_STRING, movie.getReleaseDateString());
                    }

                    // Different type for releases or in theaters
                    values.put(FeedReaderContract.FeedEntryReleases.COLUMN_TYPE, status?1:2);

                    Log.d(TAG, "Updating movie " + movie.getTitle());
                    db[1].update(FeedReaderContract.FeedEntryReleases.TABLE_NAME, values,
                            FeedEntryReleases.COLUMN_REF + "='" +
                            movie.getLink() + "'", null);
                    values.clear();

                }

                currentProgress++;
                // Update load bar
                publishProgress((int) Math.floor(currentProgress*10/totalProgress));
                cursor.close();
            }
            status = !status;
        }while(!status);


        // Any movie that was on the database but was not retrieved on the download is removed,
        // because it is no longer relevant.
        // TODO: except if the download failed
        String selection2 = FeedEntryReleases.COLUMN_TYPE + "<'0'";
        if (!isCancelled())
            db[0].delete(FeedReaderContract.FeedEntryReleases.TABLE_NAME, selection2, null);

        return null;
    }

    // This method reads a HTML and converts it to string
    @NonNull
    private static String getHTML(String url) throws IOException {
        Log.d(TAG, "Downloading HTML from " + url);
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

    private ArrayList<Movie> getMovies(int type){

        ArrayList<Movie> movieList = new ArrayList<>();

        String staticUrl;
        JSONObject jObject;
        JSONArray pageResults;

        int page = 1;
        int numPages;
        int totalResults = 0;
        boolean hasEnded;

        String response;

        // Data to obtain:
        int id;
        String link;
        String title;
        String cover;
        String synopsis;
        String date;
        String textDate;

        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date tomorrow = calendar.getTime();
        calendar.add(Calendar.WEEK_OF_YEAR, -9);
        Date sixWeeksAgo = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

        String todayAsString = dateFormat.format(today);
        String tomorrowAsString = dateFormat.format(tomorrow);
        String sixWeeksAgoAsString = dateFormat.format(sixWeeksAgo);

        switch (type){
            case INDEX_THEATER:
                staticUrl = "https://api.themoviedb.org/3/discover/movie?api_key="+apiKey+
                        "&sort_by=primary_release_date.desc&release_date.lte="+todayAsString+
                        "&release_date.gte="+sixWeeksAgoAsString+"&with_release_type=3";
                break;
            case INDEX_RELEASES:
                staticUrl = "https://api.themoviedb.org/3/discover/movie?api_key="+apiKey+
                        "&sort_by=primary_release_date.asc&release_date.gte="+tomorrowAsString+
                        "&with_release_type=2|3";
                break;
            default:
                return null;
        }

        try {
            do {
                Log.d(TAG, staticUrl);
                response = getHTML(staticUrl + "&language=" + language + "&page=" + page +
                        "&region=" + country + "&adult=true");

                jObject = new JSONObject(response);
                numPages = jObject.getInt("total_pages");
                pageResults = jObject.getJSONArray("results");
                hasEnded = false;

                for (int results = 0;!hasEnded && (totalResults<MAX_RESULTS_PER_SECTION);results++){
                    try {
                        id = pageResults.getJSONObject(results).getInt("id");
                        link = preLink + pageResults.getJSONObject(results).getString("id");
                        title = pageResults.getJSONObject(results).getString("title");
                        cover = preImage + pageResults.getJSONObject(results).getString("poster_path");
                        synopsis = pageResults.getJSONObject(results).getString("overview");
                        date = pageResults.getJSONObject(results).getString("release_date");
                        textDate = getDateText(date);

                        // Add movies to the right list. If the date does not match, skip it.
                        switch (type) {
                            case INDEX_THEATER:
                                if (isItReleased(date)){
                                    movieList.add(new Movie(id, link, cover, title, synopsis, textDate, date, false));
                                }else{
                                    Log.d(TAG, "Skipping movie in wrong section: " + title);
                                }
                                break;
                            case INDEX_RELEASES:
                                if (!isItReleased(date)){
                                    movieList.add(new Movie(id, link, cover, title, synopsis, textDate, date, false));
                                }else{
                                    Log.d(TAG, "Skipping movie in wrong section: " + title);
                                }
                                break;
                            default:
                                break;
                        }

                        totalResults++;
                    }catch (Exception e){
                        hasEnded = true;
                    }
                }
                // Keep downloading the next page of results.
                page++;
            } while ((page <= numPages) && (totalResults < MAX_RESULTS_PER_SECTION));
        }catch (Exception e){
            if (e.getMessage() != null)
                Log.e(TAG, e.getMessage());
            else
                Log.e(TAG, "Something failed during parsing the JSON response.");
        }
        return movieList;
    }

    private String getDateText (String date){
        String dateText;
        String[] months_es = {"enero", "febrero", "marzo", "abril", "mayo", "junio",
                "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
        String[] months_en = {"january", "february", "march", "april", "may", "june",
                "july", "august", "september", "october", "november", "december"};

        String [] splitDate = date.split("-");

        if (language.equalsIgnoreCase("es-ES")) {
            dateText = splitDate[2] + " de " + months_es[Integer.parseInt(splitDate[1]) - 1] + " del " + splitDate[0];
        }else{
            dateText = splitDate[2] + " " + months_en[Integer.parseInt(splitDate[1]) - 1] + " " + splitDate[0];
        }
        return dateText;
    }

    private byte [] bitmapToByte (Bitmap bmp){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    @Override
    protected void onPostExecute(Void v) {
        Log.d(TAG, "Download ended, updating interface.");
        //movieList.notifyDataSetChanged();
        movieList.updateData();
        movieList.updateInterface();
        movieList.removeX(); // Extra method to remove the X button.
        loadBar.get().setVisibility(View.GONE);
    }

    @Override
    protected void onCancelled(Void v){
        Log.d(TAG, "Download canceled, updating interface.");
        //movieList.notifyDataSetChanged();
        movieList.updateInterface();
        movieList.removeX(); // Extra method to remove the X button.
        movieList.updateData();
        loadBar.get().setVisibility(View.GONE);
    }

    private boolean isItReleased(String date){
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(today).compareToIgnoreCase(date) >= 0;
    }

    @Override
    protected void onProgressUpdate(Integer... i) {
        Log.d(TAG, "Download at " + ((i[0]+1)*10) + "%");
        if (i[0] == -2)
            movieList.showNoMoviesMessage();
        else if (i[0] == 0){
            for(int j = 0; j < 10; j++)
                loadBar.get().getChildAt(j).setBackgroundColor(Color.parseColor("#455a64"));
            loadBar.get().setVisibility(View.VISIBLE);
            loadBar.get().getChildAt(0).setBackgroundColor(Color.parseColor("#37474f"));
        } else if(i[0]<10) {
            loadBar.get().getChildAt(i[0]).setBackgroundColor(Color.parseColor("#37474f"));
        }
    }

}
