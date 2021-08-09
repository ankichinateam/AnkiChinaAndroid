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
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Utils;
import com.ichi2.ui.CustomStyleDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import timber.log.Timber;

import static com.ichi2.async.CollectionTask.TASK_TYPE.DISMISS;
import static com.ichi2.async.CollectionTask.TASK_TYPE.DISMISS_MULTI;

/**
 * The previewer intent must supply an array of cards to show and the index in the list from where
 * to begin showing them. Special rules are applied if the list size is 1 (i.e., no scrolling
 * buttons will be shown).
 */
public class Previewer2 extends AbstractFlashcardViewer {
    private long[] mCardList;
    private int mIndex;
    private boolean mShowingAnswer;

    /**
     * Communication with Browser
     */
    private boolean mReloadRequired;
    private boolean mNoteChanged;


    protected int getContentViewAttr(int fullscreenMode) {
        return R.layout.reviewer3;
    }

    private SharedPreferences preferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.d("onCreate()");
        super.onCreate(savedInstanceState);

        mCardList = getIntent().getLongArrayExtra("cardList");
        mIndex = getIntent().getIntExtra("index", -1);

        if (savedInstanceState != null) {
            mIndex = savedInstanceState.getInt("index", mIndex);
            mShowingAnswer = savedInstanceState.getBoolean("showingAnswer", mShowingAnswer);
            mReloadRequired = savedInstanceState.getBoolean("reloadRequired");
            mNoteChanged = savedInstanceState.getBoolean("noteChanged");
        }

        if (mCardList.length == 0 || mIndex < 0 || mIndex > mCardList.length - 1) {
            Timber.e("Previewer started with empty card list or invalid index");
            finishWithoutAnimation();
            return;
        }
//        showBackIcon();
        // Ensure navigation drawer can't be opened. Various actions in the drawer cause crashes.
//        disableDrawerSwipe();
        startLoadingCollection();
        preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        mSelfStudyFreeVipCount = preferences.getInt("self_study_count", 0);
//        mFreeVipRecordDay = preferences.getInt("self_study_count_day", 0);
        Calendar calendar = Calendar.getInstance();
//        if (mFreeVipRecordDay != calendar.get(Calendar.DAY_OF_YEAR)) {//已经不是记录里的同一天
//            mSelfStudyFreeVipCount = 0;
//            preferences.edit().putInt("self_study_count", mSelfStudyFreeVipCount)
//                    .putInt("self_study_count_day", calendar.get(Calendar.DAY_OF_YEAR)).apply();
//        }
        findViewById(R.id.review_count_layout).setVisibility(View.VISIBLE);
        mTitle.setVisibility(View.GONE);

    }
//    protected int mFreeVipRecordDay;
    protected int mSelfStudyFreeVipCount = 1;
    @Override
    protected void onCollectionLoaded(Collection col) {
        super.onCollectionLoaded(col);
        mCurrentCard = col.getCard(mCardList[mIndex]);

        displayCardQuestion();
        if (mShowingAnswer) {
            displayCardAnswer();
        }
//        refreshButtons();
//        showBackIcon();
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
    protected void updateDeckName() {
        if (mCurrentCard == null) {
            return;
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
//            String title = Decks.basename(getCol().getDecks().get(mCurrentCard.getDid()).getString("name"));
            actionBar.setTitle((mIndex + 1) + "/" + mCardList.length);
        }

    }


    /**
     * Given a new collection of card Ids, find the 'best' valid card given the current collection
     * We define the best as searching to the left, then searching to the right of the current element
     * This occurs as many cards can be deleted when editing a note (from the Card Template Editor)
     */
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

        throw new IllegalStateException("newCardList was empty");
    }


    @Override
    protected void setTitle() {
        getSupportActionBar().setTitle(R.string.preview_title);
    }


    protected LinearLayout mShowPreview2AnswerLayout;
    protected LinearLayout mMarkButtonsLayout;
    protected LinearLayout mMark1Layout;
    protected LinearLayout mMark2Layout;
    protected LinearLayout mMark3Layout;
    //    protected LinearLayout mMark4Layout;
    protected LinearLayout mNextCardLayout;
    protected TextView mMark1Text;
    protected TextView mMark2Text;
    protected TextView mMark3Text;
    //    protected TextView mMark4Text;
    protected TextView mNextText;
    private TextView mTextBarNew;
    private TextView mTextBarLearn;
    private TextView mTextBarHandled;

    @Override
    protected void initLayout() {
        super.initLayout();
        mTopBarLayout.setVisibility(View.GONE);

        findViewById(R.id.answer_options_layout).setVisibility(View.GONE);
        findViewById(R.id.preview_buttons_layout).setVisibility(View.GONE);

        mShowPreview2AnswerLayout = findViewById(R.id.flashcard_layout_flip_preview2);
        mMarkButtonsLayout = findViewById(R.id.mark_buttons);
        mMark1Layout = findViewById(R.id.mark1);
//        mMark2Layout = findViewById(R.id.mark2);
//        mMark3Layout = findViewById(R.id.mark3);
//        mMark4Layout = findViewById(R.id.mark4);

        mMark1Layout.setOnClickListener(new MarkListener(1));
//        mMark2Layout.setOnClickListener(new MarkListener(2));
//        mMark3Layout.setOnClickListener(new MarkListener(3));
//        mMark4Layout.setOnClickListener(new MarkListener(4));

        mNextCardLayout = findViewById(R.id.next);
        mMark1Text = findViewById(R.id.mark1_text);
//        mMark2Text = findViewById(R.id.mark2_text);
//        mMark3Text = findViewById(R.id.mark3_text);
//        mMark4Text = findViewById(R.id.mark4_text);
        mNextText = findViewById(R.id.next_text);

        mMarkButtonsLayout.setVisibility(View.GONE);
        mShowPreview2AnswerLayout.setVisibility(View.VISIBLE);
        mShowPreview2AnswerLayout.setOnClickListener(mToggleAnswerHandler);

//        mPreviewPrevCard.setOnClickListener(mSelectScrollHandler);
        mNextCardLayout.setOnClickListener(mSelectScrollHandler);

        mTextBarNew = (TextView) findViewById(R.id.new_number);
        mTextBarLearn = (TextView) findViewById(R.id.learn_number);
        mTextBarHandled = (TextView) findViewById(R.id.handled_number);
//        if (Build.VERSION.SDK_INT >= 21 && animationEnabled()) {
//            int resId = Themes.getResFromAttr(this, R.attr.hardButtonRippleRef);
//            mPreviewButtonsLayout.setBackgroundResource(resId);
//            mPreviewPrevCard.setBackgroundResource(R.drawable.item_background_light_selectable_borderless);
//            mNextCardLayout.setBackgroundResource(R.drawable.item_background_light_selectable_borderless);
//        }
    }


    private void refreshButtons() {
        switch (mCurrentCard.userFlag()) {
            case 0:
            case 4:
                mMark1Text.setText("标红");
                mMark2Text.setText("标橙");
                mMark3Text.setText("标绿");
                mMark1Layout.setSelected(false);
                mMark2Layout.setSelected(false);
                mMark3Layout.setSelected(false);
                break;
            case 1:
                mMark1Text.setText("取标");
                mMark2Text.setText("标橙");
                mMark3Text.setText("标绿");
                mMark1Layout.setSelected(true);
                mMark2Layout.setSelected(false);
                mMark3Layout.setSelected(false);
                break;
            case 2:
                mMark1Text.setText("标红");
                mMark2Text.setText("取标");
                mMark3Text.setText("标绿");
                mMark1Layout.setSelected(false);
                mMark2Layout.setSelected(true);
                mMark3Layout.setSelected(false);
                break;
            case 3:
                mMark1Text.setText("标红");
                mMark2Text.setText("标橙");
                mMark3Text.setText("取标");
                mMark1Layout.setSelected(false);
                mMark2Layout.setSelected(false);
                mMark3Layout.setSelected(true);
                break;
        }
    }


    private final int[] mFlagRes = {R.mipmap.button_white_flag_normal, R.mipmap.mark_red_flag_normal, R.mipmap.mark_yellow_flag_normal, R.mipmap.mark_green_flag_normal, R.mipmap.mark_blue_flag_normal};


    private List<Long> mHardCardList=new ArrayList<>();//重来一次的卡牌

    class MarkListener implements View.OnClickListener {
        int flag;


        MarkListener(int flag) {
            this.flag = flag;
        }


        @Override
        public void onClick(View v) {

            int random = new Random().nextInt(20) + 10;//限定在1~5张卡后
            int needSwitchIndex = Math.min(mCardList.length - 1, mIndex + random);
            long temId = mCardList[needSwitchIndex];
            mCardList[needSwitchIndex] = mCardList[mIndex];//将当前卡的位置和5张后的卡的位置对调
            if(!mHardCardList.contains(mCardList[mIndex])){
                mHardCardList.add(mCardList[mIndex]);
            }
            mCardList[mIndex] = temId;

            mIndex--;
            mNextCardLayout.performClick();
//            if (v.isSelected()) {
//                flagTask(0);
//            } else {
//                flagTask(flag);
//            }

//            CollectionTask.launchCollectionTask(DISMISS_MULTI,
//                    flagCardHandler(),
//                    new TaskData(new Object[] {new long[] {mCurrentCard.getId()}, Collection.DismissType.FLAG, flag}));

        }
    }


    private FlagCardHandler flagCardHandler() {
        return new FlagCardHandler(this, mCurrentCard);
    }


    private static class FlagCardHandler extends TaskListener {
        Previewer2 activity;
        Card editCard;


        FlagCardHandler(Previewer2 activity, Card editCard) {
            this.activity = activity;
            this.editCard = editCard;
        }


        @Override
        public void onPreExecute() {

        }


        @Override
        public void onPostExecute(TaskData result) {
            Card[] cards = (Card[]) result.getObjArray();
            if (activity.mCurrentCard.getId() == editCard.getId()) {
                activity.mCurrentCard.setUserFlag(cards[0].userFlag());
                activity.invalidateOptionsMenu();
//                activity.refreshButtons();
            }
        }
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

        mTextBarNew.setText(""+(mCardList.length-mIndex-mHardCardList.size()));
        mTextBarLearn.setText(""+mHardCardList.size());
        mTextBarHandled.setText(""+mIndex);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_edit:
                editCard();
                return true;
            case R.id.action_suspend:
                if (mCurrentCard != null && mCurrentCard.getQueue() == Consts.QUEUE_TYPE_SUSPENDED) {
                    Toast.makeText(Previewer2.this, "已恢复该卡牌", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(Previewer2.this, "已暂停该卡牌", Toast.LENGTH_SHORT).show();
                }
                blockControls(false);
                CollectionTask.launchCollectionTask(DISMISS, mDismissCardHandler,
                        new TaskData(new Object[] {mCurrentCard, Collection.DismissType.SUSPEND_CARD}));
//                }

                return true;
            case R.id.action_delete:

                showDeleteNoteDialog();
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
            case R.id.action_mark_card:
                onMark(mCurrentCard);
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
                    mCardList[mIndex] = mCardList[mCardList.length-1];
                    //数组缩容
                    mCardList = Arrays.copyOf(mCardList, mCardList.length-1);
                    mIndex--;
                    mNextCardLayout.performClick();
//                    updateScreenCounts();
                    Toast.makeText(Previewer2.this, "成功删除卡牌", Toast.LENGTH_SHORT).show();

                })
                .build().show();
    }

    protected final TaskListener mDismissCardHandler = new NextCardHandler() { /* superclass is sufficient */

        protected void displayNext(Card nextCard) {
           unblockControls();

//           mNextCardLayout.performClick();
        }
    };

    private void flagTask(int flag) {
        CollectionTask.launchCollectionTask(DISMISS_MULTI,
                flagCardHandler(),
                new TaskData(new Object[] {new long[] {mCurrentCard.getId()}, Collection.DismissType.FLAG, flag}));
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

    private Drawable mEmptyMark;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.previewer2, menu);
//        int alpha = (getControlBlocked() != ReviewerUi.ControlBlock.SLOW) ? Themes.ALPHA_ICON_ENABLED_LIGHT : Themes.ALPHA_ICON_DISABLED_LIGHT ;
        MenuItem markCardIcon = menu.findItem(R.id.action_mark_card);
        if (mCurrentCard != null && mCurrentCard.note().hasTag("marked")) {
            markCardIcon.setTitle(R.string.menu_unmark_note).setIcon(R.mipmap.mark_star_normal);
        } else {
            int[] attrs = new int[] {
                    R.attr.itemIconMarkEmpty,//0
            };
            TypedArray ta = obtainStyledAttributes(attrs);
            mEmptyMark = ta.getDrawable(0);
            markCardIcon.setTitle(R.string.menu_mark_note).setIcon(mEmptyMark);
        }


        MenuItem suspended = menu.findItem(R.id.action_suspend);
        if (mCurrentCard != null && mCurrentCard.getQueue() == Consts.QUEUE_TYPE_SUSPENDED) {
            suspended.setTitle("恢复卡牌");
        } else {
            suspended.setTitle("暂停卡牌");
        }
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
//        flag_icon.getIcon().mutate().setAlpha(alpha);
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


    @Override
    public boolean executeCommand(int which) {
        /* do nothing */
        return false;
    }


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






    private CustomStyleDialog mBeVipDialog;
    private final View.OnClickListener mSelectScrollHandler = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (view.getId() == R.id.next) {
                if(mIndex+1==mCardList.length){
                    //最后一张
                    Toast.makeText(Previewer2.this,"恭喜你，已经学完啦",Toast.LENGTH_SHORT).show();
                    finishActivityWithFade(Previewer2.this);
                    return;
                }
                if (!mVip && mSelfStudyFreeVipCount > 298) {
                    mBeVipDialog = new CustomStyleDialog.Builder(Previewer2.this)
                            .setCustomLayout(R.layout.dialog_common_custom_next)
                            .setTitle("主动练习次数不足")
                            .centerTitle()
                            .setMessage("普通用户累计可主动练习300次，成为学霸用户不限次数，还有更多有用的超能力！")
                            .setPositiveButton("立即升级", (dialog, which) -> {
                                dialog.dismiss();
                                openVipUrl(mVipUrl);
                            })
                            .create();
                    mBeVipDialog.show();
                } else {
                    if(mIndex>=0)
                    mHardCardList.remove(mCardList[mIndex]);
                    mIndex++;

                    mSelfStudyFreeVipCount++;
                    preferences.edit().putInt("self_study_count", mSelfStudyFreeVipCount).apply();
                    mCurrentCard = getCol().getCard(mCardList[mIndex]);
                    displayCardQuestion();
                }
            }
        }
    };

    private View.OnClickListener mToggleAnswerHandler = new View.OnClickListener() {
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
        mMarkButtonsLayout.setVisibility(mShowingAnswer ? View.VISIBLE : View.GONE);
        mShowPreview2AnswerLayout.setVisibility(mShowingAnswer ? View.GONE : View.VISIBLE);
        // If we are in single-card mode, we show the "Show Answer" button on the question side
        // and hide navigation buttons.
//        if (mCardList.length == 1) {
////            mPreviewPrevCard.setVisibility(View.GONE);
//            mNextCardLayout.setVisibility(View.GONE);
//            return;
//        }

//        boolean prevBtnDisabled = mIndex <= 0;
//        boolean nextBtnDisabled = mIndex >= mCardList.length - 1;

//        mPreviewPrevCard.setEnabled(!prevBtnDisabled);
//        mNextCardLayout.setEnabled(!nextBtnDisabled);

//        mPreviewPrevCard.setAlpha(prevBtnDisabled ? 0.38F : 1);
//        mNextCardLayout.setAlpha(nextBtnDisabled ? 0.38F : 1);
        invalidateOptionsMenu();
    }


    @NonNull
    private Intent getResultIntent() {
        Intent intent = new Intent();
        intent.putExtra("reloadRequired", mReloadRequired);
        intent.putExtra("noteChanged", mNoteChanged);
        return intent;
    }
}
