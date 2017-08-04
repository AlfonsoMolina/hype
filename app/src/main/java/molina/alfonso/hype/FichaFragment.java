package molina.alfonso.hype;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FichaFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FichaFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FichaFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TÍTULO = "Título";
    private static final String LINK = "LINK";

    private static final String TAG = "FichaFragment";

    // TODO: Rename and change types of parameters
    private String titulo;
    private String link;

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
    // TODO: Rename and change types and number of parameters
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

        return inflater.inflate(R.layout.fragment_ficha, container, false);
    }

    //TODO: ¡PREPARAR CONTENIDO DEL FRAGMENT!
    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((TextView) view.findViewById(R.id.ficha_titulo)).setText(titulo);

        AsyncTask task = new AsyncTask() {

            String sinopsis;
            String director;
            String imagen_url;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Void doInBackground(Object[] objects) {

                try {
                    String contenido = getHTML(link);
                    // Aquí el parseo

                    // Saco la sinopsis
                    Pattern pattern = Pattern.compile("<dt>Sinopsis</dt>(.*?)<dd itemprop=\"description\">(.*?)</dd>");
                    Matcher matcher = pattern.matcher(contenido);

                    while (matcher.find()) {
                        sinopsis = matcher.group(2);
                        Log.d(TAG, matcher.group(2));
                    }

                    pattern = Pattern.compile("<dt id=\"full-director\">Director</dt>(.*?)<span itemprop=\"name\">(.*?)</span>");
                    matcher = pattern.matcher(contenido);

                    while (matcher.find()) {
                        director = matcher.group(2);
                        Log.d(TAG, matcher.group(2));
                    }

                    pattern = Pattern.compile("<a id=\"main-poster\" href=\"#\">(.*?)<img itemprop=\"image\" src=\"(.*?)\"");
                    matcher = pattern.matcher(contenido);

                    while (matcher.find()) {
                        imagen_url = matcher.group(2);
                        Log.d(TAG, matcher.group(2));
                    }

                }catch (Exception e){
                    Log.e(TAG, e.toString());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                ((TextView) view.findViewById(R.id.ficha_sinopsis)).setText(sinopsis);
            }

            @NonNull
            private String getHTML(String url) throws IOException {
                Log.d(TAG, "Obteniendo contenido HTML desde " + url);
                // Build and set timeout values for the request.
                URLConnection connection = (new URL(url)).openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                // Read and store the result line by line then return the entire string.
                InputStream in = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder html = new StringBuilder();
                for (String line; (line = reader.readLine()) != null; ) {
                    html.append(line);
                }
                in.close();

                return html.toString();
            }
        };

        task.execute();
    }

    // TODO: Rename method, update argument and hook method into UI event
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
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
