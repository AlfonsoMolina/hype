package com.clacksdepartment.hype;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.support.v7.widget.SearchView;
import android.widget.TextView;

import com.google.android.gms.ads.MobileAds;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, FichaFragment.OnFragmentInteractionListener{

    /*
     * Declaración de variables
     */
    private static final String TAG = "MainActivity";

    private static final String MY_ADMOB_APP_ID = "ca-app-pub-6428634425759083~8703294528";

   // private ListaModificadaAdapter mRecyclerViewAdapter;
    private FeedReaderDbHelper mFeedReaderDbHelper;
    private HiloDescargasTMDB mHiloDescargasTMDB;
    private Interfaz mInterfaz;
    private Menu mMenu;

    SharedPreferences sharedPreferences;

    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mRecyclerViewAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    /*
     * Métodos override
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Actividad principal creada");
        MobileAds.initialize(this, MY_ADMOB_APP_ID);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Fijamos el layout a usar
        setContentView(R.layout.activity_main);

        // Hook y setup del Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Creamos el helper de la BBDD
        mFeedReaderDbHelper = new FeedReaderDbHelper(getApplicationContext());


        mRecyclerView = (RecyclerView) findViewById(R.id.lista);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        mRecyclerViewAdapter = new RecyclerViewAdapter(this, mFeedReaderDbHelper);
        mRecyclerView.setAdapter(mRecyclerViewAdapter);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //Se actualiza una vez al día
        int diaHoy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        //int semanaHoy = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        int diaGuardado = sharedPreferences.getInt("dia",0);

        if (diaHoy > diaGuardado) {
            //Se hace una actualización suave
            mHiloDescargasTMDB = new HiloDescargasTMDB(this, mRecyclerViewAdapter,
                    ((LinearLayout) findViewById(R.id.carga_barra)));
            mHiloDescargasTMDB.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase());

            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putInt("iniciado", 1);
            sharedPreferencesEditor.putInt("dia",diaHoy);
            sharedPreferencesEditor.apply();
        }

        mInterfaz = new Interfaz(this, mRecyclerViewAdapter);
        mInterfaz.seleccionaBotonCartelera();
        mostrarCartelera(findViewById(R.id.cartelera));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Menú de opciones creado");
        // Expande el mMenu, añade las opciones
        this.mMenu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);

            SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
            SearchView searchView = (SearchView) menu.findItem(R.id.busqueda).getActionView();
            searchView.setMaxWidth(Integer.MAX_VALUE);
            // Assumes current activity is the searchable activity
            searchView.setSearchableInfo(searchManager.getSearchableInfo(new ComponentName(this.getApplicationContext(),SearchableActivity.class)));
            searchView.setIconifiedByDefault(true); // Do not iconify the widget; expand it by default

        // Para dar color a los botones de la ActionBar
        for(int i = 0; i < mMenu.size(); i++){
            Drawable drawable = mMenu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
                //noinspection deprecation
                drawable.setColorFilter(getResources().getColor(R.color.colorAppText), PorterDuff.Mode.SRC_ATOP);
            }
        }

        //Y si es la primera ejecución y ahora mismo se están descargando cosas:
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        if (sharedPreferences.getInt("iniciado",0)<2) {
            mMenu.findItem(R.id.actualizar).setEnabled(false);
            mMenu.findItem(R.id.actualizar).setVisible(false);
            mMenu.findItem(R.id.cancelar).setEnabled(true);
            mMenu.findItem(R.id.cancelar).setVisible(true);
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putInt("iniciado", 2);
            sharedPreferencesEditor.apply();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem selectedItem) {
        Log.d(TAG, "Opción seleccionada " + selectedItem.toString());

        // Interpretamos lo seleccionado en el mMenu
        switch (selectedItem.getItemId()) {

            //Mostrar las opciones (pendiente)
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            //Actualiza las películas guardadas..
            case R.id.actualizar:

                mHiloDescargasTMDB = new HiloDescargasTMDB(this, mRecyclerViewAdapter,
                        ((LinearLayout) findViewById(R.id.carga_barra)));
                mHiloDescargasTMDB.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase());

                selectedItem.setEnabled(false);
                selectedItem.setVisible(false);
                mMenu.findItem(R.id.cancelar).setEnabled(true);
                mMenu.findItem(R.id.cancelar).setVisible(true);

                //mRecyclerViewAdapter.reiniciarPagina();
                //mInterfaz.mostrarPaginador(false);
                return true;

            case R.id.cancelar:

                mHiloDescargasTMDB.cancel(true);
                selectedItem.setEnabled(false);
                selectedItem.setVisible(false);
                mMenu.findItem(R.id.actualizar).setEnabled(true);
                mMenu.findItem(R.id.actualizar).setVisible(true);
                return true;

            case R.id.busqueda:
                SearchView searchView = (SearchView) selectedItem.getActionView();
                searchView.setMaxWidth(Integer.MAX_VALUE);
                searchView.setIconifiedByDefault(false);
                return true;
            default:
                return super.onOptionsItemSelected(selectedItem);

        }
    }

    /*
     * Métodos custom
     */

    public void retrocederPagina(View view) {
        int pagina = mRecyclerViewAdapter.getPagina();
        if (pagina > 0) {
            Log.d(TAG, "Retrocediendo a la página " + pagina);
            mRecyclerViewAdapter.pasarPagina(pagina - 1);
            ((TextView) findViewById(R.id.paginaActual)).setText(String.valueOf(pagina));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que restarle uno, porque se ha ido a la págin aanterior, y se suma uno
            //porque se ha cogido del adaptador. Así que, se queda igual.
            mInterfaz.enfocaPrimerElementoBrusco();
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    public void avanzarPagina(View view) {
        int pagina = mRecyclerViewAdapter.getPagina();
        if (pagina < mRecyclerViewAdapter.getUltPagina()-1) {
            Log.d(TAG, "Avanzando a la página " + (pagina+2));
            mRecyclerViewAdapter.pasarPagina(pagina + 1);
            ((TextView) findViewById(R.id.paginaActual)).setText(String.valueOf(pagina+2));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que sumarle uno, porque se ha ido a la págin siguiente, y otro más
            //porque se ha cogido del adaptador.
            mInterfaz.enfocaPrimerElementoBrusco();
            mRecyclerViewAdapter.notifyDataSetChanged();
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String clave) {
        if(clave.equalsIgnoreCase("pref_db")){
            mRecyclerViewAdapter.eliminarLista();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mRecyclerViewAdapter.actualizarInterfaz();
            mRecyclerViewAdapter.mostrarNoPelis();
        } else if (clave.equalsIgnoreCase("pref_pais")){
            //Cuando cambia el país se borra la mRecyclerViewAdapter anterior
            mFeedReaderDbHelper.getWritableDatabase().delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, null);
            mRecyclerViewAdapter.eliminarLista();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mRecyclerViewAdapter.actualizarInterfaz();
            mRecyclerViewAdapter.mostrarNoPelis();
        } else if (clave.equalsIgnoreCase("provider")){
            mFeedReaderDbHelper.getWritableDatabase().delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, null);
            mRecyclerViewAdapter.eliminarLista();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mRecyclerViewAdapter.actualizarInterfaz();
            mRecyclerViewAdapter.mostrarNoPelis();
        }
    }

    protected void onDestroy() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public Menu getMenu(){ return mMenu;}

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void mostrarHype(View view){

        Log.d(TAG, "Mostrando películas hypeadas");

        if (mRecyclerViewAdapter.getEstado() != RecyclerViewAdapter.HYPE){
            mInterfaz.animaListado();
            mRecyclerViewAdapter.mostrarHype();
            mInterfaz.seleccionaBotonHype();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mInterfaz.enfocaPrimerElementoBrusco();
            ((RecyclerView) findViewById(R.id.lista)).smoothScrollToPosition(0);
            mRecyclerViewAdapter.mostrarNoPelis();
        }else{
            mInterfaz.enfocaPrimerElementoSuave();
        }



    }

    public void mostrarCartelera(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        if (mRecyclerViewAdapter.getEstado() != RecyclerViewAdapter.CARTELERA) {
            mInterfaz.animaListado();
            mRecyclerViewAdapter.mostrarCartelera();
            mInterfaz.seleccionaBotonCartelera();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mInterfaz.enfocaPrimerElementoBrusco();
            mRecyclerViewAdapter.mostrarNoPelis();
        }else{
            mInterfaz.enfocaPrimerElementoSuave();
        }
    }

    public void mostrarEstrenos(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        if (mRecyclerViewAdapter.getEstado() != RecyclerViewAdapter.ESTRENOS) {
            mInterfaz.animaListado();
            mRecyclerViewAdapter.mostrarEstrenos();
            mInterfaz.seleccionaBotonEstrenos();
            mRecyclerViewAdapter.notifyDataSetChanged();
            mInterfaz.enfocaPrimerElementoBrusco();
            mRecyclerViewAdapter.mostrarNoPelis();
        }else{
            mInterfaz.enfocaPrimerElementoSuave();
        }
    }


    public void mostrarAvanzado(View view){
        mRecyclerViewAdapter.setItemExpandido(view);
    }
    public void marcarHype(View view){
        mRecyclerViewAdapter.marcarHype(view);
     }

    public void enviarCalendario(View view) {
        startActivity(mRecyclerViewAdapter.abrirCalendario());
    }

    public void abrirFicha(View view) {
        mRecyclerViewAdapter.abrirFicha();
    }

    public void abrirWeb(View view) {
        startActivity(mRecyclerViewAdapter.abrirWeb());
    }

    public void abrirMenuCompartir(View view) {
        mRecyclerViewAdapter.abrirMenuCompartir();
    }

    public void verCines(View view){
        startActivity(mRecyclerViewAdapter.verCines());
    }
}