package com.clacksdepartment.hype;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

class Interfaz {

    private MainActivity mMainActivity;
    private RecyclerViewAdapter mRecyclerViewAdapter;

    private LayoutAnimationController layoutAnimationController;

    Interfaz(MainActivity mainActivity, RecyclerViewAdapter RecyclerViewAdapter){
        this.mMainActivity = mainActivity;
        this.mRecyclerViewAdapter = RecyclerViewAdapter;

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(mMainActivity,R.anim.rellenar_lista);
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

            // Actualizo el número de la página
            String txt = "" + (mRecyclerViewAdapter.getPagina()+1);
            ((TextView) mMainActivity.findViewById(R.id.paginaActual)).setText(txt);

            // Evaluamos la página en la que estamos, casos: prim. pagina, ultima pagina, cualquier pagina en medio

            if (mRecyclerViewAdapter.getPagina() == 0){
                animaBoton(mMainActivity.findViewById(R.id.previousPageButton), false);
                animaBoton(mMainActivity.findViewById(R.id.nextPageButton), true);
            }else if ((mRecyclerViewAdapter.getPagina()+1) == mRecyclerViewAdapter.getUltPagina()){
                animaBoton(mMainActivity.findViewById(R.id.previousPageButton), true);
                animaBoton(mMainActivity.findViewById(R.id.nextPageButton), false);
            }else{
                animaBoton(mMainActivity.findViewById(R.id.previousPageButton), true);
                animaBoton(mMainActivity.findViewById(R.id.nextPageButton), true);
            }

            // Quiero mostrar el paginador
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.VISIBLE);

        }else{
            // Quiero ocultar el navegador
            mMainActivity.findViewById(R.id.paginador).setVisibility(View.GONE);
            animaBoton(mMainActivity.findViewById(R.id.previousPageButton), false);
            animaBoton(mMainActivity.findViewById(R.id.nextPageButton), false);
        }
    }

    void mostrarNoHayPelis(Boolean mostrar){
        if (mostrar){
            ((TextView) mMainActivity.findViewById(R.id.nopelis)).setText(
                    mRecyclerViewAdapter.getEstado() == RecyclerViewAdapter.HYPE?
                            R.string.no_hype : R.string.no_pelis);
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
        ((RecyclerView) mMainActivity.findViewById(R.id.lista)).setLayoutAnimation(layoutAnimationController);
    }

    void actualizar(){

        if (mRecyclerViewAdapter.getEstado() != RecyclerViewAdapter.HYPE) {
            if (mRecyclerViewAdapter.getItemCount() == 1 ) {
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
            if (mRecyclerViewAdapter.getItemCount() == 1) {
                mostrarNoHayPelis(true);
            } else {
                mostrarNoHayPelis(false);
            }
        }

    }

    void mostrarCabecera(Boolean b){
        if (b){
            mMainActivity.findViewById(R.id.navegacion).setVisibility(View.VISIBLE);
        } else
            mMainActivity.findViewById(R.id.navegacion).setVisibility(View.GONE);
    }

    private void animaBoton(final View v, Boolean mostrar){

        final float initialAlpha = v.getAlpha();
        final float targetAlpha;

        if (mostrar){
            targetAlpha = 1.0f;
        }else{
            targetAlpha = 0.2f;
        }

        final float alphaIncrease = targetAlpha - initialAlpha;

        Animation a = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {

                if (interpolatedTime == 1){
                    v.setAlpha(targetAlpha);
                }else{
                    v.setAlpha(initialAlpha + alphaIncrease * interpolatedTime);
                }

            }

            @Override
            public boolean willChangeBounds() {
                return false;
            }

        };

        // 1dp/ms
        a.setDuration(250);
        v.startAnimation(a);
    }


}
