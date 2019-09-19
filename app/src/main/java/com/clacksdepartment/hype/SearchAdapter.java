package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.CalendarContract;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SearchAdapter extends  RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    private static final String TAG = "SearchAdapter";

    private final SearchableActivity mActivity;
    private ArrayList<Movie> mSearchResults;
    private FeedReaderDbHelper mDB;
    private FragmentManager mFragmentManager;
    private int expandedItem = -1;
    private int itemToExpand;
    private int itemToCollapse;
    private RecyclerView mRecyclerView;
    private final int viewMeasureSpecHeight;

    // you provide access to all the views for a data item in a view holder
    static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        RelativeLayout mView;

        ViewHolder(RelativeLayout v) {
            super(v);
            mView = v;
        }
        void clearAnimation(){
            mView.clearAnimation();
        }
    }

    SearchAdapter(SearchableActivity searchableActivity,
                  FeedReaderDbHelper db) {

        mSearchResults = new ArrayList<>();
        this.mActivity = searchableActivity;
        mFragmentManager = searchableActivity.getSupportFragmentManager();
        this.mDB = db;
        mRecyclerView = mActivity.findViewById(R.id.movieList);
        setHasStableIds(true);
        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        itemToCollapse = -1;
        itemToExpand = -1;

        viewMeasureSpecHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    }

    @Override
    @NonNull
    public SearchAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.movie_row, parent, false);
        // set the view's size, margins, padding and layout parameters
        return new SearchAdapter.ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(SearchAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Movie movie = mSearchResults.get(position);

        View rowView = holder.mView;

        ((TextView) rowView.findViewById(R.id.title)).setText(movie.getTitle());
        ((TextView) rowView.findViewById(R.id.releaseDate)).setText(movie.getReleaseDateString());
        ((ImageView) rowView.findViewById(R.id.cover)).setImageBitmap(movie.getCover());

        // If it stored as hyped, label it
        if (movie.getHype()) {
            rowView.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
        } else
            rowView.findViewById(R.id.hype_msg).setVisibility(View.GONE);

        View expandedRow = rowView.findViewById(R.id.expandedData);

        // If the user has touched a movie, expand the synopsis and options. If not, collapse
        if (position == expandedItem) {

            if (movie.getSynopsis().length() > 0){
                ((TextView) expandedRow.findViewById(R.id.av_synopsis)).setText(
                        mActivity.getResources().getString(R.string.synopsis_list_structure,
                        movie.getSynopsis().substring(0,
                        Math.min(movie.getSynopsis().length(), 200))));
            }else{
                ((TextView) expandedRow.findViewById(R.id.av_synopsis)).setText("");
                expandedRow.findViewById(R.id.av_synopsis).setVisibility(View.GONE);
            }

            if (movie.getHype()) {
                ((ImageButton) expandedRow.findViewById(R.id.av_hype)).
                        setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                ((ImageButton) expandedRow.findViewById(R.id.av_hype)).
                        setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

            if (itemToExpand == position) {
                expand(expandedRow);
                itemToExpand = -1;
            }else{
                expandedRow.setVisibility(View.VISIBLE);
            }

        } else{
            if (itemToCollapse == position) {
                collapse(expandedRow);
                itemToCollapse = -1;
            }else{
                expandedRow.setVisibility(View.GONE);
            }
        }

        Log.v(TAG, "Adding movie " + movie.getTitle() + " to view number " + position);
    }


    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemCount() {
        return mSearchResults.size();
    }

    void search(String query){

        mSearchResults.clear();
        String title, coverLink, link, synopsis, releaseDateString, releaseDate;
        boolean hype;
        byte[] coverByte;
        Bitmap coverBitmap;
        String[] q = {"%" +query+"%"};

        SQLiteDatabase dbr = mDB.getReadableDatabase();

        // Read DB and save the elements in a cursor
        String[] projection = {
                FeedReaderContract.FeedEntryReleases._ID,
                FeedReaderContract.FeedEntryReleases.COLUMN_TITLE,
                FeedReaderContract.FeedEntryReleases.COLUMN_COVER,
                FeedReaderContract.FeedEntryReleases.COLUMN_COVER_LINK,
                FeedReaderContract.FeedEntryReleases.COLUMN_REF,
                FeedReaderContract.FeedEntryReleases.COLUMN_SYNOPSIS,
                FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE_STRING,
                FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE,
                FeedReaderContract.FeedEntryReleases.COLUMN_HYPE
        };

        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntryReleases.TABLE_NAME, // The table to query
                projection,                               // The columns to return
                FeedReaderContract.FeedEntryReleases.COLUMN_TITLE + " LIKE ?",  // The columns for the WHERE clause
                q ,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE + " ASC"                                    // The sort order
        );

        // Read results
        while (cursor.moveToNext()) {
            releaseDate = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE));
            title = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_TITLE));
            link = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_REF));
            coverByte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_COVER));
            coverLink = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_COVER_LINK));
            synopsis = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_SYNOPSIS));
            releaseDateString = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_RELEASE_DATE_STRING));
            hype = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryReleases.COLUMN_HYPE)) == 1;

            coverBitmap = BitmapFactory.decodeByteArray(coverByte, 0, coverByte.length);

            // Add result
            mSearchResults.add(new Movie(link, coverBitmap, coverLink, title, synopsis,
                    releaseDateString, releaseDate, hype));

            Log.d(TAG, "Find movie release: " + title + ".");
        }
        cursor.close();

        expandedItem = -1;
        itemToExpand = -1;
        itemToCollapse = -1;

        notifyDataSetChanged();
    }

    void  setExpandedItem(View view){
        Log.d(TAG, "Movie touched!");
        // Find the position.
        String title = (String) ((TextView) view.findViewById(R.id.title)).getText();
        int oldPosition = expandedItem;
        int position = 0;

        for (Movie m : mSearchResults) {
            if (m.getTitle().equals(title)) {
                break;
            }
            position++;
        }

        if (expandedItem == position){
            expandedItem = -1;
            Log.d(TAG, "Building element " + position);
            itemToCollapse = position;
        } else {
            expandedItem = position;
            Log.d(TAG, "Expandiendo  elemento " + position);
            itemToExpand = position;
        }
        if(oldPosition != -1)
            notifyItemChanged(oldPosition);
        notifyItemChanged(expandedItem);

        if (expandedItem != -1) {
            ((RecyclerView) mActivity.findViewById(R.id.movieList)).smoothScrollToPosition(expandedItem);
        }

        // Check if we need to add the Calendar button.
        String date = mSearchResults.get(position).getReleaseDate();
        Calendar calendar = Calendar.getInstance();
        Date today = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        boolean isItReleased = dateFormat.format(today).compareToIgnoreCase(date) >= 0;
        if (isItReleased){
            view.findViewById(R.id.av_theaters).setVisibility(View.GONE);
            view.findViewById(R.id.av_date).setVisibility(View.GONE);
        }else{
            view.findViewById(R.id.av_theaters).setVisibility(View.GONE);
            view.findViewById(R.id.av_date).setVisibility(View.VISIBLE);
        }
    }

    void openMovieDetail(){
        Log.i(TAG, "Button touched to open movieDetailFragment");
        Movie movie = mSearchResults.get(expandedItem);
        MovieDetailFragment movieDetailFragment = MovieDetailFragment.newInstance(movie.getTitle(), movie.getLink(), movie.getSynopsis());
        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.open_movie_detail, R.anim.close_movie_detail, R.anim.open_movie_detail, R.anim.close_movie_detail);
        fragmentTransaction.replace(R.id.movie_detail_container, movieDetailFragment).addToBackStack(null).commit();
    }

    Intent sendToCalendar(){
        Movie movie = mSearchResults.get(expandedItem);

        Calendar beginTime = Calendar.getInstance();
        String [] f = movie.getReleaseDate().split("-");
        int[] date = {Integer.parseInt(f[0]), Integer.parseInt(f[1]),Integer.parseInt(f[2])};
        beginTime.set(date[0], date[1]-1, date[2], 0, 0);
        // Check if the event can be edited.
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra("allDay", true)
                .putExtra(CalendarContract.Events.TITLE, movie.getTitle())
                .putExtra(CalendarContract.Events.DESCRIPTION, movie.getSynopsis());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d(TAG, "Asking the calendar to store the movie " + movie.getTitle());

        return intent;
    }

    // Save the movie as a favorite
    void flagHype(View v) {

        Movie movie = mSearchResults.get(expandedItem);

        Log.d(TAG, "Button \"Hype\" touched on movie " + movie.getTitle());

        SQLiteDatabase dbw = mDB.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        movie.setHype(!movie.getHype());

        if (movie.getHype()) {
            ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_black_24dp);
        } else {
            ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }

        contentValues.put(FeedReaderContract.FeedEntryReleases.COLUMN_HYPE, movie.getHype()?1:0);

        String selection = FeedReaderContract.FeedEntryReleases.COLUMN_REF + " LIKE ?";
        String[] selectionArgs = {movie.getLink()};

        dbw.update(
                FeedReaderContract.FeedEntryReleases.TABLE_NAME,
                contentValues,
                selection,
                selectionArgs);


        notifyItemChanged(expandedItem);
    }

    Intent openIntoWeb() {
        Movie movie = mSearchResults.get(expandedItem);
        Log.d(TAG, "Button \"Web\" touched on movie " + movie.getTitle());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(movie.getLink()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    void openShareMenu() {
        Movie movie = mSearchResults.get(expandedItem);

        Log.d(TAG, "Button \"Share\" touched on movie " + movie.getTitle());

        String mensaje;
        Resources res = mActivity.getResources();

        SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        try {
            Date date1 = myFormat.parse(movie.getReleaseDate());
            if (date1 == null)
                date1 = new Date();
            Date date2 = new Date();
            long diff = date1.getTime() - date2.getTime();
            int numDays = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) + 1;
            if (diff <= 0)
                mensaje = res.getString(R.string.share_theaters, movie.getTitle());
            else
                mensaje = res.getString(R.string.share_release, numDays, movie.getTitle());
        } catch (ParseException e) {
            e.printStackTrace();
            mensaje = res.getString(R.string.share_release_ind, movie.getTitle());
        }

        // TODO: add language to link
        mensaje = mensaje + "\n" + movie.getLink();

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, mensaje);
        mActivity.startActivity(Intent.createChooser(intent, res.getString(R.string.share_msg, movie.getTitle())));
    }


    private void expand(final View v) {
        v.measure(View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(),
                View.MeasureSpec.EXACTLY), viewMeasureSpecHeight);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (interpolatedTime == 1){
                    v.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }else{
                    v.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                }

                v.requestLayout();
                mRecyclerView.scrollToPosition(expandedItem);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }

        };

        // 1dp/ms
        a.setDuration((int) (2*targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        v.startAnimation(a);
    }

    private void collapse(final View v) {
        final int initialHeight = v.getMeasuredHeight();
        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }
            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        // 1dp/ms
        a.setDuration((int) (2*initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        v.startAnimation(a);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.clearAnimation();
    }
}