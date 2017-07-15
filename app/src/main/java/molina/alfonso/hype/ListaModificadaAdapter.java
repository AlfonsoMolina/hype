package molina.alfonso.hype;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v4.content.ContextCompat;
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
 * según la naturaleza de la lista.
 *
 * @author Alfonso Molina
 */
public class ListaModificadaAdapter extends ArrayAdapter {

    private ArrayList<Pelicula> lista = new ArrayList<>();  //Los elementos de la lista
    private int resourceID;                                           //El layout en que se va a mostrar
    private SQLiteDatabase db;

    /**
     * Constructor.
     * @param context contexto de la actividad.
     * @param resourceID recurso con el layout de cada fila.
     */
    public ListaModificadaAdapter(Context context, int resourceID, SQLiteDatabase db) {
        super(context,resourceID);
        this.resourceID = resourceID;
        this.db = db;
        //Inicia la lista con las pelis en la bbdd
        leerBBDD();

    }

    private void leerBBDD(){
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
                FeedReaderContract.FeedEntry.COLUMN_HYPE
        };

        Cursor cursor = db.query(
                FeedReaderContract.FeedEntry.TABLE_NAME,                     // The table to query
                projection,                               // The columns to return
                null,                                // The columns for the WHERE clause
                null,                                     // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                FeedReaderContract.FeedEntry.COLUMN_FECHA+" ASC"                                    // The sort order
        );

        String l, p, t, s, e, f, h;

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

            //Si fecha_hoy después que f, >0
            // si al revés, < 0
            if(fecha_hoy.compareTo(f)>0) {
                String selection = FeedReaderContract.FeedEntry.COLUMN_REF + " LIKE ?";
                // Specify arguments in placeholder order.
                String[] selectionArgs = { l };
                // Issue SQL statement.
                db.delete(FeedReaderContract.FeedEntry.TABLE_NAME, selection, selectionArgs);
            }else
                this.add(new Pelicula(l,p,t,s,e,f,h.equalsIgnoreCase("T")));
        }
        cursor.close();

    }

    /**
     * Devuelve el número de filas en la lista.
     * @return devuelve un entero con el número de elementos en la lista.
     */
    @Override
    public int getCount() {
        return this.lista.size();
    }

    /**
     * Devuelve el elemento de la lista de la posición elegida.
     * @param position entero con la posición del elemento en la lista.
     * @return devuelve el objeto en la fila elegida.
     */
    @Override
    public Object getItem(int position) {
        return this.lista.get(position);
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

        View fila = convertView;

        if (convertView == null) {
            //Se añade una nueva view a la lista.
            LayoutInflater inflater = (LayoutInflater) this.getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            fila = inflater.inflate(resourceID, parent, false);
        }

        Pelicula p = lista.get(position);

        ((TextView) fila.findViewById(R.id.titulo)).setText(p.getTitulo());
        ((TextView) fila.findViewById(R.id.sinopsis)).setText(p.getSinopsis());
        ((TextView) fila.findViewById(R.id.estreno)).setText(p.getEstreno());
        ((ImageView) fila.findViewById(R.id.portada)).setImageBitmap(p.getPortada());

        if (p.getisPressed()){
            fila.setBackgroundResource(R.drawable.gradient_bg_hover);
        } else
            fila.setBackgroundResource(R.drawable.gradient_bg);
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

    public void add(ArrayList<Pelicula> p) {
        lista.addAll(p);
    }
     /**
     * Elimina una fila de la lista.
     *
     * @param posicion entero con la posición del elemento a eliminar
     */
    public void remove(int posicion){
        lista.remove(posicion);
    }


    public void actualizar() {
        lista.clear();
        leerBBDD();
    }

    public void delete(int i) {
        for (int j = 0; j<i; j++)
            lista.remove(j);
    }

    public Pelicula getPelicula (int position) {
        return lista.get(position);
    }

    public void setIsPressed(int position, boolean isPressed) {
        lista.get(position).setisPressed(isPressed);
    }
}
