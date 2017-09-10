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
import android.support.v7.widget.ShareActionProvider;
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
 * según la naturaleza de la listaEstrenos.
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

    private ArrayList<Pelicula> listaEstrenos = new ArrayList<>();  //Los elementos de la listaEstrenos
    private ArrayList<Pelicula> listaCartelera = new ArrayList<>();  //Los elementos de la listaEstrenos

    private int resourceID;                                 //El layout en que se va a mostrar
    private FeedReaderDbHelper db;                          //Base de datos
    private int paginaEstrenos = 0;                                 //La página que se está mostrando (empezando por 0)
    private int ultPaginaEstrenos;                                  //El número (empezando por 1) de la última página
    private int paginaCartelera = 0;
    private int ultPaginaCartelera;
    private int peliculaPorPagina = 25;                     //Número de películas por página
    private MainActivity activity;                          //Actividad, para cambiar la IU
    private int expandido = -1;                             //Posición del elemento expandido

    private int estado = CARTELERA;

    private FragmentManager fragmentManager;

    /**
     * Constructor.
     * @param resourceID recurso con el layout de cada fila.
     * @param activity actividad principal, para acutalizar la IU
     * @param db la base de datos
     */
    public ListaModificadaAdapter(MainActivity activity, int resourceID, FeedReaderDbHelper db) {
        super(activity.getApplicationContext(),resourceID);
        Log.d(TAG, "Construyendo el adaptador de la listaEstrenos");
        this.resourceID = resourceID;
        this.activity = activity;
        this.db = db;
        ultPaginaEstrenos = 1;  //Temporal
        ultPaginaCartelera = 1;
        fragmentManager = activity.getSupportFragmentManager();
        HiloLeerBBDD hilo = new HiloLeerBBDD(db.getReadableDatabase(), db.getWritableDatabase(),this);
        hilo.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    }

    /**
     * Devuelve el número de filas en la listaEstrenos. Puede ser:
     *      0, si la fila está vaciá
     *      peliculaPorPagina, porque solo muestra paginas completas en teoría
     *      las que sea si están guardadas
     * @return devuelve un entero con el número de elementos en la listaEstrenos.
     */
    @Override
    public int getCount() {
        int count = 0;
        if(estado == HYPE){
            int i = 0;
            while(i < listaCartelera.size()) {
                if (listaCartelera.get(i).getisHyped())
                    count++;
                i++;
            }
            i = 0;
            while(i < listaEstrenos.size()) {
                if (listaEstrenos.get(i).getisHyped())
                    count++;
                i++;
            }
            return count;

        } else {
            ArrayList<Pelicula> lista = estado == CARTELERA ? listaCartelera : listaEstrenos;
            if (lista.size() > peliculaPorPagina)
                return peliculaPorPagina;
            else if (lista.size() > 0)
                return lista.size();
            else
                return 0;
        }

    }

    //Este método devuelve la posición en la listaEstrenos de Peliculas según la posición en la listaEstrenos.
    //No siempre es el mismo valor porque se usan varias páginas y a veces se muestran las que están
    //guardadas unicamente.
    //p es la posición en la listaEstrenos mostrada en pantalla. posicion es la posición en la listaEstrenos.
    private int getPosicionReal(int p) {
        int posicion = 0;
        if (estado == HYPE) {
            int i = 0;

            while (i < listaCartelera.size()) {
                if (listaCartelera.get(i).getisHyped()) {
                    if (p == 0) {
                        posicion = i;
                    }
                    p--;
                }
                i++;
            }

            int j = 0;
            while (j < listaEstrenos.size()) {
                if (listaEstrenos.get(j).getisHyped()) {
                    if (p == 0) {
                        posicion = i;
                    }
                    p--;
                }
                i++;
                j++;
            }

        } else if (estado == CARTELERA){
            posicion = p + paginaCartelera * peliculaPorPagina;
        } else
            posicion = p + paginaEstrenos * peliculaPorPagina;

        return posicion;

    }

    /**
     * Devuelve el elemento de la listaEstrenos de la posición elegida.
     * @param position entero con la posición del elemento en la listaEstrenos.
     * @return devuelve el objeto en la fila elegida.
     */
    @Override
    public Object getItem(int position) {
        return listaEstrenos.get(getPosicionReal(position));
    }

     /**
     * Crea la vista del elemento de la listaEstrenos.
     *
     * @param position entero con la posición del elemento en la listaEstrenos.
     * @param convertView vista de la fila.
     * @param parent vista de la listaEstrenos padre.
     * @return devuelve la fila modificada.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // workaround para que no se rompa si aún no está listaEstrenos la lectura
        View mView;
        mView = prepareView(position, convertView, parent);
        return mView;
    }

    public View prepareView(int position, View convertView, ViewGroup parent) {

        // workaround para que no se rompa si aún no está listaEstrenos la lectura
        View fila;

        if (convertView == null) {
            //Se añade una nueva view a la listaEstrenos.
            LayoutInflater inflater = (LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            fila = inflater.inflate(resourceID, parent, false);
        }else{
            fila = convertView;
        }

        Pelicula p;
        if(estado == HYPE){
            int preal = getPosicionReal(position);
            if (preal >= listaCartelera.size())
                p = listaEstrenos.get(preal-listaCartelera.size());
            else
                p = listaCartelera.get(preal);
        }else {
            p = (estado == CARTELERA ? listaCartelera : listaEstrenos).get(getPosicionReal(position));
        }
        ((TextView) fila.findViewById(R.id.titulo)).setText(p.getTitulo());
        ((TextView) fila.findViewById(R.id.estreno)).setText(p.getEstreno());
        ((ImageView) fila.findViewById(R.id.portada)).setImageBitmap(p.getPortada());

        if (p.getisHyped()) {
            fila.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
        } else
            fila.findViewById(R.id.hype_msg).setVisibility(View.GONE);

        if (expandido == position) {
            fila.findViewById(R.id.avanzado).setVisibility(View.VISIBLE);
            ((TextView) fila.findViewById(R.id.av_sinopsis)).setText(p.getSinopsis());

            fila.findViewById(R.id.av_fecha).setOnClickListener(enviar_Calendario);
            fila.findViewById(R.id.av_hype).setOnClickListener(get_hype);
            fila.findViewById(R.id.av_enlace).setOnClickListener(get_info);
            fila.findViewById(R.id.av_ficha).setOnClickListener(abre_ficha);
            fila.findViewById(R.id.av_compartir).setOnClickListener(compartir);

            if (p.getisHyped()){
                ((AppCompatImageButton) fila.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_black_24dp);
            }else{
                ((AppCompatImageButton) fila.findViewById(R.id.av_hype)).setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }
        } else
            fila.findViewById(R.id.avanzado).setVisibility(View.GONE);
        Log.v(TAG, "Añadiendo película " + p.getTitulo() + " a la vista número " + position);

        return fila;
    }

    /**
     * Añade un nuevo elemento al final de la listaEstrenos.
     *
     * @param p Pelicua con el nuevo elemento a introducir.
     */
    public void addCartelera(Pelicula p){
        listaCartelera.add(p);
    }
    public void addEstrenos(Pelicula p){
        listaEstrenos.add(p);
    }


    public void addCartelera(ArrayList <Pelicula> p){
        listaCartelera.addAll(p);
    }
    public void addEstrenos(ArrayList <Pelicula> p){
        listaEstrenos.addAll(p);
    }

    //Guarda el elemento que está expandido con más información.
    //Si se pulsa otra vez, se oculta
    public void setExpandido(int position){
        if (expandido == position){
            expandido = -1;
            Log.d(TAG, "Contrayendo  elemento " + position);
        } else {
            expandido = position;
            Log.d(TAG, "Expandiendo  elemento " + position);
        }
    }

    //Lo que hace este método es mostrar los elementos de la interfaz adecuados.
    //Cuando la listaEstrenos ya no esté vacía, muestra la barra de navegación y esconde el mensaje
    //Cuando haya una página nueva, mostrará el botón para pasar la página.
    //Todo pasar al navegador
    public void actualizarInterfaz(){
        if (estado != HYPE) {
            if(estado == CARTELERA) {
                if (listaCartelera.size() == 0) {
                    //  activity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
                    activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
                } else {
                    activity.findViewById(R.id.nopelis).setVisibility(View.GONE);
                    //   activity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                    if (paginaCartelera + 1 < ultPaginaCartelera) {
                        activity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
                    } else
                        activity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
                }
            }else{
                if (listaEstrenos.size() == 0) {
                    //  activity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
                    activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
                } else {
                    activity.findViewById(R.id.nopelis).setVisibility(View.GONE);
                    //   activity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                    if (paginaEstrenos + 1 < ultPaginaEstrenos) {
                        activity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
                    } else
                        activity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    //Si la listaEstrenos está vacía, muestra el mensaje de que no hay películas.
    public void noHayPelis(){
        if (((estado==CARTELERA)?listaCartelera:listaEstrenos).size()==0) {
            ((TextView) activity.findViewById(R.id.nopelis)).setText(R.string.no_pelis);
            activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);        }

        if(activity.getMenu()!= null) {
            activity.getMenu().findItem(R.id.actualizar).setEnabled(true);
            activity.getMenu().findItem(R.id.actualizar).setVisible(true);
            activity.getMenu().findItem(R.id.cancelar).setEnabled(false);
            activity.getMenu().findItem(R.id.cancelar).setVisible(false);
        }
    }


    private View.OnClickListener abre_ficha = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            Log.i(TAG, "Pulsado botón de abrir fichaFragment");
            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionReal(expandido);
                if (preal >= listaCartelera.size())
                    p = listaEstrenos.get(preal-listaCartelera.size());
                else
                    p = listaCartelera.get(preal);
            }else {
                p = (estado == CARTELERA ? listaCartelera : listaEstrenos).get(getPosicionReal(expandido));
            }            FichaFragment fichaFragment = FichaFragment.newInstance(p.getTitulo(), p.getEnlace());

            //activity.findViewById(R.id.ficha_container).setVisibility(View.VISIBLE);
            fragmentManager.beginTransaction().replace(R.id.ficha_container, fichaFragment).addToBackStack(null).commit();
        }
    };

    //Envía la película al calendario en forma de evento
    private View.OnClickListener enviar_Calendario = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = expandido;

            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionReal(position);
                if (preal >= listaCartelera.size())
                    p = listaEstrenos.get(preal-listaCartelera.size());
                else
                    p = listaCartelera.get(preal);
            }else {
                p = (estado == CARTELERA ? listaCartelera : listaEstrenos).get(getPosicionReal(position));
            }
            Calendar beginTime = Calendar.getInstance();
            String [] f = p.getFecha_estreno().split("/");
            int[] fecha = {Integer.parseInt(f[0]), Integer.parseInt(f[1]),Integer.parseInt(f[2])};
            beginTime.set(fecha[0], fecha[1]-1, fecha[2], 0, 0);
            // Primero comprobar si puedo editar el evento.
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                    .putExtra("allDay", true)
                    .putExtra(CalendarContract.Events.TITLE, p.getTitulo())
                    .putExtra(CalendarContract.Events.DESCRIPTION, p.getSinopsis());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            Log.d(TAG, "Solicitando al calendario almacenar la película " + p.getTitulo());

            getContext().startActivity(intent);
        }
    };

    //Guarda la película
    private View.OnClickListener get_hype = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            int position = expandido;
            int bbdd = estado;

            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionReal(position);
                if (preal >= listaCartelera.size()) {
                    p = listaEstrenos.get(preal - listaCartelera.size());
                    bbdd = ESTRENOS;
                }else {
                    p = listaCartelera.get(preal);
                    bbdd = CARTELERA;
                }
            }else {
                p = (estado == CARTELERA ? listaCartelera : listaEstrenos).get(getPosicionReal(position));
            }            Log.d(TAG, "Pulsado botón \"Hype\" en película " + p.getTitulo());

            SQLiteDatabase dbw = db.getWritableDatabase();
            ContentValues values = new ContentValues();
            String h;

            p.setisHyped(!p.getisHyped());

            if (p.getisHyped()) {
                h = "T";
                ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_black_24dp);
            } else {
                h = "F";
                ((AppCompatImageButton) v).setImageResource(R.drawable.ic_favorite_border_black_24dp);
            }

            if(bbdd == CARTELERA){
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
            int position = expandido;
            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionReal(position);
                if (preal >= listaCartelera.size())
                    p = listaEstrenos.get(preal-listaCartelera.size());
                else
                    p = listaCartelera.get(preal);
            }else {
                p = (estado == CARTELERA ? listaCartelera : listaEstrenos).get(getPosicionReal(position));
            }
            Log.d(TAG, "Pulsado botón \"Info\" en película " + p.getTitulo());

            // Instanciamos el intent de navegador
            Intent i = new Intent(Intent.ACTION_VIEW);
            // Se le pasa la web parseada
            i.setData(Uri.parse(p.getEnlace()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // Lanzamos el intent
            getContext().startActivity(i);
            // profit!

        }
    };

    private View.OnClickListener compartir = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            int position = expandido;
            Pelicula p;
            if(estado == HYPE){
                int preal = getPosicionReal(position);
                if (preal >= listaCartelera.size())
                    p = listaEstrenos.get(preal-listaCartelera.size());
                else
                    p = listaCartelera.get(preal);
            }else {
                p = (estado == CARTELERA ? listaCartelera : listaEstrenos).get(getPosicionReal(position));
            }
            Log.d(TAG, "Pulsado botón \"Share\" en película " + p.getTitulo());

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "He compartido \"" + p.getTitulo() + "\" a través de Hype!");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, p.getEnlace());
            activity.startActivity(Intent.createChooser(sharingIntent, "Compartir película: " + p.getTitulo() + "."));

        }
    };


    public void pasarPagina(int i){
        if (estado == CARTELERA)
            paginaCartelera = i;
        else if (estado == ESTRENOS)
            paginaEstrenos = i;
        expandido = -1;
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
        this.ultPaginaEstrenos = listaEstrenos.size() / peliculaPorPagina;
        this.ultPaginaCartelera = listaCartelera.size() / peliculaPorPagina;

    }

    public void eliminarLista() {
        listaEstrenos.clear();
        listaCartelera.clear();
    }

    public int getEstado(){
        return estado;
    }

    public void mostrarEstrenos(){
        expandido = -1;
        estado = ESTRENOS;
    }

    public void mostrarHype(){
        expandido = -1;
        estado = HYPE;
    }

    // TODO: Hacer que muestre la cartelera de verdad
    public void mostrarCartelera(){
        expandido = -1;
        estado = CARTELERA;
    }


    public int getUltPagina() {
        if (estado==CARTELERA)
            return ultPaginaCartelera;
        else if (estado==ESTRENOS)
            return ultPaginaEstrenos;
        else
            return 0;
    }
}
