package com.clacksdepartment.hype;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Created by Usuario on 12/09/2017.
 */

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private static final String TAG = "RecyclerViewAdapter";

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
    private boolean flagTickets = false;

    private FragmentManager mFragmentManager;
    LinearLayoutManager mLinearLayoutManager;
    RecyclerView mRecyclerView;

    private int vistaParaExpandir;
    private int vistaParaContraer;


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

        public void clearAnimation(){
            mView.clearAnimation();
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

        if (sharedPreferences.getString("provider", "TMDB").equalsIgnoreCase("fa")){
            flagTickets = true;
        }

        RecyclerView.ItemAnimator animator = ((RecyclerView) mMainActivity.findViewById(R.id.lista)).getItemAnimator();

        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        mRecyclerView = ((RecyclerView) mMainActivity.findViewById(R.id.lista));
        mLinearLayoutManager = ((LinearLayoutManager) mRecyclerView.getLayoutManager());
        vistaParaExpandir = -1;
        vistaParaContraer = -1;
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
                v = (RelativeLayout) LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.footer, parent, false);
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
            NativeExpressAdView mAdView = (NativeExpressAdView) filaView.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        } else if (position == getItemCount() -1) {
            if (estado == HYPE ||
                    (mLinearLayoutManager.findFirstVisibleItemPosition() == mLinearLayoutManager.findFirstCompletelyVisibleItemPosition()) &&
                            ((mLinearLayoutManager.findLastCompletelyVisibleItemPosition()-mLinearLayoutManager.findFirstCompletelyVisibleItemPosition() +2 - getItemCount()) >= 0)) {
                filaView.setVisibility(View.GONE);
            } else {
                filaView.setVisibility(View.VISIBLE);
            }
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

            if (position == itemExpandido) {

                if (pelicula.getSinopsis().length() > 0){
                    ((TextView) avanzado.findViewById(R.id.av_sinopsis)).setText( mMainActivity.getResources().getString(R.string.sinopsis_list_structure,pelicula.getSinopsis().substring(0, Math.min(pelicula.getSinopsis().length(), 200))));
                }else{
                    ((TextView) avanzado.findViewById(R.id.av_sinopsis)).setText("");
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
            } else {
                if (vistaParaContraer == position) {
                    collapse(avanzado);
                    vistaParaContraer = -1;
                }else{
                    avanzado.setVisibility(View.GONE);
                }
            }

            Log.v(TAG, "Añadiendo película " + pelicula.getTitulo() + " a la vista número " + position);
        }

    }

    @Override
    public void onViewDetachedFromWindow(ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        ((ViewHolder) holder).clearAnimation();
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
        if (getItemCount()==1) {
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
            vistaParaContraer = posicion;
        } else {

            // El siguiente If habilita la posibilidad de comprimir y expandir a la vez, diferentes animaciones.
            /*if (itemExpandido != -1){
                Log.d(TAG, "Contrayendo  elemento " + itemExpandido);
                vistaParaContraer = itemExpandido;
            }*/

            itemExpandido = posicion;
            Log.d(TAG, "Expandiendo  elemento " + posicion);
            vistaParaExpandir = posicion;
        }

        if(posicionAntigua != -1)
            notifyItemChanged(posicionAntigua);
        notifyItemChanged(itemExpandido);
        if (itemExpandido != -1) {
            ((RecyclerView) mMainActivity.findViewById(R.id.lista)).smoothScrollToPosition(itemExpandido);
        }

        String [] fecha = getPelicula(posicion).getEstrenoFecha().split("-");

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
            if (flagTickets) {
                view.findViewById(R.id.av_cines).setVisibility(View.VISIBLE);
            }else{
                view.findViewById(R.id.av_cines).setVisibility(View.GONE);
            }
            view.findViewById(R.id.av_fecha).setVisibility(View.GONE);
        }else{
            view.findViewById(R.id.av_cines).setVisibility(View.GONE);
            view.findViewById(R.id.av_fecha).setVisibility(View.VISIBLE);
        }
    }


    public void abrirFicha(){
        Log.i(TAG, "Pulsado botón de abrir fichaFragment");
        Pelicula pelicula = getPelicula(itemExpandido);
        FichaFragment fichaFragment = FichaFragment.newInstance(pelicula.getTitulo(), pelicula.getEnlace(), pelicula.getSinopsis());

        FragmentTransaction fragmentTransaction = mFragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(R.anim.abrir_ficha, R.anim.cerrar_ficha, R.anim.abrir_ficha, R.anim.cerrar_ficha);
        fragmentTransaction.replace(R.id.ficha_container, fichaFragment).addToBackStack(null).commit();
    }


    //Envía la película al calendario en forma de evento
    public Intent abrirCalendario(){
            Pelicula pelicula = getPelicula(itemExpandido);

            Calendar beginTime = Calendar.getInstance();
            String [] f = pelicula.getEstrenoFecha().split("-");
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
            if (getItemCount() == 1){
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

    public Intent verCines(){
        Pelicula pelicula = getPelicula(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Cines\" en película " + pelicula.getTitulo());

        // Instanciamos el intent de navegador
        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Se le pasa la web parseada
        intent.setData(Uri.parse(pelicula.getEnlace().replace("movie","movie-showtimes")));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        return intent;
    }

    public void abrirMenuCompartir() {
        Pelicula pelicula = getPelicula(itemExpandido);

        Log.d(TAG, "Pulsado botón \"Share\" en película " + pelicula.getTitulo());

        String mensaje = "";

        int estado = this.estado;

        if (estado == HYPE) {

            for (ArrayList<Pelicula> pp : mListaCartelera) {
                if (pp.contains(pelicula))
                    estado = CARTELERA;
            }
            for (ArrayList<Pelicula> pp : mListaEstrenos) {
                if (pp.contains(pelicula))
                    estado = ESTRENOS;
            }
        }

        Resources res = mMainActivity.getResources();

        if (estado == CARTELERA){
            mensaje = res.getString(R.string.share_cartelera,pelicula.getTitulo());
        } else if (estado == ESTRENOS){
            //Se coge el día de hoy
            String f = "09/09/2099";
            if (f.matches(pelicula.getEstrenoFecha())){
                mensaje = res.getString(R.string.share_estreno_ind,pelicula.getTitulo());;
            } else {
                SimpleDateFormat myFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH);

                try {
                    Date date1 = myFormat.parse(pelicula.getEstrenoFecha());
                    Date date2 = new Date();
                    long diff = date1.getTime() - date2.getTime();
                    int dias = (int) TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                    mensaje = res.getString(R.string.share_estreno,dias,pelicula.getTitulo());
                } catch (ParseException e) {
                    e.printStackTrace();
                    mensaje = res.getString(R.string.share_estreno_ind,pelicula.getTitulo());;
                }
            }
        } else {
            mensaje = pelicula.getTitulo();
        }

        mensaje = mensaje + "\n" + pelicula.getEnlace();

        Intent intent = new Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");

        //intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "He compartido \"" + pelicula.getTitulo() + "\" a través de Hype!");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, mensaje);
        mMainActivity.startActivity(Intent.createChooser(intent,res.getString(R.string.share_mensaje,pelicula.getTitulo())));

    }

    public void actualizarDatos() {
        mListaCartelera.clear();
        mListaEstrenos.clear();
        notifyDataSetChanged();
        HiloLeerBBDD hiloLeerBBDD = new HiloLeerBBDD(mFeedReaderDbHelper.getReadableDatabase(), mFeedReaderDbHelper.getWritableDatabase(),this);
        hiloLeerBBDD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void expand(final View v) {
        //if (v.getVisibility() == View.GONE) {
            v.measure(View.MeasureSpec.makeMeasureSpec(((View) v.getParent()).getWidth(), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
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
            v.startAnimation(a);
        //}

    }


}