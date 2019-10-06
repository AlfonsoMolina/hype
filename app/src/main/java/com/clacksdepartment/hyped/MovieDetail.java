package com.clacksdepartment.hyped;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import androidx.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;
import static android.os.Process.THREAD_PRIORITY_MORE_FAVORABLE;

public class MovieDetail extends AsyncTask<Void,Integer,Void> {

    private static final String TAG = "MovieDetail";
    private static final String apiKey = "8ac0d37839748f4647039ef00d859d13";
    private static final String preImage = "https://image.tmdb.org/t/p/w342";
    private static final String preYoutube = "https://www.youtube.com/watch?v=";

    // Value of result progress
    private static final int progress_COVER = 0;
    private static final int progress_YEAR = 2;
    private static final int progress_DURATION = 3;
    private static final int progress_DIRECTOR = 4;
    private static final int progress_CAST = 5;
    private static final int progress_GENRE = 6;
    private static final int progress_RATING = 7;
    private static final int progress_TRAILER = 8;
    private static final int progress_SYNOPSIS = 9;

    private String link;
    private String id;
    private String year;
    private String duration;
    private Drawable cover;
    private List<String> director;
    private List <String> cast;
    private List <String> genre;
    private String rating;
    private String votes;
    private String videoLink;
    private FeedReaderDbHelper mFeedReaderDbHelper;
    private String synopsis;

    // View to be updated
    // WeakReference to avoid leaking the context
    private final WeakReference<View> mView;

    MovieDetail(String url, View view){
        String [] urlSplit = url.split("/");
        this.id = urlSplit[urlSplit.length-1];
        link = url;
        mView = new WeakReference<>(view);
        director = new ArrayList<>();
        cast = new ArrayList<>();
        genre = new ArrayList<>();
        mFeedReaderDbHelper = new FeedReaderDbHelper(mView.get().getContext());
        synopsis = ((TextView) mView.get().findViewById(R.id.movie_detail_synopsis)).getText().toString();

    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {

            Process.setThreadPriority(THREAD_PRIORITY_BACKGROUND + THREAD_PRIORITY_MORE_FAVORABLE);

            // Read HTML
            String language = Locale.getDefault().toString();
            String result = getHTML("https://api.themoviedb.org/3/movie/"+id+"?api_key="+apiKey+
                    "&language=" + language + "&append_to_response=videos,credits");

            // Parse:
            JSONObject jObject;
            JSONArray jArray;

            jObject = new JSONObject(result);

            // First, check the synopsis. If it is empty, retrieve it from the default language.
            if (synopsis.isEmpty()) {
                if (jObject.getString("overview").isEmpty()) {
                    String result2 = getHTML("https://api.themoviedb.org/3/movie/" + id + "?api_key="
                            + apiKey + "&append_to_response=videos,credits");
                    synopsis = (new JSONObject(result2)).getString("overview");
                    Log.d(TAG, "Synopsis obtained from default language");
                    Log.d(TAG, synopsis);
                } else {
                    synopsis = jObject.getString("overview");
                }
                publishProgress(progress_SYNOPSIS);
                saveSynopsisInDatabase(synopsis);
            }

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

            for (int i=0; i< jArray.length(); i++){
                try{
                    genre.add(jArray.getJSONObject(i).getString("name"));
                }catch (Exception e){
                    if(e.getMessage() != null)
                        Log.d(TAG,"Error during genre parsing " + e.getMessage());
                    else
                        Log.d(TAG, "Something happened during genre parsing");
                }
            }

            publishProgress(progress_GENRE);

            jArray = jObject.getJSONObject("credits").getJSONArray("cast");

            for (int i=0; i< jArray.length(); i++){
                try{
                    cast.add(jArray.getJSONObject(i).getString("name"));
                }catch (Exception e){
                    if(e.getMessage() != null)
                        Log.d(TAG,"Error during cast parsing " + e.getMessage());
                    else
                        Log.d(TAG, "Something happened during cast parsing");
                }
            }

            publishProgress(progress_CAST);

            jArray = jObject.getJSONObject("credits").getJSONArray("crew");


            for (int i=0; i< jArray.length(); i++){
                try{
                    if (jArray.getJSONObject(i).getString("job").equalsIgnoreCase("director")) {
                        director.add(jArray.getJSONObject(i).getString("name"));
                        Log.d(TAG, "Director found: " + director.get(director.size()-1));
                    }
                }catch (Exception e){
                    if(e.getMessage() != null)
                        Log.d(TAG,"Error during director parsing " + e.getMessage());
                    else
                        Log.d(TAG, "Something happened during director parsing");
                }
            }
            publishProgress(progress_DIRECTOR);

            try {
                jArray = jObject.getJSONObject("videos").getJSONArray("results");
                String videoProvider = jArray.getJSONObject(0).getString("site");
                if (videoProvider.equalsIgnoreCase("youtube")) {
                    videoLink = preYoutube + jArray.getJSONObject(0).getString("key");
                } else {
                    videoLink = "";
                }
            }catch (Exception e){
                videoLink = "";
            }

            Log.d(TAG, videoLink);

            publishProgress(progress_TRAILER);

            String coverUrl = preImage + jObject.getString("poster_path");
            Log.d(TAG, coverUrl);
            cover = loadImageFromURL(coverUrl);

            publishProgress(progress_COVER);
        }catch (Exception e){
            Log.e(TAG, e.toString());
        }

        return null;
    }

    private void saveSynopsisInDatabase(String synopsis) {
        SQLiteDatabase dbw = mFeedReaderDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(FeedReaderContract.FeedEntryReleases.COLUMN_SYNOPSIS, synopsis);
        dbw.update(FeedReaderContract.FeedEntryReleases.TABLE_NAME, values,
                FeedReaderContract.FeedEntryReleases.COLUMN_REF + "='" +
                        link + "'", null);
        values.clear();
        dbw.close();

        // It will be updated on the main view when it is restored.
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
            case progress_SYNOPSIS:
                if(!synopsis.isEmpty()){
                    ((TextView) mView.get().findViewById(R.id.movie_detail_synopsis)).setText(synopsis);
                }
                break;
            case progress_COVER:
                if (cover != null)
                    ((ImageView) mView.get().findViewById(R.id.movie_detail_cover)).setImageDrawable(cover);
                break;
            case progress_YEAR:
                if (!year.equalsIgnoreCase("null"))
                    ((TextView) mView.get().findViewById(R.id.movie_detail_year)).setText(year);
                else
                    ((TextView) mView.get().findViewById(R.id.movie_detail_year)).setText("N/A");
                break;
            case progress_DURATION:
                if (!duration.equalsIgnoreCase("0 min") && !duration.equalsIgnoreCase("null min"))
                    ((TextView) mView.get().findViewById(R.id.movie_detail_duration)).setText(duration);
                else
                    ((TextView) mView.get().findViewById(R.id.movie_detail_duration)).setText("N/A");
                break;
            case progress_DIRECTOR:
                if (director != null && !director.isEmpty())
                    ((TextView) mView.get().findViewById(R.id.movie_detail_director)).setText(director.toString().replace("[", "").replace("]", ""));
                else
                    ((TextView) mView.get().findViewById(R.id.movie_detail_director)).setText("N/A");
                break;
            case progress_GENRE:
                if(genre != null && !genre.isEmpty())
                    ((TextView) mView.get().findViewById(R.id.movie_detail_genre)).setText(genre.toString().replace("[", "").replace("]", ""));
                else
                    ((TextView) mView.get().findViewById(R.id.movie_detail_genre)).setText("N/A");
                break;
            case progress_CAST:
                if(cast != null && !cast.isEmpty())
                    ((TextView) mView.get().findViewById(R.id.movie_detail_cast)).setText(cast.toString().replace("[", "").replace("]", ""));
                else
                    ((TextView) mView.get().findViewById(R.id.movie_detail_cast)).setText("N/A");
                break;
            case progress_RATING:
                if (rating != null && !votes.equalsIgnoreCase("0"))
                    ((TextView) mView.get().findViewById(R.id.movie_detail_rating)).setText(
                            mView.get().getResources().getString(R.string.votes_structure, rating, votes));
                else
                    ((TextView) mView.get().findViewById(R.id.movie_detail_rating)).setText("N/A");
                break;
            case progress_TRAILER:
                if (!videoLink.equals("")) {
                    mView.get().findViewById(R.id.movie_detail_trailer).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(videoLink));
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            mView.get().getContext().startActivity(intent);
                        }
                    });
                    mView.get().findViewById(R.id.movie_detail_trailer_container).setVisibility(View.VISIBLE);
                }
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

    private Drawable loadImageFromURL(String url) {
        SQLiteDatabase dbr = mFeedReaderDbHelper.getReadableDatabase();

        // BIG COVER not null and ref = link
        String whereClauseColumns = FeedReaderContract.FeedEntryReleases.COLUMN_BIG_COVER +
                " NOT NULL AND " + FeedReaderContract.FeedEntryReleases.COLUMN_REF + " = ?";
        String[] whereClauseValues = new String[1];
        whereClauseValues[0] = link;
        String[] projection = {
                FeedReaderContract.FeedEntryReleases.COLUMN_BIG_COVER
        };

        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntryReleases.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                whereClauseColumns,                                // The columns for the WHERE clause
                whereClauseValues,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                null                                      // The sort order
        );

        // If it is not stored, we add it
        if (cursor.getCount() == 0) {
            Log.d(TAG,"Downloading cover.");
            cursor.close();
            dbr.close();

            try {
                InputStream is = (InputStream) new URL(url).getContent();

                // Get bitmap and store it into the database
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] coverByte = stream.toByteArray();

                SQLiteDatabase dbw = mFeedReaderDbHelper.getWritableDatabase();

                ContentValues values = new ContentValues();
                values.put(FeedReaderContract.FeedEntryReleases.COLUMN_BIG_COVER, coverByte);
                dbw.update(FeedReaderContract.FeedEntryReleases.TABLE_NAME, values,
                        FeedReaderContract.FeedEntryReleases.COLUMN_REF + "='" +
                                link + "'", null);
                values.clear();
                dbw.close();
                // Return a drawable
                return new BitmapDrawable(mView.get().getResources(),bitmap);
            } catch (Exception e) {
                if (e.getMessage() != null)
                    Log.e(TAG,e.getMessage());
                else
                    Log.e(TAG,"Error downloading the big cover.");
                return null;
            }
        // If it is stored, retrieve it.
        }else {
            Log.d(TAG,"Retrieving cover from database.");
            cursor.moveToFirst();
            byte[] coverByte = cursor.getBlob(cursor.getColumnIndexOrThrow(
                    FeedReaderContract.FeedEntryReleases.COLUMN_BIG_COVER));
            cursor.close();
            dbr.close();
            Bitmap coverBitmap = BitmapFactory.decodeByteArray(coverByte, 0, coverByte.length);
            return new BitmapDrawable(mView.get().getResources(),coverBitmap);
        }

    }


}
