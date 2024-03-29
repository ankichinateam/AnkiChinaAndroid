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
//
//import android.app.AlertDialog;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.content.SharedPreferences;
//import android.graphics.Typeface;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.SystemClock;
//import android.text.TextUtils;
//import android.util.Pair;
//import android.util.TypedValue;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.WindowManager;
//import android.widget.AbsListView;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.BaseAdapter;
//import android.widget.CheckBox;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ListView;
//import android.widget.RelativeLayout;
//import android.widget.Spinner;
//import android.widget.TextView;
//
//import com.afollestad.materialdialogs.MaterialDialog;
//import com.google.android.material.snackbar.Snackbar;
//import com.ichi2.anim.ActivityTransitionAnimation;
//import com.ichi2.anki.dialogs.CardBrowserMySearchesDialog;
//import com.ichi2.anki.dialogs.ConfirmationDialog;
//import com.ichi2.anki.dialogs.IntegerDialog;
//import com.ichi2.anki.dialogs.RescheduleDialog;
//import com.ichi2.anki.dialogs.SimpleMessageDialog;
//import com.ichi2.anki.dialogs.TagsDialog;
//import com.ichi2.anki.receiver.SdCardReceiver;
//import com.ichi2.anki.widgets.DeckDropDownAdapter;
//import com.ichi2.anki.widgets.DeckInfoListAdapter;
//import com.ichi2.async.CollectionTask;
//import com.ichi2.async.TaskData;
//import com.ichi2.async.TaskListenerWithContext;
//import com.ichi2.compat.Compat;
//import com.ichi2.compat.CompatHelper;
//import com.ichi2.libanki.Card;
//import com.ichi2.libanki.Collection;
//import com.ichi2.libanki.Consts;
//import com.ichi2.libanki.Deck;
//import com.ichi2.libanki.Decks;
//import com.ichi2.libanki.Utils;
//import com.ichi2.themes.Themes;
//import com.ichi2.ui.SlidingTabLayout;
//import com.ichi2.upgrade.Upgrade;
//import com.ichi2.utils.FunctionalInterfaces;
//import com.ichi2.utils.JSONException;
//import com.ichi2.utils.JSONObject;
//import com.ichi2.utils.LanguageUtil;
//import com.ichi2.utils.Permissions;
//import com.ichi2.widget.WidgetStatus;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Objects;
//import java.util.Set;
//import java.util.regex.Pattern;
//
//import androidx.annotation.CheckResult;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.annotation.VisibleForTesting;
//import androidx.appcompat.app.ActionBar;
//import androidx.appcompat.widget.SearchView;
//import androidx.appcompat.widget.Toolbar;
//import androidx.fragment.app.Fragment;
//import androidx.fragment.app.FragmentManager;
//import androidx.fragment.app.FragmentPagerAdapter;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.viewpager.widget.ViewPager;
//import timber.log.Timber;
//
//import static com.ichi2.async.CollectionTask.TASK_TYPE.CHECK_CARD_SELECTION;
//import static com.ichi2.async.CollectionTask.TASK_TYPE.DISMISS_MULTI;
//import static com.ichi2.async.CollectionTask.TASK_TYPE.RENDER_BROWSER_QA;
//import static com.ichi2.async.CollectionTask.TASK_TYPE.SEARCH_CARDS;
//import static com.ichi2.async.CollectionTask.TASK_TYPE.UNDO;
//import static com.ichi2.async.CollectionTask.TASK_TYPE.UPDATE_NOTE;
//import static com.ichi2.libanki.stats.Stats.SECONDS_PER_DAY;
//
//
//
//
//public class CardsListFragment extends AnkiFragment {
//    /** List of cards in the browser.
//     * When the list is changed, the position member of its elements should get changed.*/
//    @NonNull
//    private List<CardCache> mCards = new ArrayList<>();
//    private ArrayList<Deck> mDropDownDecks;
//    private ListView mCardsListView;
//    private SearchView mSearchView;
//    private MultiColumnListAdapter mCardsAdapter;
//    private String mSearchTerms;
//    private String mRestrictOnDeck;
//
//    private MenuItem mSearchItem;
//    private MenuItem mSaveSearchItem;
//    private MenuItem mMySearchesItem;
//    private MenuItem mPreviewItem;
//
//    private Snackbar mUndoSnackbar;
//
//    public static Card sCardsListFragmentCard;
//
//    // card that was clicked (not marked)
//    private long mCurrentCardId;
//
//    private int mOrder;
//    private boolean mOrderAsc;
//    private int mColumn1Index;
//    private int mColumn2Index;
//
//    //DEFECT: Doesn't need to be a local
//    private long mNewDid;   // for change_deck
//
////    private static final int EDIT_CARD = 0;
////    private static final int ADD_NOTE = 1;
//    private static final int PREVIEW_CARDS = 2;
//
//    private static final int DEFAULT_FONT_SIZE_RATIO = 100;
//    // Should match order of R.array.card_browser_order_labels
//    public static final int CARD_ORDER_NONE = 0;
//    private static final String[] fSortTypes = new String[] {
//            "",
//            "noteFld",
//            "noteCrt",
//            "noteMod",
//            "cardMod",
//            "cardDue",
//            "cardIvl",
//            "cardEase",
//            "cardReps",
//            "cardLapses"};
////    private static final Column[] COLUMN1_KEYS = {QUESTION, SFLD};
////
////    // list of available keys in mCards corresponding to the column names in R.array.browser_column2_headings.
////    // Note: the last 6 are currently hidden
////    private static final Column[] COLUMN2_KEYS = {ANSWER,
////            CARD,
////            DECK,
////            NOTE_TYPE,
////            QUESTION,
////            TAGS,
////            LAPSES,
////            REVIEWS,
////            INTERVAL,
////            EASE,
////            DUE,
////            CHANGED,
////            CREATED,
////            EDITED,
////    };
//    private long mLastRenderStart = 0;
//    private DeckDropDownAdapter mDropDownAdapter;
//
//    private boolean mReloadRequired = false;
//    private boolean mInMultiSelectMode = false;
//    private Set<CardCache> mCheckedCards = Collections.synchronizedSet(new LinkedHashSet<>());
//    private int mLastSelectedPosition;
//    @Nullable
//    private Menu mActionBarMenu;
//
//    private static final int SNACKBAR_DURATION = 8000;
//
//
//    // Values related to persistent state data
//    private static final long ALL_DECKS_ID = 0L;
//    private static String PERSISTENT_STATE_FILE = "DeckPickerState";
//    private static String LAST_DECK_ID_KEY = "lastDeckId";
//
//    /**
//     * Broadcast that informs us when the sd card is about to be unmounted
//     */
//    private BroadcastReceiver mUnmountReceiver = null;
//
//    private MaterialDialog.ListCallbackSingleChoice mOrderDialogListener =
//            new MaterialDialog.ListCallbackSingleChoice() {
//                @Override
//                public boolean onSelection(MaterialDialog materialDialog, View view, int which,
//                                           CharSequence charSequence) {
//                    if (which != mOrder) {
//                        mOrder = which;
//                        mOrderAsc = false;
//                        if (mOrder == 0) {
//                            getAnkiActivity().getCol().getConf().put("sortType", fSortTypes[1]);
//                            AnkiDroidApp.getSharedPrefs(getAnkiActivity()).edit()
//                                    .putBoolean("CardsListFragmentNoSorting", true)
//                                    .commit();
//                        } else {
//                            getAnkiActivity().getCol().getConf().put("sortType", fSortTypes[mOrder]);
//                            AnkiDroidApp.getSharedPrefs(getAnkiActivity()).edit()
//                                    .putBoolean("CardsListFragmentNoSorting", false)
//                                    .commit();
//                        }
//                        getAnkiActivity().getCol().getConf().put("sortBackwards", mOrderAsc);
//                        searchCards();
//                    } else if (which != CARD_ORDER_NONE) {
//                        mOrderAsc = !mOrderAsc;
//                        getAnkiActivity().getCol().getConf().put("sortBackwards", mOrderAsc);
//                        Collections.reverse(mCards);
//                        updateList();
//                    }
//                    return true;
//                }
//            };
//
//
//    private RepositionCardHandler repositionCardHandler() {
//        return new RepositionCardHandler(this);
//    }
//
//    private static class RepositionCardHandler extends TaskListenerWithContext<CardsListFragment> {
//        public RepositionCardHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListFragment browser) {
//            Timber.d("CardsListFragment::RepositionCardHandler() onPreExecute");
//        }
//
//
//        @Override
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            Timber.d("CardsListFragment::RepositionCardHandler() onPostExecute");
//            browser.mReloadRequired = true;
//            int cardCount = result.getObjArray().length;
//            UIUtils.showThemedToast(browser.getAnkiActivity(),
//                    browser.getResources().getQuantityString(R.plurals.reposition_card_dialog_acknowledge, cardCount, cardCount), true);
//        }
//    }
//
//    private ResetProgressCardHandler resetProgressCardHandler() {
//        return new ResetProgressCardHandler(this);
//    }
//    private static class ResetProgressCardHandler extends TaskListenerWithContext<CardsListFragment>{
//        public ResetProgressCardHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListFragment browser) {
//            Timber.d("CardsListFragment::ResetProgressCardHandler() onPreExecute");
//        }
//
//
//        @Override
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            Timber.d("CardsListFragment::ResetProgressCardHandler() onPostExecute");
//            browser.mReloadRequired = true;
//            int cardCount = result.getObjArray().length;
//            UIUtils.showThemedToast(browser.getAnkiActivity(),
//                    browser.getResources().getQuantityString(R.plurals.reset_cards_dialog_acknowledge, cardCount, cardCount), true);
//        }
//    }
//
//    private RescheduleCardHandler rescheduleCardHandler() {
//        return new RescheduleCardHandler(this);
//    }
//    private static class RescheduleCardHandler extends TaskListenerWithContext<CardsListFragment>{
//        public RescheduleCardHandler (CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListFragment browser) {
//            Timber.d("CardsListFragment::RescheduleCardHandler() onPreExecute");
//        }
//
//
//        @Override
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            Timber.d("CardsListFragment::RescheduleCardHandler() onPostExecute");
//            browser.mReloadRequired = true;
//            int cardCount = result.getObjArray().length;
//            UIUtils.showThemedToast(browser.getAnkiActivity(),
//                    browser.getResources().getQuantityString(R.plurals.reschedule_cards_dialog_acknowledge, cardCount, cardCount), true);
//        }
//    }
//
//    private CardBrowserMySearchesDialog.MySearchesDialogListener mMySearchesDialogListener =
//            new  CardBrowserMySearchesDialog.MySearchesDialogListener() {
//                @Override
//                public void onSelection(String searchName) {
//                    Timber.d("OnSelection using search named: %s", searchName);
//                    JSONObject savedFiltersObj = getAnkiActivity().getCol().getConf().optJSONObject("savedFilters");
//                    Timber.d("SavedFilters are %s", savedFiltersObj.toString());
//                    if (savedFiltersObj != null) {
//                        mSearchTerms = savedFiltersObj.optString(searchName);
//                        Timber.d("OnSelection using search terms: %s", mSearchTerms);
//                        mSearchView.setQuery(mSearchTerms, false);
//                        mSearchItem.expandActionView();
//                        searchCards();
//                    }
//                }
//
//                @Override
//                public void onRemoveSearch(String searchName) {
//                    Timber.d("OnRemoveSelection using search named: %s", searchName);
//                    JSONObject savedFiltersObj = getAnkiActivity().getCol().getConf().optJSONObject("savedFilters");
//                    if (savedFiltersObj != null && savedFiltersObj.has(searchName)) {
//                        savedFiltersObj.remove(searchName);
//                        getAnkiActivity().getCol().getConf().put("savedFilters", savedFiltersObj);
//                        getAnkiActivity().getCol().flush();
//                        if (savedFiltersObj.length() == 0) {
//                            mMySearchesItem.setVisible(false);
//                        }
//                    }
//
//                }
//
//                @Override
//                public void onSaveSearch(String searchName, String searchTerms) {
//                    if (TextUtils.isEmpty(searchName)) {
//                        UIUtils.showThemedToast(getAnkiActivity(),
//                                getString(R.string.card_browser_list_my_searches_new_search_error_empty_name), true);
//                        return;
//                    }
//                    JSONObject savedFiltersObj = getAnkiActivity().getCol().getConf().optJSONObject("savedFilters");
//                    boolean should_save = false;
//                    if (savedFiltersObj == null) {
//                        savedFiltersObj = new JSONObject();
//                        savedFiltersObj.put(searchName, searchTerms);
//                        should_save = true;
//                    } else if (!savedFiltersObj.has(searchName)) {
//                        savedFiltersObj.put(searchName, searchTerms);
//                        should_save = true;
//                    } else {
//                        UIUtils.showThemedToast(getAnkiActivity(),
//                                getString(R.string.card_browser_list_my_searches_new_search_error_dup), true);
//                    }
//                    if (should_save) {
//                        getAnkiActivity().getCol().getConf().put("savedFilters", savedFiltersObj);
//                        getAnkiActivity().getCol().flush();
//                        mSearchView.setQuery("", false);
//                        mMySearchesItem.setVisible(true);
//                    }
//                }
//            };
//
//
//    private void onSearch() {
//        mSearchTerms = mSearchView.getQuery().toString();
//        if (mSearchTerms.length() == 0) {
//            mSearchView.setQueryHint(getResources().getString(R.string.downloaddeck_search));
//        }
//        searchCards();
//    }
//
//    private long[] getSelectedCardIds() {
//        long[] ids = new long[mCheckedCards.size()];
//        int count = 0;
//        for (CardCache cardPosition : mCheckedCards) {
//            ids[count++] = cardPosition.getId();
//        }
//        return ids;
//    }
//
//    private boolean canPerformMultiSelectEditNote() {
//        //The noteId is not currently available. Only allow if a single card is selected for now.
//        return checkedCardCount() == 1;
//    }
//
//    @VisibleForTesting
//    void changeDeck(int deckPosition) {
//        long[] ids = getSelectedCardIds();
//
//        Deck selectedDeck = getValidDecksForChangeDeck().get(deckPosition);
//
//        try {
//            //#5932 - can't be dynamic
//            if (Decks.isDynamic(selectedDeck)) {
//                Timber.w("Attempted to change cards to dynamic deck. Cancelling operation.");
//                displayCouldNotChangeDeck();
//                return;
//            }
//        } catch (Exception e) {
//            displayCouldNotChangeDeck();
//            Timber.e(e);
//            return;
//        }
//
//        mNewDid = selectedDeck.getLong("id");
//
//        Timber.i("Changing selected cards to deck: %d", mNewDid);
//
//        if (ids.length == 0) {
//            endMultiSelectMode();
//            mCardsAdapter.notifyDataSetChanged();
//            return;
//        }
//
//        if (CardUtils.isIn(ids, getReviewerCardId())) {
//            mReloadRequired = true;
//        }
//
//        executeChangeCollectionTask(ids, mNewDid);
//    }
//
//
//    private void displayCouldNotChangeDeck() {
//        UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.card_browser_deck_change_error), true);
//    }
//
//
//    private Long getLastDeckId() {
//        SharedPreferences state = getAnkiActivity().getSharedPreferences(PERSISTENT_STATE_FILE,0);
//        if (!state.contains(LAST_DECK_ID_KEY)) {
//            return null;
//        }
//        return state.getLong(LAST_DECK_ID_KEY, -1);
//    }
//
//    public static void clearLastDeckId() {
//        Context context = AnkiDroidApp.getInstance();
//        context.getSharedPreferences(PERSISTENT_STATE_FILE,0).edit().remove(LAST_DECK_ID_KEY).apply();
//    }
//
//    private void saveLastDeckId(Long id) {
//        if (id == null) {
//            clearLastDeckId();
//            return;
//        }
//        getAnkiActivity().getSharedPreferences(PERSISTENT_STATE_FILE, 0).edit().putLong(LAST_DECK_ID_KEY, id).apply();
//    }
//
//
//    private View mRoot;
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Timber.d("onCreate()");
//    }
//
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        if (mRoot != null) {
//            return mRoot;
//        }
//        mRoot = inflater.inflate(R.layout.fragment_self_study_child, container, false);
//
//        return mRoot;
//    }
//
//
//    // Finish initializing the activity after the collection has been correctly loaded
//    @Override
//    public void onCollectionLoaded(Collection col) {
//        super.onCollectionLoaded(col);
//        Timber.d("onCollectionLoaded()");
//        registerExternalStorageListener();
//
//        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getAnkiActivity());
//        // Load reference to action bar title
//
//        // Add drop-down menu to select deck to action bar.
//        mDropDownDecks = getAnkiActivity().getCol().getDecks().allSorted();
//        mDropDownAdapter = new DeckDropDownAdapter(getAnkiActivity(), mDropDownDecks,R.layout.dropdown_deck_selected_item,getAnkiActivity());
//
//
//
//        mOrder = CARD_ORDER_NONE;
//        String colOrder = getAnkiActivity().getCol().getConf().getString("sortType");
//        for (int c = 0; c < fSortTypes.length; ++c) {
//            if (fSortTypes[c].equals(colOrder)) {
//                mOrder = c;
//                break;
//            }
//        }
//        if (mOrder == 1 && preferences.getBoolean("CardsListFragmentNoSorting", false)) {
//            mOrder = 0;
//        }
//        //getAnkiActivity() upgrade should already have been done during
//        //setConf. However older version of AnkiDroid didn't call
//        //upgradeJSONIfNecessary during setConf, which means the
//        //conf saved may still have getAnkiActivity() bug.
//        mOrderAsc = Upgrade.upgradeJSONIfNecessary(getCol(), getAnkiActivity().getCol().getConf(), "sortBackwards", false);
//
//        mCards = new ArrayList<>();
//        mCardsListView = (ListView) findViewById(R.id.card_browser_list);
//        // Create a spinner for column1
//        Spinner cardsColumn1Spinner = (Spinner) findViewById(R.id.browser_column1_spinner);
//        ArrayAdapter<CharSequence> column1Adapter = ArrayAdapter.createFromResource(getAnkiActivity(),
//                R.array.browser_column1_headings, android.R.layout.simple_spinner_item);
//        column1Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        cardsColumn1Spinner.setAdapter(column1Adapter);
//        mColumn1Index = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getInt("CardsListFragmentColumn1", 0);
//        cardsColumn1Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                // If a new column was selected then change the key used to map from mCards to the column TextView
//                if (pos != mColumn1Index) {
//                    mColumn1Index = pos;
//                    AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().getAnkiActivity()).edit()
//                            .putInt("CardsListFragmentColumn1", mColumn1Index).commit();
//                    Column[] fromMap = mCardsAdapter.getFromMapping();
//                    fromMap[0] = COLUMN1_KEYS[mColumn1Index];
//                    mCardsAdapter.setFromMapping(fromMap);
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do Nothing
//            }
//        });
//        // Load default value for column2 selection
//        mColumn2Index = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getInt("CardsListFragmentColumn2", 0);
//        // Setup the column 2 heading as a spinner so that users can easily change the column type
//        Spinner cardsColumn2Spinner = (Spinner) findViewById(R.id.browser_column2_spinner);
//        ArrayAdapter<CharSequence> column2Adapter = ArrayAdapter.createFromResource(getAnkiActivity(),
//                R.array.browser_column2_headings, android.R.layout.simple_spinner_item);
//        column2Adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        cardsColumn2Spinner.setAdapter(column2Adapter);
//        // Create a new list adapter with updated column map any time the user changes the column
//        cardsColumn2Spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                // If a new column was selected then change the key used to map from mCards to the column TextView
//                if (pos != mColumn2Index) {
//                    mColumn2Index = pos;
//                    AnkiDroidApp.getSharedPrefs(AnkiDroidApp.getInstance().getAnkiActivity()).edit()
//                            .putInt("CardsListFragmentColumn2", mColumn2Index).commit();
//                    Column[] fromMap = mCardsAdapter.getFromMapping();
//                    fromMap[1] = COLUMN2_KEYS[mColumn2Index];
//                    mCardsAdapter.setFromMapping(fromMap);
//                }
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do Nothing
//            }
//        });
//        // get the font and font size from the preferences
//        int sflRelativeFontSize = preferences.getInt("relativeCardsListFragmentFontSize", DEFAULT_FONT_SIZE_RATIO);
//        String sflCustomFont = preferences.getString("browserEditorFont", "");
//        Column[] columnsContent = {COLUMN1_KEYS[mColumn1Index], COLUMN2_KEYS[mColumn2Index]};
//        // make a new list adapter mapping the data in mCards to column1 and column2 of R.layout.card_item_browser
//        mCardsAdapter = new MultiColumnListAdapter(
//                getAnkiActivity(),
//                R.layout.card_item_browser,
//                columnsContent,
//                new int[] {R.id.card_sfld, R.id.card_column2},
//                sflRelativeFontSize,
//                sflCustomFont);
//        // link the adapter to the main mCardsListView
//        mCardsListView.setAdapter(mCardsAdapter);
//        // make the items (e.g. question & answer) render dynamically when scrolling
//        mCardsListView.setOnScrollListener(new RenderOnScroll());
//        // set the spinner index
//        cardsColumn1Spinner.setSelection(mColumn1Index);
//        cardsColumn2Spinner.setSelection(mColumn2Index);
//
//
//        mCardsListView.setOnItemClickListener(new ListView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (mInMultiSelectMode) {
//                    // click on whole cell triggers select
//                    CheckBox cb = (CheckBox) view.findViewById(R.id.card_checkbox);
//                    cb.toggle();
//                    onCheck(position, view);
//                } else {
//                    // load up the card selected on the list
//                    long clickedCardId = getCards().get(position).getId();
//                    openNoteEditorForCard(clickedCardId);
//                }
//            }
//        });
//        mCardsListView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
//            @Override
//            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long id) {
//                mLastSelectedPosition = position;
//                loadMultiSelectMode();
//
//                // click on whole cell triggers select
//                CheckBox cb = (CheckBox) view.findViewById(R.id.card_checkbox);
//                cb.toggle();
//                onCheck(position, view);
//                recenterListView(view);
//                mCardsAdapter.notifyDataSetChanged();
//                return true;
//            }
//        });
//
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//
//        // If a valid value for last deck exists then use it, otherwise use libanki selected deck
//        if (getLastDeckId() != null && getLastDeckId() == ALL_DECKS_ID) {
//            selectAllDecks();
//        } else  if (getLastDeckId() != null && getAnkiActivity().getCol().getDecks().get(getLastDeckId(), false) != null) {
//            selectDeckById(getLastDeckId());
//        } else {
//            selectDeckById(getAnkiActivity().getCol().getDecks().selected());
//        }
//    }
//
//
//    private void selectAllDecks() {
//        selectDropDownItem(0);
//    }
//
//
//    /** Opens the note editor for a card.
//     * We use the Card ID to specify the preview target */
//    public void openNoteEditorForCard(long cardId) {
//        mCurrentCardId = cardId;
//        sCardsListFragmentCard = getAnkiActivity().getCol().getCard(mCurrentCardId);
//        // start note editor using the card we just loaded
//        Intent editCard = new Intent(getAnkiActivity(), NoteEditor.class);
//        editCard.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CardsListFragment_EDIT);
//        editCard.putExtra(NoteEditor.EXTRA_CARD_ID, sCardsListFragmentCard.getId());
//        startActivityForResultWithAnimation(editCard, EDIT_CARD, ActivityTransitionAnimation.LEFT);
//        //#6432 - FIXME - onCreateOptionsMenu crashes if receiving an activity result from edit card when in multiselect
//        endMultiSelectMode();
//    }
//
//    private void openNoteEditorForCurrentlySelectedNote() {
//        try {
//            //Just select the first one. It doesn't particularly matter if there's a multiselect occurring.
//            openNoteEditorForCard(getSelectedCardIds()[0]);
//        } catch (Exception e) {
//            Timber.w(e, "Error Opening Note Editor");
//            UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.card_browser_note_editor_error), false);
//        }
//    }
//
//
//    @Override
//    protected void onStop() {
//        Timber.d("onStop()");
//        // cancel rendering the question and answer, which has shared access to mCards
//        super.onStop();
//        if (!isFinishing()) {
//            WidgetStatus.update(getAnkiActivity());
//            UIUtils.saveCollectionInBackground();
//        }
//    }
//
//
//    @Override
//    protected void onDestroy() {
//        Timber.d("onDestroy()");
//        invalidate();
//        super.onDestroy();
//        if (mUnmountReceiver != null) {
//            unregisterReceiver(mUnmountReceiver);
//        }
//    }
//
//
//    @Override
//    public void onBackPressed() {
////        if (isDrawerOpen()) {
////            super.onBackPressed();
////        } else
//        if (mInMultiSelectMode) {
//            endMultiSelectMode();
//        } else {
//            Timber.i("Back key pressed");
//            Intent data = new Intent();
//            if (mReloadRequired) {
//                // Add reload flag to result intent so that schedule reset when returning to note editor
//                data.putExtra("reloadRequired", true);
//            }
//            closeCardsListFragment(RESULT_OK, data);
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        Timber.d("onResume()");
//        super.onResume();
////        selectNavigationItem(R.id.nav_browser);
//    }
//
//
//    @Override
//    public boolean onCreateOptionsMenu(final Menu menu) {
//        Timber.d("onCreateOptionsMenu()");
//        mActionBarMenu = menu;
//        if (!mInMultiSelectMode) {
//            // restore drawer click listener and icon
////            restoreDrawerIcon();
//            getMenuInflater().inflate(R.menu.card_browser, menu);
//            mSaveSearchItem = menu.findItem(R.id.action_save_search);
//            mSaveSearchItem.setVisible(false); //the searchview's query always starts empty.
//            mMySearchesItem = menu.findItem(R.id.action_list_my_searches);
//            JSONObject savedFiltersObj = getAnkiActivity().getCol().getConf().optJSONObject("savedFilters");
//            mMySearchesItem.setVisible(savedFiltersObj != null && savedFiltersObj.length() > 0);
//            mSearchItem = menu.findItem(R.id.action_search);
//            mSearchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
//                @Override
//                public boolean onMenuItemActionExpand(MenuItem item) {
//                    return true;
//                }
//
//                @Override
//                public boolean onMenuItemActionCollapse(MenuItem item) {
//                    // SearchView doesn't support empty queries so we always reset the search when collapsing
//                    mSearchTerms = "";
//                    mSearchView.setQuery(mSearchTerms, false);
//                    searchCards();
//                    // invalidate options menu so that disappeared icons would appear again
//                    supportInvalidateOptionsMenu();
//                    return true;
//                }
//            });
//            mSearchView = (SearchView) mSearchItem.getActionView();
//            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
//                @Override
//                public boolean onQueryTextChange(String newText) {
//                    mSaveSearchItem.setVisible(!TextUtils.isEmpty(newText));
//                    return true;
//                }
//
//                @Override
//                public boolean onQueryTextSubmit(String query) {
//                    onSearch();
//                    mSearchView.clearFocus();
//                    return true;
//                }
//            });
//            mSearchView.setOnSearchClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    // Provide SearchView with the previous search terms
//                    mSearchView.setQuery(mSearchTerms, false);
//                }
//            });
//            // Fixes #6500 - keep the search consistent
//            if (!TextUtils.isEmpty(mSearchTerms)) {
//                mSearchItem.expandActionView();
//                mSearchView.setQuery(mSearchTerms, false);
//            }
//        } else {
//            // multi-select mode
//            getMenuInflater().inflate(R.menu.card_browser_multiselect, menu);
////            showBackIcon();
//        }
//
//        if (mActionBarMenu != null && mActionBarMenu.findItem(R.id.action_undo) != null) {
//            MenuItem undo =  mActionBarMenu.findItem(R.id.action_undo);
//            undo.setVisible(getAnkiActivity().getCol().undoAvailable());
//            undo.setTitle(getResources().getString(R.string.studyoptions_congrats_undo, getAnkiActivity().getCol().undoName(getResources())));
//        }
//
//        // Maybe we were called from ACTION_PROCESS_TEXT.
//        // In that case we already fill in the search.
//        Intent intent = getIntent();
//        Compat compat = CompatHelper.getCompat();
//        if (intent.getAction() == compat.ACTION_PROCESS_TEXT) {
//            CharSequence search = intent.getCharSequenceExtra(compat.EXTRA_PROCESS_TEXT);
//            if (search != null && search.length() != 0) {
//                Timber.i("CardsListFragment :: Called with search intent: %s", search.toString());
//                mSearchView.setQuery(search, true);
//                intent.setAction(Intent.ACTION_DEFAULT);
//            }
//        }
//
//        mPreviewItem = menu.findItem(R.id.action_preview);
//        onSelectionChanged();
//        updatePreviewMenuItem();
//        return super.onCreateOptionsMenu(menu);
//    }
//
////    @Override
////    protected void onNavigationPressed() {
////        if (mInMultiSelectMode) {
////            endMultiSelectMode();
////        } else {
////            super.onNavigationPressed();
////        }
////    }
//
//
//
//
//
//
//
//    private void updatePreviewMenuItem() {
//        if (mPreviewItem == null) {
//            return;
//        }
//        mPreviewItem.setVisible(getCardCount() > 0);
//    }
//
//    /** Returns the number of cards that are visible on the screen */
//    public int getCardCount() {
//        return getCards().size();
//    }
//
//
//    private void updateMultiselectMenu() {
//        Timber.d("updateMultiselectMenu()");
//        if (mActionBarMenu == null || mActionBarMenu.findItem(R.id.action_suspend_card) == null) {
//            return;
//        }
//
//        if (!mCheckedCards.isEmpty()) {
//            CollectionTask.cancelAllTasks(CHECK_CARD_SELECTION);
//            CollectionTask.launchCollectionTask(CHECK_CARD_SELECTION,
//                    mCheckSelectedCardsHandler,
//                    new TaskData(new Object[]{mCheckedCards, getCards()}));
//        }
//
//        mActionBarMenu.findItem(R.id.action_select_all).setVisible(!hasSelectedAllCards());
//        //Note: Theoretically should not happen, as getAnkiActivity() should kick us back to the menu
//        mActionBarMenu.findItem(R.id.action_select_none).setVisible(hasSelectedCards());
//        mActionBarMenu.findItem(R.id.action_edit_note).setVisible(canPerformMultiSelectEditNote());
//    }
//
//
//    private boolean hasSelectedCards() {
//        return !mCheckedCards.isEmpty();
//    }
//
//    private boolean hasSelectedAllCards() {
//        return checkedCardCount() >= getCardCount(); //must handle 0.
//    }
//
//
//    private void flagTask (int flag) {
//        CollectionTask.launchCollectionTask(DISMISS_MULTI,
//                flagCardHandler(),
//                new TaskData(new Object[]{getSelectedCardIds(), Collection.DismissType.FLAG, new Integer (flag)}));
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
////        if (getDrawerToggle().onOptionsItemSelected(item)) {
////            return true;
////        }
//
//        // dismiss undo-snackbar if shown to avoid race condition
//        // (when another operation will be performed on the model, it will undo the latest operation)
//        if (mUndoSnackbar != null && mUndoSnackbar.isShown())
//            mUndoSnackbar.dismiss();
//
//        switch (item.getItemId()) {
//            case android.R.id.home:
//                endMultiSelectMode();
//                return true;
//            case R.id.action_add_note_from_card_browser: {
//                Intent intent = new Intent(getAnkiActivity(), NoteEditor.class);
//                intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CardsListFragment_ADD);
//                startActivityForResultWithAnimation(intent, ADD_NOTE, ActivityTransitionAnimation.LEFT);
//                return true;
//            }
//
//            case R.id.action_save_search: {
//                String searchTerms = mSearchView.getQuery().toString();
//                showDialogFragment(CardsListFragmentMySearchesDialog.newInstance(null, mMySearchesDialogListener,
//                        searchTerms, CardsListFragmentMySearchesDialog.CARD_BROWSER_MY_SEARCHES_TYPE_SAVE));
//                return true;
//            }
//
//            case R.id.action_list_my_searches: {
//                JSONObject savedFiltersObj = getAnkiActivity().getCol().getConf().optJSONObject("savedFilters");
//                HashMap<String, String> savedFilters = new HashMap<>();
//                if (savedFiltersObj != null) {
//                    Iterator<String> it = savedFiltersObj.keys();
//                    while (it.hasNext()) {
//                        String searchName = it.next();
//                        savedFilters.put(searchName, savedFiltersObj.optString(searchName));
//                    }
//                }
//                showDialogFragment(CardsListFragmentMySearchesDialog.newInstance(savedFilters, mMySearchesDialogListener,
//                        "", CardsListFragmentMySearchesDialog.CARD_BROWSER_MY_SEARCHES_TYPE_LIST));
//                return true;
//            }
//
//            case R.id.action_sort_by_size:
//                showDialogFragment(CardsListFragmentOrderDialog
//                        .newInstance(mOrder, mOrderAsc, mOrderDialogListener));
//                return true;
//
//            case R.id.action_show_marked:
//                mSearchTerms = "tag:marked";
//                mSearchView.setQuery("", false);
//                mSearchView.setQueryHint(getResources().getString(R.string.card_browser_show_marked));
//                searchCards();
//                return true;
//
//            case R.id.action_show_suspended:
//                mSearchTerms = "is:suspended";
//                mSearchView.setQuery("", false);
//                mSearchView.setQueryHint(getResources().getString(R.string.card_browser_show_suspended));
//                searchCards();
//                return true;
//
//            case R.id.action_search_by_tag:
//                showTagsDialog();
//                return true;
//
//            case R.id.action_flag_zero:
//                flagTask(0);
//                return true;
//
//            case R.id.action_flag_one:
//                flagTask(1);
//                return true;
//
//            case R.id.action_flag_two:
//                flagTask(2);
//                return true;
//
//            case R.id.action_flag_three:
//                flagTask(3);
//                return true;
//
//            case R.id.action_flag_four:
//                flagTask(4);
//                return true;
//
//            case R.id.action_delete_card:
//                if (mInMultiSelectMode) {
//                    CollectionTask.launchCollectionTask(DISMISS_MULTI,
//                            mDeleteNoteHandler,
//                            new TaskData(new Object[]{getSelectedCardIds(), Collection.DismissType.DELETE_NOTE_MULTI}));
//
//                    mCheckedCards.clear();
//                    endMultiSelectMode();
//                    mCardsAdapter.notifyDataSetChanged();
//                }
//                return true;
//
//            case R.id.action_mark_card:
//                CollectionTask.launchCollectionTask(DISMISS_MULTI,
//                        markCardHandler(),
//                        new TaskData(new Object[]{getSelectedCardIds(), Collection.DismissType.MARK_NOTE_MULTI}));
//
//                return true;
//
//
//            case R.id.action_suspend_card:
//                CollectionTask.launchCollectionTask(DISMISS_MULTI,
//                        suspendCardHandler(),
//                        new TaskData(new Object[]{getSelectedCardIds(), Collection.DismissType.SUSPEND_CARD_MULTI}));
//
//                return true;
////            case R.id.action_change_deck: {
////                AlertDialog.Builder builderSingle = new AlertDialog.Builder(getAnkiActivity());
////                builderSingle.setTitle(getString(R.string.move_all_to_deck));
////
////                //WARNING: changeDeck depends on getAnkiActivity() index, so any changes should be reflected there.
////                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getAnkiActivity(), R.layout.dropdown_deck_item);
////                for (Deck deck : getValidDecksForChangeDeck()) {
////                    try {
////                        arrayAdapter.add(deck.getString("name"));
////                    } catch (JSONException e) {
////                        e.printStackTrace();
////                    }
////                }
////
////                builderSingle.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
////                    @Override
////                    public void onClick(DialogInterface dialog, int which) {
////                        dialog.dismiss();
////                    }
////                });
////
////                builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
////                    @Override
////                    public void onClick(DialogInterface dialog, int which) {
////                        changeDeck(which);
////                    }
////                });
////                builderSingle.show();
////
////                return true;
////            }
////
////            case R.id.action_undo:
////                if (getAnkiActivity().getCol().undoAvailable()) {
////                    CollectionTask.launchCollectionTask(UNDO, mUndoHandler);
////                }
////                return true;
////            case R.id.action_select_none:
////                onSelectNone();
////                return true;
////            case R.id.action_select_all:
////                onSelectAll();
////                return true;
////
////            case R.id.action_preview: {
////                Intent previewer = new Intent(getAnkiActivity(), Previewer.class);
////                if (mInMultiSelectMode && checkedCardCount() > 1) {
////                    // Multiple cards have been explicitly selected, so preview only those cards
////                    previewer.putExtra("index", 0);
////                    previewer.putExtra("cardList", getSelectedCardIds());
////                } else {
////                    // Preview all cards, starting from the one that is currently selected
////                    int startIndex = mCheckedCards.isEmpty() ? 0 : mCheckedCards.iterator().next().getPosition();
////                    previewer.putExtra("index", startIndex);
////                    previewer.putExtra("cardList", getAllCardIds());
////                }
////                startActivityForResultWithoutAnimation(previewer, PREVIEW_CARDS);
////                return true;
////            }
////
////            case R.id.action_reset_cards_progress: {
////                Timber.i("NoteEditor:: Reset progress button pressed");
////                // Show confirmation dialog before resetting card progress
////                ConfirmationDialog dialog = new ConfirmationDialog();
////                String title = getString(R.string.reset_card_dialog_title);
////                String message = getString(R.string.reset_card_dialog_message);
////                dialog.setArgs(title, message);
////                Runnable confirm = () -> {
////                    Timber.i("CardsListFragment:: ResetProgress button pressed");
////                    CollectionTask.launchCollectionTask(DISMISS_MULTI, resetProgressCardHandler(),
////                            new TaskData(new Object[]{getSelectedCardIds(), Collection.DismissType.RESET_CARDS}));
////                };
////                dialog.setConfirm(confirm);
////                showDialogFragment(dialog);
////                return true;
////            }
////            case R.id.action_reschedule_cards: {
////                Timber.i("CardsListFragment:: Reschedule button pressed");
////
////                long[] selectedCardIds = getSelectedCardIds();
////                FunctionalInterfaces.Consumer<Integer> consumer = newDays ->
////                        CollectionTask.launchCollectionTask(DISMISS_MULTI,
////                                rescheduleCardHandler(),
////                                new TaskData(new Object[]{selectedCardIds, Collection.DismissType.RESCHEDULE_CARDS, newDays}));
////
////                RescheduleDialog rescheduleDialog;
////                if (selectedCardIds.length == 1) {
////                    long cardId = selectedCardIds[0];
////                    Card selected = getAnkiActivity().getCol().getCard(cardId);
////                    rescheduleDialog = RescheduleDialog.rescheduleSingleCard(getResources(), selected, consumer);
////                } else {
////                    rescheduleDialog = RescheduleDialog.rescheduleMultipleCards(getResources(),
////                            consumer,
////                            selectedCardIds.length);
////                }
////                showDialogFragment(rescheduleDialog);
////                return true;
////            }
////            case R.id.action_reposition_cards: {
////                Timber.i("CardsListFragment:: Reposition button pressed");
////
////                // Only new cards may be repositioned
////                long[] cardIds = getSelectedCardIds();
////                for (int i = 0; i < cardIds.length; i++) {
////                    if (getAnkiActivity().getCol().getCard(cardIds[i]).getQueue() != Consts.CARD_TYPE_NEW) {
////                        SimpleMessageDialog dialog = SimpleMessageDialog.newInstance(
////                                getString(R.string.vague_error),
////                                getString(R.string.reposition_card_not_new_error),
////                                false);
////                        showDialogFragment(dialog);
////                        return false;
////                    }
////                }
////
////                IntegerDialog repositionDialog = new IntegerDialog();
////                repositionDialog.setArgs(
////                        getString(R.string.reposition_card_dialog_title),
////                        getString(R.string.reposition_card_dialog_message),
////                        5);
////                repositionDialog.setCallbackRunnable(days ->
////                        CollectionTask.launchCollectionTask(DISMISS_MULTI, repositionCardHandler(),
////                                new TaskData(new Object[] {cardIds, Collection.DismissType.REPOSITION_CARDS, days}))
////                );
////                showDialogFragment(repositionDialog);
////                return true;
////            }
////            case R.id.action_edit_note: {
////                openNoteEditorForCurrentlySelectedNote();
////            }
//
//            default:
//                return super.onOptionsItemSelected(item);
//
//        }
//    }
//
//
////    @Override
////    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
////        // FIXME:
////        Timber.d("onActivityResult(requestCode=%d, resultCode=%d)", requestCode, resultCode);
////        super.onActivityResult(requestCode, resultCode, data);
////
////        if (resultCode == DeckPicker.RESULT_DB_ERROR) {
////            closeCardsListFragment(DeckPicker.RESULT_DB_ERROR);
////        }
////
////        if (requestCode == EDIT_CARD && resultCode != RESULT_CANCELED) {
////            Timber.i("CardsListFragment:: CardsListFragment: Saving card...");
////            CollectionTask.launchCollectionTask(UPDATE_NOTE, updateCardHandler(),
////                    new TaskData(sCardsListFragmentCard, false));
////        } else if (requestCode == ADD_NOTE && resultCode == RESULT_OK) {
////            if (mSearchView != null) {
////                mSearchTerms = mSearchView.getQuery().toString();
////                searchCards();
////            } else {
////                Timber.w("Note was added from browser and on return mSearchView == null");
////            }
////        }
////
////        // Previewing can now perform an "edit", so it can pass on a reloadRequired
////        if (requestCode == PREVIEW_CARDS && data != null
////                && (data.getBooleanExtra("reloadRequired", false) || data.getBooleanExtra("noteChanged", false))) {
////            searchCards();
////            if (getReviewerCardId() == mCurrentCardId) {
////                mReloadRequired = true;
////            }
////        }
////
////        if (requestCode == EDIT_CARD &&  data != null &&
////                (data.getBooleanExtra("reloadRequired", false) ||
////                        data.getBooleanExtra("noteChanged", false))) {
////            // if reloadRequired or noteChanged flag was sent from note editor then reload card list
////            searchCards();
////            // in use by reviewer?
////            if (getReviewerCardId() == mCurrentCardId) {
////                mReloadRequired = true;
////            }
////        }
////
////        invalidateOptionsMenu();    // maybe the availability of undo changed
////    }
//
//
//
//
//    private long getReviewerCardId() {
//        if (getIntent().hasExtra("currentCard")) {
//            return getIntent().getExtras().getLong("currentCard");
//        } else {
//            return -1;
//        }
//    }
//
//    private void showTagsDialog() {
//        TagsDialog dialog = TagsDialog.newInstance(
//                TagsDialog.TYPE_FILTER_BY_TAG, new ArrayList<String>(), new ArrayList<>(getAnkiActivity().getCol().getTags().all()));
//        dialog.setTagsDialogListener(getAnkiActivity()::filterByTag);
//        showDialogFragment(dialog);
//    }
//
//    /** Selects the given position in the deck list */
//    public void selectDropDownItem(int position) {
//        mActionBarSpinner.setSelection(position);
//        deckDropDownItemChanged(position);
//    }
//
//    /**
//     * Performs changes relating to the Deck DropDown Item changing
//     * Exists as mActionBarSpinner.setSelection() caused a loop in roboelectirc (calling onItemSelected())
//     */
//    private void deckDropDownItemChanged(int position) {
//        if (position == 0) {
//            mRestrictOnDeck = "";
//            saveLastDeckId(ALL_DECKS_ID);
//        } else {
//            Deck deck = mDropDownDecks.get(position - 1);
//            mRestrictOnDeck = "deck:\"" + deck.getString("name") + "\" ";
//            saveLastDeckId(deck.getLong("id"));
//        }
//        searchCards();
//    }
//
//    @Override
//    public void onSaveInstanceState(Bundle savedInstanceState) {
//        // Save current search terms
//        savedInstanceState.putString("mSearchTerms", mSearchTerms);
//        super.onSaveInstanceState(savedInstanceState);
//    }
//
//    @Override
//    public void onRestoreInstanceState(Bundle savedInstanceState) {
//        super.onRestoreInstanceState(savedInstanceState);
//        mSearchTerms = savedInstanceState.getString("mSearchTerms");
//        searchCards();
//    }
//
//    private void invalidate() {
//        CollectionTask.cancelAllTasks(SEARCH_CARDS);
//        CollectionTask.cancelAllTasks(RENDER_BROWSER_QA);
//        CollectionTask.cancelAllTasks(CHECK_CARD_SELECTION);
//        mCards.clear();
//        mCheckedCards.clear();
//    }
//
//    private void searchCards() {
//        // cancel the previous search & render tasks if still running
//        invalidate();
//        String searchText;
//        if (mSearchTerms == null) {
//            mSearchTerms = "";
//        }
//        if (!"".equals(mSearchTerms) && (mSearchView != null)) {
//            mSearchView.setQuery(mSearchTerms, false);
//            mSearchItem.expandActionView();
//        }
//        if (mSearchTerms.contains("deck:")) {
//            searchText = mSearchTerms;
//        } else {
//            searchText = mRestrictOnDeck + mSearchTerms;
//        }
//        if (colIsOpen() && mCardsAdapter!= null) {
//            // clear the existing card list
//            mCards = new ArrayList<>();
//            mCardsAdapter.notifyDataSetChanged();
//            //  estimate maximum number of cards that could be visible (assuming worst-case minimum row height of 20dp)
//            int numCardsToRender = (int) Math.ceil(mCardsListView.getHeight()/
//                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics())) + 5;
//            // Perform database query to get all card ids
//            CollectionTask.launchCollectionTask(SEARCH_CARDS,
//                    mSearchCardsHandler,
//                    new TaskData(new Object[] {
//                            searchText,
//                            ((mOrder != CARD_ORDER_NONE)),
//                            numCardsToRender,
//                            mColumn1Index,
//                            mColumn2Index
//                    })
//            );
//        }
//    }
//
//
//    private void updateList() {
//        mCardsAdapter.notifyDataSetChanged();
//        mDropDownAdapter.notifyDataSetChanged();
//        onSelectionChanged();
//        updatePreviewMenuItem();
//    }
//
//    /**
//     * @return text to be used in the subtitle of the drop-down deck selector
//     */
//    public String getSubtitleText() {
//        int count = getCardCount();
//        return getResources().getQuantityString(R.plurals.card_browser_subtitle, count, count);
//    }
//
//
//    private static Map<Long, Integer> getPositionMap(List<CardCache> list) {
//        Map<Long, Integer> positions = new HashMap<>();
//        for (int i = 0; i < list.size(); i++) {
//            positions.put(list.get(i).getId(), i);
//        }
//        return positions;
//    }
//
//    // Iterates the drop down decks, and selects the one matching the given id
//    private boolean selectDeckById(@NonNull Long deckId) {
//        for (int dropDownDeckIdx = 0; dropDownDeckIdx < mDropDownDecks.size(); dropDownDeckIdx++) {
//            if (mDropDownDecks.get(dropDownDeckIdx).getLong("id") == deckId) {
//                selectDropDownItem(dropDownDeckIdx + 1);
//                return true;
//            }
//        }
//        return false;
//    }
//
//    // convenience method for updateCardsInList(...)
//    private void updateCardInList(Card card, String updatedCardTags){
//        List<Card> cards = new ArrayList<>();
//        cards.add(card);
//        if (updatedCardTags != null) {
//            Map<Long, String> updatedCardTagsMult = new HashMap<>();
//            updatedCardTagsMult.put(card.getNid(), updatedCardTags);
//            updateCardsInList(cards, updatedCardTagsMult);
//        } else {
//            updateCardsInList(cards, null);
//        }
//    }
//
//    /** Returns the decks which are valid targets for "Change Deck" */
//    @VisibleForTesting
//    List<Deck> getValidDecksForChangeDeck() {
//        List<Deck> nonDynamicDecks = new ArrayList<>();
//        for (Deck d : mDropDownDecks) {
//            if (Decks.isDynamic(d)) {
//                continue;
//            }
//            nonDynamicDecks.add(d);
//        }
//        return nonDynamicDecks;
//    }
//
//
//    private void filterByTag(List<String> selectedTags, int option) {
//        //TODO: Duplication between here and CustomStudyDialog:customStudyFromTags
//        mSearchView.setQuery("", false);
//        String tags = selectedTags.toString();
//        mSearchView.setQueryHint(getResources().getString(R.string.card_browser_tags_shown,
//                tags.substring(1, tags.length() - 1)));
//        StringBuilder sb = new StringBuilder();
//        switch (option) {
//            case 1:
//                sb.append("is:new ");
//                break;
//            case 2:
//                sb.append("is:due ");
//                break;
//            default:
//                // Logging here might be appropriate : )
//                break;
//        }
//        int i = 0;
//        for (String tag : selectedTags) {
//            if (i != 0) {
//                sb.append("or ");
//            } else {
//                sb.append("("); // Only if we really have selected tags
//            }
//            // 7070: quote tags so brackets are properly escaped
//            sb.append("tag:").append("'").append(tag).append("'").append(" ");
//            i++;
//        }
//        if (i > 0) {
//            sb.append(")"); // Only if we added anything to the tag list
//        }
//        mSearchTerms = sb.toString();
//        searchCards();
//    }
//
//
//    private static abstract class ListenerWithProgressBar extends TaskListenerWithContext<CardsListFragment>{
//        public ListenerWithProgressBar(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListFragment browser) {
//            browser.showProgressBar();
//        }
//    }
//
//    /** Does not leak Card Browser. */
//    private static abstract class ListenerWithProgressBarCloseOnFalse extends ListenerWithProgressBar {
//        private final String mTimber;
//        public ListenerWithProgressBarCloseOnFalse(String timber, CardsListFragment browser) {
//            super(browser);
//            mTimber = timber;
//        }
//
//        public ListenerWithProgressBarCloseOnFalse(CardsListFragment browser) {
//            getAnkiActivity()(null, browser);
//        }
//
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            if (mTimber != null) {
//                Timber.d(mTimber);
//            }
//            if (result.getBoolean()) {
//                actualOnValidPostExecute(browser, result);
//            } else {
//                browser.closeCardsListFragment(DeckPicker.RESULT_DB_ERROR);
//            }
//        }
//
//        protected abstract void actualOnValidPostExecute(CardsListFragment browser, TaskData result);
//    }
//
//    /**
//     * @param cards Cards that were changed
//     * @param updatedCardTags Mapping note id -> updated tags
//     */
//    private void updateCardsInList(List<Card> cards, Map<Long, String> updatedCardTags) {
//        List<CardCache> cardList = getCards();
//        Map<Long, Integer> idToPos = getPositionMap(cardList);
//        for (Card c : cards) {
//            // get position in the mCards search results HashMap
//            Integer pos = idToPos.get(c.getId());
//            if (pos == null || pos >= getCardCount()) {
//                continue;
//            }
//            // update Q & A etc
//            cardList.get(pos).load(true, mColumn1Index, mColumn2Index);
//        }
//
//        updateList();
//    }
//
//    private UpdateCardHandler updateCardHandler() {
//        return new UpdateCardHandler(getAnkiActivity());
//    }
//
//    private static class UpdateCardHandler extends ListenerWithProgressBarCloseOnFalse {
//        public UpdateCardHandler(CardsListFragment browser) {
//            super("Card Browser - UpdateCardHandler.actualOnPostExecute(CardsListFragment browser)", browser);
//        }
//
//        @Override
//        public void actualOnProgressUpdate(@NonNull CardsListFragment browser, TaskData value) {
//            browser.updateCardInList(value.getCard(), value.getString());
//        }
//
//        @Override
//        protected void actualOnValidPostExecute(CardsListFragment browser, TaskData result) {
//            browser.hideProgressBar();
//        }
//    };
//
//    private  ChangeDeckHandler changeDeckHandler() {
//        return new ChangeDeckHandler(getAnkiActivity());
//    }
//    private static class ChangeDeckHandler extends  ListenerWithProgressBarCloseOnFalse {
//        public ChangeDeckHandler(CardsListFragment browser) {
//            super("Card Browser - changeDeckHandler.actualOnPostExecute(CardsListFragment browser)", browser);
//        }
//
//        @Override
//        protected void actualOnValidPostExecute(CardsListFragment browser, TaskData result) {
//            browser.hideProgressBar();
//
//            browser.searchCards();
//            browser.endMultiSelectMode();
//            browser.mCardsAdapter.notifyDataSetChanged();
//            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
//
//            if (!result.getBoolean()) {
//                Timber.i("changeDeckHandler failed, not offering undo");
//                browser.displayCouldNotChangeDeck();
//                return;
//            }
//            // snackbar to offer undo
//            String deckName = browser.getAnkiActivity().getCol().getDecks().name(browser.mNewDid);
//            browser.mUndoSnackbar = UIUtils.showSnackbar(browser, String.format(browser.getString(R.string.changed_deck_message), deckName), SNACKBAR_DURATION, R.string.undo, new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    CollectionTask.launchCollectionTask(UNDO, browser.mUndoHandler);
//                }
//            }, browser.mCardsListView, null);
//        }
//    };
//
//    @CheckResult
//    private static String formatQA(String text, Context context) {
//        boolean showFilenames = AnkiDroidApp.getSharedPrefs(context).getBoolean("card_browser_show_media_filenames", false);
//        return formatQAInternal(text, showFilenames);
//    }
//
//
//    /**
//     * @param txt The text to strip HTML, comments, tags and media from
//     * @param showFileNames Whether [sound:foo.mp3] should be rendered as " foo.mp3 " or  " "
//     * @return The formatted string
//     */
//    @VisibleForTesting
//    @CheckResult
//    static String formatQAInternal(String txt, boolean showFileNames) {
//        /* Strips all formatting from the string txt for use in displaying question/answer in browser */
//        String s = txt;
//        s = s.replaceAll("<!--.*?-->", "");
//        s = s.replace("<br>", " ");
//        s = s.replace("<br />", " ");
//        s = s.replace("<div>", " ");
//        s = s.replace("\n", " ");
//        s = showFileNames ? Utils.stripSoundMedia(s) : Utils.stripSoundMedia(s, " ");
//        s = s.replaceAll("\\[\\[type:[^]]+\\]\\]", "");
//        s = showFileNames ? Utils.stripHTMLMedia(s) : Utils.stripHTMLMedia(s, " ");
//        s = s.trim();
//        return s;
//    }
//
//    /**
//     * Removes cards from view. Doesn't delete them in model (database).
//     */
//    private void removeNotesView(Card[] cards, boolean reorderCards) {
//        List<Long> cardIds = new ArrayList<>(cards.length);
//        for (Card c : cards) {
//            cardIds.add(c.getId());
//        }
//        removeNotesView(cardIds, reorderCards);
//    }
//
//    /**
//     * Removes cards from view. Doesn't delete them in model (database).
//     * @param reorderCards Whether to rearrange the positions of checked items (DEFECT: Currently deselects all)
//     */
//    private void removeNotesView(java.util.Collection<Long> cardsIds, boolean reorderCards) {
//        long reviewerCardId = getReviewerCardId();
//        List<CardCache> oldMCards = getCards();
//        Map<Long, Integer> idToPos = getPositionMap(oldMCards);
//        Set<Long> idToRemove = new HashSet<Long>();
//        for (Long cardId : cardsIds) {
//            if (cardId == reviewerCardId) {
//                mReloadRequired = true;
//            }
//            if (idToPos.containsKey(cardId)) {
//                idToRemove.add(cardId);
//            }
//        }
//
//        List<CardCache> newMCards = new ArrayList<>();
//        int pos = 0;
//        for (CardCache card: oldMCards) {
//            if (!idToRemove.contains(card.getId())) {
//                newMCards.add(new CardCache(card, pos++));
//            }
//        }
//        mCards = newMCards;
//
//        if (reorderCards) {
//            //Suboptimal from a UX perspective, we should reorder
//            //but getAnkiActivity() is only hit on a rare sad path and we'd need to rejig the data structures to allow an efficient
//            //search
//            Timber.w("Removing current selection due to unexpected removal of cards");
//            onSelectNone();
//        }
//
//        updateList();
//    }
//
//    private SuspendCardHandler suspendCardHandler() {
//        return new SuspendCardHandler(getAnkiActivity());
//    }
//    private static class SuspendCardHandler extends ListenerWithProgressBarCloseOnFalse {
//        public SuspendCardHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        protected void actualOnValidPostExecute(CardsListFragment browser, TaskData result) {
//            Card[] cards = (Card[]) result.getObjArray();
//            browser.updateCardsInList(Arrays.asList(cards), null);
//            browser.hideProgressBar();
//            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
//        }
//    };
//
//    private FlagCardHandler flagCardHandler(){
//        return new FlagCardHandler(getAnkiActivity());
//    }
//    private static class FlagCardHandler extends SuspendCardHandler {public FlagCardHandler(CardsListFragment browser) {super(browser);}};
//
//    private MarkCardHandler markCardHandler() {
//        return new MarkCardHandler(getAnkiActivity());
//    }
//    private static class MarkCardHandler extends ListenerWithProgressBarCloseOnFalse {
//        public MarkCardHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        protected void actualOnValidPostExecute(CardsListFragment browser, TaskData result) {
//            Card[] cards = (Card[]) result.getObjArray();
//            browser.updateCardsInList(CardUtils.getAllCards(CardUtils.getNotes(Arrays.asList(cards))), null);
//            browser.hideProgressBar();
//            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
//        }
//    };
//
//    private DeleteNoteHandler mDeleteNoteHandler = new DeleteNoteHandler(getAnkiActivity());
//    private static class DeleteNoteHandler extends ListenerWithProgressBarCloseOnFalse {
//        public DeleteNoteHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListFragment browser) {
//            super.actualOnPreExecute(browser);
//            browser.invalidate();
//        }
//
//        @Override
//        public void actualOnProgressUpdate(@NonNull CardsListFragment browser, TaskData value) {
//            Card[] cards = (Card[]) value.getObjArray();
//            //we don't need to reorder cards here as we've already deselected all notes,
//            browser.removeNotesView(cards, false);
//        }
//
//
//        @Override
//        protected void actualOnValidPostExecute(CardsListFragment browser, TaskData result) {
//            browser.hideProgressBar();
//            browser.mActionBarTitle.setText(Integer.toString(browser.checkedCardCount()));
//            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
//            // snackbar to offer undo
//            browser.mUndoSnackbar = UIUtils.showSnackbar(browser, browser.getString(R.string.deleted_message), SNACKBAR_DURATION, R.string.undo, new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    CollectionTask.launchCollectionTask(UNDO, browser.mUndoHandler);
//                }
//            }, browser.mCardsListView, null);
//            browser.searchCards();
//        }
//    };
//
//    private final UndoHandler mUndoHandler = new UndoHandler(getAnkiActivity());
//    private static class UndoHandler extends ListenerWithProgressBarCloseOnFalse {
//        public UndoHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnValidPostExecute(CardsListFragment browser, TaskData result) {
//            Timber.d("Card Browser - mUndoHandler.actualOnPostExecute(CardsListFragment browser)");
//            browser.hideProgressBar();
//            // reload whole view
//            browser.searchCards();
//            browser.endMultiSelectMode();
//            browser.mCardsAdapter.notifyDataSetChanged();
//            browser.updatePreviewMenuItem();
//            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
//        }
//    };
//
//    private final SearchCardsHandler mSearchCardsHandler = new SearchCardsHandler(getAnkiActivity());
//    private class SearchCardsHandler extends ListenerWithProgressBar {
//        public SearchCardsHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            if (result != null) {
//                mCards = result.getCards();
//                updateList();
//                handleSearchResult();
//            }
//            updatePreviewMenuItem();
//            hideProgressBar();
//        }
//
//
//        private void handleSearchResult() {
//            Timber.i("CardsListFragment:: Completed doInBackgroundSearchCards Successfully");
//            updateList();
//
//            if ((mSearchView == null) || mSearchView.isIconified()) {
//                return;
//            }
//
//            if (hasSelectedAllDecks()) {
//                UIUtils.showSimpleSnackbar(getAnkiActivity(), getSubtitleText(), true);
//                return;
//            }
//
//            //If we haven't selected all decks, allow the user the option to search all decks.
//            String displayText;
//            if (getCardCount() == 0) {
//                displayText = getString(R.string.card_browser_no_cards_in_deck, getSelectedDeckNameForUi());
//            } else {
//                displayText = getSubtitleText();
//            }
//            View root = getAnkiActivity().findViewById(R.id.root_layout);
//            UIUtils.showSnackbar(getAnkiActivity(),
//                    displayText,
//                    SNACKBAR_DURATION,
//                    R.string.card_browser_search_all_decks,
//                    (v) -> searchAllDecks(),
//                    root,
//                    null);
//
//        }
//
//        @Override
//        public void actualOnCancelled(@NonNull CardsListFragment browser) {
//            super.actualOnCancelled(browser);
//            hideProgressBar();
//        }
//    };
//
//    public boolean hasSelectedAllDecks() {
//        Long lastDeckId = getLastDeckId();
//        return lastDeckId != null && lastDeckId == ALL_DECKS_ID;
//    }
//
//
//    public void searchAllDecks() {
//        //all we need to do is select all decks
//        selectAllDecks();
//    }
//
//    /**
//     * Returns the current deck name, "All Decks" if all decks are selected, or "Unknown"
//     * Do not use getAnkiActivity() for any business logic, as getAnkiActivity() will return inconsistent data
//     * with the collection.
//     */
//    public String getSelectedDeckNameForUi() {
//        try {
//            Long lastDeckId = getLastDeckId();
//            if (lastDeckId == null) {
//                return getString(R.string.card_browser_unknown_deck_name);
//            }
//            if (lastDeckId == ALL_DECKS_ID) {
//                return getString(R.string.card_browser_all_decks);
//            }
//            return getAnkiActivity().getCol().getDecks().name(lastDeckId);
//        } catch (Exception e) {
//            Timber.w(e, "Unable to get selected deck name");
//            return getString(R.string.card_browser_unknown_deck_name);
//        }
//    }
//
//    private final RenderQAHandler mRenderQAHandler = new RenderQAHandler(getAnkiActivity());
//    private static class RenderQAHandler extends TaskListenerWithContext<CardsListFragment>{
//        public RenderQAHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnProgressUpdate(@NonNull CardsListFragment browser, TaskData value) {
//            // Note: getAnkiActivity() is called every time a card is rendered.
//            // It blocks the long-click callback while the task is running, so usage of the task should be minimized
//            browser.mCardsAdapter.notifyDataSetChanged();
//        }
//
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListFragment browser) {
//            Timber.d("Starting Q&A background rendering");
//        }
//
//
//        @Override
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            if (result != null) {
//                if (result.getObjArray() != null && result.getObjArray().length > 1) {
//                    try {
//                        @SuppressWarnings("unchecked")
//                        List<Long> cardsIdsToHide = (List<Long>) result.getObjArray()[1];
//                        if (cardsIdsToHide.size() > 0) {
//                            Timber.i("Removing %d invalid cards from view", cardsIdsToHide.size());
//                            browser.removeNotesView(cardsIdsToHide, true);
//                        }
//                    } catch (Exception e) {
//                        Timber.e(e, "failed to hide cards");
//                    }
//                }
//                browser.hideProgressBar();
//                browser.mCardsAdapter.notifyDataSetChanged();
//                Timber.d("Completed doInBackgroundRenderBrowserQA Successfuly");
//            } else {
//                // Might want to do something more proactive here like show a message box?
//                Timber.e("doInBackgroundRenderBrowserQA was not successful... continuing anyway");
//            }
//        }
//
//
//        @Override
//        public void actualOnCancelled(@NonNull CardsListFragment browser) {
//            browser.hideProgressBar();
//        }
//    };
//
//    private final CheckSelectedCardsHandler mCheckSelectedCardsHandler = new CheckSelectedCardsHandler(getAnkiActivity());
//    private static class CheckSelectedCardsHandler extends ListenerWithProgressBar {
//        public CheckSelectedCardsHandler(CardsListFragment browser) {
//            super(browser);
//        }
//
//        @Override
//        public void actualOnPostExecute(@NonNull CardsListFragment browser, TaskData result) {
//            if (result == null) {
//                return;
//            }
//            browser.hideProgressBar();
//
//            Object[] resultArr = result.getObjArray();
//            boolean hasUnsuspended = (boolean) resultArr[0];
//            boolean hasUnmarked = (boolean) resultArr[1];
//
//            int title;
//            int icon;
//            if (hasUnsuspended) {
//                title = R.string.card_browser_suspend_card;
//                icon = R.drawable.ic_action_suspend;
//            } else {
//                title = R.string.card_browser_unsuspend_card;
//                icon = R.drawable.ic_action_unsuspend;
//            }
//            MenuItem suspend_item = browser.mActionBarMenu.findItem(R.id.action_suspend_card);
//            suspend_item.setTitle(browser.getString(title));
//            suspend_item.setIcon(icon);
//
//            if (hasUnmarked) {
//                title = R.string.card_browser_mark_card;
//                icon = R.drawable.ic_star_outline_white_24dp;
//            } else {
//                title = R.string.card_browser_unmark_card;
//                icon = R.drawable.ic_star_white_24dp;
//            }
//            MenuItem mark_item = browser.mActionBarMenu.findItem(R.id.action_mark_card);
//            mark_item.setTitle(browser.getString(title));
//            mark_item.setIcon(icon);
//        }
//
//
//        @Override
//        public void actualOnCancelled(@NonNull CardsListFragment browser) {
//            super.actualOnCancelled(browser);
//            browser.hideProgressBar();
//        }
//    }
//
//
//    private void closeCardsListFragment(int result) {
//        closeCardsListFragment(result, null);
//    }
//
//    private void closeCardsListFragment(int result, Intent data) {
//        // Set result and finish
//        setResult(result, data);
//        finishWithAnimation(ActivityTransitionAnimation.RIGHT);
//    }
//
//    /**
//     * Render the second column whenever the user stops scrolling
//     */
//    private final class RenderOnScroll implements AbsListView.OnScrollListener {
//        @Override
//        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//            // Show the progress bar if scrolling to given position requires rendering of the question / answer
//            int lastVisibleItem = firstVisibleItem + visibleItemCount;
//            List<CardCache> cards = getCards();
//            // List is never cleared, only reset to a new list. So it's safe here.
//            int size = cards.size();
//            if ((size > 0) && (firstVisibleItem < size) && ((lastVisibleItem - 1) < size)) {
//                boolean firstLoaded = cards.get(firstVisibleItem).isLoaded();
//                // Note: max value of lastVisibleItem is totalItemCount, so need to subtract 1
//                boolean lastLoaded = cards.get(lastVisibleItem - 1).isLoaded();
//                if (!firstLoaded || !lastLoaded) {
//                    showProgressBar();
//                    // Also start rendering the items on the screen every 300ms while scrolling
//                    long currentTime = SystemClock.elapsedRealtime ();
//                    if ((currentTime - mLastRenderStart > 300 || lastVisibleItem >= totalItemCount)) {
//                        mLastRenderStart = currentTime;
//                        CollectionTask.cancelAllTasks(RENDER_BROWSER_QA);
//                        CollectionTask.launchCollectionTask(RENDER_BROWSER_QA, mRenderQAHandler,
//                                new TaskData(new Object[]{cards, firstVisibleItem, visibleItemCount, mColumn1Index, mColumn2Index}));
//                    }
//                }
//            }
//        }
//
//        @Override
//        public void onScrollStateChanged(AbsListView listView, int scrollState) {
//            // TODO: Try change to RecyclerView as currently gets stuck a lot when using scrollbar on right of ListView
//            // Start rendering the question & answer every time the user stops scrolling
//            if (scrollState == SCROLL_STATE_IDLE) {
//                int startIdx = listView.getFirstVisiblePosition();
//                int numVisible = listView.getLastVisiblePosition() - startIdx;
//                CollectionTask.launchCollectionTask(RENDER_BROWSER_QA, mRenderQAHandler,
//                        new TaskData(new Object[]{getCards(), startIdx - 5, 2 * numVisible + 5, mColumn1Index, mColumn2Index}));
//            }
//        }
//    }
//
//    private final class MultiColumnListAdapter extends BaseAdapter {
//        private final int mResource;
//        private Column[] mFromKeys;
//        private final int[] mToIds;
//        private float mOriginalTextSize = -1.0f;
//        private final int mFontSizeScalePcent;
//        private Typeface mCustomTypeface = null;
//        private LayoutInflater mInflater;
//
//        public MultiColumnListAdapter(Context context, int resource, Column[] from, int[] to,
//                                      int fontSizeScalePcent, String customFont) {
//            mResource = resource;
//            mFromKeys = from;
//            mToIds = to;
//            mFontSizeScalePcent = fontSizeScalePcent;
//            if (!"".equals(customFont)) {
//                mCustomTypeface = AnkiFont.getTypeface(context, customFont);
//            }
//            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        }
//
//
//        public View getView(int position, View convertView, ViewGroup parent) {
//            // Get the main container view if it doesn't already exist, and call bindView
//            View v;
//            if (convertView == null) {
//                v = mInflater.inflate(mResource, parent, false);
//                final int count = mToIds.length;
//                final View[] columns = new View[count];
//                for (int i = 0; i < count; i++) {
//                    columns[i] = v.findViewById(mToIds[i]);
//                }
//                v.setTag(columns);
//            } else {
//                v = convertView;
//            }
//            bindView(position, v);
//            return v;
//        }
//
//
//        private void bindView(final int position, final View v) {
//            // Draw the content in the columns
//            View[] columns = (View[]) v.getTag();
//            final CardCache card = getCards().get(position);
//            for (int i = 0; i < mToIds.length; i++) {
//                TextView col = (TextView) columns[i];
//                // set font for column
//                setFont(col);
//                // set text for column
//                col.setText(card.getColumnHeaderText(mFromKeys[i]));
//            }
//            // set card's background color
//            final int backgroundColor = Themes.getColorFromAttr(getAnkiActivity(), card.getColor());
//            v.setBackgroundColor(backgroundColor);
//            // setup checkbox to change color in multi-select mode
//            final CheckBox checkBox = (CheckBox) v.findViewById(R.id.card_checkbox);
//            // if in multi-select mode, be sure to show the checkboxes
//            if(mInMultiSelectMode) {
//                checkBox.setVisibility(View.VISIBLE);
//                if (mCheckedCards.contains(card)) {
//                    checkBox.setChecked(true);
//                } else {
//                    checkBox.setChecked(false);
//                }
//                // getAnkiActivity() prevents checkboxes from showing an animation from selected -> unselected when
//                // checkbox was selected, then selection mode was ended and now restarted
//                checkBox.jumpDrawablesToCurrentState();
//            } else {
//                checkBox.setChecked(false);
//                checkBox.setVisibility(View.GONE);
//            }
//            // change bg color on check changed
//            checkBox.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    onCheck(position, v);
//                }
//            });
//        }
//
//        private void setFont(TextView v) {
//            // Set the font and font size for a TextView v
//            float currentSize = v.getTextSize();
//            if (mOriginalTextSize < 0) {
//                mOriginalTextSize = v.getTextSize();
//            }
//            // do nothing when pref is 100% and apply scaling only once
//            if (mFontSizeScalePcent != 100 && Math.abs(mOriginalTextSize - currentSize) < 0.1) {
//                // getTextSize returns value in absolute PX so use that in the setter
//                v.setTextSize(TypedValue.COMPLEX_UNIT_PX, mOriginalTextSize * (mFontSizeScalePcent / 100.0f));
//            }
//
//            if (mCustomTypeface != null) {
//                v.setTypeface(mCustomTypeface);
//            }
//        }
//
//        public void setFromMapping(Column[] from) {
//            mFromKeys = from;
//            notifyDataSetChanged();
//        }
//
//
//        public Column[] getFromMapping() {
//            return mFromKeys;
//        }
//
//
//        @Override
//        public int getCount() {
//            return getCardCount();
//        }
//
//
//        @Override
//        public Object getItem(int position) {
//            return getCards().get(position);
//        }
//
//
//        @Override
//        public long getItemId(int position) {
//            return position;
//        }
//
//    }
//
//
//    private void onCheck(int position, View cell) {
//        CheckBox checkBox = (CheckBox) cell.findViewById(R.id.card_checkbox);
//        CardCache card = getCards().get(position);
//
//        if (checkBox.isChecked()) {
//            mCheckedCards.add(card);
//        } else {
//            mCheckedCards.remove(card);
//        }
//
//        onSelectionChanged();
//    }
//
//    private void onSelectAll() {
//        mCheckedCards.addAll(mCards);
//        onSelectionChanged();
//    }
//
//    private void onSelectNone() {
//        mCheckedCards.clear();
//        onSelectionChanged();
//    }
//
//    private void onSelectionChanged() {
//        Timber.d("onSelectionChanged()");
//        try {
//            if (!mInMultiSelectMode && !mCheckedCards.isEmpty()) {
//                //If we have selected cards, load multiselect
//                loadMultiSelectMode();
//            } else if (mInMultiSelectMode && mCheckedCards.isEmpty()) {
//                //If we don't have cards, unload multiselect
//                endMultiSelectMode();
//            }
//
//            //If we're not in mutliselect, we can select cards if there are cards to select
//            if (!mInMultiSelectMode && getAnkiActivity().mActionBarMenu != null) {
//                MenuItem selectAll = mActionBarMenu.findItem(R.id.action_select_all);
//                selectAll.setVisible(mCards != null && cardCount() != 0);
//            }
//
//            if (!mInMultiSelectMode) {
//                return;
//            }
//
//            updateMultiselectMenu();
//            mActionBarTitle.setText(Integer.toString(checkedCardCount()));
//        } finally {
//            mCardsAdapter.notifyDataSetChanged();
//        }
//    }
//
//    private List<CardCache> getCards() {
//        if (mCards == null) {
//            mCards = new ArrayList<>();
//        }
//        return mCards;
//    }
//
//    private long[] getAllCardIds() {
//        long[] l = new long[mCards.size()];
//        for (int i = 0; i < mCards.size(); i++) {
//            l[i] = mCards.get(i).getId();
//        }
//        return l;
//    }
//
//    public static class CardCache extends Card.Cache {
//        private boolean mLoaded = false;
//        private Pair<String, String> mQa = null;
//        private int mPosition;
//
//        public CardCache(long id, Collection col, int position) {
//            super(col, id);
//            mPosition = position;
//        }
//
//        protected CardCache(CardCache cache, int position) {
//            super(cache);
//            mLoaded = cache.mLoaded;
//            mQa = cache.mQa;
//            mPosition = position;
//        }
//
//        public int getPosition() {
//            return mPosition;
//        }
//
//        /** clear all values except ID.*/
//        public void reload() {
//            super.reload();
//            mLoaded = false;
//            mQa = null;
//        }
//
//        /**
//         * Get the background color of items in the card list based on the Card
//         * @return index into TypedArray specifying the background color
//         */
//        public int getColor() {
//            int flag = getCard().userFlag();
//            switch (flag) {
//                case 1:
//                    return R.attr.flagRed;
//                case 2:
//                    return R.attr.flagOrange;
//                case 3:
//                    return R.attr.flagGreen;
//                case 4:
//                    return R.attr.flagBlue;
//                default:
//                    if (getCard().note().hasTag("marked")) {
//                        return R.attr.markedColor;
//                    } else {
//                        if (getCard().getQueue() == Consts.QUEUE_TYPE_SUSPENDED) {
//                            return R.attr.suspendedColor;
//                        } else {
//                            return android.R.attr.colorBackground;
//                        }
//                    }
//            }
//        }
//
//        public String getColumnHeaderText(Column key) {
//            switch (key) {
//                case FLAGS:
//                    return (new Integer(getCard().userFlag())).toString();
//                case SUSPENDED:
//                    return getCard().getQueue() == Consts.QUEUE_TYPE_SUSPENDED ? "True": "False";
//                case MARKED:
//                    return getCard().note().hasTag("marked") ? "marked" : null;
//                case SFLD:
//                    return getCard().note().getSFld();
//                case DECK:
//                    return getAnkiActivity().getCol().getDecks().name(getCard().getDid());
//                case TAGS:
//                    return getCard().note().stringTags();
//                case CARD:
//                    return getCard().template().optString("name");
//                case DUE:
//                    return getCard().getDueString();
//                case EASE:
//                    if (getCard().getType() == Consts.CARD_TYPE_NEW) {
//                        return AnkiDroidApp.getInstance().getString(R.string.card_browser_ease_new_card);
//                    } else {
//                        return (getCard().getFactor()/10)+"%";
//                    }
//                case CHANGED:
//                    return LanguageUtil.getShortDateFormatFromS(getCard().getMod());
//                case CREATED:
//                    return LanguageUtil.getShortDateFormatFromMs(getCard().note().getId());
//                case EDITED:
//                    return LanguageUtil.getShortDateFormatFromS(getCard().note().getMod());
//                case INTERVAL:
//                    switch (getCard().getType()) {
//                        case Consts.CARD_TYPE_NEW:
//                            return AnkiDroidApp.getInstance().getString(R.string.card_browser_interval_new_card);
//                        case Consts.CARD_TYPE_LRN :
//                            return AnkiDroidApp.getInstance().getString(R.string.card_browser_interval_learning_card);
//                        default:
//                            return Utils.roundedTimeSpanUnformatted(AnkiDroidApp.getInstance(), getCard().getIvl()*SECONDS_PER_DAY);
//                    }
//                case LAPSES:
//                    return Integer.toString(getCard().getLapses());
//                case NOTE_TYPE:
//                    return getCard().model().optString("name");
//                case REVIEWS:
//                    return Integer.toString(getCard().getReps());
//                case QUESTION:
//                    updateSearchItemQA();
//                    return mQa.first;
//                case ANSWER:
//                    updateSearchItemQA();
//                    return mQa.second;
//                default:
//                    return null;
//            }
//        }
//
//        /** pre compute the note and question/answer.  It can safely
//         be called twice without doing extra work. */
//        public void load(boolean reload, int column1Index, int column2Index) {
//            if (reload) {
//                reload();
//            }
//            getCard().note();
//            if (
//                    COLUMN1_KEYS[column1Index] == QUESTION ||
//                            COLUMN2_KEYS[column2Index] == QUESTION ||
//                            COLUMN2_KEYS[column2Index] == ANSWER
//                // First column can not be the answer. If it were to
//                // change, getAnkiActivity() code should also be changed.
//            ) {
//                updateSearchItemQA();
//            }
//            mLoaded = true;
//        }
//
//        public boolean isLoaded() {
//            return mLoaded;
//        }
//
//        /**
//         Reload question and answer. Use browser format. If it's empty
//         uses non-browser format. If answer starts by question, remove
//         question.
//         */
//        public void updateSearchItemQA() {
//            if (mQa != null) {
//                return;
//            }
//            // render question and answer
//            Map<String, String> qa = getCard()._getQA(true, true);
//            // Render full question / answer if the bafmt (i.e. "browser appearance") setting forced blank result
//            if ("".equals(qa.get("q")) || "".equals(qa.get("a"))) {
//                HashMap<String, String> qaFull = getCard()._getQA(true, false);
//                if ("".equals(qa.get("q"))) {
//                    qa.put("q", qaFull.get("q"));
//                }
//                if ("".equals(qa.get("a"))) {
//                    qa.put("a", qaFull.get("a"));
//                }
//            }
//            // update the original hash map to include rendered question & answer
//            String q = qa.get("q");
//            String a = qa.get("a");
//            // remove the question from the start of the answer if it exists
//            if (a.startsWith(q)) {
//                a = a.replaceFirst(Pattern.quote(q), "");
//            }
//            a = formatQA(a, AnkiDroidApp.getInstance());
//            q = formatQA(q, AnkiDroidApp.getInstance());
//            mQa = new Pair<>(q, a);
//        }
//
//        @Override
//        public boolean equals(Object obj) {
//            if (getAnkiActivity() == obj) {
//                return true;
//            }
//            if (obj == null) {
//                return false;
//            }
//            if (getClass() != obj.getClass()) {
//                return false;
//            }
//            return getId() == ((CardCache) obj).getId();
//        }
//
//        @Override
//        public int hashCode() {
//            return new Long(getId()).hashCode();
//        }
//    }
//
//    /**
//     * Show/dismiss dialog when sd card is ejected/remounted (collection is saved by SdCardReceiver)
//     */
//    private void registerExternalStorageListener() {
//        if (mUnmountReceiver == null) {
//            mUnmountReceiver = new BroadcastReceiver() {
//                @Override
//                public void onReceive(Context context, Intent intent) {
//                    if (intent.getAction().equals(SdCardReceiver.MEDIA_EJECT)) {
//                        finishWithoutAnimation();
//                    }
//                }
//            };
//            IntentFilter iFilter = new IntentFilter();
//            iFilter.addAction(SdCardReceiver.MEDIA_EJECT);
//            registerReceiver(mUnmountReceiver, iFilter);
//        }
//    }
//
//    /**
//     * The views expand / contract when switching between multi-select mode so we manually
//     * adjust so that the vertical position of the given view is maintained
//     */
//    private void recenterListView(@NonNull View view) {
//        final int position = mCardsListView.getPositionForView(view);
//        // Get the current vertical position of the top of the selected view
//        final int top = view.getTop();
//        final Handler handler = new Handler();
//        // Post to event queue with some delay to give time for the UI to update the layout
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                // Scroll to the same vertical position before the layout was changed
//                mCardsListView.setSelectionFromTop(position, top);
//            }
//        }, 10);
//    }
//
//    /**
//     * Turn on Multi-Select Mode so that the user can select multiple cards at once.
//     */
//    private void loadMultiSelectMode() {
//        if (mInMultiSelectMode) {
//            return;
//        }
//        Timber.d("loadMultiSelectMode()");
//        // set in multi-select mode
//        mInMultiSelectMode = true;
//        // show title and hide spinner
//        mActionBarTitle.setVisibility(View.VISIBLE);
//        mActionBarTitle.setText(String.valueOf(checkedCardCount()));
//        mActionBarSpinner.setVisibility(View.GONE);
//        // reload the actionbar using the multi-select mode actionbar
//        supportInvalidateOptionsMenu();
//    }
//
//    /**
//     * Turn off Multi-Select Mode and return to normal state
//     */
//    private void endMultiSelectMode() {
//        Timber.d("endMultiSelectMode()");
//        mCheckedCards.clear();
//        mInMultiSelectMode = false;
//        // If view which was originally selected when entering multi-select is visible then maintain its position
//        View view = mCardsListView.getChildAt(mLastSelectedPosition - mCardsListView.getFirstVisiblePosition());
//        if (view != null) {
//            recenterListView(view);
//        }
//        // update adapter to remove check boxes
//        mCardsAdapter.notifyDataSetChanged();
//        // update action bar
//        supportInvalidateOptionsMenu();
//        mActionBarSpinner.setVisibility(View.VISIBLE);
//        mActionBarTitle.setVisibility(View.GONE);
//    }
//
//    @VisibleForTesting
//    public int checkedCardCount() {
//        return mCheckedCards.size();
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    boolean isInMultiSelectMode() {
//        return mInMultiSelectMode;
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
//    long cardCount() {
//        return mCards.size();
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    boolean isShowingSelectAll() {
//        return mActionBarMenu != null && mActionBarMenu.findItem(R.id.action_select_all).isVisible();
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    boolean isShowingSelectNone() {
//        return mActionBarMenu != null &&
//                mActionBarMenu.findItem(R.id.action_select_none) != null && //
//                mActionBarMenu.findItem(R.id.action_select_none).isVisible();
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    void clearCardData(int position) {
//        mCards.get(position).reload();
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    void rerenderAllCards() {
//        CollectionTask.launchCollectionTask(RENDER_BROWSER_QA, mRenderQAHandler,
//                new TaskData(new Object[]{getCards(), 0, mCards.size()-1, mColumn1Index, mColumn2Index}));
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    long[] getCardIds() {
//        @SuppressWarnings("unchecked")
//        CardCache[] cardsCopy = mCards.toArray(new CardCache[0]);
//        long[] ret = new long[cardsCopy.length];
//        for (int i = 0; i < cardsCopy.length; i++) {
//            ret[i] = cardsCopy[i].getId();
//        }
//        return ret;
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    void checkedCardsAtPositions(int[] positions) {
//        for (int position : positions) {
//            if (position >= mCards.size()) {
//                throw new IllegalStateException(
//                        String.format(Locale.US, "Attempted to check card at index %d. %d cards available",
//                                position, mCards.size()));
//            }
//            mCheckedCards.add(getCards().get(position));
//        }
//        onSelectionChanged();
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    boolean hasCheckedCardAtPosition(int i) {
//        return mCheckedCards.contains(getCards().get(i));
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    public int getChangeDeckPositionFromId(long deckId) {
//        List<Deck> decks = getValidDecksForChangeDeck();
//        for (int i = 0; i < decks.size(); i++) {
//            Deck deck = decks.get(i);
//            if (deck.getLong("id") == deckId) {
//                return i;
//            }
//        }
//        throw new IllegalStateException(String.format(Locale.US, "Deck %d not found", deckId));
//    }
//
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    public List<Long> getCheckedCardIds() {
//        List<Long> cardIds = new ArrayList<>();
//        for (CardCache card : mCheckedCards) {
//            long id = card.getId();
//            cardIds.add(Objects.requireNonNull(id));
//        }
//        return cardIds;
//    }
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE) //should only be called from changeDeck()
//    void executeChangeCollectionTask(long[] ids, long newDid) {
//        mNewDid = newDid; //line required for unit tests, not necessary, but a noop in regular call.
//        CollectionTask.launchCollectionTask(DISMISS_MULTI, new ChangeDeckHandler(getAnkiActivity()),
//                new TaskData(new Object[]{ids, Collection.DismissType.CHANGE_DECK_MULTI, newDid}));
//    }
//
//
//    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
//    public CardCache getPropertiesForCardId(long cardId) {
//        for (CardCache props : mCards) {
//            long id = Objects.requireNonNull(props.getId());
//            if (id == cardId) {
//                return props;
//            }
//        }
//        throw new IllegalStateException(String.format(Locale.US, "Card '%d' not found", cardId));
//    }
//
//
//    @VisibleForTesting
//    void filterByTag(String... tags) {
//        filterByTag(Arrays.asList(tags), 0);
//    }
//
//}
//
//
//
//class CardsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//    private AnkiActivity mContext;
//    private LayoutInflater mLayoutInflater;
//
//
//    CardsAdapter(LayoutInflater layoutInflater, AnkiActivity context
//
//    ) {
//        mLayoutInflater = layoutInflater;
//        mContext = context;
//    }
//
//
//    @NonNull
//    @Override
//    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        return new DeckInfoListAdapter.HeaderViewHolder(mLayoutInflater.inflate(R.layout.deck_item_self_study, parent, false));
//    }
//
//
//    class ViewHolder extends RecyclerView.ViewHolder {
//        public RelativeLayout deckLayout;
//        public LinearLayout countsLayout;
//        public ImageButton deckExpander;
//        public ImageButton indentView;
//        public TextView deckName;
//        public TextView deckNew, deckLearn, deckRev;
//        public ImageView endIcon;
//
//
//        public ViewHolder(View v) {
//            super(v);
//            deckLayout = (RelativeLayout) v.findViewById(R.id.DeckPickerHoriz);
//            countsLayout = (LinearLayout) v.findViewById(R.id.counts_layout);
//            deckExpander = (ImageButton) v.findViewById(R.id.deckpicker_expander);
//            indentView = (ImageButton) v.findViewById(R.id.deckpicker_indent);
//            deckName = (TextView) v.findViewById(R.id.deckpicker_name);
//            deckNew = (TextView) v.findViewById(R.id.deckpicker_new);
//            deckLearn = (TextView) v.findViewById(R.id.deckpicker_lrn);
//            deckRev = (TextView) v.findViewById(R.id.deckpicker_rev);
//            endIcon = (ImageView) v.findViewById(R.id.end_icon);
//        }
//    }
//
//
//    @Override
//    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
//
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return 0;
//    }
//}