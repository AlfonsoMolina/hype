package com.clacksdepartment.hype;

import android.os.AsyncTask;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MovieDetailFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MovieDetailFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MovieDetailFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TITLE = "Título";
    private static final String LINK = "Link";
    private static final String SYNOPSIS = "Sinopsis";

    private static final String TAG = "MovieDetailFragment";

    private String title;
    private String link;
    private String synopsis;
    private ViewGroup container;

    public MovieDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MovieDetailFragment.
     */
    static MovieDetailFragment newInstance(String param1, String param2, String param3) {
        MovieDetailFragment fragment = new MovieDetailFragment();
        Bundle args = new Bundle();
        args.putString(TITLE, param1);
        args.putString(LINK, param2);
        args.putString(SYNOPSIS, param3);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(TITLE);
            link = getArguments().getString(LINK);
            synopsis = getArguments().getString(SYNOPSIS);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        Log.i(TAG, "MovieDetailFragment open.");

        this.container = container;
        this.container.setVisibility(View.VISIBLE);

        return inflater.inflate(R.layout.fragment_expanded_movie_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // We already know the title and synopsis, so it can be set now
        ((TextView) view.findViewById(R.id.movie_detail_title)).setText(title);
        if (synopsis.length() > 0) {
            ((TextView) view.findViewById(R.id.movie_detail_synopsis)).setText(synopsis);
        }else{
            ((TextView) view.findViewById(R.id.movie_detail_synopsis)).setText("");
            view.findViewById(R.id.movie_detail_synopsis).setVisibility(View.GONE);
        }

        MovieDetail movieDetail = new MovieDetail(link, view);
        movieDetail.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        container.setVisibility(View.GONE);
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
    interface OnFragmentInteractionListener {

    }
}
