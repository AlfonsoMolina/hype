package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import android.provider.CalendarContract;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import android.util.DisplayMetrics;
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

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

    static final int HYPE = 0;
    static final int THEATERS = 1;
    static final int RELEASES = 2;

    private static final int NUM_ITEM_ADD = 9;

    private ArrayList<ArrayList<Movie>> mReleasesList;
    private ArrayList<ArrayList<Movie>> mTheatersList;
    private MainActivity mMainActivity;                          //Activity, to change the IU
    private GUIManager mGUIManager;
    private int releasesPage = 0;              // Page that is being shown, starting at 0
    private int theatersPage = 0;
    private int numMoviesPerPage = 25;
    private int expandedItem = -1;
    private FeedReaderDbHelper mFeedReaderDbHelper;
    private int section = THEATERS;
    private boolean flagAds;
    private FragmentManager mFragmentManager;
    private LinearLayoutManager mLinearLayoutManager;
    private RecyclerView mRecyclerView;
    private View footer;
    private int movieToExpand;
    private int movieToCollapse;
    private boolean breakNextMoveAnimation;
    private RecyclerView.ItemAnimator animator;
    private long moveDurationBackup;
    private final int viewMeasureSpecHeight;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
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

    // Provide a suitable constructor (depends on the kind of dataset)
    RecyclerViewAdapter(MainActivity mainActivity, FeedReaderDbHelper feedReaderDbHelper) {
        mTheatersList = new ArrayList<>();
        mReleasesList = new ArrayList<>();
        Log.d(TAG, "Building the adapter for mReleasesList");
        this.mMainActivity = mainActivity;
        mFragmentManager = mainActivity.getSupportFragmentManager();
        this.mFeedReaderDbHelper = feedReaderDbHelper;
        ReadDBThread readDBThread = new ReadDBThread(feedReaderDbHelper.getReadableDatabase(),
                feedReaderDbHelper.getWritableDatabase(),this);
        readDBThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mGUIManager = new GUIManager(mainActivity, this);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext());

        setHasStableIds(true);
        flagAds = sharedPref.getBoolean("pref_adds",true);

        mRecyclerView = mMainActivity.findViewById(R.id.movieList);

        breakNextMoveAnimation = false;

        animator = mRecyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            animator.setRemoveDuration(0);
            moveDurationBackup = animator.getMoveDuration();
        }

        mLinearLayoutManager = ((LinearLayoutManager) mRecyclerView.getLayoutManager());
        movieToExpand = -1;
        movieToCollapse = -1;

        viewMeasureSpecHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

    }

    public int getItemViewType(int position) {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous

        if (flagAds && (position == NUM_ITEM_ADD || position == NUM_ITEM_ADD*2))
            return 0;
        else if (position == getItemCount()-1)
            return 1;
        else
            return 2;
    }

    @Override
    public long getItemId(int position) {
        return section *1000 + getPage()*100 + position;
    }

    // Create new views (invoked by the layout manager)
    @Override
    @NonNull
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                                             int viewType) {
        // create a new view
        RelativeLayout v;
        switch (viewType) {
            case 0:
                v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.ad_row, parent, false);
                break;
            case 1:
                v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.footer, parent, false);
                break;
            default:
                v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.movie_row, parent, false);
                break;
        }
        // set the view's size, margin, padding and layout parameters
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        View rowView = holder.mView;

        if (breakNextMoveAnimation){
            animator.setMoveDuration(0);
            breakNextMoveAnimation = false;
        }

        if (flagAds && (position == NUM_ITEM_ADD || position == NUM_ITEM_ADD*2)) {
            NativeExpressAdView mAdView = rowView.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else if (position == getItemCount() -1) {
            footer = rowView;
            updateWall();

        } else {
            Movie movie = getMovie(position);
            ((TextView) rowView.findViewById(R.id.title)).setText(movie.getTitle());
            ((TextView) rowView.findViewById(R.id.releaseDate)).setText(movie.getReleaseDateString());
            ((ImageView) rowView.findViewById(R.id.cover)).setImageBitmap(movie.getCover());

            if (movie.getHype()) {
                rowView.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
            } else
                rowView.findViewById(R.id.hype_msg).setVisibility(View.GONE);

            View expanded = rowView.findViewById(R.id.expandedData);

            if (position == expandedItem) {

                if (movie.getSynopsis().length() > 0){
                    String syn = movie.getSynopsis().substring(0, Math.min(movie.getSynopsis().length(),
                            200)) + ((movie.getSynopsis().length() > 200)? "...":"");
                    ((TextView) expanded.findViewById(R.id.av_synopsis)).setText( syn );
                }else{
                    ((TextView) expanded.findViewById(R.id.av_synopsis)).setText("");
                    expanded.findViewById(R.id.av_synopsis).setVisibility(View.GONE);
                }

                if (movie.getHype())
                    ((ImageButton) expanded.findViewById(R.id.av_hype)).setImageResource(
                            R.drawable.ic_favorite_black_24dp);
                else
                    ((ImageButton) expanded.findViewById(R.id.av_hype)).setImageResource(
                            R.drawable.ic_favorite_border_black_24dp);

                if (movieToExpand == position) {
                    animator.setMoveDuration(moveDurationBackup);
                    expand(expanded);
                    movieToExpand = -1;
                }else{
                    expanded.setVisibility(View.VISIBLE);
                }
            } else {
                if (movieToCollapse == position) {
                    collapse(expanded);
                    movieToCollapse = -1;
                }else{
                    expanded.setVisibility(View.GONE);
                }
            }

            Log.v(TAG, "Adding movie " + movie.getTitle() + " to view number " + position);
        }

    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.clearAnimation();
    }

    private Movie getMovie(int position) {
        if (flagAds && position > NUM_ITEM_ADD){
            position--;
            if (position >= NUM_ITEM_ADD*2)
                position--;
        }
        if (section == HYPE) {
            for (int i = mTheatersList.size() - 1; i >= 0; i--) {
                for (int j = mTheatersList.get(i).size() - 1; j >= 0; j--) {
                    if (mTheatersList.get(i).get(j).getHype()) {
                        if (position == 0)
                            return mTheatersList.get(i).get(j);
                        position--;
                    }
                }
            }

            for (int i = 0; i < mReleasesList.size(); i++) {
                for (int j = 0; j < mReleasesList.get(i).size(); j++) {
                    if (mReleasesList.get(i).get(j).getHype()) {
                        if (position == 0)
                            return mReleasesList.get(i).get(j);
                        position--;
                    }
                }
            }

        } else {
            int page = section == THEATERS ? theatersPage : releasesPage;
            return section == THEATERS ? mTheatersList.get(page).get(position) : mReleasesList.get(page).get(position);
        }

        return mTheatersList.get(0).get(0);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        int count = 0;
        if (section == HYPE){
            for (int i = 0; i < mTheatersList.size(); i++){
                for (int j = 0; j < mTheatersList.get(i).size(); j++){
                    if (mTheatersList.get(i).get(j).getHype()){
                        count++;
                    }
                }
            }

            for (int i = 0; i < mReleasesList.size(); i++){
                for (int j = 0; j < mReleasesList.get(i).size(); j++){
                    if (mReleasesList.get(i).get(j).getHype()){
                        count++;
                    }
                }
            }
        } else if (section == THEATERS) {
            if (mTheatersList.size() > 0)
                count = mTheatersList.get(theatersPage).size();
        } else {
            if (mReleasesList.size() > 0)
                count = mReleasesList.get(releasesPage).size();
        }

        if (flagAds && count > NUM_ITEM_ADD) {
            count++;
            if (count > NUM_ITEM_ADD*2)
                count++;
        }

        count ++; //Por el footer.

        return count;
    }

    int getLastPage() {
        if (section == THEATERS)
            return mTheatersList.size();
        else if (section == RELEASES)
            return mReleasesList.size();
        else
            return 0;
    }

    void turnPage(int i){
        if (section == THEATERS)
            theatersPage = i;
        else if (section == RELEASES)
            releasesPage = i;
        expandedItem = -1;
        updateInterface();
    }

    int getPage(){
        if (section == THEATERS)
            return theatersPage;
        else if (section == RELEASES)
            return releasesPage;
        else
            return 0;
    }

    private void addToTheatersList(Movie p){
        int lastPage = mTheatersList.size() -1;
        if (lastPage >= 0 && mTheatersList.get(lastPage).size() < numMoviesPerPage)
            mTheatersList.get(lastPage).add(p);
        else {
            mTheatersList.add(new ArrayList<Movie>());
            mTheatersList.get(lastPage+1).add(p);
        }
        // If we are in the section and page where data has been added
        if (section == THEATERS && theatersPage == (mTheatersList.size()-1)){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                // If we are moving the view, stop
                Log.d(TAG, "Aborting the updating of the list because of scroll.");
            }
        } else if (section == HYPE && p.getHype()){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                // If we are moving the view, stop
                Log.d(TAG, "Aborting the updating of the list because of scroll.");
            }
        }
    }
    private void addToReleasesList(Movie p){
        int lastPage = mReleasesList.size() -1;
        if (lastPage >= 0 &&  mReleasesList.get(lastPage).size() < numMoviesPerPage)
            mReleasesList.get(lastPage).add(p);
        else {
            mReleasesList.add(new ArrayList<Movie>());
            mReleasesList.get(lastPage+1).add(p);
        }
        if (section == RELEASES && releasesPage == (mReleasesList.size()-1)){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                // If we are moving the view, stop
                Log.d(TAG, "Aborting the updating of the list because of scroll.");
            }
        } else if (section == HYPE && p.getHype()){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                // If we are moving the view, stop
                Log.d(TAG, "Aborting the updating of the list because of scroll.");
            }
        }
    }

    // More efficient methods, maybe
    void addToTheatersList(ArrayList<Movie> movies){
        for (int i = 0; i < movies.size(); i++) {
            addToTheatersList(movies.get(i));
        }
    }
    void addToReleasesList(ArrayList<Movie> movies){
        for (int i = 0; i < movies.size(); i++) {
            addToReleasesList(movies.get(i));
        }
    }

    // If mReleasesList is empty, show no movies message
    void removeX(){
        if(mMainActivity.getMenu()!= null) {
            mMainActivity.getMenu().findItem(R.id.update_button).setEnabled(true);
            mMainActivity.getMenu().findItem(R.id.update_button).setVisible(true);
            mMainActivity.getMenu().findItem(R.id.cancel_update_button).setEnabled(false);
            mMainActivity.getMenu().findItem(R.id.cancel_update_button).setVisible(false);
        }
    }

    void showNoMoviesMessage(){
        if (getItemCount()==1) {
            mGUIManager.showNoMoviesMessage(true);
        } else
            mGUIManager.showNoMoviesMessage(false);
    }

    void updateInterface(){
        mGUIManager.update();
    }

    void removeList() {
        mReleasesList.clear();
        mTheatersList.clear();
    }

    int getSection(){
        return section;
    }

    void showReleasesSection(){
        if (section != RELEASES){
            expandedItem = -1;
            section = RELEASES;
            mGUIManager.update();
        }
    }

    void showHypeSection(){
        if (section != HYPE){
            expandedItem = -1;
            section = HYPE;
            mGUIManager.update();
        }
    }

    void showTheatersSection(){
        if (section != THEATERS){
            expandedItem = -1;
            section = THEATERS;
            mGUIManager.update();
        }
    }

    // If the item has been expanded because of a click on the cover, it should never be collapsed.
    void setExpandedItem(View view, boolean isCoverTouched){
        // Find the position.
        String title = (String) ((TextView) view.findViewById(R.id.title)).getText();
        int oldPosition = expandedItem;
        int position = 0;

        if (section == HYPE){
            Movie p;
            boolean flag = true;
            for (int i = mTheatersList.size()-1; flag && i >=0; i--){
                for (int j = mTheatersList.get(i).size()-1; flag && j >=0 ; j--) {
                    p = mTheatersList.get(i).get(j);
                    if (p.getHype()) {
                        if (p.getTitle().equals(title)) {
                            flag = false;
                        } else
                            position++;
                    }
                }
            }
            for (int i = 0; flag && i < mReleasesList.size(); i++){
                for (int j = 0; flag && j < mReleasesList.get(i).size(); j++) {
                    p = mReleasesList.get(i).get(j);
                    if (p.getHype()) {
                        if (p.getTitle().equals(title)) {
                            flag = false;
                        } else
                            position++;
                    }
                }
            }
        }else {
            for (Movie p : section == THEATERS ? mTheatersList.get(theatersPage) : mReleasesList.get(releasesPage)) {
                if (p.getTitle().equals(title)) {
                    break;
                }
                position++;
            }
        }

        if (flagAds && position >= NUM_ITEM_ADD){
            position++;
            if (position >= NUM_ITEM_ADD*2)
                position++;
        }

        // If the cover of an expanded item has been touched, do not collapse.
        if (expandedItem == position && !isCoverTouched){
            expandedItem = -1;
            Log.d(TAG, "Building element " + position);
            movieToCollapse = position;
        } else if (expandedItem != position){
            expandedItem = position;
            Log.d(TAG, "Expanding element " + position);
            movieToExpand = position;
        }

        if(oldPosition != -1)
            notifyItemChanged(oldPosition);
        notifyItemChanged(expandedItem);
        if (expandedItem != -1) {
            ((RecyclerView) mMainActivity.findViewById(R.id.movieList)).smoothScrollToPosition(expandedItem);
        }

        // Check if we need to add the Calendar button. It can be sorted by section, but in Hype
        // they are mixed up.
        String date = getMovie(position).getReleaseDate();
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
        Movie movie = getMovie(expandedItem);
        MovieDetailFragment movieDetailFragment = MovieDetailFragment.newInstance(movie.getTitle(), movie.getLink(), movie.getSynopsis());

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.open_movie_detail, R.anim.close_movie_detail, R.anim.open_movie_detail, R.anim.close_movie_detail);
        fragmentTransaction.replace(R.id.movie_detail_container, movieDetailFragment).addToBackStack(null).commit();
    }

    void setExpandedItemAndOpenMovieDetail(View view){
        setExpandedItem(view, true);
        openMovieDetail();
    }

    Intent sendToCalendar(){
            Movie movie = getMovie(expandedItem);

            Calendar beginTime = Calendar.getInstance();
            String [] f = movie.getReleaseDate().split("-");
            int[] date = {Integer.parseInt(f[0]), Integer.parseInt(f[1]),Integer.parseInt(f[2])};
            beginTime.set(date[0], date[1]-1, date[2], 0, 0);
            // Check if the event can be edited.
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                    .putExtra(CalendarContract.Events.TITLE, movie.getTitle())
                    .putExtra(CalendarContract.Events.DESCRIPTION, movie.getSynopsis());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Log.d(TAG, "Asking the calendar to store the movie " + movie.getTitle());

            return intent;
        }

    //todo: if a movie is removed from saved while in hype section, show a pop up before
    void flagHype(View v) {
        Movie movie = getMovie(expandedItem);
        Log.d(TAG, "Button \"Hype\" touched on movie " + movie.getTitle());

        SQLiteDatabase dbw = mFeedReaderDbHelper.getWritableDatabase();
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

        if (section != HYPE) {
            notifyItemChanged(expandedItem);
        }else{
            if (expandedItem != 0){
                notifyItemRemoved(expandedItem);
            }else{
                breakNextMoveAnimation = true;
                notifyDataSetChanged();
            }
            if (getItemCount() == 1){
                mGUIManager.update();
            }
            expandedItem = -1;
        }
    }

    Intent openIntoWeb() {
        Movie movie = getMovie(expandedItem);
        Log.d(TAG, "Button \"Web\" touched on movie " + movie.getTitle());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(movie.getLink()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    Intent showCinemas(){
        Movie movie = getMovie(expandedItem);

        Log.d(TAG, "Button \"Theaters\" on movie " + movie.getTitle());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(movie.getLink().replace("movie","movie-showtimes")));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    void openShareMenu() {
        Movie movie = getMovie(expandedItem);

        Log.d(TAG, "Button \"Share\" touched on movie " + movie.getTitle());

        String message;

        int section = this.section;

        if (section == HYPE) {

            for (ArrayList<Movie> pp : mTheatersList) {
                if (pp.contains(movie))
                    section = THEATERS;
            }
            for (ArrayList<Movie> pp : mReleasesList) {
                if (pp.contains(movie))
                    section = RELEASES;
            }
        }

        Resources res = mMainActivity.getResources();

        if (section == THEATERS){
            message = res.getString(R.string.share_theaters, movie.getTitle());
        } else if (section == RELEASES){
            SimpleDateFormat myFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            try {
                Date date1 = myFormat.parse(movie.getReleaseDate());
                if (date1 == null)
                    date1 = new Date();
                Date date2 = new Date();
                long diff = date1.getTime() - date2.getTime();
                int numDays = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS) +1;
                message = res.getQuantityString(R.plurals.share_release,numDays,numDays,movie.getTitle());
            } catch (ParseException e) {
                e.printStackTrace();
                message = res.getString(R.string.share_release_ind,movie.getTitle());
            }
        } else {
            message = res.getString(R.string.share_release_ind,movie.getTitle());
        }

        //Modify the link according to the phone language.
        message = message + "\n" + movie.getLink()+"?language="+Locale.getDefault().toString()
                + " - " + res.getString(R.string.share_ad);

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");

        //intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "He compartido \"" + movie.getTitle() + "\" a través de Hype!");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
        mMainActivity.startActivity(Intent.createChooser(intent,res.getString(R.string.share_msg,movie.getTitle())));

    }

    void updateData() {
        ReadDBThread readDBThread = new ReadDBThread(mFeedReaderDbHelper.getReadableDatabase(),
                mFeedReaderDbHelper.getWritableDatabase(),this);
        readDBThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void expand(final View v) {
            v.measure(View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(), View.MeasureSpec.EXACTLY), viewMeasureSpecHeight);
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
                    mRecyclerView.smoothScrollToPosition(expandedItem);
                }

                @Override
                public boolean hasEnded() {
                    updateWall();
                    return super.hasEnded();
                }

                @Override
                public boolean willChangeBounds() {
                    return true;
                }

            };

            a.setDuration((int) (2*targetHeight / v.getContext().getResources().getDisplayMetrics().density));
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    updateWall();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
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

            a.setDuration((int) (2*initialHeight / v.getContext().getResources().getDisplayMetrics().density));
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            a.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }
                @Override
                public void onAnimationEnd(Animation animation) {
                    updateWall();
                }
                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
            v.startAnimation(a);
    }

    void updatePref(){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mMainActivity.getApplicationContext());

        flagAds = sharedPreferences.getBoolean("pref_adds",true);
    }

    private void updateWall(){
        if (section == HYPE) {
            footer.setVisibility(View.GONE);
            footer.getLayoutParams().height = 0;
        } else {
            if ((mLinearLayoutManager.findLastCompletelyVisibleItemPosition()-mLinearLayoutManager.findFirstCompletelyVisibleItemPosition() + 2 - getItemCount()) > 0){
                footer.setVisibility(View.GONE);
                footer.getLayoutParams().height = 0;
            } else if (mRecyclerView.canScrollVertically(-1)) {
                footer.setVisibility(View.VISIBLE);
                Resources resources = mMainActivity.getResources();
                DisplayMetrics metrics = resources.getDisplayMetrics();
                // 70 must be the height of the footer
                footer.getLayoutParams().height = 70 * (metrics.densityDpi / 160);
                ((TextView) footer.findViewById(R.id.num_pag)).setText(resources.getString(R.string.num_page,(getPage()+1), getLastPage()));
            }else{
                footer.setVisibility(View.GONE);
                footer.getLayoutParams().height = 0;
            }
        }
    }

}