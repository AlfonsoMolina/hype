package molina.alfonso.hype;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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

import org.w3c.dom.Text;

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
    private int resourceID;                                           //El layout en que se va a mostrar
    private FeedReaderDbHelper db;
    private boolean mostrarHype = false;
    private int pagina = 0;
    private int maxPaginas = 10;
    private int peliculaPorPagina = 25;
    private Activity activity;

    private int expandido = -1;

    /**
     * Constructor.
     * @param resourceID recurso con el layout de cada fila.
     */
    public ListaModificadaAdapter(Activity activity, int resourceID, SQLiteDatabase db) {
        super(activity.getApplicationContext(),resourceID);
        Log.d(TAG, "ListaModificadaAdapter");
        this.resourceID = resourceID;
        this.activity = activity;
        //Inicia la lista con las pelis en la bbdd
        //leerBBDD();
        maxPaginas = 1;  //Las paginas siempre tendran 25, si hay más peliculas no se muestran
        HiloLeerBBDD hilo = new HiloLeerBBDD(db,this,(LinearLayout)activity.findViewById(R.id.carga_barra),(TextView) activity.findViewById(R.id.carga_mensaje));
        hilo.execute();


    }

    public void actualizarInterfaz(){
        if (!mostrarHype) {
            if (lista.size() == 0) {
                activity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
                activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
            } else {
                activity.findViewById(R.id.nopelis).setVisibility(View.GONE);
                activity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
                if (pagina + 1 < getMaxPaginas()) {
                    activity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
                } else
                    activity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);
            }
        }
    }

    public void noHayPelis(){
        if (lista.size()==0) {
            ((TextView) activity.findViewById(R.id.nopelis)).setText(R.string.no_pelis);
            activity.findViewById(R.id.navegacion).setVisibility(View.INVISIBLE);
            activity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);        }
    }
    private void leerBBDD(){
        Log.d(TAG, "leerBBDD");

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                FeedReaderContract.FeedEntry._ID,
                FeedReaderContract.FeedEntry.COLUMN_TITULO,
                FeedReaderContract.FeedEntry.COLUMN_PORTADA,
                FeedReaderContract.FeedEntry.COLUMN_REF,
                FeedReaderContract.FeedEntry.COLUMN_SINOPSIS,
                FeedReaderContract.FeedEntry.COLUMN_ESTRENO,
                FeedReaderContract.FeedEntry.COLUMN_FECHA,
                FeedReaderContract.FeedEntry.COLUMN_HYPE,
                FeedReaderContract.FeedEntry.COLUMN_CORTO
        };

        SQLiteDatabase dbr = db.getReadableDatabase();
        Cursor cursor = dbr.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntry.COLUMN_FECHA+" ASC"                                    // The sort order
        );

        String l, p, t, s, e, f, h, fc;

        String year = "" + Calendar.getInstance().get(Calendar.YEAR);
        int month_i = Calendar.getInstance().get(Calendar.MONTH)+1;
        int day_i = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);

        String month;
        String day;

        if (month_i < 10)
            month = "0" + month_i;
        else
            month = ""+month_i;

        if (day_i < 10)
            day = "0" + day_i;
        else
            day = "" + day_i;

        String fecha_hoy = year + '/' + month + '/' + day;

        while(cursor.moveToNext()) {
            l = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_REF));
            p = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_PORTADA));
            t = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_TITULO));
            s = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_SINOPSIS));
            e = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_ESTRENO));
            f = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_FECHA));
            h = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_HYPE));
            fc = cursor.getString(cursor.getColumnIndexOrThrow(FeedReaderContract.FeedEntry.COLUMN_CORTO));


            //Si fecha_hoy después que f, >0
            // si al revés, < 0
            if(fecha_hoy.compareTo(f)>0) {
                String selection = FeedReaderContract.FeedEntry.COLUMN_REF + " LIKE ?";
                // Specify arguments in placeholder order.
                String[] selectionArgs = { l };
                // Issue SQL statement.
                dbr.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
            }else
                this.add(new Pelicula(l,p,t,s,e,f,fc,h.equalsIgnoreCase("T")));
        }
        cursor.close();

    }

    /**
     * Devuelve el número de filas en la lista.
     * @return devuelve un entero con el número de elementos en la lista.
     */
    @Override
    public int getCount() {
        Log.d(TAG, "getCount");

        int count = 0;

        if(mostrarHype){
            int i = 0;
            while(i < lista.size()) {
                if (lista.get(i).getisPressed())
                    count++;
                i++;
            }
            return count;
        }

        else if (lista.size() == 0)
            return 0;
        else
            return peliculaPorPagina;

    }

    /**
     * Devuelve el elemento de la lista de la posición elegida.
     * @param position entero con la posición del elemento en la lista.
     * @return devuelve el objeto en la fila elegida.
     */
    @Override
    public Object getItem(int position) {
        Log.d(TAG, "getItem");

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
        Log.d(TAG, "getView");
        View fila = convertView;

        if (convertView == null) {
            //Se añade una nueva view a la lista.
            LayoutInflater inflater = (LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            fila = inflater.inflate(resourceID, parent, false);
        }

        int cuenta = getPosicionReal(position);
        Pelicula p = lista.get(cuenta);

        ((TextView) fila.findViewById(R.id.titulo)).setText(p.getTitulo());
        ((TextView) fila.findViewById(R.id.estreno)).setText(p.getEstreno());
        ((ImageView) fila.findViewById(R.id.portada)).setImageBitmap(p.getPortada());

        if (p.getisPressed()){
            fila.findViewById(R.id.hype_msg).setVisibility(View.VISIBLE);
        } else
            fila.findViewById(R.id.hype_msg).setVisibility(View.GONE);

        if (expandido == position){
            fila.findViewById(R.id.avanzado).setVisibility(View.VISIBLE);
            ((TextView) fila.findViewById(R.id.av_fecha)).setText(p.getEstreno_corto());
            ((TextView) fila.findViewById(R.id.av_sinopsis)).setText(p.getSinopsis());
            fila.findViewById(R.id.av_fecha).setOnClickListener(enviar_Calendario);
            fila.findViewById(R.id.av_hype).setOnClickListener(get_hype);
            fila.findViewById(R.id.av_enlace).setOnClickListener(get_info);
        } else
            fila.findViewById(R.id.avanzado).setVisibility(View.GONE);

        return fila;
    }

    /**
     * Añade un nuevo elemento al final de la lista.
     *
     * @param p Pelicua con el nuevo elemento a introducir.
     */
    public void add(Pelicula p){
        Log.d(TAG, "add");
        lista.add(p);
    }

    public void add(ArrayList<Pelicula> p) {
        Log.d(TAG, "add");
        lista.addAll(p);
    }
     /**
     * Elimina una fila de la lista.
     *
     * @param position entero con la posición del elemento a eliminar
     */
    public void remove(int position) {
        Log.d(TAG, "remove");
        int cuenta = getPosicionReal(position);
        lista.remove(cuenta);
    }

    public void delete(int i) {
        Log.d(TAG, "delete");
        for (int j = 0; j<i; j++)
            lista.remove(j);
    }

    public Pelicula getPelicula (int position) {
        Log.d(TAG, "getPelicula");
        return lista.get(getPosicionReal(position));
    }
    public void setIsPressed(int position, boolean isPressed) {
        Log.d(TAG, "setIsPressed");
        lista.get(getPosicionReal(position)).setisPressed(isPressed);
    }


    public void setExpandido(int position){
        Log.d(TAG, "setExpandido");
        if (expandido == position){
            expandido = -1;
        } else
            expandido = position;
    }

    private View.OnClickListener enviar_Calendario = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "enviar_Calendario");

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
            getContext().startActivity(intent);
        }
    };

    private View.OnClickListener get_hype = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.d(TAG, "get_hype");

            int position = expandido;
            Pelicula p = lista.get(getPosicionReal(position));

            SQLiteDatabase dbw = db.getWritableDatabase();
            ContentValues values = new ContentValues();
            String h;

            p.setisPressed(!p.getisPressed());

            if (p.getisPressed()) {
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

    public boolean toogleHype() {
        this.mostrarHype = !this.mostrarHype;
        expandido = -1;
        return mostrarHype;
    }
    /*
     * Método llamado al pedir más info en una peli seleccionada.
     */
    private View.OnClickListener get_info = new View.OnClickListener(){

        @Override
        public void onClick(View view) {

            Log.d(TAG, "get_info");
            int position = expandido;
            Pelicula p = lista.get(getPosicionReal(position));


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

    public int getMaxPaginas() {
        return maxPaginas;
    }

    public void setMaxPaginas(){
        this.maxPaginas = lista.size()/peliculaPorPagina;
    }


    //Este método devuelve la posición en la lista de Peliculas según la posición en la lista.
    //No siempre es el mismo valor porque se usan varias páginas y a veces se muestran las que están
    //guardadas unicamente.
    private int getPosicionReal(int p){
        int posicion = 0;
        if(mostrarHype){
            int i = 0;
            while (i < lista.size()){
                if (lista.get(i).getisPressed()) {
                    if (p == 0) {
                        posicion = i;
                    }
                    p--;
                }
                i++;
            }
        } else {
            posicion = p+pagina*peliculaPorPagina;
        }

        return posicion;

    }

}
