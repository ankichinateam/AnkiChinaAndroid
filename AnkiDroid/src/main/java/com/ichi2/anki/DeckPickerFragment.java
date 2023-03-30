/****************************************************************************************
 * Copyright (c) 2009 Andrew Dubya <andrewdubya@gmail.com>                              *
 * Copyright (c) 2009 Nicolas Raoul <nicolas.raoul@gmail.com>                           *
 * Copyright (c) 2009 Edu Zamora <edu.zasu@gmail.com>                                   *
 * Copyright (c) 2009 Daniel Svard <daniel.svard@gmail.com>                             *
 * Copyright (c) 2010 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>
 *                                                                                      *
 * getAnkiActivity() program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU General Public License as published by the Free Software        *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * getAnkiActivity() program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU General Public License along with         *
 * getAnkiActivity() program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.SQLException;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ActionMenuView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.JsonObject;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.CustomStudyDialog;
import com.ichi2.anki.dialogs.DatabaseErrorDialog;
import com.ichi2.anki.dialogs.DeckPickerContextMenu;
import com.ichi2.anki.dialogs.ImportDialog;
import com.ichi2.anki.dialogs.MediaCheckDialog;
import com.ichi2.anki.dialogs.SyncErrorDialog;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.anki.widgets.DeckAdapter;
import com.ichi2.anki.widgets.DeckInfoListAdapter;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.Connection;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.libanki.sync.AnkiChinaSyncer;
import com.ichi2.ui.BadgeDrawableBuilder;
import com.ichi2.ui.SettingItem;
import com.ichi2.utils.OKHttpUtil;
import com.ichi2.utils.Permissions;
import com.ichi2.utils.SyncStatus;
import com.ichi2.widget.SwipeItemLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.anki.AnkiActivity.REQUEST_REVIEW;
import static com.ichi2.anki.DeckPicker.REQUEST_STORAGE_PERMISSION;
import static com.ichi2.anki.SelfStudyActivity.ALL_DECKS_ID;
import static com.ichi2.async.CollectionTask.TASK_TYPE.LOAD_COLLECTION_COMPLETE;
import static com.ichi2.async.CollectionTask.TASK_TYPE.LOAD_DECK_COUNTS;
import static com.ichi2.async.CollectionTask.TASK_TYPE.LOAD_DECK_QUICK;
import static com.ichi2.async.CollectionTask.TASK_TYPE.UNDO;

public class DeckPickerFragment extends AnkiFragment {

    final protected Boolean mFragmented = false;
    private static final int SWIPE_TO_SYNC_TRIGGER_DISTANCE = 400;

    // Short animation duration from system
    private int mShortAnimDuration;

//    private RelativeLayout mDeckPickerContent;


    private View mStudyoptionsFrame;
    private RecyclerView mRecyclerView;
    private LinearLayoutManager mRecyclerViewLayoutManager;
    private DeckAdapter mDeckListAdapter;
    private FloatingActionsMenu mActionsMenu;
    private Snackbar.Callback mSnackbarShowHideCallback = new Snackbar.Callback();

//    private LinearLayout mNoDecksPlaceholder;


    public void updatePullToSyncWrapper(boolean refreshing) {
        if (mPullToSyncWrapper != null) {
            mPullToSyncWrapper.setRefreshing(refreshing);
        }
    }


    private View mSyncMenuItemActionView;
    private TextView mSyncMediaPercent;

    private Handler mSyncingMediaHandler;
    private Handler mSyncingDataHandler;


    public void onSync(boolean syncing) {
        Timber.i("on syncing:" + syncing + ",loading view is init:" + mSyncMenuItem);
        if (mSyncMenuItem != null) {
            if (syncing) {
                mSyncMenuItemActionView = mSyncMenuItem.getActionView();
                mSyncMenuItem
                        .setActionView(R.layout.actionbar_indeterminate_progress);
                mSyncMediaPercent = mSyncMenuItem.getActionView().findViewById(R.id.sync_percent);
                mSyncMediaPercent.setText(String.format("%.0f", mCachedProgress) + "%");
                if (mSyncingDataHandler == null) {
                    mSyncingDataHandler = new Handler();
                    mSyncingDataHandler.postDelayed(() -> {
                        if (AnkiChinaSyncer.SYNCING) {
                            Toast.makeText(getAnkiActivity(), "如果同步时间过长，请执行【检查数据】功能", Toast.LENGTH_LONG).show();
                            mSyncingDataHandler = null;
                        }
                    },  60*1000);
                }
            } else {
                mCachedProgress=1;
                mSyncMenuItem.setActionView(mSyncMenuItemActionView);
            }
        }

    }


    double mCachedProgress;
    @SuppressLint("DefaultLocale")
    public void updateSyncingPercent(double progress) {
        if (mSyncMediaPercent != null) {
            mSyncMediaPercent.setText(String.format("%.0f", progress) + "%");
            mCachedProgress=progress;
            if (mSyncingMediaHandler == null) {
                mSyncingMediaHandler = new Handler();
                mSyncingMediaHandler.postDelayed(() -> {
                    if (AnkiChinaSyncer.SYNCING) {
                        Toast.makeText(getAnkiActivity(), "正在同步媒体文件", Toast.LENGTH_SHORT).show();
                        mSyncingMediaHandler = null;
                    }
                }, 3000);
            }
        }

    }


    private SwipeRefreshLayout mPullToSyncWrapper;

    private TextView mReviewSummaryTextView;


    private EditText mDialogEditText;


    // flag keeping track of when the app has been paused


    protected List<AbstractDeckTreeNode> mDueTree;


    /**
     * Keep track of which deck was last given focus in the deck list. If we find that getAnkiActivity() value
     * has changed between deck list refreshes, we need to recenter the deck list to the new current
     * deck.
     */
    private long mFocusedDeck;

    private boolean initOnResume = true;

//    private boolean mSyncOnStart = true;


    @Override
    public void onResume() {
        super.onResume();
        Timber.i("on resume");
        if (getAnkiActivity() == null) {
            return;
        }
        if (mToolbar != null) {
            getAnkiActivity().setSupportActionBar(mToolbar);
        }
        mToolbar.setNavigationOnClickListener(v -> onNavigationPressed());
        if (mTabLayout != null) {
            mTabLayout.selectTab(mTabLayout.getTabAt(0));
        }
        if (!Permissions.hasStorageAccessPermission(getContext())) {
            return;
        } else {
            mPullToSyncWrapper.setVisibility(View.VISIBLE);
            mToolbar.setVisibility(View.VISIBLE);
            mRoot.findViewById(R.id.no_permission_layout).setVisibility(View.GONE);
        }
        if (getAnkiActivity().colIsOpen() && initOnResume) {
            initOnResume = false;
//            selectNavigationItem(R.id.nav_decks);
            if (mDueTree == null) {
                updateDeckList(true);
            }
        }
//        else {
//            //            int index = -1;
////            Timber.i("mDueTree size:"+mDeckListAdapter.getDeckList().size());
////            for (int i = 0; i < mDeckListAdapter.getDeckList().size(); i++) {
////                if (getCol().getDecks().current().getLong("id") == mDeckListAdapter.getDeckList().get(i).getDid()) {
////                    index = i;
////                    Timber.i("mDueTree current did:"+getCol().getDecks().current().getLong("id"));
////                }
////            }
////            if (index > -1) {
////                mDeckListAdapter.getDeckList().add(index,getCol().getSched().updateDeck(getCol().getDecks().get(mDeckListAdapter.getDeckList().get(index).getDid())));
////                mDeckListAdapter.getDeckList().remove(index + 1);
//////                mDeckListAdapter.getDeckList().remove()
//////                mDeckListAdapter.notifyItemChanged(index);
////                mDeckListAdapter.notifyDataSetChanged();
////
////            }
//
//        }
        updateDeckList();


        /** Complete task and enqueue fetching nonessential data for
         * startup. */
        CollectionTask.launchCollectionTask(LOAD_COLLECTION_COMPLETE);
//        if (mSyncOnStart && Consts.loginAnkiChina()) {
//            mSyncOnStart = false;
//            getAnkiActivity().sync();
//        }
        setHasOptionsMenu(true);
    }


    private String mVipUrl;
    private boolean mVip;
    private int mVipDay;
    private String mVipExpireAt;
    private ImageView mVipLogo;


    //VIP页面地址在登录和非登录态是不一样的，如有APP登录或者登出操作请重新调此接口更新URL
    protected void onRefreshVipState(boolean isVip, String vipUrl, int vipDay, String vipExpireAt) {
        Timber.e("on refresh vip state:" + isVip);
        mVip = isVip;
        mVipUrl = vipUrl;
        mVipDay = vipDay;
        mVipExpireAt = vipExpireAt;
        if (mVipLogo != null) {
            mVipLogo.setImageResource(mVip ? R.mipmap.supre_xueba_logo_normal : R.mipmap.super_heroes_normal);
        }
//        mToolbar.setNavigationIcon(ResourcesCompat.getDrawable(getAnkiActivity().getResources(),mVip?R.mipmap.supre_xueba_vip_kaitong_normal:R.mipmap.supre_xueba_logo_normal,null) );
//        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
//        int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, displayMetrics);
//        mToolbar.setPadding(mVip?0:padding,0,0,0);
    }


    // ----------------------------------------------------------------------------
    // LISTENERS
    // ----------------------------------------------------------------------------

    private final OnClickListener mDeckExpanderClickListener = view -> {
        Long did = (Long) view.getTag();
        if (getCol().getDecks().children(did).size() > 0) {
            getCol().getDecks().collpase(did);
            __renderPage();
            getAnkiActivity().dismissAllDialogFragments();
        }
    };

    private final OnClickListener mDeckClickListener = v -> onDeckClick(v, true);

    private final OnClickListener mCountsClickListener = v -> onDeckClick(v, true);


    private void onDeckClick(View v, boolean dontSkipStudyOptions) {
        long deckId = (long) v.getTag();
        Timber.i("DeckPicker:: Selected deck with id %d", deckId);
        if (mActionsMenu != null && mActionsMenu.isExpanded()) {
            mActionsMenu.collapse();
        }

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
        getAnkiActivity().setContextMenuDid(deckId);
        showDialogFragment(DeckPickerContextMenu.newInstance(deckId));
        return true;
    };


    public void showDialogFragment(DialogFragment newFragment) {
        AnkiActivity.showDialogFragment(getAnkiActivity(), newFragment);
    }


    // ----------------------------------------------------------------------------
    // ANDROID ACTIVITY METHODS
    // ----------------------------------------------------------------------------

    private View mRoot;
    private Toolbar mToolbar;
    private TabLayout mTabLayout;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRoot != null) {
            return mRoot;
        }
        Timber.i("on create view in deck picker fragment");
        mRoot = inflater.inflate(R.layout.deck_picker, container, false);
        mToolbar = mRoot.findViewById(R.id.toolbar);
        mPullToSyncWrapper = mRoot.findViewById(R.id.pull_to_sync_wrapper);
        if (!Permissions.hasStorageAccessPermission(getAnkiActivity())) {
            mPullToSyncWrapper.setVisibility(View.GONE);
            mToolbar.setVisibility(View.GONE);
            mRoot.findViewById(R.id.no_permission_layout).setVisibility(View.VISIBLE);
            mRoot.findViewById(R.id.hint_button).setOnClickListener(v -> ActivityCompat.requestPermissions(getAnkiActivity(), new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION));
//            return mRoot;

        }
        if (mToolbar != null) {
            setHasOptionsMenu(true);
            mToolbar.setNavigationIcon(null);
            mToolbar.setTitle("");
            mTabLayout = mToolbar.findViewById(R.id.tab_layout);
            mVipLogo = mToolbar.findViewById(R.id.vip_logo);
            mVipLogo.setOnClickListener(v -> onNavigationPressed());
            TabLayout.Tab tab = mTabLayout.newTab();
            View view = getLayoutInflater().inflate(R.layout.item_deckpicker_tab, null);
            ((TextView) view.findViewById(R.id.name)).setText("学习");
            tab.setCustomView(view);
            mTabLayout.addTab(tab);
            TabLayout.Tab tab2 = mTabLayout.newTab();
            View view2 = getLayoutInflater().inflate(R.layout.item_deckpicker_tab, null);
            ((TextView) view2.findViewById(R.id.name)).setText("笔记");
            tab2.setCustomView(view2);
            mTabLayout.addTab(tab2);
            mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab == tab2) {
                        getAnkiActivity().openCardBrowser(ALL_DECKS_ID);
                    }
                }


                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }


                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        }
        // check, if tablet layout
        mStudyoptionsFrame = mRoot.findViewById(R.id.studyoptions_fragment);
        if (mFragmented) {
            loadStudyOptionsFragment(false);
        }
        mRecyclerView = mRoot.findViewById(R.id.files);
        mRecyclerViewLayoutManager = new LinearLayoutManager(getAnkiActivity());
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);
        View view = mFragmented ? mRoot.findViewById(R.id.deckpicker_view) : mRoot.findViewById(R.id.root_layout);
        boolean hasDeckPickerBackground = false;
        try {
            hasDeckPickerBackground = applyDeckPickerBackground(view);
        } catch (OutOfMemoryError e) { //6608 - OOM should be catchable here.
            Timber.w(e, "Failed to apply background - OOM");
            UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.background_image_too_large), false);
        } catch (Exception e) {
            Timber.w(e, "Failed to apply background");
            UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.failed_to_apply_background_image, e.getLocalizedMessage()), false);
        }
        // create and set an adapter for the RecyclerView
        mDeckListAdapter = new DeckAdapter(getLayoutInflater(), getAnkiActivity());
        mDeckListAdapter.setDeckClickListener(mDeckClickListener);
        mDeckListAdapter.setCountsClickListener(mCountsClickListener);
        mDeckListAdapter.setDeckExpanderClickListener(mDeckExpanderClickListener);
        mDeckListAdapter.setDeckLongClickListener(mDeckLongClickListener);
        mDeckListAdapter.enablePartialTransparencyForBackground(hasDeckPickerBackground);
        mDeckListAdapter.setMarketClickListener(v -> getAnkiActivity().openSourceMarket());

        mRecyclerView.setAdapter(mDeckListAdapter);

        mPullToSyncWrapper.setDistanceToTriggerSync(SWIPE_TO_SYNC_TRIGGER_DISTANCE);
        mPullToSyncWrapper.setOnRefreshListener(() -> {
            Timber.i("Pull to Sync: Syncing");
            mPullToSyncWrapper.setRefreshing(false);
            updateDeckList();
//            getAnkiActivity().sync();
        });
        mPullToSyncWrapper.getViewTreeObserver().addOnScrollChangedListener(() ->
                mPullToSyncWrapper.setEnabled(mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition() == 0));

        // Setup the FloatingActionButtons, should work everywhere with min API >= 15
        mActionsMenu = mRoot.findViewById(R.id.add_content_menu);
        mActionsMenu.findViewById(R.id.fab_expand_menu_button).setContentDescription(getString(R.string.menu_add));
//        configureFloatingActionsMenu();

        mReviewSummaryTextView = mRoot.findViewById(R.id.today_stats_text_view);
//        mRoot.findViewById(R.id.tv_resource).setOnClickListener(v -> getAnkiActivity().openSourceMarket());

        mShortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        fetchAds();


//        mRecyclerView.setOnTouchListener(new TouchListener());


        return mRoot;
    }


    public DeckPicker getAnkiActivity() {
        return (DeckPicker) super.getAnkiActivity();
    }


    private void fetchAds() {
        OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "common/ad", "", "", fetchAdsListener);
//        Connection.sendCommonGet(fetchAdsListener, new Connection.Payload("common/ad", "", Connection.Payload.REST_TYPE_GET, HostNumFactory.getInstance(getContext())));
    }


    private OKHttpUtil.MyCallBack fetchAdsListener = new OKHttpUtil.MyCallBack() {
        @Override
        public void onFailure(Call call, IOException e) {

        }


        @Override
        public void onResponse(Call call, String token, Object arg1, Response response) {
            if (response.isSuccessful()) {
                JSONObject ads;
                try {
                    ads = (new JSONObject(response.body().string())).getJSONObject("data");
                    Timber.i("fetch ads successfully!:%s ", ads.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Timber.e("fetch ads failed, error code %d", response.code());
                    return;
                }
                try {
                    final JSONObject mainAd = ads.getJSONObject("text_ad");
                    String mainAdsText = mainAd.getString("text");
                    String mainAdsLinkUrl = mainAd.getString("link_url");
                    SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getAnkiActivity());
                    if (!preferences.getString(Consts.KEY_MAIN_AD_LINK, "").equals(mainAdsLinkUrl) || !preferences.getString(Consts.KEY_MAIN_AD_TEXT, "").equals(mainAdsText)) {
                        getAnkiActivity().runOnUiThread(() -> {
                            mDeckListAdapter.updateAds(mainAdsText, mainAdsLinkUrl);
                            mDeckListAdapter.setAdClickListener(v -> WebViewActivity.openUrlInApp(getAnkiActivity(), mainAdsLinkUrl, ""));
                        });
                    }
                    preferences.edit().putString(Consts.KEY_MAIN_AD_LINK, mainAdsLinkUrl).putString(Consts.KEY_MAIN_AD_TEXT, mainAdsText).apply();
                } catch (Exception e) {
                    e.printStackTrace();
//                    getAnkiActivity().findViewById(R.id.main_ad_layout).setVisibility(View.GONE);
                }
                try {
                    final JSONObject optionAd = ads.getJSONObject("image_ad");
                    DeckInfoListAdapter.AD_IMAGE_URL = optionAd.getString("image_url");
                    DeckInfoListAdapter.AD_LINK_URL = optionAd.getString("link_url");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Timber.e("fetch ads failed, error code %d", response.code());
            }
        }
    };


    public Collection getCol() {
        return getAnkiActivity().getCol();
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu in deck fragment ");
        inflater.inflate(R.menu.deck_picker, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        Timber.d("on prepare options menu:" + mToolbar.getMenu() + "," + menu);
        if (!Permissions.hasStorageAccessPermission(getContext())) {
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_STORAGE_PERMISSION);
            return;
        }
        initMenu(menu);
        prepareMenu(menu);
        super.onPrepareOptionsMenu(menu);
    }


    private void prepareMenu(Menu menu) {
        if (mFragmented || !getCol().undoAvailable()) {
            menu.findItem(R.id.action_undo).setVisible(false);
        } else {
            Resources res = getResources();
            menu.findItem(R.id.action_undo).setVisible(true);
            String undo = res.getString(R.string.studyoptions_congrats_undo, getCol().undoName(res));
            menu.findItem(R.id.action_undo).setTitle(undo);
        }
    }


    MenuItem mSyncMenuItem;


    private void initMenu(Menu menu) {
        if (menu != null && menu.findItem(R.id.action_sync) != null) {
            mSyncMenuItem = menu.findItem(R.id.action_sync);
            onSync(AnkiChinaSyncer.SYNCING);
        }
        if (CollectionHelper.getInstance().getColSafe(getAnkiActivity()) == null) {
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_STORAGE_PERMISSION);
            return;
        }
        boolean sdCardAvailable = AnkiDroidApp.isSdCardMounted();

        if (menu != null && menu.findItem(R.id.action_sync) != null &&
                menu.findItem(R.id.action_new_filtered_deck) != null &&
                menu.findItem(R.id.action_check_database) != null &&
                menu.findItem(R.id.action_check_media) != null &&
                menu.findItem(R.id.action_empty_cards) != null) {
            menu.findItem(R.id.action_sync).setEnabled(sdCardAvailable);
            menu.findItem(R.id.action_new_filtered_deck).setEnabled(sdCardAvailable);
            menu.findItem(R.id.action_check_database).setEnabled(sdCardAvailable);
            menu.findItem(R.id.action_check_media).setEnabled(sdCardAvailable);
            menu.findItem(R.id.action_empty_cards).setEnabled(sdCardAvailable);
        }


        // I haven't had an exception here, but it feels getAnkiActivity() may be flaky
//        try {
//            displaySyncBadge(menu);
//        } catch (Exception e) {
//            Timber.w(e, "Error Displaying Sync Badge");
//        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Timber.i("on attach");
    }


    @Override
    public void onDetach() {
        super.onDetach();
        Timber.i("on detach");
    }


    private void onNavigationPressed() {
        Timber.i("onNavigationPressed");
        if (mVipUrl == null || mVipUrl.isEmpty()) {
            getAnkiActivity().getVipInfo();
        } else {
            getAnkiActivity().openVipUrl(mVipUrl);
        }

//        getAnkiActivity().getAccount().getToken(getAnkiActivity(), new MyAccount.TokenCallback() {
//            @Override
//            public void onSuccess(String token) {
//                RequestBody formBody = new FormBody.Builder()
//                        .add("phone", "13126359689")
//                        .build();
//                OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/deleteServerData", formBody, token, "", new OKHttpUtil.MyCallBack() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//
//                    }
//
//
//                    @Override
//                    public void onResponse(Call call, String token, Object arg1, Response response) {
//                        if (response.isSuccessful()) {
//                            try {
//                                final com.ichi2.utils.JSONObject object = new com.ichi2.utils.JSONObject(response.body().string());
//                                Timber.i("object:%s", object.toString());
//
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        } else {
//                            Timber.e("deleteServerData error, code %d", response.code());
//                        }
//                    }
//                });
//
//            }
//
//
//            @Override
//            public void onFail(String message) {
//
//            }
//        });
    }


    // throws doesn't seem to be checked by the compiler - consider it to be documentation
    private boolean applyDeckPickerBackground(View view) throws OutOfMemoryError {
        //Allow the user to clear data and get back to a good state if they provide an invalid background.
        if (!AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getBoolean("deckPickerBackground", false)) {
            Timber.d("No DeckPicker background preference");
//            view.setBackgroundResource(0);
            return false;
        }
        String currentAnkiDroidDirectory = CollectionHelper.getCurrentAnkiDroidDirectory(getAnkiActivity());
        File imgFile = new File(currentAnkiDroidDirectory, "DeckPickerBackground.png");
        if (!imgFile.exists()) {
            Timber.d("No DeckPicker background image");
//            view.setBackgroundResource(0);
            return false;
        } else {
            Timber.i("Applying background");
            Drawable drawable = Drawable.createFromPath(imgFile.getAbsolutePath());
            view.setBackground(drawable);
            return true;
        }
    }


    private void displaySyncBadge(Menu menu) {
        MenuItem syncMenu = menu.findItem(R.id.action_sync);
        SyncStatus syncStatus = SyncStatus.getSyncStatus(getAnkiActivity()::getCol);
        Timber.d("SyncStatus：" + syncStatus);
        switch (syncStatus) {
            case BADGE_DISABLED:
            case NO_CHANGES:
            case INCONCLUSIVE:
                BadgeDrawableBuilder.removeBadge(syncMenu);
                syncMenu.setTitle(R.string.sync_menu_title);
                break;
            case HAS_CHANGES:
                // Light orange icon
                new BadgeDrawableBuilder(getResources())
                        .withColor(ContextCompat.getColor(getAnkiActivity(), R.color.badge_warning))
                        .replaceBadge(syncMenu);
                syncMenu.setTitle(R.string.sync_menu_title);
                break;
            case NO_ACCOUNT:
            case FULL_SYNC:
                if (syncStatus == SyncStatus.NO_ACCOUNT) {
                    syncMenu.setTitle(R.string.sync_menu_title_no_account);
                } else if (syncStatus == SyncStatus.FULL_SYNC) {
                    syncMenu.setTitle(R.string.sync_menu_title_full_sync);
                }
                // Orange-red icon with exclamation mark
                new BadgeDrawableBuilder(getResources())
                        .withText('!')
                        .withColor(ContextCompat.getColor(getAnkiActivity(), R.color.badge_error))
                        .replaceBadge(syncMenu);
                break;
            default:
                Timber.w("Unhandled sync status: %s", syncStatus);
                syncMenu.setTitle(R.string.sync_title);
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!Permissions.hasStorageAccessPermission(getContext())) {
            getAnkiActivity().firstCollectionOpen();
            return false;
        }
        Resources res = getResources();
        switch (item.getItemId()) {
            case R.id.action_undo:
                Timber.i("DeckPicker:: Undo button pressed");
                undo();
                return true;

            case R.id.action_sync:
                Timber.i("DeckPicker:: Sync button pressed，sync state：%s", AnkiChinaSyncer.SYNCING);
                if (!AnkiChinaSyncer.SYNCING) {
                    getAnkiActivity().sync(true);
                }
                return true;

            case R.id.action_import:
                Timber.i("DeckPicker:: Import button pressed");
                getAnkiActivity().showImportDialog(ImportDialog.DIALOG_IMPORT_HINT);
                return true;

            case R.id.action_new_filtered_deck: {
                Timber.i("DeckPicker:: New filtered deck button pressed");
                mDialogEditText = new EditText(getAnkiActivity());
                ArrayList<String> names = getCol().getDecks().allNames();
                int n = 1;
                String name = String.format(Locale.getDefault(), "%s %d", res.getString(R.string.filtered_deck_name), n);
                while (names.contains(name)) {
                    n++;
                    name = String.format(Locale.getDefault(), "%s %d", res.getString(R.string.filtered_deck_name), n);
                }
                mDialogEditText.setText(name);
                // mDialogEditText.setFilters(new InputFilter[] { mDeckNameFilter });
                new MaterialDialog.Builder(getAnkiActivity())
                        .title(res.getString(R.string.new_deck))
                        .customView(mDialogEditText, true)
                        .positiveText(res.getString(R.string.create))
                        .negativeText(res.getString(R.string.dialog_cancel))
                        .onPositive((dialog, which) -> {
                            String filteredDeckName = mDialogEditText.getText().toString();
                            if (!Decks.isValidDeckName(filteredDeckName)) {
                                Timber.i("Not creating deck with invalid name '%s'", filteredDeckName);
                                UIUtils.showThemedToast(getAnkiActivity(), getString(R.string.invalid_deck_name), false);
                                return;
                            }
                            Timber.i("DeckPicker:: Creating filtered deck...");
                            getCol().getDecks().newDyn(filteredDeckName);
                            openStudyOptions(true);
                        })
                        .show();
                return true;
            }
            case R.id.action_check_database:
                Timber.i("DeckPicker:: Check database button pressed");
                getAnkiActivity().showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CONFIRM_DATABASE_CHECK);
                return true;

            case R.id.action_check_media:
                Timber.i("DeckPicker:: Check media button pressed");
                getAnkiActivity().showMediaCheckDialog(MediaCheckDialog.DIALOG_CONFIRM_MEDIA_CHECK);
                return true;

            case R.id.action_empty_cards:
                Timber.i("DeckPicker:: Empty cards button pressed");
                getAnkiActivity().handleEmptyCards();
                return true;

            case R.id.action_model_browser_open: {
                Timber.i("DeckPicker:: Model browser button pressed");
                Intent noteTypeBrowser = new Intent(getAnkiActivity(), ModelBrowser.class);
                getAnkiActivity().startActivityForResultWithAnimation(noteTypeBrowser, 0, ActivityTransitionAnimation.LEFT);
                return true;
            }
            case R.id.action_restore_backup:
                Timber.i("DeckPicker:: Restore from backup button pressed");
                getAnkiActivity().showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_CONFIRM_RESTORE_BACKUP);
                return true;

            case R.id.action_export: {
                Timber.i("DeckPicker:: Export collection button pressed");
                getAnkiActivity().showExportDialog();
                return true;
            }
            case R.id.action_browser: {
                Timber.i("DeckPicker:: Old browser button pressed");
                getAnkiActivity().openOldCardBrowser();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);

        }
    }


    //    /**
//     * Called when the activity is first created.
//     */
    @Override
    public void onCreate(Bundle savedInstanceState) throws SQLException {
        Timber.d("onCreate()");


        // Then set theme and content view
        super.onCreate(savedInstanceState);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();

//        if(mRoot!=null){
//            ((ViewGroup)mRoot.getParent()).removeView(mRoot);
//        }
        Timber.d("onDestroy()");
    }


    // ----------------------------------------------------------------------------
    // CUSTOM METHODS
    // ----------------------------------------------------------------------------


    @Override
    public void onCollectionLoaded(Collection col) {

    }


    private UndoTaskListener undoTaskListener(boolean isReview) {
        return new UndoTaskListener(isReview, this);
    }


    private static class UndoTaskListener extends TaskListenerWithContext<DeckPickerFragment> {
        private final boolean isReview;


        public UndoTaskListener(boolean isReview, DeckPickerFragment deckPicker) {
            super(deckPicker);
            this.isReview = isReview;
        }


        @Override
        public void actualOnCancelled(@NonNull DeckPickerFragment deckPicker) {
            deckPicker.getAnkiActivity().hideProgressBar();
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPickerFragment deckPicker) {
            deckPicker.getAnkiActivity().showProgressBar();
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPickerFragment deckPicker, TaskData result) {
            deckPicker.getAnkiActivity().hideProgressBar();
            Timber.i("Undo completed");
            if (isReview) {
                Timber.i("Review undone - opening reviewer.");
                deckPicker.openReviewer();
            }
        }
    }


    private void undo() {
        Timber.i("undo()");
        String undoReviewString = getResources().getString(R.string.undo_action_review);
        final boolean isReview = undoReviewString.equals(getCol().undoName(getResources()));
        TaskListener listener = undoTaskListener(isReview);
        CollectionTask.launchCollectionTask(UNDO, listener);
    }


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
            getAnkiActivity().startStudyOption(withDeckOptions);
        }
    }


    public void notifyDataSetChanged() {
        Timber.i("notify data set changed");
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
        openStudyOptions(false);
//        int pos = mDeckListAdapter.findDeckPosition(did);
//        AbstractDeckTreeNode deckDueTreeNode = mDeckListAdapter.getDeckList().get(pos);
//        if (!deckDueTreeNode.shouldDisplayCounts() || deckDueTreeNode.knownToHaveRep()) {
//            // If we don't yet have numbers, we trust the user that they knows what they opens, tries to open it.
//            // If there is nothing to review, it'll come back to deck picker.
//            openReviewerOrStudyOptions(dontSkipStudyOptions);
//            return;
//        }
//        // There are numbers
//        // Figure out what action to take
//        if (getCol().getSched().hasCardsTodayAfterStudyAheadLimit()) {
//            // If there are cards due that can't be studied yet (due to the learn ahead limit) then go to study options
//            openStudyOptions(false);
//        } else if (getCol().getSched().newDue() || getCol().getSched().revDue()) {
//            // If there are no cards to review because of the daily study limit then give "Study more" option
//            UIUtils.showSnackbar(getAnkiActivity(), R.string.studyoptions_limit_reached, false, R.string.study_more, v -> {
//                CustomStudyDialog d = CustomStudyDialog.newInstance(
//                        CustomStudyDialog.CONTEXT_MENU_LIMITS,
//                        getCol().getDecks().selected(), true, getAnkiActivity());
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
//                        getCol().getDecks().selected(), true, getAnkiActivity());
//                showDialogFragment(d);
//            }, getView().findViewById(R.id.root_layout), mSnackbarShowHideCallback);
//            if (mFragmented) {
//                openStudyOptions(false);
//            } else {
//                updateDeckList();
//            }
//        }
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


    private final UpdateDeckListListener updateDeckListListener() {
        return new UpdateDeckListListener(this);
    }


    private static class UpdateDeckListListener extends TaskListenerWithContext<DeckPickerFragment> {
        public UpdateDeckListListener(DeckPickerFragment deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPickerFragment deckPicker) {
            if (!deckPicker.getAnkiActivity().colIsOpen()) {
                deckPicker.getAnkiActivity().showProgressBar();
            }
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPickerFragment deckPicker, TaskData result) {
            Timber.i("Updating deck list UI");
            deckPicker.getAnkiActivity().hideProgressBar();
            // Make sure the fragment is visible
            if (deckPicker.mFragmented) {
                deckPicker.mStudyoptionsFrame.setVisibility(View.VISIBLE);
            }
            if (result == null) {
                Timber.e("null result loading deck counts");
                deckPicker.getAnkiActivity().showCollectionErrorDialog();
                return;
            }
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
//        CollectionHelper.getInstance().closeCollection(true,"refresh");
        TaskListener listener = updateDeckListListener();
        CollectionTask.TASK_TYPE taskType = quick ? LOAD_DECK_QUICK : LOAD_DECK_COUNTS;
        CollectionTask.launchCollectionTask(taskType, listener);
    }


    private void __renderPage() {
        Timber.i("render page");
        if (mDueTree == null) {
            // mDueTree may be set back to null when the activity restart.
            // We may need to recompute it.

            updateDeckList();
            return;
        }

        // Check if default deck is the only available and there are no cards
//        boolean isEmpty = mDueTree.size() == 1 && mDueTree.get(0).getDid() == 1 && getCol().isEmpty();

//        if (getAnkiActivity().animationDisabled()) {
//            mDeckPickerContent.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
//            mNoDecksPlaceholder.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
//        } else {
//            float translation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
//                    getResources().getDisplayMetrics());
//
//            boolean decksListShown = mDeckPickerContent.getVisibility() == View.VISIBLE;
//            boolean placeholderShown = mNoDecksPlaceholder.getVisibility() == View.VISIBLE;
//
//            if (isEmpty) {
//                if (decksListShown) {
//                    fadeOut(mDeckPickerContent, mShortAnimDuration, translation);
//                }
//
//                if (!placeholderShown) {
//                    fadeIn(mNoDecksPlaceholder, mShortAnimDuration, translation)
//                            // getAnkiActivity() is some bad choreographing here
//                            .setStartDelay(decksListShown ? mShortAnimDuration * 2 : 0);
//                }
//            } else {
//                if (!decksListShown) {
//                    fadeIn(mDeckPickerContent, mShortAnimDuration, translation)
//                            .setStartDelay(placeholderShown ? mShortAnimDuration * 2 : 0);
//                }
//
//                if (placeholderShown) {
//                    fadeOut(mNoDecksPlaceholder, mShortAnimDuration, translation);
//                }
//            }
//        }

//        if (isEmpty) {
//            if (getAnkiActivity().getSupportActionBar() != null) {
//                getAnkiActivity().getSupportActionBar().setSubtitle(null);
//            }
//            // We're done here
//            return;
//        }

        mDeckListAdapter.buildDeckList(mDueTree, getCol());
        // Set the "x due in y minutes" subtitle
//        try {
////            Integer eta = mDeckListAdapter.getEta();
//            Integer newCard = mDeckListAdapter.getNewCard();
//            Integer needReviewCard = mDeckListAdapter.getReviewCard();
////            Resources res = getResources();
////            if (getCol().cardCount() != -1) {
////                String time = "-";
////                String unit = "";
////                if (eta != -1 && eta != null) {
//////                    time = Utils.timeQuantityTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
////                    time = Utils.timeQuantityNumTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
////                    unit = Utils.timeQuantityUnitTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
////                }
//            int eta = (newCard + needReviewCard) * 10 / 60;
//            if ((newCard + needReviewCard) % 60 != 0) {
//                eta++;
//            }
//
////                ((TextView) mRoot.findViewById(R.id.new_card_num)).setText();
////                ((TextView) mRoot.findViewById(R.id.review_card_num)).setText();
////                ((TextView) mRoot.findViewById(R.id.cost_time)).setText(time);
////                ((TextView) mRoot.findViewById(R.id.cost_time_unit)).setText("预计耗时(" + unit + ")");
////            }
//        } catch (RuntimeException e) {
//            Timber.e(e, "RuntimeException setting time remaining");
//        }
        mDeckListAdapter.updateHeaderData();
        if (getCol().getDecks().current() != null) {
            long current = getCol().getDecks().current().optLong("id");
            if (mFocusedDeck != current) {
                scrollDecklistToDeck(current);
                mFocusedDeck = current;
            }
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
    @SuppressWarnings("deprecation")
    public void onAttachFragment(@NonNull Fragment childFragment) {
        super.onAttachFragment(childFragment);
        if (!mFragmented) {
            Window window = getAnkiActivity().getWindow();
            window.setFormat(PixelFormat.RGBA_8888);
        }

    }


    private void openReviewer() {
        Intent reviewer = new Intent(getAnkiActivity(), Reviewer.class);
        getAnkiActivity().startActivityForResultWithAnimation(reviewer, REQUEST_REVIEW, ActivityTransitionAnimation.LEFT);
    }


}
