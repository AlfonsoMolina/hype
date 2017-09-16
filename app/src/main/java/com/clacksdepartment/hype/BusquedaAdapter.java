package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Usuario on 15/09/2017.
 */

public class BusquedaAdapter extends  RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "BusquedaAdapter";

    private final int resourceID;
    private final SearchableActivity mActivity;

    private ArrayList<Pelicula> mListaBusqueda;
    private FeedReaderDbHelper mDB;
    private FragmentManager mFragmentManager;

    private int itemExpandido = -1;

    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public RelativeLayout mView;

        public ViewHolder(RelativeLayout v) {
            super(v);
            mView = v;

        }
    }
    // Provide a suitable constructor (depends on the kind of dataset)
    public BusquedaAdapter (SearchableActivity searchableActivity, int resourceID, FeedReaderDbHelper db) {

        mListaBusqueda = new ArrayList<>();
        this.resourceID = resourceID;
        this.mActivity = searchableActivity;
        mFragmentManager = searchableActivity.getSupportFragmentManager();
        this.mDB = db;

        RecyclerView.ItemAnimator animator = ((RecyclerView) mActivity.findViewById(R.id.lista)).getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fila, parent, false);
        // set the view's size, margins, paddings and layout parameters
        RecyclerViewAdapter.ViewHolder vh = new RecyclerViewAdapter.ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerViewAdapter.ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Pelicula pelicula = mListaBusqueda.get(position);

        View filaView = holder.mView;

        ((TextView) filaView.findViewById(R.id.titulo)).setText(pelicula.getTitulo());
        ((TextView) filaView.findViewById(R.id.estreno)).setText(pelicula.getEstrenoLetras());
        ((ImageView) filaView.findViewById(R.id.portada)).setImageBitmap(pelicula.getPortada());

        if (pelicula.getHype()) {
            filaView.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
        } else
            filaView.findViewById(R.id.hype_msg).setVisibility(View.GONE);

        View avanzado = filaView.findViewById(R.id.avanzado);

        if (position == itemExpandido) {
            avanzado.setVisibility(View.VISIBLE);
            ((TextView) avanzado.findViewById(R.id.av_sinopsis)).setText(pelicula.getSinopsis());

            if (pelicula.getHype()) {
                ((ImageButton) avanzado.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                ((ImageButton) avanzado.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

        } else
            avanzado.setVisibility(View.GONE);

        Log.v(TAG, "Añadiendo película " + pelicula.getTitulo() + " a la vista número " + position);
    }

    @Override
    public int getItemCount() {
        return mListaBusqueda.size();
    }

    void  setItemExpandido(View view){
        //Encontramos la posición del elemento.
        String titulo = (String) ((TextView) view.findViewById(R.id.titulo)).getText();
        int posicionAntigua = itemExpandido;
        int posicion = 0;

        for (Pelicula p : mListaBusqueda) {
                if (p.getTitulo().equals(titulo)) {
                    break;
                }
                posicion++;
            }

        if (itemExpandido == posicion){
            itemExpandido = -1;
            Log.d(TAG, "Contrayendo  elemento " + posicion);
        } else {

            itemExpandido = posicion;
            Log.d(TAG, "Expandiendo  elemento " + posicion);
        }
        if(posicionAntigua != -1)
            notifyItemChanged(posicionAntigua);
        notifyItemChanged(itemExpandido);
        if (itemExpandido != -1) {
            ((RecyclerView) mActivity.findViewById(R.id.lista)).smoothScrollToPosition(itemExpandido);
        }
    }

    public void buscar(String query){

        mListaBusqueda.clear();

        String l, t, s, e, f, h;
        byte[] p_byte;
        Bitmap p_bitmap;
        String[] q = {"%" +query+"%"};

        SQLiteDatabase dbr = mDB.getReadableDatabase();

        Log.d(TAG,"a");
        String []projection2 = {
                FeedReaderContract.FeedEntryCartelera._ID,
                FeedReaderContract.FeedEntryCartelera.COLUMN_TITULO,
                FeedReaderContract.FeedEntryCartelera.COLUMN_PORTADA,
                FeedReaderContract.FeedEntryCartelera.COLUMN_REF,
                FeedReaderContract.FeedEntryCartelera.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntryCartelera.COLUMN_ESTRENO,
                FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA,
                FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE,
        };
        Log.d(TAG,"aa");

        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntryCartelera.TABLE_NAME,                     // The table to query
                projection2,                               // The columns to return
                FeedReaderContract.FeedEntryCartelera.COLUMN_TITULO + " LIKE ?",  // The columns for the WHERE clause
                q ,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA + " ASC"                                    // The sort order
        );
        Log.d(TAG,"aaa " + cursor.getCount());

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_FECHA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_TITULO));
            l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_REF));
            p_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_PORTADA));
            s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_SINOPSIS));
            e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_ESTRENO));
            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE));

            p_bitmap = BitmapFactory.decodeByteArray(p_byte, 0, p_byte.length);

            mListaBusqueda.add(new Pelicula(l, p_bitmap, t, s, e, f, h.equalsIgnoreCase("T")));

            Log.d(TAG, "Encontrada película Cartelera: " + t + ".");
        }

        Log.d(TAG,"aaaa");

        //Se lee la bbdd y se guardan los elementos en cursor
        String[] projection = {
                FeedReaderContract.FeedEntryEstrenos._ID,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_REF,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_CORTO
        };

        cursor = dbr.query(
                FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO + " LIKE ?",  // The columns for the WHERE clause
                q ,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA + " ASC"                                    // The sort order
        );

        Log.d(TAG,"aaaaa " + cursor.getCount());

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_FECHA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO));
            l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_REF));
            p_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA));
            s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS));
            e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO));
            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE));

            p_bitmap = BitmapFactory.decodeByteArray(p_byte, 0, p_byte.length);

            mListaBusqueda.add(new Pelicula(l, p_bitmap, t, s, e, f, h.equalsIgnoreCase("T")));

            Log.d(TAG, "Encontrada película Estrenos: " + t + ".");
        }
        cursor.close();

        Log.d(TAG,"aaaaaa ");

        notifyDataSetChanged();
    }


    public void abrirFicha(){
        Log.i(TAG, "Pulsado botón de abrir fichaFragment");
        Pelicula pelicula = mListaBusqueda.get(itemExpandido);
        FichaFragment fichaFragment = FichaFragment.newInstance(pelicula.getTitulo(), pelicula.getEnlace());

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.setCustomAnimations(R.anim.abrir_ficha, R.anim.cerrar_ficha, R.anim.abrir_ficha, R.anim.cerrar_ficha);

        fragmentTransaction.replace(R.id.ficha_container, fichaFragment).addToBackStack(null).commit();

    }


    //Envía la película al calendario en forma de evento
    public Intent abrirCalendario(){
        Pelicula pelicula = mListaBusqueda.get(itemExpandido);

        Calendar beginTime = Calendar.getInstance();
        String [] f = pelicula.getEstrenoFecha().split("/");
        int[] fecha = {Integer.parseInt(f[0]), Integer.parseInt(f[1]),Integer.parseInt(f[2])};
        beginTime.set(fecha[0], fecha[1]-1, fecha[2], 0, 0);
        // Primero comprobar si puedo editar el evento.
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra("allDay", true)
                .putExtra(CalendarContract.Events.TITLE, pelicula.getTitulo())
                .putExtra(CalendarContract.Events.DESCRIPTION, pelicula.getSinopsis());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        Log.d(TAG, "Solicitando al calendario almacenar la película " + pelicula.getTitulo());

        return intent;
    }

    //Guarda la película
    public void marcarHype (View v) {

        Pelicula pelicula = mListaBusqueda.get(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Hype\" en película " + pelicula.getTitulo());

        SQLiteDatabase dbw = mDB.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        String isHyped;

        pelicula.setHype(!pelicula.getHype());

        if (pelicula.getHype()) {
            isHyped = "T";
            ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_black_24dp);
        } else {
            isHyped = "F";
            ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }

        // Primero intentamos en la bbdd de cartelera...
        contentValues.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, isHyped);

        String selection = FeedReaderContract.FeedEntryCartelera.COLUMN_REF + " LIKE ?";
        String[] selectionArgs = {pelicula.getEnlace()};

        dbw.update(
                FeedReaderContract.FeedEntryCartelera.TABLE_NAME,
                contentValues,
                selection,
                selectionArgs);

        contentValues.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, isHyped);

        selection = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + " LIKE ?";

        dbw.update(
                FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,
                contentValues,
                selection,
                selectionArgs);


        notifyItemChanged(itemExpandido);
    }

    /*
     * Método llamado al pedir más info en una peli seleccionada.
     */
    public Intent abrirWeb () {
        Pelicula pelicula = mListaBusqueda.get(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Info\" en película " + pelicula.getTitulo());

        // Instanciamos el intent de navegador
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Se le pasa la web parseada
        intent.setData(Uri.parse(pelicula.getEnlace()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public void abrirMenuCompartir() {
        Pelicula pelicula = mListaBusqueda.get(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Share\" en película " + pelicula.getTitulo());

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "He compartido \"" + pelicula.getTitulo() + "\" a través de Hype!");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, pelicula.getEnlace());
        mActivity.startActivity(Intent.createChooser(intent, "Compartir película: " + pelicula.getTitulo() + "."));

    }

}