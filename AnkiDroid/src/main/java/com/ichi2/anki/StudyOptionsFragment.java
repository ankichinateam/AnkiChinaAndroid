/****************************************************************************************
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
 * this program. If not, see <http://www.gnu.org/licenses/>.                            *
 ****************************************************************************************/

package com.ichi2.anki;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.text.Editable;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.AbstractFlashcardViewer.NextCardHandler;
import com.ichi2.anki.dialogs.CustomStudyDialog;
import com.ichi2.anki.dialogs.DeckPickerContextMenu;
import com.ichi2.anki.widgets.DeckInfoListAdapter;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.DeckConfig;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.libanki.stats.Stats;
import com.ichi2.themes.StyledProgressDialog;

import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java8.util.Maps;
import timber.log.Timber;

import static com.ichi2.anki.DeckPicker.REQUEST_BROWSE_CARDS;
import static com.ichi2.anki.DeckPicker.SHOW_STUDYOPTIONS;
import static com.ichi2.anki.DeckPicker.fadeIn;
import static com.ichi2.anki.DeckPicker.fadeOut;
import static com.ichi2.anki.StudySettingActivity.KEY_LRN_AND_REV_CARD_MAX;
import static com.ichi2.anki.StudySettingActivity.KEY_MIND_MODE;
import static com.ichi2.anki.StudySettingActivity.KEY_STOPPED;
import static com.ichi2.anki.StudySettingActivity.STUDY_SETTING;
import static com.ichi2.async.CollectionTask.TASK_TYPE.*;
import static com.ichi2.libanki.Consts.KEY_SHOW_TTS_ICON;
import static com.ichi2.libanki.stats.Stats.ALL_DECKS_ID;
import static com.ichi2.libanki.stats.Stats.AxisType.TYPE_LIFE;
import static com.ichi2.preferences.StepsPreference.convertToJSON;

import com.ichi2.async.TaskData;
import com.ichi2.ui.CustomStyleDialog;
import com.ichi2.utils.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class StudyOptionsFragment extends Fragment implements Toolbar.OnMenuItemClickListener, CustomStudyDialog.CustomStudyListener {


    protected Boolean mFragmented = false;
//    /**
//     * Result codes from other activities
//     */
//    public static final int RESULT_MEDIA_EJECTED = 202;
//    public static final int RESULT_DB_ERROR = 203;
//    public static final int RESULT_UPDATE_REST_SPACE = 204;


    /**
     * Available options performed by other activities (request codes for onActivityResult())
     */


    private static final int SWIPE_TO_SYNC_TRIGGER_DISTANCE = 400;

    // Short animation duration from system
    private int mShortAnimDuration;


    private View mStudyoptionsFrame;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mRecyclerViewLayoutManager;
    private DeckInfoListAdapter mDeckListAdapter;
    private TextView mNoSubDeckHint;
    private Snackbar.Callback mSnackbarShowHideCallback = new Snackbar.Callback();

    private long mCacheID = -1;
    private EditText mDialogEditText;


    // flag keeping track of when the app has been paused


    protected List<AbstractDeckTreeNode> mDueTree;


    /**
     * Keep track of which deck was last given focus in the deck list. If we find that getAnkiActivity() value
     * has changed between deck list refreshes, we need to recenter the deck list to the new current
     * deck.
     */
    private long mFocusedDeck;
    private boolean firstLoadDeck = true;


    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        if (getAnkiActivity() == null) {
            return;
        }
        if (mTabLayout != null) {
            mTabLayout.selectTab(mTabLayout.getTabAt(0));
        }
        if (mCacheID != -1) {
            getCol().getDecks().select(mCacheID);
        } else {
            mCacheID = getCol().getDecks().selected();
        }
        new Handler().postDelayed(() -> {
            firstLoadDeck = false;
            refreshInterface(true);

        }, firstLoadDeck ? 0 : 500);
//        updateDeckList();
        /** Complete task and enqueue fetching nonessential data for
         * startup. */
        CollectionTask.launchCollectionTask(LOAD_COLLECTION_COMPLETE);
        getAnkiActivity().supportInvalidateOptionsMenu();
    }


    public AnkiActivity getAnkiActivity() {
//        if (getActivity() == null) {
        return (AnkiActivity) getActivity();
//        }
//        return ((DeckPicker) getActivity());
    }

    // ----------------------------------------------------------------------------
    // LISTENERS
    // ----------------------------------------------------------------------------

    private final View.OnClickListener mDeckExpanderClickListener = view -> {
        Long did = (Long) view.getTag();
        if (getCol().getDecks().children(did).size() > 0) {
            getCol().getDecks().collpase(did);
            __renderPage();
            getAnkiActivity().dismissAllDialogFragments();
        }
    };

    private final View.OnClickListener mDeckClickListener = v -> onDeckClick(v, false);

    private final View.OnClickListener mCountsClickListener = v -> onDeckClick(v, true);


    private void onDeckClick(View v, boolean dontSkipStudyOptions) {
        long deckId = (long) v.getTag();
        Timber.i("DeckPicker:: Selected deck with id %d", deckId);
        boolean collectionIsOpen = false;
        try {
            collectionIsOpen = getAnkiActivity().colIsOpen();
            handleDeckSelection(deckId, dontSkipStudyOptions);
            if (mFragmented || !CompatHelper.isLollipop()) {
                // Calling notifyDataSetChanged() will update the color of the selected deck.
                // getAnkiActivity() interferes with the ripple effect, so we don't do it if lollipop and not tablet view
                mDeckListAdapter.notifyDataSetChangedAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Maybe later don't report if collectionIsOpen is false?
            String info = deckId + " colOpen:" + collectionIsOpen;
            AnkiDroidApp.sendExceptionReport(e, "deckPicker::onDeckClick", info);
            displayFailedToOpenDeck(deckId);
        }
    }


    private void displayFailedToOpenDeck(long deckId) {
        // #6208 - if the click is accepted before the sync completes, we get a failure.
        // We use the Deck ID as the deck likely doesn't exist any more.
        String message = getString(R.string.deck_picker_failed_deck_load, Long.toString(deckId));
        UIUtils.showThemedToast(getAnkiActivity(), message, false);
        Timber.w(message);
    }


    private final View.OnLongClickListener mDeckLongClickListener = v -> {
        long deckId = (long) v.getTag();
        Timber.i("DeckPicker:: Long tapped on deck with id %d", deckId);
        getAnkiActivity().mContextMenuDid = deckId;
        showDialogFragment(DeckPickerContextMenu.newInstance(deckId));
        return true;
    };


    public void showDialogFragment(DialogFragment newFragment) {
        AnkiActivity.showDialogFragment(getAnkiActivity(), newFragment);
    }


    // ----------------------------------------------------------------------------
    // ANDROID ACTIVITY METHODS
    // ----------------------------------------------------------------------------

    Toolbar mToolbar;
    TextView mTextNewCardToday, mTextReviewCardToday, mCostTime, mCostTimeUnit;
    private DeckConfig mOptions;

    private TabLayout mTabLayout;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        mStudyOptionsView = inflater.inflate(R.layout.studyoptions_fragment, container, false);
        initOption();
        //we need to restore here, as we need it before super.onCreate() is called.
        // Open Collection on UI thread while splash screen is showing
        mToolbar = mStudyOptionsView.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.inflateMenu(R.menu.study_options_fragment);
            mToolbar.setNavigationOnClickListener(v -> onNavigationPressed());
//            TextView title = mToolbar.findViewById(R.id.toolbar_title);
//            title.setOnClickListener(v -> getAnkiActivity().startActivityForResultWithAnimation(new Intent(getAnkiActivity(), CardBrowser.class), REQUEST_BROWSE_CARDS, ActivityTransitionAnimation.LEFT));
//            title.setVisibility(View.VISIBLE);
//            title.setText("卡牌浏览器");
            mTabLayout = mToolbar.findViewById(R.id.tab_layout);
            TabLayout.Tab tab = mTabLayout.newTab();
            View view = getLayoutInflater().inflate(R.layout.item_deckinfo_tab, null);
            ((TextView) view.findViewById(R.id.name)).setText("学习");
            tab.setCustomView(view);
            mTabLayout.addTab(tab);
            TabLayout.Tab tab2 = mTabLayout.newTab();
            View view2 = getLayoutInflater().inflate(R.layout.item_deckinfo_tab, null);
            ((TextView) view2.findViewById(R.id.name)).setText("笔记");
            tab2.setCustomView(view2);
            mTabLayout.addTab(tab2);
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab == tab2) {
                        getAnkiActivity().openCardBrowser();
                    }
                }


                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }


                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
            configureToolbar();
        }
//        refreshInterface(true);
//        displaySyncBadge();
        // check, if tablet layout
        mStudyoptionsFrame = mStudyOptionsView.findViewById(R.id.studyoptions_fragment);
        // set protected variable from NavigationDrawerActivity
//        mFragmented = mStudyoptionsFrame != null && mStudyoptionsFrame.getVisibility() == View.VISIBLE;
        mFragmented = false;
        if (mFragmented) {
            loadStudyOptionsFragment(false);
        }


        mRecyclerView = mStudyOptionsView.findViewById(R.id.files);

//        mNoDecksPlaceholder.setVisibility(View.GONE);

        // specify a LinearLayoutManager and set up item dividers for the RecyclerView
        mRecyclerViewLayoutManager = new LinearLayoutManager(getAnkiActivity());
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);
        TypedArray ta = getContext().obtainStyledAttributes(new int[] {R.attr.deckDivider});
        Drawable divider = ta.getDrawable(0);
        ta.recycle();
//        DividerItemDecoration dividerDecorator = new DividerItemDecoration(getAnkiActivity(), mRecyclerViewLayoutManager.getOrientation());
//        dividerDecorator.setDrawable(divider);
//
//        mRecyclerView.addItemDecoration(dividerDecorator);
        // create and set an adapter for the RecyclerView
        mDeckListAdapter = new DeckInfoListAdapter(getLayoutInflater(), getAnkiActivity());
        mDeckListAdapter.setDeckClickListener(mDeckClickListener);
        mDeckListAdapter.setCountsClickListener(mCountsClickListener);
        mDeckListAdapter.setDeckExpanderClickListener(mDeckExpanderClickListener);
        mDeckListAdapter.setDeckLongClickListener(mDeckLongClickListener);
        mRecyclerView.setAdapter(mDeckListAdapter);
        mNoSubDeckHint = mStudyOptionsView.findViewById(R.id.text_no_child);

        mShortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        return mStudyOptionsView;
    }


    private void initOption() {
        Collection mCol = getCol();
        Deck deck = getCol().getDecks().current();
        mOptions = null;
        if (deck.optInt("dyn", 0) != 1) {
            long id = deck.getLong("id");
            mOptions = mCol.getDecks().confForDid(id);
            if (deck.getLong("conf") == 1 && id != 1) {
                long confID = mCol.getDecks().confId(deck.getString("name"), mOptions.toString());
                deck.put("conf", confID);
                mCol.getDecks().save(deck);
                mOptions = mCol.getDecks().confForDid(id);

                TreeMap<String, Long> children = getCol().getDecks().children(id);
                Set<String> childKeys = children.keySet();
                for (String str : childKeys) {
                    Deck child = getCol().getDecks().get(children.get(str));
                    child.put("conf", confID);
                    getCol().getDecks().save(child);
                }
                mCol.save();
            }
        }
    }


    public Collection getCol() {
        return getAnkiActivity().getCol();
    }


    private void onNavigationPressed() {
        getAnkiActivity().onBackPressed();
    }


    // ----------------------------------------------------------------------------
    // CUSTOM METHODS
    // ----------------------------------------------------------------------------


    /**
     * Load a new studyOptionsFragment. If withDeckOptions is true, the deck options activity will
     * be loaded on top of it. Use getAnkiActivity() flag when creating a new filtered deck to allow the user to
     * modify the filter settings before being shown the fragment. The fragment itself will handle
     * rebuilding the deck if the settings change.
     */
    public void loadStudyOptionsFragment(boolean withDeckOptions) {
        StudyOptionsFragment details = StudyOptionsFragment.newInstance(withDeckOptions);
        FragmentTransaction ft = getAnkiActivity().getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.studyoptions_fragment, details);
        ft.commit();
    }


    public StudyOptionsFragment getFragment() {
        Fragment frag = getAnkiActivity().getSupportFragmentManager().findFragmentById(R.id.studyoptions_fragment);
        if ((frag instanceof StudyOptionsFragment)) {
            return (StudyOptionsFragment) frag;
        }
        return null;
    }


    protected void openStudyOptions(boolean withDeckOptions) {
        if (mFragmented) {
            // The fragment will show the study options screen instead of launching a new activity.
            loadStudyOptionsFragment(withDeckOptions);
        } else {
            startStudyOption(withDeckOptions);
        }
    }


    protected void startStudyOption(boolean withDeckOptions) {
        Intent intent = new Intent();
        intent.putExtra("withDeckOptions", withDeckOptions);
        intent.setClass(getAnkiActivity(), StudyOptionsActivity.class);
        getAnkiActivity().startActivityForResultWithAnimation(intent, SHOW_STUDYOPTIONS, ActivityTransitionAnimation.LEFT);
    }


    public void notifyDataSetChanged() {
        mDeckListAdapter.notifyDataSetChanged();
        updateDeckList();
        if (mFragmented) {
            loadStudyOptionsFragment(false);
        }
    }


    private void openReviewerOrStudyOptions(boolean dontSkipStudyOptions) {
        if (mFragmented || dontSkipStudyOptions) {
            // Go to StudyOptions screen when tablet or deck counts area was clicked
            openStudyOptions(false);
        } else {
            // Otherwise jump straight to the reviewer
            openReviewer();
        }
    }


    private void handleDeckSelection(long did, boolean dontSkipStudyOptions) {
        // Clear the undo history when selecting a new deck
        if (getCol().getDecks().selected() != did) {
            getCol().clearUndo();
        }
        // Select the deck
        getCol().getDecks().select(did);
        // Also forget the last deck used by the Browser
        CardBrowser.clearLastDeckId();
        // Reset the schedule so that we get the counts for the currently selected deck
        mFocusedDeck = did;
        // Get some info about the deck to handle special cases
        int pos = mDeckListAdapter.findDeckPosition(did);
        AbstractDeckTreeNode deckDueTreeNode = mDeckListAdapter.getDeckList().get(pos);
//        if (!deckDueTreeNode.shouldDisplayCounts() || deckDueTreeNode.knownToHaveRep()) {
        // If we don't yet have numbers, we trust the user that they knows what they opens, tries to open it.
        // If there is nothing to review, it'll come back to deck picker.
        openReviewerOrStudyOptions(dontSkipStudyOptions);
//            return;
//        }
        // There are numbers
        // Figure out what action to take
//        if (getCol().getSched().hasCardsTodayAfterStudyAheadLimit()) {
//            // If there are cards due that can't be studied yet (due to the learn ahead limit) then go to study options
//            openStudyOptions(false);
//        } else if (getCol().getSched().newDue() || getCol().getSched().revDue()) {
//            // If there are no cards to review because of the daily study limit then give "Study more" option
//            UIUtils.showSnackbar(getAnkiActivity(), R.string.studyoptions_limit_reached, false, R.string.study_more, v -> {
//                CustomStudyDialog d = CustomStudyDialog.newInstance(
//                        CustomStudyDialog.CONTEXT_MENU_LIMITS,
//                        getCol().getDecks().selected(), true, this);
//                showDialogFragment(d);
//            }, getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
//            // Check if we need to update the fragment or update the deck list. The same checks
//            // are required for all snackbars below.
//            if (mFragmented) {
//                // Tablets must always show the study options that corresponds to the current deck,
//                // regardless of whether the deck is currently reviewable or not.
//                openStudyOptions(false);
//            } else {
//                // On phones, we update the deck list to ensure the currently selected deck is
//                // highlighted correctly.
//                updateDeckList();
//            }
//        } else if (getCol().getDecks().isDyn(did)) {
//            // Go to the study options screen if filtered deck with no cards to study
//            openStudyOptions(false);
//        } else if (!deckDueTreeNode.hasChildren() && getCol().cardCount(new Long[] {did}) == 0) {
//            // If the deck is empty and has no children then show a message saying it's empty
//            final Uri helpUrl = Uri.parse(getResources().getString(R.string.link_manual_getting_started));
//            getAnkiActivity().mayOpenUrl(helpUrl);
//            UIUtils.showSnackbar(getAnkiActivity(), R.string.empty_deck, false, R.string.help,
//                    v -> openHelpUrl(helpUrl), getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
//            if (mFragmented) {
//                openStudyOptions(false);
//            } else {
//                updateDeckList();
//            }
//        } else {
//            // Otherwise say there are no cards scheduled to study, and give option to do custom study
//            UIUtils.showSnackbar(getAnkiActivity(), R.string.studyoptions_empty_schedule, false, R.string.custom_study, v -> {
//                CustomStudyDialog d = CustomStudyDialog.newInstance(
//                        CustomStudyDialog.CONTEXT_MENU_EMPTY_SCHEDULE,
//                        getCol().getDecks().selected(), true, this);
//                showDialogFragment(d);
//            }, getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
//            if (mFragmented) {
//                openStudyOptions(false);
//            } else {
//                updateDeckList();
//            }
//        }
    }


    @Override
    public void onCreateCustomStudySession() {
        Timber.i("成功创建临时库！");
        updateDeckList(false);

    }


    @Override
    public void onExtendStudyLimits() {
        updateDeckList(false);
    }


    private void openHelpUrl(Uri helpUrl) {
        getAnkiActivity().openUrl(helpUrl);
    }


    /**
     * Scroll the deck list so that it is centered on the current deck.
     *
     * @param did The deck ID of the deck to select.
     */
    private void scrollDecklistToDeck(long did) {
        int position = mDeckListAdapter.findDeckPosition(did);
        mRecyclerViewLayoutManager.scrollToPositionWithOffset(position, (mRecyclerView.getHeight() / 2));
    }


    private UpdateDeckListListener updateDeckListListener() {
        return new UpdateDeckListListener(this);
    }


    private static class UpdateDeckListListener extends TaskListenerWithContext<StudyOptionsFragment> {
        public UpdateDeckListListener(StudyOptionsFragment deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull StudyOptionsFragment deckPicker) {
            if (deckPicker.getAnkiActivity() == null) {
                return;
            }
            if (!deckPicker.getAnkiActivity().colIsOpen()) {
                deckPicker.getAnkiActivity().showProgressBar();
            }
            Timber.d("Refreshing deck list");
        }


        @Override
        public void actualOnPostExecute(@NonNull StudyOptionsFragment deckPicker, TaskData result) {
            Timber.i("Updating deck list UI");
            if (deckPicker.getAnkiActivity() == null) {
                return;
            }
            deckPicker.getAnkiActivity().hideProgressBar();
            // Make sure the fragment is visible
            if (deckPicker.mFragmented) {
                deckPicker.mStudyoptionsFrame.setVisibility(View.VISIBLE);
            }
//            if (result == null) {
//                Timber.e("null result loading deck counts");
//                deckPicker.getAnkiActivity().showCollectionErrorDialog();
//                return;
//            }
            deckPicker.mDueTree = (List<AbstractDeckTreeNode>) result.getObjArray()[0];

            deckPicker.__renderPage();
            // Update the mini statistics bar as well
//            AnkiStatsTaskHandler.createReviewSummaryStatistics(deckPicker.getCol(), deckPicker.mReviewSummaryTextView);
            Timber.d("Startup - Deck List UI Completed");
        }
    }


    /**
     * Launch an asynchronous task to rebuild the deck list and recalculate the deck counts. Use getAnkiActivity()
     * after any change to a deck (e.g., rename, importing, add/delete) that needs to be reflected
     * in the deck list.
     * <p>
     * getAnkiActivity() method also triggers an update for the widget to reflect the newly calculated counts.
     */
    public void updateDeckList() {
        updateDeckList(false);
    }


    public void updateDeckList(boolean quick) {
        processNames();
        TaskListener listener = updateDeckListListener();
        CollectionTask.TASK_TYPE taskType = quick ? LOAD_DECK_QUICK : LOAD_SPECIFIC_DECK_COUNTS;
        CollectionTask.launchCollectionTask(taskType, listener, new TaskData(mCacheID));
    }


    private boolean mHasSubDecks = false;


    private boolean isInitStruct() {
        StringBuilder initIds = new StringBuilder(AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(KEY_STRUCT_INIT, ""));
        if (initIds.length() > 0) {
            String[] ids = initIds.toString().split(",");
            if (ids.length > 0) {
                for (String id : ids) {
                    if (getCol().getDecks().current().getLong("id") == Long.parseLong(id)) {
                        //已经初始化过了，直接跳过
                        return true;
                    }
                }
            }

        }
//        //没初始化过，那现在也初始化了。
//        if (!initIds.toString().isEmpty()) {
//            initIds.append(",");
//        }
//        initIds.append(getCol().getDecks().current().getLong("id"));
//        AnkiDroidApp.getSharedPrefs(getAnkiActivity()).edit().putString(KEY_STRUCT_INIT, initIds.toString()).apply();
        return false;
    }


    private void processNames() {
        Deck deck = getCol().getDecks().current();
        long id = deck.optLong("id");
        TreeMap<String, Long> children = getCol().getDecks().children(id);
//        for (Deck parent : getCol().getDecks().parents(id)) {
//            parent.put("collapsed", false);//祖先节点全部打开
//            getCol().getDecks().save(parent);
//        }
        mHasSubDecks = children.size() > 0;
        mNoSubDeckHint.setText(mHasSubDecks ? "" : "暂无子记忆库");
        mInitCollapsedStatus = true;
    }


    private boolean mInitCollapsedStatus = false;


    private void __renderPage() {
        if (mDueTree == null) {
            // mDueTree may be set back to null when the activity restart.
            // We may need to recompute it.


            updateDeckList();
            return;
        }

        // Check if default deck is the only available and there are no cards
        boolean isEmpty = mDueTree.size() == 1 && mDueTree.get(0).getDid() == 1 && getCol().isEmpty();
        if (getAnkiActivity().animationDisabled()) {
            mRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        } else {
            float translation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                    getResources().getDisplayMetrics());

            boolean decksListShown = mRecyclerView.getVisibility() == View.VISIBLE;

            if (isEmpty) {
                if (decksListShown) {
                    fadeOut(mRecyclerView, mShortAnimDuration, translation);
                }

            }
        }

        if (isEmpty) {
            if (getAnkiActivity().getSupportActionBar() != null) {
                getAnkiActivity().getSupportActionBar().setSubtitle(null);
            }
            // We're done here
            return;
        }
        Timber.i("update adapter list :%s", mDueTree.size());
        mDeckListAdapter.buildDeckList(mDueTree, getCol());

        // Set the "x due in y minutes" subtitle
//        try {
//            Integer eta = mDeckListAdapter.getEta();
//            Integer due = mDeckListAdapter.getDue();
//            Resources res = getResources();
//            if (getCol().cardCount() != -1) {
//                String time = "-";
//                if (eta != -1 && eta != null) {
//                    time = Utils.timeQuantityTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
//                }
//                if (due != null && getAnkiActivity().getSupportActionBar() != null) {
//                    getAnkiActivity().getSupportActionBar().setSubtitle(res.getQuantityString(R.plurals.deckpicker_title, due, due, time));
//                }
//            }
//        } catch (RuntimeException e) {
//            Timber.e(e, "RuntimeException setting time remaining");
//        }

//        long current = getCol().getDecks().current().optLong("id");
//        if (mFocusedDeck != current) {
//            scrollDecklistToDeck(current);
//            mFocusedDeck = current;
//        }
    }


//    @Override
//    public void onAttachedToWindow() {
//
//        if (!mFragmented) {
//            Window window = getWindow();
//            window.setFormat(PixelFormat.RGBA_8888);
//        }
//    }


    /**
     * Available options performed by other activities
     */
    private static final int BROWSE_CARDS = 3;
    private static final int STATISTICS = 4;
    private static final int DECK_OPTIONS = 5;

    /**
     * Constants for selecting which content view to display
     */
    private static final int CONTENT_STUDY_OPTIONS = 0;
    private static final int CONTENT_CONGRATS = 1;
    private static final int CONTENT_EMPTY = 2;

    // Threshold at which the total number of new cards is truncated by libanki
    private static final int NEW_CARD_COUNT_TRUNCATE_THRESHOLD = 99999;

    /**
     * Preferences
     */
    private int mCurrentContentView = CONTENT_STUDY_OPTIONS;

    /**
     * Alerts to inform the user about different situations
     */
    private MaterialDialog mProgressDialog;

    /**
     * UI elements for "Study Options" view
     */
    @Nullable
    private View mStudyOptionsView;


    // Flag to indicate if the fragment should load the deck options immediately after it loads
    private boolean mLoadWithDeckOptions;


    private Thread mFullNewCountThread = null;

    private StudyOptionsListener mListener;

    /**
     * Callbacks for UI events
     */
    private final View.OnClickListener mSelfStudyListener = v -> {

        Intent intent = new Intent(getActivity(), SelfStudyActivity.class);
        intent.putExtra("type", (int) v.getTag());
        startActivity(intent);
    };

    private final View.OnClickListener mButtonClickListener = v -> {

        if (mCurrentContentView != CONTENT_CONGRATS) {
            openReviewer();
        } else {
//                showCustomStudyContextMenu();
            showCustomStudyDialog(getCol());
        }

    };
    private Dialog mCustomStudyDialog;


    private void showCustomStudyDialog(Collection col) {
        //1、使用Dialog、设置style
//        if (mCloudCustomDialog == null) {
        mCustomStudyDialog = new Dialog(getAnkiActivity());
        //2、设置布局
        View view = View.inflate(getAnkiActivity(), R.layout.custom_study_dialog, null);
        mCustomStudyDialog.setContentView(view);
        Window window = mCustomStudyDialog.getWindow();
//        mCustomStudyDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        //设置弹出位置
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        EditText addNewEditor = mCustomStudyDialog.findViewById(R.id.add_new_card);
        EditText addRevEditor = mCustomStudyDialog.findViewById(R.id.add_rev_card);
        int totalNew = col.getSched().totalNewForCurrentDeck();
        int totalRev = col.getSched().totalRevForCurrentDeck();
        ((TextView) mCustomStudyDialog.findViewById(R.id.content)).setText(String.format(getAnkiActivity().getString(R.string.custom_study_dialog_content), totalNew, totalRev));
        Button start = mCustomStudyDialog.findViewById(R.id.start);
        if (totalNew + totalRev == 0) {
            start.setEnabled(false);
            start.setText("今日已无待学习的卡牌");
        } else {
            start.setOnClickListener(view1 -> {
                int addNew = 0;
                try {
                    addNew = Integer.parseInt(addNewEditor.getText().toString());
                } catch (Exception ignored) {

                }
                int addRev = 0;
                try {
                    addRev = Integer.parseInt(addRevEditor.getText().toString());
                } catch (Exception ignored) {

                }
                AnkiDroidApp.getSharedPrefs(getActivity()).edit().putInt("extendNew", addNew).apply();
                Deck deck = col.getDecks().current();
                deck.put("extendNew", addNew);
                col.getDecks().save(deck);
                col.getSched().extendLimits(addNew, 0);

                AnkiDroidApp.getSharedPrefs(getActivity()).edit().putInt("extendRev", addRev).apply();
                deck.put("extendRev", addRev);
                col.getDecks().save(deck);
                col.getSched().extendLimits(0, addRev);
                refreshInterface();
                getAnkiActivity().startActivityForResultWithoutAnimation(new Intent(getAnkiActivity(), Reviewer.class), AnkiActivity.REQUEST_REVIEW);
                mCustomStudyDialog.dismiss();
            });
        }

        mCustomStudyDialog.setCanceledOnTouchOutside(true);
        mCustomStudyDialog.setCancelable(true);
        mCustomStudyDialog.show();
    }


    public interface StudyOptionsListener {
        void onRequireDeckListUpdate();
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (StudyOptionsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement StudyOptionsListener");
        }
        if (!mFragmented) {
            Window window = getAnkiActivity().getWindow();
            window.setFormat(PixelFormat.RGBA_8888);
        }
    }


    private void openFilteredDeckOptions() {
        openFilteredDeckOptions(false);
    }


    /**
     * Open the FilteredDeckOptions activity to allow the user to modify the parameters of the
     * filtered deck.
     *
     * @param defaultConfig If true, signals to the FilteredDeckOptions activity that the filtered
     *                      deck has no options associated with it yet and should use a default
     *                      set of values.
     */
    @SuppressWarnings("deprecation")
    private void openFilteredDeckOptions(boolean defaultConfig) {
        Intent i = new Intent(getActivity(), FilteredDeckOptions.class);
        i.putExtra("defaultConfig", defaultConfig);
        getActivity().startActivityForResult(i, DECK_OPTIONS);
        ActivityTransitionAnimation.slide(getActivity(), ActivityTransitionAnimation.FADE);
    }


    /**
     * Get a new instance of the fragment.
     *
     * @param withDeckOptions If true, the fragment will load a new activity on top of itself
     *                        which shows the current deck's options. Set to true when programmatically
     *                        opening a new filtered deck for the first time.
     */
    public static StudyOptionsFragment newInstance(boolean withDeckOptions) {
        StudyOptionsFragment f = new StudyOptionsFragment();
        Bundle args = new Bundle();
        args.putBoolean("withDeckOptions", withDeckOptions);
        f.setArguments(args);
        return f;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //If we're being restored, don't launch deck options again.
        if (savedInstanceState == null && getArguments() != null) {
            mLoadWithDeckOptions = getArguments().getBoolean("withDeckOptions");
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFullNewCountThread != null) {
            mFullNewCountThread.interrupt();
        }
        Timber.d("onDestroy()");
    }


    private void closeStudyOptions(int result) {
        Activity a = getActivity();
        if (!mFragmented && a != null) {
            a.setResult(result);
            a.finish();
            ActivityTransitionAnimation.slide(a, ActivityTransitionAnimation.RIGHT);
        } else if (a == null) {
            // getActivity() can return null if reference to fragment lingers after parent activity has been closed,
            // which is particularly relevant when using AsyncTasks.
            Timber.e("closeStudyOptions() failed due to getActivity() returning null");
        }
    }


    private Map<Integer, String> mMindModeMap = new HashMap<>();
    private String[] mMindModeValues;
    private String[] mMindModeLabels;
    private String[] mMindModeHints;
    private int mCurrentMindModeValue = -1;

    public static String KEY_CONFIG_INIT = "KEY_CONFIG_INIT";//首次学习配置
    public static String KEY_STRUCT_INIT = "KEY_STRUCT_INIT";//首次进入详情页查看结构


    private void openReviewer() {
        if (mShouldConfigBeforeStudy && mOptions != null) {
            SharedPreferences sharedPreferences = getAnkiActivity().getSharedPreferences(STUDY_SETTING, 0);
            StringBuilder initIds = new StringBuilder(sharedPreferences.getString(KEY_CONFIG_INIT, ""));
            if (initIds.length() > 0) {
                String[] ids = initIds.toString().split(",");
                if (ids.length > 0) {
                    for (String id : ids) {
                        if (getCol().getDecks().current().getLong("id") == Long.parseLong(id)) {
                            //已经初始化过了，直接跳过
                            openReviewerInternal();
                            return;
                        }
                    }
                }
                //没初始化过，那现在也初始化了。
//                initIds.append(",");
            }

            for (long id : getDeckIds(getCol().getDecks().current().getLong("id"), getCol())) {
                if (!initIds.toString().isEmpty()) {
                    initIds.append(",");
                }
                initIds.append(id);
            }
//            initIds += Stats.deckLimit(getCol().getDecks().current().getLong("id"), getCol()).replace("(","").replace(")","").trim();//要把子牌组也加入里面
            sharedPreferences.edit().putString(KEY_CONFIG_INIT, initIds.toString()).apply();

            String savedMindModeValue = sharedPreferences.getString(KEY_MIND_MODE, "");
            Map<String, Integer> map = null;
            try {
                Gson gson = new Gson();
                map = gson.fromJson(savedMindModeValue, new TypeToken<Map<String, Integer>>() {
                }.getType());
            } catch (Exception e) {
                e.printStackTrace();
            }
            String mDeckIdStr = String.valueOf(getCol().getDecks().current().getLong("id"));
            mCurrentMindModeValue = map != null && map.get(mDeckIdStr) != null ? map.get(mDeckIdStr) : 0;
            if (mMindModeValues == null) {
                mMindModeValues = getResources().getStringArray(R.array.mind_mode_values);
                mMindModeLabels = getResources().getStringArray(R.array.mind_mode_labels);
                mMindModeHints = getResources().getStringArray(R.array.mind_mode_hint);
                for (int i = 0; i < mMindModeValues.length; i++) {
                    mMindModeMap.put(Integer.valueOf(mMindModeValues[i]), mMindModeLabels[i]);
                }
            }
            CustomStyleDialog customDialog = new CustomStyleDialog.Builder(getAnkiActivity())
                    .setTitle("记忆模式")
                    .setCustomLayout(R.layout.dialog_common_custom_next)
                    .setSelectListModeCallback(new CustomStyleDialog.Builder.SelectListModeCallback() {
                        @Override
                        public String[] getItemContent() {
                            return mMindModeLabels;
                        }


                        @Override
                        public String[] getItemHint() {
                            return mMindModeHints;
                        }


                        @Override
                        public int getDefaultSelectedPosition() {
                            return mCurrentMindModeValue;
                        }


                        @Override
                        public void onItemSelect(int position) {
                            mCurrentMindModeValue = position;
                        }
                    })
                    .setPositiveButton("下一步", (dialog, which) -> {
                        String oldValue = sharedPreferences.getString(KEY_MIND_MODE, "");
                        Map<String, Integer> oldMap = null;
                        Gson gson = new Gson();
                        try {
                            oldMap = gson.fromJson(oldValue, new TypeToken<Map<String, Integer>>() {
                            }.getType());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (oldMap == null) {
                            oldMap = new HashMap<>();
                        }
                        for (long id : getDeckIds(getCol().getDecks().current().getLong("id"), getCol())) {
//                            Timber.i("看看都是什么id %s", id);
                            oldMap.put(String.valueOf(id), mCurrentMindModeValue);
                        }
                        String newValue = gson.toJson(oldMap);
                        sharedPreferences.edit().putString(KEY_MIND_MODE, newValue).apply();
                        CollectionTask.launchCollectionTask(CONF_RESET, new ConfChangeHandler(StudyOptionsFragment.this, mCurrentMindModeValue),
                                new TaskData(new Object[] {mOptions}));//先恢复默认，即长记模式
                        dialog.dismiss();
                    })
                    .setNegativeButton("跳过", (dialog, which) -> {
                        dialog.dismiss();
                        try {
                            getCol().getDecks().save(mOptions);
                        } catch (RuntimeException e) {
                            Timber.e(e, "DeckOptions - RuntimeException on saving conf");
                            AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
                        }
                        refreshOption();
                        openReviewerInternal();
                    })
                    .create();
            customDialog.show();

        } else {
            openReviewerInternal();
        }

    }


    public static List<Long> getDeckIds(Long deckId, Collection col) {
        ArrayList<Long> ids = new ArrayList<>();
        if (deckId == ALL_DECKS_ID) {
            // All decks
            for (Deck d : col.getDecks().all()) {
                ids.add(d.getLong("id"));
            }
        } else {
            // The given deck id and its children
            ids.add(deckId);
            ids.addAll(col.getDecks().children(deckId).values());
        }
        return ids;
    }


    private void refreshOption() {
        if (mLoadWithDeckOptions) {
            mLoadWithDeckOptions = false;
            Deck deck = getCol().getDecks().current();
            if (deck.getInt("dyn") != 0 && deck.has("empty")) {
                deck.remove("empty");
            }
            mProgressDialog = StyledProgressDialog.show(getActivity(), "",
                    getResources().getString(R.string.rebuild_filtered_deck), true);
            CollectionTask.launchCollectionTask(REBUILD_CRAM, getCollectionTaskListener(true));
        } else {
            CollectionTask.waitToFinish();
            refreshInterface(true);
        }
    }


    @SuppressWarnings("deprecation")
    private void openReviewerInternal() {
        mInitCollapsedStatus = false;
        Intent reviewer = new Intent(getActivity(), Reviewer.class);
        if (mFragmented) {
            getActivity().startActivityForResult(reviewer, AnkiActivity.REQUEST_REVIEW);
        } else {
            // Go to DeckPicker after studying when not tablet
            reviewer.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
            startActivity(reviewer);
//            getActivity().finish();
        }
        animateLeft();
    }


    private static class ConfChangeHandler extends TaskListenerWithContext<StudyOptionsFragment> {
        public ConfChangeHandler(StudyOptionsFragment deckPreferenceHack) {
            super(deckPreferenceHack);
        }


        int selectMindMode = -1;


        public ConfChangeHandler(StudyOptionsFragment deckPreferenceHack, int selectMindMode) {
            super(deckPreferenceHack);
            this.selectMindMode = selectMindMode;
        }


        @Override
        public void actualOnPreExecute(@NonNull StudyOptionsFragment deckPreferenceHack) {

        }


        @Override
        public void actualOnPostExecute(@NonNull StudyOptionsFragment deckPreferenceHack, TaskData result) {
            deckPreferenceHack.initOption();
//            if (selectMindMode <= 0) {
////                deckPreferenceHack.refreshInterface();
//
//            } else
            if (selectMindMode == 1) {
                deckPreferenceHack.mOptions.getJSONObject("new").put("delays", convertToJSON("1 5 10"));
                JSONArray newInts = new JSONArray();
                newInts.put(deckPreferenceHack.mOptions.getJSONObject("new").getJSONArray("ints").getInt(0));//
                newInts.put(3);
                deckPreferenceHack.mOptions.getJSONObject("new").put("ints", newInts);
                deckPreferenceHack.mOptions.getJSONObject("rev").put("ivlFct", 70 / 100.0f);
                deckPreferenceHack.mOptions.getJSONObject("rev").put("maxIvl", 10);
            } else if (selectMindMode == 2) {
                deckPreferenceHack.mOptions.getJSONObject("new").put("delays", convertToJSON("1 3 7 10 15 20"));
                JSONArray newInts = new JSONArray();
                newInts.put(deckPreferenceHack.mOptions.getJSONObject("new").getJSONArray("ints").getInt(0));//
                newInts.put(1);
                deckPreferenceHack.mOptions.getJSONObject("new").put("ints", newInts);
                deckPreferenceHack.mOptions.getJSONObject("rev").put("maxIvl", 1);
            }
            deckPreferenceHack.configMaxDayLearnAndRev();
        }
    }


    private void configMaxDayLearnAndRev() {
        CustomStyleDialog customDialog = new CustomStyleDialog.Builder(getAnkiActivity())
                .setTitle("设置每天学习量")
                .setCustomLayout(R.layout.dialog_common_custom_next)
                .setMultiEditorModeCallback(new CustomStyleDialog.Builder.MultiEditorModeCallback() {
                    @Override
                    public String[] getEditorText() {
//                        return new String[] {mOptions.getJSONObject("new").getString("perDay"), mOptions.getJSONObject("rev").getString("perDay"),};
                        return new String[] {"30", "200"};
                    }


                    @Override
                    public String[] getEditorHint() {
                        return new String[] {"每天新卡上限", "每天复习上限"};
                    }


                    @Override
                    public String[] getItemHint() {
                        return new String[] {"学习一段时间后，未来每天新卡+复习卡约180张，大约需30分钟", "建议设置为最大9999，有多少复习多少"};
                    }
                }).addSingleTextChangedListener(new CustomStyleDialog.Builder.MyTextWatcher() {

                    @Override
                    public void beforeTextChanged(Dialog dialog, CharSequence s, int start, int count, int after) {

                    }


                    @Override
                    public void onTextChanged(Dialog dialog, CharSequence s, int start, int before, int count) {
                        if (s.toString().isEmpty()) {
                            ((CustomStyleDialog) dialog).getSingleEditorModeHintView().setText("未来每天学习量=新卡数x6");
                        } else {
                            int num = 0;
                            try {
                                num = Integer.parseInt(s.toString()) * 6;
                            } catch (Exception ignored) {

                            }
                            int time = num * 10 / 60;
                            ((CustomStyleDialog) dialog).getSingleEditorModeHintView().setText(String.format("学习一段时间后，未来每天新卡+复习卡约%s张，大约需%s分钟", num, time));
                        }
                    }


                    @Override
                    public void afterTextChanged(Dialog dialog, Editable s) {

                    }
                })
                .setPositiveButton("进入学习", (dialog, which) -> {
                    String text1 = ((CustomStyleDialog) dialog).getMultiEditor().get(0).getText().toString();
                    String text2 = ((CustomStyleDialog) dialog).getMultiEditor().get(1).getText().toString();
                    int maxNewCard = 0;
                    int maxRevCard = 0;
                    try {
                        maxNewCard = Integer.parseInt(text1);
                    } catch (Exception ignored) {

                    }
                    try {
                        maxRevCard = Integer.parseInt(text2);
                    } catch (Exception ignored) {

                    }
                    if (maxNewCard >= 0 && maxNewCard <= 9999 && maxRevCard >= 0 && maxRevCard <= 9999) {
                        mOptions.getJSONObject("new").put("perDay", maxNewCard);
                        mOptions.getJSONObject("rev").put("perDay", maxRevCard);
                        SharedPreferences sharedPreferences = getAnkiActivity().getSharedPreferences(STUDY_SETTING, 0);
                        String oldValue = sharedPreferences.getString(KEY_LRN_AND_REV_CARD_MAX, "");
                        Map<String, String> oldMap = null;
                        Gson gson = new Gson();
                        try {
                            oldMap = gson.fromJson(oldValue, new TypeToken<Map<String, String>>() {
                            }.getType());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (oldMap == null) {
                            oldMap = new HashMap<>();
                        }
                        for (long id : getDeckIds(getCol().getDecks().current().getLong("id"), getCol())) {
                            oldMap.put(String.valueOf(id), maxNewCard + "," + maxRevCard);
                        }
                        String newValue = gson.toJson(oldMap);
                        sharedPreferences.edit().putString(KEY_LRN_AND_REV_CARD_MAX, newValue).apply();

                        Timber.i("edit new and rev max:" + maxNewCard + "," + maxRevCard);
                        try {
                            getCol().getDecks().save(mOptions);
                        } catch (RuntimeException e) {
                            Timber.e(e, "DeckOptions - RuntimeException on saving conf");
                            AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
                        }
                        refreshOption();
                        dialog.dismiss();
                        openReviewerInternal();
                    } else {
                        UIUtils.showThemedToast(getAnkiActivity(), "请填写0至9999之间的数值",
                                false);
                    }

                })
                .setNegativeButton("跳过", (dialog, which) -> {
                    dialog.dismiss();
                    try {
                        getCol().getDecks().save(mOptions);
                    } catch (RuntimeException e) {
                        Timber.e(e, "DeckOptions - RuntimeException on saving conf");
                        AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
                    }
                    refreshOption();
                    openReviewerInternal();
                })
                .create();
        customDialog.show();
    }


    private void animateLeft() {
        ActivityTransitionAnimation.slide(getActivity(), ActivityTransitionAnimation.LEFT);
    }


    private void initAllContentViews(@NonNull View studyOptionsView) {
        if (mFragmented) {
            studyOptionsView.findViewById(R.id.studyoptions_gradient).setVisibility(View.VISIBLE);
        }

    }


    /**
     * Show the context menu for the custom study options
     */
    private void showCustomStudyContextMenu() {
        CustomStudyDialog d = CustomStudyDialog.newInstance(CustomStudyDialog.CONTEXT_MENU_STANDARD,
                getCol().getDecks().selected(), (CustomStudyDialog.CustomStudyListener) getActivity());
        ((AnkiActivity) getActivity()).showDialogFragment(d);
    }


    void setFragmentContentView(View newView) {
        ViewGroup parent = (ViewGroup) this.getView();
        parent.removeAllViews();
        parent.addView(newView);
    }


    private final TaskListener undoListener = new TaskListener() {
        @Override
        public void onPreExecute() {

        }


        @Override
        public void onPostExecute(TaskData result) {
            openReviewer();
        }
    };


    @SuppressWarnings("deprecation")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_browser: {
                Timber.i("DeckPicker:: Old browser button pressed");
                getAnkiActivity().openOldCardBrowser();
                return true;
            }
            case R.id.action_undo:
                Timber.i("StudyOptionsFragment:: Undo button pressed");
                CollectionTask.launchCollectionTask(UNDO, undoListener);
                return true;
            case R.id.action_deck_options:
                Timber.i("StudyOptionsFragment:: Deck options button pressed");
                if (getCol().getDecks().isDyn(getCol().getDecks().selected())) {
                    openFilteredDeckOptions();
                } else {
                    Intent i = new Intent(getActivity(), DeckOptions.class);
                    getActivity().startActivityForResult(i, DECK_OPTIONS);
                    ActivityTransitionAnimation.slide(getActivity(), ActivityTransitionAnimation.FADE);
                }
                return true;
            case R.id.action_setting:
                Timber.i("StudyOptionsFragment:: Deck setting button pressed");
                Intent i = new Intent(getActivity(), StudySettingActivity.class);
                getActivity().startActivityForResult(i, DECK_OPTIONS);
                ActivityTransitionAnimation.slide(getActivity(), ActivityTransitionAnimation.FADE);
                return true;
            case R.id.action_custom_study:
                Timber.i("StudyOptionsFragment:: custom study button pressed");
                showCustomStudyContextMenu();
                return true;
            case R.id.action_unbury:
                Timber.i("StudyOptionsFragment:: unbury button pressed");
                getCol().getSched().unburyCardsForDeck();
                refreshInterfaceAndDecklist(true);
                item.setVisible(false);
                return true;
            case R.id.action_rebuild:
                Timber.i("StudyOptionsFragment:: rebuild cram deck button pressed");
                mProgressDialog = StyledProgressDialog.show(getActivity(), "",
                        getResources().getString(R.string.rebuild_filtered_deck), true);
                CollectionTask.launchCollectionTask(REBUILD_CRAM, getCollectionTaskListener(true));
                return true;
            case R.id.action_empty:
                Timber.i("StudyOptionsFragment:: empty cram deck button pressed");
                mProgressDialog = StyledProgressDialog.show(getActivity(), "",
                        getResources().getString(R.string.empty_filtered_deck), false);
                CollectionTask.launchCollectionTask(EMPTY_CRAM, getCollectionTaskListener(true));
                return true;
            case R.id.action_rename:
                ((AnkiActivity) getActivity()).renameDeckDialog(getCol().getDecks().selected());
                return true;
            case R.id.action_delete:
                getAnkiActivity().mContextMenuDid = getCol().getDecks().selected();
                ((AnkiActivity) getActivity()).confirmDeckDeletion(getCol().getDecks().selected());
                return true;
            case R.id.action_export:
                ((AnkiActivity) getActivity()).exportDeck(getCol().getDecks().selected());
                return true;
            case R.id.action_add_card:
                addNote();
                return true;
            case R.id.create_deck:
                getAnkiActivity().mContextMenuDid = getCol().getDecks().selected();
                ((AnkiActivity) getActivity()).createSubdeckDialog();
                return true;
            case R.id.action_suspend://停止/恢复
                mDeckIsStopped = deckIsStopped();
                if (mDeckIsStopped) {
                    customDialog = new CustomStyleDialog.Builder(getAnkiActivity())
                            .setTitle("恢复学习")
                            .setMessage("恢复学习后将每天新卡数和复习数值调整成默认值，请确认是否开始学习")
                            .setPositiveButton("确定", (dialog, which) -> {
                                resumeDeckStudy();
                                dialog.dismiss();
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .create();

                } else {
                    customDialog = new CustomStyleDialog.Builder(getAnkiActivity())
                            .setTitle("停止学习")
                            .setMessage("暂停学习将会自动把每天新卡数和复习数调整为零，适用于有事暂停一两周不学习，若该记忆库学习的时长不超过2个月，又暂停了30天未学习，建议重设进度，重新开始学习")
                            .setPositiveButton("确定", (dialog, which) -> {
                                stopDeckStudy();
                                dialog.dismiss();
                            })
                            .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                            .create();
                }
                customDialog.show();
                return true;
            case R.id.action_reset_card_progress://重设学习进度
                customDialog = new CustomStyleDialog.Builder(getAnkiActivity())
                        .setTitle("重设学习进度")
                        .setMessage("重设学习进度后，该记忆库的所有卡片学习记录都会被清除，请谨慎操作")
                        .setPositiveButton("确定", (dialog, which) -> {
                            CollectionTask.launchCollectionTask(RESET_DECK, new ResetCardHandler(StudyOptionsFragment.this),
                                    new TaskData(new Object[] {"deck:\"" + getCol().getDecks().current().getString("name") + "\" "}));
                            dialog.dismiss();
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                customDialog.show();
                return true;
            default:
                return false;
        }
    }


    private static class ResetCardHandler extends TaskListenerWithContext<StudyOptionsFragment> {
        public ResetCardHandler(StudyOptionsFragment browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull StudyOptionsFragment browser) {
        }


        @Override
        public void actualOnPostExecute(@NonNull StudyOptionsFragment browser, TaskData result) {
            int cardCount = result.getObjArray().length;
            browser.refreshInterface(true);

            UIUtils.showThemedToast(browser.getAnkiActivity(), String.format("共有%d张卡片被重设", cardCount), true);
        }
    }



    CustomStyleDialog customDialog;

    private boolean mDeckIsStopped = false;


    private boolean deckIsStopped() {
        return mOptions.getJSONObject("new").getInt("perDay") == 0 && mOptions.getJSONObject("rev").getInt("perDay") == 0;
    }


    private void stopDeckStudy() {
        int maxNewCard = mOptions.getJSONObject("new").getInt("perDay");
        int maxRevCard = mOptions.getJSONObject("rev").getInt("perDay");
        SharedPreferences sharedPreferences = getAnkiActivity().getSharedPreferences(STUDY_SETTING, 0);
        String oldValue = sharedPreferences.getString(KEY_LRN_AND_REV_CARD_MAX, "");
        Map<String, String> oldMap = null;
        Gson gson = new Gson();
        try {
            oldMap = gson.fromJson(oldValue, new TypeToken<Map<String, String>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (oldMap == null) {
            oldMap = new HashMap<>();
        }
        oldMap.put(String.valueOf(getCol().getDecks().current().getLong("id")), maxNewCard + "," + maxRevCard);
        String newValue = gson.toJson(oldMap);
        sharedPreferences.edit().putString(KEY_LRN_AND_REV_CARD_MAX, newValue).apply();
        mOptions.getJSONObject("new").put("perDay", 0);
        mOptions.getJSONObject("rev").put("perDay", 0);
        try {
            getCol().getDecks().save(mOptions);
        } catch (RuntimeException e) {
            Timber.e(e, "DeckOptions - RuntimeException on saving conf");
            AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
        }
        refreshOption();
    }


    private void resumeDeckStudy() {
        SharedPreferences sharedPreferences = getAnkiActivity().getSharedPreferences(STUDY_SETTING, 0);
        String oldValue = sharedPreferences.getString(KEY_LRN_AND_REV_CARD_MAX, "");
        Map<String, String> oldMap = null;
        Gson gson = new Gson();
        try {
            oldMap = gson.fromJson(oldValue, new TypeToken<Map<String, String>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }

//        mOptions =  getCol().getDecks().confForDid(getCol().getDecks().current().getLong("id"));
        if (oldMap == null) {
            mOptions.getJSONObject("new").put("perDay", 30);
            mOptions.getJSONObject("rev").put("perDay", 200);
        }else {
            String data=oldMap.get(String.valueOf(getCol().getDecks().current().getLong("id")));
            String newCardMax=data.split(",")[0];
            String revCardMax=data.split(",")[1];
            mOptions.getJSONObject("new").put("perDay", Integer.valueOf(newCardMax));
            mOptions.getJSONObject("rev").put("perDay", Integer.valueOf(revCardMax));
        }

        try {
            getCol().getDecks().save(mOptions);
        } catch (RuntimeException e) {
            Timber.e(e, "DeckOptions - RuntimeException on saving conf");
            AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
        }
        refreshOption();
    }


    private void createDeck() {
        EditText mDialogEditText = new EditText(getAnkiActivity());
        mDialogEditText.setSingleLine(true);
        // mDialogEditText.setFilters(new InputFilter[] { mDeckNameFilter });
        new MaterialDialog.Builder(getAnkiActivity())
                .title(R.string.new_deck)
                .positiveText(R.string.dialog_ok)
                .customView(mDialogEditText, true)
                .onPositive((dialog, which) -> {
                    String deckName = mDialogEditText.getText().toString();
                    if (Decks.isValidDeckName(deckName)) {
                        getAnkiActivity().createNewDeck(deckName);
                    } else {
                        Timber.i("configureFloatingActionsMenu::addDeckButton::onPositiveListener - Not creating invalid deck name '%s'", deckName);
                        UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.invalid_deck_name), false);
                    }
                })
                .negativeText(R.string.dialog_cancel)
                .onNegative((dialog, which) -> {
                    String deckName = mDialogEditText.getText().toString();
                    if (Decks.isValidDeckName(deckName)) {
                        getAnkiActivity().createNewDeck(deckName);
                    } else {
                        Timber.i("configureFloatingActionsMenu::addDeckButton::onPositiveListener - Not creating invalid deck name '%s'", deckName);
                        UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.invalid_deck_name), false);
                    }
                })
                .show();

    }


    public void configureToolbar() {
        configureToolbarInternal(true);
    }


    // This will allow a maximum of one recur in order to workaround database closes
    // caused by sync on startup where this might be running then have the collection close
    private void configureToolbarInternal(boolean recur) {
        try {
            mToolbar.setOnMenuItemClickListener(this);
            Menu menu = mToolbar.getMenu();
            // Switch on or off rebuild/empty/custom study depending on whether or not filtered deck
            if (getCol().getDecks().isDyn(getCol().getDecks().selected())) {
                menu.findItem(R.id.action_add_card).setVisible(false);
                menu.findItem(R.id.action_rebuild).setVisible(true);
                menu.findItem(R.id.action_empty).setVisible(true);
                menu.findItem(R.id.action_custom_study).setVisible(false);

                menu.findItem(R.id.action_rename).setVisible(false);
                menu.findItem(R.id.create_deck).setVisible(false);
                menu.findItem(R.id.action_suspend).setVisible(false);
                menu.findItem(R.id.action_reset_card_progress).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
                menu.findItem(R.id.action_export).setVisible(false);
                menu.findItem(R.id.action_setting).setVisible(false);
            } else {
                menu.findItem(R.id.action_rebuild).setVisible(false);
                menu.findItem(R.id.action_empty).setVisible(false);
                menu.findItem(R.id.action_custom_study).setVisible(true);

                menu.findItem(R.id.action_rename).setVisible(true);

                menu.findItem(R.id.create_deck).setVisible(true);
                menu.findItem(R.id.action_suspend).setVisible(true);
                menu.findItem(R.id.action_suspend).setTitle(deckIsStopped() ? "恢复学习" : "停止学习");
                menu.findItem(R.id.action_reset_card_progress).setVisible(true);

                menu.findItem(R.id.action_delete).setVisible(true);
                menu.findItem(R.id.action_export).setVisible(true);
                menu.findItem(R.id.action_setting).setVisible(true);
                menu.findItem(R.id.action_deck_options).setVisible(true);

            }
            // Don't show custom study icon if congrats shown
//            if (mCurrentContentView == CONTENT_CONGRATS) {
//                menu.findItem(R.id.action_custom_study).setVisible(false);
//            }


            // Switch on or off unbury depending on if there are cards to unbury
            menu.findItem(R.id.action_unbury).setVisible(getCol().getSched().haveBuried());
            // Switch on or off undo depending on whether undo is available
            menu.findItem(R.id.action_undo).setVisible(false);
        } catch (IllegalStateException e) {
            if (!CollectionHelper.getInstance().colIsOpen()) {
                if (recur) {
                    Timber.i(e, "Database closed while working. Probably auto-sync. Will re-try after sleep.");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                        Timber.i(ex, "Thread interrupted while waiting to retry. Likely unimportant.");
                        Thread.currentThread().interrupt();
                    }
                    configureToolbarInternal(false);
                } else {
                    Timber.w(e, "Database closed while working. No re-tries left.");
                }
            }
        }
    }


    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Timber.d("onActivityResult (requestCode = %d, resultCode = %d)", requestCode, resultCode);

        // rebuild action bar
        configureToolbar();

        // boot back to deck picker if there was an error
        if (resultCode == DeckPicker.RESULT_DB_ERROR || resultCode == DeckPicker.RESULT_MEDIA_EJECTED) {
            closeStudyOptions(resultCode);
            return;
        }

        // perform some special actions depending on which activity we're returning from
        if (requestCode == STATISTICS || requestCode == BROWSE_CARDS) {
            // select original deck if the statistics or card browser were opened,
            // which can change the selected deck
            if (intent.hasExtra("originalDeck")) {
                getCol().getDecks().select(intent.getLongExtra("originalDeck", 0L));
            }
        }
        if (requestCode == DECK_OPTIONS) {
            refreshOption();
        } else if (requestCode == AnkiActivity.REQUEST_REVIEW) {
            if (resultCode == Reviewer.RESULT_NO_MORE_CARDS) {
                // If no more cards getting returned while counts > 0 (due to learn ahead limit) then show a snackbar
                if (getCol().getSched().count() > 0 && mStudyOptionsView != null) {
                    View rootLayout = mStudyOptionsView.findViewById(R.id.studyoptions_main);
                    UIUtils.showSnackbar(getActivity(), R.string.studyoptions_no_cards_due, false, 0, null, rootLayout);
                }
            }
        } else if (requestCode == STATISTICS && mCurrentContentView == CONTENT_CONGRATS) {
            mCurrentContentView = CONTENT_STUDY_OPTIONS;
            setFragmentContentView(mStudyOptionsView);
        }
    }


    private void dismissProgressDialog() {
        if (mStudyOptionsView != null && mStudyOptionsView.findViewById(R.id.progress_bar) != null) {
            mStudyOptionsView.findViewById(R.id.progress_bar).setVisibility(View.GONE);
        }
        // for rebuilding cram decks
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            try {
                mProgressDialog.dismiss();
            } catch (Exception e) {
                Timber.e("onPostExecute - Dialog dismiss Exception = %s", e.getMessage());
            }
        }
    }


    protected void refreshInterfaceAndDecklist(boolean resetSched) {
        refreshInterface(resetSched, true);
    }


    protected void refreshInterfaceWithCacheSelected() {
        getCol().getDecks().select(mCacheID);
        refreshInterface(false, false);
    }


    protected void refreshInterface() {
        refreshInterface(false, false);
    }


    protected void askForRefreshInterface() {
        new MaterialDialog.Builder(getAnkiActivity())
                .title("已创建临时记忆库，是否跳转过去？")
                .content("已根据你的条件筛选出了临时的记忆库，请确认是否现在跳转过去学习？")
//                .customView(mDialogEditText, true)
                .positiveText("是")
                .negativeText("否")
                .onNegative((dialog, which) -> {
                    getCol().getDecks().select(mCacheID);
                    initOption();
//                    refreshInterface();
                })
                .onPositive((dialog, which) -> {
                    mCacheID = getCol().getDecks().selected();
                    initOption();
                    refreshInterface();
                })
                .show();
    }


    protected void refreshInterface(boolean resetSched) {
        refreshInterface(resetSched, false);
    }


    /**
     * Rebuild the fragment's interface to reflect the status of the currently selected deck.
     *
     * @param resetSched    Indicates whether to rebuild the queues as well. Set to true for any
     *                      task that modifies queues (e.g., unbury or empty filtered deck).
     * @param resetDecklist Indicates whether to call back to the parent activity in order to
     *                      also refresh the deck list.
     */
    protected void refreshInterface(boolean resetSched, boolean resetDecklist) {
        Timber.d("Refreshing StudyOptionsFragment");
        // Load the deck counts for the deck from Collection asynchronously
        if (resetDecklist) {
            mInitCollapsedStatus = false;
        }
        CollectionTask.launchCollectionTask(UPDATE_VALUES_FROM_DECK, getCollectionTaskListener(resetDecklist),
                new TaskData(new Object[] {resetSched}));
    }


    private int mNewCardsNum;
    private int mRevCardsNum;
    private boolean mShouldConfigBeforeStudy = true;


    /**
     * Returns a listener that rebuilds the interface after execute.
     *
     * @param refreshDecklist If true, the listener notifies the parent activity to update its deck list
     *                        to reflect the latest values.
     */
    private TaskListener getCollectionTaskListener(final boolean refreshDecklist) {
        return new TaskListener() {
            @Override
            public void onPreExecute() {

            }


            @Override
            public void onPostExecute(TaskData result) {
                dismissProgressDialog();
                if (result != null) {

                    // Get the return values back from the AsyncTask
                    Object[] obj = result.getObjArray();
                    int newCards = (Integer) obj[0];
                    int lrnCards = (Integer) obj[1];
                    int revCards = (Integer) obj[1] + (Integer) obj[2];
                    int totalNew = (Integer) obj[3];
                    int totalCards = (Integer) obj[4];
                    Timber.i("start refresh list data:" + newCards + "," + lrnCards + "," + revCards + "," + totalNew + "," + totalCards);
//                    int eta = (Integer) obj[5];

                    // Don't do anything if the fragment is no longer attached to it's Activity or col has been closed
                    if (getActivity() == null) {
                        Timber.e("StudyOptionsFragment.mRefreshFragmentListener :: can't refresh");
                        return;
                    }

                    //#5506 If we have no view, short circuit all UI logic
                    if (mStudyOptionsView == null) {
                        tryOpenCramDeckOptions();
                        return;
                    }

                    // Reinitialize controls incase changed to filtered deck
                    initAllContentViews(mStudyOptionsView);
                    // Set the deck name
                    String fullName;
                    Deck deck = getCol().getDecks().current();
                    // Main deck name
                    fullName = deck.getString("name");
                    String[] name = Decks.path(fullName);
                    StringBuilder nameBuilder = new StringBuilder();
                    if (name.length > 0) {
                        nameBuilder.append(name[name.length - 1]);
                    }
//                    if (name.length > 1) {
//                        nameBuilder.append("\n").append(name[1]);
//                    }
//                    if (name.length > 3) {
//                        nameBuilder.append("...");
//                    }
//                    if (name.length > 2) {
//                        nameBuilder.append("\n").append(name[name.length - 1]);
//                    }
//                    mTextDeckName.setText(nameBuilder.toString());
                    mDeckListAdapter.mTextDeckName = nameBuilder.toString();

                    if (tryOpenCramDeckOptions()) {
                        return;
                    }

                    // Switch between the empty view, the ordinary view, and the "congratulations" view
                    boolean isDynamic = deck.optInt("dyn", 0) != 0;
                    if (totalCards == 0 && !isDynamic) {
                        mCurrentContentView = CONTENT_EMPTY;
                        mDeckListAdapter.mDeckInfoLayoutVisible = View.VISIBLE;
                        mDeckListAdapter.mTextCongratsMessageVisible = View.VISIBLE;
//                        mDeckListAdapter.mTextCongratsMessage=getString(R.string.studyoptions_empty);
                        mDeckListAdapter.mButtonStartEnable = false;
                        mDeckListAdapter.mTextButtonStart = getString(R.string.studyoptions_start);
                    } else if (newCards + lrnCards + revCards == 0) {
                        mCurrentContentView = CONTENT_CONGRATS;
                        if (!isDynamic) {
                            mDeckListAdapter.mDeckInfoLayoutVisible = View.GONE;
                            mDeckListAdapter.mButtonStartEnable = true;
                            mDeckListAdapter.mTextButtonStart = getString(R.string.add_today_study_amount);
                        } else {
                            mDeckListAdapter.mButtonStartEnable = true;
                            mDeckListAdapter.mTextButtonStart = getString(R.string.add_today_study_amount);
                        }
                        mDeckListAdapter.mTextCongratsMessageVisible = View.VISIBLE;
//                        mDeckListAdapter.mTextCongratsMessage=getCol().getSched().finishedMsg(getActivity()).toString();
//                        mTextCongratsMessage.setText(getCol().getSched().finishedMsg(getActivity()));
                    } else {
                        mCurrentContentView = CONTENT_STUDY_OPTIONS;
                        mDeckListAdapter.mDeckInfoLayoutVisible = View.VISIBLE;
                        mDeckListAdapter.mTextCongratsMessageVisible = View.GONE;
                        mDeckListAdapter.mButtonStartEnable = true;
                        mDeckListAdapter.mTextButtonStart = getString(R.string.studyoptions_start);
                    }
                    mDeckListAdapter.setButtonStartClickListener(mButtonClickListener);
                    mDeckListAdapter.setSelfStudyClickListener(mSelfStudyListener);
                    // Set deck description
                    String desc;
                    if (isDynamic) {
                        desc = getResources().getString(R.string.dyn_deck_desc);
                    } else {
                        desc = "";
//                        desc = getCol().getDecks().getActualDescription();
                    }
                    if (desc.length() > 0) {
                        mDeckListAdapter.mTextDeckDescription = desc;
                        mDeckListAdapter.mTextDeckDescriptionVisible = View.VISIBLE;
//                        mTextDeckDescription.setText(formatDescription(desc));
//                        mTextDeckDescription.setVisibility(View.VISIBLE);
                    } else {
                        mDeckListAdapter.mTextDeckDescriptionVisible = View.GONE;
                    }

                    // Set new/learn/review card counts
                    mDeckListAdapter.mTextTodayNew = String.valueOf(newCards);
                    mDeckListAdapter.mTextTodayRev = String.valueOf(revCards);
//                    mTextTodayNew.setText(String.valueOf(newCards));
//                    mTextTodayLrn.setText(String.valueOf(lrnCards));
//                    mTextTodayRev.setText(String.valueOf(revCards));

                    // Set the total number of new cards in deck
                    if (totalNew < NEW_CARD_COUNT_TRUNCATE_THRESHOLD) {
                        // if it hasn't been truncated by libanki then just set it usually
//                        mTextNewTotal.setText(String.valueOf(totalNew));
                    } else {
                        // if truncated then make a thread to allow full count to load
//                        mTextNewTotal.setText(">1000");
                        if (mFullNewCountThread != null) {
                            // a thread was previously made -- interrupt it
                            mFullNewCountThread.interrupt();
                        }
//                        mFullNewCountThread = new Thread(() -> {
//                            Collection collection = getCol();
//                             TODO: refactor code to not rewrite this query, add to Sched.totalNewForCurrentDeck()
//                            String query = "SELECT count(*) FROM cards WHERE did IN " +
//                                    Utils.ids2str(collection.getDecks().active()) +
//                                    " AND queue = " + Consts.QUEUE_TYPE_NEW;
//                            final int fullNewCount = collection.getDb().queryScalar(query);
//                            if (fullNewCount > 0) {
//                                Runnable setNewTotalText = new Runnable() {
//                                    @Override
//                                    public void run() {
//                                            mTextNewTotal.setText(String.valueOf(fullNewCount));
//                                    }
//                                };
//                                if (!Thread.currentThread().isInterrupted()) {
//                                        mTextNewTotal.post(setNewTotalText);
//                                }
//                            }
//                        });
//                        mFullNewCountThread.start();
                    }

                    // Set total number of cards
//                    mTextTotal.setText(String.valueOf(totalCards));
                    double[] data = calculateStat(getCol(), getCol().getDecks().current().optLong("id"));
                    mNewCardsNum = (int) data[2];
                    mRevCardsNum = revCards;
                    mShouldConfigBeforeStudy = mNewCardsNum == totalCards && mShouldConfigBeforeStudy;
                    int hardNum = getLapses(getCol(), getCol().getDecks().current().optLong("id"));
                    mDeckListAdapter.mTextCountHandled = String.format(Locale.CHINA, "%d", (int) data[0]);
                    mDeckListAdapter.mTextCountLearning = String.format(Locale.CHINA, "%d", (int) data[1]);
                    mDeckListAdapter.mTextCountNew = String.format(Locale.CHINA, "%d", (int) data[2]);
                    mDeckListAdapter.mTextCountHard = String.format(Locale.CHINA, "%d", hardNum);
                    mDeckListAdapter.mTextTotal = String.format(Locale.CHINA, "共%d张卡牌", totalCards);
                    double percent = 0;
                    if (data[2] == 0) {
                        //新卡已学完，显示已掌握
                        percent = (data[0] + data[1] + data[2] <= 0) ? 0 : (data[0] / (data[0] + data[1] + data[2]) * 100);
                        mDeckListAdapter.mTextHandledNum = String.format(Locale.CHINA, "%.0f/%.0f", data[0], (data[0] + data[1] + data[2]));
//                        holder.handled_percent.setText((String.format(Locale.CHINA, "已掌握 %.1f", percent)) + "%");
                    } else {
                        percent = (data[0] + data[1] + data[2] <= 0) ? 0 : ((data[0] + data[1]) / (data[0] + data[1] + data[2]) * 100);
                        mDeckListAdapter.mTextHandledNum = String.format(Locale.CHINA, "%.0f/%.0f", data[0] + data[1], data[0] + data[1] + data[2]);
//                        holder.handled_percent.setText((String.format(Locale.CHINA, "已学 %.1f", percent)) + "%");
                    }

//                    double percent = (data[0] + data[1] + data[2] <= 0) ? 0 : (data[0] / (data[0] + data[1] + data[2]) * 100);

//                    mStudyProgress.setMax(100*100);
                    mDeckListAdapter.mStudyProgress = (int) (percent * 100);
                    mDeckListAdapter.mTextHandledPercent = (String.format(Locale.CHINA, data[2] == 0 ? "已掌握 %.1f" : "已学 %.1f", percent)) + "%";

                    // Set estimated time remaining
                    int eta = (newCards + revCards) * 10 / 60;
                    if ((newCards + revCards) % 60 != 0) {
                        eta++;
                    }
                    if (eta != -1) {
                        mDeckListAdapter.mTextETA = "" + eta;
                    } else {
                        mDeckListAdapter.mTextETA = "-";
                    }

                    mDeckListAdapter.notifyDataSetChangedAll();
                    // Rebuild the options menu
                    configureToolbar();
                }
                updateDeckList();
                // If in fragmented mode, refresh the deck list
                if (mFragmented && refreshDecklist) {
                    mListener.onRequireDeckListUpdate();
                }
            }
        };
    }


    private void addNote() {
        Intent intent = new Intent(getAnkiActivity(), NoteEditor.class);
        intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER);
        getAnkiActivity().startActivityWithAnimation(intent, ActivityTransitionAnimation.LEFT);
//        getAnkiActivity().startActivityForResultWithAnimation(intent, ADD_NOTE, ActivityTransitionAnimation.LEFT);
    }


    private double[] calculateStat(Collection col, long deckId) {
        //计算已熟悉
        Stats stats = new Stats(col, deckId);
        stats.calculateCardTypes(TYPE_LIFE);
        return stats.getSeriesList()[0];
    }


    private int getLapses(Collection col, long deckId) {
        //计算失误卡牌数
        Stats stats = new Stats(col, deckId);
        return stats.calculateCardLapses();
    }


    /**
     * Open cram deck option if deck is opened for the first time
     *
     * @return Whether we opened the deck options
     */
    private boolean tryOpenCramDeckOptions() {
        if (!mLoadWithDeckOptions) {
            return false;
        }

        openFilteredDeckOptions(true);
        mLoadWithDeckOptions = false;
        return true;
    }


//    private Collection getCol() {
//        return CollectionHelper.getInstance().getCol(getContext());
//    }
}
