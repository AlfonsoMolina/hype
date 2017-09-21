package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Usuario on 12/09/2017.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "listaNueva";

    public static final int HYPE = 0;
    public static final int CARTELERA = 1;
    public static final int ESTRENOS = 2;

    private static final int NUM_ITEM_ADD = 9;

    private ArrayList<ArrayList<Pelicula>> mListaEstrenos;  //Los elementos de la mListaEstrenos
    private ArrayList<ArrayList<Pelicula>> mListaCartelera;  //Los elementos de la mListaEstrenos

    private MainActivity mMainActivity;                          //Actividad, para cambiar la IU
    private Interfaz mInterfaz;

    private int paginaEstrenos = 0;                                 //La página que se está mostrando (empezando por 0)
    private int paginaCartelera = 0;
    private int numPeliculasPorPagina = 25;                     //Número de películas por página

    private Pelicula peliculaFocus;                             //Posición del elemento itemExpandido
    private int itemExpandido = -1;
    private View primerElemento;

    private FeedReaderDbHelper mFeedReaderDbHelper;

    private int estado = CARTELERA;
    private boolean flagAdds = true;

    private FragmentManager mFragmentManager;


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public RelativeLayout mView;

        public ViewHolder(RelativeLayout v) {
            super(v);
            mView = v;

        }
    }


    // Provide a suitable constructor (depends on the kind of dataset)
    public RecyclerViewAdapter(MainActivity mainActivity, FeedReaderDbHelper feedReaderDbHelper) {
        mListaCartelera = new ArrayList<>();
        mListaEstrenos = new ArrayList<>();
        Log.d(TAG, "Construyendo el adaptador de la mListaEstrenos");
        this.mMainActivity = mainActivity;
        mFragmentManager = mainActivity.getSupportFragmentManager();
        this.mFeedReaderDbHelper = feedReaderDbHelper;
        HiloLeerBBDD hiloLeerBBDD = new HiloLeerBBDD(feedReaderDbHelper.getReadableDatabase(), feedReaderDbHelper.getWritableDatabase(),this);
        hiloLeerBBDD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        mInterfaz = new Interfaz(mainActivity, this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity.getApplicationContext());

        flagAdds = sharedPreferences.getBoolean("pref_adds",true);


        RecyclerView.ItemAnimator animator = ((RecyclerView) mMainActivity.findViewById(R.id.lista)).getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

    }


    @Override
    public int getItemViewType(int position) {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous

        if (flagAdds && (position == NUM_ITEM_ADD || position == NUM_ITEM_ADD*2))
            return 0;
        else if (position == getItemCount()-1)
            return 1;
        else
            return 2;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                             int viewType) {
        // create a new view

        RelativeLayout v;
        switch (viewType) {
            case 0:
                v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fila_add, parent, false);
                break;
            case 1:
                if (estado == HYPE || primerElemento == null || primerElemento.isShown()) {
                    mMainActivity.findViewById(R.id.paginador).setBackgroundColor(mMainActivity.getColor(R.color.colorPrimaryDark));
                } else{
                    v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.footer, parent, false);
                }
                v=new RelativeLayout(mMainActivity.getApplicationContext());
                break;
            default:
                v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.fila, parent, false);
                break;
        }
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        View filaView = holder.mView;

        if (flagAdds && (position == NUM_ITEM_ADD || position == NUM_ITEM_ADD*2)) {
            AdView mAdView = (AdView) filaView.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else if (position == getItemCount() -1) {
            /*
            if (estado == HYPE || primerElemento == null || primerElemento.isShown()) {
                filaView.setVisibility(View.GONE);
            } else{
                filaView.setVisibility(View.VISIBLE);
            }*/
        } else {
            if (position == 0)
                primerElemento = filaView;

            Pelicula pelicula = getPelicula(position);
            ((TextView) filaView.findViewById(R.id.titulo)).setText(pelicula.getTitulo());
            ((TextView) filaView.findViewById(R.id.estreno)).setText(pelicula.getEstrenoLetras());
            ((ImageView) filaView.findViewById(R.id.portada)).setImageBitmap(pelicula.getPortada());

            if (pelicula.getHype()) {
                filaView.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
            } else
                filaView.findViewById(R.id.hype_msg).setVisibility(View.GONE);

            View avanzado = filaView.findViewById(R.id.avanzado);

            //position = getPosicionAbsoluta(position);

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
    }

    private Pelicula getPelicula(int position) {

        if (flagAdds && position > NUM_ITEM_ADD){
            position--;
            if (position >= NUM_ITEM_ADD*2)
                position--;
        }

        if (estado == HYPE) {
            for (int i = mListaCartelera.size() - 1; i >= 0; i--) {
                for (int j = mListaCartelera.get(i).size() - 1; j >= 0; j--) {
                    if (mListaCartelera.get(i).get(j).getHype()) {
                        if (position == 0)
                            return mListaCartelera.get(i).get(j);

                        position--;
                    }

                }
            }

            for (int i = 0; i < mListaEstrenos.size(); i++) {
                for (int j = 0; j < mListaEstrenos.get(i).size(); j++) {
                    if (mListaEstrenos.get(i).get(j).getHype()) {
                        if (position == 0)
                            return mListaEstrenos.get(i).get(j);
                        position--;

                    }
                }
            }

        } else {
            int pagina = estado == CARTELERA ? paginaCartelera : paginaEstrenos;
            return estado == CARTELERA ? mListaCartelera.get(pagina).get(position) : mListaEstrenos.get(pagina).get(position);
        }

        return mListaCartelera.get(0).get(0);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        int cuenta = 0;
        if (estado == HYPE){
            int count = 0;
            for (int i = 0; i < mListaCartelera.size(); i++){
                for (int j = 0; j < mListaCartelera.get(i).size(); j++){
                    if (mListaCartelera.get(i).get(j).getHype()){
                        count++;
                    }
                }
            }

            for (int i = 0; i < mListaEstrenos.size(); i++){
                for (int j = 0; j < mListaEstrenos.get(i).size(); j++){
                    if (mListaEstrenos.get(i).get(j).getHype()){
                        count++;
                    }
                }
            }
            cuenta = count;
         } else if (estado == CARTELERA) {
            if (mListaCartelera.size() > 0) {
                cuenta = mListaCartelera.get(paginaCartelera).size();
            }else
                cuenta = 0;
        } else {
            if (mListaEstrenos.size() > 0) {
                cuenta = mListaEstrenos.get(paginaEstrenos).size();
            }else
                cuenta = 0;
        }

        if (flagAdds && cuenta > NUM_ITEM_ADD) {
            cuenta++;
            if (cuenta > NUM_ITEM_ADD*2)
                cuenta++;
        }

        cuenta ++; //Por el footer.

        return cuenta;
    }

    int getUltPagina() {
        if (estado==CARTELERA)
            return mListaCartelera.size();
        else if (estado==ESTRENOS)
            return mListaEstrenos.size();
        else
            return 0;
    }

    void pasarPagina(int i){
        if (estado == CARTELERA)
            paginaCartelera = i;
        else if (estado == ESTRENOS)
            paginaEstrenos = i;
        itemExpandido = -1;
        actualizarInterfaz();
    }

    void reiniciarPagina(){
        paginaCartelera = 0;
        paginaEstrenos = 0;
    }

    int getPagina(){
        if (estado == CARTELERA)
            return paginaCartelera;
        else if (estado == ESTRENOS)
            return paginaEstrenos;
        else
            return 0;
    }


    void addCartelera(Pelicula p){
        int ultPagina = mListaCartelera.size() -1;
        if (ultPagina >= 0 && mListaCartelera.get(ultPagina).size() < numPeliculasPorPagina)
            mListaCartelera.get(ultPagina).add(p);
        else {
            mListaCartelera.add(new ArrayList<Pelicula>());
            mListaCartelera.get(ultPagina+1).add(p);
        }
        // Si estamos en la sección y la página donde se han añadido los datos:
        if (estado == CARTELERA && paginaCartelera == (mListaCartelera.size()-1)){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                //Si estamos moviendo la view no se hace nada o peta.
                Log.d(TAG, "Actualización de lista abortada por scroll.");
            }
        } else if (estado == HYPE && p.getHype()){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                //Si estamos moviendo la view no se hace nada o peta.
                Log.d(TAG, "Actualización de lista abortada por scroll.");
            }
        }
    }
    void addEstrenos(Pelicula p){
        int ultPagina = mListaEstrenos.size() -1;
        if (ultPagina >= 0 &&  mListaEstrenos.get(ultPagina).size() < numPeliculasPorPagina)
            mListaEstrenos.get(ultPagina).add(p);
        else {
            mListaEstrenos.add(new ArrayList<Pelicula>());
            mListaEstrenos.get(ultPagina+1).add(p);
        }
        if (estado == ESTRENOS && paginaEstrenos == (mListaEstrenos.size()-1)){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                //Si estamos moviendo la view no se hace nada o peta.
                Log.d(TAG, "Actualización de lista abortada por scroll.");
            }
        } else if (estado == HYPE && p.getHype()){
            try {
                notifyItemInserted(getItemCount()-1);
            }catch(Exception e){
                //Si estamos moviendo la view no se hace nada o peta.
                Log.d(TAG, "Actualización de lista abortada por scroll.");
            }
        }
    }

    //Estos dos pueden ser más eficientes
    void addCartelera(ArrayList<Pelicula> peliculas){
        for (int i = 0; i < peliculas.size();i++) {
            addCartelera(peliculas.get(i));

        }
    }
    void addEstrenos(ArrayList<Pelicula> peliculas){
        for (int i = 0; i < peliculas.size();i++) {
            addEstrenos(peliculas.get(i));
        }
    }

    //Si la mListaEstrenos está vacía, muestra el mensaje de que no hay películas.
    void quitarX(){

        if(mMainActivity.getMenu()!= null) {
            mMainActivity.getMenu().findItem(R.id.actualizar).setEnabled(true);
            mMainActivity.getMenu().findItem(R.id.actualizar).setVisible(true);
            mMainActivity.getMenu().findItem(R.id.cancelar).setEnabled(false);
            mMainActivity.getMenu().findItem(R.id.cancelar).setVisible(false);
        }
    }

    void mostrarNoPelis(){
        if (getItemCount()==0) {
            mInterfaz.mostrarNoHayPelis(true);
        } else
            mInterfaz.mostrarNoHayPelis(false);


    }

    void actualizarInterfaz(){
        mInterfaz.actualizar();
    }

    void eliminarLista() {
        mListaEstrenos.clear();
        mListaCartelera.clear();
    }

    public int getEstado(){
        return estado;
    }

    boolean mostrarEstrenos(){
        boolean haCambiado = false;
        if (estado != ESTRENOS){
            itemExpandido = -1;
            estado = ESTRENOS;
            haCambiado = true;
            mInterfaz.actualizar();
        }
        return haCambiado;
    }

    boolean mostrarHype(){
        boolean haCambiado = false;
        if (estado != HYPE){
            itemExpandido = -1;
            estado = HYPE;
            haCambiado = true;
            mInterfaz.actualizar();
        }
        return haCambiado;
    }

    boolean mostrarCartelera(){
        boolean haCambiado = false;
        if (estado != CARTELERA){
            itemExpandido = -1;
            estado = CARTELERA;
            haCambiado = true;
            mInterfaz.actualizar();
        }
        return haCambiado;
    }

    void  setItemExpandido(View view){
        //Encontramos la posición del elemento.
        String titulo = (String) ((TextView) view.findViewById(R.id.titulo)).getText();
        int posicionAntigua = itemExpandido;
        int posicion = 0;


        if (estado == HYPE){
            Pelicula p;
            Boolean flag = true;
            for (int i =  mListaCartelera.size()-1; flag && i >=0; i--){
                for (int j = mListaCartelera.get(i).size()-1; flag && j >=0 ; j--) {
                    p = mListaCartelera.get(i).get(j);
                    if (p.getHype()) {
                        if (p.getTitulo().equals(titulo)) {
                            flag = false;
                        } else
                            posicion++;
                    }
                }
            }
            for (int i = 0; flag && i < mListaEstrenos.size(); i++){
                for (int j = 0; flag && j < mListaEstrenos.get(i).size(); j++) {
                    p = mListaEstrenos.get(i).get(j);
                    if (p.getHype()) {
                        if (p.getTitulo().equals(titulo)) {
                            flag = false;
                        } else
                            posicion++;
                    }
                }
            }
        }else {
            for (Pelicula p : estado == CARTELERA ? mListaCartelera.get(paginaCartelera) : mListaEstrenos.get(paginaEstrenos)) {
                if (p.getTitulo().equals(titulo)) {
                    break;
                }
                posicion++;
            }
        }

        if (flagAdds && posicion >= NUM_ITEM_ADD){
            posicion++;
            if (posicion >= NUM_ITEM_ADD*2)
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
            ((RecyclerView) mMainActivity.findViewById(R.id.lista)).smoothScrollToPosition(itemExpandido);
        }
    }


    public void abrirFicha(){
        Log.i(TAG, "Pulsado botón de abrir fichaFragment");
        Pelicula pelicula = getPelicula(itemExpandido);
        FichaFragment fichaFragment = FichaFragment.newInstance(pelicula.getTitulo(), pelicula.getEnlace());

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();

        fragmentTransaction.setCustomAnimations(R.anim.abrir_ficha, R.anim.cerrar_ficha, R.anim.abrir_ficha, R.anim.cerrar_ficha);

        fragmentTransaction.replace(R.id.ficha_container, fichaFragment).addToBackStack(null).commit();

    }


    //Envía la película al calendario en forma de evento
    public Intent abrirCalendario(){
            Pelicula pelicula = getPelicula(itemExpandido);

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

        Pelicula pelicula = getPelicula(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Hype\" en película " + pelicula.getTitulo());

        SQLiteDatabase dbw = mFeedReaderDbHelper.getWritableDatabase();
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

        //if (estado == CARTELERA) {

        // Primero intentamos en la bbdd de cartelera...
            contentValues.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, isHyped);

            String selection = FeedReaderContract.FeedEntryCartelera.COLUMN_REF + " LIKE ?";
            String[] selectionArgs = {pelicula.getEnlace()};

            dbw.update(
                    FeedReaderContract.FeedEntryCartelera.TABLE_NAME,
                    contentValues,
                    selection,
                    selectionArgs);

        //} else {
        // luego en la bbdd de hype
            contentValues.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, isHyped);

            selection = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF + " LIKE ?";
            //String[] selectionArgs = {pelicula.getEnlace()};

            dbw.update(
                    FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,
                    contentValues,
                    selection,
                    selectionArgs);

        //}

        if (estado != HYPE) {
            notifyItemChanged(itemExpandido);
        }else{
            notifyDataSetChanged();
            itemExpandido = -1;
            if (getItemCount() == 0){
                mInterfaz.actualizar();
            }
        }
    }

    /*
     * Método llamado al pedir más info en una peli seleccionada.
     */
    public Intent abrirWeb () {
        Pelicula pelicula = getPelicula(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Info\" en película " + pelicula.getTitulo());

        // Instanciamos el intent de navegador
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Se le pasa la web parseada
        intent.setData(Uri.parse(pelicula.getEnlace()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public void abrirMenuCompartir() {
        Pelicula pelicula = getPelicula(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Share\" en película " + pelicula.getTitulo());

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "He compartido \"" + pelicula.getTitulo() + "\" a través de Hype!");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, pelicula.getEnlace());
        mMainActivity.startActivity(Intent.createChooser(intent, "Compartir película: " + pelicula.getTitulo() + "."));

    }

    public void actualizarDatos() {
        mListaCartelera.clear();
        mListaEstrenos.clear();
        notifyDataSetChanged();
        HiloLeerBBDD hiloLeerBBDD = new HiloLeerBBDD(mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase(),this);
        hiloLeerBBDD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}