/****************************************************************************************
 * Copyright (c) 2014 Michael Goldbach <michael@m-goldbach.net>                         *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/
package com.ichi2.anki;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.ichi2.anki.stats.AnkiStatsTaskHandler;
import com.ichi2.anki.stats.ChartView;
import com.ichi2.anki.widgets.DeckDropDownAdapter;
import com.ichi2.anki.widgets.DeckInfoListAdapter;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.stats.Stats;
import com.ichi2.ui.OverView;
import com.ichi2.ui.SlidingTabLayout;
import com.ichi2.utils.JSONException;

import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;


public class SelfStudyActivity extends AnkiActivity implements DeckDropDownAdapter.SubtitleListener {

    public static final int TAB_STUDY_STATE = 0;
    public static final int TAB_MARK_STATE = 1;
    public static final int TAB_ANSWER_STATE = 2;
    public static final int TAB_CUSTOM_STATE = 3;


    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_study);
        mToolbar =  findViewById(R.id.toolbar);
        if (mToolbar != null) {
             setSupportActionBar(mToolbar);
        }
        SlidingTabLayout slidingTabLayout;
        // Add drop-down menu to select deck to action bar.

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter( getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager =  findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        slidingTabLayout =  findViewById(R.id.sliding_tabs);

        slidingTabLayout.setViewPager(mViewPager);
    }


    public static class SectionsPagerAdapter extends FragmentPagerAdapter {
        private int tabType = TAB_STUDY_STATE;
        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }


        @Override
        public Fragment getItem(int position) {
            CardsListFragment fragment = new CardsListFragment( );
            Bundle args = new Bundle();
            fragment.setArguments(args);
            return fragment;
        }


        @Override
        public int getCount() {
            switch (tabType) {
                case TAB_STUDY_STATE:
                case TAB_MARK_STATE:
                    return 5;
                case TAB_ANSWER_STATE:
                    return 4;
                case TAB_CUSTOM_STATE:
                    return 1;
            }
            return 0;
        }


        public void updateTabStyle(int style){
            tabType=style;
            notifyDataSetChanged();
        }


        @Override
        public CharSequence getPageTitle(int position) {
            switch (tabType) {
                case TAB_STUDY_STATE:
                    switch (position) {
                        case 0:
                            return "疑难";
                        case 1:
                            return "学习中";
                        case 2:
                            return "未学习";
                        case 3:
                            return "已掌握";
                        case 4:
                            return "暂停";
                    }
                    break;
                case TAB_MARK_STATE:
                    switch (position) {
                        case 0:
                            return "红色";
                        case 1:
                            return "橙色";
                        case 2:
                            return "绿色";
                        case 3:
                            return "蓝色";
                        case 4:
                            return "Mark";
                    }
                    break;
                case TAB_ANSWER_STATE:
                    switch (position) {
                        case 0:
                            return "忘记";
                        case 1:
                            return "困难";
                        case 2:
                            return "犹豫";
                        case 3:
                            return "简单";

                    }
                    break;
                case TAB_CUSTOM_STATE:
                    return "";
            }

            return null;
        }
    }


    @Override
    public void onResume() {
        Timber.d("onResume()");
//        selectNavigationItem(R.id.nav_stats);
        super.onResume();
    }


    /**
     * @return text to be used in the subtitle of the drop-down deck selector
     */
    public String getSubtitleText() {
        return getResources().getString(R.string.statistics);
    }


    public static class CardsListFragment extends AnkiFragment {
        public CardsListFragment() {
            super();
        }


        private RecyclerView mRecyclerView;
        private LinearLayoutManager mRecyclerViewLayoutManager;
        private CardsAdapter mDeckListAdapter;


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            setHasOptionsMenu(true);
            View rootView = inflater.inflate(R.layout.fragment_self_study_child, container, false);

            return rootView;
        }


    }



    public class CardsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return null;
        }


        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        }


        @Override
        public int getItemCount() {
            return 0;
        }
    }

}
