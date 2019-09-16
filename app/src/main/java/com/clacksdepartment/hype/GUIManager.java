package com.clacksdepartment.hype;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.Transformation;
import android.widget.ImageView;
import android.widget.TextView;

class GUIManager {

    private MainActivity mMainActivity;
    private RecyclerViewAdapter mRecyclerViewAdapter;

    private LayoutAnimationController layoutAnimationController;

    GUIManager(MainActivity mainActivity, RecyclerViewAdapter RecyclerViewAdapter){
        this.mMainActivity = mainActivity;
        this.mRecyclerViewAdapter = RecyclerViewAdapter;

        layoutAnimationController = AnimationUtils.loadLayoutAnimation(mMainActivity,R.anim.fill_movie_list);
    }

    void selectHypeSection(){
        colorButton((ImageView) mMainActivity.findViewById(R.id.hype), true);
        colorButton((ImageView) mMainActivity.findViewById(R.id.theaters), false);
        colorButton((ImageView) mMainActivity.findViewById(R.id.releases), false);
        ((TextView) mMainActivity.findViewById(R.id.section)).setText(R.string.hype_section_text);

    }

    void selectInTheatersSection(){
        colorButton((ImageView) mMainActivity.findViewById(R.id.hype), false);
        colorButton((ImageView) mMainActivity.findViewById(R.id.theaters), true);
        colorButton((ImageView) mMainActivity.findViewById(R.id.releases), false);
        ((TextView) mMainActivity.findViewById(R.id.section)).setText(R.string.theaters_section_text);
    }

    void selectReleasesSection(){
        colorButton((ImageView) mMainActivity.findViewById(R.id.hype), false);
        colorButton((ImageView) mMainActivity.findViewById(R.id.theaters), false);
        colorButton((ImageView) mMainActivity.findViewById(R.id.releases), true);
        ((TextView) mMainActivity.findViewById(R.id.section)).setText(R.string.releases_section_text);
    }

    void showPager(Boolean show){
        if (show){
            // Update page number
            String txt = "" + (mRecyclerViewAdapter.getPage()+1);
            ((TextView) mMainActivity.findViewById(R.id.currentPage)).setText(txt);

            // The current page may fall in three cases: first, last and in between
            if (mRecyclerViewAdapter.getPage() == 0){
                animateButton(mMainActivity.findViewById(R.id.previousPageButton), false);
                animateButton(mMainActivity.findViewById(R.id.nextPageButton), true);
            }else if ((mRecyclerViewAdapter.getPage()+1) == mRecyclerViewAdapter.getLastPage()){
                animateButton(mMainActivity.findViewById(R.id.previousPageButton), true);
                animateButton(mMainActivity.findViewById(R.id.nextPageButton), false);
            }else{
                animateButton(mMainActivity.findViewById(R.id.previousPageButton), true);
                animateButton(mMainActivity.findViewById(R.id.nextPageButton), true);
            }

            // Show pager
            mMainActivity.findViewById(R.id.pager).setVisibility(View.VISIBLE);

        }else{
            // Hide pager
            mMainActivity.findViewById(R.id.pager).setVisibility(View.GONE);
            animateButton(mMainActivity.findViewById(R.id.previousPageButton), false);
            animateButton(mMainActivity.findViewById(R.id.nextPageButton), false);
        }
    }

    void showNoMoviesMessage(Boolean show){
        if (show){
            ((TextView) mMainActivity.findViewById(R.id.noMoviesMsg)).setText(
                    mRecyclerViewAdapter.getSection() == RecyclerViewAdapter.HYPE?
                            R.string.no_hype_msg : R.string.no_movies_msg);
            mMainActivity.findViewById(R.id.noMoviesMsg).setVisibility(View.VISIBLE);
        }else{
            mMainActivity.findViewById(R.id.noMoviesMsg).setVisibility(View.GONE);
        }
    }

    private void colorButton(ImageView imageView, Boolean selected){
        if (selected){
            imageView.setColorFilter(ContextCompat.getColor(mMainActivity,R.color.colorSelectedButton));
        }else{
            imageView.setColorFilter(ContextCompat.getColor(mMainActivity,R.color.colorNonSelectedButton));
        }
    }

    void smoothFocusOnFirstElement(){
        ((RecyclerView) mMainActivity.findViewById(R.id.movieList)).smoothScrollToPosition(0);
    }

    void hardFocusOnFirstElement(){
        ((RecyclerView) mMainActivity.findViewById(R.id.movieList)).scrollToPosition(0);
        mMainActivity.findViewById(R.id.movieList).requestFocus();
    }

    void animateList(){
        RecyclerView recyclerView = ((RecyclerView) mMainActivity.findViewById(R.id.movieList));
        recyclerView.setLayoutAnimation(layoutAnimationController);
        mRecyclerViewAdapter.notifyDataSetChanged();
        recyclerView.scheduleLayoutAnimation();
    }

    void update(){
        if (mRecyclerViewAdapter.getSection() != RecyclerViewAdapter.HYPE) {
            if (mRecyclerViewAdapter.getItemCount() == 1 ) {
                showPager(false);
                showNoMoviesMessage(true);
            } else if (mRecyclerViewAdapter.getLastPage() > 1) {
                showPager(true);
                showNoMoviesMessage(false);
            } else {
                showPager(false);
                showNoMoviesMessage(false);
            }
        }else{
            showPager(false);
            if (mRecyclerViewAdapter.getItemCount() == 1) {
                showNoMoviesMessage(true);
            } else {
                showNoMoviesMessage(false);
            }
        }
    }

    void showHeader(Boolean show){
        if (show){
            mMainActivity.findViewById(R.id.navigation).setVisibility(View.VISIBLE);
        } else
            mMainActivity.findViewById(R.id.navigation).setVisibility(View.GONE);
    }

    private void animateButton(final View v, Boolean show){
        final float initialAlpha = v.getAlpha();
        final float targetAlpha;
        if (show){
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
