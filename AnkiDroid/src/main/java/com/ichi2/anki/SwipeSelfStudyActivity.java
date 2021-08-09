///****************************************************************************************
// * Copyright (c) 2014 Michael Goldbach <michael@m-goldbach.net>                         *
// *                                                                                      *
// * getAnkiActivity() program is free software; you can redistribute it and/or modify it under        *
// * the terms of the GNU General Public License as published by the Free Software        *
// * Foundation; either version 3 of the License, or (at your option) any later           *
// * version.                                                                             *
// *                                                                                      *
// * getAnkiActivity() program is distributed in the hope that it will be useful, but WITHOUT ANY      *
// * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
// * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
// *                                                                                      *
// * You should have received a copy of the GNU General Public License along with         *
// * getAnkiActivity() program.  If not, see <http://www.gnu.org/licenses/>.                           *
// ****************************************************************************************/
//package com.ichi2.anki;
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Spinner;
//import com.ichi2.anim.ActivityTransitionAnimation;
//import com.ichi2.async.CollectionTask;
//import com.ichi2.libanki.Collection;
//
//import com.ichi2.ui.SlidingTabLayout;
//
//import com.ichi2.utils.Permissions;
//import androidx.appcompat.app.ActionBar;
//import androidx.appcompat.widget.Toolbar;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
//import androidx.viewpager.widget.ViewPager;
//import timber.log.Timber;
//
//
//
//public class SelfStudyActivity extends AnkiActivity  {
//
//    public static final int TAB_STUDY_STATE = 0;
//    public static final int TAB_MARK_STATE = 1;
//    public static final int TAB_ANSWER_STATE = 2;
//    public static final int TAB_CUSTOM_STATE = 3;
//
//
//    private SectionsPagerAdapter mSectionsPagerAdapter;
//    private ViewPager mViewPager;
//    private Toolbar mToolbar;
//    private Spinner mActionBarSpinner;
//
//    private boolean wasLoadedFromExternalTextActionItem() {
//        Intent intent =  getIntent();
//        if (intent == null) {
//            return false;
//        }
//        //API 23: Replace with Intent.ACTION_PROCESS_TEXT
//        return "android.intent.action.PROCESS_TEXT".equalsIgnoreCase(intent.getAction());
//    }
//    private void displayDeckPickerForPermissionsDialog() {
//        //TODO: Combine getAnkiActivity() with class: IntentHandler after both are well-tested
//        Intent deckPicker = new Intent(this, DeckPicker.class);
//        deckPicker.setAction(Intent.ACTION_MAIN);
//        deckPicker.addCategory(Intent.CATEGORY_LAUNCHER);
//        deckPicker.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivityWithAnimation(deckPicker, ActivityTransitionAnimation.FADE);
//        AnkiActivity.finishActivityWithFade(this);
//        finishActivityWithFade(this);
//        setResult(RESULT_CANCELED);
//    }
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Timber.d("onCreate()");
//        if (wasLoadedFromExternalTextActionItem() && !Permissions.hasStorageAccessPermission(this)) {
//            Timber.w("'Card Browser' Action item pressed before storage permissions granted.");
//            UIUtils.showThemedToast(this, getString(R.string.intent_handler_failed_no_storage_permission), false);
//            displayDeckPickerForPermissionsDialog();
//            return;
//        }
//        setContentView(R.layout.activity_self_study);
////        mToolbar = findViewById(R.id.toolbar);
////        if (mToolbar != null) {
////            setSupportActionBar(mToolbar);
////        }
//        Toolbar toolbar = findViewById(R.id.toolbar);
//        if (toolbar != null) {
//            setSupportActionBar(toolbar);
//            // enable ActionBar app icon to behave as action to toggle nav drawer
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeButtonEnabled(true);
//
//            // Decide which action to take when the navigation button is tapped.
//            toolbar.setNavigationOnClickListener(v -> finishActivityWithFade(this,ActivityTransitionAnimation.RIGHT));
//        }
//        ActionBar mActionBar = getSupportActionBar();
//        if (mActionBar != null) {
//            mActionBar.setDisplayShowTitleEnabled(false);
//        }
////        mActionBarSpinner = (Spinner) findViewById(R.id.toolbar_spinner);
////        mActionBarSpinner.setAdapter(mDropDownAdapter);
////        mActionBarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
////            @Override
////            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
////                deckDropDownItemChanged(position);
////            }
////
////            @Override
////            public void onNothingSelected(AdapterView<?> parent) {
////                // do nothing
////            }
////        });
////        mActionBarSpinner.setVisibility(View.VISIBLE);
//        SlidingTabLayout slidingTabLayout;
//        // Add drop-down menu to select deck to action bar.
//
//        // Create the adapter that will return a fragment for each of the three
//        // primary sections of the activity.
//        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
//        mSectionsPagerAdapter.updateTabStyle(getIntent().getIntExtra("type",0));
//        // Set up the ViewPager with the sections adapter.
//        mViewPager = findViewById(R.id.pager);
//        mViewPager.setAdapter(mSectionsPagerAdapter);
//        slidingTabLayout = findViewById(R.id.sliding_tabs);
//        slidingTabLayout.setViewPager(mViewPager);
//        startLoadingCollection();
//    }
//
//    //activity(searchview)->viewpager+tablayout->cardslistfragment
//    // Finish initializing the activity after the collection has been correctly loaded
//    @Override
//    protected void onCollectionLoaded(Collection col) {
//        super.onCollectionLoaded(col);
//        Timber.d("onCollectionLoaded()");
//
//    }
//
//    // We spawn CollectionTasks that may create memory pressure, getAnkiActivity() transmits it so polling isCancelled sees the pressure
//    @Override
//    public void onTrimMemory(int pressureLevel) {
//        super.onTrimMemory(pressureLevel);
//        CollectionTask.cancelCurrentlyExecutingTask();
//    }
//}
//
//class SectionsPagerAdapter extends FragmentPagerAdapter {
//    private int tabType = SelfStudyActivity.TAB_STUDY_STATE;
//
//
//    public SectionsPagerAdapter(FragmentManager fm) {
//        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
//    }
//
//
//    @Override
//    public Fragment getItem(int position) {
//        CardsListFragment fragment = new CardsListFragment();
//        Bundle args = new Bundle();
//        fragment.setArguments(args);
//        return fragment;
//    }
//
//
//    @Override
//    public int getCount() {
//        switch (tabType) {
//            case SelfStudyActivity.TAB_STUDY_STATE:
//            case SelfStudyActivity.TAB_MARK_STATE:
//                return 5;
//            case SelfStudyActivity.TAB_ANSWER_STATE:
//                return 4;
//            case SelfStudyActivity.TAB_CUSTOM_STATE:
//                return 1;
//        }
//        return 0;
//    }
//
//
//    public void updateTabStyle(int style) {
//        tabType = style;
//        notifyDataSetChanged();
//    }
//
//
//    @Override
//    public CharSequence getPageTitle(int position) {
//        switch (tabType) {
//            case SelfStudyActivity.TAB_STUDY_STATE:
//                switch (position) {
//                    case 0:
//                        return "疑难";
//                    case 1:
//                        return "学习中";
//                    case 2:
//                        return "未学习";
//                    case 3:
//                        return "已掌握";
//                    case 4:
//                        return "暂停";
//                }
//                break;
//            case SelfStudyActivity.TAB_MARK_STATE:
//                switch (position) {
//                    case 0:
//                        return "红色";
//                    case 1:
//                        return "橙色";
//                    case 2:
//                        return "绿色";
//                    case 3:
//                        return "蓝色";
//                    case 4:
//                        return "Mark";
//                }
//                break;
//            case SelfStudyActivity.TAB_ANSWER_STATE:
//                switch (position) {
//                    case 0:
//                        return "忘记";
//                    case 1:
//                        return "困难";
//                    case 2:
//                        return "犹豫";
//                    case 3:
//                        return "简单";
//
//                }
//                break;
//            case SelfStudyActivity.TAB_CUSTOM_STATE:
//                return "";
//        }
//
//        return null;
//    }
//}
//
