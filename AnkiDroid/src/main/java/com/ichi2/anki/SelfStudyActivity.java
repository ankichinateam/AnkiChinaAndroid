package com.ichi2.anki;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.CardBrowserMySearchesDialog;
import com.ichi2.anki.dialogs.CardBrowserOrderDialog;
import com.ichi2.anki.dialogs.ConfirmationDialog;
import com.ichi2.anki.dialogs.IntegerDialog;
import com.ichi2.anki.dialogs.RescheduleDialog;
import com.ichi2.anki.dialogs.SimpleMessageDialog;
import com.ichi2.anki.dialogs.TagsDialog;
import com.ichi2.anki.receiver.SdCardReceiver;
import com.ichi2.anki.widgets.CardsListAdapter;
import com.ichi2.anki.widgets.DeckDropDownAdapter;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.stats.Stats;
import com.ichi2.themes.Themes;
import com.ichi2.ui.CustomStyleDialog;
import com.ichi2.ui.KeyBoardListenerLayout;
import com.ichi2.ui.WarpLinearLayout;
import com.ichi2.upgrade.Upgrade;
import com.ichi2.utils.FunctionalInterfaces;
import com.ichi2.utils.JSONException;
import com.ichi2.utils.JSONObject;
import com.ichi2.utils.OKHttpUtil;
import com.ichi2.utils.Permissions;
import com.ichi2.widget.WidgetStatus;
import com.jaygoo.widget.OnRangeChangedListener;
import com.jaygoo.widget.RangeSeekBar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import okhttp3.Call;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.anki.CardBrowser.CardCache;
import static com.ichi2.anki.CardBrowser.Column;
import static com.ichi2.anki.CardBrowser.Column.ANSWER;
import static com.ichi2.anki.CardBrowser.Column.CARD;
import static com.ichi2.anki.CardBrowser.Column.CHANGED;
import static com.ichi2.anki.CardBrowser.Column.CREATED;
import static com.ichi2.anki.CardBrowser.Column.DECK;
import static com.ichi2.anki.CardBrowser.Column.DUE;
import static com.ichi2.anki.CardBrowser.Column.EASE;
import static com.ichi2.anki.CardBrowser.Column.EDITED;
import static com.ichi2.anki.CardBrowser.Column.INTERVAL;
import static com.ichi2.anki.CardBrowser.Column.LAPSES;
import static com.ichi2.anki.CardBrowser.Column.NOTE_TYPE;
import static com.ichi2.anki.CardBrowser.Column.QUESTION;
import static com.ichi2.anki.CardBrowser.Column.REVIEWS;
import static com.ichi2.anki.CardBrowser.Column.SFLD;
import static com.ichi2.anki.CardBrowser.Column.TAGS;
import static com.ichi2.anki.CardBrowser.sCardBrowserCard;
import static com.ichi2.anki.DeckPicker.BE_VIP;
import static com.ichi2.async.CollectionTask.TASK_TYPE.CHECK_CARD_SELECTION;
import static com.ichi2.async.CollectionTask.TASK_TYPE.DISMISS_MULTI;
import static com.ichi2.async.CollectionTask.TASK_TYPE.RENDER_BROWSER_QA;
import static com.ichi2.async.CollectionTask.TASK_TYPE.SEARCH_CARDS;
import static com.ichi2.async.CollectionTask.TASK_TYPE.UNDO;
import static com.ichi2.async.CollectionTask.TASK_TYPE.UPDATE_NOTE;

public class SelfStudyActivity extends AnkiActivity implements
        DeckDropDownAdapter.SubtitleListener {

    public static final int TAB_STUDY_STATE = 0;
    public static final int TAB_MARK_STATE = 1;
    public static final int TAB_ANSWER_STATE = 2;
    public static final int TAB_CUSTOM_STATE = 3;
    public static final int TAB_MAIN_STATE = 4;
    /**
     * List of cards in the browser.
     * When the list is changed, the position member of its elements should get changed.
     */
    @NonNull
    private List<CardCache> mCards = new ArrayList<>();
    private ArrayList<Deck> mDropDownDecks;

    private RecyclerView mCardsListView;
    private SearchView mSearchView;
    private CardsListAdapter mCardsAdapter;
    private String mSearchTerms;
    private String mRestrictOnDeck;
    private String mRestrictOnTab;


    private Snackbar mUndoSnackbar;

//    public static Card sCardBrowserCard;

    // card that was clicked (not marked)
    private long mCurrentCardId;

    private int mOrder;
    private boolean mOrderAsc;
    private int mColumn1Index;
    private int mColumn2Index;

    //DEFECT: Doesn't need to be a local
    private long mNewDid;   // for change_deck

    private static final int EDIT_CARD = 0;
    private static final int ADD_NOTE = 1;
    public static final int PREVIEW_CARDS = 2;

    private static final int DEFAULT_FONT_SIZE_RATIO = 100;
    // Should match order of R.array.card_browser_order_labels
    public static final int CARD_ORDER_NONE = 0;
    public static final int CARD_ORDER_CREATE_TIME = 2;
    private static final String[] fSortTypes = new String[] {
            "",
            "noteFld",
            "noteCrt",
            "noteMod",
            "cardMod",
            "cardDue",
            "cardIvl",
            "cardEase",
            "cardReps",
            "cardLapses"};
    private static final Column[] COLUMN1_KEYS = {QUESTION, SFLD};

    // list of available keys in mCards corresponding to the column names in R.array.browser_column2_headings.
    // Note: the last 6 are currently hidden
    private static final Column[] COLUMN2_KEYS = {ANSWER,
            CARD,
            DECK,
            NOTE_TYPE,
            QUESTION,
            TAGS,
            LAPSES,
            REVIEWS,
            INTERVAL,
            EASE,
            DUE,
            CHANGED,
            CREATED,
            EDITED,
    };
    private long mLastRenderStart = 0;
    private DeckDropDownAdapter mDropDownDeckAdapter;
    //    private Spinner mActionBarSpinner;
    private TextView mActionBarTitle;
    private TextView mComplete;
    private ImageView  mBack;
    private TabLayout mTabLayout;
    private boolean mReloadRequired = false;
    //    private boolean inMultiSelectMode() = false;
//    private Set<CardCache> mCheckedCards = Collections.synchronizedSet(new LinkedHashSet<>());
    private int mLastSelectedPosition;
    @Nullable
    private Menu mActionBarMenu;

    private static final int SNACKBAR_DURATION = 8000;


    // Values related to persistent state data
    public static final long ALL_DECKS_ID = 0L;
    private static String PERSISTENT_STATE_FILE = "DeckPickerState";
    private static String LAST_DECK_ID_KEY = "lastDeckId";


    /**
     * Broadcast that informs us when the sd card is about to be unmounted
     */
    private BroadcastReceiver mUnmountReceiver = null;

    private MaterialDialog.ListCallbackSingleChoice mOrderDialogListener =
            new MaterialDialog.ListCallbackSingleChoice() {
                @Override
                public boolean onSelection(MaterialDialog materialDialog, View view, int which,
                                           CharSequence charSequence) {
                    if (which != mOrder) {
                        mOrder = which;
                        mOrderAsc = false;
                        if (mOrder == 0) {
                            getCol().getConf().put("sortType", fSortTypes[1]);
                            AnkiDroidApp.getSharedPrefs(getBaseContext()).edit()
                                    .putBoolean("cardBrowserNoSorting", true)
                                    .apply();
                        } else {
                            getCol().getConf().put("sortType", fSortTypes[mOrder]);
                            AnkiDroidApp.getSharedPrefs(getBaseContext()).edit()
                                    .putBoolean("cardBrowserNoSorting", false)
                                    .apply();
                        }
                        getCol().getConf().put("sortBackwards", mOrderAsc);
                        searchCards();
                    } else if (which != CARD_ORDER_NONE) {
                        mOrderAsc = !mOrderAsc;
                        getCol().getConf().put("sortBackwards", mOrderAsc);
                        Collections.reverse(mCards);
                        updateList();
                    }
                    return true;
                }
            };


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    private RepositionCardHandler repositionCardHandler() {
        return new RepositionCardHandler(this);
    }


    private static class RepositionCardHandler extends TaskListenerWithContext<SelfStudyActivity> {
        public RepositionCardHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull SelfStudyActivity browser) {
            Timber.d("CardBrowser::RepositionCardHandler() onPreExecute");
        }


        @Override
        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            Timber.d("CardBrowser::RepositionCardHandler() onPostExecute");
            browser.mReloadRequired = true;
            int cardCount = result.getObjArray().length;
            UIUtils.showThemedToast(browser,
                    browser.getResources().getQuantityString(R.plurals.reposition_card_dialog_acknowledge, cardCount, cardCount), true);
        }
    }


    private ResetProgressCardHandler resetProgressCardHandler() {
        return new ResetProgressCardHandler(this);
    }


    private static class ResetProgressCardHandler extends TaskListenerWithContext<SelfStudyActivity> {
        public ResetProgressCardHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull SelfStudyActivity browser) {
            Timber.d("CardBrowser::ResetProgressCardHandler() onPreExecute");
        }


        @Override
        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            Timber.d("CardBrowser::ResetProgressCardHandler() onPostExecute");
            browser.mReloadRequired = true;
            int cardCount = result.getObjArray().length;
            UIUtils.showThemedToast(browser,
                    browser.getResources().getQuantityString(R.plurals.reset_cards_dialog_acknowledge, cardCount, cardCount), true);
        }
    }


    private RescheduleCardHandler rescheduleCardHandler() {
        return new RescheduleCardHandler(this);
    }


    private static class RescheduleCardHandler extends TaskListenerWithContext<SelfStudyActivity> {
        public RescheduleCardHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull SelfStudyActivity browser) {
            Timber.d("CardBrowser::RescheduleCardHandler() onPreExecute");
        }


        @Override
        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            Timber.d("CardBrowser::RescheduleCardHandler() onPostExecute");
            browser.mReloadRequired = true;
            int cardCount = result.getObjArray().length;
            UIUtils.showThemedToast(browser,
                    browser.getResources().getQuantityString(R.plurals.reschedule_cards_dialog_acknowledge, cardCount, cardCount), true);
        }
    }



    private CardBrowserMySearchesDialog.MySearchesDialogListener mMySearchesDialogListener =
            new CardBrowserMySearchesDialog.MySearchesDialogListener() {
                @Override
                public void onSelection(String searchName) {
                    Timber.d("OnSelection using search named: %s", searchName);
                    JSONObject savedFiltersObj = getCol().getConf().optJSONObject("savedFilters");
                    Timber.d("SavedFilters are %s", savedFiltersObj.toString());
                    if (savedFiltersObj != null) {
                        mSearchTerms = savedFiltersObj.optString(searchName);

                        Timber.d("OnSelection using search terms: %s", mSearchTerms);
                        mSearchView.setQuery(mSearchTerms, false);
                        searchCards();
                    }
                }


                @Override
                public void onRemoveSearch(String searchName) {
                    Timber.d("OnRemoveSelection using search named: %s", searchName);
                    JSONObject savedFiltersObj = getCol().getConf().optJSONObject("savedFilters");
                    if (savedFiltersObj != null && savedFiltersObj.has(searchName)) {
                        savedFiltersObj.remove(searchName);
                        getCol().getConf().put("savedFilters", savedFiltersObj);
                        getCol().flush();

                    }

                }


                @Override
                public void onSaveSearch(String searchName, String searchTerms) {
                    if (TextUtils.isEmpty(searchName)) {
                        UIUtils.showThemedToast(SelfStudyActivity.this,
                                getString(R.string.card_browser_list_my_searches_new_search_error_empty_name), true);
                        return;
                    }
                    JSONObject savedFiltersObj = getCol().getConf().optJSONObject("savedFilters");
                    boolean should_save = false;
                    if (savedFiltersObj == null) {
                        savedFiltersObj = new JSONObject();
                        savedFiltersObj.put(searchName, searchTerms);
                        should_save = true;
                    } else if (!savedFiltersObj.has(searchName)) {
                        savedFiltersObj.put(searchName, searchTerms);
                        should_save = true;
                    } else {
                        UIUtils.showThemedToast(SelfStudyActivity.this,
                                getString(R.string.card_browser_list_my_searches_new_search_error_dup), true);
                    }
                    if (should_save) {
                        getCol().getConf().put("savedFilters", savedFiltersObj);
                        getCol().flush();
                        mSearchView.setQuery("", false);
                    }
                }
            };


    private void onSearch() {
        mSearchTerms = mSearchView.getQuery().toString();
        if (mSearchTerms.length() == 0) {
            mSearchView.setQueryHint(getResources().getString(R.string.downloaddeck_search));
        }
        searchCards();
    }


    private long[] getSelectedCardIds() {
        return mCardsAdapter != null ? mCardsAdapter.getSelectedItemIdArray() : new long[] {};
    }
    private Set<CardBrowser.CardCache> getSelectedCards() {
        return mCardsAdapter != null ? mCardsAdapter.getSelectedCards() : null;
    }

    private boolean canPerformMultiSelectEditNote() {
        //The noteId is not currently available. Only allow if a single card is selected for now.
        return checkedCardCount() == 1;
    }


    @VisibleForTesting
    void changeDeck(int deckPosition) {
        long[] ids = getSelectedCardIds();

        Deck selectedDeck = getValidDecksForChangeDeck().get(deckPosition);

        try {
            //#5932 - can't be dynamic
            if (Decks.isDynamic(selectedDeck)) {
                Timber.w("Attempted to change cards to dynamic deck. Cancelling operation.");
                displayCouldNotChangeDeck();
                return;
            }
        } catch (Exception e) {
            displayCouldNotChangeDeck();
            Timber.e(e);
            return;
        }

        mNewDid = selectedDeck.getLong("id");

        Timber.i("Changing selected cards to deck: %d", mNewDid);

        if (ids.length == 0) {
//            endMultiSelectMode();
            mCardsAdapter.notifyDataSetChanged();
            return;
        }

        if (CardUtils.isIn(ids, getReviewerCardId())) {
            mReloadRequired = true;
        }

        executeChangeCollectionTask(ids, mNewDid);
    }


    private void displayCouldNotChangeDeck() {
        UIUtils.showThemedToast(this, getString(R.string.card_browser_deck_change_error), true);
    }


    private Long getLastDeckId() {
        SharedPreferences state = getSharedPreferences(PERSISTENT_STATE_FILE, 0);
        if (!state.contains(LAST_DECK_ID_KEY)) {
            return null;
        }
        return state.getLong(LAST_DECK_ID_KEY, -1);
    }


    public static void clearLastDeckId() {
        Context context = AnkiDroidApp.getInstance();
        context.getSharedPreferences(PERSISTENT_STATE_FILE, 0).edit().remove(LAST_DECK_ID_KEY).apply();
    }


    public static  void saveLastDeckId(Long id) {
        if (id == null) {
            clearLastDeckId();
            return;
        }
        AnkiDroidApp.getInstance().getSharedPreferences(PERSISTENT_STATE_FILE, 0).edit().putLong(LAST_DECK_ID_KEY, id).apply();
    }


    String[] mArrayStudy;
    String[] mArrayMark;
    String[] mArrayAnswer;
    String[] mArrayMain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate()");
        if (wasLoadedFromExternalTextActionItem() && !Permissions.hasStorageAccessPermission(this)) {
            Timber.w("'Card Browser' Action item pressed before storage permissions granted.");
            UIUtils.showThemedToast(this, getString(R.string.intent_handler_failed_no_storage_permission), false);
            displayDeckPickerForPermissionsDialog();
            return;
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN|WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setContentView(R.layout.activity_self_study);
//        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        mArrayStudy = getResources().getStringArray(R.array.self_study_items_study);
        mArrayMark = getResources().getStringArray(R.array.self_study_items_mark);
        mArrayAnswer = getResources().getStringArray(R.array.self_study_items_answer);
        mArrayMain = getResources().getStringArray(R.array.self_study_items_main);
        ((KeyBoardListenerLayout)findViewById(R.id.root_layout)).setKeyboardListener((isActive, keyboardHeight) -> {
            findViewById(R.id.bottom_area_layout).setVisibility(isActive?View.GONE:View.VISIBLE);
            RelativeLayout.LayoutParams params= (RelativeLayout.LayoutParams) mCardsListView.getLayoutParams();
            params.bottomMargin=isActive?0:(int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 52,  getResources().getDisplayMetrics());;
            mCardsListView = findViewById(R.id.card_browser_list);
        });

//        initNavigationDrawer(findViewById(android.R.id.content));
        startLoadingCollection();
    }


    private void updateTabLayout(int index, int count) {
        ((TextView) mTabLayout.getTabAt(index).view.findViewById(R.id.count)).setText("" + count);
    }


    private TabLayout.Tab generateTab(int count, String name, int index, View.OnClickListener clickListener) {
        TabLayout.Tab tab = mTabLayout.newTab();
        View view = getLayoutInflater().inflate(R.layout.item_self_study_tab, null);
        ((TextView) view.findViewById(R.id.count)).setText("" + count);
        ((TextView) view.findViewById(R.id.name)).setText(name);
        tab.setCustomView(view);
        view.setTag(index);
        view.setOnClickListener(clickListener);
        return tab;
    }


    private Toolbar mToolbar;
    private int mTabType = -1;
    private TextView mStartStudyButton;

    private ListPopupWindow mListPop;
    private final List<Map<String, Object>> mFlagList = new ArrayList<>();
    private final String[] mFlagContent = {"无标志", "红色标志", "橙色标志", "绿色标志", "蓝色标志"};
    private final int[] mFlagRes = {R.mipmap.button_white_flag_normal, R.mipmap.mark_red_flag_normal, R.mipmap.mark_yellow_flag_normal, R.mipmap.mark_green_flag_normal, R.mipmap.mark_blue_flag_normal};

    private RelativeLayout mMultiModeBottomLayout;


    // Finish initializing the activity after the collection has been correctly loaded
    @Override
    protected void onCollectionLoaded(Collection col) {
        super.onCollectionLoaded(col);
        Timber.d("onCollectionLoaded()");
        registerExternalStorageListener();
        final SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        // Load reference to action bar title
        mActionBarTitle = findViewById(R.id.toolbar_title);
        mTabLayout = findViewById(R.id.tab_layout);
        mRestrictOnTab = "";
        mTabType = getIntent().getIntExtra("type", 0);
        mComplete = findViewById(R.id.tv_complete);
        mComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleMultiSelectMode(false);
            }
        });
        mBack = findViewById(R.id.iv_back);
        mStartStudyButton = findViewById(R.id.confirm);
        mStartStudyButton.setOnClickListener(v -> {
            Intent previewer = new Intent(SelfStudyActivity.this, Previewer2.class);
            if (inMultiSelectMode() && checkedCardCount() > 1) {
                // Multiple cards have been explicitly selected, so preview only those cards
                previewer.putExtra("index", 0);
                previewer.putExtra("cardList", getSelectedCardIds());
            } else {
                // Preview all cards, starting from the one that is currently selected
//                int startIndex = mCheckedCards.isEmpty() ? 0 : mCheckedCards.iterator().next().getPosition();
                previewer.putExtra("index", 0);
                previewer.putExtra("cardList", getAllCardIds());
            }
            startActivityForResultWithoutAnimation(previewer, PREVIEW_CARDS);


        });
        // Add drop-down menu to select deck to action bar.
        mDropDownDecks = getCol().getDecks().allSorted();
        mDropDownDeckAdapter = new DeckDropDownAdapter(this, mDropDownDecks, R.layout.dropdown_deck_selected_item_self, this);
        mToolbar = findViewById(R.id.toolbar);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            // enable ActionBar app icon to behave as action to toggle nav drawer
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeButtonEnabled(true);

            // Decide which action to take when the navigation button is tapped.
//            mToolbar.setNavigationIcon(R.mipmap.nav_bar_back_normal);
//            mToolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayShowTitleEnabled(false);
        }


        mOrder = CARD_ORDER_CREATE_TIME;
//        String colOrder = getCol().getConf().getString("sortType");
//        for (int c = 0; c < fSortTypes.length; ++c) {
//            if (fSortTypes[c].equals(colOrder)) {
//                mOrder = c;
//                break;
//            }
//        }
//        if (mOrder == 1 && preferences.getBoolean("cardBrowserNoSorting", false)) {
//            mOrder = 0;
//        }
        //This upgrade should already have been done during
        //setConf. However older version of AnkiDroid didn't call
        //upgradeJSONIfNecessary during setConf, which means the
        //conf saved may still have this bug.
//        mOrderAsc = Upgrade.upgradeJSONIfNecessary(getCol(), getCol().getConf(), "sortBackwards", false);
        mOrderAsc = true;
        getCol().getConf().put("sortType",fSortTypes[mOrder]);
        getCol().getConf().put("sortBackwards", mOrderAsc);

        mCards = new ArrayList<>();
        mCardsListView = findViewById(R.id.card_browser_list);
        mMultiModeBottomLayout = findViewById(R.id.rl_multi_mode);
        findViewById(R.id.add_note_action).setOnClickListener(v -> {
            Intent intent = new Intent(SelfStudyActivity.this, NoteEditor.class);
            intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CARDBROWSER_ADD);
            startActivityForResultWithAnimation(intent, ADD_NOTE, ActivityTransitionAnimation.LEFT);
        });
        TextView selectCount = findViewById(R.id.select_count);
        TextView move = findViewById(R.id.move);
        TextView delete = findViewById(R.id.delete);
        TextView cancel = findViewById(R.id.cancel);
        CheckBox stick = findViewById(R.id.stick);
        move.setOnClickListener(v -> {
            if(mCardsAdapter.getSelectedItemIds().isEmpty())return;
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(SelfStudyActivity.this);
            builderSingle.setTitle(getString(R.string.move_all_to_deck));
            //WARNING: changeDeck depends on this index, so any changes should be reflected there.
            final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(SelfStudyActivity.this, R.layout.dropdown_deck_item);
            for (Deck deck : getValidDecksForChangeDeck()) {
                try {
                    arrayAdapter.add(deck.getString("name"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            builderSingle.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

            builderSingle.setAdapter(arrayAdapter, (dialog, which) -> changeDeck(which));
            builderSingle.show();

        });
        delete.setOnClickListener(v -> {
            if(mCardsAdapter.getSelectedItemIds().isEmpty())return;
            CollectionTask.launchCollectionTask(DISMISS_MULTI,
                    mDeleteNoteHandler,
                    new TaskData(new Object[] {mCardsAdapter.getSelectedItemIdArray(), Collection.DismissType.DELETE_NOTE_MULTI}));
            toggleMultiSelectMode(false);
            mCardsAdapter.getSelectedItemIds().clear();
            mCardsAdapter.notifyDataSetChanged();
        });
        int[] attrs = new int[] {
                R.attr.primary_text_third_color999999,
        };
        TypedArray ta = obtainStyledAttributes(attrs);
        int textGrayColor = ta.getColor(0, ContextCompat.getColor(this, R.color.new_primary_text_third_color));
        ta.recycle();
        stick.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mCardsAdapter.selectItem(isChecked);
            selectCount.setText((isChecked ? "全选" : "已选") + mCardsAdapter.selectItemCount());
            selectCount.setTextColor(isChecked ? ContextCompat.getColor(this, R.color.primary_color) : textGrayColor);
        });
        selectCount.setOnClickListener(v -> stick.performClick());

        cancel.setOnClickListener(v -> {
            toggleMultiSelectMode(false);
        });
        mCardsAdapter = new CardsListAdapter(getLayoutInflater(), this, new CardsListAdapter.CardListAdapterCallback() {
            @Override
            public List<CardCache> getCards() {
                return SelfStudyActivity.this.getCards();
            }


            @Override
            public void onChangeMultiMode(boolean isMultiMode) {
                mMultiModeBottomLayout.setVisibility(isMultiMode ? View.VISIBLE : View.GONE);
                mStartStudyButton.setVisibility(isMultiMode ? View.GONE : mCards.size() > 0 ? View.VISIBLE : View.GONE);
                mSearchView.setVisibility(isMultiMode ? View.INVISIBLE : View.VISIBLE);
                mBack.setVisibility(isMultiMode?View.GONE:View.VISIBLE);
                mComplete.setVisibility(isMultiMode?View.VISIBLE:View.GONE);
                selectCount.setText("已选0");
                supportInvalidateOptionsMenu();

            }


            @Override
            public void onItemSelect(int count) {
                selectCount.setText("已选" + count);
                updateMultiselectMenu();
            }
        });
        // link the adapter to the main mCardsListView
        mCardsListView.setAdapter(mCardsAdapter);
        mCardsListView.setLayoutManager(new LinearLayoutManager(this));
        mCardsAdapter.setDeckClickListener(view -> {
            if(mCardsAdapter.isMultiCheckableMode())return;
            Intent previewer = new Intent(SelfStudyActivity.this, Previewer.class);
            long[] ids = inMultiSelectMode() && checkedCardCount() > 1 ? getSelectedCardIds() : getAllCardIds();
            long targetId = (long) view.getTag();
//            mLastSelectedPosition = position;
            if (ids.length > 100) {
                //为提高效率 直接复制卡牌
                long[] finalIds = new long[ids.length + 1];
                finalIds[0] = targetId;
                System.arraycopy(ids, 0, finalIds, 1, ids.length);
                previewer.putExtra("cardList", finalIds);
            } else {
                for (int i = 0; i < ids.length; i++) {
                    if (ids[i] == targetId) {
                        ids[i] = ids[0];
                        ids[0] = targetId;
                    }
                }
                previewer.putExtra("cardList", ids);
            }
            previewer.putExtra("index", 0);
            startActivityForResultWithoutAnimation(previewer, PREVIEW_CARDS);
//            openNoteEditorForCard((long) view.getTag());
        });
        mCardsAdapter.setDeckLongClickListener(view -> {
            if(mCardsAdapter.isMultiCheckableMode())return false;
            mCardsAdapter.setMultiCheckable(true);
            return true;
        });
        mCardsAdapter.setMarkClickListener(v -> {
            CollectionTask.launchCollectionTask(DISMISS_MULTI,
                    markCardHandler(),
                    new TaskData(new Object[] {new long[] {(long) v.getTag()}, Collection.DismissType.MARK_NOTE_MULTI}));
            mCardsAdapter.notifyDataSetChanged();
        });
        mCardsAdapter.setFlagClickListener(v -> {
            if (mListPop == null) {
                mListPop = new ListPopupWindow(this);
                for (int i = 0; i < mFlagRes.length; i++) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("img", mFlagRes[i]);
                    map.put("content", mFlagContent[i]);
                    mFlagList.add(map);
                }
                mListPop.setAdapter(new SimpleAdapter(SelfStudyActivity.this, mFlagList, R.layout.item_flags_list, new String[] {"img", "content"}, new int[] {R.id.flag_icon, R.id.flag_text}));
                mListPop.setWidth(v.getRootView().getWidth() / 2);
                mListPop.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                mListPop.setModal(true);//设置是否是模式


            }
            mListPop.setOnItemClickListener((parent, view, position, id) -> {
                CollectionTask.launchCollectionTask(DISMISS_MULTI,
                        flagCardHandler(),
                        new TaskData(new Object[] {new long[] {(long) v.getTag()}, Collection.DismissType.FLAG, position}));
                mCardsAdapter.notifyDataSetChanged();
                mListPop.dismiss();
            });
            mListPop.setAnchorView(v);
            mListPop.show();
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        // If a valid value for last deck exists then use it, otherwise use libanki selected deck
        if ((getLastDeckId() != null && getLastDeckId() == ALL_DECKS_ID)  ) {
            selectAllDecks();
        } else if (getLastDeckId() != null && getCol().getDecks().get(getLastDeckId(), false) != null) {
            selectDeckById(getLastDeckId());
        } else {
            selectDeckById(getCol().getDecks().selected());
        }

        initSearchView();
        initTabLayout();
        findViewById(R.id.shadeView).setOnClickListener(v -> mPopupWindow.dismiss());
    }


    private boolean mShowGrammarInSearchView;


    public void onBackPressed(View view) {
        onBackPressed();
    }


    private void initSearchView() {
        mSearchView = findViewById(R.id.search_view);
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mSearchView.getLayoutParams();
        layoutParams.gravity = Gravity.CENTER;
        mSearchView.setLayoutParams(layoutParams);
        mSearchView.setOnCloseListener(() -> {
            mSearchTerms = "";
            Timber.i("close search view");
            mSearchView.setQuery(mSearchTerms, false);
            searchCards();
            return false;
        });

        mSearchView.setQueryHint("输入关键词或语法");
        mSearchView.setIconifiedByDefault(false);
        //Get ImageView of icon
//         ImageView searchViewIcon = (ImageView)mSearchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
////         Get parent of gathered icon
//         ViewGroup linearLayoutSearchView = (ViewGroup) searchViewIcon.getParent(); //Remove it from the left...
//         linearLayoutSearchView.removeView(searchViewIcon); //then put it back (to the right by default)
//         linearLayoutSearchView.addView(searchViewIcon);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.equals("")) {
                    mSearchTerms = "";
                }
                return true;
            }


            @Override
            public boolean onQueryTextSubmit(String query) {
                onSearch();
                mSearchView.clearFocus();
                return true;
            }
        });
        mSearchView.setOnSearchClickListener(v -> {
            // Provide SearchView with the previous search terms
            mSearchView.setQuery(mSearchTerms, false);
//            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
//            //为数组最后一位赋值
//            mHits[mHits.length - 1] = SystemClock.uptimeMillis();
//            if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
//                mHits = new long[COUNTS];//重新初始化数组
//                mShowGrammarInSearchView=true;
//                Toast.makeText(SelfStudyActivity.this, "显示语法在搜索栏", Toast.LENGTH_LONG).show();
//            }
        });
        mSearchView.setOnFocusChangeListener((v, hasFocus) -> {
            mStartStudyButton.setVisibility(hasFocus?View.GONE:View.VISIBLE);
            if (hasFocus) {
                return;
            }
            InputMethodManager manager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
            if (manager != null && getCurrentFocus() != null) {
                manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        mSearchView.setOnClickListener(v -> {
            //每次点击时，数组向前移动一位
            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
            //为数组最后一位赋值
            mHits[mHits.length - 1] = SystemClock.uptimeMillis();
            if (mHits[0] >= (SystemClock.uptimeMillis() - DURATION)) {
                mHits = new long[COUNTS];//重新初始化数组
                mShowGrammarInSearchView = true;
                Toast.makeText(SelfStudyActivity.this, "显示语法在搜索栏", Toast.LENGTH_LONG).show();
            }
        });

    }


    final static int COUNTS = 5;// 点击次数
    final static long DURATION = 1000;// 规定有效时间
    long[] mHits = new long[COUNTS];


    private void selectAllDecks() {
        selectDropDownItem(0);
    }


    /**
     * Opens the note editor for a card.
     * We use the Card ID to specify the preview target
     */
    public void openNoteEditorForCard(long cardId) {
        mCurrentCardId = cardId;
        sCardBrowserCard = getCol().getCard(mCurrentCardId);
        // start note editor using the card we just loaded
        Intent editCard = new Intent(this, NoteEditor.class);
        editCard.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CARDBROWSER_EDIT);
        editCard.putExtra(NoteEditor.EXTRA_CARD_ID, sCardBrowserCard.getId());
        startActivityForResultWithAnimation(editCard, EDIT_CARD, ActivityTransitionAnimation.LEFT);
        //#6432 - FIXME - onCreateOptionsMenu crashes if receiving an activity result from edit card when in multiselect
//        endMultiSelectMode();
    }


    private void openNoteEditorForCurrentlySelectedNote() {
        try {
            //Just select the first one. It doesn't particularly matter if there's a multiselect occurring.
            openNoteEditorForCard(getSelectedCardIds()[0]);
        } catch (Exception e) {
            Timber.w(e, "Error Opening Note Editor");
            UIUtils.showThemedToast(this, getString(R.string.card_browser_note_editor_error), false);
        }
    }


    @Override
    protected void onStop() {
        Timber.d("onStop()");
        // cancel rendering the question and answer, which has shared access to mCards
        super.onStop();
        if (!isFinishing()) {
            WidgetStatus.update(this);
            UIUtils.saveCollectionInBackground();
        }
    }


    @Override
    protected void onDestroy() {
        Timber.d("onDestroy()");
        invalidate();
        super.onDestroy();
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
        }
    }


    @Override
    public void onBackPressed() {
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        } else if (inMultiSelectMode()) {
            toggleMultiSelectMode(false);
        } else {
            Timber.i("Back key pressed");
            Intent data = new Intent();
            if (mReloadRequired) {
                // Add reload flag to result intent so that schedule reset when returning to note editor
                data.putExtra("reloadRequired", true);
            }
            closeCardBrowser(RESULT_OK, data);
        }
    }


    @Override
    protected void onResume() {
        Timber.d("onResume()");
        super.onResume();
    }


    private int mSelectedTabIndex = -1;
    List<Integer> mSelectedStudyButtonList = new ArrayList<>();
    List<Integer> mSelectedAnswerButtonList = new ArrayList<>();
    List<Integer> mSelectedMarkButtonList = new ArrayList<>();
    List<Integer> mSelectedMainButtonList = new ArrayList<>();


    private void updateButtonFilter() {
        mSearchStudyFilter = "";
        if (!mSelectedStudyButtonList.contains(0)) {
            for (int i : mSelectedStudyButtonList) {
                if (!mSearchStudyFilter.isEmpty()) {
                    mSearchStudyFilter += " or ";
                } else {
                    mSearchStudyFilter += "(";
                }
                mSearchStudyFilter += getRestrictByTab(TAB_STUDY_STATE, i);
            }
            if (!mSearchStudyFilter.isEmpty()) {
                mSearchStudyFilter += ")";
            }
        }

        if (!mSelectedMarkButtonList.contains(0)) {
            mSearchMarkFilter = "";
            for (int i : mSelectedMarkButtonList) {
                if (!mSearchMarkFilter.isEmpty()) {
                    mSearchMarkFilter += " or ";
                } else {
                    mSearchMarkFilter += "(";
                }
                mSearchMarkFilter += getRestrictByTab(TAB_MARK_STATE, i);
            }
            if (!mSearchMarkFilter.isEmpty()) {
                mSearchMarkFilter += ")";
            }
        }

        if (!mSelectedAnswerButtonList.contains(0)) {
            mSearchAnswerFilter = "";
            for (int i : mSelectedAnswerButtonList) {
                if (!mSearchAnswerFilter.isEmpty()) {
                    mSearchAnswerFilter += " or ";
                } else {
                    mSearchAnswerFilter += "(";
                }
                mSearchAnswerFilter += getRestrictByTab(TAB_ANSWER_STATE, i);
            }
            if (!mSearchAnswerFilter.isEmpty()) {
                mSearchAnswerFilter += ")";
            }
        }


    }


    private String getRestrictByTab(int type, int index) {
        String restrict = "";
        switch (type) {
            case TAB_STUDY_STATE:
                switch (index) {
                    case 0:
//                        restrict= "(prop:lapses>=3) or ((is:learn) or (is:review prop:ivl<21)) or (is:new) or (is:review prop:ivl>=21) or (is:suspended)";
                        restrict = "";
                        break;

                    case 1:
                        restrict = "((is:learn) or (is:review prop:ivl<10))";
                        break;
                    case 2:
                        restrict = "(is:new)";
                        break;
                    case 3:
                        restrict = "(is:review prop:ivl>=10)";
                        break;
                    case 4:
                        restrict = "(prop:lapses>=3)";
                        break;
                    case 5:
                        restrict = "(is:suspended)";
                        break;
                }
                break;
            case TAB_MARK_STATE:
                switch (index) {
                    case 0://事实上不会来到这里，这里是弹窗里的全部
//                        restrict= "(flag:1) or (flag:2) or (flag:3) or (flag:4) or (tag:marked)";
                        restrict = "";
                        break;
                    case 1:
                    case 2:
                    case 3:
                    case 4:
                        restrict = "(flag:" + index + ")";
                        break;
                    case 5:
                        restrict = "(tag:marked)";
                        break;
                }
                break;
            case TAB_ANSWER_STATE:
                switch (index) {
                    case 0://事实上不会来到这里，这里是弹窗里的全部
//                        restrict= "(rated:31:1) or (rated:31:2) or (rated:31:3) or (rated:31:4)";
                        restrict = "";
                        break;
                    case 1:
                        restrict = "(rated:31:1)";
                        break;
                    case 2:
                        restrict = "(rated:31:2)";
                        break;
                    case 3:
                        restrict = "(rated:31:3)";
                        break;
                    case 4:
                        restrict = "(rated:31:4)";
                        break;
                }
                break;

            case TAB_MAIN_STATE:
                switch (index) {
                    case 0: //全部
                        restrict = "";
                        break;
                    case 1:
                        restrict = "(-is:new)";//fixme 需要有排序功能
                        break;
                    case 2:
                        restrict = "(is:new)";
                        break;
                    case 3:
                        restrict = "((is:learn) or (is:review prop:ivl<10))";
                        break;
                    case 4:
                        restrict = "(is:review prop:ivl>=10)";
                        break;
                    case 5:
                        restrict = "((rated:31:1) or (rated:31:2))";
                        break;
                    case 6:
                        restrict = "(is:suspended)";
                        break;
                }
                break;
            case TAB_CUSTOM_STATE:
                break;
        }
        return restrict;

    }


    private void initTabLayout() {
//        mTabLayout.setTabMode(TabLayout.MODE_FIXED);

        LinearLayout linearLayout = (LinearLayout) mTabLayout.getChildAt(0);
        linearLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        linearLayout.setDividerDrawable(ContextCompat.getDrawable(this,
                R.drawable.divider_vertical)); //设置分割线的样式
        linearLayout.setDividerPadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, getResources().getDisplayMetrics())); //设置分割线间隔

        mTabLayout.removeAllTabs();
        if (mTabType == TAB_MAIN_STATE) {
            mTabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        }
        switch (mTabType) {
            case TAB_STUDY_STATE:
                addTab(mArrayStudy, mSelectedStudyButtonList);
                break;
            case TAB_MARK_STATE:
                addTab(mArrayMark, mSelectedMarkButtonList);
                break;
            case TAB_ANSWER_STATE:
                addTab(mArrayAnswer, mSelectedAnswerButtonList);
                break;
            case TAB_MAIN_STATE:
                addTab(mArrayMain, mSelectedMainButtonList);
                break;
            case TAB_CUSTOM_STATE:
                mTabLayout.setVisibility(View.GONE);
                new Handler().postDelayed(this::showScreenDialog, 500);
                break;
        }
        if (mTabType != TAB_CUSTOM_STATE) {
            Objects.requireNonNull(mTabLayout.getTabAt(0)).getCustomView().performClick();
        }

    }


    private void addTab(String[] titles, List<Integer> needUpdateList) {

        if (mTabType != TAB_MAIN_STATE) {
            for (int i = 0; i < titles.length - 1; i++) {
                int finalI = i;
                mTabLayout.addTab(generateTab(0, titles[i + 1], i + 1, v -> onTabClick(finalI, v, needUpdateList)));
            }
        } else {
            for (int i = 0; i < titles.length; i++) {
                int finalI = i;
                mTabLayout.addTab(generateTab(0, titles[i], i, v -> onTabClick(finalI, v, needUpdateList)));
            }
        }
    }


    private void onTabClick(int position, View view, List<Integer> needUpdateList) {
//        Timber.i("on tab click: " + (mSearchView == null));
        toggleMultiSelectMode(false);
        mTabLayout.selectTab(mTabLayout.getTabAt(position));
        for (int m = 0; m < mTabLayout.getTabCount(); m++) {
            Objects.requireNonNull(mTabLayout.getTabAt(m)).view.setSelected(m == position);
        }
        mSelectedTabIndex = (int) view.getTag();

        mRangeSearchForgetTimesFilter = "";
        mRangeSearchReviewTimesFilter = "";
        mRangeSearchAnswerFilter = "";
        mRangeCreateTimeFilter = "";
        mSelectedTags.clear();
        updateTagsSearchTerms();
        mSelectedMarkButtonList.clear();
        mSelectedAnswerButtonList.clear();
        mSelectedStudyButtonList.clear();
        mSelectedMainButtonList.clear();
        updateButtonFilter();//按钮选择
        mSelectedDropDownDeckPosition = mDefaultDropDownDeckPosition;
        deckDropDownItemChanged(mSelectedDropDownDeckPosition);//卡牌选择

        mSearchTerms = "";
        if (mSearchView != null) {
            mSearchView.setQuery(mSearchTerms, false);
        }
        mRestrictOnTab = getRestrictByTab(mTabType, mSelectedTabIndex);
        Timber.i("this is what i select tab filter:%s", mRestrictOnTab);
        needUpdateList.clear();
        needUpdateList.add(mSelectedTabIndex);
        searchCards();
    }


    private PopupWindow mPopupWindow;
    private String mSearchMarkFilter = "";
    private String mSearchAnswerFilter = "";
    private String mSearchStudyFilter = "";
    private String mRangeSearchAnswerFilter = "";
    private String mRangeCreateTimeFilter = "";
    private String mRangeSearchReviewTimesFilter = "";
    private String mRangeSearchForgetTimesFilter = "";


    private void resetFilter() {

        mRangeSearchForgetTimesFilter = "";
        mRangeSearchReviewTimesFilter = "";
        mRangeSearchAnswerFilter = "";
        mRangeCreateTimeFilter = "";
        mSelectedTags.clear();
        updateTagsSearchTerms();

        mSelectedMarkButtonList.clear();
        mSelectedAnswerButtonList.clear();
        mSelectedStudyButtonList.clear();


        updateButtonFilter();//按钮选择
        mSelectedDropDownDeckPosition = mDefaultDropDownDeckPosition;
        deckDropDownItemChanged(mSelectedDropDownDeckPosition);//卡牌选择
    }


    private boolean showScreenDialog() {
        InputMethodManager manager = ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE));
        if (manager != null && getCurrentFocus() != null) {
            manager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
        View dialog = getLayoutInflater().inflate(R.layout.pop_window_screen_self_study, null);
        mPopupWindow = new PopupWindow(dialog, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        TextView studyTitle = dialog.findViewById(R.id.memory_title);
        TextView markTitle = dialog.findViewById(R.id.mark_title);
        TextView answerTitle = dialog.findViewById(R.id.answer_title);

        WarpLinearLayout studyLayout = dialog.findViewById(R.id.memory_layout);
        WarpLinearLayout markLayout = dialog.findViewById(R.id.mark_layout);
        WarpLinearLayout answerLayout = dialog.findViewById(R.id.answer_layout);

        studyLayout.setVisibility(mTabType != TAB_MARK_STATE && mTabType != TAB_ANSWER_STATE ? View.VISIBLE : View.GONE);
        studyTitle.setVisibility(studyLayout.getVisibility());

        markLayout.setVisibility(View.VISIBLE);
        markTitle.setVisibility(View.VISIBLE);

        answerLayout.setVisibility(mTabType != TAB_MARK_STATE && mTabType != TAB_STUDY_STATE ? View.VISIBLE : View.GONE);
        answerTitle.setVisibility(answerLayout.getVisibility());

        List<Button> studyButtonList = new ArrayList<>();
        for (int i = 0; i < mArrayStudy.length; i++) {
            View view = getLayoutInflater().inflate(R.layout.item_warp, null);
            Button button = view.findViewById(R.id.text);
            button.setSelected(mSelectedStudyButtonList.contains(i));
            button.setText(mArrayStudy[i]);
            button.setTag(i);
            button.setOnClickListener(v -> {
                if ((Integer) v.getTag() == 0) {
                    for (int m = 1; m < studyButtonList.size(); m++) {
                        if (mSelectedStudyButtonList.contains(m)) {
                            mSelectedStudyButtonList.remove((Object) m);
                        }
                        studyButtonList.get(m).setSelected(false);
                    }
                } else {
                    studyButtonList.get(0).setSelected(false);
                    if (mSelectedStudyButtonList.contains(0)) {
                        mSelectedStudyButtonList.remove((Object) 0);
                    }
                }
                if (mSelectedStudyButtonList.contains(v.getTag())) {
                    mSelectedStudyButtonList.remove(v.getTag());
                } else {
                    mSelectedStudyButtonList.add((Integer) v.getTag());
                }
                v.setSelected(!v.isSelected());

            });
            studyButtonList.add(button);
            studyLayout.addView(view);
        }
        List<Button> markButtonList = new ArrayList<>();
        for (int i = 0; i < mArrayMark.length; i++) {
            View view = getLayoutInflater().inflate(R.layout.item_warp, null);
            Button button = view.findViewById(R.id.text);
            button.setSelected(mSelectedMarkButtonList.contains(i));
            button.setText(mArrayMark[i]);
            button.setTag(i);
            button.setOnClickListener(v -> {
                if ((Integer) v.getTag() == 0) {
                    for (int m = 1; m < markButtonList.size(); m++) {
                        if (mSelectedMarkButtonList.contains(m)) {
                            mSelectedMarkButtonList.remove((Object) m);
                        }

                        markButtonList.get(m).setSelected(false);
                    }
                } else {
                    markButtonList.get(0).setSelected(false);
                    if (mSelectedMarkButtonList.contains(0)) {
                        mSelectedMarkButtonList.remove((Object) 0);
                    }
                }
                if (mSelectedMarkButtonList.contains(v.getTag())) {
                    mSelectedMarkButtonList.remove(v.getTag());
                } else {
                    mSelectedMarkButtonList.add((Integer) v.getTag());
                }
                v.setSelected(!v.isSelected());

            });
            markButtonList.add(button);
            markLayout.addView(view);
        }

        List<Button> answerButtonList = new ArrayList<>();
        for (int i = 0; i < mArrayAnswer.length; i++) {
            View view = getLayoutInflater().inflate(R.layout.item_warp, null);
            Button button = view.findViewById(R.id.text);
            button.setSelected(mSelectedAnswerButtonList.contains(i));
            button.setText(mArrayAnswer[i]);
            button.setTag(i);
            button.setOnClickListener(v -> {
                if ((Integer) v.getTag() == 0) {
                    for (int m = 1; m < answerButtonList.size(); m++) {
                        if (mSelectedAnswerButtonList.contains(m)) {
                            mSelectedAnswerButtonList.remove((Object) m);
                        }

                        answerButtonList.get(m).setSelected(false);
                    }
                } else {
                    answerButtonList.get(0).setSelected(false);
                    if (mSelectedAnswerButtonList.contains(0)) {
                        mSelectedAnswerButtonList.remove((Object) 0);
                    }
                }
                if (mSelectedAnswerButtonList.contains(v.getTag())) {
                    mSelectedAnswerButtonList.remove(v.getTag());
                } else {
                    mSelectedAnswerButtonList.add((Integer) v.getTag());
                }
                v.setSelected(!v.isSelected());

            });
            answerButtonList.add(button);
            answerLayout.addView(view);
        }

        RangeSeekBar answerTimesSeekBar = dialog.findViewById(R.id.answer_times_seek_bar);
        answerTimesSeekBar.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                if (mTabType == TAB_CUSTOM_STATE) {
                    ((TextView) dialog.findViewById(R.id.answer_times_num)).setText(leftValue == answerTimesSeekBar.getMaxProgress() ? "不限" : String.format(Locale.CHINA, "最近%.0f天", leftValue));
                } else {
                    ((TextView) dialog.findViewById(R.id.answer_times_num)).setText(String.format(Locale.CHINA, "最近%.0f天", leftValue));
                }

                if (leftValue == answerTimesSeekBar.getMaxProgress()) {
                    mRangeSearchAnswerFilter = "";
                } else {
                    mRangeSearchAnswerFilter = String.format(Locale.CHINA, "(rated:%.0f)", leftValue);
                }
            }


            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }


            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });
        answerTimesSeekBar.setRange(0, mTabType == TAB_CUSTOM_STATE ? 31 : 30);
        answerTimesSeekBar.setProgress(mTabType == TAB_CUSTOM_STATE ? 31 : 30);

        RangeSeekBar createTimeSeekBar = dialog.findViewById(R.id.create_time_seek_bar);
        createTimeSeekBar.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                ((TextView) dialog.findViewById(R.id.create_time_num)).setText(leftValue == createTimeSeekBar.getMaxProgress() ? "不限" : String.format(Locale.CHINA, "最近%.0f天", leftValue));

                if (leftValue == createTimeSeekBar.getMaxProgress()) {
                    mRangeCreateTimeFilter = "";
                } else {
                    mRangeCreateTimeFilter = String.format(Locale.CHINA, "(added:%.0f)", leftValue);
                }
            }


            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }


            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });
        createTimeSeekBar.setRange(0, 366);
        createTimeSeekBar.setProgress(366);

        dialog.findViewById(R.id.answer_times_seek_layout).setVisibility(mTabType == TAB_ANSWER_STATE || mTabType == TAB_CUSTOM_STATE || mTabType == TAB_MAIN_STATE ? View.VISIBLE : View.GONE);
        RangeSeekBar reviewTimesSeekBar = dialog.findViewById(R.id.review_times_seek_bar);
        reviewTimesSeekBar.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                String rightStr = rightValue == reviewTimesSeekBar.getMaxProgress() ? "不限" : String.format(Locale.CHINA, "%.0f", rightValue);
                ((TextView) dialog.findViewById(R.id.review_times_num)).setText(String.format(Locale.CHINA, "%.0f-%s", leftValue, rightStr));
                if (rightValue == reviewTimesSeekBar.getMaxProgress()) {
                    if (leftValue > 0) {
                        mRangeSearchReviewTimesFilter = String.format(Locale.CHINA, "(prop:reps>=%.0f)", leftValue);
                    } else {
                        mRangeSearchReviewTimesFilter = "";
                    }
                } else {
                    mRangeSearchReviewTimesFilter = String.format(Locale.CHINA, "(prop:reps>=%.0f prop:reps<=%.0f)", leftValue, rightValue);
                }

            }


            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }


            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });
        reviewTimesSeekBar.setRange(0, 20);
        reviewTimesSeekBar.setProgress(0, 20);


        RangeSeekBar forgetTimesSeekBar = dialog.findViewById(R.id.forget_times_seek_bar);
        forgetTimesSeekBar.setOnRangeChangedListener(new OnRangeChangedListener() {
            @Override
            public void onRangeChanged(RangeSeekBar view, float leftValue, float rightValue, boolean isFromUser) {
                String rightStr = rightValue == forgetTimesSeekBar.getMaxProgress() ? "不限" : String.format(Locale.CHINA, "%.0f", rightValue);
                ((TextView) dialog.findViewById(R.id.forget_times_num)).setText(String.format(Locale.CHINA, "%.0f-%s", leftValue, rightStr));
                if (rightValue == forgetTimesSeekBar.getMaxProgress()) {
                    if (leftValue > 0) {
                        mRangeSearchForgetTimesFilter = String.format(Locale.CHINA, "(prop:lapses>=%.0f)", leftValue);
                    } else {
                        mRangeSearchForgetTimesFilter = "";
                    }
                } else {
                    mRangeSearchForgetTimesFilter = String.format(Locale.CHINA, "(prop:lapses>=%.0f prop:lapses<=%.0f)", leftValue, rightValue);
                }
            }


            @Override
            public void onStartTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }


            @Override
            public void onStopTrackingTouch(RangeSeekBar view, boolean isLeft) {

            }
        });
        forgetTimesSeekBar.setRange(0, 10);
        forgetTimesSeekBar.setProgress(0, 10);


        Spinner deck_spinner = dialog.findViewById(R.id.deck_spinner);
        deck_spinner.setAdapter(mDropDownDeckAdapter);
        deck_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mSelectedDropDownDeckPosition = position;
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
        deck_spinner.setSelection(mSelectedDropDownDeckPosition);
        RelativeLayout tag_spinner = dialog.findViewById(R.id.tag_spinner);
        tag_spinner.setVisibility(mTabType == TAB_CUSTOM_STATE ||mTabType == TAB_MAIN_STATE? View.VISIBLE : View.GONE);
        TextView tag_selected = dialog.findViewById(R.id.tag_selected);
        tag_spinner.setOnClickListener(v -> showTagsDialog(tag_selected));
        Button reset = dialog.findViewById(R.id.reset);
        Button confirm = dialog.findViewById(R.id.confirm);
        reset.setOnClickListener(v -> {
            resetFilter();

            deck_spinner.setSelection(mSelectedDropDownDeckPosition);//重置卡组选择
            for (int i = 1; i < studyLayout.getChildCount(); i++) {
                studyLayout.getChildAt(i).setSelected(false);
            }
            for (int i = 1; i < markLayout.getChildCount(); i++) {
                markLayout.getChildAt(i).setSelected(false);
            }
            for (int i = 1; i < answerLayout.getChildCount(); i++) {
                answerLayout.getChildAt(i).setSelected(false);
            }
            switch (mTabType) {
                case TAB_STUDY_STATE:
                    studyLayout.getChildAt(1).performClick();
                    break;
                case TAB_MARK_STATE:
                    markLayout.getChildAt(1).performClick();
                    break;
                case TAB_ANSWER_STATE:
                    answerLayout.getChildAt(1).performClick();
                    break;
                case TAB_CUSTOM_STATE:
                case TAB_MAIN_STATE:
                    break;
            }
            forgetTimesSeekBar.setProgress(0, 10);
            reviewTimesSeekBar.setProgress(0, 20);
            answerTimesSeekBar.setProgress(mTabType == TAB_CUSTOM_STATE || mTabType == TAB_MAIN_STATE ? 31 : 30);
//            mSelectedMarkButtonList.add(0);
//            mSelectedAnswerButtonList.add(0);
//            mSelectedStudyButtonList.add(0);
//            studyLayout.getChildAt(0).setSelected(true);
//            markLayout.getChildAt(0).setSelected(true);
//            answerLayout.getChildAt(0).setSelected(true);


        });
        confirm.setOnClickListener(v -> {
            if (mTabType == TAB_MAIN_STATE) {
                for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                    mTabLayout.getTabAt(i).getCustomView().setSelected(false);
                }
            } else if (mTabType != TAB_CUSTOM_STATE) {
                mTabLayout.getTabAt(mSelectedTabIndex - 1).getCustomView().setSelected(false);
            }

            mRestrictOnTab = "";

            updateTagsSearchTerms();//标签选择

            deckDropDownItemChanged(mSelectedDropDownDeckPosition);//卡牌选择

            updateButtonFilter();//按钮选择

            mPopupWindow.dismiss();

            searchCards();

        });

        mPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        mPopupWindow.setOutsideTouchable(false);
        mPopupWindow.setFocusable(true);
//        ViewGroup.LayoutParams lp = dialog.getLayoutParams();
////        lp.alpha = 0.4f;
////        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
//        lp.height=2000;
//        dialog.setLayoutParams(lp);
//        // 在dismiss中恢复透明度
        mPopupWindow.setOnDismissListener(() -> findViewById(R.id.shadeView).setVisibility(View.GONE));
//        DisplayMetrics outMetrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
//        int heightPixels = outMetrics.heightPixels;
//
//        int notificationBar = Resources.getSystem().getDimensionPixelSize(
//                Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android"));
//        int[] location = new int[2];
////        mToolbar.getLocationInWindow(location); //获取在当前窗体内的绝对坐标
//        mToolbar.getLocationOnScreen(location);//获取在整个屏幕内的绝对坐标
////        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
////        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
////        lp.x = location[0];
//        mPopupWindow.setHeight(heightPixels - (location[1] + mToolbar.getHeight()));
        mPopupWindow.showAsDropDown(mToolbar, 0, 0);
        findViewById(R.id.shadeView).setVisibility(View.VISIBLE);
        return false;
    }


    private String mTagsFilter = "";


    private void updateTagsSearchTerms() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        mTagsFilter = "";
        for (String tag : mSelectedTags) {
            if (i != 0) {
                sb.append("or ");
            } else {
                sb.append("("); // Only if we really have selected tags
            }
            // 7070: quote tags so brackets are properly escaped
            sb.append("tag:").append("'").append(tag).append("'").append(" ");
            i++;
        }
        if (i > 0) {
            sb.append(")"); // Only if we added anything to the tag list
            mTagsFilter += sb.toString();//tags
        }

    }


    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        Timber.d("onCreateOptionsMenu()");
        mActionBarMenu = menu;
        if (mCardsAdapter != null && mCardsAdapter.isMultiCheckableMode()) {
            getMenuInflater().inflate(R.menu.card_browser_multiselect2, menu);

        } else {
            getMenuInflater().inflate(R.menu.activity_self_study_menu, menu);
            menu.findItem(R.id.screen).setOnMenuItemClickListener(item -> showScreenDialog());
        }
        return super.onCreateOptionsMenu(menu);
    }

//    @Override
//    protected void onNavigationPressed() {
//        if (inMultiSelectMode()) {
//            endMultiSelectMode();
//        } else {
//            super.onNavigationPressed();
//        }
//    }


    private void displayDeckPickerForPermissionsDialog() {
        //TODO: Combine this with class: IntentHandler after both are well-tested
        Intent deckPicker = new Intent(this, DeckPicker.class);
        deckPicker.setAction(Intent.ACTION_MAIN);
        deckPicker.addCategory(Intent.CATEGORY_LAUNCHER);
        deckPicker.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityWithAnimation(deckPicker, ActivityTransitionAnimation.FADE);
        AnkiActivity.finishActivityWithFade(this);
        finishActivityWithFade(this);
        this.setResult(RESULT_CANCELED);
    }


    private boolean wasLoadedFromExternalTextActionItem() {
        Intent intent = this.getIntent();
        if (intent == null) {
            return false;
        }
        //API 23: Replace with Intent.ACTION_PROCESS_TEXT
        return "android.intent.action.PROCESS_TEXT".equalsIgnoreCase(intent.getAction());
    }


    /**
     * Returns the number of cards that are visible on the screen
     */
    public int getCardCount() {
        return getCards().size();
    }


    private void updateMultiselectMenu() {
        Timber.d("updateMultiselectMenu()");
        if (mActionBarMenu == null || mActionBarMenu.findItem(R.id.action_suspend_card) == null) {
            return;
        }

        if ( getSelectedCardIds().length!=0) {
            CollectionTask.cancelAllTasks(CHECK_CARD_SELECTION);
            CollectionTask.launchCollectionTask(CHECK_CARD_SELECTION,
                    mCheckSelectedCardsHandler,
                    new TaskData(new Object[] {getSelectedCards(), getCards()}));
        }

    }


    //    private boolean hasSelectedCards() {
//        return !mCheckedCards.isEmpty();
//    }
//
//
    private boolean hasSelectedAllCards() {
        return checkedCardCount() >= getCardCount(); //must handle 0.
    }


    private void flagTask(int flag) {
        CollectionTask.launchCollectionTask(DISMISS_MULTI,
                flagCardHandler(),
                new TaskData(new Object[] {getSelectedCardIds(), Collection.DismissType.FLAG, new Integer(flag)}));
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (getDrawerToggle().onOptionsItemSelected(item)) {
//            return true;
//        }

        // dismiss undo-snackbar if shown to avoid race condition
        // (when another operation will be performed on the model, it will undo the latest operation)
        if (mUndoSnackbar != null && mUndoSnackbar.isShown()) {
            mUndoSnackbar.dismiss();
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                endMultiSelectMode();
                return true;
            case R.id.action_add_note_from_card_browser: {
                Intent intent = new Intent(SelfStudyActivity.this, NoteEditor.class);
                intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_CARDBROWSER_ADD);
                startActivityForResultWithAnimation(intent, ADD_NOTE, ActivityTransitionAnimation.LEFT);
                return true;
            }

            case R.id.action_save_search: {
                String searchTerms = mSearchView.getQuery().toString();
                showDialogFragment(CardBrowserMySearchesDialog.newInstance(null, mMySearchesDialogListener,
                        searchTerms, CardBrowserMySearchesDialog.CARD_BROWSER_MY_SEARCHES_TYPE_SAVE));
                return true;
            }

            case R.id.action_list_my_searches: {
                JSONObject savedFiltersObj = getCol().getConf().optJSONObject("savedFilters");
                HashMap<String, String> savedFilters = new HashMap<>();
                if (savedFiltersObj != null) {
                    Iterator<String> it = savedFiltersObj.keys();
                    while (it.hasNext()) {
                        String searchName = it.next();
                        savedFilters.put(searchName, savedFiltersObj.optString(searchName));
                    }
                }
                showDialogFragment(CardBrowserMySearchesDialog.newInstance(savedFilters, mMySearchesDialogListener,
                        "", CardBrowserMySearchesDialog.CARD_BROWSER_MY_SEARCHES_TYPE_LIST));
                return true;
            }

            case R.id.action_sort_by_size:
                showDialogFragment(CardBrowserOrderDialog
                        .newInstance(mOrder, mOrderAsc, mOrderDialogListener));
                return true;

            case R.id.action_show_marked:
                mSearchTerms = "tag:marked";
                mSearchView.setQuery("", false);
                mSearchView.setQueryHint(getResources().getString(R.string.card_browser_show_marked));
                searchCards();
                return true;

            case R.id.action_show_suspended:
                mSearchTerms = "is:suspended";
                mSearchView.setQuery("", false);
                mSearchView.setQueryHint(getResources().getString(R.string.card_browser_show_suspended));
                searchCards();
                return true;
            case R.id.action_flag_zero:
                flagTask(0);
                return true;

            case R.id.action_flag_one:
                flagTask(1);
                return true;

            case R.id.action_flag_two:
                flagTask(2);
                return true;

            case R.id.action_flag_three:
                flagTask(3);
                return true;

            case R.id.action_flag_four:
                flagTask(4);
                return true;

            case R.id.action_delete_card:
                if (inMultiSelectMode()) {
                    CollectionTask.launchCollectionTask(DISMISS_MULTI,
                            mDeleteNoteHandler,
                            new TaskData(new Object[] {getSelectedCardIds(), Collection.DismissType.DELETE_NOTE_MULTI}));
                    mCardsAdapter.getSelectedItemIds().clear();


                }
                return true;

            case R.id.action_mark_card:
                CollectionTask.launchCollectionTask(DISMISS_MULTI,
                        markCardHandler(),
                        new TaskData(new Object[] {getSelectedCardIds(), Collection.DismissType.MARK_NOTE_MULTI}));

                return true;


            case R.id.action_suspend_card:
                CollectionTask.launchCollectionTask(DISMISS_MULTI,
                        suspendCardHandler(),
                        new TaskData(new Object[] {getSelectedCardIds(), Collection.DismissType.SUSPEND_CARD_MULTI}));
                toggleMultiSelectMode(false);

                return true;

            case R.id.action_change_deck: {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
                builderSingle.setTitle(getString(R.string.move_all_to_deck));

                //WARNING: changeDeck depends on this index, so any changes should be reflected there.
                final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.dropdown_deck_item);
                for (Deck deck : getValidDecksForChangeDeck()) {
                    try {
                        arrayAdapter.add(deck.getString("name"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                builderSingle.setNegativeButton(getString(R.string.cancel), (dialog, which) -> dialog.dismiss());

                builderSingle.setAdapter(arrayAdapter, (dialog, which) -> changeDeck(which));
                builderSingle.show();

                return true;
            }

            case R.id.action_undo:
                if (getCol().undoAvailable()) {
                    CollectionTask.launchCollectionTask(UNDO, mUndoHandler);
                }
                return true;
//            case R.id.action_select_none:
//                onSelectNone();
//                return true;
//            case R.id.action_select_all:
//                onSelectAll();
//                return true;

            case R.id.action_preview: {
                mStartStudyButton.performClick();
                return true;
            }

            case R.id.action_reset_cards_progress: {
                Timber.i("NoteEditor:: Reset progress button pressed");
                // Show confirmation dialog before resetting card progress
                ConfirmationDialog dialog = new ConfirmationDialog();
                String title = getString(R.string.reset_card_dialog_title);
                String message = getString(R.string.reset_card_dialog_message);
                dialog.setArgs(title, message);
                Runnable confirm = () -> {
                    Timber.i("CardBrowser:: ResetProgress button pressed");
                    CollectionTask.launchCollectionTask(DISMISS_MULTI, resetProgressCardHandler(),
                            new TaskData(new Object[] {getSelectedCardIds(), Collection.DismissType.RESET_CARDS}));
                };
                dialog.setConfirm(confirm);
                showDialogFragment(dialog);
                return true;
            }
            case R.id.action_reschedule_cards: {
                Timber.i("CardBrowser:: Reschedule button pressed");

                long[] selectedCardIds = getSelectedCardIds();
                FunctionalInterfaces.Consumer<Integer> consumer = newDays ->
                        CollectionTask.launchCollectionTask(DISMISS_MULTI,
                                rescheduleCardHandler(),
                                new TaskData(new Object[] {selectedCardIds, Collection.DismissType.RESCHEDULE_CARDS, newDays}));

                RescheduleDialog rescheduleDialog;
                if (selectedCardIds.length == 1) {
                    long cardId = selectedCardIds[0];
                    Card selected = getCol().getCard(cardId);
                    rescheduleDialog = RescheduleDialog.rescheduleSingleCard(getResources(), selected, consumer);
                } else {
                    rescheduleDialog = RescheduleDialog.rescheduleMultipleCards(getResources(),
                            consumer,
                            selectedCardIds.length);
                }
                showDialogFragment(rescheduleDialog);
                return true;
            }
            case R.id.action_reposition_cards: {
                Timber.i("CardBrowser:: Reposition button pressed");

                // Only new cards may be repositioned
                long[] cardIds = getSelectedCardIds();
                for (int i = 0; i < cardIds.length; i++) {
                    if (getCol().getCard(cardIds[i]).getQueue() != Consts.CARD_TYPE_NEW) {
                        SimpleMessageDialog dialog = SimpleMessageDialog.newInstance(
                                getString(R.string.vague_error),
                                getString(R.string.reposition_card_not_new_error),
                                false);
                        showDialogFragment(dialog);
                        return false;
                    }
                }

                IntegerDialog repositionDialog = new IntegerDialog();
                repositionDialog.setArgs(
                        getString(R.string.reposition_card_dialog_title),
                        getString(R.string.reposition_card_dialog_message),
                        5);
                repositionDialog.setCallbackRunnable(days ->
                        CollectionTask.launchCollectionTask(DISMISS_MULTI, repositionCardHandler(),
                                new TaskData(new Object[] {cardIds, Collection.DismissType.REPOSITION_CARDS, days}))
                );
                showDialogFragment(repositionDialog);
                return true;
            }
            case R.id.action_edit_note: {
                openNoteEditorForCurrentlySelectedNote();
            }

            default:
                return super.onOptionsItemSelected(item);

        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // FIXME:
        Timber.d("onActivityResult(requestCode=%d, resultCode=%d)", requestCode, resultCode);
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == DeckPicker.RESULT_DB_ERROR) {
            closeCardBrowser(DeckPicker.RESULT_DB_ERROR);
        }
        if (requestCode == EDIT_CARD && resultCode != RESULT_CANCELED) {
            Timber.i("CardBrowser:: CardBrowser: Saving card...");
            CollectionTask.launchCollectionTask(UPDATE_NOTE, updateCardHandler(),
                    new TaskData(sCardBrowserCard, false));
        } else if (requestCode == ADD_NOTE && resultCode == RESULT_OK) {
            if (mSearchView != null) {
                mSearchTerms = mSearchView.getQuery().toString();
                searchCards();
            } else {
                Timber.w("Note was added from browser and on return mSearchView == null");
            }
        }

        // Previewing can now perform an "edit", so it can pass on a reloadRequired
//        if (requestCode == PREVIEW_CARDS) {
//            updateDeckNum();
//            searchCards();
////            if (getReviewerCardId() == mCurrentCardId) {
////                mReloadRequired = true;
////            }
//        }
        if (requestCode == PREVIEW_CARDS && data != null
                && (data.getBooleanExtra("reloadRequired", false) || data.getBooleanExtra("noteChanged", false))) {
            searchCards();
            if (getReviewerCardId() == mCurrentCardId) {
                mReloadRequired = true;
            }
        }

        if (requestCode == EDIT_CARD && data != null &&
                (data.getBooleanExtra("reloadRequired", false) ||
                        data.getBooleanExtra("noteChanged", false))) {
            // if reloadRequired or noteChanged flag was sent from note editor then reload card list
            searchCards();
            // in use by reviewer?
            if (getReviewerCardId() == mCurrentCardId) {
                mReloadRequired = true;
            }
        }

        invalidateOptionsMenu();    // maybe the availability of undo changed
    }


    // We spawn CollectionTasks that may create memory pressure, this transmits it so polling isCancelled sees the pressure
    @Override
    public void onTrimMemory(int pressureLevel) {
        super.onTrimMemory(pressureLevel);
        CollectionTask.cancelCurrentlyExecutingTask();
    }


    private long getReviewerCardId() {
        if (getIntent().hasExtra("currentCard")) {
            return getIntent().getExtras().getLong("currentCard");
        } else {
            return -1;
        }
    }


    private void showTagsDialog(TextView tag_selected) {
        TagsDialog dialog = TagsDialog.newInstance(
                TagsDialog.TYPE_FILTER_BY_TAG, new ArrayList<>(), new ArrayList<>(getCol().getTags().all()));
        dialog.setTagsDialogListener((selectedTags, option) -> filterByTag(selectedTags, option, tag_selected));
        showDialogFragment(dialog);
    }


    private int mSelectedDropDownDeckPosition;
    private int mDefaultDropDownDeckPosition = -1;


    /**
     * Selects the given position in the deck list
     */
    public void selectDropDownItem(int position) {
        if (mDefaultDropDownDeckPosition == -1) {
            mDefaultDropDownDeckPosition = position;
        }
        mSelectedDropDownDeckPosition = position;
        deckDropDownItemChanged(position);
    }


    /**
     * Performs changes relating to the Deck DropDown Item changing
     * Exists as mActionBarSpinner.setSelection() caused a loop in roboelectirc (calling onItemSelected())
     */
    private void deckDropDownItemChanged(int position) {
        if (position == 0) {
            mRestrictOnDeck = "";
            saveLastDeckId(ALL_DECKS_ID);
        } else {
            Deck deck = mDropDownDecks.get(position - 1);
            mRestrictOnDeck = "deck:\"" + deck.getString("name") + "\" ";
            saveLastDeckId(deck.getLong("id"));
        }
//        searchCards();
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save current search terms
        savedInstanceState.putString("mSearchTerms", mSearchTerms);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mSearchTerms = savedInstanceState.getString("mSearchTerms");
        searchCards();
    }


    private void invalidate() {
        CollectionTask.cancelAllTasks(SEARCH_CARDS);
        CollectionTask.cancelAllTasks(RENDER_BROWSER_QA);
        CollectionTask.cancelAllTasks(CHECK_CARD_SELECTION);
        mCards.clear();
        if (mCardsAdapter != null) {
            mCardsAdapter.getSelectedItemIds().clear();
            mCardsAdapter.getSelectedCards().clear();
            mCardsAdapter.notifyDataSetChanged();
        }
    }


    private void searchCards() {
        // cancel the previous search & render tasks if still running
        invalidate();
        String searchText;
        if (mSearchTerms == null) {
            mSearchTerms = "";
        }
        if (!"".equals(mSearchTerms) && (mSearchView != null)) {
            mSearchView.setQuery(mSearchTerms, false);
        }
        if (mSearchTerms == null) {
            Timber.i("update mSearchTerms text:%s", mSearchTerms);
            mSearchTerms = "";
        }
        Timber.i("show the final mSearchTerms text:%s", mSearchTerms);
        if (mSearchTerms.contains("deck:")) {
            searchText = mRestrictOnTab + " " + mSearchStudyFilter + " " + mSearchMarkFilter + " " + mSearchAnswerFilter + " " + mRangeSearchReviewTimesFilter + " " + mRangeCreateTimeFilter+ " " + mRangeSearchAnswerFilter + " " + mRangeSearchForgetTimesFilter + " " + mTagsFilter + " " + mSearchTerms;
        } else {
            searchText = mRestrictOnDeck + " " + mRestrictOnTab + " " + mSearchStudyFilter + " " + mSearchMarkFilter + " " + mSearchAnswerFilter + " " + mRangeSearchReviewTimesFilter + " " + mRangeCreateTimeFilter+ " " + mRangeSearchAnswerFilter + " " + mRangeSearchForgetTimesFilter + " " + mTagsFilter + " " + mSearchTerms;
        }
//        String showingText=searchText;
//        if(!mShowGrammarInSearchView){
//            searchText=mSearchTerms;
//        }
        Timber.i("mRestrictOnDeck： +%s+  mRestrictOnTab +%s+  mSearchStudyFilter +%s+  mSearchMarkFilter +%s+  mSearchAnswerFilter+%s+  mRangeSearchReviewTimesFilter + %s + mRangeCreateTimeFilter +%s+  mRangeSearchAnswerFilter +%s+  mRangeSearchForgetTimesFilter +%s+ mTagsFilter +%s+  mSearchTerms +%s+ ",
                mRestrictOnDeck, mRestrictOnTab, mSearchStudyFilter, mSearchMarkFilter, mSearchAnswerFilter, mRangeSearchReviewTimesFilter, mRangeCreateTimeFilter,mRangeSearchAnswerFilter, mRangeSearchForgetTimesFilter, mTagsFilter,mSearchTerms);
        Timber.i("show the final search text:%s", searchText);
        if (!mShowGrammarInSearchView) {
            mSearchView.setQuery(mSearchTerms, false);
        } else if (!"".equals(searchText) && (mSearchView != null)) {
            mSearchView.setQuery(searchText, false);
        }
        if (colIsOpen() && mCardsAdapter != null) {
            // clear the existing card list
            mCards = new ArrayList<>();

            mCardsAdapter.notifyDataSetChanged();
            //  estimate maximum number of cards that could be visible (assuming worst-case minimum row height of 20dp)
            int numCardsToRender = (int) Math.ceil(mCardsListView.getHeight() /
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics())) + 5;
            Timber.i("I wanna get %d cards", numCardsToRender);
            // Perform database query to get all card ids
            CollectionTask.launchCollectionTask(SEARCH_CARDS,
                    mSearchCardsHandler,
                    new TaskData(new Object[] {
                            searchText,
                            ((mOrder != CARD_ORDER_NONE)),
                            numCardsToRender,
                            mColumn1Index,
                            mColumn2Index
                    })
            );
        }
    }


    private void updateList() {
        mCardsAdapter.notifyDataSetChanged();
        mDropDownDeckAdapter.notifyDataSetChanged();
        onSelectionChanged();
    }


    /**
     * @return text to be used in the subtitle of the drop-down deck selector
     */
    public String getSubtitleText() {
        int count = getCardCount();
        return getResources().getQuantityString(R.plurals.card_browser_subtitle, count, count);
    }


    public static Map<Long, Integer> getPositionMap(List<CardCache> list) {
        Map<Long, Integer> positions = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            positions.put(list.get(i).getId(), i);
        }
        return positions;
    }


    // Iterates the drop down decks, and selects the one matching the given id
    private boolean selectDeckById(@NonNull Long deckId) {
        for (int dropDownDeckIdx = 0; dropDownDeckIdx < mDropDownDecks.size(); dropDownDeckIdx++) {
            if (mDropDownDecks.get(dropDownDeckIdx).getLong("id") == deckId) {
                selectDropDownItem(dropDownDeckIdx + 1);
                return true;
            }
        }
        return false;
    }


    // convenience method for updateCardsInList(...)
    private void updateCardInList(Card card, String updatedCardTags) {
        List<Card> cards = new ArrayList<>();
        cards.add(card);
        if (updatedCardTags != null) {
            Map<Long, String> updatedCardTagsMult = new HashMap<>();
            updatedCardTagsMult.put(card.getNid(), updatedCardTags);
            updateCardsInList(cards, updatedCardTagsMult);
        } else {
            updateCardsInList(cards, null);
        }
    }


    /**
     * Returns the decks which are valid targets for "Change Deck"
     */
    @VisibleForTesting
    List<Deck> getValidDecksForChangeDeck() {
        List<Deck> nonDynamicDecks = new ArrayList<>();
        for (Deck d : mDropDownDecks) {
            if (Decks.isDynamic(d)) {
                continue;
            }
            nonDynamicDecks.add(d);
        }
        return nonDynamicDecks;
    }


    List<String> mSelectedTags = new ArrayList<>();


    private void filterByTag(List<String> selectedTags, int option, TextView tag_selected) {
        //TODO: Duplication between here and CustomStudyDialog:customStudyFromTags
//        mSearchView.setQuery("", false);
        String tags = selectedTags.toString();
//        mSearchView.setQueryHint(getResources().getString(R.string.card_browser_tags_shown,
//                tags.substring(1, tags.length() - 1)));

        tag_selected.setText(getResources().getString(R.string.card_browser_tags_shown,
                tags.substring(1, tags.length() - 1)));

        mSelectedTags=selectedTags;
//        searchCards();
    }


    private static abstract class ListenerWithProgressBar extends TaskListenerWithContext<SelfStudyActivity> {
        public ListenerWithProgressBar(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull SelfStudyActivity browser) {
            browser.showProgressBar();
        }
    }



    /**
     * Does not leak Card Browser.
     */
    private static abstract class ListenerWithProgressBarCloseOnFalse extends ListenerWithProgressBar {
        private final String mTimber;


        public ListenerWithProgressBarCloseOnFalse(String timber, SelfStudyActivity browser) {
            super(browser);
            mTimber = timber;
        }


        public ListenerWithProgressBarCloseOnFalse(SelfStudyActivity browser) {
            this(null, browser);
        }


        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            if (mTimber != null) {
                Timber.d(mTimber);
            }
            if (result.getBoolean()) {
                actualOnValidPostExecute(browser, result);
            } else {
                browser.closeCardBrowser(DeckPicker.RESULT_DB_ERROR);
            }
        }


        protected abstract void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result);
    }


    /**
     * @param cards           Cards that were changed
     * @param updatedCardTags Mapping note id -> updated tags
     */
    private void updateCardsInList(List<Card> cards, Map<Long, String> updatedCardTags) {
        List<CardCache> cardList = getCards();
        Map<Long, Integer> idToPos = getPositionMap(cardList);
        for (Card c : cards) {
            // get position in the mCards search results HashMap
            Integer pos = idToPos.get(c.getId());
            if (pos == null || pos >= getCardCount()) {
                continue;
            }
            // update Q & A etc
            cardList.get(pos).load(true, 0, 1);
        }

        updateList();
    }


    private UpdateCardHandler updateCardHandler() {
        return new UpdateCardHandler(this);
    }


    private static class UpdateCardHandler extends ListenerWithProgressBarCloseOnFalse {
        public UpdateCardHandler(SelfStudyActivity browser) {
            super("Card Browser - UpdateCardHandler.actualOnPostExecute(CardBrowser browser)", browser);
        }


        @Override
        public void actualOnProgressUpdate(@NonNull SelfStudyActivity browser, TaskData value) {
            browser.updateCardInList(value.getCard(), value.getString());
        }


        @Override
        protected void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result) {
            browser.hideProgressBar();
        }
    }



    ;


    private ChangeDeckHandler changeDeckHandler() {
        return new ChangeDeckHandler(this);
    }


    private static class ChangeDeckHandler extends ListenerWithProgressBarCloseOnFalse {
        public ChangeDeckHandler(SelfStudyActivity browser) {
            super("Card Browser - changeDeckHandler.actualOnPostExecute(CardBrowser browser)", browser);
        }


        @Override
        protected void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result) {
            browser.hideProgressBar();

            browser.searchCards();
            browser.toggleMultiSelectMode(false);

            if (!result.getBoolean()) {
                Timber.i("changeDeckHandler failed, not offering undo");
                browser.displayCouldNotChangeDeck();
                return;
            }
            // snackbar to offer undo
            String deckName = browser.getCol().getDecks().name(browser.mNewDid);
            browser.mUndoSnackbar = UIUtils.showSnackbar(browser, String.format(browser.getString(R.string.changed_deck_message), deckName), SNACKBAR_DURATION, R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CollectionTask.launchCollectionTask(UNDO, browser.mUndoHandler);
                }
            }, browser.mCardsListView, null);
        }
    }



    ;


    @CheckResult
    private static String formatQA(String text, Context context) {
        boolean showFilenames = AnkiDroidApp.getSharedPrefs(context).getBoolean("card_browser_show_media_filenames", false);
        return formatQAInternal(text, showFilenames);
    }


    /**
     * @param txt           The text to strip HTML, comments, tags and media from
     * @param showFileNames Whether [sound:foo.mp3] should be rendered as " foo.mp3 " or  " "
     * @return The formatted string
     */
    @VisibleForTesting
    @CheckResult
    static String formatQAInternal(String txt, boolean showFileNames) {
        /* Strips all formatting from the string txt for use in displaying question/answer in browser */
        String s = txt;
        s = s.replaceAll("<!--.*?-->", "");
        s = s.replace("<br>", " ");
        s = s.replace("<br />", " ");
        s = s.replace("<div>", " ");
        s = s.replace("\n", " ");
        s = showFileNames ? Utils.stripSoundMedia(s) : Utils.stripSoundMedia(s, " ");
        s = s.replaceAll("\\[\\[type:[^]]+\\]\\]", "");
        s = showFileNames ? Utils.stripHTMLMedia(s) : Utils.stripHTMLMedia(s, " ");
        s = s.trim();
        return s;
    }


    /**
     * Removes cards from view. Doesn't delete them in model (database).
     */
    private void removeNotesView(Card[] cards, boolean reorderCards) {
        List<Long> cardIds = new ArrayList<>(cards.length);
        for (Card c : cards) {
            cardIds.add(c.getId());
        }
        removeNotesView(cardIds, reorderCards);
    }


    /**
     * Removes cards from view. Doesn't delete them in model (database).
     *
     * @param reorderCards Whether to rearrange the positions of checked items (DEFECT: Currently deselects all)
     */
    private void removeNotesView(java.util.Collection<Long> cardsIds, boolean reorderCards) {
        long reviewerCardId = getReviewerCardId();
        List<CardCache> oldMCards = getCards();
        Map<Long, Integer> idToPos = getPositionMap(oldMCards);
        Set<Long> idToRemove = new HashSet<Long>();
        for (Long cardId : cardsIds) {
            if (cardId == reviewerCardId) {
                mReloadRequired = true;
            }
            if (idToPos.containsKey(cardId)) {
                idToRemove.add(cardId);
            }
        }

        List<CardCache> newMCards = new ArrayList<>();
        int pos = 0;
        for (CardCache card : oldMCards) {
            if (!idToRemove.contains(card.getId())) {
                newMCards.add(new CardCache(card, pos++));
            }
        }
        mCards = newMCards;

        if (reorderCards) {
            //Suboptimal from a UX perspective, we should reorder
            //but this is only hit on a rare sad path and we'd need to rejig the data structures to allow an efficient
            //search
            Timber.w("Removing current selection due to unexpected removal of cards");
        }

        updateList();
    }


    private SuspendCardHandler suspendCardHandler() {
        return new SuspendCardHandler(this);
    }


    private static class SuspendCardHandler extends ListenerWithProgressBarCloseOnFalse {
        public SuspendCardHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        protected void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result) {
            Card[] cards = (Card[]) result.getObjArray();
            browser.updateCardsInList(Arrays.asList(cards), null);
            browser.hideProgressBar();
            browser.updateDeckNum();
            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
        }
    }



    ;


    private FlagCardHandler flagCardHandler() {
        return new FlagCardHandler(this);
    }


    private static class FlagCardHandler extends SuspendCardHandler {
        public FlagCardHandler(SelfStudyActivity browser) {
            super(browser);
        }
    }


    private MarkCardHandler markCardHandler() {
        return new MarkCardHandler(this);
    }


    private static class MarkCardHandler extends ListenerWithProgressBarCloseOnFalse {
        public MarkCardHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        protected void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result) {
            Card[] cards = (Card[]) result.getObjArray();
            browser.updateCardsInList(CardUtils.getAllCards(CardUtils.getNotes(Arrays.asList(cards))), null);
            browser.hideProgressBar();
            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
        }
    }



    ;

    private DeleteNoteHandler mDeleteNoteHandler = new DeleteNoteHandler(this);



    private static class DeleteNoteHandler extends ListenerWithProgressBarCloseOnFalse {
        public DeleteNoteHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull SelfStudyActivity browser) {
            super.actualOnPreExecute(browser);
            browser.invalidate();
        }


        @Override
        public void actualOnProgressUpdate(@NonNull SelfStudyActivity browser, TaskData value) {
            Card[] cards = (Card[]) value.getObjArray();
            //we don't need to reorder cards here as we've already deselected all notes,
            browser.removeNotesView(cards, false);
        }


        @Override
        protected void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result) {
            browser.hideProgressBar();
//            browser.mActionBarTitle.setText(Integer.toString(browser.checkedCardCount()));
            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
            // snackbar to offer undo
            browser.mUndoSnackbar = UIUtils.showSnackbar(browser, browser.getString(R.string.deleted_message), SNACKBAR_DURATION, R.string.undo, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CollectionTask.launchCollectionTask(UNDO, browser.mUndoHandler);
                }
            }, browser.mCardsListView, null);

            browser.searchCards();

        }
    }



    ;

    private final UndoHandler mUndoHandler = new UndoHandler(this);



    private static class UndoHandler extends ListenerWithProgressBarCloseOnFalse {
        public UndoHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnValidPostExecute(SelfStudyActivity browser, TaskData result) {
            Timber.d("Card Browser - mUndoHandler.actualOnPostExecute(CardBrowser browser)");
            browser.hideProgressBar();
            // reload whole view
            browser.searchCards();
            browser.endMultiSelectMode();
            browser.mCardsAdapter.notifyDataSetChanged();
            browser.invalidateOptionsMenu();    // maybe the availability of undo changed
        }
    }



    private final SearchCardsHandler mSearchCardsHandler = new SearchCardsHandler(this);

    private boolean mInitTabDeckNum = false;


    private void updateDeckNum() {
        if (getLastDeckId() == null) {
            return;
        }
        switch (getIntent().getIntExtra("type", 0)) {
            case TAB_STUDY_STATE:
                double[] count = calculateStudyState(getCol(), getLastDeckId());
                for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                    updateTabLayout(i, (int) count[i]);
                }
                break;
            case TAB_MARK_STATE:
                double[] count2 = calculateFlagNum(getCol(), getLastDeckId());
                for (int i = 0; i < mTabLayout.getTabCount(); i++) {//
                    updateTabLayout(i, (int) count2[i]);
                }
                break;
            case TAB_ANSWER_STATE:
                double[] count3 = calculateAnswerButtonNum(getCol(), getLastDeckId());
                for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                    updateTabLayout(i, (int) count3[i]);
                }
                break;
            case TAB_MAIN_STATE:
                double[] count4 = calculateMainButtonNum(getCol(), getLastDeckId());
                for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                    updateTabLayout(i, (int) count4[i]);
                }
                break;
            case TAB_CUSTOM_STATE:

                break;
        }
    }


    private double[] calculateStudyState(Collection col, long deckId) {
        //计算已熟悉/全部卡片数
        Stats stats = new Stats(col, deckId);
        return stats.calculateCardStudyState();
    }


    private double[] calculateFlagNum(Collection col, long deckId) {
        //计算旗子或marked的数目
        Stats stats = new Stats(col, deckId);
        return stats.calculateFlagNum();
    }


    private double[] calculateAnswerButtonNum(Collection col, long deckId) {
        //计算各个回答按钮的数目
        Stats stats = new Stats(col, deckId);
        return stats.calculateAnswerButtonNum();
    }


    private double[] calculateMainButtonNum(Collection col, long deckId) {
        //计算各个回答按钮的数目
        Stats stats = new Stats(col, deckId);
        return stats.calculateCardMainState();
    }


    private class SearchCardsHandler extends ListenerWithProgressBar {
        public SearchCardsHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            if (result != null) {
                mCards = result.getCards();
                updateList();
                handleSearchResult();
                if (!browser.mInitTabDeckNum) {
                    browser.mInitTabDeckNum = true;
                    updateDeckNum();
                }

            }
            hideProgressBar();
        }


        private void handleSearchResult() {
            Timber.i("CardBrowser:: Completed doInBackgroundSearchCards Successfully");
            updateList();

            if ((mSearchView == null) || mSearchView.isIconified()) {
                return;
            }

            if (hasSelectedAllDecks()) {
                UIUtils.showSimpleSnackbar(SelfStudyActivity.this, getSubtitleText(), true);
//                return;
            }

            //If we haven't selected all decks, allow the user the option to search all decks.
            String displayText = null;
            if (getCardCount() == 0) {
                displayText = getString(R.string.card_browser_no_cards_in_deck, getSelectedDeckNameForUi());
                mStartStudyButton.setVisibility(View.GONE);
            } else {
                mStartStudyButton.setVisibility(View.VISIBLE);
                displayText = getSubtitleText();
            }
//            ((TextView) findViewById(R.id.search_result_num)).setText("筛选出" + getCardCount() + "张卡片");
//            View root = SelfStudyActivity.this.findViewById(R.id.root_layout);
//            UIUtils.showSnackbar(SelfStudyActivity.this,
//                    displayText,
//                    SNACKBAR_DURATION,
//                    R.string.card_browser_search_all_decks,
//                    (v) -> searchAllDecks(),
//                    root,
//                    null);
//            UIUtils.showSimpleSnackbar(SelfStudyActivity.this,
//                    displayText,
//                   true);

        }


        @Override
        public void actualOnCancelled(@NonNull SelfStudyActivity browser) {
            super.actualOnCancelled(browser);
            hideProgressBar();
        }
    }



    ;


    public boolean hasSelectedAllDecks() {
        Long lastDeckId = getLastDeckId();
        return lastDeckId != null && lastDeckId == ALL_DECKS_ID;
    }


    public void searchAllDecks() {
        //all we need to do is select all decks
        selectAllDecks();
    }


    /**
     * Returns the current deck name, "All Decks" if all decks are selected, or "Unknown"
     * Do not use this for any business logic, as this will return inconsistent data
     * with the collection.
     */
    public String getSelectedDeckNameForUi() {
        try {
            Long lastDeckId = getLastDeckId();
            if (lastDeckId == null) {
                return getString(R.string.card_browser_unknown_deck_name);
            }
            if (lastDeckId == ALL_DECKS_ID) {
                return getString(R.string.card_browser_all_decks);
            }
            return getCol().getDecks().name(lastDeckId);
        } catch (Exception e) {
            Timber.w(e, "Unable to get selected deck name");
            return getString(R.string.card_browser_unknown_deck_name);
        }
    }


    private final RenderQAHandler mRenderQAHandler = new RenderQAHandler(this);



    private static class RenderQAHandler extends TaskListenerWithContext<SelfStudyActivity> {
        public RenderQAHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnProgressUpdate(@NonNull SelfStudyActivity browser, TaskData value) {
            // Note: This is called every time a card is rendered.
            // It blocks the long-click callback while the task is running, so usage of the task should be minimized
            browser.mCardsAdapter.notifyDataSetChanged();
        }


        @Override
        public void actualOnPreExecute(@NonNull SelfStudyActivity browser) {
            Timber.d("Starting Q&A background rendering");
        }


        @Override
        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            if (result != null) {
                if (result.getObjArray() != null && result.getObjArray().length > 1) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<Long> cardsIdsToHide = (List<Long>) result.getObjArray()[1];
                        if (cardsIdsToHide.size() > 0) {
                            Timber.i("Removing %d invalid cards from view", cardsIdsToHide.size());
                            browser.removeNotesView(cardsIdsToHide, true);
                        }
                    } catch (Exception e) {
                        Timber.e(e, "failed to hide cards");
                    }
                }
                browser.hideProgressBar();
                browser.mCardsAdapter.notifyDataSetChanged();
                Timber.d("Completed doInBackgroundRenderBrowserQA Successfuly");
            } else {
                // Might want to do something more proactive here like show a message box?
                Timber.e("doInBackgroundRenderBrowserQA was not successful... continuing anyway");
            }
        }


        @Override
        public void actualOnCancelled(@NonNull SelfStudyActivity browser) {
            browser.hideProgressBar();
        }
    }



    ;

    private final CheckSelectedCardsHandler mCheckSelectedCardsHandler = new CheckSelectedCardsHandler(this);



    private static class CheckSelectedCardsHandler extends ListenerWithProgressBar {
        public CheckSelectedCardsHandler(SelfStudyActivity browser) {
            super(browser);
        }


        @Override
        public void actualOnPostExecute(@NonNull SelfStudyActivity browser, TaskData result) {
            if (result == null) {
                return;
            }
            browser.hideProgressBar();

            Object[] resultArr = result.getObjArray();
            boolean hasUnsuspended = (boolean) resultArr[0];
            boolean hasUnmarked = (boolean) resultArr[1];

            int title;
            int icon;
            int[] attrs = new int[] {
                    R.attr.reviewMenuSuspendIconRef,//0
                    R.attr.reviewMenuUnSuspendIconRef,//1
                    R.attr.reviewMenuMarkIconRef,//2
            };
            TypedArray ta =  browser.obtainStyledAttributes(attrs);

            MenuItem suspend_item = browser.mActionBarMenu.findItem(R.id.action_suspend_card);
            if (hasUnsuspended) {
                title = R.string.card_browser_suspend_card;
//                icon = R.drawable.ic_action_suspend;
                suspend_item.setIcon(ta.getDrawable(0));
            } else {
                title = R.string.card_browser_unsuspend_card;
//                icon = R.drawable.ic_action_unsuspend;
                suspend_item.setIcon(ta.getDrawable(1));
            }
            suspend_item.setTitle(browser.getString(title));
//            suspend_item.setIcon(icon);


            MenuItem mark_item = browser.mActionBarMenu.findItem(R.id.action_mark_card);
            if (hasUnmarked) {
                title = R.string.card_browser_mark_card;
//                icon = R.drawable.ic_star_outline_white_24dp;
                mark_item.setIcon(ta.getDrawable(2));
            } else {
                title = R.string.card_browser_unmark_card;
//                icon = R.mipmap.mark_star_normal;
                mark_item.setIcon(R.mipmap.note_star_normal);
            }
            mark_item.setTitle(browser.getString(title));

            ta.recycle();
        }


        @Override
        public void actualOnCancelled(@NonNull SelfStudyActivity browser) {
            super.actualOnCancelled(browser);
            browser.hideProgressBar();
        }
    }


    private void closeCardBrowser(int result) {
        closeCardBrowser(result, null);
    }


    private void closeCardBrowser(int result, Intent data) {
        // Set result and finish
        setResult(result, data);
        finishWithAnimation(ActivityTransitionAnimation.RIGHT);
    }


    /**
     * Render the second column whenever the user stops scrolling
     */
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

//    private final class MultiColumnListAdapter extends BaseAdapter {
//        private final int mResource;
//        private Column[] mFromKeys;
//        private final int[] mToIds;
//        private float mOriginalTextSize = -1.0f;
//        private final int mFontSizeScalePcent;
//        private Typeface mCustomTypeface = null;
//        private LayoutInflater mInflater;
//
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
//            final int backgroundColor = Themes.getColorFromAttr(SelfStudyActivity.this, card.getColor());
//            v.setBackgroundColor(backgroundColor);
//            // setup checkbox to change color in multi-select mode
//            final CheckBox checkBox = (CheckBox) v.findViewById(R.id.card_checkbox);
//            // if in multi-select mode, be sure to show the checkboxes
//            if (inMultiSelectMode()) {
//                checkBox.setVisibility(View.VISIBLE);
//                if (mCheckedCards.contains(card)) {
//                    checkBox.setChecked(true);
//                } else {
//                    checkBox.setChecked(false);
//                }
//                // this prevents checkboxes from showing an animation from selected -> unselected when
//                // checkbox was selected, then selection mode was ended and now restarted
//                checkBox.jumpDrawablesToCurrentState();
//            } else {
//                checkBox.setChecked(false);
//                checkBox.setVisibility(View.GONE);
//            }
//            // change bg color on check changed
//            checkBox.setOnClickListener(view -> onCheck((CardCache) view.getTag(), v));
//        }
//
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
//    private void onCheck(CardCache card, View cell) {
//        CheckBox checkBox = (CheckBox) cell.findViewById(R.id.card_checkbox);
////        CardCache card = getCards().get(position);
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
//
//    private void onSelectAll() {
//        mCheckedCards.addAll(mCards);
//        onSelectionChanged();
//    }
//
//
//    private void onSelectNone() {
//        mCheckedCards.clear();
//        onSelectionChanged();
//    }
//
//
    private void onSelectionChanged() {
        Timber.d("onSelectionChanged()");
        try {
//            if (!inMultiSelectMode() && !mCardsAdapter.getSelectedCards().isEmpty()) {
//                //If we have selected cards, load multiselect
//                loadMultiSelectMode();
//            } else if (inMultiSelectMode() && mCheckedCards.isEmpty()) {
//                //If we don't have cards, unload multiselect
//                endMultiSelectMode();
//            }

            //If we're not in mutliselect, we can select cards if there are cards to select
//            if (!inMultiSelectMode() && this.mActionBarMenu != null) {
//                MenuItem selectAll = mActionBarMenu.findItem(R.id.action_select_all);
//                selectAll.setVisible(mCards != null && mCardsAdapter.getSelectedItemIds().size() != 0);
//            }

            if (!inMultiSelectMode()) {
                return;
            }

            updateMultiselectMenu();
//            mActionBarTitle.setText(Integer.toString(checkedCardCount()));
        } finally {
            mCardsAdapter.notifyDataSetChanged();
        }
    }


    private List<CardCache> getCards() {
        return mCards;
    }


    private long[] getAllCardIds() {
        long[] l = new long[mCards.size()];
        for (int i = 0; i < mCards.size(); i++) {
            l[i] = mCards.get(i).getId();
        }
        return l;
    }


    /**
     * Show/dismiss dialog when sd card is ejected/remounted (collection is saved by SdCardReceiver)
     */
    private void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(SdCardReceiver.MEDIA_EJECT)) {
                        finishWithoutAnimation();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(SdCardReceiver.MEDIA_EJECT);
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }

    /**
     * The views expand / contract when switching between multi-select mode so we manually
     * adjust so that the vertical position of the given view is maintained
     */
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


    /**
     * Turn on Multi-Select Mode so that the user can select multiple cards at once.
     */
//    private void loadMultiSelectMode() {
//        if (inMultiSelectMode()) {
//            return;
//        }
//        Timber.d("loadMultiSelectMode()");
//        // set in multi-select mode
//        inMultiSelectMode() = true;
//        // show title and hide spinner
//        mActionBarTitle.setVisibility(View.VISIBLE);
//        mActionBarTitle.setText(String.valueOf(checkedCardCount()));
////        mActionBarSpinner.setVisibility(View.GONE);
//        // reload the actionbar using the multi-select mode actionbar
//
//    }
    private void toggleMultiSelectMode(boolean enable) {
        if (mCardsAdapter != null) {
            mCardsAdapter.setMultiCheckable(enable);

        }
    }


    private boolean inMultiSelectMode() {
        return mCardsAdapter != null && mCardsAdapter.isMultiCheckableMode();
    }


    /**
     * Turn off Multi-Select Mode and return to normal state
     */
    private void endMultiSelectMode() {
        Timber.d("endMultiSelectMode()");
//        mCheckedCards.clear();
//        inMultiSelectMode() = false;
//         If view which was originally selected when entering multi-select is visible then maintain its position
//        View view = mCardsListView.getChildAt(mLastSelectedPosition - mCardsListView.getFirstVisiblePosition());
//        if (view != null) {
//            recenterListView(view);
//        }
//         update adapter to remove check boxes
//
//         update action bar
//        supportInvalidateOptionsMenu();
//        mActionBarSpinner.setVisibility(View.VISIBLE);
//        mActionBarTitle.setVisibility(View.GONE);

        mCardsAdapter.notifyDataSetChanged();
    }


    @VisibleForTesting
    public int checkedCardCount() {
        return mCardsAdapter != null ? mCardsAdapter.getSelectedItemIds().size() : 0;
    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        //should only be called from changeDeck()
    void executeChangeCollectionTask(long[] ids, long newDid) {
        mNewDid = newDid; //line required for unit tests, not necessary, but a noop in regular call.
        CollectionTask.launchCollectionTask(DISMISS_MULTI, new ChangeDeckHandler(this),
                new TaskData(new Object[] {ids, Collection.DismissType.CHANGE_DECK_MULTI, newDid}));
    }


}
