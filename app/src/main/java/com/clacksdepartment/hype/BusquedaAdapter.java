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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Usuario on 15/09/2017.
 */

public class BusquedaAdapter extends  RecyclerView.Adapter<BusquedaAdapter.ViewHolder> {

    private static final String TAG = "BusquedaAdapter";

    private final int resourceID;
    private final SearchableActivity mActivity;

    private ArrayList<Pelicula> mListaBusqueda;
    private FeedReaderDbHelper mDB;
    private FragmentManager mFragmentManager;

    private int itemExpandido = -1;

    private int vistaParaExpandir;
    private int vistaParaContraer;

    private RecyclerView mRecyclerView;

    private final int viewMeasureSpecHeight;


    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public RelativeLayout mView;

        public ViewHolder(RelativeLayout v) {
            super(v);
            mView = v;
        }

        public void clearAnimation(){
            mView.clearAnimation();
        }
    }
    // Provide a suitable constructor (depends on the kind of dataset)
    public BusquedaAdapter (SearchableActivity searchableActivity, int resourceID, FeedReaderDbHelper db) {

        mListaBusqueda = new ArrayList<>();
        this.resourceID = resourceID;
        this.mActivity = searchableActivity;
        mFragmentManager = searchableActivity.getSupportFragmentManager();
        this.mDB = db;

        mRecyclerView = ((RecyclerView) mActivity.findViewById(R.id.lista));

        RecyclerView.ItemAnimator animator = mRecyclerView.getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        vistaParaContraer = -1;
        vistaParaExpandir = -1;

        viewMeasureSpecHeight = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
    }

    @Override
    public BusquedaAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view
        RelativeLayout v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fila, parent, false);
        // set the view's size, margins, paddings and layout parameters
        BusquedaAdapter.ViewHolder vh = new BusquedaAdapter.ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(BusquedaAdapter.ViewHolder holder, int position) {
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

            if (pelicula.getSinopsis().length() > 0){
                ((TextView) avanzado.findViewById(R.id.av_sinopsis)).setText( mActivity.getResources().getString(R.string.sinopsis_list_structure,pelicula.getSinopsis().replace("(FILMAFFINITY)","").substring(0, Math.min(pelicula.getSinopsis().length(), 200))));
            }else{
                ((TextView) avanzado.findViewById(R.id.av_sinopsis)).setText("");
                avanzado.findViewById(R.id.av_sinopsis).setVisibility(View.GONE);
            }

            if (pelicula.getHype()) {
                ((ImageButton) avanzado.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                ((ImageButton) avanzado.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

            if (vistaParaExpandir == position) {
                expand(avanzado);
                vistaParaExpandir = -1;
            }else{
                avanzado.setVisibility(View.VISIBLE);
            }

        } else{
            if (vistaParaContraer == position) {
                collapse(avanzado);
                vistaParaContraer = -1;
            }else{
                avanzado.setVisibility(View.GONE);
            }
        }

        Log.v(TAG, "Añadiendo película " + pelicula.getTitulo() + " a la vista número " + position);
    }

    @Override
    public int getItemCount() {
        return mListaBusqueda.size();
    }

    void  setItemExpandido(View view){
        Log.d(TAG, "Hacemos click!");
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
            vistaParaContraer = posicion;
        } else {
            itemExpandido = posicion;
            Log.d(TAG, "Expandiendo  elemento " + posicion);
            vistaParaExpandir = posicion;
        }
        if(posicionAntigua != -1)
            notifyItemChanged(posicionAntigua);
        notifyItemChanged(itemExpandido);

        if (itemExpandido != -1) {
            ((RecyclerView) mActivity.findViewById(R.id.lista)).smoothScrollToPosition(itemExpandido);
        }

        String [] fecha = mListaBusqueda.get(posicion).getEstrenoFecha().split("-");

        int difAno = Integer.parseInt(fecha[0]) - Calendar.getInstance().get(Calendar.YEAR);
        int difMes = Integer.parseInt(fecha[1]) - Calendar.getInstance().get(Calendar.MONTH) - 1;
        int difDia = Integer.parseInt(fecha[2]) - Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        boolean futuro = false;

        if (difAno == 0){
            if (difMes == 0){
                if (difDia > 0){
                    futuro = true;
                }
            }else if (difMes > 0){
                futuro = true;
            }
        }else if (difAno > 0){
            futuro = true;
        }

        if (!futuro){
            view.findViewById(R.id.av_cines).setVisibility(View.GONE);
            view.findViewById(R.id.av_fecha).setVisibility(View.GONE);
        }else{
            view.findViewById(R.id.av_cines).setVisibility(View.GONE);
            view.findViewById(R.id.av_fecha).setVisibility(View.VISIBLE);
        }
    }

    public void buscar(String query){

        mListaBusqueda.clear();

        String titulo, portada_enlace, enlace, sinopsis, estreno_letras, estreno_fecha;
        boolean hype;
        byte[] portada_byte;
        Bitmap portada_bitmap;
        String[] q = {"%" +query+"%"};

        SQLiteDatabase dbr = mDB.getReadableDatabase();

        //Se lee la bbdd y se guardan los elementos en cursor
        String[] projection = {
                FeedReaderContract.FeedEntryEstrenos._ID,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA_ENLACE,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_REF,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA,
                FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE
        };

        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO + " LIKE ?",  // The columns for the WHERE clause
                q ,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA + " ASC"                                    // The sort order
        );

        //Y empezamos a mirar las tuplas una a una
        while (cursor.moveToNext()) {
            estreno_fecha = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_FECHA));
            titulo = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_TITULO));
            enlace = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_REF));
            portada_byte = cursor.getBlob(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA));
            portada_enlace = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_PORTADA_ENLACE));
            sinopsis = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_SINOPSIS));
            estreno_letras = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_ESTRENO_LETRAS));
            hype = cursor.getInt(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE)) == 1;

            portada_bitmap = BitmapFactory.decodeByteArray(portada_byte, 0, portada_byte.length);

            mListaBusqueda.add(new Pelicula(enlace, portada_bitmap, portada_enlace, titulo, sinopsis,
                    estreno_letras, estreno_fecha, hype));

            Log.d(TAG, "Encontrada película Estrenos: " + titulo + ".");
        }
        cursor.close();

        itemExpandido = -1;
        vistaParaExpandir = -1;
        vistaParaContraer = -1;

        notifyDataSetChanged();
    }


    public void abrirFicha(){
        Log.i(TAG, "Pulsado botón de abrir fichaFragment");
        Pelicula pelicula = mListaBusqueda.get(itemExpandido);
        FichaFragment fichaFragment = FichaFragment.newInstance(pelicula.getTitulo(), pelicula.getEnlace(), pelicula.getSinopsis());
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

        pelicula.setHype(!pelicula.getHype());

        if (pelicula.getHype()) {
            ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_black_24dp);
        } else {
            ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_border_black_24dp);
        }

        contentValues.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, pelicula.getHype()?1:0);

        String selection = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + " LIKE ?";
        String[] selectionArgs = {pelicula.getEnlace()};

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


    public void expand(final View v) {
        //if (v.getVisibility() == View.GONE) {
        v.measure(View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(), View.MeasureSpec.EXACTLY), viewMeasureSpecHeight);
        final int targetHeight = v.getMeasuredHeight();

        // Older versions of android (pre API 21) cancel animations for views with a height of 0.
        v.getLayoutParams().height = 1;
        v.setVisibility(View.VISIBLE);

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (interpolatedTime == 1){
                    v.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
                }else{
                    v.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
                }

                v.requestLayout();
                mRecyclerView.scrollToPosition(itemExpandido);
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }

        };

        // 1dp/ms
        a.setDuration((int) (2*targetHeight / v.getContext().getResources().getDisplayMetrics().density));
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        v.startAnimation(a);
        // }
    }

    public void collapse(final View v) {

        //if (v.getVisibility() == View.VISIBLE) {
        final int initialHeight = v.getMeasuredHeight();

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                } else {
                    v.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                    v.requestLayout();
                }

            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }

        };

        // 1dp/ms
        a.setDuration((int) (2*initialHeight / v.getContext().getResources().getDisplayMetrics().density));
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        v.startAnimation(a);
        //}

    }

   // }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        ((ViewHolder) holder).clearAnimation();
    }

}