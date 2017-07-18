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
import android.widget.ListView;

import java.io.IOException;

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

        // Creación del adaptador de la lista
        listaAdapter = new ListaModificadaAdapter(getApplicationContext(), R.layout.fila_pelicula3, mDbHelper);

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
                Hilo hilo = new Hilo(listaAdapter);
                hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());
                return true;

            default:
                return super.onOptionsItemSelected(item);

        }
    }

    /*
     * Métodos custom
     */

    public void cargarHTML() throws IOException {
        Log.d(TAG, "cargarHTML");
        Hilo hilo = new Hilo(listaAdapter);
        hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());
    }

    public void mostrarHype(View view) {
        listaAdapter.toogleHype();
        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();
    }

}