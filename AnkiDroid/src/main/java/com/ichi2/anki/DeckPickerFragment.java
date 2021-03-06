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

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.SQLException;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.JsonObject;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.CustomStudyDialog;
import com.ichi2.anki.dialogs.DatabaseErrorDialog;
import com.ichi2.anki.dialogs.DeckPickerContextMenu;
import com.ichi2.anki.dialogs.ImportDialog;
import com.ichi2.anki.dialogs.MediaCheckDialog;
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
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.ui.BadgeDrawableBuilder;
import com.ichi2.ui.SettingItem;
import com.ichi2.utils.Permissions;
import com.ichi2.utils.SyncStatus;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import timber.log.Timber;

import static com.ichi2.anki.AnkiActivity.REQUEST_REVIEW;
import static com.ichi2.anki.DeckPicker.fadeIn;
import static com.ichi2.anki.DeckPicker.fadeOut;
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


    @Override
    public void onResume() {
        super.onResume();
        if (getAnkiActivity() == null) {
            return;
        }
        if (getAnkiActivity().mSyncOnResume) {
            Timber.i("Performing Sync on Resume");
            getAnkiActivity().sync();
            getAnkiActivity().mSyncOnResume = false;
        } else if (getAnkiActivity().colIsOpen()) {
//            selectNavigationItem(R.id.nav_decks);
            if (mDueTree == null) {
                updateDeckList(true);
            }
            updateDeckList();
        }
        /** Complete task and enqueue fetching nonessential data for
         * startup. */
        CollectionTask.launchCollectionTask(LOAD_COLLECTION_COMPLETE);
        getAnkiActivity().supportInvalidateOptionsMenu();
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


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        if (mRoot != null) {
            return mRoot;
        }
        mRoot = inflater.inflate(R.layout.deck_picker, container, false);
        mToolbar = mRoot.findViewById(R.id.toolbar);
        if (mToolbar != null) {
            mToolbar.inflateMenu(R.menu.deck_picker);
            getAnkiActivity().setSupportActionBar(mToolbar);
            getAnkiActivity().getSupportActionBar().setTitle(null);
            TypedValue value = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.problemRef, value, true);
            mToolbar.setNavigationIcon(value.resourceId);
            // Decide which action to take when the navigation button is tapped.
            mToolbar.setNavigationOnClickListener(v -> onNavigationPressed());
            TextView title = mToolbar.findViewById(R.id.toolbar_title);
            title.setOnClickListener(v -> getAnkiActivity().openCardBrowser());
            title.setVisibility(View.VISIBLE);
            title.setText("卡牌浏览器");
        }
        // check, if tablet layout
        mStudyoptionsFrame = mRoot.findViewById(R.id.studyoptions_fragment);
        if (mFragmented) {
            loadStudyOptionsFragment(false);
        }
//        mDeckPickerContent = mRoot.findViewById(R.id.deck_picker_content);
        mRecyclerView = mRoot.findViewById(R.id.files);
//        mNoDecksPlaceholder = mRoot.findViewById(R.id.no_decks_placeholder);
//        mDeckPickerContent.setVisibility(View.GONE);
        mRecyclerViewLayoutManager = new LinearLayoutManager(getAnkiActivity());
        mRecyclerView.setLayoutManager(mRecyclerViewLayoutManager);
        TypedArray ta = getContext().obtainStyledAttributes(new int[] {R.attr.deckDivider});
        Drawable divider = ta.getDrawable(0);
        ta.recycle();
//        DividerItemDecoration dividerDecorator = new DividerItemDecoration(getAnkiActivity(), mRecyclerViewLayoutManager.getOrientation());
//        dividerDecorator.setDrawable(divider);
//        mRecyclerView.addItemDecoration(dividerDecorator);
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
        mPullToSyncWrapper = mRoot.findViewById(R.id.pull_to_sync_wrapper);
        mPullToSyncWrapper.setDistanceToTriggerSync(SWIPE_TO_SYNC_TRIGGER_DISTANCE);
        mPullToSyncWrapper.setOnRefreshListener(() -> {
            Timber.i("Pull to Sync: Syncing");
            mPullToSyncWrapper.setRefreshing(false);
            getAnkiActivity().sync();
        });
        mPullToSyncWrapper.getViewTreeObserver().addOnScrollChangedListener(() ->
                mPullToSyncWrapper.setEnabled(mRecyclerViewLayoutManager.findFirstCompletelyVisibleItemPosition() == 0));

        // Setup the FloatingActionButtons, should work everywhere with min API >= 15
        mActionsMenu = mRoot.findViewById(R.id.add_content_menu);
        mActionsMenu.findViewById(R.id.fab_expand_menu_button).setContentDescription(getString(R.string.menu_add));
//        configureFloatingActionsMenu();

        mReviewSummaryTextView = (TextView) mRoot.findViewById(R.id.today_stats_text_view);
//        mRoot.findViewById(R.id.tv_resource).setOnClickListener(v -> getAnkiActivity().openSourceMarket());

        mShortAnimDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

//        Handler delay=new Handler();
        new Handler().postDelayed(this::fetchAds, 1000);
        return mRoot;
    }


    private void fetchAds() {
        Connection.sendCommonGet(fetchAdsListener, new Connection.Payload("common/ad", "", Connection.Payload.REST_TYPE_GET, HostNumFactory.getInstance(getContext())));
    }


    private final Connection.TaskListener fetchAdsListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {

        }


        @Override
        public void onPostExecute(Connection.Payload data) {
            if (data.success) {
                Timber.i("fetch ads successfully!:%s", data.result);
                try {
                    final JSONObject ads = ((JSONObject) data.result).getJSONObject("data");
                    final JSONObject mainAd = ads.getJSONObject("text_ad");
                    String mainAdsText = mainAd.getString("text");
                    String mainAdsLinkUrl = mainAd.getString("link_url");
                    SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getAnkiActivity());
                    if (!preferences.getString(Consts.KEY_MAIN_AD_LINK, "").equals(mainAdsLinkUrl) || !preferences.getString(Consts.KEY_MAIN_AD_TEXT, "").equals(mainAdsText)) {
                        getAnkiActivity().runOnUiThread(() -> {
                            mDeckListAdapter.updateAds(mainAdsText, mainAdsLinkUrl);
                            mDeckListAdapter.setAdClickListener(v ->WebViewActivity.openUrlInApp(getAnkiActivity(), mainAdsLinkUrl,  "") );
                        });
                    }
                    preferences.edit().putString(Consts.KEY_MAIN_AD_LINK,mainAdsLinkUrl).putString(Consts.KEY_MAIN_AD_TEXT,mainAdsText).apply();
                } catch (Exception e) {
                    e.printStackTrace();
//                    getAnkiActivity().findViewById(R.id.main_ad_layout).setVisibility(View.GONE);
                }
                try {
                    final JSONObject ads = ((JSONObject) data.result).getJSONObject("data");
                    final JSONObject optionAd = ads.getJSONObject("image_ad");
                    DeckInfoListAdapter.AD_IMAGE_URL = optionAd.getString("image_url");
                    DeckInfoListAdapter.AD_LINK_URL = optionAd.getString("link_url");
                } catch (Exception e) {
                    e.printStackTrace();
//                    getAnkiActivity().findViewById(R.id.main_ad_layout).setVisibility(View.GONE);
                }
            } else {
                Timber.e("fetch ads failed, error code %d", data.statusCode);
//                getAnkiActivity().findViewById(R.id.main_ad_layout).setVisibility(View.GONE);
            }
        }


        @Override
        public void onDisconnected() {
//            getAnkiActivity().findViewById(R.id.main_ad_layout).setVisibility(View.GONE);
        }
    };


//    public static void removeBadge(MenuItem menuItem,Toolbar mToolbar) {
//        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return;
//        }
//
//        Drawable icon = menuItem.getIcon();
//        if (icon instanceof BadgeDrawable) {
//            BadgeDrawable bd = (BadgeDrawable) icon;
//            menuItem.setIcon(bd.getCurrent());
//            mToolbar.setNavigationIcon(bd.getDrawable());
//            Timber.d("Badge removed,%s", menuItem.getItemId());
//        }
//    }


    public Collection getCol() {
        return getAnkiActivity().getCol();
    }


    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // Null check to prevent crash when col inaccessible
        if (!Permissions.hasStorageAccessPermission(getContext())) {
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_STORAGE_PERMISSION);
            return;
        }
        prepareMenu(mToolbar.getMenu());
        super.onPrepareOptionsMenu(menu);
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        Timber.d("onCreateOptionsMenu in deck fragment");
        initMenu(mToolbar.getMenu());
        super.onCreateOptionsMenu(menu, inflater);
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


    private void initMenu(Menu menu) {
        if (CollectionHelper.getInstance().getColSafe(getAnkiActivity()) == null) {
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                    REQUEST_STORAGE_PERMISSION);
            return;
        }
        boolean sdCardAvailable = AnkiDroidApp.isSdCardMounted();
        Timber.d("onCreateOptionsMenu in deck fragment,sdCardAvailable:" + sdCardAvailable);
        menu.findItem(R.id.action_sync).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_new_filtered_deck).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_check_database).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_check_media).setEnabled(sdCardAvailable);
        menu.findItem(R.id.action_empty_cards).setEnabled(sdCardAvailable);

        // I haven't had an exception here, but it feels getAnkiActivity() may be flaky
        try {
            displaySyncBadge(menu);
        } catch (Exception e) {
            Timber.w(e, "Error Displaying Sync Badge");
        }
    }


    private void onNavigationPressed() {
        getAnkiActivity().openInstructions();
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
                Timber.i("DeckPicker:: Sync button pressed");
                getAnkiActivity().sync();
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
        setHasOptionsMenu(true);

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
            Timber.d("Refreshing deck list");
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
        TaskListener listener = updateDeckListListener();
        CollectionTask.TASK_TYPE taskType = quick ? LOAD_DECK_QUICK : LOAD_DECK_COUNTS;
        CollectionTask.launchCollectionTask(taskType, listener);
    }


    private void __renderPage() {
        if (mDueTree == null) {
            // mDueTree may be set back to null when the activity restart.
            // We may need to recompute it.
            updateDeckList();
            return;
        }

        // Check if default deck is the only available and there are no cards
        boolean isEmpty = mDueTree.size() == 1 && mDueTree.get(0).getDid() == 1 && getCol().isEmpty();

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

        if (isEmpty) {
            if (getAnkiActivity().getSupportActionBar() != null) {
                getAnkiActivity().getSupportActionBar().setSubtitle(null);
            }
            // We're done here
            return;
        }

        mDeckListAdapter.buildDeckList(mDueTree, getCol());
        // Set the "x due in y minutes" subtitle
        try {
//            Integer eta = mDeckListAdapter.getEta();
            Integer newCard = mDeckListAdapter.getNewCard();
            Integer needReviewCard = mDeckListAdapter.getReviewCard();
            Resources res = getResources();
//            if (getCol().cardCount() != -1) {
//                String time = "-";
//                String unit = "";
//                if (eta != -1 && eta != null) {
////                    time = Utils.timeQuantityTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
//                    time = Utils.timeQuantityNumTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
//                    unit = Utils.timeQuantityUnitTopDeckPicker(AnkiDroidApp.getInstance(), eta * 60);
//                }
                int eta = (newCard + needReviewCard) * 10 / 60;
                if ((newCard + needReviewCard) % 60 != 0) {
                    eta++;
                }
                mDeckListAdapter.updateHeaderData("" + newCard, "" + needReviewCard, String.valueOf(eta));
//                ((TextView) mRoot.findViewById(R.id.new_card_num)).setText();
//                ((TextView) mRoot.findViewById(R.id.review_card_num)).setText();
//                ((TextView) mRoot.findViewById(R.id.cost_time)).setText(time);
//                ((TextView) mRoot.findViewById(R.id.cost_time_unit)).setText("预计耗时(" + unit + ")");
//            }
        } catch (RuntimeException e) {
            Timber.e(e, "RuntimeException setting time remaining");
        }

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


    private void openReviewer() {
        Intent reviewer = new Intent(getAnkiActivity(), Reviewer.class);
        getAnkiActivity().startActivityForResultWithAnimation(reviewer, REQUEST_REVIEW, ActivityTransitionAnimation.LEFT);
    }


}
