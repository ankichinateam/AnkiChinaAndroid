/***************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
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

package com.ichi2.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences.Editor;

import android.icu.util.Calendar;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.Toast;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.DeckPicker;
import com.ichi2.anki.MetaDB;
import com.ichi2.anki.Preferences;
import com.ichi2.anki.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

@SuppressWarnings("deprecation")
public class CustomDialogPreference extends android.preference.DialogPreference implements DialogInterface.OnClickListener {
    private Context mContext;


    public CustomDialogPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }


    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            if (this.getTitle().equals(mContext.getResources().getString(R.string.deck_conf_reset))) {
                // Deck Options :: Restore Defaults for Options Group
                Editor editor = AnkiDroidApp.getSharedPrefs(mContext).edit();
                editor.putBoolean("confReset", true);
                editor.commit();
            } else if (this.getTitle().equals(mContext.getResources().getString(R.string.clear_local_data_title))) {
                // backup and delete collection.anki2
                File anki = new File(CollectionHelper.getCollectionPath(mContext));
                if (anki.exists()) {
                    boolean result = anki.renameTo(new File(CollectionHelper.getCollectionPath(mContext) + "_backup_" + CollectionHelper.getInstance().getTimeSafe(mContext).intTime()));
                    Toast.makeText(mContext, result ? "备份并清除本地数据成功" : "操作失败", Toast.LENGTH_SHORT).show();
                    if (result) {
                        Intent intent = AnkiDroidApp.getInstance().getPackageManager().getLaunchIntentForPackage(AnkiDroidApp.getInstance().getPackageName());
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        AnkiDroidApp.getInstance().startActivity(intent);
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
////                        Intent intent = Preferences.getPreferenceSubscreenIntent(mContext, "com.ichi2.anki.prefs.advanced");
//                        Intent intent = new Intent(mContext, DeckPicker.class);
////                        intent.setClass(mContext, mContext.getClass());
//                        intent.putExtras(new Bundle());
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
//                        mContext.startActivity(intent);
//
//                        ActivityTransitionAnimation.slide((Activity) mContext, ActivityTransitionAnimation.NONE);
//                        ((Activity) mContext).finish();
//                        ActivityTransitionAnimation.slide((Activity) mContext, ActivityTransitionAnimation.NONE);
                    }
                }
            } else if (this.getTitle().equals(mContext.getResources().getString(R.string.deck_conf_remove))) {
                // Deck Options :: Remove Options Group
                Editor editor = AnkiDroidApp.getSharedPrefs(mContext).edit();
                editor.putBoolean("confRemove", true);
                editor.commit();
            } else if (this.getTitle().equals(mContext.getResources().getString(R.string.deck_conf_set_subdecks))) {
                // Deck Options :: Set Options Group for all Sub-decks
                Editor editor = AnkiDroidApp.getSharedPrefs(mContext).edit();
                editor.putBoolean("confSetSubdecks", true);
                editor.commit();
            } else {
                // Main Preferences :: Reset Languages
                if (MetaDB.resetLanguages(mContext) && MetaDB.resetSpeech(mContext) && MetaDB.resetAzureLanguages(mContext)) {

                    Toast successReport = Toast.makeText(this.getContext(),
                            AnkiDroidApp.getAppResources().getString(R.string.reset_confirmation), Toast.LENGTH_SHORT);
                    successReport.show();
                }
            }
        }
    }

}
