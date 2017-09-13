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
    private RecyclerViewAdapter mRecyclerViewAdapter;

  /*  Interfaz(MainActivity mainActivity, ListaModificadaAdapter listaModificadaAdapter){
        this.mMainActivity = mainActivity;
        this.mListaNueva = listaModificadaAdapter;
    }*/

    Interfaz(MainActivity mainActivity, RecyclerViewAdapter RecyclerViewAdapter){
        this.mMainActivity = mainActivity;
        this.mRecyclerViewAdapter = RecyclerViewAdapter;
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

            // Quiero mostrar el paginador
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.VISIBLE);

            // Actualizo el número de la página
            int pagina = mRecyclerViewAdapter.getPagina();
            String txt = "" + (pagina+1);
            ((TextView) mMainActivity.findViewById(R.id.paginaActual)).setText(txt);

            // Evaluamos la página en la que estamos, casos: prim. pagina, ultima pagina, cualquier pagina en medio
            if (pagina == 0){
                mMainActivity.findViewById(R.id.previousPageButton).setVisibility(View.GONE);
            }else if (pagina+1 == mRecyclerViewAdapter.getUltPagina()){
                mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.GONE);
            }else{
                mMainActivity.findViewById(R.id.previousPageButton).setVisibility(View.VISIBLE);
                mMainActivity.findViewById(R.id.nextPageButton).setVisibility(View.VISIBLE);
            }

        }else{
            // Quiero ocultar el navegador
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.GONE);
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

    void actualizar(){

        if (mRecyclerViewAdapter.getEstado() != RecyclerViewAdapter.HYPE) {
            if (mRecyclerViewAdapter.getItemCount() == 0 ) {
                mostrarPaginador(false);
                mostrarNoHayPelis(true);
            } else if (mRecyclerViewAdapter.getUltPagina() > 1) {
                mostrarPaginador(true);
                mostrarNoHayPelis(false);
            } else {
                mostrarPaginador(false);
                mostrarNoHayPelis(false);
            }
        }else{
            mostrarPaginador(false);
            if (mRecyclerViewAdapter.getItemCount() == 0) {
                mostrarNoHayPelis(true);
            } else {
                mostrarNoHayPelis(false);
            }
        }

    }
}
