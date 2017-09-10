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

public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener, FichaFragment.OnFragmentInteractionListener{

    /*
     * Declaración de variables
     */

    // TODO: Refinar icono de Hype
    // TODO: Crear icono para la aplicación
    // TODO: Añadir "sección" cartelera (descargar la info e ya)
    // TODO: Facilitar acceso a sección Hype (y señalizarlo)
    // TODO: Mejorar (y completar) traducción "countrie"

    // Etiqueta para logs
    private static final String TAG = "MainActivity";

    // Adaptador de la lista
    private ListaModificadaAdapter listaAdapter;
    // Helper para manipular la BBDD
    private FeedReaderDbHelper mDbHelper;

    private HiloDescargarEstrenos hilo;
    private Navegador navegador;

    private Menu menu;
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
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        //getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Creamos el helper de la BBDD
        mDbHelper = new FeedReaderDbHelper(getApplicationContext());

        // Hook de la lista
        ListView lista = (ListView) findViewById(R.id.lista);

        //Se le manda a la lista esta actividad, para poder modificar la interfaz,
        //el layout de la row y la bbdd
        listaAdapter = new ListaModificadaAdapter(this, R.layout.fila, mDbHelper);

        // Setup de la lista
        lista.setAdapter(listaAdapter);
        lista.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lista.setItemsCanFocus(false);

        //Al pulsar en una fila se expande y muestra más información.
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listaAdapter.setExpandido(position);
                listaAdapter.notifyDataSetChanged();
            }
        });

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);


        //Si es el primer uso se iniliaciza (parte 1)
        if (sharedPref.getInt("iniciado",0)==0){

            HiloDescargarEstrenos hilo = new HiloDescargarEstrenos(this, listaAdapter,
                    ((LinearLayout) findViewById(R.id.carga_barra)));

            hilo.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());

            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("iniciado", 1);
            editor.apply();
        }

        navegador = new Navegador(this, listaAdapter);
        navegador.seleccionaCartelera();

        onClickCartelera(findViewById(R.id.cartelera));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Menú de opciones creado");
        // Expande el menu, añade las opciones
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu, menu);

        // Para dar color a los botones de la ActionBar
        for(int i = 0; i < menu.size(); i++){
            Drawable drawable = menu.getItem(i).getIcon();
            if(drawable != null) {
                drawable.mutate();
                drawable.setColorFilter(getResources().getColor(R.color.colorAppText), PorterDuff.Mode.SRC_ATOP);
            }
        }

        //Y si es la primera ejecución y ahora mismo se están descargando cosas:
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        if (sharedPref.getInt("iniciado",0)<2) {
            menu.findItem(R.id.actualizar).setEnabled(false);
            menu.findItem(R.id.actualizar).setVisible(false);
            menu.findItem(R.id.cancelar).setEnabled(true);
            menu.findItem(R.id.cancelar).setVisible(true);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("iniciado", 2);
            editor.apply();
        }


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Opción seleccionada " + item.toString());

        // Interpretamos lo seleccionado en el menu
        switch (item.getItemId()) {

            //Mostrar las opciones (pendiente)
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;

            //Actualiza las películas guardadas..
            case R.id.actualizar:
                hilo = new HiloDescargarEstrenos(this, listaAdapter,
                        ((LinearLayout) findViewById(R.id.carga_barra)));
                // Lanzamos el Thread que descargará la información.
                hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());
                item.setEnabled(false);
                item.setVisible(false);
                menu.findItem(R.id.cancelar).setEnabled(true);
                menu.findItem(R.id.cancelar).setVisible(true);
                return true;

            case R.id.cancelar:

                hilo.cancel(true);
                item.setEnabled(false);
                item.setVisible(false);
                menu.findItem(R.id.actualizar).setEnabled(true);
                menu.findItem(R.id.actualizar).setVisible(true);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /*
     * Métodos custom
     */

    public void pasarPaginaAtras(View view) {
        int pag = listaAdapter.getPagina();
        Log.d(TAG, "Retrocediendo a la página " + pag);
        findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
        if (pag > 0) {
            listaAdapter.pasarPagina(pag - 1);
            ((TextView) findViewById(R.id.paginaActual)).setText(String.valueOf(pag));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que restarle uno, porque se ha ido a la págin aanterior, y se suma uno
            //porque se ha cogido del adaptador. Así que, se queda igual.
            ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);
            if (pag == 1) {
                findViewById(R.id.previousPageButton).setVisibility(View.INVISIBLE);
            }
        }

        listaAdapter.notifyDataSetChanged();
    }

    public void pasarPaginaAdelante(View view) {
        int pag = listaAdapter.getPagina();
        Log.d(TAG, "Avanzando a la página " + (pag+2));
        findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
        if (pag < listaAdapter.getUltPagina()) {
            listaAdapter.pasarPagina(pag + 1);
            ((TextView) findViewById(R.id.paginaActual)).setText(String.valueOf(pag+2));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que sumarle uno, porque se ha ido a la págin siguiente, y otro más
            //porque se ha cogido del adaptador.
            ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);
            if (pag+2 == listaAdapter.getUltPagina()) {
                findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
            }
        }

        listaAdapter.notifyDataSetChanged();
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equalsIgnoreCase("pref_db")){
            listaAdapter.eliminarLista();
            listaAdapter.notifyDataSetChanged();
            listaAdapter.actualizarInterfaz();
            listaAdapter.noHayPelis();
        } else if (key.equalsIgnoreCase("pref_pais")){
            //Cuando cambia el país se borra la lista anterior
            mDbHelper.getWritableDatabase().delete(FeedReaderContract.FeedEntryEstrenos.TABLE_NAME, null, null);
            listaAdapter.eliminarLista();
            listaAdapter.notifyDataSetChanged();
            listaAdapter.actualizarInterfaz();
            listaAdapter.noHayPelis();
        }
    }

    protected void onDestroy() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public Menu getMenu(){ return menu;}

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void onClickHype(View view){

        Log.d(TAG, "Mostrando películas hypeadas");

        listaAdapter.mostrarHype();
        navegador.seleccionaHype();
        navegador.mostrarPaginador(false);

        //Si no hay ninguna guardada, se muestra un mensaje
        if (listaAdapter.getCount()== 0){
            navegador.mostrarNoPelis(true);
        } else {
            navegador.mostrarNoPelis(false);
        }

        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();
        ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);

    }

    public void onClickCartelera(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        listaAdapter.mostrarCartelera();
        navegador.seleccionaCartelera();
        navegador.mostrarPaginador(false);

        if (listaAdapter.getCount()== 0){
            navegador.mostrarPaginador(false);
            navegador.mostrarNoPelis(true);
        } else if (listaAdapter.getUltPagina() > 1){
            navegador.mostrarPaginador(true);
            navegador.mostrarNoPelis(false);
        } else {
            navegador.mostrarPaginador(false);
            navegador.mostrarNoPelis(false);
        }

        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();
        ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);


    }

    public void onClickEstrenos(View view){

        Log.d(TAG, "Mostrando películas de estreno");

        listaAdapter.mostrarEstrenos();
        navegador.seleccionaEstrenos();
        navegador.mostrarPaginador(false);

        if (listaAdapter.getCount()== 0){
            navegador.mostrarPaginador(false);
            navegador.mostrarNoPelis(true);
        } else if (listaAdapter.getUltPagina() > 1){
            navegador.mostrarPaginador(true);
            navegador.mostrarNoPelis(false);
        } else {
            navegador.mostrarPaginador(false);
            navegador.mostrarNoPelis(false);
        }

        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();
        ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);

    }

}