package com.clacksdepartment.hype;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.TextView;

/**
 * Created by Usuario on 15/09/2017.
 */

public class SearchableActivity extends AppCompatActivity implements FichaFragment.OnFragmentInteractionListener{

    private RecyclerView mRecyclerView;
    private BusquedaAdapter mBusquedaAdapter;
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

        mRecyclerView = (RecyclerView) findViewById(R.id.lista);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mBusquedaAdapter= new BusquedaAdapter(this,R.layout.fila, feedReaderDbHelper);
        mRecyclerView.setAdapter(mBusquedaAdapter);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            doMySearch(query);

        }



    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Menú de opciones creado");
        // Expande el mMenu, añade las opciones
        getMenuInflater().inflate(R.menu.menu, menu);

     //   SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.busqueda).getActionView();
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
        LayoutAnimationController layoutAnimationController = AnimationUtils.loadLayoutAnimation(this,R.anim.rellenar_lista);
        ((RecyclerView) findViewById(R.id.lista)).setLayoutAnimation(layoutAnimationController);
        mBusquedaAdapter.buscar(query);

    }

    @Override
    public void onBackPressed(){
        FrameLayout fragmento = (FrameLayout) findViewById(R.id.ficha_container);

        if (fragmento != null && fragmento.getVisibility()==View.VISIBLE) {
            super.onBackPressed();
        }
        else {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);        }
    }



    public void mostrarAvanzado(View view){
        mBusquedaAdapter.setItemExpandido(view);
    }
    public void marcarHype(View view){
        mBusquedaAdapter.marcarHype(view);
    }

    public void enviarCalendario(View view) {
        startActivity(mBusquedaAdapter.abrirCalendario());
    }

    public void abrirFicha(View view) {
        mBusquedaAdapter.abrirFicha();
    }

    public void abrirWeb(View view) {
        startActivity(mBusquedaAdapter.abrirWeb());
    }

    public void abrirMenuCompartir(View view) {
        mBusquedaAdapter.abrirMenuCompartir();
    }



    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
