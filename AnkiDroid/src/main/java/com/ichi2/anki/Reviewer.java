/****************************************************************************************
 * Copyright (c) 2011 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2014 Bruno Romero de Azevedo <brunodea@inf.ufsm.br>                    *
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
// TODO: implement own menu? http://www.codeproject.com/Articles/173121/Android-Menus-My-Way

package com.ichi2.anki;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ActionProvider;
import androidx.core.view.MenuItemCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.ConfirmationDialog;
import com.ichi2.anki.multimediacard.AudioView;
import com.ichi2.anki.dialogs.RescheduleDialog;
import com.ichi2.anki.reviewer.PeripheralKeymap;
import com.ichi2.anki.reviewer.ReviewerUi;
import com.ichi2.anki.workarounds.FirefoxSnackbarWorkaround;
import com.ichi2.async.CollectionTask;
import com.ichi2.anki.reviewer.ActionButtons;
import com.ichi2.async.TaskListener;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Collection.DismissType;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.themes.Themes;
import com.ichi2.utils.FunctionalInterfaces.Consumer;
import com.ichi2.utils.Permissions;
import com.ichi2.widget.WidgetStatus;

import java.lang.ref.WeakReference;
import java.util.Arrays;

import timber.log.Timber;


import static com.ichi2.anki.reviewer.CardMarker.*;
import static com.ichi2.anki.reviewer.CardMarker.FLAG_NONE;
import static com.ichi2.anki.cardviewer.ViewerCommand.COMMAND_NOTHING;
import static com.ichi2.async.CollectionTask.TASK_TYPE.*;

import com.ichi2.async.TaskData;


public class Reviewer extends AbstractFlashcardViewer {
    private boolean mHasDrawerSwipeConflicts = false;
    private boolean mShowWhiteboard = true;
    private boolean mBlackWhiteboard = true;
    private boolean mPrefFullscreenReview = false;
    private static final int ADD_NOTE = 12;
    private static final int REQUEST_AUDIO_PERMISSION = 0;
    private LinearLayout colorPalette;

    // TODO: Consider extracting to ViewModel
    // Card counts
    private SpannableString newCount;
    private SpannableString lrnCount;
    private SpannableString revCount;

    private TextView mTextBarNew;
    private TextView mTextBarLearn;
    private TextView mTextBarReview;

    private boolean mPrefHideDueCount;

    // ETA
    private int eta;
    private boolean mPrefShowETA;


    // Preferences from the collection
    private boolean mShowRemainingCardCount;

    private ActionButtons mActionButtons = new ActionButtons(this);


    private TaskListener mRescheduleCardHandler = new ScheduleCollectionTaskListener() {
        protected int getToastResourceId() {
            return R.plurals.reschedule_cards_dialog_acknowledge;
        }
    };

    private TaskListener mResetProgressCardHandler = new ScheduleCollectionTaskListener() {
        protected int getToastResourceId() {
            return R.plurals.reset_cards_dialog_acknowledge;
        }
    };

    @VisibleForTesting
    protected PeripheralKeymap mProcessor = new PeripheralKeymap(this, this);
    @Override
    protected boolean shouldChangeToolbarBgLikeCss2(){
        return true;
    }

    /**
     * We need to listen for and handle reschedules / resets very similarly
     */
    abstract class ScheduleCollectionTaskListener extends NextCardHandler {

        abstract protected int getToastResourceId();


        @Override
        public void onPostExecute(TaskData result) {
            super.onPostExecute(result);
            invalidateOptionsMenu();
            int cardCount = result.getObjArray().length;
            UIUtils.showThemedToast(Reviewer.this,
                    getResources().getQuantityString(getToastResourceId(), cardCount, cardCount), true);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");
        super.onCreate(savedInstanceState);

        if (FirefoxSnackbarWorkaround.handledLaunchFromWebBrowser(getIntent(), this)) {
            this.setResult(RESULT_CANCELED);
            finishWithAnimation(ActivityTransitionAnimation.RIGHT);
            return;
        }

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            Timber.d("onCreate() :: received Intent with action = %s", getIntent().getAction());
            selectDeckFromExtra();
        }
        colorPalette = (LinearLayout) findViewById(R.id.whiteboard_pen_color);

        startLoadingCollection();
        findViewById(R.id.review_count_layout).setVisibility(View.VISIBLE);
        mTitle.setVisibility(View.GONE);


    }


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    @Override
    protected int getStatusBarColorAttr() {
        return R.attr.reviewStatusBarColor;
    }


    @Override
    protected int getFlagToDisplay() {
        int actualValue = super.getFlagToDisplay();
        if (actualValue == FLAG_NONE) {
            return FLAG_NONE;
        }
        Boolean isShownInActionBar = mActionButtons.isShownInActionBar(ActionButtons.RES_FLAG);
        if (isShownInActionBar != null && isShownInActionBar) {
            return FLAG_NONE;
        }
        return actualValue;
    }


    @Override
    protected boolean shouldDisplayMark() {
        boolean markValue = super.shouldDisplayMark();
        if (!markValue) {
            return false;
        }
        Boolean isShownInActionBar = mActionButtons.isShownInActionBar(ActionButtons.RES_MARK);
        //If we don't know, show it.
        //Otherwise, if it's in the action bar, don't show it again.
        return isShownInActionBar == null || !isShownInActionBar;
    }


    private void selectDeckFromExtra() {
        Bundle extras = getIntent().getExtras();
        long did = Long.MIN_VALUE;
        if (extras != null) {
            did = extras.getLong("deckId", Long.MIN_VALUE);
        }

        if (did == Long.MIN_VALUE) {
            // deckId is not set, load default
            return;
        }

        Timber.d("selectDeckFromExtra() with deckId = %d", did);

        // Clear the undo history when selecting a new deck
        if (getCol().getDecks().selected() != did) {
            getCol().clearUndo();
        }
        // Select the deck
        getCol().getDecks().select(did);
        // Reset the schedule so that we get the counts for the currently selected deck
        getCol().getSched().deferReset();
    }


    @Override
    protected void setTitle() {
//        String title;
//        if (colIsOpen()) {
//            title = Decks.basename(getCol().getDecks().current().getString("name"));
//        } else {
//            Timber.e("Could not set title in reviewer because collection closed");
//            title = "";
//        }
//        getSupportActionBar().setTitle(title);
//        mTitle.setText(title);
//        super.setTitle(title);
//        getSupportActionBar().setSubtitle("");
    }


    @Override
    protected int getContentViewAttr(int fullscreenMode) {
        return R.layout.reviewer;
    }


    @Override
    protected void onCollectionLoaded(Collection col) {
        super.onCollectionLoaded(col);
        // Load the first card and start reviewing. Uses the answer card
        // task to load a card, but since we send null
        // as the card to answer, no card will be answered.

        mPrefWhiteboard = MetaDB.getWhiteboardState(this, getParentDid());
        if (mPrefWhiteboard) {
            //DEFECT: Slight inefficiency here, as we set the database using these methods
            boolean whiteboardVisibility = MetaDB.getWhiteboardVisibility(this, getParentDid());
            setWhiteboardEnabledState(true);
            setWhiteboardVisibility(whiteboardVisibility);
        }

        col.getSched().deferReset();     // Reset schedule in case card was previously loaded
        getCol().startTimebox();
        CollectionTask.launchCollectionTask(ANSWER_CARD, mAnswerCardHandler(false),
                new TaskData(null, 0));

        disableDrawerSwipeOnConflicts();
        // Add a weak reference to current activity so that scheduler can talk to to Activity
        mSched.setContext(new WeakReference<Activity>(this));

        // Set full screen/immersive mode if needed
        if (mPrefFullscreenReview) {
            CompatHelper.getCompat().setFullScreen(this);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_layout_config:
                Timber.i("Reviewer:: Action layout button pressed (from menu)");
                showLayoutDialog( );
                break;
            case R.id.action_back:
                Timber.i("Reviewer:: Action back button pressed (from menu)");
                if (mShowWhiteboard && mWhiteboard != null && !mWhiteboard.undoEmpty()) {
                    mWhiteboard.undo();
                } else {
                    undo();
                }
                break;
            case android.R.id.home:
                Timber.i("Reviewer:: Home button pressed");
                closeReviewer(RESULT_OK, true);
                break;

            case R.id.action_undo:
                Timber.i("Reviewer:: Undo button pressed");
                if (mShowWhiteboard && mWhiteboard != null && !mWhiteboard.undoEmpty()) {
                    mWhiteboard.undo();
                } else {
                    undo();
                }
                break;

            case R.id.action_reset_card_progress:
                Timber.i("Reviewer:: Reset progress button pressed");
                showResetCardDialog();
                break;

            case R.id.action_mark_card:
                Timber.i("Reviewer:: Mark button pressed");
                onMark(mCurrentCard);
                break;
//            case R.id.action_replay:
//                Timber.i("Reviewer:: Replay audio button pressed (from menu)");
//                playSounds(true);
//                break;
            case R.id.action_toggle_mic_tool_bar:
                Timber.i("Reviewer:: Record mic");
                // Check permission to record and request if not granted
                if (!Permissions.canRecordAudio(this)) {
                    ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.RECORD_AUDIO},
                            REQUEST_AUDIO_PERMISSION);
                } else {
                    toggleMicToolBar();
                }
                break;

            case R.id.action_tag:
                Timber.i("Reviewer:: Tag button pressed");
                showTagsDialog();
                break;

            case R.id.action_edit:
                Timber.i("Reviewer:: Edit note button pressed");
                return editCard();

            case R.id.action_bury:
                Timber.i("Reviewer:: Bury button pressed");
                if (!MenuItemCompat.getActionProvider(item).hasSubMenu()) {
                    Timber.d("Bury card due to no submenu");
                    dismiss(DismissType.BURY_CARD);
                }
                break;
            case R.id.action_flip_enable:
                Timber.i("Reviewer:: Flip card button pressed");
                AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(Consts.KEY_FLIP_CARD,!AnkiDroidApp.getSharedPrefs(this).getBoolean(Consts.KEY_FLIP_CARD,false)).apply();
                refreshActionBar();
                if(sDisplayAnswer)displayCardAnswer();else displayCardQuestion();
                break;

            case R.id.action_suspend:
                Timber.i("Reviewer:: Suspend button pressed");
                if (!MenuItemCompat.getActionProvider(item).hasSubMenu()) {
                    Timber.d("Suspend card due to no submenu");
                    dismiss(DismissType.SUSPEND_CARD);
                }
                break;

            case R.id.action_delete:
                Timber.i("Reviewer:: Delete note button pressed");
                showDeleteNoteDialog();
                break;

            case R.id.action_change_whiteboard_pen_color:
                Timber.i("Reviewer:: Pen Color button pressed");
                if (colorPalette.getVisibility() == View.GONE) {
                    colorPalette.setVisibility(View.VISIBLE);
                } else {
                    colorPalette.setVisibility(View.GONE);
                }
                break;

            case R.id.action_save_whiteboard:
                Timber.i("Reviewer:: Save whiteboard button pressed");
                if (mWhiteboard != null) {
                    try {
                        String savedWhiteboardFileName = mWhiteboard.saveWhiteboard(getCol().getTime());
                        UIUtils.showThemedToast(Reviewer.this, getString(R.string.white_board_image_saved, savedWhiteboardFileName), true);
                    } catch (Exception e) {
                        UIUtils.showThemedToast(Reviewer.this, getString(R.string.white_board_image_save_failed, e.getLocalizedMessage()), true);
                    }
                }
                break;

            case R.id.action_clear_whiteboard:
                Timber.i("Reviewer:: Clear whiteboard button pressed");
                if (mWhiteboard != null) {
                    mWhiteboard.clear();
                }
                break;

            case R.id.action_hide_whiteboard:
                // toggle whiteboard visibility
                Timber.i("Reviewer:: Whiteboard visibility set to %b", !mShowWhiteboard);
                setWhiteboardVisibility(!mShowWhiteboard);
                refreshActionBar();
                break;

            case R.id.action_toggle_remark:
                boolean enable = AnkiDroidApp.getSharedPrefs(getBaseContext()).getBoolean("enable_remark", true);
                if (enable) {
                    mRemark.setVisibility(View.GONE);
                    item.setTitle("启用助记");
                } else {
                    mRemark.setVisibility(View.VISIBLE);
                    item.setTitle("禁用助记");
                }
                AnkiDroidApp.getSharedPrefs(getBaseContext()).edit().putBoolean("enable_remark", !enable).apply();
                break;

            case R.id.action_toggle_whiteboard:
                toggleWhiteboard();
                break;

            case R.id.action_search_dictionary:
                Timber.i("Reviewer:: Search dictionary button pressed");
                lookUpOrSelectText();
                break;

            case R.id.action_open_deck_options:
                Intent i = new Intent(this, DeckOptions.class);
                startActivityForResultWithAnimation(i, DECK_OPTIONS, ActivityTransitionAnimation.FADE);
                break;
            case R.id.action_button_config:
                Intent i2 = Preferences.getPreferenceSubscreenIntent(this, "com.ichi2.anki.prefs.custom_buttons");
                startActivityForResultWithAnimation(i2,REFRESH_TOP_BUTTONS, ActivityTransitionAnimation.FADE);

                break;
            case R.id.action_controller_config:
                Intent i4 = Preferences.getPreferenceSubscreenIntent(this, "com.ichi2.anki.prefs.custom_controller_buttons");
                startActivityForResultWithAnimation(i4,REFRESH_CONTROLLER, ActivityTransitionAnimation.FADE);

                break;
            case R.id.action_gesture:
                Intent i3 = Preferences.getPreferenceSubscreenIntent(this, "com.ichi2.anki.prefs.gestures");
                startActivityForResultWithAnimation(i3,REFRESH_GESTURE, ActivityTransitionAnimation.FADE);

                break;
//            case R.id.action_select_tts:
//                Timber.i("Reviewer:: Select TTS button pressed");
//                showSelectTtsDialogue();
//                break;

            case R.id.action_add_note_reviewer:
                Timber.i("Reviewer:: Add note button pressed");
                addNote();
                break;

            case R.id.action_flag_zero:
                Timber.i("Reviewer:: No flag");
                onFlag(mCurrentCard, FLAG_NONE);
                break;
            case R.id.action_flag_one:
                Timber.i("Reviewer:: Flag one");
                onFlag(mCurrentCard, FLAG_RED);
                break;
            case R.id.action_flag_two:
                Timber.i("Reviewer:: Flag two");
                onFlag(mCurrentCard, FLAG_ORANGE);
                break;
            case R.id.action_flag_three:
                Timber.i("Reviewer:: Flag three");
                onFlag(mCurrentCard, FLAG_GREEN);
                break;
            case R.id.action_flag_four:
                Timber.i("Reviewer:: Flag four");
                onFlag(mCurrentCard, FLAG_BLUE);
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    protected void toggleWhiteboard() {
        // toggle whiteboard enabled state (and show/hide whiteboard item in action bar)
        mPrefWhiteboard = !mPrefWhiteboard;
        Timber.i("Reviewer:: Whiteboard enabled state set to %b", mPrefWhiteboard);
        //Even though the visibility is now stored in its own setting, we want it to be dependent
        //on the enabled status
        setWhiteboardEnabledState(mPrefWhiteboard);
        setWhiteboardVisibility(mPrefWhiteboard);
        if (!mPrefWhiteboard) {
            colorPalette.setVisibility(View.GONE);
        }
        refreshActionBar();
    }


    private void toggleMicToolBar() {
        if (mMicToolBar != null) {
            // It exists swap visibility status
            if (mMicToolBar.getVisibility() != View.VISIBLE) {
                mMicToolBar.setVisibility(View.VISIBLE);
            } else {
                mMicToolBar.setVisibility(View.GONE);
            }
        } else {
            // Record mic tool bar does not exist yet
            mTempAudioPath = AudioView.generateTempAudioFile(this);
            if (mTempAudioPath == null) {
                return;
            }
            mMicToolBar = AudioView.createRecorderInstance(this, R.drawable.av_play, R.drawable.av_pause,
                    R.drawable.av_stop, R.drawable.av_rec, R.drawable.av_rec_stop, mTempAudioPath);
            if (mMicToolBar == null) {
                mTempAudioPath = null;
                return;
            }
            FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            mMicToolBar.setLayoutParams(lp2);
            LinearLayout micToolBarLayer = findViewById(R.id.mic_tool_bar_layer);
            micToolBarLayer.addView(mMicToolBar);
        }
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if ((requestCode == REQUEST_AUDIO_PERMISSION) &&
                (permissions.length >= 1) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            // Get get audio record permission, so we can create the record tool bar
            toggleMicToolBar();
        }
    }


    private void showRescheduleCardDialog() {
        Consumer<Integer> runnable = days ->
                CollectionTask.launchCollectionTask(DISMISS_MULTI, mRescheduleCardHandler,
                        new TaskData(new Object[] {new long[] {mCurrentCard.getId()},
                                Collection.DismissType.RESCHEDULE_CARDS, days})
                );
        RescheduleDialog dialog = RescheduleDialog.rescheduleSingleCard(getResources(), mCurrentCard, runnable);

        showDialogFragment(dialog);
    }


    private void showResetCardDialog() {
        // Show confirmation dialog before resetting card progress
        Timber.i("showResetCardDialog() Reset progress button pressed");
        // Show confirmation dialog before resetting card progress
        ConfirmationDialog dialog = new ConfirmationDialog();
        String title = getResources().getString(R.string.reset_card_dialog_title);
        String message = getResources().getString(R.string.reset_card_dialog_message);
        dialog.setArgs(title, message);
        Runnable confirm = () -> {
            Timber.i("NoteEditor:: ResetProgress button pressed");
            CollectionTask.launchCollectionTask(DISMISS_MULTI, mResetProgressCardHandler,
                    new TaskData(new Object[] {new long[] {mCurrentCard.getId()}, Collection.DismissType.RESET_CARDS}));
        };
        dialog.setConfirm(confirm);
        showDialogFragment(dialog);
    }


    private void addNote() {
        Intent intent = new Intent(this, NoteEditor.class);
        intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_REVIEWER_ADD);
        startActivityForResultWithAnimation(intent, ADD_NOTE, ActivityTransitionAnimation.LEFT);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // NOTE: This is called every time a new question is shown via invalidate options menu
        getMenuInflater().inflate(R.menu.reviewer, menu);
        mActionButtons.setCustomButtonsStatus(menu);
        int alpha = (getControlBlocked() != ReviewerUi.ControlBlock.SLOW) ? Themes.ALPHA_ICON_ENABLED_LIGHT : Themes.ALPHA_ICON_DISABLED_LIGHT;
        MenuItem markCardIcon = menu.findItem(R.id.action_mark_card);
        int[] attrs = new int[] {
                R.attr.reviewMenuSuspendIconRef,//0
                R.attr.reviewMenuMarkIconRef,//1
                R.attr.reviewMenuFlipIconRef,//2
                R.attr.reviewMenuFlipDropDownIconRef,//3
                R.attr.reviewMenuEraserIconRef,//4
                R.attr.reviewMenuColorLensIconRef,//5
                R.attr.reviewMenuGestureIconRef,//6
                R.attr.reviewMenuSuspendDropDownIconRef,//7

        };
        TypedArray ta = obtainStyledAttributes(attrs);
        if (mCurrentCard != null && mCurrentCard.note().hasTag("marked")) {
//            markCardIcon.setTitle(R.string.menu_unmark_note).setIcon(R.drawable.ic_star_white_24dp);
            markCardIcon.setTitle(R.string.menu_unmark_note).setIcon(R.mipmap.note_star_normal);
        } else {
            markCardIcon.setTitle(R.string.menu_mark_note).setIcon(ta.getDrawable(1));
        }
        markCardIcon.getIcon().mutate().setAlpha(alpha);

        MenuItem flag_icon = menu.findItem(R.id.action_flag);
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(R.attr.reviewMenuFlagIconRef, value, true);
        if (mCurrentCard != null) {
            switch (mCurrentCard.userFlag()) {
                case 1:
                    flag_icon.setIcon(R.drawable.ic_flag_red);
                    break;
                case 2:
                    flag_icon.setIcon(R.drawable.ic_flag_orange);
                    break;
                case 3:
                    flag_icon.setIcon(R.drawable.ic_flag_green);
                    break;
                case 4:
                    flag_icon.setIcon(R.drawable.ic_flag_blue);
                    break;
                default:
                    flag_icon.setIcon(value.resourceId);
                    break;
            }
        }
        flag_icon.getIcon().mutate().setAlpha(alpha);

        // Undo button
        @DrawableRes int undoIconId;
        boolean undoEnabled;
        MenuItem undoIcon = menu.findItem(R.id.action_undo);
        if (mShowWhiteboard && mWhiteboard != null && mWhiteboard.isUndoModeActive()) {
            // Whiteboard is here and strokes have been added at some point
//            undoIconId = R.drawable.ic_eraser_variant_white_24dp;
            undoEnabled = !mWhiteboard.undoEmpty();
            undoIcon.setIcon(ta.getDrawable(4));
        } else {
            // We can arrive here even if `mShowWhiteboard &&
            // mWhiteboard != null` if no stroke had ever been made
            undoIconId = R.mipmap.nav_bar_back_highlight;
            undoEnabled = (colIsOpen() && getCol().undoAvailable());
            undoIcon.setIcon(undoIconId);
        }
        int alpha_undo = (undoEnabled && getControlBlocked() != ReviewerUi.ControlBlock.SLOW) ? Themes.ALPHA_ICON_ENABLED_LIGHT : Themes.ALPHA_ICON_DISABLED_LIGHT;
        undoIcon.setEnabled(undoEnabled).getIcon().mutate().setAlpha(alpha_undo);

        MenuItem toggleRemark = menu.findItem(R.id.action_toggle_remark);
        if (AnkiDroidApp.getSharedPrefs(getBaseContext()).getBoolean("enable_remark", true)) {
            toggleRemark.setTitle("禁用助记");
        } else {
            toggleRemark.setTitle("启用助记");
        }

        MenuItem toggle_whiteboard_icon = menu.findItem(R.id.action_toggle_whiteboard);
        MenuItem hide_whiteboard_icon = menu.findItem(R.id.action_hide_whiteboard);
        MenuItem change_pen_color_icon = menu.findItem(R.id.action_change_whiteboard_pen_color);
        // White board button
        if (mPrefWhiteboard) {
            // Configure the whiteboard related items in the action bar
            toggle_whiteboard_icon.setTitle(R.string.disable_whiteboard);
            // Always allow "Disable Whiteboard", even if "Enable Whiteboard" is disabled
            toggle_whiteboard_icon.setVisible(true);

            if (!mActionButtons.getStatus().hideWhiteboardIsDisabled()) {
                hide_whiteboard_icon.setVisible(true);
            }
            if (!mActionButtons.getStatus().clearWhiteboardIsDisabled()) {
                menu.findItem(R.id.action_clear_whiteboard).setVisible(true);
            }
            if (!mActionButtons.getStatus().saveWhiteboardIsDisabled()) {
                menu.findItem(R.id.action_save_whiteboard).setVisible(true);
            }
            if (!mActionButtons.getStatus().whiteboardPenColorIsDisabled()) {
                change_pen_color_icon.setVisible(true);
            }

//            Drawable whiteboardIcon = ContextCompat.getDrawable(this, R.drawable.ic_gesture_white_24dp).mutate();
//            Drawable whiteboardColorPaletteIcon = VectorDrawableCompat.create(getResources(), R.drawable.ic_color_lens_white_24dp, null).mutate();
            Drawable whiteboardIcon = ta.getDrawable(6).mutate();
            Drawable whiteboardColorPaletteIcon = ta.getDrawable(5).mutate();
            if (mShowWhiteboard) {
                whiteboardIcon.setAlpha(Themes.ALPHA_ICON_ENABLED_LIGHT);
                hide_whiteboard_icon.setIcon(whiteboardIcon);
                hide_whiteboard_icon.setTitle(R.string.hide_whiteboard);

                whiteboardColorPaletteIcon.setAlpha(Themes.ALPHA_ICON_ENABLED_LIGHT);
                change_pen_color_icon.setIcon(whiteboardColorPaletteIcon);
            } else {
                whiteboardIcon.setAlpha(Themes.ALPHA_ICON_DISABLED_LIGHT);
                hide_whiteboard_icon.setIcon(whiteboardIcon);
                hide_whiteboard_icon.setTitle(R.string.show_whiteboard);

                whiteboardColorPaletteIcon.setAlpha(Themes.ALPHA_ICON_DISABLED_LIGHT);
                change_pen_color_icon.setEnabled(false);
                change_pen_color_icon.setIcon(whiteboardColorPaletteIcon);
                colorPalette.setVisibility(View.GONE);
            }
        } else {
            toggle_whiteboard_icon.setTitle(R.string.enable_whiteboard);
        }
        if (colIsOpen() && getCol().getDecks().isDyn(getParentDid())) {
            menu.findItem(R.id.action_open_deck_options).setVisible(false);
        }
        if (!mActionButtons.getStatus().vipSpeakIsDisabled()) {
            menu.findItem(R.id.action_speak).setVisible(true);
        }
//        if (mSpeakText && !mActionButtons.getStatus().selectTtsIsDisabled()) {
//            menu.findItem(R.id.action_select_tts).setVisible(true);
//        }
        // Setup bury / suspend providers
        MenuItem suspend_icon = menu.findItem(R.id.action_suspend);
        MenuItem bury_icon = menu.findItem(R.id.action_bury);
        MenuItemCompat.setActionProvider(suspend_icon, new SuspendProvider(this));
        MenuItemCompat.setActionProvider(bury_icon, new BuryProvider(this));

        if (suspendNoteAvailable()) {
//            suspend_icon.setIcon(R.drawable.ic_action_suspend_dropdown);
            suspend_icon.setIcon(ta.getDrawable(7));
            suspend_icon.setTitle(R.string.menu_suspend);
        } else {
            suspend_icon.setIcon(ta.getDrawable(0));
            suspend_icon.setTitle(R.string.menu_suspend_card);
        }
        if (buryNoteAvailable()) {
//            bury_icon.setIcon(R.drawable.ic_flip_to_back_white_24px_dropdown);
//            bury_icon.setIcon(ta.getDrawable(3));
            bury_icon.setTitle(R.string.menu_bury);
        } else {
//            bury_icon.setIcon(ta.getDrawable(2));
            bury_icon.setTitle(R.string.menu_bury_card);
        }
        ta.recycle();
        alpha = (getControlBlocked() != ReviewerUi.ControlBlock.SLOW) ? Themes.ALPHA_ICON_ENABLED_LIGHT : Themes.ALPHA_ICON_DISABLED_LIGHT;
//        bury_icon.getIcon().mutate().setAlpha(alpha);
        suspend_icon.getIcon().mutate().setAlpha(alpha);

        MenuItemCompat.setActionProvider(menu.findItem(R.id.action_schedule), new ScheduleProvider(this));
        MenuItem flip = menu.findItem(R.id.action_flip_enable);
        boolean enableFlip=AnkiDroidApp.getSharedPrefs(getBaseContext()).getBoolean(Consts.KEY_FLIP_CARD,false);
        flip.setTitle(enableFlip?R.string.card_flip_enable:R.string.card_flip_disable);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return mProcessor.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        Timber.i("show me the key code on key up:%s", keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                executeCommandByController(mControllerUp);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                executeCommandByController(mControllerDown);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                executeCommandByController(mControllerRight);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                executeCommandByController(mControllerLeft);
                return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBL:
                executeCommandByController(mControllerLeftPad);
                return true;
            case KeyEvent.KEYCODE_BUTTON_THUMBR:
                executeCommandByController(mControllerRightPad);
                return true;
            case KeyEvent.KEYCODE_BUTTON_X:
                executeCommandByController(mControllerX);
                return true;
            case KeyEvent.KEYCODE_BUTTON_Y:
                executeCommandByController(mControllerY);
                return true;
            case KeyEvent.KEYCODE_BUTTON_A:
                executeCommandByController(mControllerA);
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                executeCommandByController(mControllerB);
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                executeCommandByController(mControllerLT);
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                executeCommandByController(mControllerRT);
                return true;
            case KeyEvent.KEYCODE_BUTTON_L2:
                executeCommandByController(mControllerLB);
                return true;
            case KeyEvent.KEYCODE_BUTTON_R2:
                executeCommandByController(mControllerRB);
                return true;
            case KeyEvent.KEYCODE_BUTTON_START:
                executeCommandByController(mControllerMenu);
                return true;
            case KeyEvent.KEYCODE_BUTTON_MODE:
                executeCommandByController(mControllerOption);
                return true;
        }
        if (answerFieldIsFocused()) {
            return super.onKeyUp(keyCode, event);
        }
        if (mProcessor.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }


    @Override
    protected void performReload() {
        getCol().getSched().deferReset();
        CollectionTask.launchCollectionTask(ANSWER_CARD, mAnswerCardHandler(false),
                new TaskData(null, 0));
    }


    @Override
    protected void displayAnswerBottomBar() {
        super.displayAnswerBottomBar();
        int buttonCount;
        try {
            buttonCount = mSched.answerButtons(mCurrentCard);
        } catch (RuntimeException e) {
            AnkiDroidApp.sendExceptionReport(e, "AbstractReviewer-showEaseButtons");
            closeReviewer(DeckPicker.RESULT_DB_ERROR, true);
            return;
        }

        // Set correct label and background resource for each button
        // Note that it's necessary to set the resource dynamically as the ease2 / ease3 buttons
        // (which libanki expects ease to be 2 and 3) can either be hard, good, or easy - depending on num buttons shown
        int[] backgroundIds;
        if (Build.VERSION.SDK_INT >= 21 && animationEnabled()) {
            backgroundIds = new int[] {
                    R.attr.againButtonRippleRef,
                    R.attr.hardButtonRippleRef,
                    R.attr.goodButtonRippleRef,
                    R.attr.easyButtonRippleRef};
        } else {
            backgroundIds = new int[] {
                    R.attr.againButtonRef,
                    R.attr.hardButtonRef,
                    R.attr.goodButtonRef,
                    R.attr.easyButtonRef};
        }
        final int[] background = Themes.getResFromAttr(this, backgroundIds);
        final int[] textColor = Themes.getColorFromAttr(this, new int[] {
                R.attr.againButtonTextColor,
                R.attr.hardButtonTextColor,
                R.attr.goodButtonTextColor,
                R.attr.easyButtonTextColor});
        mEase1Layout.setVisibility(View.VISIBLE);
        mEase1Layout.setBackgroundResource(background[0]);
        mEase4Layout.setBackgroundResource(background[3]);
        switch (buttonCount) {
            case 2:
                // Ease 2 is "good"
                mEase2Layout.setVisibility(View.VISIBLE);
                mEase2Layout.setBackgroundResource(background[2]);
                mEase2.setText(R.string.ease_button_good);
                mEase2.setTextColor(textColor[2]);
                mNext2.setTextColor(textColor[2]);
                mEase2Layout.requestFocus();
                break;
            case 3:
                // Ease 2 is good
                mEase2Layout.setVisibility(View.VISIBLE);
                mEase2Layout.setBackgroundResource(background[2]);
                mEase2.setText(R.string.ease_button_good);
                mEase2.setTextColor(textColor[2]);
                mNext2.setTextColor(textColor[2]);
                // Ease 3 is easy
                mEase3Layout.setVisibility(View.VISIBLE);
                mEase3Layout.setBackgroundResource(background[3]);
                mEase3.setText(R.string.ease_button_easy);
                mEase3.setTextColor(textColor[3]);
                mNext3.setTextColor(textColor[3]);
                mEase2Layout.requestFocus();
                break;
            default:
                mEase2Layout.setVisibility(View.VISIBLE);
                // Ease 2 is "hard"
                mEase2Layout.setVisibility(View.VISIBLE);
                mEase2Layout.setBackgroundResource(background[1]);
                mEase2.setText(R.string.ease_button_hard);
                mEase2.setTextColor(textColor[1]);
                mNext2.setTextColor(textColor[1]);
                mEase2Layout.requestFocus();
                // Ease 3 is good
                mEase3Layout.setVisibility(View.VISIBLE);
                mEase3Layout.setBackgroundResource(background[2]);
                mEase3.setText(R.string.ease_button_good);
                mEase3.setTextColor(textColor[2]);
                mNext3.setTextColor(textColor[2]);
                mEase4Layout.setVisibility(View.VISIBLE);
                mEase3Layout.requestFocus();
                break;
        }

        // Show next review time
        if (shouldShowNextReviewTime()) {
            mNext1.setText(mSched.nextIvlStr(this, mCurrentCard, Consts.BUTTON_ONE));
            mNext2.setText(mSched.nextIvlStr(this, mCurrentCard, Consts.BUTTON_TWO));
            if (buttonCount > 2) {
                mNext3.setText(mSched.nextIvlStr(this, mCurrentCard, Consts.BUTTON_THREE));
            }
            if (buttonCount > 3) {
                mNext4.setText(mSched.nextIvlStr(this, mCurrentCard, Consts.BUTTON_FOUR));
            }
        }
    }


    @Override
    protected SharedPreferences restorePreferences() {
        Timber.i("restorePreferences");
        SharedPreferences preferences = super.restorePreferences();
        mPrefHideDueCount = preferences.getBoolean("hideDueCount", false);
        mPrefShowETA = preferences.getBoolean("showETA", true);
        this.mProcessor.setup();
        mBlackWhiteboard = preferences.getBoolean("blackWhiteboard", true);
        mPrefFullscreenReview = Integer.parseInt(preferences.getString("fullscreenMode", "0")) > 0;
        mActionButtons.setup(preferences);
        return preferences;
    }


    @Override
    protected void updateActionBar() {
        super.updateActionBar();
        updateScreenCounts();
    }


    protected void updateScreenCounts() {
        if (mCurrentCard == null) {
            return;
        }
        super.updateActionBar();
        ActionBar actionBar = getSupportActionBar();
        int[] counts = mSched.counts(mCurrentCard);

//        if (actionBar != null) {
//            if (mPrefShowETA) {
//                eta = mSched.eta(counts, false);
//                actionBar.setSubtitle(Utils.remainingTime(AnkiDroidApp.getInstance(), eta * 60));
//            }
//        }


        newCount = new SpannableString(String.valueOf(counts[0]));
        lrnCount = new SpannableString(String.valueOf(counts[1]));
        revCount = new SpannableString(String.valueOf(counts[2]));
        if (mPrefHideDueCount) {
            revCount = new SpannableString("???");
        }
//
//        switch (mSched.countIdx(mCurrentCard)) {
//            case Consts.CARD_TYPE_NEW:
//                newCount.setSpan(new UnderlineSpan(), 0, newCount.length(), 0);
//                break;
//            case Consts.CARD_TYPE_LRN:
//                lrnCount.setSpan(new UnderlineSpan(), 0, lrnCount.length(), 0);
//                break;
//            case Consts.CARD_TYPE_REV:
//                revCount.setSpan(new UnderlineSpan(), 0, revCount.length(), 0);
//                break;
//            default:
//                Timber.w("Unknown card type %s", mSched.countIdx(mCurrentCard));
//                break;
//        }

        mTextBarNew.setText(newCount);
        mTextBarLearn.setText(lrnCount);
        mTextBarReview.setText(revCount);
//        SpannableString all=new SpannableString("新卡 "+newCount+" | "+"学习中 "+lrnCount+" | "+"复习 "+revCount);
//        final int[] textColor = Themes.getColorFromAttr(this, new int [] {
//                R.attr.primary_text_third_color,
//                R.attr.primaryForthTextColor,
//                });
//        ForegroundColorSpan describe =new ForegroundColorSpan(textColor[0]);
//        ForegroundColorSpan count =new ForegroundColorSpan(textColor[1]);
//        ForegroundColorSpan divider =new ForegroundColorSpan(ContextCompat.getColor(this, R.color.review_top_divider));
//        all.setSpan(count,0,all.length(),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//        all.setSpan(describe,0,1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//        all.setSpan(describe,6+newCount.length(),6+newCount.length()+2, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//        all.setSpan(describe,11+newCount.length()+lrnCount.length(),11+newCount.length()+lrnCount.length()+1, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//
//        all.setSpan(divider,5+newCount.length(), 5 + newCount.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//        all.setSpan(divider,10+newCount.length()+lrnCount.length(),10+newCount.length()+lrnCount.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
//
//        mTitle.setText(all);
    }


    @Override
    public void fillFlashcard() {
        super.fillFlashcard();
        if (!sDisplayAnswer && mShowWhiteboard && mWhiteboard != null) {
            mWhiteboard.clear();
        }
    }


    @Override
    public void displayCardQuestion() {
        // show timer, if activated in the deck's preferences
        initTimer();
        super.displayCardQuestion();
    }


    @Override
    protected void initLayout() {
        mTextBarNew = (TextView) findViewById(R.id.new_number);
        mTextBarLearn = (TextView) findViewById(R.id.learn_number);
        mTextBarReview = (TextView) findViewById(R.id.review_number);

        super.initLayout();

        if (!mShowRemainingCardCount) {
            mTextBarNew.setVisibility(View.GONE);
            mTextBarLearn.setVisibility(View.GONE);
            mTextBarReview.setVisibility(View.GONE);
        }
    }


    @Override
    protected void switchTopBarVisibility(int visible) {
        super.switchTopBarVisibility(visible);
        if (mShowRemainingCardCount) {
            mTextBarNew.setVisibility(visible);
            mTextBarLearn.setVisibility(visible);
            mTextBarReview.setVisibility(visible);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();

        if (!isFinishing() && colIsOpen() && mSched != null) {
            WidgetStatus.update(this);
        }
        UIUtils.saveCollectionInBackground();
    }


    @Override
    protected void initControls() {
        super.initControls();
        if (mPrefWhiteboard) {
            setWhiteboardVisibility(mShowWhiteboard);
        }
        if (mShowRemainingCardCount) {
            mTextBarNew.setVisibility(View.VISIBLE);
            mTextBarLearn.setVisibility(View.VISIBLE);
            mTextBarReview.setVisibility(View.VISIBLE);
        }
    }


    protected void restoreCollectionPreferences() {
        super.restoreCollectionPreferences();
        mShowRemainingCardCount = getCol().getConf().getBoolean("dueCounts");
    }


    @Override
    protected boolean onSingleTap() {
        if (mPrefFullscreenReview &&
                CompatHelper.getCompat().isImmersiveSystemUiVisible(this)) {
            delayedHide(INITIAL_HIDE_DELAY);
            return true;
        }
        return false;
    }


    @Override
    protected void onFling() {
        if (mPrefFullscreenReview &&
                CompatHelper.getCompat().isImmersiveSystemUiVisible(this)) {
            delayedHide(INITIAL_HIDE_DELAY);
        }
    }


    protected final Handler mFullScreenHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mPrefFullscreenReview) {
                CompatHelper.getCompat().setFullScreen(Reviewer.this);
            }
        }
    };


    protected void delayedHide(int delayMillis) {
        Timber.d("Fullscreen delayed hide in %dms", delayMillis);
        mFullScreenHandler.removeMessages(0);
        mFullScreenHandler.sendEmptyMessageDelayed(0, delayMillis);
    }


    private void setWhiteboardEnabledState(boolean state) {
        mPrefWhiteboard = state;
        MetaDB.storeWhiteboardState(this, getParentDid(), state);
        if (state && mWhiteboard == null) {
            createWhiteboard();
        }
    }


    // Create the whiteboard
    private void createWhiteboard() {
        mWhiteboard = new Whiteboard(this, isInNightMode(), mBlackWhiteboard);
        FrameLayout.LayoutParams lp2 = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mWhiteboard.setLayoutParams(lp2);
        FrameLayout fl = findViewById(R.id.whiteboard);
        fl.addView(mWhiteboard);

        mWhiteboard.setOnTouchListener((v, event) -> {
            //If the whiteboard is currently drawing, and triggers the system UI to show, we want to continue drawing.
            if (!mWhiteboard.isCurrentlyDrawing() && (!mShowWhiteboard || (mPrefFullscreenReview
                    && CompatHelper.getCompat().isImmersiveSystemUiVisible(Reviewer.this)))) {
                // Bypass whiteboard listener when it's hidden or fullscreen immersive mode is temporarily suspended
                v.performClick();
                return getGestureDetector().onTouchEvent(event);
            }
            return mWhiteboard.handleTouchEvent(event);
        });
        mWhiteboard.setEnabled(true);
    }


    // Show or hide the whiteboard
    private void setWhiteboardVisibility(boolean state) {
        mShowWhiteboard = state;
        MetaDB.storeWhiteboardVisibility(this, getParentDid(), state);
        if (state) {
            mWhiteboard.setVisibility(View.VISIBLE);
//            disableDrawerSwipe();
        } else {
            mWhiteboard.setVisibility(View.GONE);
//            if (!mHasDrawerSwipeConflicts) {
//                enableDrawerSwipe();
//            }
        }
    }


    private void disableDrawerSwipeOnConflicts() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        boolean gesturesEnabled = AnkiDroidApp.initiateGestures(preferences);
        if (gesturesEnabled) {
            int gestureSwipeUp = Integer.parseInt(preferences.getString("gestureSwipeUp", "9"));
            int gestureSwipeDown = Integer.parseInt(preferences.getString("gestureSwipeDown", "0"));
            int gestureSwipeRight = Integer.parseInt(preferences.getString("gestureSwipeRight", "17"));
            if (gestureSwipeUp != COMMAND_NOTHING ||
                    gestureSwipeDown != COMMAND_NOTHING ||
                    gestureSwipeRight != COMMAND_NOTHING) {
                mHasDrawerSwipeConflicts = true;
//                super.disableDrawerSwipe();
            }
        }
    }

//
//    @Override
//    protected Long getCurrentCardId() {
//        return mCurrentCard.getId();
//    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Restore full screen once we regain focus
        if (hasFocus) {
            delayedHide(INITIAL_HIDE_DELAY);
        } else {
            mFullScreenHandler.removeMessages(0);
        }
    }


    /**
     * Whether or not dismiss note is available for current card and specified DismissType
     *
     * @return true if there is another card of same note that could be dismissed
     */
    private boolean suspendNoteAvailable() {
        if (mCurrentCard == null || isControlBlocked()) {
            return false;
        }
        // whether there exists a sibling not buried.
        return getCol().getDb().queryScalar("select 1 from cards where nid = ? and id != ? and queue != " + Consts.QUEUE_TYPE_SUSPENDED + " limit 1",
                mCurrentCard.getNid(), mCurrentCard.getId()) == 1;
    }


    private boolean buryNoteAvailable() {
        if (mCurrentCard == null || isControlBlocked()) {
            return false;
        }
        // Whether there exists a sibling which is neither susbended nor buried
        boolean bury = getCol().getDb().queryScalar("select 1 from cards where nid = ? and id != ? and queue >=  " + Consts.QUEUE_TYPE_NEW + " limit 1",
                mCurrentCard.getNid(), mCurrentCard.getId()) == 1;
        return bury;
    }


    /**
     * Inner class which implements the submenu for the Suspend button
     */
    class SuspendProvider extends ActionProvider implements MenuItem.OnMenuItemClickListener {
        public SuspendProvider(Context context) {
            super(context);
        }


        @Override
        public View onCreateActionView() {
            return null;  // Just return null for a simple dropdown menu
        }


        @Override
        public boolean hasSubMenu() {
            return suspendNoteAvailable();
        }


        @Override
        public void onPrepareSubMenu(SubMenu subMenu) {
            subMenu.clear();
            getMenuInflater().inflate(R.menu.reviewer_suspend, subMenu);
            for (int i = 0; i < subMenu.size(); i++) {
                subMenu.getItem(i).setOnMenuItemClickListener(this);
            }
        }


        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_suspend_card:
                    dismiss(DismissType.SUSPEND_CARD);
                    return true;
                case R.id.action_suspend_note:
                    dismiss(DismissType.SUSPEND_NOTE);
                    return true;
                default:
                    return false;
            }
        }
    }



    /**
     * Inner class which implements the submenu for the Bury button
     */
    class BuryProvider extends ActionProvider implements MenuItem.OnMenuItemClickListener {
        public BuryProvider(Context context) {
            super(context);
        }


        @Override
        public View onCreateActionView() {
            return null;    // Just return null for a simple dropdown menu
        }


        @Override
        public boolean hasSubMenu() {
            return buryNoteAvailable();
        }


        @Override
        public void onPrepareSubMenu(SubMenu subMenu) {
            subMenu.clear();
            getMenuInflater().inflate(R.menu.reviewer_bury, subMenu);
            for (int i = 0; i < subMenu.size(); i++) {
                subMenu.getItem(i).setOnMenuItemClickListener(this);
            }
        }


        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_bury_card:
                    dismiss(DismissType.BURY_CARD);
                    return true;
                case R.id.action_bury_note:
                    dismiss(DismissType.BURY_NOTE);
                    return true;
                default:
                    return false;
            }
        }
    }



    /**
     * Inner class which implements the submenu for the Schedule button
     */
    class ScheduleProvider extends ActionProvider implements MenuItem.OnMenuItemClickListener {
        public ScheduleProvider(Context context) {
            super(context);
        }


        @Override
        public View onCreateActionView() {
            return null;    // Just return null for a simple dropdown menu
        }


        @Override
        public boolean hasSubMenu() {
            return true;
        }


        @Override
        public void onPrepareSubMenu(SubMenu subMenu) {
            subMenu.clear();
            getMenuInflater().inflate(R.menu.reviewer_schedule, subMenu);
            for (int i = 0; i < subMenu.size(); i++) {
                subMenu.getItem(i).setOnMenuItemClickListener(this);
            }
        }


        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_reschedule_card:
                    showRescheduleCardDialog();
                    return true;
                case R.id.action_reset_card_progress:
                    showResetCardDialog();
                    return true;
                default:
                    return false;
            }
        }
    }


    public ReviewerJavaScriptFunction javaScriptFunction() {
        return new ReviewerJavaScriptFunction();
    }


    public class ReviewerJavaScriptFunction extends JavaScriptFunction {
        @JavascriptInterface
        @Override
        public String ankiGetNewCardCount() {
            return newCount.toString();
        }


        @JavascriptInterface
        @Override
        public String ankiGetLrnCardCount() {
            return lrnCount.toString();
        }


        @JavascriptInterface
        @Override
        public String ankiGetRevCardCount() {
            return revCount.toString();
        }


        @JavascriptInterface
        @Override
        public int ankiGetETA() {
            return eta;
        }
    }
}
