package molina.alfonso.hype;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by vicente on 9/9/17.
 */

class Interfaz {

    private MainActivity mMainActivity;
    private ListaModificadaAdapter mListaModificadaAdapter;

    Interfaz(MainActivity mainActivity, ListaModificadaAdapter listaModificadaAdapter){
        this.mMainActivity = mainActivity;
        this.mListaModificadaAdapter = listaModificadaAdapter;
    }

    void seleccionaBotonHype(){
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.hype), true);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.cartelera), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.estrenos), false);
    }

    void seleccionaBotonCartelera(){
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.hype), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.cartelera), true);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.estrenos), false);
    }

    void seleccionaBotonEstrenos(){
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.hype), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.cartelera), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.estrenos), true);
    }

    void mostrarPaginador(Boolean mostrar){
        if (mostrar){
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.VISIBLE);
            int pagina = mListaModificadaAdapter.getPagina();
            String txt = "" + (pagina+1);
            ((TextView) mMainActivity.findViewById(R.id.paginaActual)).setText(txt);

            mMainActivity.findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
            mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);

            if (pagina == 0)
                mMainActivity.findViewById(R.id.previousPageButton).setVisibility(View.INVISIBLE);

            if (pagina+1 == mListaModificadaAdapter.getUltPagina())
                mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.INVISIBLE);

        }else{
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.INVISIBLE);
        }
    }

    void mostrarNoHayPelis(Boolean mostrar){
        if (mostrar){
            ((TextView) mMainActivity.findViewById(R.id.nopelis)).setText(R.string.no_pelis);
            mMainActivity.findViewById(R.id.nopelis).setVisibility(View.VISIBLE);
        }else{
            mMainActivity.findViewById(R.id.nopelis).setVisibility(View.GONE);
        }
    }

    private void coloreaBoton(ImageView imageView, Boolean seleccionado){
        if (seleccionado){
            imageView.setColorFilter(ContextCompat.getColor(mMainActivity,R.color.colorSelectedButton));
        }else{
            imageView.setColorFilter(ContextCompat.getColor(mMainActivity,R.color.colorNonSelectedButton));
        }
    }

    void enfocaPrimerElemento(){
        ((ListView) mMainActivity.findViewById(R.id.lista)).smoothScrollToPosition(0);
    }

}
