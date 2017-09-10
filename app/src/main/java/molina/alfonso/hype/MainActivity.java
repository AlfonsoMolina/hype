package molina.alfonso.hype;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
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

    private ListaModificadaAdapter mListaModificadaAdapter;
    private FeedReaderDbHelper mFeedReaderDbHelper;
    private HiloDescargas mHiloDescargas;
    private Interfaz mInterfaz;
    private Menu mMenu;

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
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Creamos el helper de la BBDD
        mFeedReaderDbHelper = new FeedReaderDbHelper(getApplicationContext());

        // Hook de la mListaModificadaAdapter
        ListView listView = (ListView) findViewById(R.id.lista);

        //Se le manda a la mListaModificadaAdapter esta actividad, para poder modificar la interfaz,
        //el layout de la row y la bbdd
        mListaModificadaAdapter = new ListaModificadaAdapter(this, R.layout.fila, mFeedReaderDbHelper);

        // Setup de la mListaModificadaAdapter
        listView.setAdapter(mListaModificadaAdapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listView.setItemsCanFocus(false);

        //Al pulsar en una fila se expande y muestra más información.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mListaModificadaAdapter.setItemExpandido(position);
                mListaModificadaAdapter.notifyDataSetChanged();
            }
        });

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //Se actualiza una vez cada dos días
        //Cojo la fecha actual:
        String ano = "" + Calendar.getInstance().get(Calendar.YEAR);
        int mesTemp = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int diaTemp = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        String mes;
        String dia;

        if (mesTemp < 10)
            mes = "0" + mesTemp;
        else
            mes = "" + mesTemp;

        if (diaTemp < 10)
            dia = "0" + diaTemp;
        else
            dia = "" + diaTemp;

        String fechaHoy = ano + '/' + mes + '/' + dia;

        //Y la guardada:
        String fechaGuardada = sharedPreferences.getString("fecha","01/01/1990");
        if (fechaHoy.compareTo(fechaGuardada) > 0) {
            //Se hace una actualización suave
            mHiloDescargas = new HiloDescargas(this, mListaModificadaAdapter,
                    ((LinearLayout) findViewById(R.id.carga_barra)),false);

            mHiloDescargas.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase());

            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putInt("iniciado", 1);
            sharedPreferencesEditor.putString("fecha",fechaHoy);
            sharedPreferencesEditor.apply();
        }


            //Si es el primer uso se iniliaciza (parte 1)
        if (sharedPreferences.getInt("iniciado",0)==0){

            mHiloDescargas = new HiloDescargas(this, mListaModificadaAdapter,
                    ((LinearLayout) findViewById(R.id.carga_barra)),false);

            mHiloDescargas.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase());

            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.putInt("iniciado", 1);
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
            ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);
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
            ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);
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

        mListaModificadaAdapter.mostrarHype();
        mInterfaz.seleccionaBotonHype();
        mInterfaz.mostrarPaginador(false);

        //Si no hay ninguna guardada, se muestra un mensaje
        if (mListaModificadaAdapter.getCount()== 0){
            mInterfaz.mostrarNoHayPelis(true);
        } else {
            mInterfaz.mostrarNoHayPelis(false);
        }

        mListaModificadaAdapter.setItemExpandido(-1);
        mListaModificadaAdapter.notifyDataSetChanged();
        ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);

    }

    public void mostrarCartelera(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        mListaModificadaAdapter.mostrarCartelera();
        mInterfaz.seleccionaBotonCartelera();
        mInterfaz.mostrarPaginador(false);

        if (mListaModificadaAdapter.getCount()== 0){
            mInterfaz.mostrarPaginador(false);
            mInterfaz.mostrarNoHayPelis(true);
        } else if (mListaModificadaAdapter.getUltPagina() > 1){
            mInterfaz.mostrarPaginador(true);
            mInterfaz.mostrarNoHayPelis(false);
        } else {
            mInterfaz.mostrarPaginador(false);
            mInterfaz.mostrarNoHayPelis(false);
        }

        mListaModificadaAdapter.setItemExpandido(-1);
        mListaModificadaAdapter.notifyDataSetChanged();
        mInterfaz.enfocaPrimerElemento();

    }

    public void mostrarEstrenos(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        mListaModificadaAdapter.mostrarEstrenos();
        mInterfaz.seleccionaBotonEstrenos();
        mInterfaz.mostrarPaginador(false);

        if (mListaModificadaAdapter.getCount()== 0){
            mInterfaz.mostrarPaginador(false);
            mInterfaz.mostrarNoHayPelis(true);
        } else if (mListaModificadaAdapter.getUltPagina() > 1){
            mInterfaz.mostrarPaginador(true);
            mInterfaz.mostrarNoHayPelis(false);
        } else {
            mInterfaz.mostrarPaginador(false);
            mInterfaz.mostrarNoHayPelis(false);
        }

        mListaModificadaAdapter.setItemExpandido(-1);
        mListaModificadaAdapter.notifyDataSetChanged();
        mInterfaz.enfocaPrimerElemento();

    }

}