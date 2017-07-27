package molina.alfonso.hype;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

/**
 * Adaptador creado para las diferentes listas. Tiene un comportamiento diferente
 * según la naturaleza de la lista.
 *
 * @author Alfonso Molina
 */
public class ListaModificadaAdapter extends ArrayAdapter {

    /*
     * Declaración de variables
     */

    private static final String TAG = "ListaModificadaAdapter";

    private ArrayList<Pelicula> lista = new ArrayList<>();  //Los elementos de la lista
    private int resourceID;                                 //El layout en que se va a mostrar
    private FeedReaderDbHelper db;                          //Base de datos
    private boolean mostrarHype = false;                    //¿Se están mostrando las guardadas o todas?
    private int pagina = 0;                                 //La página que se está mostrando (empezando por 0)
    private int ultPagina;                                  //El número (empezando por 1) de la última página
    private int peliculaPorPagina = 25;                     //Número de películas por página
    private Activity activity;                              //Actividad, para cambiar la IU
    private int expandido = -1;                             //Posición del elemento expandido

    /**
     * Constructor.
     * @param resourceID recurso con el layout de cada fila.
     * @param activity actividad principal, para acutalizar la IU
     * @param db la base de datos
     */
    public ListaModificadaAdapter(Activity activity, int resourceID, FeedReaderDbHelper db) {
        super(activity.getApplicationContext(),resourceID);
        Log.d(TAG, "Construyendo el adaptador de la lista");
        this.resourceID = resourceID;
        this.activity = activity;
        this.db = db;
        ultPagina = 1;  //Temporal
        HiloLeerBBDD hilo = new HiloLeerBBDD(db.getReadableDatabase(),this,
                (LinearLayout)activity.findViewById(R.id.carga_barra),
                (TextView) activity.findViewById(R.id.carga_mensaje));
        hilo.execute();

    }



    /**
     * Devuelve el número de filas en la lista. Puede ser:
     *      0, si la fila está vaciá
     *      peliculaPorPagina, porque solo muestra paginas completas en teoría
     *      las que sea si están guardadas
     * @return devuelve un entero con el número de elementos en la lista.
     */
    @Override
    public int getCount() {
        int count = 0;
        if(mostrarHype){
            int i = 0;
            while(i < lista.size()) {
                if (lista.get(i).getisHyped())
                    count++;
                i++;
            }
            return count;
        }
        else if (lista.size() > peliculaPorPagina)
            return peliculaPorPagina;
        else if (lista.size() > 0 )
            return lista.size();
        else
            return 0;

    /*    else if (lista.size() == 0)
            return 0;
        else
            return peliculaPorPagina;*/
    }

    //Este método devuelve la posición en la lista de Peliculas según la posición en la lista.
    //No siempre es el mismo valor porque se usan varias páginas y a veces se muestran las que están
    //guardadas unicamente.
    //p es la posición en la lista mostrada en pantalla. posicion es la posición en la lista.
    private int getPosicionReal(int p) {
        int posicion = 0;
        if (mostrarHype) {
            int i = 0;
            while (i < lista.size()) {
                if (lista.get(i).getisHyped()) {
                    if (p == 0) {
                        posicion = i;
                    }
                    p--;
                }
                i++;
            }
        } else {
            posicion = p + pagina * peliculaPorPagina;
        }

        return posicion;

    }

    /**
     * Devuelve el elemento de la lista de la posición elegida.
     * @param position entero con la posición del elemento en la lista.
     * @return devuelve el objeto en la fila elegida.
     */
    @Override
    public Object getItem(int position) {
        return lista.get(getPosicionReal(position));
    }

     /**
     * Crea la vista del elemento de la lista.
     *
     * @param position entero con la posición del elemento en la lista.
     * @param convertView vista de la fila.
     * @param parent vista de la lista padre.
     * @return devuelve la fila modificada.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // workaround para que no se rompa si aún no está lista la lectura
        View mView;
        mView = prepareView(position, convertView, parent);
        return mView;
    }

    public View prepareView(int position, View convertView, ViewGroup parent) {

        // workaround para que no se rompa si aún no está lista la lectura
        View fila;

        if (convertView == null) {
            //Se añade una nueva view a la lista.
            LayoutInflater inflater = (LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            fila = inflater.inflate(resourceID, parent, false);
        }else{
            fila = convertView;
        }

        Pelicula p = lista.get(getPosicionReal(position));

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
        } else
            fila.findViewById(R.id.avanzado).setVisibility(View.GONE);
        Log.v(TAG, "Añadiendo película " + p.getTitulo() + " a la vista número " + position);
        return fila;
    }

    /**
     * Añade un nuevo elemento al final de la lista.
     *
     * @param p Pelicua con el nuevo elemento a introducir.
     */
    public void add(Pelicula p){
        lista.add(p);
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
    //Cuando la lista ya no esté vacía, muestra la barra de navegación y esconde el mensaje
    //Cuando haya una página nueva, mostrará el botón para pasar la página.
    public void actualizarInterfaz(){
        if (!mostrarHype) {
            if (lista.size() == 0) {
                activity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
                activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
            } else {
                activity.findViewById(R.id.nopelis).setVisibility(View.GONE);
                activity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                if (pagina + 1 < getUltPagina()) {
                    activity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
                } else
                    activity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
            }
        }
    }

    //Si la lista está vacía, muestra el mensaje de que no hay películas.
    public void noHayPelis(){
        if (lista.size()==0) {
            ((TextView) activity.findViewById(R.id.nopelis)).setText(R.string.no_pelis);
            activity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);        }
    }

    //Guarda si se muestran las películas guardadas o todas
    public boolean toogleHype() {
        this.mostrarHype = !this.mostrarHype;
        expandido = -1;
        return mostrarHype;
    }

    //Envía la película al calendario en forma de evento
    private View.OnClickListener enviar_Calendario = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = expandido;
            Pelicula p = lista.get(getPosicionReal(position));

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
            Pelicula p = lista.get(getPosicionReal(position));
            Log.d(TAG, "Pulsado botón \"Hype\" en película " + p.getTitulo());

            SQLiteDatabase dbw = db.getWritableDatabase();
            ContentValues values = new ContentValues();
            String h;

            p.setisHyped(!p.getisHyped());

            if (p.getisHyped()) {
                h = "T";
            } else
                h = "F";

            values.put(FeedReaderContract.FeedEntry.COLUMN_HYPE, h);

            String selection = FeedReaderContract.FeedEntry.COLUMN_REF + " LIKE ?";
            String[] selectionArgs = {p.getEnlace()};

            int count = dbw.update(
                    FeedReaderContract.FeedEntry.TABLE_NAME,
                    values,
                    selection,
                    selectionArgs);

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
            Pelicula p = lista.get(getPosicionReal(position));

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

    public void pasarPagina(int i){
        pagina = i;
        expandido = -1;
    }

    public int getPagina(){
        return pagina;
    }

    public int getUltPagina() {
        return ultPagina;
    }

    public void setMaxPaginas() {
        this.ultPagina = lista.size() / peliculaPorPagina;
    }

}
