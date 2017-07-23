package molina.alfonso.hype;

import android.os.Bundle;
import android.os.StrictMode;
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

    /*
     * Métodos override
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // Fijamos el layout a usar
        setContentView(R.layout.activity_main);

        // Relajamos políticas de Threads
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Hook y setup del Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Creamos el helper de la BBDD
        mDbHelper = new FeedReaderDbHelper(getApplicationContext());

        // Borrado de la BBDD
        //mDbHelper.getWritableDatabase().execSQL("delete from "+ TABLE_NAME);

        // Hook de la lista
        ListView lista = (ListView) findViewById(R.id.lista);

        listaAdapter = new ListaModificadaAdapter(getApplicationContext(), R.layout.fila_pelicula3, mDbHelper.getReadableDatabase(),((LinearLayout) findViewById(R.id.carga_barra)),((TextView) findViewById(R.id.carga_mensaje)));

        // Setup de la lista
        lista.setAdapter(listaAdapter);
        lista.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lista.setItemsCanFocus(false);
        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listaAdapter.setExpandido(position);
                listaAdapter.notifyDataSetChanged();
            }
        });

        //Si la lista está vacía, desaparece la barra de navegación y aparece un texto diciéndolo.
        /*if (listaAdapter.getCount()==0) {
            findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
            findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
        }*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        // Expande el menu, añade las opciones
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");

        // Interpretamos lo seleccionado en el menu
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;

            case R.id.action_favorite:
                // Lanzamos el Thread que maneja la lista
                HiloDescargarEstrenos hilo = new HiloDescargarEstrenos(listaAdapter,((LinearLayout) findViewById(R.id.carga_barra)),((TextView) findViewById(R.id.carga_mensaje)));
                hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());

                //Estoy habría que ponerlo cuando termine de actualizar, y no al principio, pero estoy cansado ya todo
                findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                findViewById(R.id.nopelis).setVisibility(View.INVISIBLE);
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /*
     * Métodos custom
     */


    public void mostrarHype(View view) {
        if(listaAdapter.toogleHype()){
            findViewById(R.id.navegacion).setVisibility(View.GONE);
        } else if (listaAdapter.getCount()!=0)
            findViewById(R.id.navegacion).setVisibility(View.VISIBLE);

        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();
        ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);


    }

    public void pasarPaginaAtras(View view) {
        int pagina = listaAdapter.getPagina();
        findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
        if (pagina > 0) {
            listaAdapter.pasarPagina(pagina - 1);
            ((TextView) findViewById(R.id.actualPageText)).setText(""+pagina);
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que restarle uno, porque se ha ido a la págin aanterior, y se suma uno
            //porque se ha cogido del adaptador. Así que, se queda igual.
            ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);
            if (pagina == 1) {
                findViewById(R.id.previousPageButton).setVisibility(View.INVISIBLE);
            }
        }

        listaAdapter.notifyDataSetChanged();
    }

    public void pasarPaginaAdelante(View view) {
        int pagina = listaAdapter.getPagina();
        findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
        if (pagina < listaAdapter.getMaxPaginas()) {
            listaAdapter.pasarPagina(pagina + 1);
            ((TextView) findViewById(R.id.actualPageText)).setText(""+(pagina+2));
            //La pagina en el adaptador va de 0 a la que sea, en el texto que sale empieza por uno.
            //Así que hay que sumarle uno, porque se ha ido a la págin siguiente, y otro más
            //porque se ha cogido del adaptador.
            ((ListView) findViewById(R.id.lista)).smoothScrollToPosition(0);
            if (pagina+2 == listaAdapter.getMaxPaginas()) {
                findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
            }
        }

        listaAdapter.notifyDataSetChanged();
    }
}