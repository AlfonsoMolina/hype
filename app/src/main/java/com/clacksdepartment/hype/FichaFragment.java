package com.clacksdepartment.hype;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FichaFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FichaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FichaFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TÍTULO = "Título";
    private static final String LINK = "LINK";

    private static final String TAG = "FichaFragment";

    private String titulo;
    private String link;

    private FichaFA fichaFA;
    private FichaTMDB fichaTMDB;
    private ViewGroup contenedor;

    private OnFragmentInteractionListener mListener;

    public FichaFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FichaFragment.
     */
    public static FichaFragment newInstance(String param1, String param2) {
        FichaFragment fragment = new FichaFragment();
        Bundle args = new Bundle();
        args.putString(TÍTULO, param1);
        args.putString(LINK, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            titulo = getArguments().getString(TÍTULO);
            link = getArguments().getString(LINK);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i(TAG, "FichaFragment abierta");

        contenedor = container;
        contenedor.setVisibility(View.VISIBLE);

        return inflater.inflate(R.layout.fragment_ficha, container, false);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Fijamos el título, ya conocido, para ir rellenando algo la fichaFA...
        ((TextView) view.findViewById(R.id.ficha_titulo)).setText(titulo);

        // Creamos la fichaFA con su constructor, lo que va agilizando algunas operaciones de compilación de regex
        //fichaFA = new FichaFA(link, view);

        if (link.contains("themoviedb")){
            fichaTMDB = new FichaTMDB(link, view);
            fichaTMDB.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }else if (link.contains("filmaffinity")){
            fichaFA = new FichaFA(link, view);
            fichaFA.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        contenedor.setVisibility(View.GONE);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
