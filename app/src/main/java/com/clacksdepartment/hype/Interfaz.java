package com.clacksdepartment.hype;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.widget.ImageView;
import android.widget.TextView;

class Interfaz {

    private MainActivity mMainActivity;
//    private ListaModificadaAdapter mListaNueva;
    private listaNueva mListaNueva;

  /*  Interfaz(MainActivity mainActivity, ListaModificadaAdapter listaModificadaAdapter){
        this.mMainActivity = mainActivity;
        this.mListaNueva = listaModificadaAdapter;
    }*/

    Interfaz(MainActivity mainActivity, listaNueva listaNueva){
        this.mMainActivity = mainActivity;
        this.mListaNueva = listaNueva;
    }

    void seleccionaBotonHype(){
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.hype), true);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.cartelera), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.estrenos), false);
        ((TextView) mMainActivity.findViewById(R.id.seccion)).setText(R.string.texto_seccion_hype);

    }

    void seleccionaBotonCartelera(){
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.hype), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.cartelera), true);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.estrenos), false);
        ((TextView) mMainActivity.findViewById(R.id.seccion)).setText(R.string.texto_seccion_cartelera);
    }

    void seleccionaBotonEstrenos(){
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.hype), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.cartelera), false);
        coloreaBoton((ImageView) mMainActivity.findViewById(R.id.estrenos), true);
        ((TextView) mMainActivity.findViewById(R.id.seccion)).setText(R.string.texto_seccion_estrenos);
    }

    void mostrarPaginador(Boolean mostrar){
        if (mostrar){
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.VISIBLE);
            int pagina = mListaNueva.getPagina();
            String txt = "" + (pagina+1);
            ((TextView) mMainActivity.findViewById(R.id.paginaActual)).setText(txt);

            mMainActivity.findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
            mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);

            if (pagina == 0)
                mMainActivity.findViewById(R.id.previousPageButton).setVisibility(View.INVISIBLE);

            if (pagina+1 == mListaNueva.getUltPagina())
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

    void enfocaPrimerElementoSuave(){
        ((RecyclerView) mMainActivity.findViewById(R.id.lista)).smoothScrollToPosition(0);
    }

    void enfocaPrimerElementoBrusco(){
        ((RecyclerView) mMainActivity.findViewById(R.id.lista)).scrollToPosition(0);
        mMainActivity.findViewById(R.id.lista).requestFocus();
    }

    void animaListado(){
        LayoutAnimationController layoutAnimationController = AnimationUtils.loadLayoutAnimation(mMainActivity,R.anim.rellenar_lista);
        ((RecyclerView) mMainActivity.findViewById(R.id.lista)).setLayoutAnimation(layoutAnimationController);
    }

}
