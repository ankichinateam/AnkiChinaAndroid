/***************************************************************************************
 * Copyright (c) 2012 Norbert Nagold <norbert.nagold@gmail.com>                         *
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

import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.StudyOptionsFragment.StudyOptionsListener;
import com.ichi2.anki.dialogs.CustomStudyDialog;
import com.ichi2.anki.dialogs.DeckPickerExportCompleteDialog;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.widget.WidgetStatus;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

public class StudyOptionsActivity extends AnkiActivity implements StudyOptionsListener,
        CustomStudyDialog.CustomStudyListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mainView = getLayoutInflater().inflate(R.layout.studyoptions, null);
        setContentView(mainView);
        if (savedInstanceState == null) {
            loadStudyOptionsFragment();
        }
    }


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    private void loadStudyOptionsFragment() {
        boolean withDeckOptions = false;
        if (getIntent().getExtras() != null) {
            withDeckOptions = getIntent().getExtras().getBoolean("withDeckOptions");
        }
        StudyOptionsFragment currentFragment = StudyOptionsFragment.newInstance(withDeckOptions);
        getSupportFragmentManager().beginTransaction().replace(R.id.studyoptions_frame, currentFragment).commit();
    }


    private StudyOptionsFragment getCurrentFragment() {
        return (StudyOptionsFragment) getSupportFragmentManager().findFragmentById(R.id.studyoptions_frame);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (getDrawerToggle().onOptionsItemSelected(item)) {
//            return true;
//        }
        switch (item.getItemId()) {

            case android.R.id.home:
                closeStudyOptions();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void closeStudyOptions() {
        closeStudyOptions(RESULT_OK);
    }


    private void closeStudyOptions(int result) {
        // mCompat.invalidateOptionsMenu(this);
        setResult(result);
        finishWithAnimation(ActivityTransitionAnimation.RIGHT);
    }


    @Override
    public void onBackPressed() {
        Timber.i("Back key pressed");
        closeStudyOptions();
    }


    @Override
    public void onStop() {
        super.onStop();
        if (colIsOpen()) {
            WidgetStatus.update(this);
            UIUtils.saveCollectionInBackground();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
//        selectNavigationItem(-1);
    }


    public void onRequireDeckListUpdateWithCacheSelected() {
        getCurrentFragment().refreshInterfaceWithCacheSelected();
    }


    @Override
    public void onRequireDeckListUpdate() {
        getCurrentFragment().refreshInterface();
    }


    /**
     * Callback methods from CustomStudyDialog
     */
    @Override
    public void onCreateCustomStudySession() {
        // Sched already reset by CollectionTask in CustomStudyDialog
        getCurrentFragment().askForRefreshInterface();
    }


    @Override
    public void onExtendStudyLimits() {
        // Sched needs to be reset so provide true argument
        getCurrentFragment().refreshInterface(true);
    }


    protected TaskListener deleteDeckListener(long did) {
        return new DeleteDeckListener(did, this);
    }


    private static class DeleteDeckListener extends TaskListenerWithContext<StudyOptionsActivity> {
        private final long did;
        // Flag to indicate if the deck being deleted is the current deck.
        private boolean removingCurrent;


        public DeleteDeckListener(long did, StudyOptionsActivity deckPicker) {
            super(deckPicker);
            this.did = did;
        }


        @Override
        public void actualOnPreExecute(@NonNull StudyOptionsActivity deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.delete_deck), false);
            if (did == deckPicker.getCol().getDecks().current().optLong("id")) {
                removingCurrent = true;
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        public void actualOnPostExecute(@NonNull StudyOptionsActivity deckPicker, @Nullable TaskData result) {
            // In fragmented mode, if the deleted deck was the current deck, we need to reload
            // the study options fragment with a valid deck and re-center the deck list to the
            // new current deck. Otherwise we just update the list normally.
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                try {
                    deckPicker.mProgressDialog.dismiss();
                } catch (Exception e) {
                    Timber.e(e, "onPostExecute - Exception dismissing dialog");
                }
            }
            if (removingCurrent) {

                deckPicker.finishWithAnimation(ActivityTransitionAnimation.RIGHT);
            } else {
                deckPicker.onRequireDeckListUpdateWithCacheSelected();
            }

        }
    }


    protected TaskListener exportListener() {
        return new ExportListener(this);
    }


    private MaterialDialog mProgressDialog;



    private static class ExportListener extends TaskListenerWithContext<StudyOptionsActivity> {
        public ExportListener(StudyOptionsActivity deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull StudyOptionsActivity deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.export_in_progress), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull StudyOptionsActivity deckPicker, TaskData result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }

            // If boolean and string are both set, we are signalling an error message
            // instead of a successful result.
            if (result.getBoolean() && result.getString() != null) {
                Timber.w("Export Failed: %s", result.getString());
                deckPicker.showSimpleMessageDialog(result.getString());
            } else {
                Timber.i("Export successful");
                String exportPath = result.getString();
                if (exportPath != null) {
                    deckPicker.showAsyncDialogFragment(DeckPickerExportCompleteDialog.newInstance(exportPath));
                } else {
                    UIUtils.showThemedToast(deckPicker, deckPicker.getResources().getString(R.string.export_unsuccessful), true);
                }
            }
        }
    }


    protected void refreshDeckListUI(boolean onlyRefresh) {
        getCurrentFragment().refreshInterface(false, true);
    }


    private static final int MOVE_LIMITATION = 100;// 触发移动的像素距离
    private float mLastMotionX; // 手指触碰屏幕的最后一次x坐标
    private float mLastMotionY; // 手指触碰屏幕的最后一次y坐标


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
//        Timber.i("dispatchTouchEvent:"+event );
//        if(viewPager==null||viewPager.getCurrentItem()!=0)
//            return super.dispatchTouchEvent(event);
        final float x = event.getX();
        final float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = event.getX();
                mLastMotionY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (Math.abs(mLastMotionY - y) < MOVE_LIMITATION && mLastMotionX - x > MOVE_LIMITATION) {
                    // snapToDestination(); // 跳到指定页
                    openCardBrowser();
//                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }
}
