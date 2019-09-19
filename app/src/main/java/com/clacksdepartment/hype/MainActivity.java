package com.clacksdepartment.hype;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.preference.PreferenceManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        MovieDetailFragment.OnFragmentInteractionListener{

    private static final String TAG = "MainActivity";

    private FeedReaderDbHelper mFeedReaderDbHelper;
    private DownloadTMDBThread mDownloadTMDBThread;
    private GUIManager mGUIManager;
    private Menu mMenu;
    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    private RecyclerViewAdapter mRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "Main activity created.");

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        setContentView(R.layout.activity_main);

        // Hook and setup the Toolbar
        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayShowTitleEnabled(false);

        mFeedReaderDbHelper = new FeedReaderDbHelper(getApplicationContext());

        //Set up the list variable, using RecyclerView and a linear layout manager
        mRecyclerView = findViewById(R.id.movieList);
        mRecyclerView.setHasFixedSize(true);    //Increase performance
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerViewAdapter = new RecyclerViewAdapter(this, mFeedReaderDbHelper);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        //Configure Shared Preferences for configuration
        SharedPreferences sharedPref;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);

        //Update the database. Once per day (if configured)
        if (sharedPref.getBoolean("pref_update", true)){
            //Compare today with the last day that the database was updated
            int lastUpdatedDay = sharedPref.getInt("day",0);
            int today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            if (today != lastUpdatedDay) {
                //Soft update.
                mDownloadTMDBThread = new DownloadTMDBThread(this, mRecyclerViewAdapter,
                        ((LinearLayout) findViewById(R.id.load_bar)));
                mDownloadTMDBThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        mFeedReaderDbHelper.getReadableDatabase(),
                        mFeedReaderDbHelper.getWritableDatabase());

                SharedPreferences.Editor sharedPreferencesEditor = sharedPref.edit();
                sharedPreferencesEditor.putInt("initialized", 1);
                sharedPreferencesEditor.putInt("day",today);
                sharedPreferencesEditor.apply();
            }
        }

        //Select the "In Theaters" view as the default one
        mGUIManager = new GUIManager(this, mRecyclerViewAdapter);
        mGUIManager.selectInTheatersSection();
        showTheatersSection(findViewById(R.id.theaters));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Options menu created");
        this.mMenu = menu;

        // Add options to the menu and configure them
        getMenuInflater().inflate(R.menu.menu, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem menuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        // Assumes current activity is the searchable activity
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(
                    new ComponentName(this.getApplicationContext(), SearchableActivity.class)));
            searchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default
        }
        // Add color to the buttons
        for(int i = 0; i < mMenu.size(); i++){
            Drawable drawable = mMenu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
                // It can be changed for this, but it only works on API 29
                // drawable.setColorFilter(new BlendModeColorFilter(
                //        getResources().getColor(R.color.colorAppText), BlendMode.SRC_ATOP));
                drawable.setColorFilter(getResources().getColor(R.color.colorAppText),
                        PorterDuff.Mode.SRC_ATOP);
            }
        }

        // If it is the first execution and it is still downloading (initialized 1),
        // change the update button to cancel
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        if (sharedPref.getInt("initialized",0)<2) {
            mMenu.findItem(R.id.update_button).setEnabled(false);
            mMenu.findItem(R.id.update_button).setVisible(false);
            mMenu.findItem(R.id.cancel_update_button).setEnabled(true);
            mMenu.findItem(R.id.cancel_update_button).setVisible(true);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPref.edit();
            sharedPreferencesEditor.putInt("initialized", 2);
            sharedPreferencesEditor.apply();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem selectedItem) {
        Log.d(TAG, "Option selected: " + selectedItem.toString());

        switch (selectedItem.getItemId()) {

            // Show options
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            // Update movies
            case R.id.update_button:
                mDownloadTMDBThread = new DownloadTMDBThread(this, mRecyclerViewAdapter,
                        ((LinearLayout) findViewById(R.id.load_bar)));
                mDownloadTMDBThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                        mFeedReaderDbHelper.getReadableDatabase(),
                        mFeedReaderDbHelper.getWritableDatabase());

                // Change the update button for the cancel button
                selectedItem.setEnabled(false);
                selectedItem.setVisible(false);
                mMenu.findItem(R.id.cancel_update_button).setEnabled(true);
                mMenu.findItem(R.id.cancel_update_button).setVisible(true);
                return true;

            // Cancel update
            case R.id.cancel_update_button:
                mDownloadTMDBThread.cancel(true);
                // Change the cancel button for the update button
                selectedItem.setEnabled(false);
                selectedItem.setVisible(false);
                mMenu.findItem(R.id.update_button).setEnabled(true);
                mMenu.findItem(R.id.update_button).setVisible(true);
                return true;

            // Open search box
            case R.id.search:
                SearchView searchView = (SearchView) selectedItem.getActionView();
                searchView.setMaxWidth(Integer.MAX_VALUE);
                searchView.setIconifiedByDefault(false);
                return true;

            default:
                return super.onOptionsItemSelected(selectedItem);
        }
    }

    /*
     * Custom methods
     */

    public void turnBackPage(View view) {
        // Internal current page value in the adapter, starting with 0
        int currPage = mRecyclerViewAdapter.getPage();
        if (currPage > 0) {
            // The page variable in the adapter starts in 0, but the text in the app starts in 1
            // We go to currPage - 1 (one page earlier) + 1 (because of the array start point)
            Log.d(TAG, "Going back to page: " + currPage);
            mRecyclerViewAdapter.turnPage(currPage - 1);
            ((TextView) findViewById(R.id.currentPage)).setText(String.valueOf(currPage));
            mGUIManager.hardFocusOnFirstElement();
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    public void turnPage(View view) {
        int currPage = mRecyclerViewAdapter.getPage();
        if (currPage < mRecyclerViewAdapter.getLastPage()-1) {
            // The page variable in the adapter starts in 0, but the text in the app starts in 1
            // We go to currPage + 1 (one page forward) + 1 (because of the array start point)
            Log.d(TAG, "Going forward to page: " + (currPage+2));
            mRecyclerViewAdapter.turnPage(currPage + 1);
            ((TextView) findViewById(R.id.currentPage)).setText(String.valueOf(currPage+2));
            mGUIManager.hardFocusOnFirstElement();
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPref, String key) {
        // Remove all data in the database
        if(key.equalsIgnoreCase("pref_db")){
            mRecyclerViewAdapter.removeList();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mRecyclerViewAdapter.updateInterface();
            mRecyclerViewAdapter.showNoMoviesMessage();

        // Delete everything and download movies for the new country
        } else if (key.equalsIgnoreCase("pref_country")){
            mFeedReaderDbHelper.getWritableDatabase().delete(FeedReaderContract.FeedEntryReleases.TABLE_NAME, null, null);
            mRecyclerViewAdapter.removeList();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mRecyclerViewAdapter.updateInterface();
            mRecyclerViewAdapter.showNoMoviesMessage();
        }
    }

    protected void onDestroy() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public Menu getMenu(){
        return mMenu;
    }

    public void showHypeSection(View view){
        Log.d(TAG, "Displaying Hype view.");
        if (mRecyclerViewAdapter.getSection() != RecyclerViewAdapter.HYPE){
            mGUIManager.animateList();
            mRecyclerViewAdapter.showHypeSection();
            mGUIManager.selectHypeSection();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mGUIManager.hardFocusOnFirstElement();
            ((RecyclerView) findViewById(R.id.movieList)).smoothScrollToPosition(0);
            mRecyclerViewAdapter.showNoMoviesMessage();
        }else{
            mGUIManager.smoothFocusOnFirstElement();
        }
    }

    public void showTheatersSection(View view){
        Log.d(TAG, "Displaying In theaters view.");
        if (mRecyclerViewAdapter.getSection() != RecyclerViewAdapter.THEATERS) {
            mGUIManager.animateList();
            mRecyclerViewAdapter.showTheatersSection();
            mGUIManager.selectInTheatersSection();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mGUIManager.hardFocusOnFirstElement();
            mRecyclerViewAdapter.showNoMoviesMessage();
        }else{
            mGUIManager.smoothFocusOnFirstElement();
        }
    }

    public void showReleasesSection(View view){
        Log.d(TAG, "Displaying Releases view");
        if (mRecyclerViewAdapter.getSection() != RecyclerViewAdapter.RELEASES) {
            mGUIManager.animateList();
            mRecyclerViewAdapter.showReleasesSection();
            mGUIManager.selectReleasesSection();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mGUIManager.hardFocusOnFirstElement();
            mRecyclerViewAdapter.showNoMoviesMessage();
        }else{
            mGUIManager.smoothFocusOnFirstElement();
        }
    }

    public void showExpandedMovieData(View view){
        mRecyclerViewAdapter.setExpandedItem(view, false);
    }

    public void flagHype(View view){
        mRecyclerViewAdapter.flagHype(view);
     }

    public void sendToCalendar(View view) {
        startActivity(mRecyclerViewAdapter.sendToCalendar());
    }

    public void openMovieDetail(View view) {
        mRecyclerViewAdapter.openMovieDetail();
    }

    public void setExpandedItemAndOpenMovieDetail(View view) {
        mRecyclerViewAdapter.setExpandedItemAndOpenMovieDetail((View) view.getParent().getParent());
    }

    public void openIntoWeb(View view) {
        startActivity(mRecyclerViewAdapter.openIntoWeb());
    }

    public void openShareMenu(View view) {
        mRecyclerViewAdapter.openShareMenu();
    }

    public void showCinemas(View view){
        startActivity(mRecyclerViewAdapter.showCinemas());
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first

        mRecyclerViewAdapter.updatePref();
        mRecyclerViewAdapter.notifyDataSetChanged();
    }}