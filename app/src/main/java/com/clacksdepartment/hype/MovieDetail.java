package com.clacksdepartment.hype;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;

public class MovieDetail extends AsyncTask<Void,Integer,Void> {

    private static final String TAG = "MovieDetail";
    private static final String apiKey = "8ac0d37839748f4647039ef00d859d13";
    private static final String preImage = "https://image.tmdb.org/t/p/original";
    private static final String preYoutube = "https://www.youtube.com/watch?v=";

    // Value of result progress
    private static final int progress_COVER = 0;
    private static final int progress_YEAR = 2;
    private static final int progress_DURATION = 3;
    private static final int progress_DIRECTOR = 4;
    private static final int progress_CAST = 5;
    private static final int progress_GENRE = 6;
    private static final int progress_RATING = 7;
    private static final int progress_trailer = 8;

    private String id;
    private String year;
    private String duration;
    private Drawable cover;
    private List<String> director;
    private List <String> cast;
    private List <String> genre;
    private String rating;
    private String votes;
    private String videoProvider;
    private String videoLink;

    // View to be updated
    private final View mView;

    MovieDetail(String url, View view){
        String [] urlSplit = url.split("/");
        this.id = urlSplit[urlSplit.length-1];
        mView = view;
        director = new ArrayList<>();
        cast = new ArrayList<>();
        genre = new ArrayList<>();
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {

            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND + THREAD_PRIORITY_MORE_FAVORABLE);

            // Read HTML
            String country = PreferenceManager.getDefaultSharedPreferences(mView.getContext()).getString("pref_country", "");
            String language = "es-ES";
            if (country.equalsIgnoreCase("uk") || country.equalsIgnoreCase("us") || country.equalsIgnoreCase("fr"))
                language = "en-US";
            String result = getHTML("https://api.themoviedb.org/3/movie/"+id+"?api_key="+apiKey+"&language=" + language + "&append_to_response=videos,credits");

            // Parse:
            JSONObject jObject;
            JSONArray jArray;

            jObject = new JSONObject(result);

            String coverUrl = preImage + jObject.getString("poster_path");
            Log.d(TAG, coverUrl);
            cover = loadImageFromURL(coverUrl, coverUrl);

            publishProgress(progress_COVER);

            duration = jObject.getString("runtime") + " min";
            Log.d(TAG, duration);

            publishProgress(progress_DURATION);

            year = jObject.getString("release_date").split("-")[0];
            Log.d(TAG, year);

            publishProgress(progress_YEAR);

            rating = jObject.getString("vote_average");
            Log.d(TAG, rating);
            votes = jObject.getString("vote_count");
            Log.d(TAG, votes);

            publishProgress(progress_RATING);

            jArray = jObject.getJSONArray("genres");

            boolean mustContinue = true;
            int cont = 0;

            while (mustContinue){
                try{
                    genre.add(jArray.getJSONObject(cont).getString("name"));
                    cont++;
                }catch (Exception e){
                    mustContinue = false;
                }
            }

            publishProgress(progress_GENRE);

            jArray = jObject.getJSONObject("credits").getJSONArray("cast");

            mustContinue = true;
            cont = 0;

            while (mustContinue){
                try{
                    cast.add(jArray.getJSONObject(cont).getString("name"));
                    cont++;
                }catch (Exception e){
                    mustContinue = false;
                }
            }

            publishProgress(progress_CAST);

            jArray = jObject.getJSONObject("credits").getJSONArray("crew");

            mustContinue = true;
            cont = 0;

            while (mustContinue){
                try{
                    if (jArray.getJSONObject(cont).getString("job").equalsIgnoreCase("director")) {
                        director.add(jArray.getJSONObject(cont).getString("name"));
                        Log.d(TAG, "Director found: " + director.get(director.size()-1));
                    }
                    cont++;
                }catch (Exception e){
                    mustContinue = false;
                }
            }
            publishProgress(progress_DIRECTOR);

            jArray = jObject.getJSONObject("videos").getJSONArray("results");
            videoProvider = jArray.getJSONObject(0).getString("site");
            if (videoProvider.equalsIgnoreCase("youtube")) {
                videoLink = preYoutube + jArray.getJSONObject(0).getString("key");
            }
            Log.d(TAG, videoLink);

            publishProgress(progress_trailer);
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
        // Movie detail completed!
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        switch (values[0]){
            case progress_COVER:
                if (cover != null)
                    ((ImageView) mView.findViewById(R.id.movie_detail_cover)).setImageDrawable(cover);
                break;
            case progress_YEAR:
                if (!year.equalsIgnoreCase("null"))
                    ((TextView) mView.findViewById(R.id.movie_detail_year)).setText(year);
                else
                    ((TextView) mView.findViewById(R.id.movie_detail_year)).setText("N/A");
                break;
            case progress_DURATION:
                if (!duration.equalsIgnoreCase("0 min") && !duration.equalsIgnoreCase("null min"))
                    ((TextView) mView.findViewById(R.id.movie_detail_duration)).setText(duration);
                else
                    ((TextView) mView.findViewById(R.id.movie_detail_duration)).setText("N/A");
                break;
            case progress_DIRECTOR:
                ((TextView) mView.findViewById(R.id.movie_detail_director)).setText(director.toString().replace("[", "").replace("]", ""));
                break;
            case progress_CAST:
                ((TextView) mView.findViewById(R.id.movie_detail_cast)).setText(cast.toString().replace("[", "").replace("]", ""));
                break;
            case progress_GENRE:
                ((TextView) mView.findViewById(R.id.movie_detail_genre)).setText(genre.toString().replace("[", "").replace("]", ""));
                break;
            case progress_RATING:
                if (rating != null && !votes.equalsIgnoreCase("0")) {
                    ((TextView) mView.findViewById(R.id.movie_detail_rating)).setText(mView.getResources().getString(R.string.votes_structure, rating, votes));
                }else {
                    ((TextView) mView.findViewById(R.id.movie_detail_rating)).setText("N/A");
                }
                break;
            case progress_trailer:
                mView.findViewById(R.id.movie_detail_trailer).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(videoLink));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mView.getContext().startActivity(intent);
                    }
                });
                mView.findViewById(R.id.movie_detail_trailer_container).setVisibility(View.VISIBLE);
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

    @NonNull
    private String getHTML(String url) throws IOException {
        Log.d(TAG, "Obtaining HTML from " + url);
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

    private Drawable loadImageFromURL(String url, String name) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(is, name);
        } catch (Exception e) {
            return null;
        }
    }


}
