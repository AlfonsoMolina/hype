package molina.alfonso.hype;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by vicente on 9/9/17.
 */

public class Navegador {

    MainActivity mainActivity;
    ListaModificadaAdapter lista;

    Navegador(MainActivity mainActivity, ListaModificadaAdapter lista){
        this.mainActivity = mainActivity;
        this.lista = lista;
    }

    public void seleccionaHype(){
        seleccionar((ImageView) mainActivity.findViewById(R.id.hype), true);
        seleccionar((ImageView) mainActivity.findViewById(R.id.cartelera), false);
        seleccionar((ImageView) mainActivity.findViewById(R.id.estrenos), false);
    }

    public void seleccionaCartelera(){
        seleccionar((ImageView) mainActivity.findViewById(R.id.hype), false);
        seleccionar((ImageView) mainActivity.findViewById(R.id.cartelera), true);
        seleccionar((ImageView) mainActivity.findViewById(R.id.estrenos), false);
    }

    public void seleccionaEstrenos(){
        seleccionar((ImageView) mainActivity.findViewById(R.id.hype), false);
        seleccionar((ImageView) mainActivity.findViewById(R.id.cartelera), false);
        seleccionar((ImageView) mainActivity.findViewById(R.id.estrenos), true);
    }

    public void mostrarPaginador(Boolean mostrar){
        if (mostrar){
            mainActivity.findViewById(R.id.paginador).setVisibility(View.VISIBLE);
            int pagina = lista.getPagina();
            String txt = "" + (pagina+1);
            ((TextView) mainActivity.findViewById(R.id.paginaActual)).setText(txt);

            mainActivity.findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
            mainActivity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);

            if (pagina == 0)
                mainActivity.findViewById(R.id.previousPageButton).setVisibility(View.INVISIBLE);

            if (pagina+1 == lista.getUltPagina())
                mainActivity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);

        }else{
            mainActivity.findViewById(R.id.paginador).setVisibility(View.INVISIBLE);
        }
    }

    public void mostrarNoPelis(Boolean mostrar){
        if (mostrar){
            ((TextView) mainActivity.findViewById(R.id.nopelis)).setText("\n\nNinguna película guardada.\nPor ahora.");
            mainActivity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
        }else{
            mainActivity.findViewById(R.id.nopelis).setVisibility(View.GONE);
        }
    }

    private void seleccionar(ImageView imageView, Boolean seleccionado){
        if (seleccionado){
            imageView.setColorFilter(ContextCompat.getColor(mainActivity,R.color.colorSelectedButton));
        }else{
            imageView.setColorFilter(ContextCompat.getColor(mainActivity,R.color.colorNonSelectedButton));
        }
    }

}
