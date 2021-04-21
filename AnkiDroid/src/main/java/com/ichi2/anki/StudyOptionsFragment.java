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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.snackbar.Snackbar;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.CustomStudyDialog;
import com.ichi2.anki.dialogs.DeckPickerContextMenu;
import com.ichi2.anki.widgets.DeckInfoListAdapter;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Collection;
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
import timber.log.Timber;

import static com.ichi2.anki.DeckPicker.REQUEST_BROWSE_CARDS;
import static com.ichi2.anki.DeckPicker.SHOW_STUDYOPTIONS;
import static com.ichi2.anki.DeckPicker.fadeIn;
import static com.ichi2.anki.DeckPicker.fadeOut;
import static com.ichi2.async.CollectionTask.TASK_TYPE.*;
import static com.ichi2.libanki.stats.Stats.AxisType.TYPE_LIFE;

import com.ichi2.async.TaskData;

import java.util.List;
import java.util.Locale;
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


    @Override
    public void onResume() {
        super.onResume();
        Timber.d("onResume()");
        if (getAnkiActivity() == null) {
            return;
        }
        if (mCacheID != -1) {
            getCol().getDecks().select(mCacheID);
        } else {
            mCacheID = getCol().getDecks().selected();
        }
        refreshInterface(true);
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
                mDeckListAdapter.notifyDataSetChanged();
            }
        } catch (Exception e) {
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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (container == null) {
            return null;
        }
        mStudyOptionsView = inflater.inflate(R.layout.studyoptions_fragment, container, false);
        //we need to restore here, as we need it before super.onCreate() is called.
        // Open Collection on UI thread while splash screen is showing
        mToolbar = mStudyOptionsView.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.inflateMenu(R.menu.study_options_fragment);
            mToolbar.setNavigationOnClickListener(v -> onNavigationPressed());
            TextView title = mToolbar.findViewById(R.id.toolbar_title);
            title.setOnClickListener(v -> getAnkiActivity().startActivityForResultWithAnimation(new Intent(getAnkiActivity(), CardBrowser.class), REQUEST_BROWSE_CARDS, ActivityTransitionAnimation.LEFT));
            title.setVisibility(View.VISIBLE);
            title.setText("卡牌浏览器");
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
        DividerItemDecoration dividerDecorator = new DividerItemDecoration(getAnkiActivity(), mRecyclerViewLayoutManager.getOrientation());
        dividerDecorator.setDrawable(divider);
        mRecyclerView.addItemDecoration(dividerDecorator);
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
        if (!deckDueTreeNode.shouldDisplayCounts() || deckDueTreeNode.knownToHaveRep()) {
            // If we don't yet have numbers, we trust the user that they knows what they opens, tries to open it.
            // If there is nothing to review, it'll come back to deck picker.
            openReviewerOrStudyOptions(dontSkipStudyOptions);
            return;
        }
        // There are numbers
        // Figure out what action to take
        if (getCol().getSched().hasCardsTodayAfterStudyAheadLimit()) {
            // If there are cards due that can't be studied yet (due to the learn ahead limit) then go to study options
            openStudyOptions(false);
        } else if (getCol().getSched().newDue() || getCol().getSched().revDue()) {
            // If there are no cards to review because of the daily study limit then give "Study more" option
            UIUtils.showSnackbar(getAnkiActivity(), R.string.studyoptions_limit_reached, false, R.string.study_more, v -> {
                CustomStudyDialog d = CustomStudyDialog.newInstance(
                        CustomStudyDialog.CONTEXT_MENU_LIMITS,
                        getCol().getDecks().selected(), true, this);
                showDialogFragment(d);
            }, getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
            // Check if we need to update the fragment or update the deck list. The same checks
            // are required for all snackbars below.
            if (mFragmented) {
                // Tablets must always show the study options that corresponds to the current deck,
                // regardless of whether the deck is currently reviewable or not.
                openStudyOptions(false);
            } else {
                // On phones, we update the deck list to ensure the currently selected deck is
                // highlighted correctly.
                updateDeckList();
            }
        } else if (getCol().getDecks().isDyn(did)) {
            // Go to the study options screen if filtered deck with no cards to study
            openStudyOptions(false);
        } else if (!deckDueTreeNode.hasChildren() && getCol().cardCount(new Long[] {did}) == 0) {
            // If the deck is empty and has no children then show a message saying it's empty
            final Uri helpUrl = Uri.parse(getResources().getString(R.string.link_manual_getting_started));
            getAnkiActivity().mayOpenUrl(helpUrl);
            UIUtils.showSnackbar(getAnkiActivity(), R.string.empty_deck, false, R.string.help,
                    v -> openHelpUrl(helpUrl), getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
            if (mFragmented) {
                openStudyOptions(false);
            } else {
                updateDeckList();
            }
        } else {
            // Otherwise say there are no cards scheduled to study, and give option to do custom study
            UIUtils.showSnackbar(getAnkiActivity(), R.string.studyoptions_empty_schedule, false, R.string.custom_study, v -> {
                CustomStudyDialog d = CustomStudyDialog.newInstance(
                        CustomStudyDialog.CONTEXT_MENU_EMPTY_SCHEDULE,
                        getCol().getDecks().selected(), true, this);
                showDialogFragment(d);
            }, getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
            if (mFragmented) {
                openStudyOptions(false);
            } else {
                updateDeckList();
            }
        }
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
            if (!deckPicker.getAnkiActivity().colIsOpen()) {
                deckPicker.getAnkiActivity().showProgressBar();
            }
            Timber.d("Refreshing deck list");
        }


        @Override
        public void actualOnPostExecute(@NonNull StudyOptionsFragment deckPicker, TaskData result) {
            Timber.i("Updating deck list UI");
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
        CollectionTask.TASK_TYPE taskType = quick ? LOAD_DECK_QUICK : LOAD_DECK_COUNTS;
        CollectionTask.launchCollectionTask(taskType, listener);
    }


    private boolean mHasSubDecks = false;


    private void processNames() {
        if (mInitCollapsedStatus) {
            return;
        }
        Deck deck = getCol().getDecks().current();

        long id = deck.optLong("id");
        for (Deck parent : getCol().getDecks().parents(id)) {
            Timber.d("my parents names:%s", parent.optString("name"));
            parent.put("collapsed", false);//祖先节点全部打开
            getCol().getDecks().save(parent);

        }
        TreeMap<String, Long> children = getCol().getDecks().children(id);
        Set<String> childKeys = children.keySet();
        for (String str : childKeys) {
            //孩子节点全部打开
            Timber.d("my child name:%s，%s", str, children.get(str));
            getCol().getDecks().get(children.get(str)).put("collapsed", false);
            getCol().getDecks().save(getCol().getDecks().get(children.get(str)));
        }
        if (children.size() > 0) {
            mHasSubDecks = true;
            deck.put("collapsed", false);
            getCol().getDecks().save(deck);
        } else {
            mHasSubDecks = false;
        }
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
        Timber.i("update adapter list :" + mDueTree.size());
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

        long current = getCol().getDecks().current().optLong("id");
        if (mFocusedDeck != current) {
            scrollDecklistToDeck(current);
            mFocusedDeck = current;
        }
    }


//    @Override
//    public void onAttachedToWindow() {
//
//        if (!mFragmented) {
//            Window window = getWindow();
//            window.setFormat(PixelFormat.RGBA_8888);
//        }
//    }


    @Override
    public void onAttachFragment(@NonNull Fragment childFragment) {
        super.onAttachFragment(childFragment);
        if (!mFragmented) {
            Window window = getAnkiActivity().getWindow();
            window.setFormat(PixelFormat.RGBA_8888);
        }

    }


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
        ((TextView) mCustomStudyDialog.findViewById(R.id.content)).setText(String.format(getAnkiActivity().getString(R.string.custom_study_dialog_content), col.getSched().totalNewForCurrentDeck(), col.getSched().totalRevForCurrentDeck()));
        mCustomStudyDialog.findViewById(R.id.start).setOnClickListener(view1 -> {
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


    private void openReviewer() {
//        mCacheID = getCol().getDecks().selected();
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


    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
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
                ((DeckPicker) getActivity()).renameDeckDialog(getCol().getDecks().selected());
                return true;
            case R.id.action_delete:
                ((DeckPicker) getActivity()).confirmDeckDeletion(getCol().getDecks().selected());
                return true;
            case R.id.action_export:
                ((DeckPicker) getActivity()).exportDeck(getCol().getDecks().selected());
                return true;
            case R.id.action_add_card:
                addNote();
                return true;
            default:
                return false;
        }
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
            } else {
                menu.findItem(R.id.action_rebuild).setVisible(false);
                menu.findItem(R.id.action_empty).setVisible(false);
                menu.findItem(R.id.action_custom_study).setVisible(true);
            }
            // Don't show custom study icon if congrats shown
//            if (mCurrentContentView == CONTENT_CONGRATS) {
//                menu.findItem(R.id.action_custom_study).setVisible(false);
//            }
            // Switch on rename / delete / export if tablet layout
            if (mFragmented) {
                menu.findItem(R.id.action_rename).setVisible(true);
                menu.findItem(R.id.action_delete).setVisible(true);
                menu.findItem(R.id.action_export).setVisible(true);
            } else {
                menu.findItem(R.id.action_rename).setVisible(false);
                menu.findItem(R.id.action_delete).setVisible(false);
                menu.findItem(R.id.action_export).setVisible(false);
            }
            // Switch on or off unbury depending on if there are cards to unbury
            menu.findItem(R.id.action_unbury).setVisible(getCol().getSched().haveBuried());
            // Switch on or off undo depending on whether undo is available
            menu.findItem(R.id.action_undo).setVisible(false);
//            if (!getCol().undoAvailable()) {
//                menu.findItem(R.id.action_undo).setVisible(false);
//            } else {
//                menu.findItem(R.id.action_undo).setVisible(true);
//                Resources res = AnkiDroidApp.getAppResources();
//                menu.findItem(R.id.action_undo).setTitle(res.getString(R.string.studyoptions_congrats_undo, getCol().undoName(res)));
//            }
            // Set the back button listener
//            if (!mFragmented) {
//                mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
//                mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        ((AnkiActivity) getActivity()).finishWithAnimation(ActivityTransitionAnimation.RIGHT);
//                    }
//                });
//            }
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


    private void refreshInterfaceAndDecklist(boolean resetSched) {
        refreshInterface(resetSched, true);
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
//                    refreshInterface();
                })
                .onPositive((dialog, which) -> {
                    mCacheID = getCol().getDecks().selected();
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
        CollectionTask.launchCollectionTask(UPDATE_VALUES_FROM_DECK, getCollectionTaskListener(resetDecklist),
                new TaskData(new Object[] {resetSched}));
    }


    int mNewCardsNum;
    int mRevCardsNum;


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
                    Timber.i("start refresh list data");
                    // Get the return values back from the AsyncTask
                    Object[] obj = result.getObjArray();
                    int newCards = (Integer) obj[0];
                    int lrnCards = (Integer) obj[1];
                    int revCards = (Integer) obj[1]+(Integer) obj[2];
                    int totalNew = (Integer) obj[3];
                    int totalCards = (Integer) obj[4];

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
                    int hardNum = getLapses(getCol(), getCol().getDecks().current().optLong("id"));
                    mDeckListAdapter.mTextCountHandled = String.format(Locale.CHINA, "%d", (int) data[0]);
                    mDeckListAdapter.mTextCountLearning = String.format(Locale.CHINA, "%d", (int) data[1]);
                    mDeckListAdapter.mTextCountNew = String.format(Locale.CHINA, "%d", (int) data[2]);
                    mDeckListAdapter.mTextCountHard = String.format(Locale.CHINA, "%d", hardNum);
                    mDeckListAdapter.mTextTotal = String.format(Locale.CHINA, "共%d张卡牌", totalCards);
                    double percent = (data[0] + data[1] + data[2] <= 0) ? 0 : (data[0] / (data[0] + data[1] + data[2]) * 100);
//                    mStudyProgress.setMax(100*100);
                    mDeckListAdapter.mStudyProgress = (int) (percent * 100);
                    mDeckListAdapter.mTextHandledPercent = (String.format(Locale.CHINA, "已掌握 %.1f", percent)) + "%";
                    mDeckListAdapter.mTextHandledNum = String.format(Locale.CHINA, "%.0f/%.0f", data[0], (data[0] + data[1] + data[2]));
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

                    mDeckListAdapter.notifyDataSetChanged();
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
        //计算已熟悉/全部卡片数
        Stats stats = new Stats(col, deckId);
        stats.calculateCardTypes(TYPE_LIFE);
        return stats.getSeriesList()[0];
    }


    private int getLapses(Collection col, long deckId) {
        //计算已熟悉/全部卡片数
        Stats stats = new Stats(col, deckId);
        return stats.calculateCardLapses(deckId);
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
