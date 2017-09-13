package com.clacksdepartment.hype;

import android.animation.LayoutTransition;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, FichaFragment.OnFragmentInteractionListener{

    /*
     * Declaración de variables
     */

    // TODO: Refinar icono de Hype
    // TODO: Crear icono para la aplicación
    // TODO: Mejorar (y completar) traducción "countrie"

    private static final String TAG = "MainActivity";

   // private ListaModificadaAdapter mListaModificadaAdapter;
    private FeedReaderDbHelper mFeedReaderDbHelper;
    private HiloDescargas mHiloDescargas;
    private Interfaz mInterfaz;
    private Menu mMenu;


    private RecyclerView mRecyclerView;
    private RecyclerViewAdapter mListaModificadaAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    /*
     * Métodos override
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Actividad principal creada");

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        // Fijamos el layout a usar
        setContentView(R.layout.activity_main);

        // Hook y setup del Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
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
        mListaModificadaAdapter = new RecyclerViewAdapter(this, R.layout.fila, mFeedReaderDbHelper);
        mRecyclerView.setAdapter(mListaModificadaAdapter);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //Se actualiza una vez al día
        int diaHoy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        //int semanaHoy = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR);
        int diaGuardado = sharedPreferences.getInt("dia",0);
        if (diaHoy > diaGuardado) {
            //Se hace una actualización suave
            mHiloDescargas = new HiloDescargas(this, mListaModificadaAdapter,
                    ((LinearLayout) findViewById(R.id.carga_barra)),false);

            mHiloDescargas.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase());

            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putInt("iniciado", 1);
            sharedPreferencesEditor.putInt("dia",diaHoy);
            sharedPreferencesEditor.apply();
        }

        mInterfaz = new Interfaz(this, mListaModificadaAdapter);
        mInterfaz.seleccionaBotonCartelera();

        mostrarCartelera(findViewById(R.id.cartelera));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Menú de opciones creado");
        // Expande el mMenu, añade las opciones
        this.mMenu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);

        // Para dar color a los botones de la ActionBar
        for(int i = 0; i < mMenu.size(); i++){
            Drawable drawable = mMenu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
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
                mHiloDescargas = new HiloDescargas(this, mListaModificadaAdapter,
                        ((LinearLayout) findViewById(R.id.carga_barra)),true);
                // Lanzamos el Thread que descargará la información.
                mHiloDescargas.execute(mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase());
                selectedItem.setEnabled(false);
                selectedItem.setVisible(false);
                mMenu.findItem(R.id.cancelar).setEnabled(true);
                mMenu.findItem(R.id.cancelar).setVisible(true);

                mListaModificadaAdapter.reiniciarPagina();
                mInterfaz.mostrarPaginador(false);
                return true;

            case R.id.cancelar:

                mHiloDescargas.cancel(true);
                selectedItem.setEnabled(false);
                selectedItem.setVisible(false);
                mMenu.findItem(R.id.actualizar).setEnabled(true);
                mMenu.findItem(R.id.actualizar).setVisible(true);
                return true;

            default:
                return super.onOptionsItemSelected(selectedItem);

        }
    }

    /*
     * Métodos custom
     */

    public void retrocederPagina(View view) {
        int pagina = mListaModificadaAdapter.getPagina();
        Log.d(TAG, "Retrocediendo a la página " + pagina);
        findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
        if (pagina > 0) {
            mListaModificadaAdapter.pasarPagina(pagina - 1);
            ((TextView) findViewById(R.id.paginaActual)).setText(String.valueOf(pagina));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que restarle uno, porque se ha ido a la págin aanterior, y se suma uno
            //porque se ha cogido del adaptador. Así que, se queda igual.
            mInterfaz.enfocaPrimerElementoBrusco();
            if (pagina == 1) {
                findViewById(R.id.previousPageButton).setVisibility(View.INVISIBLE);
            }
        }

        mListaModificadaAdapter.notifyDataSetChanged();
    }

    public void avanzarPagina(View view) {
        int pagina = mListaModificadaAdapter.getPagina();
        Log.d(TAG, "Avanzando a la página " + (pagina+2));
        findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
        if (pagina < mListaModificadaAdapter.getUltPagina()) {
            mListaModificadaAdapter.pasarPagina(pagina + 1);
            ((TextView) findViewById(R.id.paginaActual)).setText(String.valueOf(pagina+2));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que sumarle uno, porque se ha ido a la págin siguiente, y otro más
            //porque se ha cogido del adaptador.
            mInterfaz.enfocaPrimerElementoBrusco();
            if (pagina+2 == mListaModificadaAdapter.getUltPagina()) {
                findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
            }
        }

        mListaModificadaAdapter.notifyDataSetChanged();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String clave) {
        if(clave.equalsIgnoreCase("pref_db")){
            mListaModificadaAdapter.eliminarLista();
            mListaModificadaAdapter.notifyDataSetChanged();
            mListaModificadaAdapter.actualizarInterfaz();
            mListaModificadaAdapter.noHayPelis();
        } else if (clave.equalsIgnoreCase("pref_pais")){
            //Cuando cambia el país se borra la mListaModificadaAdapter anterior
            mFeedReaderDbHelper.getWritableDatabase().delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, null);
            mListaModificadaAdapter.eliminarLista();
            mListaModificadaAdapter.notifyDataSetChanged();
            mListaModificadaAdapter.actualizarInterfaz();
            mListaModificadaAdapter.noHayPelis();
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

        if (mListaModificadaAdapter.getEstado() != ListaModificadaAdapter.HYPE){
            mInterfaz.animaListado();
            mListaModificadaAdapter.mostrarHype();
            mInterfaz.seleccionaBotonHype();
            mListaModificadaAdapter.notifyDataSetChanged();
            mInterfaz.enfocaPrimerElementoBrusco();
            ((RecyclerView) findViewById(R.id.lista)).smoothScrollToPosition(0);
        }else{
            mInterfaz.enfocaPrimerElementoSuave();
        }



    }

    public void mostrarCartelera(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        if (mListaModificadaAdapter.getEstado() != ListaModificadaAdapter.CARTELERA) {
            mInterfaz.animaListado();
            mListaModificadaAdapter.mostrarCartelera();
            mInterfaz.seleccionaBotonCartelera();
            mListaModificadaAdapter.notifyDataSetChanged();
            mInterfaz.enfocaPrimerElementoBrusco();
        }else{
            mInterfaz.enfocaPrimerElementoSuave();
        }
    }

    public void mostrarEstrenos(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        if (mListaModificadaAdapter.getEstado() != ListaModificadaAdapter.ESTRENOS) {

            mInterfaz.animaListado();
            mListaModificadaAdapter.mostrarEstrenos();
            mInterfaz.seleccionaBotonEstrenos();
            mListaModificadaAdapter.notifyDataSetChanged();
            mInterfaz.enfocaPrimerElementoBrusco();

        }else{
            mInterfaz.enfocaPrimerElementoSuave();
        }
    }


    public void mostrarAvanzado(View view){
        mListaModificadaAdapter.setItemExpandido(view);
    }
    public void marcarHype(View view){
        mListaModificadaAdapter.marcarHype(view);
     }

    public void enviarCalendario(View view) {
        startActivity(mListaModificadaAdapter.abrirCalendario());
    }

    public void abrirFicha(View view) {
        mListaModificadaAdapter.abrirFicha();
    }

    public void abrirWeb(View view) {
        startActivity(mListaModificadaAdapter.abrirWeb());
    }

    public void abrirMenuCompartir(View view) {
        mListaModificadaAdapter.abrirMenuCompartir();
    }
}