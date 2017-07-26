package molina.alfonso.hype;

import android.os.Bundle;
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

public class MainActivity extends AppCompatActivity {

    /*
     * Declaración de variables
     */

    // Etiqueta para logs
    private static final String TAG = "MainActivity";

    // Adaptador de la lista
    private ListaModificadaAdapter listaAdapter;
    // Helper para manipular la BBDD
    private FeedReaderDbHelper mDbHelper;

    private float x1=0;
    private float x2=0;
    static final int MIN_DISTANCE = 150;


    /*
     * Métodos override
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Actividad principal creada");

        // Fijamos el layout a usar
        setContentView(R.layout.activity_main);

        // Relajamos políticas de Threads
        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        // Hook y setup del Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "Menú de opciones creado");
        // Expande el menu, añade las opciones
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "Opción seleccionada " + item.toString());

        // Interpretamos lo seleccionado en el menu
        switch (item.getItemId()) {

            //Mostrar las opciones (pendiente)
            case R.id.action_settings:
                return true;

            //Actualiza las películas guardadas..
            case R.id.actualizar:
                // Lanzamos el Thread que descargará la información.
                HiloDescargarEstrenos hilo = new HiloDescargarEstrenos(listaAdapter,
                        ((LinearLayout) findViewById(R.id.carga_barra)),
                        ((TextView) findViewById(R.id.carga_mensaje)));

                hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /*
     * Métodos custom
     */


    //Muestra las películas guardadas
    public void mostrarHype(View view) {
        Log.d(TAG, "Mostrando películas guardadas");

        if(listaAdapter.toogleHype()){
            findViewById(R.id.navegacion).setVisibility(View.GONE);
            //Si no hay ninguna guardada, se muestra un mensaje
            if (listaAdapter.getCount()== 0){
                findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.nopelis)).setText("\n\nNinguna película guardada.\nPor ahora.");
            }


        } else if (listaAdapter.getCount()!=0) {
            findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
            findViewById(R.id.nopelis).setVisibility(View.GONE);
        }

        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();
        ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);


    }

    public void pasarPaginaAtras(View view) {
        int pag = listaAdapter.getPagina();
        Log.d(TAG, "Retrocediendo a la página " + pag);
        findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
        if (pag > 0) {
            listaAdapter.pasarPagina(pag - 1);
            ((TextView) findViewById(R.id.actualPageText)).setText(String.valueOf(pag));
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
            ((TextView) findViewById(R.id.actualPageText)).setText(String.valueOf(pag+2));
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
}