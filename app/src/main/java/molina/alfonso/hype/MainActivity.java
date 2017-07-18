package molina.alfonso.hype;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import molina.alfonso.hype.FeedReaderContract.FeedEntry;

import static java.security.AccessController.getContext;
import static molina.alfonso.hype.FeedReaderContract.FeedEntry.TABLE_NAME;

/*
4. poner en el calendario
5. Que salga un popup
6. Añadir un enlace a FA

ponerlo mas bonito en general
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ListaModificadaAdapter listaAdapter;

    private FeedReaderDbHelper mDbHelper;

    // Create a new map of values, where column names are the keys

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        mDbHelper = new FeedReaderDbHelper(getApplicationContext());

        //mDbHelper.getWritableDatabase().execSQL("delete from "+ TABLE_NAME);   //Para cuando haya que cambiar la bbdd.
        ListView lista = (ListView) findViewById(R.id.lista);
        listaAdapter = new ListaModificadaAdapter(getApplicationContext(), R.layout.fila_pelicula3, mDbHelper);
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

        getSupportActionBar().setDisplayShowTitleEnabled(false);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public void cargarHTML() throws IOException {
        Log.d(TAG, "cargarHTML");
        Hilo hilo = new Hilo(listaAdapter);
        hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;

            case R.id.action_favorite:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                Hilo hilo = new Hilo(listaAdapter);
                hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }


    public void mostrarHype(View view) {
        listaAdapter.toogleHype();
        listaAdapter.setExpandido(-1);
        listaAdapter.notifyDataSetChanged();

    }
}