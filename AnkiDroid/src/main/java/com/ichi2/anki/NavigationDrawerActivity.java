///****************************************************************************************
// * Copyright (c) 2014 Timothy Rae <perceptualchaos2@gmail.com>                          *
// *                                                                                      *
// * This program is free software; you can redistribute it and/or modify it under        *
// * the terms of the GNU General Public License as published by the Free Software        *
// * Foundation; either version 3 of the License, or (at your option) any later           *
// * version.                                                                             *
// *                                                                                      *
// * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
// * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
// * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
// *                                                                                      *
// * You should have received a copy of the GNU General Public License along with         *
// * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
// ****************************************************************************************/
//package com.ichi2.anki;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.res.Configuration;
//import android.graphics.drawable.Drawable;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import com.google.android.material.navigation.NavigationView;
//import androidx.core.app.TaskStackBuilder;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.GravityCompat;
//import androidx.appcompat.app.ActionBarDrawerToggle;
//import androidx.appcompat.widget.SwitchCompat;
//import androidx.appcompat.widget.Toolbar;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//
//import com.ichi2.anim.ActivityTransitionAnimation;
//import com.ichi2.anki.web.HostNumFactory;
//import com.ichi2.async.Connection;
//import com.ichi2.compat.CompatHelper;
//import com.ichi2.themes.Themes;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.io.IOException;
//import java.net.URL;
//
//import androidx.drawerlayout.widget.ClosableDrawerLayout;
//
//import androidx.drawerlayout.widget.DrawerLayout;
//import timber.log.Timber;
//
//import static com.ichi2.libanki.Consts.URL_ANKI_COURSE;
//import static com.ichi2.libanki.Consts.URL_FEEDBACK;
//import static com.ichi2.libanki.Consts.URL_UPGRADE_CLOUD_SPACE;
//import static com.ichi2.libanki.Consts.URL_VOLUNTEER;
//import static com.ichi2.libanki.Consts.URL_VERSION;
//
//
//public abstract class NavigationDrawerActivity extends AnkiActivity implements NavigationView.OnNavigationItemSelectedListener {
//
//    /**
//     * Navigation Drawer
//     */
//    protected CharSequence mTitle;
//    protected Boolean mFragmented = false;
//    private boolean mNavButtonGoesBack = false;
//    // Other members
//    private String mOldColPath;
//    private int mOldTheme;
//    // Navigation drawer list item entries
//    private DrawerLayout mDrawerLayout;
//
//    public NavigationView getNavigationView() {
//        return mNavigationView;
//    }
//
//    private NavigationView mNavigationView;
//    private ActionBarDrawerToggle mDrawerToggle;
//    private SwitchCompat mNightModeSwitch;
//    // Intent request codes
//
//    public static final int REQUEST_BROWSE_CARDS = 101;
//    public static final int REQUEST_STATISTICS = 102;
//    private static final String NIGHT_MODE_PREFERENCE = "invertedColors";
//
//    /**
//     * runnable that will be executed after the drawer has been closed.
//     */
//    private Runnable pendingRunnable;
//
//    // Navigation drawer initialisation
//    protected void initNavigationDrawer(View mainView) {
//        // Create inherited navigation drawer layout here so that it can be used by parent class
//        mDrawerLayout = mainView.findViewById(R.id.drawer_layout);
//        // set a custom shadow that overlays the main content when the drawer opens
//        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
//        // Force transparent status bar with primary dark color underlayed so that the drawer displays under status bar
//        CompatHelper.getCompat().setStatusBarColor(getWindow(), ContextCompat.getColor(this, R.color.transparent));
//        mDrawerLayout.setStatusBarBackgroundColor(Themes.getColorFromAttr(this, R.attr.colorPrimaryDark));
//        // Setup toolbar and hamburger
//        mNavigationView = mDrawerLayout.findViewById(R.id.navdrawer_items_container);
//        initMoreDrawerMenuItem();
//        mNavigationView.setNavigationItemSelectedListener(this);
//        mNavigationView.setItemIconTintList(null);
//        Toolbar toolbar = mainView.findViewById(R.id.toolbar);
//        if (toolbar != null) {
//            setSupportActionBar(toolbar);
//            // enable ActionBar app icon to behave as action to toggle nav drawer
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//            getSupportActionBar().setHomeButtonEnabled(true);
//
//            // Decide which action to take when the navigation button is tapped.
//            toolbar.setNavigationOnClickListener(v -> onNavigationPressed());
//        }
//        // Configure night-mode switch
//        final SharedPreferences preferences = getPreferences();
//        View actionLayout = mNavigationView.getMenu().findItem(R.id.nav_night_mode).getActionView();
//        mNightModeSwitch = actionLayout.findViewById(R.id.switch_compat);
//        mNightModeSwitch.setChecked(preferences.getBoolean(NIGHT_MODE_PREFERENCE, false));
//        mNightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> { applyNightMode(isChecked); });
//        // ActionBarDrawerToggle ties together the the proper interactions
//        // between the sliding drawer and the action bar app icon
//        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, 0, 0) {
//            @Override
//            public void onDrawerClosed(View drawerView) {
//                super.onDrawerClosed(drawerView);
//                supportInvalidateOptionsMenu();
//
//                // If animations are disabled, this is executed before onNavigationItemSelected is called
//                // PERF: May be able to reduce this delay
//                new Handler().postDelayed(() -> {
//                    if (pendingRunnable != null) {
//                        new Handler().post(pendingRunnable);
//                        pendingRunnable = null;
//                    }
//                }, 100);
//
//            }
//
//
//            @Override
//            public void onDrawerOpened(View drawerView) {
//                super.onDrawerOpened(drawerView);
//                supportInvalidateOptionsMenu();
//            }
//        };
//        if (mDrawerLayout instanceof ClosableDrawerLayout) {
//            ((ClosableDrawerLayout) mDrawerLayout).setAnimationEnabled(animationEnabled());
//        } else {
//            Timber.w("Unexpected Drawer layout - could not modify navigation animation");
//        }
//        mDrawerToggle.setDrawerSlideAnimationEnabled(animationEnabled());
//        mDrawerLayout.addDrawerListener(mDrawerToggle);
//    }
//    private void initMoreDrawerMenuItem() {
//        Connection.sendCommonGet(initMoreDrawerMenuItemListener, new Connection.Payload("configs/1", "", Connection.Payload.REST_TYPE_GET, HostNumFactory.getInstance(this)));
//    }
//
//
//    Connection.TaskListener initMoreDrawerMenuItemListener = new Connection.TaskListener() {
//
//        @Override
//        public void onProgressUpdate(Object... values) {
//            // Pass
//        }
//
//
//        @Override
//        public void onPreExecute() {
//
//        }
//
//
//        @Override
//        public void onPostExecute(Connection.Payload data) {
//            if (data.success) {
//                Timber.i("initMoreDrawerMenuItem successfully!");
//                try {
//                    final JSONArray items = ((JSONObject) data.result).getJSONArray("data");
//                    Timber.i("initMoreDrawerMenuItem %d ", items.length());
//                    MyAccount myAccount = new MyAccount();
//                    myAccount.getToken(getBaseContext(), new MyAccount.TokenCallback() {
//                        @Override
//                        public void onSuccess(String token) {
//                            for (int i = 0; i < items.length(); i++) {
//                                try {
//                                    JSONObject jsonObject = items.getJSONObject(i);
//                                    MenuItem menuItem = mNavigationView.getMenu().add(10086, 10086 * 10 + i, i, jsonObject.getString("title"));
//                                    menuItem.setOnMenuItemClickListener(item -> {
//                                        try {
//                                            WebViewActivity.openUrlInApp(NavigationDrawerActivity.this, jsonObject.getString("target_url"),token,-1);
//                                        } catch (JSONException e) {
//                                            e.printStackTrace();
//                                        }
//                                        return false;
//                                    });
//                                    new Thread(() -> {
//                                        try {
//
//                                            Drawable drawable = Drawable.createFromStream(
//                                                    new URL(jsonObject.getString("image_url")).openStream(), menuItem.getTitle() + ".jpg");
////                                    Drawable drawable = drawableFromUrl("https://avatar.csdnimg.cn/D/6/2/3_codekxx_1571028676.jpg");
//                                            Timber.i("load drawable result %s ", drawable.toString());
//
//                                            runOnUiThread(() -> {
//                                                        menuItem.setIcon(drawable);
//                                                    }
//                                            );
//                                        } catch (IOException | JSONException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }).start();
//
//                                } catch (JSONException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//                            mNavigationView.getMenu().removeGroup(R.id.group3);
//                        }
//
//                        @Override
//                        public void onFail(String message) {
//
//                        }
//                    });
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    UIUtils.showSimpleSnackbar(NavigationDrawerActivity.this, R.string.sync_menu_error, true);
//                }
//            } else {
//                Timber.e("initMoreDrawerMenuItem failed, error code %d", data.statusCode);
//                UIUtils.showSimpleSnackbar(NavigationDrawerActivity.this, R.string.sync_menu_error, true);
////                if (data.returnType == 403) {
////                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.invalid_username_password, true);
////                } else {
////                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.connection_error_message, true);
////                }
//            }
//        }
//
//
//        @Override
//        public void onDisconnected() {
//            UIUtils.showSimpleSnackbar(NavigationDrawerActivity.this, R.string.youre_offline, true);
//        }
//    };
//
//    /**
//     * Sets selected navigation drawer item
//     */
//    protected void selectNavigationItem(int itemId) {
//        if (mNavigationView == null) {
//            Timber.e("Could not select item in navigation drawer as NavigationView null");
//            return;
//        }
//        Menu menu = mNavigationView.getMenu();
//        if (itemId == -1) {
//            for (int i = 0; i < menu.size(); i++) {
//                menu.getItem(i).setChecked(false);
//            }
//        } else {
//            MenuItem item = menu.findItem(itemId);
//            if (item != null) {
//                item.setChecked(true);
//            } else {
//                Timber.e("Could not find item %d", itemId);
//            }
//        }
//    }
//
//
//    @Override
//    public void setTitle(CharSequence title) {
//        mTitle = title;
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setTitle(mTitle);
//        }
//    }
//
//
//    /**
//     * When using the ActionBarDrawerToggle, you must call it during
//     * onPostCreate() and onConfigurationChanged()...
//     */
//
//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        // Sync the toggle state after onRestoreInstanceState has occurred.
//        if (mDrawerToggle != null) {
//            mDrawerToggle.syncState();
//        }
//    }
//
//    private SharedPreferences getPreferences() {
//        return AnkiDroidApp.getSharedPrefs(NavigationDrawerActivity.this);
//    }
//
//    private void applyNightMode(boolean setToNightMode) {
//        final SharedPreferences preferences = getPreferences();
//        Timber.i("Night mode was %s", setToNightMode ? "enabled" : "disabled");
//        preferences.edit().putBoolean(NIGHT_MODE_PREFERENCE, setToNightMode).apply();
//        restartActivityInvalidateBackstack(NavigationDrawerActivity.this);
//    }
//
//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        // Pass any configuration change to the drawer toggles
//        if (mDrawerToggle != null) {
//            mDrawerToggle.onConfigurationChanged(newConfig);
//        }
//    }
//
//
//    public ActionBarDrawerToggle getDrawerToggle() {
//        return mDrawerToggle;
//    }
//
//    /**
//     * This function locks the navigation drawer closed in regards to swipes,
//     * but continues to allowed it to be opened via it's indicator button. This
//     * function in a noop if the drawer hasn't been initialized.
//     */
//    protected void disableDrawerSwipe() {
//        if (mDrawerLayout != null) {
//            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
//        }
//    }
//
//    /**
//     * This function allows swipes to open the navigation drawer. This
//     * function in a noop if the drawer hasn't been initialized.
//     */
//    protected void enableDrawerSwipe() {
//        if (mDrawerLayout != null) {
//            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
//        }
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        final SharedPreferences preferences = getPreferences();
//        Timber.i("Handling Activity Result: %d. Result: %d", requestCode, resultCode);
//        NotificationChannels.setup(getApplicationContext());
//        // Restart the activity on preference change
//        if (requestCode == REQUEST_PREFERENCES_UPDATE) {
//            if (mOldColPath != null && CollectionHelper.getCurrentAnkiDroidDirectory(this).equals(mOldColPath)) {
//                // collection path hasn't been changed so just restart the current activity
////                if ((this instanceof Reviewer) && preferences.getBoolean("tts", false)) {
////                    // Workaround to kick user back to StudyOptions after opening settings from Reviewer
////                    // because onDestroy() of old Activity interferes with TTS in new Activity
////                    finishWithoutAnimation();
////                } else
//                    if (mOldTheme != Themes.getCurrentTheme(getApplicationContext())) {
//                    // The current theme was changed, so need to reload the stack with the new theme
//                    restartActivityInvalidateBackstack(this);
//                } else {
//                    restartActivity();
//                }
//            } else {
//                // collection path has changed so kick the user back to the DeckPicker
//                CollectionHelper.getInstance().closeCollection(true, "Preference Modification: collection path changed");
//                restartActivityInvalidateBackstack(this);
//            }
//        } else {
//            super.onActivityResult(requestCode, resultCode, data);
//        }
//    }
//
//
//    @Override
//    public void onBackPressed() {
//        if (isDrawerOpen()) {
//            Timber.i("Back key pressed");
//            closeDrawer();
//        } else {
//            super.onBackPressed();
//        }
//    }
//
//    /**
//     * Called, when navigation button of the action bar is pressed.
//     * Design pattern: template method. Subclasses can override this to define their own behaviour.
//     */
//    protected void onNavigationPressed() {
//        if (mNavButtonGoesBack) {
//            finishWithAnimation(ActivityTransitionAnimation.RIGHT);
//        } else {
//            openDrawer();
//        }
//    }
//
//    @Override
//    public boolean onNavigationItemSelected(final MenuItem item) {
//        // Don't do anything if user selects already selected position
//        if (item.isChecked()) {
//            return true;
//        }
//
//        /*
//         * This runnable will be executed in onDrawerClosed(...)
//         * to make the animation more fluid on older devices.
//         */
//        pendingRunnable = () -> {
//            // Take action if a different item selected
//            switch (item.getItemId()) {
//                case R.id.nav_decks: {
//                    Timber.i("Navigating to decks");
//                    Intent deckPicker = new Intent(NavigationDrawerActivity.this, DeckPicker.class);
//                    deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);    // opening DeckPicker should clear back history
//                    startActivityWithAnimation(deckPicker, ActivityTransitionAnimation.RIGHT);
//                    break;
//                }
//                case R.id.nav_browser:
//                    Timber.i("Navigating to card browser");
//                    openCardBrowser();
//                    break;
//                case R.id.nav_stats: {
//                    Timber.i("Navigating to stats");
//                    Intent intent = new Intent(NavigationDrawerActivity.this, Statistics.class);
//                    startActivityForResultWithAnimation(intent, REQUEST_STATISTICS, ActivityTransitionAnimation.LEFT);
//                    break;
//                }
//                case R.id.nav_night_mode:
//                    Timber.i("Toggling Night Mode");
//                    mNightModeSwitch.performClick();
//                    break;
//                case R.id.nav_settings:
//                    Timber.i("Navigating to settings");
//                    mOldColPath = CollectionHelper.getCurrentAnkiDroidDirectory(NavigationDrawerActivity.this);
//                    // Remember the theme we started with so we can restart the Activity if it changes
//                    mOldTheme = Themes.getCurrentTheme(getApplicationContext());
//                    startActivityForResultWithAnimation(new Intent(NavigationDrawerActivity.this, Preferences.class), REQUEST_PREFERENCES_UPDATE, ActivityTransitionAnimation.FADE);
//                    break;
//                case R.id.nav_help:
//                    Timber.i("Navigating to help");
//                    openUrl(Uri.parse(AnkiDroidApp.getManualUrl()));
//                    break;
//                case R.id.nav_cloud_space:
//                case R.id.nav_course:
//                case R.id.nav_volunteers:
//                case R.id.nav_wechat:
//                case R.id.nav_feedback2:
//                    MyAccount myAccount = new MyAccount();
//                     myAccount.getToken(getBaseContext(), new MyAccount.TokenCallback() {
//                        @Override
//                        public void onSuccess(String token) {
//                            switch (item.getItemId()) {
//                                case R.id.nav_cloud_space:
//                                    WebViewActivity.openUrlInApp(NavigationDrawerActivity.this,URL_UPGRADE_CLOUD_SPACE,token,DeckPicker.RESULT_UPDATE_REST_SPACE);
//                                    break;
//                                case R.id.nav_course:
//                                    WebViewActivity.openUrlInApp(NavigationDrawerActivity.this,URL_ANKI_COURSE,token,-1);
//                                    break;
//                                case R.id.nav_volunteers:
//                                    WebViewActivity.openUrlInApp(NavigationDrawerActivity.this,URL_VOLUNTEER,token,-1);
//
//                                    break;
//                                case R.id.nav_wechat:
//                                    WebViewActivity.openUrlInApp(NavigationDrawerActivity.this, URL_VERSION,token,-1);
//
//                                    break;
//                                case R.id.nav_feedback2:
//                                    WebViewActivity.openUrlInApp(NavigationDrawerActivity.this,URL_FEEDBACK,token,-1);
//
//                                    break;
//                                default:
//                                    break;
//                            }
//                        }
//
//                        @Override
//                        public void onFail(String message) {
//                            onTokenExpired();
//                        }
//                    });
//
//                    break;
//            }
//        };
//
//        closeDrawer();
//        return true;
//    }
//    protected void onTokenExpired() {
//
//    }
//    protected void openCardBrowser() {
//        Intent intent = new Intent(NavigationDrawerActivity.this, CardBrowser.class);
//        Long currentCardId = getCurrentCardId();
//        if (currentCardId != null) {
//            intent.putExtra("currentCard", currentCardId);
//        }
//        startActivityForResultWithAnimation(intent, REQUEST_BROWSE_CARDS, ActivityTransitionAnimation.LEFT);
//    }
//
//
//    // Override this to specify a specific card id
//    protected Long getCurrentCardId() {
//        return null;
//    }
//
//    protected void showBackIcon() {
//        if (mDrawerToggle != null) {
//            mDrawerToggle.setDrawerIndicatorEnabled(false);
//        }
//        if (getSupportActionBar() != null) {
//            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        }
//        mNavButtonGoesBack = true;
//    }
//
//    protected void restoreDrawerIcon() {
//        if (mDrawerToggle != null) {
//            getDrawerToggle().setDrawerIndicatorEnabled(true);
//        }
//        mNavButtonGoesBack = false;
//    }
//
//    public boolean isDrawerOpen() {
//        return mDrawerLayout.isDrawerOpen(GravityCompat.START);
//    }
//
//    /**
//     * Restart the activity and discard old backstack, creating it new from the hierarchy in the manifest
//     */
//    protected void restartActivityInvalidateBackstack(AnkiActivity activity) {
//        Timber.i("AnkiActivity -- restartActivityInvalidateBackstack()");
//        Intent intent = new Intent();
//        intent.setClass(activity, activity.getClass());
//        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
//        stackBuilder.addNextIntentWithParentStack(intent);
//        stackBuilder.startActivities(new Bundle());
//        activity.finishWithoutAnimation();
//    }
//    private void openDrawer() {
//        mDrawerLayout.openDrawer(GravityCompat.START, animationEnabled());
//    }
//
//    private void closeDrawer() {
//        mDrawerLayout.closeDrawer(GravityCompat.START, animationEnabled());
//    }
//}
