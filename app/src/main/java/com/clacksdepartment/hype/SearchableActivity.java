package com.clacksdepartment.hype;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.FrameLayout;


public class SearchableActivity extends AppCompatActivity implements MovieDetailFragment.OnFragmentInteractionListener{

    private RecyclerView mRecyclerView;
    private SearchAdapter mSearchAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private String query;

    private static final String TAG = "SearchableActivity";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        FeedReaderDbHelper feedReaderDbHelper = new FeedReaderDbHelper(getApplicationContext());

        mRecyclerView = (RecyclerView) findViewById(R.id.movieList);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mSearchAdapter = new SearchAdapter(this,R.layout.movie_row, feedReaderDbHelper);
        mRecyclerView.setAdapter(mSearchAdapter);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);

        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Options menu created.");
        // Expand mMenu and add options
        getMenuInflater().inflate(R.menu.menu, menu);

     //   SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        // Assumes current activity is the searchable activity
     //   searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                doMySearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // TODO Auto-generated method stub
                return false;
            }
        });

        searchView.setQuery(query, false);

       return true;
    }
    void doMySearch(String query){
        LayoutAnimationController layoutAnimationController = AnimationUtils.loadLayoutAnimation(this,R.anim.fill_movie_list);
        ((RecyclerView) findViewById(R.id.movieList)).setLayoutAnimation(layoutAnimationController);
        mSearchAdapter.search(query);

    }

    @Override
    public void onBackPressed(){
        FrameLayout fragment = (FrameLayout) findViewById(R.id.movie_detail_container);

        if (fragment != null && fragment.getVisibility()==View.VISIBLE) {
            super.onBackPressed();
        }
        else {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);        }
    }


    public void showExpandedMovieData(View view){
        mSearchAdapter.setExpandedItem(view);
    }
    public void flagHype(View view){
        mSearchAdapter.flagHype(view);
    }

    public void sendToCalendar(View view) {
        startActivity(mSearchAdapter.sendToCalendar());
    }

    public void openMovieDetail(View view) {
        mSearchAdapter.openMovieDetail();
    }

    public void openIntoWeb(View view) {
        startActivity(mSearchAdapter.openIntoWeb());
    }

    public void openShareMenu(View view) {
        mSearchAdapter.openShareMenu();
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
