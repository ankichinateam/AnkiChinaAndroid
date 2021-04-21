/***************************************************************************************
 * Copyright (c) 2011 Norbert Nagold <norbert.nagold@gmail.com>                         *
 * Copyright (c) 2015 Timothy Rae <perceptualchaos2@gmail.com>                          *
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

package com.ichi2.themes;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import timber.log.Timber;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.R;

public class Themes {
    public final static int ALPHA_ICON_ENABLED_LIGHT = 255; // 100%
    public final static int ALPHA_ICON_DISABLED_LIGHT = 76; // 31%
    public final static int ALPHA_ICON_ENABLED_DARK = 138; // 54%

    // Day themes
    public final static int THEME_DAY_LIGHT = 0;
    public final static int THEME_DAY_PLAIN = 1;
    // Night themes
    public final static int THEME_NIGHT_BLACK = 0;
    public final static int THEME_NIGHT_DARK = 1;
    public final static int NO_SPECIFIC_STATUS_BAR_COLOR = -1008611;


    public static void setTheme(Context context, boolean transparent, int colorAttr) {
        SharedPreferences prefs = AnkiDroidApp.getSharedPrefs(context.getApplicationContext());
//        int colorResId = NO_SPECIFIC_STATUS_BAR_COLOR;
//        if (colorAttr != NO_SPECIFIC_STATUS_BAR_COLOR) {
//            TypedValue value = new TypedValue();
//            context.getTheme().resolveAttribute(colorAttr, value, true);
//            colorResId = value.resourceId;
//        }
        if (prefs.getBoolean("invertedColors", false)) {
            int theme = Integer.parseInt(prefs.getString("nightTheme", "0"));
            switch (theme) {
                case THEME_NIGHT_DARK:
                    context.setTheme(R.style.Theme_Dark_Compat);
                    setImgTransparent((Activity) context, true, transparent, colorAttr);
                    break;
                case THEME_NIGHT_BLACK:
                    context.setTheme(R.style.Theme_Black_Compat);
                    setImgTransparent((Activity) context, true, transparent, colorAttr);
                    break;
            }
        } else {
            int theme = Integer.parseInt(prefs.getString("dayTheme", "0"));
            switch (theme) {
                case THEME_DAY_LIGHT:
                    context.setTheme(R.style.Theme_Light_Compat);
                    setImgTransparent((Activity) context, false, transparent, colorAttr);
                    break;
                case THEME_DAY_PLAIN:
                    context.setTheme(R.style.Theme_Plain_Compat);
                    setImgTransparent((Activity) context, false, transparent, colorAttr);
                    break;
            }
        }


    }


    public static void setTheme(Context context, boolean transparent) {
        setTheme(context, transparent, NO_SPECIFIC_STATUS_BAR_COLOR);
    }


    public static void setThemeLegacy(Context context) {
        SharedPreferences prefs = AnkiDroidApp.getSharedPrefs(context.getApplicationContext());
        if (prefs.getBoolean("invertedColors", false)) {
            int theme = Integer.parseInt(prefs.getString("nightTheme", "0"));
            switch (theme) {
                case THEME_NIGHT_DARK:
                    context.setTheme(R.style.LegacyActionBarDark);
                    break;
                case THEME_NIGHT_BLACK:
                    context.setTheme(R.style.LegacyActionBarBlack);
                    break;
            }
        } else {
            int theme = Integer.parseInt(prefs.getString("dayTheme", "0"));
            switch (theme) {
                case THEME_DAY_LIGHT:
                    context.setTheme(R.style.LegacyActionBarLight);
                    break;
                case THEME_DAY_PLAIN:
                    context.setTheme(R.style.LegacyActionBarPlain);
                    break;
            }
        }
//        setImgTransparent((Activity) context);
    }


    private static void setImgTransparent(Activity activity, boolean isDark, boolean transparent, int colorAttr) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = activity.getWindow();
            window.clearFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                            | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  //不隐藏和透明虚拟导航栏  因为会遮盖底部的布局
                            | (isDark || !transparent ? View.SYSTEM_UI_FLAG_LAYOUT_STABLE : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)//保持布局状态
            );
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//            int colorResId = NO_SPECIFIC_STATUS_BAR_COLOR;
//            if (colorAttr != NO_SPECIFIC_STATUS_BAR_COLOR) {
//                TypedValue value = new TypedValue();
//                activity.getTheme().resolveAttribute(colorAttr, value, true);
//                colorResId = value.resourceId;
//            }
            Timber.i("colorAttr %s", colorAttr);
            if (colorAttr != NO_SPECIFIC_STATUS_BAR_COLOR) {
                window.setStatusBarColor(getColorFromAttr(activity,colorAttr));
            } else if (transparent) {
                window.setStatusBarColor(Color.TRANSPARENT);
            } else if (isDark) {
                window.setStatusBarColor(Color.TRANSPARENT);
            } else {
                window.setStatusBarColor(ContextCompat.getColor(activity, R.color.primary_color));
            }
//            window.setNavigationBarColor(Color.TRANSPARENT);//不隐藏和透明虚拟导航栏  因为会遮盖底部的布局

        }
    }


    public static int getResFromAttr(Context context, int resAttr) {
        int[] attrs = new int[] {resAttr};
        return getResFromAttr(context, attrs)[0];
    }


    public static int[] getResFromAttr(Context context, int[] attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs);
        for (int i = 0; i < attrs.length; i++) {
            attrs[i] = ta.getResourceId(i, 0);
        }
        ta.recycle();
        return attrs;
    }


    public static int getColorFromAttr(Context context, int colorAttr) {
        int[] attrs = new int[] {colorAttr};
        return getColorFromAttr(context, attrs)[0];
    }


    public static int[] getColorFromAttr(Context context, int[] attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs);
        for (int i = 0; i < attrs.length; i++) {
            attrs[i] = ta.getColor(i, ContextCompat.getColor(context, R.color.white));
        }
        ta.recycle();
        return attrs;
    }


    /**
     * Return the current integer code of the theme being used, taking into account
     * whether we are in day mode or night mode.
     */
    public static int getCurrentTheme(Context context) {
        SharedPreferences prefs = AnkiDroidApp.getSharedPrefs(context);
        if (prefs.getBoolean("invertedColors", false)) {
            return Integer.parseInt(prefs.getString("nightTheme", "0"));
        } else {
            return Integer.parseInt(prefs.getString("dayTheme", "0"));
        }
    }
}
