/***************************************************************************************
 * Copyright (c) 2011 Kostas Spyropoulos <inigo.aldana@gmail.com>                       *
 * Copyright (c) 2013 Jolta Technologies                                                *
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

package com.ichi2.anki;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskData;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Utils;
import com.ichi2.themes.Themes;

import java.util.HashSet;
import java.util.List;

import androidx.annotation.NonNull;
import timber.log.Timber;

import static com.ichi2.anki.cardviewer.ViewerCommand.COMMAND_NEXT_CARD;
import static com.ichi2.anki.cardviewer.ViewerCommand.COMMAND_NOTHING;
import static com.ichi2.anki.cardviewer.ViewerCommand.COMMAND_PRE_CARD;
import static com.ichi2.anki.cardviewer.ViewerCommand.COMMAND_SHOW_ANSWER;
import static com.ichi2.async.CollectionTask.TASK_TYPE.DISMISS;

/**
 * The previewer intent must supply an array of cards to show and the index in the list from where
 * to begin showing them. Special rules are applied if the list size is 1 (i.e., no scrolling
 * buttons will be shown).
 */
public class Previewer extends AbstractFlashcardViewer {
    private long[] mCardList;
    private int mIndex;
    private boolean mShowingAnswer;

    /** Communication with Browser */
    private boolean mReloadRequired;
    private boolean mNoteChanged;

    protected int getContentViewAttr(int fullscreenMode) {
        return R.layout.reviewer2;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");
        super.onCreate(savedInstanceState);

        mCardList = getIntent().getLongArrayExtra("cardList");
        mIndex = getIntent().getIntExtra("index", -1);

        if (savedInstanceState != null){
            mIndex = savedInstanceState.getInt("index", mIndex);
            mShowingAnswer = savedInstanceState.getBoolean("showingAnswer", mShowingAnswer);
            mReloadRequired = savedInstanceState.getBoolean("reloadRequired");
            mNoteChanged = savedInstanceState.getBoolean("noteChanged");
        }
//        Timber.i("cardlist:"+mCardList+",length:"+mCardList.length+","+mIndex);
        if (mCardList==null||mCardList.length == 0 || mIndex < 0 || mIndex > mCardList.length - 1) {
            Timber.e("Previewer started with empty card list or invalid index");
            finishWithoutAnimation();
            return;
        }
//        showBackIcon();
        // Ensure navigation drawer can't be opened. Various actions in the drawer cause crashes.
//        disableDrawerSwipe();
        startLoadingCollection();
    }

    @Override
    protected void onCollectionLoaded(Collection col) {
        super.onCollectionLoaded(col);
        mCurrentCard = col.getCard(mCardList[mIndex]);

        displayCardQuestion();
        if (mShowingAnswer) {
            displayCardAnswer();
        }

//        showBackIcon();
    }

    /** Given a new collection of card Ids, find the 'best' valid card given the current collection
     * We define the best as searching to the left, then searching to the right of the current element
     * This occurs as many cards can be deleted when editing a note (from the Card Template Editor) */
    private int getNextIndex(List<Long> newCardList) {
        HashSet<Long> validIndices = new HashSet<>(newCardList);

        for (int i = mIndex; i >= 0; i--) {
            if (validIndices.contains(mCardList[i])) {
                return newCardList.indexOf(mCardList[i]);
            }
        }

        for (int i = mIndex + 1; i < validIndices.size(); i++) {
            if (validIndices.contains(mCardList[i])) {
                return newCardList.indexOf(mCardList[i]);
            }
        }
        if (newCardList.isEmpty()) {
            finishWithoutAnimation();
        }
        return -1;
//        throw new IllegalStateException("newCardList was empty");
    }


    @Override
    protected void setTitle() {
        getSupportActionBar().setTitle(R.string.preview_title);
    }


    @Override
    protected void initLayout() {
        super.initLayout();
        mTopBarLayout.setVisibility(View.GONE);

        findViewById(R.id.answer_options_layout).setVisibility(View.GONE);

        mPreviewButtonsLayout.setVisibility(View.VISIBLE);
        mPreviewButtonsLayout.setOnClickListener(mToggleAnswerHandler);

        mPreviewPrevCard.setOnClickListener(mSelectScrollHandler);
        mPreviewNextCard.setOnClickListener(mSelectScrollHandler);

//        if (Build.VERSION.SDK_INT >= 21 && animationEnabled()) {
//            int resId = Themes.getResFromAttr(this, R.attr.hardButtonRippleRef);
//            mPreviewButtonsLayout.setBackgroundResource(resId);
//            mPreviewPrevCard.setBackgroundResource(R.drawable.item_background_light_selectable_borderless);
//            mPreviewNextCard.setBackgroundResource(R.drawable.item_background_light_selectable_borderless);
//        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                editCard();
                return true;

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
                return true;

            case R.id.action_delete:
                Timber.i("Reviewer:: Delete note button pressed");
                showDeleteNoteDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    protected void showDeleteNoteDialog() {
        Resources res = getResources();
        new MaterialDialog.Builder(this)
                .title(res.getString(R.string.delete_card_title))
                .iconAttr(R.attr.dialogErrorIcon)
                .content(res.getString(R.string.delete_note_message,
                        Utils.stripHTML(mCurrentCard.q(true))))
                .positiveText(res.getString(R.string.dialog_positive_delete))
                .negativeText(res.getString(R.string.dialog_cancel))
                .onPositive((dialog, which) -> {
                    Timber.i("AbstractFlashcardViewer:: OK button pressed to delete note %d", mCurrentCard.getNid());
                    mSoundPlayer.stopSounds();
                    blockControls(false);
                    CollectionTask.launchCollectionTask(DISMISS, mDismissCardHandler,
                            new TaskData(new Object[] {mCurrentCard, Collection.DismissType.DELETE_NOTE}));
                    mNoteChanged = true;
                    if (mCardList.length > 1) {
                        long temp[] = new long[mCardList.length - 1];
                        for (int i = 0, k = 0; i < mCardList.length; i++) {
                            if (i != mIndex) {
                                temp[k++] = mCardList[i];
                            }
                        }
                        if (mIndex > temp.length - 1) {
                            mIndex = temp.length - 1;
                        }
                        mCardList = temp;
                        mCurrentCard = getCol().getCard(mCardList[mIndex]);
                        displayCardQuestion();
                    } else {
                        finishWithoutAnimation();
                    }
                    Toast.makeText(Previewer.this, "成功删除卡牌", Toast.LENGTH_SHORT).show();

                })
                .build().show();
    }


    @Override
    public void onBackPressed() {
        setResult(RESULT_OK, getResultIntent());
        super.onBackPressed();
    }

//
//    @Override
//    protected void onNavigationPressed() {
//        setResult(RESULT_OK, getResultIntent());
//        super.onNavigationPressed();
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.previewer, menu);
        MenuItem toggleRemark = menu.findItem(R.id.action_toggle_remark);
        if (AnkiDroidApp.getSharedPrefs(getBaseContext()).getBoolean("enable_remark", true)) {
            toggleRemark.setTitle("禁用助记");
        } else {
            toggleRemark.setTitle("启用助记");
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putLongArray("cardList", mCardList);
        outState.putInt("index", mIndex);
        outState.putBoolean("showingAnswer", mShowingAnswer);
        outState.putBoolean("reloadRequired", mReloadRequired);
        outState.putBoolean("noteChanged", mNoteChanged);
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void displayCardQuestion() {
        super.displayCardQuestion();
        mShowingAnswer = false;
        updateButtonsState();
    }


    // Called via mFlipCardListener in parent class when answer button pressed
    @Override
    protected void displayCardAnswer() {
        super.displayCardAnswer();
        mShowingAnswer = true;
        updateButtonsState();
    }


    @Override
    protected void hideEaseButtons() {
        /* do nothing */
    }

    @Override
    protected void displayAnswerBottomBar() {
        /* do nothing */
    }


//    @Override
//    public boolean executeCommand(int which) {
//        /* do nothing */
//        return false;
//    }


    @Override
    protected void performReload() {
        mReloadRequired = true;
        List<Long> newCardList = getCol().filterToValidCards(mCardList);

        if (newCardList.isEmpty()) {
            finishWithoutAnimation();
            return;
        }

        mIndex = getNextIndex(newCardList);
        mCardList = Utils.collection2Array(newCardList);
        mCurrentCard = getCol().getCard(mCardList[mIndex]);
        displayCardQuestion();
    }


    @Override
    protected void onEditedNoteChanged() {
        super.onEditedNoteChanged();
        mNoteChanged = true;
    }


    private View.OnClickListener mSelectScrollHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.preview_previous_flashcard) {
                mIndex--;
            } else if (view.getId() == R.id.preview_next_flashcard) {
                mIndex++;
            }

            mCurrentCard = getCol().getCard(mCardList[mIndex]);
            displayCardQuestion();
        }
    };

    private final View.OnClickListener mToggleAnswerHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mShowingAnswer) {
                displayCardQuestion();
            } else {
                displayCardAnswer();
            }
        }
    };

    private void updateButtonsState() {
        mPreviewToggleAnswerText.setText(mShowingAnswer ? R.string.hide_answer : R.string.show_answer);

        // If we are in single-card mode, we show the "Show Answer" button on the question side
        // and hide navigation buttons.
        if (mCardList.length == 1) {
            mPreviewPrevCard.setVisibility(View.GONE);
            mPreviewNextCard.setVisibility(View.GONE);
            return;
        }

        boolean prevBtnDisabled = mIndex <= 0;
        boolean nextBtnDisabled = mIndex >= mCardList.length - 1;

        mPreviewPrevCard.setEnabled(!prevBtnDisabled);
        mPreviewNextCard.setEnabled(!nextBtnDisabled);

        mPreviewPrevCard.setAlpha(prevBtnDisabled ? 0.38F : 1);
        mPreviewNextCard.setAlpha(nextBtnDisabled ? 0.38F : 1);
    }


    protected Intent getResultIntent() {
        Intent intent = new Intent();
        intent.putExtra("reloadRequired", mReloadRequired);
        intent.putExtra("noteChanged", mNoteChanged);
        return intent;
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

        return super.onKeyUp(keyCode, event);
    }

    protected void executeCommandByController(int which) {
        switch (which) {
            case COMMAND_PRE_CARD:
                mSelectScrollHandler.onClick(mPreviewPrevCard);
                break;
            case COMMAND_NEXT_CARD:
                mSelectScrollHandler.onClick(mPreviewNextCard);
                break;
            default:executeCommand(which);
        }

    }
}
