package molina.alfonso.hype;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
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

    private ListaModificadaAdapter listaAdapter;

    private FeedReaderDbHelper mDbHelper;

    // Create a new map of values, where column names are the keys

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mDbHelper = new FeedReaderDbHelper(getApplicationContext());

        //mDbHelper.getWritableDatabase().execSQL("delete from "+ TABLE_NAME);   //Para cuando haya que cambiar la bbdd.
        ListView lista = (ListView) findViewById(R.id.lista);
        listaAdapter = new ListaModificadaAdapter(getApplicationContext(), R.layout.fila_pelicula2, mDbHelper.getReadableDatabase());
        lista.setAdapter(listaAdapter);
        lista.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        lista.setItemsCanFocus(false);

        lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Pelicula p = listaAdapter.getPelicula(position);
                p.setisPressed(!p.getisPressed());
                if (p.getisPressed()) {
                    Calendar beginTime = Calendar.getInstance();
                    String [] f = p.getFecha_estreno().split("/");
                    int[] fecha = {Integer.parseInt(f[0]), Integer.parseInt(f[1]),Integer.parseInt(f[2])};
                    beginTime.set(fecha[0], fecha[1]-1, fecha[2], 0, 0);
                   // Calendar endTime = Calendar.getInstance();
                  //  endTime.set(fecha[0], fecha[1]-1, fecha[2], 1, 0);
                    Intent intent = new Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                            .putExtra("allDay", true)
                            .putExtra(CalendarContract.Events.TITLE, p.getTitulo())
                            .putExtra(CalendarContract.Events.DESCRIPTION, p.getSinopsis());
//                            .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                    //                        .putExtra(Intent.EXTRA_EMAIL, "rowan@example.com,trevor@example.com");
                    startActivity(intent);
                }

                SQLiteDatabase db = mDbHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                String h;
                if (p.getisPressed()) {
                    h = "T";
                } else
                    h = "F";

                values.put(FeedEntry.COLUMN_HYPE, h);

                String selection = FeedEntry.COLUMN_REF + " LIKE ?";
                String[] selectionArgs = {p.getEnlace()};

                int count = db.update(
                        FeedReaderContract.FeedEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

                listaAdapter.notifyDataSetChanged();
            }

        });
    }


    public void cargarHTML(View view) throws IOException {
        Hilo hilo = new Hilo(listaAdapter);
        hilo.execute(mDbHelper.getReadableDatabase(), mDbHelper.getWritableDatabase());
    }

}