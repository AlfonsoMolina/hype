package molina.alfonso.hype;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.CalendarContract;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Adaptador creado para las diferentes listas. Tiene un comportamiento diferente
 * según la naturaleza de la mListaEstrenos.
 *
 * @author Alfonso Molina
 */
public class ListaModificadaAdapter extends ArrayAdapter{

    /*
     * Declaración de variables
     */

    private static final String TAG = "ListaModificadaAdapter";

    public static final int HYPE = 0;
    public static final int CARTELERA = 1;
    public static final int ESTRENOS = 2;

    private ArrayList<Pelicula> mListaEstrenos = new ArrayList<>();  //Los elementos de la mListaEstrenos
    private ArrayList<Pelicula> mListaCartelera = new ArrayList<>();  //Los elementos de la mListaEstrenos

    private int resourceID;                                 //El layout en que se va a mostrar
    private FeedReaderDbHelper mFeedReaderDbHelper;                          //Base de datos
    private MainActivity mMainActivity;                          //Actividad, para cambiar la IU

    private int paginaEstrenos = 0;                                 //La página que se está mostrando (empezando por 0)
    private int ultimaPagEstrenos;                                  //El número (empezando por 1) de la última página
    private int paginaCartelera = 0;
    private int ultimaPagCartelera;
    private int numPeliculasPorPagina = 25;                     //Número de películas por página

    private int itemExpandido = -1;                             //Posición del elemento itemExpandido

    private int estado = CARTELERA;

    private FragmentManager mFragmentManager;

    /**
     * Constructor.
     * @param resourceID recurso con el layout de cada fila.
     * @param mainActivity actividad principal, para acutalizar la IU
     * @param feedReaderDbHelper la base de datos
     */
    public ListaModificadaAdapter(MainActivity mainActivity, int resourceID, FeedReaderDbHelper feedReaderDbHelper) {
        super(mainActivity.getApplicationContext(),resourceID);
        Log.d(TAG, "Construyendo el adaptador de la mListaEstrenos");
        this.resourceID = resourceID;
        this.mMainActivity = mainActivity;
        this.mFeedReaderDbHelper = feedReaderDbHelper;
        ultimaPagEstrenos = 1;
        ultimaPagCartelera = 1;
        mFragmentManager = mainActivity.getSupportFragmentManager();
        HiloLeerBBDD hiloLeerBBDD = new HiloLeerBBDD(feedReaderDbHelper.getReadableDatabase(), feedReaderDbHelper.getWritableDatabase(),this);
        hiloLeerBBDD.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    /**
     * Devuelve el número de filas en la mListaEstrenos. Puede ser:
     *      0, si la fila está vaciá
     *      numPeliculasPorPagina, porque solo muestra paginas completas en teoría
     *      las que sea si están guardadas
     * @return devuelve un entero con el número de elementos en la mListaEstrenos.
     */
    @Override
    public int getCount() {
        int cuenta = 0;
        if(estado == HYPE){
            int i = 0;
            while(i < mListaCartelera.size()) {
                if (mListaCartelera.get(i).getHype())
                    cuenta++;
                i++;
            }
            i = 0;
            while(i < mListaEstrenos.size()) {
                if (mListaEstrenos.get(i).getHype())
                    cuenta++;
                i++;
            }
            return cuenta;

        } else {
            ArrayList<Pelicula> listaPeliculas = estado == CARTELERA ? mListaCartelera : mListaEstrenos;
            if (listaPeliculas.size() > numPeliculasPorPagina)
                return numPeliculasPorPagina;
            else if (listaPeliculas.size() > 0)
                return listaPeliculas.size();
            else
                return 0;
        }

    }

    //Este método devuelve la posición en la mListaEstrenos de Peliculas según la posición en la mListaEstrenos.
    //No siempre es el mismo valor porque se usan varias páginas y a veces se muestran las que están
    //guardadas unicamente.
    //p es la posición en la mListaEstrenos mostrada en pantalla. posicion es la posición en la mListaEstrenos.
    private int getPosicionAbsoluta(int posicionRelativa) {
        int posicionAbsoluta = 0;
        if (estado == HYPE) {
            int i = 0;

            while (i < mListaCartelera.size()) {
                if (mListaCartelera.get(i).getHype()) {
                    if (posicionRelativa == 0) {
                        posicionAbsoluta = i;
                    }
                    posicionRelativa--;
                }
                i++;
            }

            int j = 0;
            while (j < mListaEstrenos.size()) {
                if (mListaEstrenos.get(j).getHype()) {
                    if (posicionRelativa == 0) {
                        posicionAbsoluta = i;
                    }
                    posicionRelativa--;
                }
                i++;
                j++;
            }

        } else if (estado == CARTELERA){
            posicionAbsoluta = posicionRelativa + paginaCartelera * numPeliculasPorPagina;
        } else
            posicionAbsoluta = posicionRelativa + paginaEstrenos * numPeliculasPorPagina;

        return posicionAbsoluta;

    }

    /**
     * Devuelve el elemento de la mListaEstrenos de la posición elegida.
     * @param position entero con la posición del elemento en la mListaEstrenos.
     * @return devuelve el objeto en la fila elegida.
     */
    @Override
    public Object getItem(int position) {
        return mListaEstrenos.get(getPosicionAbsoluta(position));
    }

     /**
     * Crea la vista del elemento de la mListaEstrenos.
     *
     * @param position entero con la posición del elemento en la mListaEstrenos.
     * @param convertView vista de la fila.
     * @param parent vista de la mListaEstrenos padre.
     * @return devuelve la fila modificada.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // workaround para que no se rompa si aún no está mListaEstrenos la lectura
        View view = prepareView(position, convertView, parent);
        return view;
    }

    public View prepareView(int posicionRelativa, View convertView, ViewGroup parent) {

        // workaround para que no se rompa si aún no está mListaEstrenos la lectura
        View filaView;

        if (convertView == null) {
            //Se añade una nueva view a la mListaEstrenos.
            LayoutInflater layoutInflater = (LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            filaView = layoutInflater.inflate(resourceID, parent, false);
        }else{
            filaView = convertView;
        }

        Pelicula pelicula;
        if(estado == HYPE){
            int posicionAbsoluta = getPosicionAbsoluta(posicionRelativa);
            if (posicionAbsoluta >= mListaCartelera.size())
                pelicula = mListaEstrenos.get(posicionAbsoluta- mListaCartelera.size());
            else
                pelicula = mListaCartelera.get(posicionAbsoluta);
        }else {
            pelicula = (estado == CARTELERA ? mListaCartelera : mListaEstrenos).get(getPosicionAbsoluta(posicionRelativa));
        }
        ((TextView) filaView.findViewById(R.id.titulo)).setText(pelicula.getTitulo());
        ((TextView) filaView.findViewById(R.id.estreno)).setText(pelicula.getEstrenoLetras());
        ((ImageView) filaView.findViewById(R.id.portada)).setImageBitmap(pelicula.getPortada());

        if (pelicula.getHype()) {
            filaView.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
        } else
            filaView.findViewById(R.id.hype_msg).setVisibility(View.GONE);

        if (itemExpandido == posicionRelativa) {
            filaView.findViewById(R.id.avanzado).setVisibility(View.VISIBLE);
            ((TextView) filaView.findViewById(R.id.av_sinopsis)).setText(pelicula.getSinopsis());

            filaView.findViewById(R.id.av_fecha).setOnClickListener(enviar_Calendario);
            filaView.findViewById(R.id.av_hype).setOnClickListener(get_hype);
            filaView.findViewById(R.id.av_enlace).setOnClickListener(get_info);
            filaView.findViewById(R.id.av_ficha).setOnClickListener(abre_ficha);
            filaView.findViewById(R.id.av_compartir).setOnClickListener(compartir);

            if (pelicula.getHype()){
                ((AppCompatImageButton) filaView.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_black_24dp);
            }else{
                ((AppCompatImageButton) filaView.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }
        } else
            filaView.findViewById(R.id.avanzado).setVisibility(View.GONE);
        Log.v(TAG, "Añadiendo película " + pelicula.getTitulo() + " a la vista número " + posicionRelativa);

        return filaView;
    }

    /**
     * Añade un nuevo elemento al final de la mListaEstrenos.
     *
     * @param p Pelicua con el nuevo elemento a introducir.
     */
    public void addCartelera(Pelicula p){
        mListaCartelera.add(p);
    }
    public void addEstrenos(Pelicula p){
        mListaEstrenos.add(p);
    }


    public void addCartelera(ArrayList <Pelicula> p){
        mListaCartelera.addAll(p);
    }
    public void addEstrenos(ArrayList <Pelicula> p){
        mListaEstrenos.addAll(p);
    }

    //Guarda el elemento que está itemExpandido con más información.
    //Si se pulsa otra vez, se oculta
    public void setItemExpandido(int posicionRelativa){
        if (itemExpandido == posicionRelativa){
            itemExpandido = -1;
            Log.d(TAG, "Contrayendo  elemento " + posicionRelativa);
        } else {
            itemExpandido = posicionRelativa;
            Log.d(TAG, "Expandiendo  elemento " + posicionRelativa);
        }
    }

    //Lo que hace este método es mostrar los elementos de la interfaz adecuados.
    //Cuando la mListaEstrenos ya no esté vacía, muestra la barra de navegación y esconde el mensaje
    //Cuando haya una página nueva, mostrará el botón para pasar la página.
    //Todo pasar al navegador
    public void actualizarInterfaz(){
        if (estado != HYPE) {
            if(estado == CARTELERA) {
                if (mListaCartelera.size() == 0) {
                    //  mMainActivity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
                    mMainActivity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
                } else {
                    mMainActivity.findViewById(R.id.nopelis).setVisibility(View.GONE);
                    //   mMainActivity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                    if (paginaCartelera + 1 < ultimaPagCartelera) {
                        mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
                    } else
                        mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
                }
            }else{
                if (mListaEstrenos.size() == 0) {
                    //  mMainActivity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
                    mMainActivity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
                } else {
                    mMainActivity.findViewById(R.id.nopelis).setVisibility(View.GONE);
                    //   mMainActivity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                    if (paginaEstrenos + 1 < ultimaPagEstrenos) {
                        mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
                    } else
                        mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    //Si la mListaEstrenos está vacía, muestra el mensaje de que no hay películas.
    public void noHayPelis(){
        if (((estado==CARTELERA)? mListaCartelera : mListaEstrenos).size()==0) {
            ((TextView) mMainActivity.findViewById(R.id.nopelis)).setText(R.string.no_pelis);
            mMainActivity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);        }

        if(mMainActivity.getMenu()!= null) {
            mMainActivity.getMenu().findItem(R.id.actualizar).setEnabled(true);
            mMainActivity.getMenu().findItem(R.id.actualizar).setVisible(true);
            mMainActivity.getMenu().findItem(R.id.cancelar).setEnabled(false);
            mMainActivity.getMenu().findItem(R.id.cancelar).setVisible(false);
        }
    }


    private View.OnClickListener abre_ficha = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Log.i(TAG, "Pulsado botón de abrir fichaFragment");
            Pelicula pelicula;
            if(estado == HYPE){
                int posicionAbsoluta = getPosicionAbsoluta(itemExpandido);
                if (posicionAbsoluta >= mListaCartelera.size())
                    pelicula = mListaEstrenos.get(posicionAbsoluta- mListaCartelera.size());
                else
                    pelicula = mListaCartelera.get(posicionAbsoluta);
            }else {
                pelicula = (estado == CARTELERA ? mListaCartelera : mListaEstrenos).get(getPosicionAbsoluta(itemExpandido));
            }            FichaFragment fichaFragment = FichaFragment.newInstance(pelicula.getTitulo(), pelicula.getEnlace());

            mFragmentManager.beginTransaction().replace(R.id.ficha_container, fichaFragment).addToBackStack(null).commit();
        }
    };

    //Envía la película al calendario en forma de evento
    private View.OnClickListener enviar_Calendario = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int posicionRelativa = itemExpandido;

            Pelicula pelicula;
            if(estado == HYPE){
                int posicionAbsoluta = getPosicionAbsoluta(posicionRelativa);
                if (posicionAbsoluta >= mListaCartelera.size())
                    pelicula = mListaEstrenos.get(posicionAbsoluta- mListaCartelera.size());
                else
                    pelicula = mListaCartelera.get(posicionAbsoluta);
            }else {
                pelicula = (estado == CARTELERA ? mListaCartelera : mListaEstrenos).get(getPosicionAbsoluta(posicionRelativa));
            }
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

            getContext().startActivity(intent);
        }
    };

    //Guarda la película
    private View.OnClickListener get_hype = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            int posicionRelativa = itemExpandido;
            int BBDD = estado;

            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionAbsoluta(posicionRelativa);
                if (preal >= mListaCartelera.size()) {
                    p = mListaEstrenos.get(preal - mListaCartelera.size());
                    BBDD = ESTRENOS;
                }else {
                    p = mListaCartelera.get(preal);
                    BBDD = CARTELERA;
                }
            }else {
                p = (estado == CARTELERA ? mListaCartelera : mListaEstrenos).get(getPosicionAbsoluta(posicionRelativa));
            }            Log.d(TAG, "Pulsado botón \"Hype\" en película " + p.getTitulo());

            SQLiteDatabase dbw = mFeedReaderDbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            String h;

            p.setHype(!p.getHype());

            if (p.getHype()) {
                h = "T";
                ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                h = "F";
                ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

            if(BBDD == CARTELERA){
                values.put(FeedReaderContract.FeedEntryCartelera.COLUMN_HYPE, h);

                String selection = FeedReaderContract.FeedEntryCartelera.COLUMN_REF + " LIKE ?";
                String[] selectionArgs = {p.getEnlace()};

                int count = dbw.update(
                        FeedReaderContract.FeedEntryCartelera.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);

            }else {
                values.put(FeedReaderContract.FeedEntryEstrenos.COLUMN_HYPE, h);

                String selection = FeedReaderContract.FeedEntryEstrenos.COLUMN_REF+ " LIKE ?";
                String[] selectionArgs = {p.getEnlace()};

                int count = dbw.update(
                        FeedReaderContract.FeedEntryEstrenos.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
            }

            notifyDataSetChanged();
        }
    };

    /*
     * Método llamado al pedir más info en una peli seleccionada.
     */
    private View.OnClickListener get_info = new View.OnClickListener(){

        @Override
        public void onClick(View view) {
            int posicionRelativa = itemExpandido;
            Pelicula pelicula;
            if(estado == HYPE){
                int posicionAbsoluta = getPosicionAbsoluta(posicionRelativa);
                if (posicionAbsoluta >= mListaCartelera.size())
                    pelicula = mListaEstrenos.get(posicionAbsoluta- mListaCartelera.size());
                else
                    pelicula = mListaCartelera.get(posicionAbsoluta);
            }else {
                pelicula = (estado == CARTELERA ? mListaCartelera : mListaEstrenos).get(getPosicionAbsoluta(posicionRelativa));
            }
            Log.d(TAG, "Pulsado botón \"Info\" en película " + pelicula.getTitulo());

            // Instanciamos el intent de navegador
            Intent intent = new Intent(Intent.ACTION_VIEW);
            // Se le pasa la web parseada
            intent.setData(Uri.parse(pelicula.getEnlace()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Lanzamos el intent
            getContext().startActivity(intent);
            // profit!

        }
    };

    private View.OnClickListener compartir = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            int position = itemExpandido;
            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionAbsoluta(position);
                if (preal >= mListaCartelera.size())
                    p = mListaEstrenos.get(preal- mListaCartelera.size());
                else
                    p = mListaCartelera.get(preal);
            }else {
                p = (estado == CARTELERA ? mListaCartelera : mListaEstrenos).get(getPosicionAbsoluta(position));
            }
            Log.d(TAG, "Pulsado botón \"Share\" en película " + p.getTitulo());

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "He compartido \"" + p.getTitulo() + "\" a través de Hype!");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, p.getEnlace());
            mMainActivity.startActivity(Intent.createChooser(sharingIntent, "Compartir película: " + p.getTitulo() + "."));

        }
    };


    public void pasarPagina(int i){
        if (estado == CARTELERA)
            paginaCartelera = i;
        else if (estado == ESTRENOS)
            paginaEstrenos = i;
        itemExpandido = -1;
    }

    public int getPagina(){
        if (estado == CARTELERA)
            return paginaCartelera;
        else if (estado == ESTRENOS)
            return paginaEstrenos;
        else
            return 0;
    }

    public void setMaxPaginas() {
        this.ultimaPagEstrenos = mListaEstrenos.size() / numPeliculasPorPagina;
        this.ultimaPagCartelera = mListaCartelera.size() / numPeliculasPorPagina;

    }

    public void eliminarLista() {
        mListaEstrenos.clear();
        mListaCartelera.clear();
    }

    public int getEstado(){
        return estado;
    }

    public void mostrarEstrenos(){
        itemExpandido = -1;
        estado = ESTRENOS;
    }

    public void mostrarHype(){
        itemExpandido = -1;
        estado = HYPE;
    }

    public void mostrarCartelera(){
        itemExpandido = -1;
        estado = CARTELERA;
    }


    public int getUltPagina() {
        if (estado==CARTELERA)
            return ultimaPagCartelera;
        else if (estado==ESTRENOS)
            return ultimaPagEstrenos;
        else
            return 0;
    }
}
