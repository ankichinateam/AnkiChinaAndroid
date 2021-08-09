
package com.ichi2.anki;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.analytics.UsageAnalytics;
import com.ichi2.anki.dialogs.AsyncDialogFragment;
import com.ichi2.anki.dialogs.CustomStudyDialog;
import com.ichi2.anki.dialogs.DatabaseErrorDialog;
import com.ichi2.anki.dialogs.DeckPickerConfirmDeleteDeckDialog;
import com.ichi2.anki.dialogs.DialogHandler;
import com.ichi2.anki.dialogs.ExportDialog;
import com.ichi2.anki.dialogs.ImportDialog;
import com.ichi2.anki.dialogs.SimpleMessageDialog;
import com.ichi2.anki.dialogs.SyncErrorDialog;
import com.ichi2.anki.exception.DeckRenameException;
import com.ichi2.async.CollectionLoader;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.compat.CompatHelper;
import com.ichi2.compat.customtabs.CustomTabActivityHelper;
import com.ichi2.compat.customtabs.CustomTabsFallback;
import com.ichi2.compat.customtabs.CustomTabsHelper;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.importer.AnkiPackageImporter;
import com.ichi2.libanki.utils.TimeUtils;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.themes.Themes;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.utils.ImportUtils;
import com.umeng.analytics.MobclickAgent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import timber.log.Timber;

import static com.ichi2.anki.DeckPicker.BE_VIP;
import static com.ichi2.anki.DeckPicker.CONFIRM_PRIVATE_STRATEGY;
import static com.ichi2.anki.DeckPicker.REFRESH_LOGIN_STATE;
import static com.ichi2.anki.DeckPicker.REQUEST_BROWSE_CARDS;
import static com.ichi2.anki.MyAccount.NOT_LOGIN_ANKI_CHINA;
import static com.ichi2.anki.MyAccount.NO_TOKEN_RECORD;
import static com.ichi2.anki.MyAccount.NO_WRITEABLE_PERMISSION;
import static com.ichi2.anki.MyAccount.TOKEN_IS_EXPIRED;
import static com.ichi2.anki.SelfStudyActivity.ALL_DECKS_ID;
import static com.ichi2.anki.SelfStudyActivity.TAB_MAIN_STATE;
import static com.ichi2.anki.SelfStudyActivity.saveLastDeckId;
import static com.ichi2.async.CollectionTask.TASK_TYPE.DELETE_DECK;
import static com.ichi2.async.CollectionTask.TASK_TYPE.EMPTY_CRAM;
import static com.ichi2.async.CollectionTask.TASK_TYPE.EXPORT_APKG;
import static com.ichi2.async.CollectionTask.TASK_TYPE.IMPORT;
import static com.ichi2.async.CollectionTask.TASK_TYPE.IMPORT_REPLACE;
import static com.ichi2.async.CollectionTask.TASK_TYPE.REBUILD_CRAM;
import static com.ichi2.themes.Themes.NO_SPECIFIC_STATUS_BAR_COLOR;

public class AnkiActivity extends AppCompatActivity implements SimpleMessageDialog.SimpleMessageDialogListener, ExportDialog.ExportDialogListener, CustomStudyDialog.CustomStudyListener, ImportDialog.ImportDialogListener {

    public final int SIMPLE_NOTIFICATION_ID = 0;
    public static final int REQUEST_REVIEW = 901;
    /**
     * The name of the parent class (Reviewer)
     */
    private final String mActivityName;

    private DialogHandler mHandler = new DialogHandler(this);

    // custom tabs
    private CustomTabActivityHelper mCustomTabActivityHelper;

    private boolean mIsDestroyed = false;


    public AnkiActivity() {
        super();
        this.mActivityName = getClass().getSimpleName();
    }

    public void openCardBrowser(long deckId) {
        saveLastDeckId(deckId);
        Intent intent = new Intent(this, SelfStudyActivity.class);
        intent.putExtra("type",TAB_MAIN_STATE);
        startActivityForResultWithAnimation(intent, REQUEST_BROWSE_CARDS, ActivityTransitionAnimation.LEFT);
    }
    public void openCardBrowser() {
        Intent intent = new Intent(this, SelfStudyActivity.class);
        intent.putExtra("type",TAB_MAIN_STATE);
        startActivityForResultWithAnimation(intent, REQUEST_BROWSE_CARDS, ActivityTransitionAnimation.LEFT);
    }
    public void openOldCardBrowser() {
        Intent intent = new Intent(this, CardBrowser.class);
        startActivityForResultWithAnimation(intent, REQUEST_BROWSE_CARDS, ActivityTransitionAnimation.LEFT);

    }

    MyAccount _myAccount;


    public MyAccount getAccount() {
        if (_myAccount == null) {
            _myAccount = new MyAccount();
        }
        return _myAccount;
    }


    public long serverRestSpace = -1;


    public void openVipUrl(String url) {
        getAccount().getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                if (url != null && !url.isEmpty()) {
                    WebViewActivity.openUrlInApp(AnkiActivity.this, String.format(url, token, BuildConfig.VERSION_NAME), token, BE_VIP);
                }
//                openUrl(Uri.parse(getResources().getString(R.string.shared_decks_url, token)));
            }


            @Override
            public void onFail(String message) {
//                if (message.equals(NOT_LOGIN_ANKI_CHINA)) {
//
//
////                        Toast.makeText(getAnkiActivity(), "Anki Web账号登录，无需扩容", Toast.LENGTH_SHORT).show();
////                            UIUtils.showSimpleSnackbar(getAnkiActivity(), "Anki Web账号登录，无需扩容", true);
//                    return;
//                } else if (message.equals(NO_TOKEN_RECORD)) {
//
//                }
                Toast.makeText(AnkiActivity.this, "当前未使用Anki记忆卡账号登录，无法获得超级学霸功能", Toast.LENGTH_SHORT).show();
                Intent myAccount = new Intent(AnkiActivity.this, MyAccount.class);
                myAccount.putExtra("notLoggedIn", true);
                startActivityForResultWithAnimation(myAccount, REFRESH_LOGIN_STATE, ActivityTransitionAnimation.FADE);
//                startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
                handleGetTokenFailed(message);
//                WebViewActivity.openUrlInApp(DeckPicker.this,String.format(url+"app-inner=yes&app-token=%s&app-version=%s", "",BuildConfig.VERSION_NAME),"");

            }
        });

    }


    protected void handleGetTokenFailed(String message) {
        if (message.equals(NO_WRITEABLE_PERMISSION)) {
            onNoWriteablePermission();
        } else if (message.equals(TOKEN_IS_EXPIRED)) {
            onTokenExpired();
        }
    }


    protected void onNoWriteablePermission() {

    }


    protected void onTokenExpired() {

    }


    protected boolean showedActivityFailedScreen(Bundle savedInstanceState) {
        if (!AnkiDroidApp.isInitialized()) {
            return false;
        }

        // #7630: Can be triggered with `adb shell bmgr restore com.ichi2.anki` after AnkiDroid settings are changed.
        // Application.onCreate() is not called if:
        // * The App was open
        // * A restore took place
        // * The app is reopened (until it exits: finish() does not do this - and removes it from the app list)

        Timber.w("Activity started with no application instance");
        UIUtils.showThemedToast(this, getString(R.string.ankidroid_cannot_open_after_backup_try_again), false);

        // Avoids a SuperNotCalledException
        super.onCreate(savedInstanceState);
        finishActivityWithFade(this);

        // If we don't kill the process, the backup is not "done" and reopening the app show the same message.
        new Thread(() -> {
            // 3.5 seconds sleep, as the toast is killed on process death.
            // Same as the default value of LENGTH_LONG
            try {
                Thread.sleep(3500);
            } catch (InterruptedException e) {
                Timber.w(e);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
        }).start();

        return true;
    }


    public void saveServerRestSpace(long space) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        SharedPreferences.Editor editor = preferences.edit();
        serverRestSpace = space;
        editor.putLong("serverRestSpace", space);
        editor.apply();
    }


    public long getServerRestSpace() {
        if (serverRestSpace > -1) {
            return serverRestSpace;
        }
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        serverRestSpace = preferences.getLong("serverRestSpace", -1);
        return serverRestSpace;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Timber.i("AnkiActivity::onCreate - %s-%d", mActivityName, getStatusBarColorAttr());
        // The hardware buttons should control the music volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // Set the theme
        TypedValue value = new TypedValue();
        getTheme().resolveAttribute(getStatusBarColorAttr(), value, true);
        Timber.i("AnkiActivity::onCreate - %s-%d", mActivityName, value.resourceId);
        Themes.setTheme(this, isStatusBarTransparent(), getStatusBarColorAttr());
        super.onCreate(savedInstanceState);
        // Disable the notifications bar if running under the test monkey.
        if (AdaptionUtil.isUserATestClient()) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        mCustomTabActivityHelper = new CustomTabActivityHelper();
    }


    protected boolean isStatusBarTransparent() {
        return false;
    }


    protected int getStatusBarColorAttr() {
        return NO_SPECIFIC_STATUS_BAR_COLOR;
    }


    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(AnkiDroidApp.updateContextWithLanguage(base));
    }


    @Override
    protected void onStart() {
        Timber.i("AnkiActivity::onStart - %s", mActivityName);
        super.onStart();
        mCustomTabActivityHelper.bindCustomTabsService(this);
    }


    @Override
    protected void onStop() {
        Timber.i("AnkiActivity::onStop - %s", mActivityName);
        super.onStop();
        mCustomTabActivityHelper.unbindCustomTabsService(this);
    }


    @Override
    protected void onPause() {
        Timber.i("AnkiActivity::onPause - %s", mActivityName);
        super.onPause();
        MobclickAgent.onPause(this);
    }


    @Override
    protected void onResume() {
        Timber.i("AnkiActivity::onResume - %s", mActivityName);
        super.onResume();

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(SIMPLE_NOTIFICATION_ID);
        // Show any pending dialogs which were stored persistently
        mHandler.readMessage();
        if ( AnkiDroidApp.getSharedPrefs(this).contains(CONFIRM_PRIVATE_STRATEGY)){
            UsageAnalytics.sendAnalyticsScreenView(this);
            MobclickAgent.onResume(this);
        }

    }


    @Override
    protected void onDestroy() {
        this.mIsDestroyed = true;
        Timber.i("AnkiActivity::onDestroy - %s", mActivityName);
        super.onDestroy();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Timber.i("Home button pressed");
                finishWithoutAnimation();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    // called when the CollectionLoader finishes... usually will be over-ridden
    protected void onCollectionLoaded(Collection col) {
        hideProgressBar();
    }


    private static final int REQUEST_STORAGE_PERMISSION = 0;


    public Collection getCol(Context context) {
        return CollectionHelper.getInstance().getCol(context);
    }


    public Collection getCol() {
        return CollectionHelper.getInstance().getCol(this);
    }


    public boolean colIsOpen() {
        return CollectionHelper.getInstance().colIsOpen();
    }


    public boolean animationDisabled() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        return preferences.getBoolean("safeDisplay", false);
    }


    public boolean animationEnabled() {
        return !animationDisabled();
    }


    @Override
    public void setContentView(View view) {
        if (animationDisabled()) {
            view.clearAnimation();
        }
        super.setContentView(view);
    }


    @Override
    public void setContentView(View view, LayoutParams params) {
        if (animationDisabled()) {
            view.clearAnimation();
        }
        super.setContentView(view, params);
    }


    @Override
    public void addContentView(View view, LayoutParams params) {
        if (animationDisabled()) {
            view.clearAnimation();
        }
        super.addContentView(view, params);
    }


    @Deprecated
    @Override
    public void startActivity(Intent intent) {
        Timber.i("startActivity:" + intent.getPackage());
        super.startActivity(intent);
    }


    public void startActivityWithoutAnimation(Intent intent) {
        Timber.i("startActivityWithoutAnimation:" + intent.getPackage());
        disableIntentAnimation(intent);
        super.startActivity(intent);
        disableActivityAnimation();
    }


    public void startActivityWithAnimation(Intent intent, int animation) {
        Timber.i("startActivityWithAnimation:" + intent.getComponent());
        enableIntentAnimation(intent);
        super.startActivity(intent);
        enableActivityAnimation(animation);
    }


    @Deprecated
    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (ActivityNotFoundException e) {
            UIUtils.showSimpleSnackbar(this, R.string.activity_start_failed, true);
        }
    }


    public void startActivityForResultWithoutAnimation(Intent intent, int requestCode) {
        Timber.i("startActivityForResultWithoutAnimation:" + intent.getPackage());
        disableIntentAnimation(intent);
        startActivityForResult(intent, requestCode);
        disableActivityAnimation();
    }


    public void startActivityForResultWithAnimation(Intent intent, int requestCode, int animation) {
        Timber.i("finishWithoutAnimation:" + intent.getPackage());
        enableIntentAnimation(intent);
        startActivityForResult(intent, requestCode);
        enableActivityAnimation(animation);
    }


    @Deprecated
    @Override
    public void finish() {
        super.finish();
    }


    public void finishWithoutAnimation() {
        Timber.i("finishWithoutAnimation");
        super.finish();
        disableActivityAnimation();
    }


    public void finishWithAnimation(int animation) {
        Timber.i("finishWithAnimation %d", animation);
        super.finish();
        enableActivityAnimation(animation);
    }


    protected void disableViewAnimation(View view) {
        view.clearAnimation();
    }


    /**
     * Compat shim for API 16
     */
    public boolean wasDestroyed() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            return super.isDestroyed();
        }
        return mIsDestroyed;
    }


    protected void enableViewAnimation(View view, Animation animation) {
        if (animationDisabled()) {
            disableViewAnimation(view);
        } else {
            view.setAnimation(animation);
        }
    }


    /**
     * Finish Activity using FADE animation
     **/
    public static void finishActivityWithFade(Activity activity) {
        activity.finish();
        ActivityTransitionAnimation.slide(activity, ActivityTransitionAnimation.UP);
    }


    /**
     * Finish Activity using FADE animation
     **/
    public static void finishActivityWithFade(Activity activity, int direction) {
        activity.finish();
        ActivityTransitionAnimation.slide(activity, direction);
    }


    private void disableIntentAnimation(Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
    }


    private void disableActivityAnimation() {
        ActivityTransitionAnimation.slide(this, ActivityTransitionAnimation.NONE);
    }


    private void enableIntentAnimation(Intent intent) {
        if (animationDisabled()) {
            disableIntentAnimation(intent);
        }
    }


    private void enableActivityAnimation(int animation) {
        if (animationDisabled()) {
            disableActivityAnimation();
        } else {
            ActivityTransitionAnimation.slide(this, animation);
        }
    }


    // Method for loading the collection which is inherited by all AnkiActivitys
    public void startLoadingCollection() {
        Timber.d("AnkiActivity.startLoadingCollection()");
        if (colIsOpen()) {
            Timber.d("Synchronously calling onCollectionLoaded");
            onCollectionLoaded(getCol());
            return;
        }
        // Open collection asynchronously if it hasn't already been opened
        showProgressBar();
        CollectionLoader.load(this, col -> {
            if (col != null) {
                Timber.d("Asynchronously calling onCollectionLoaded");
                onCollectionLoaded(col);
            } else {
                Intent deckPicker = new Intent(this, DeckPicker.class);
                deckPicker.putExtra("collectionLoadError", true); // don't currently do anything with this
                deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivityWithAnimation(deckPicker, ActivityTransitionAnimation.LEFT);
            }
        });
    }


    public void showProgressBar() {
        Timber.w("showProgressBar");
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }
    }


    public void hideProgressBar() {
        Timber.w("hideProgressBar");
        ProgressBar progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }


    protected void mayOpenUrl(Uri url) {
        boolean success = mCustomTabActivityHelper.mayLaunchUrl(url, null, null);
        if (!success) {
            Timber.w("Couldn't preload url: %s", url.toString());
        }
    }


    public void openUrl(Uri url) {
        //DEFECT: We might want a custom view for the toast, given i8n may make the text too long for some OSes to
        //display the toast
        Timber.i("final get url:%s", url);

        if (!AdaptionUtil.hasWebBrowser(this)) {
            UIUtils.showThemedToast(this, getResources().getString(R.string.no_browser_notification) + url, false);
            return;
        }

        CustomTabActivityHelper helper = getCustomTabActivityHelper();
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(helper.getSession());
        builder.setToolbarColor(ContextCompat.getColor(this, R.color.material_light_blue_500)).setShowTitle(true);
        builder.setStartAnimations(this, R.anim.slide_right_in, R.anim.slide_left_out);
        builder.setExitAnimations(this, R.anim.slide_left_in, R.anim.slide_right_out);
        builder.setCloseButtonIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_arrow_back_white_24dp));
        CustomTabsIntent customTabsIntent = builder.build();
        CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent);
        CustomTabActivityHelper.openCustomTab(this, customTabsIntent, url, new CustomTabsFallback());
    }


    public CustomTabActivityHelper getCustomTabActivityHelper() {
        return mCustomTabActivityHelper;
    }


    /**
     * Global method to show dialog fragment including adding it to back stack Note: DO NOT call this from an async
     * task! If you need to show a dialog from an async task, use showAsyncDialogFragment()
     *
     * @param newFragment the DialogFragment you want to show
     */
    public void showDialogFragment(DialogFragment newFragment) {
        showDialogFragment(this, newFragment);
    }


    public static void showDialogFragment(AnkiActivity activity, DialogFragment newFragment) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentManager manager = activity.getSupportFragmentManager();
        FragmentTransaction ft = manager.beginTransaction();
        Fragment prev = manager.findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        // save transaction to the back stack
        ft.addToBackStack("dialog");
        newFragment.show(ft, "dialog");
        manager.executePendingTransactions();
    }


    /**
     * Calls {@link #showAsyncDialogFragment(AsyncDialogFragment, NotificationChannels.Channel)} internally, using the channel
     * {@link NotificationChannels.Channel#GENERAL}
     *
     * @param newFragment the AsyncDialogFragment you want to show
     */
    public void showAsyncDialogFragment(AsyncDialogFragment newFragment) {
        showAsyncDialogFragment(newFragment, NotificationChannels.Channel.GENERAL);
    }


    public void showDatabaseErrorDialog(int id) {
        AsyncDialogFragment newFragment = DatabaseErrorDialog.newInstance(id);
        showAsyncDialogFragment(newFragment);
    }


    /**
     * Global method to show a dialog fragment including adding it to back stack and handling the case where the dialog
     * is shown from an async task, by showing the message in the notification bar if the activity was stopped before the
     * AsyncTask completed
     *
     * @param newFragment the AsyncDialogFragment you want to show
     * @param channel     the NotificationChannels.Channel to use for the notification
     */
    public void showAsyncDialogFragment(AsyncDialogFragment newFragment, NotificationChannels.Channel channel) {
        try {

            showDialogFragment(newFragment);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            // Store a persistent message to SharedPreferences instructing AnkiDroid to show dialog
            DialogHandler.storeMessage(newFragment.getDialogHandlerMessage());
            // Show a basic notification to the user in the notification bar in the meantime
            String title = newFragment.getNotificationTitle();
            String message = newFragment.getNotificationMessage();
            showSimpleNotification(title, message, channel);
        }
    }


    /**
     * Show a simple message dialog, dismissing the message without taking any further action when OK button is pressed.
     * If a DialogFragment cannot be shown due to the Activity being stopped then the message is shown in the
     * notification bar instead.
     *
     * @param message
     */
    protected void showSimpleMessageDialog(String message) {
        showSimpleMessageDialog(message, false);
    }


    protected void showSimpleMessageDialog(String title, String message) {
        showSimpleMessageDialog(title, message, false);
    }


    /**
     * Show a simple message dialog, dismissing the message without taking any further action when OK button is pressed.
     * If a DialogFragment cannot be shown due to the Activity being stopped then the message is shown in the
     * notification bar instead.
     *
     * @param message
     * @param reload  flag which forces app to be restarted when true
     */
    protected void showSimpleMessageDialog(String message, boolean reload) {
        AsyncDialogFragment newFragment = SimpleMessageDialog.newInstance(message, reload);
        showAsyncDialogFragment(newFragment);
    }


    protected void showSimpleMessageDialog(String title, @Nullable String message, boolean reload) {
        AsyncDialogFragment newFragment = SimpleMessageDialog.newInstance(title, message, reload);
        showAsyncDialogFragment(newFragment);
    }


    public void showSimpleNotification(String title, String message, NotificationChannels.Channel channel) {
        SharedPreferences prefs = AnkiDroidApp.getSharedPrefs(this);
        // Show a notification unless all notifications have been totally disabled
        if (Integer.parseInt(prefs.getString("minimumCardsDueForNotification", "0")) <= Preferences.PENDING_NOTIFICATIONS_ONLY) {
            // Use the title as the ticker unless the title is simply "AnkiDroid"
            String ticker = title;
            if (title.equals(getResources().getString(R.string.app_name))) {
                ticker = message;
            }
            // Build basic notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                    NotificationChannels.getId(channel))
                    .setSmallIcon(R.drawable.ic_stat_notify)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setColor(ContextCompat.getColor(this, R.color.material_light_blue_500))
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setTicker(ticker);
            // Enable vibrate and blink if set in preferences
            if (prefs.getBoolean("widgetVibrate", false)) {
                builder.setVibrate(new long[] {1000, 1000, 1000});
            }
            if (prefs.getBoolean("widgetBlink", false)) {
                builder.setLights(Color.BLUE, 1000, 1000);
            }
            // Creates an explicit intent for an Activity in your app
            Intent resultIntent = new Intent(this, DeckPicker.class);
            resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(resultPendingIntent);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // mId allows you to update the notification later on.
            notificationManager.notify(SIMPLE_NOTIFICATION_ID, builder.build());
        }

    }


    public DialogHandler getDialogHandler() {
        return mHandler;
    }


    // Handle closing simple message dialog
    @Override
    public void dismissSimpleMessageDialog(boolean reload) {
        dismissAllDialogFragments();
        if (reload) {
            Intent deckPicker = new Intent(this, DeckPicker.class);
            deckPicker.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityWithoutAnimation(deckPicker);
        }
    }


//    @Override
//    public void exportApkg(String path, Long did, boolean includeSched, boolean includeMedia) {
//
//    }


    // Restart the activity
    public void restartActivity() {
        Timber.i("AnkiActivity -- restartActivity()");
        Intent intent = new Intent();
        intent.setClass(this, this.getClass());
        intent.putExtras(new Bundle());
        this.startActivityWithoutAnimation(intent);
        this.finishWithoutAnimation();
    }


    protected void enableToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }


    protected void enableToolbar(@Nullable View view) {
        if (view == null) {
            Timber.w("Unable to enable toolbar - invalid view supplied");
            return;
        }
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }


    // Callback to show study options for currently selected deck
    public void showContextMenuDeckOptions() {
        // open deck options
        if (getCol().getDecks().isDyn(mContextMenuDid)) {
            // open cram options if filtered deck
            Intent i = new Intent(this, FilteredDeckOptions.class);
            i.putExtra("did", mContextMenuDid);
            this.startActivityWithAnimation(i, ActivityTransitionAnimation.FADE);
        } else {
            // otherwise open regular options
            Intent i = new Intent(this, DeckOptions.class);
            i.putExtra("did", mContextMenuDid);
            this.startActivityWithAnimation(i, ActivityTransitionAnimation.FADE);
        }
    }


    public void confirmDeckDeletion() {
        confirmDeckDeletion(mContextMenuDid);
    }


    public void confirmDeckDeletion(long did) {
        Resources res = getResources();
        if (!this.colIsOpen()) {
            return;
        }
        if (did == 1) {
            UIUtils.showSimpleSnackbar(this, R.string.delete_deck_default_deck, true);
            this.dismissAllDialogFragments();
            return;
        }
        // Get the number of cards contained in this deck and its subdecks
        TreeMap<String, Long> children = getCol().getDecks().children(did);
        long[] dids = new long[children.size() + 1];
        dids[0] = did;
        int i = 1;
        for (Long l : children.values()) {
            dids[i++] = l;
        }
        String ids = Utils.ids2str(dids);
        int cnt = getCol().getDb().queryScalar(
                "select count() from cards where did in " + ids + " or odid in " + ids);
        // Delete empty decks without warning
        if (cnt == 0) {
            deleteDeck(did);
            this.dismissAllDialogFragments();
            return;
        }
        // Otherwise we show a warning and require confirmation
        String msg;
        String deckName = "\'" + getCol().getDecks().name(did) + "\'";
        boolean isDyn = getCol().getDecks().isDyn(did);
        if (isDyn) {
            msg = res.getString(R.string.delete_cram_deck_message, deckName);
        } else {
            msg = res.getQuantityString(R.plurals.delete_deck_message, cnt, deckName, cnt);
        }
        showDialogFragment(DeckPickerConfirmDeleteDeckDialog.newInstance(msg));
    }


    protected long mContextMenuDid;


    public void setContextMenuDid(long mContextMenuDid) {
        this.mContextMenuDid = mContextMenuDid;
    }


    // Callback to delete currently selected deck
    public void deleteContextMenuDeck() {
        deleteDeck(mContextMenuDid);
    }


    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong("mContextMenuDid", mContextMenuDid);
    }


    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mContextMenuDid = savedInstanceState.getLong("mContextMenuDid");
    }


    public void deleteDeck(final long did) {
        try {
            TaskListener listener = deleteDeckListener(did);
            CollectionTask.launchCollectionTask(DELETE_DECK, listener, new TaskData(did));
        } catch (Exception e) {
        }

    }


    protected TaskListener deleteDeckListener(long did) throws Exception {
        throw new Exception("not implemented delete listener");
    }


    protected TaskListener exportListener() throws Exception {
        throw new Exception("not implemented export listener");
    }


    protected void refreshDeckListUI(boolean onlyRefresh) throws Exception {
        throw new Exception("not override refresh deck ui ");
    }


    // Callback to show dialog to rename the current deck
    public void renameDeckDialog() {
        renameDeckDialog(mContextMenuDid);
    }


    private EditText mDialogEditText;


    public void renameDeckDialog(final long did) {
        final Resources res = getResources();
        mDialogEditText = new EditText(this);
        mDialogEditText.setSingleLine();
        final String currentName = getCol().getDecks().name(did);
        mDialogEditText.setText(currentName);
        mDialogEditText.setSelection(mDialogEditText.getText().length());
        new MaterialDialog.Builder(this)
                .title(res.getString(R.string.rename_deck))
                .customView(mDialogEditText, true)
                .positiveText(res.getString(R.string.rename))
                .negativeText(res.getString(R.string.dialog_cancel))
                .onPositive((dialog, which) -> {
                    String newName = mDialogEditText.getText().toString().replaceAll("\"", "");
                    Collection col = getCol();
                    if (!Decks.isValidDeckName(newName)) {
                        Timber.i("renameDeckDialog not renaming deck to invalid name '%s'", newName);
                        UIUtils.showThemedToast(this, getString(R.string.invalid_deck_name), false);
                    } else if (!newName.equals(currentName)) {
                        try {
                            col.getDecks().rename(col.getDecks().get(did), newName);
                        } catch (DeckRenameException e) {
                            // We get a localized string from libanki to explain the error
                            UIUtils.showThemedToast(this, e.getLocalizedMessage(res), false);
                        }
                    }
                    dismissAllDialogFragments();
                    try {
                        refreshDeckListUI(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                })
                .onNegative((dialog, which) -> dismissAllDialogFragments())
                .build().show();
    }


    // Callback to show export dialog for currently selected deck
    public void showContextMenuExportDialog() {
        exportDeck(mContextMenuDid);
    }


    public void exportDeck(long did) {
        String msg;
        msg = getResources().getString(R.string.confirm_apkg_export_deck, getCol().getDecks().get(did).getString("name"));
        showDialogFragment(ExportDialog.newInstance(msg, did, this));
    }


    @Override
    public void exportApkg(String filename, Long did, boolean includeSched, boolean includeMedia) {
        File exportDir = new File(getExternalCacheDir(), "export");
        exportDir.mkdirs();
        File exportPath;
        String timeStampSuffix = "-" + TimeUtils.getTimestamp(getCol().getTime());
        if (filename != null) {
            // filename has been explicitly specified
            exportPath = new File(exportDir, filename);
        } else if (did != null) {
            // filename not explicitly specified, but a deck has been specified so use deck name
            exportPath = new File(exportDir, getCol().getDecks().get(did).getString("name").replaceAll("\\W+", "_") + timeStampSuffix + ".apkg");
        } else if (!includeSched) {
            // full export without scheduling is assumed to be shared with someone else -- use "All Decks.apkg"
            exportPath = new File(exportDir, "All Decks" + timeStampSuffix + ".apkg");
        } else {
            // full collection export -- use "collection.colpkg"
            File colPath = new File(getCol().getPath());
            String newFileName = colPath.getName().replace(".anki2", timeStampSuffix + ".colpkg");
            exportPath = new File(exportDir, newFileName);
        }
        // add input arguments to new generic structure
        Object[] inputArgs = new Object[5];
        inputArgs[0] = getCol();
        inputArgs[1] = exportPath.getPath();
        inputArgs[2] = did;
        inputArgs[3] = includeSched;
        inputArgs[4] = includeMedia;
        try {
            CollectionTask.launchCollectionTask(EXPORT_APKG, exportListener(), new TaskData(inputArgs));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void dismissAllDialogFragments() {
        getSupportFragmentManager().popBackStack("dialog", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }


    public void emailFile(String path) {
        // Make sure the file actually exists
        File attachment = new File(path);
        if (!attachment.exists()) {
            Timber.e("Specified apkg file %s does not exist", path);
            UIUtils.showThemedToast(this, getResources().getString(R.string.apk_share_error), false);
            return;
        }
        // Get a URI for the file to be shared via the FileProvider API
        Uri uri;
        try {
            uri = FileProvider.getUriForFile(this, "com.app.ankichinas.apkgfileprovider", attachment);
        } catch (IllegalArgumentException e) {
            Timber.e("Could not generate a valid URI for the apkg file");
            UIUtils.showThemedToast(this, getResources().getString(R.string.apk_share_error), false);
            return;
        }
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("application/apkg")
                .setStream(uri)
                .setSubject(getString(R.string.export_email_subject, attachment.getName()))
                .setHtmlText(getString(R.string.export_email_text))
                .getIntent();
        if (shareIntent.resolveActivity(getPackageManager()) != null) {
            startActivityWithoutAnimation(shareIntent);
        } else {
            // Try to save it?
            UIUtils.showSimpleSnackbar(this, R.string.export_send_no_handlers, false);
            saveExportFile(path);
        }
    }


    protected String mExportFileName;

    public static final int PICK_EXPORT_FILE = 1004;


    @TargetApi(19)
    public void saveExportFile(String path) {
        // Make sure the file actually exists
        File attachment = new File(path);
        if (!attachment.exists()) {
            Timber.e("saveExportFile() Specified apkg file %s does not exist", path);
            UIUtils.showSimpleSnackbar(this, R.string.export_save_apkg_unsuccessful, false);
            return;
        }
        if (CompatHelper.getSdkVersion() >= 19) {
            // Let the user choose where to export the file on API19+
            mExportFileName = path;
            Intent saveIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            saveIntent.addCategory(Intent.CATEGORY_OPENABLE);
            saveIntent.setType("application/apkg");
            saveIntent.putExtra(Intent.EXTRA_TITLE, attachment.getName());
            saveIntent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            saveIntent.putExtra("android.content.extra.FANCY", true);
            saveIntent.putExtra("android.content.extra.SHOW_FILESIZE", true);
            startActivityForResultWithoutAnimation(saveIntent, PICK_EXPORT_FILE);

        } else {
            // Otherwise just export to AnkiDroid directory
            File exportPath = new File(CollectionHelper.getCurrentAnkiDroidDirectory(this), new File(path).getName());
            try {
                CompatHelper.getCompat().copyFile(path, exportPath.getAbsolutePath());
                UIUtils.showThemedToast(this, getString(R.string.export_save_apkg_successful), false);
            } catch (IOException e) {
                UIUtils.showThemedToast(this, getString(R.string.export_save_apkg_unsuccessful), false);
            }
        }
    }


    private boolean exportToProvider(Intent intent, boolean deleteAfterExport) {
        if ((intent == null) || (intent.getData() == null)) {
            Timber.e("exportToProvider() provided with insufficient intent data %s", intent);
            return false;
        }
        Uri uri = intent.getData();
        Timber.d("Exporting from file to ContentProvider URI: %s/%s", mExportFileName, uri.toString());
        FileOutputStream fileOutputStream;
        ParcelFileDescriptor pfd;
        try {
            pfd = getContentResolver().openFileDescriptor(uri, "w");

            if (pfd != null) {
                fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                CompatHelper.getCompat().copyFile(mExportFileName, fileOutputStream);
                fileOutputStream.close();
                pfd.close();
            } else {
                Timber.w("exportToProvider() failed - ContentProvider returned null file descriptor for %s", uri);
                return false;
            }
            if (deleteAfterExport && !new File(mExportFileName).delete()) {
                Timber.w("Failed to delete temporary export file %s", mExportFileName);
            }
        } catch (Exception e) {
            Timber.e(e, "Unable to export file to Uri: %s/%s", mExportFileName, uri.toString());
            return false;
        }
        return true;
    }


    public void rebuildFiltered() {
        getCol().getDecks().select(mContextMenuDid);
        CollectionTask.launchCollectionTask(REBUILD_CRAM, simpleProgressListener());
    }


    public void emptyFiltered() {
        getCol().getDecks().select(mContextMenuDid);
        CollectionTask.launchCollectionTask(EMPTY_CRAM, simpleProgressListener());
    }


    /**
     * Show progress bars and rebuild deck list on completion
     */
    private final SimpleProgressListener simpleProgressListener() {
        return new SimpleProgressListener(this);
    }


    private static class SimpleProgressListener extends TaskListenerWithContext<AnkiActivity> {
        public SimpleProgressListener(AnkiActivity deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull AnkiActivity deckPicker) {
            deckPicker.showProgressBar();
        }


        @Override
        public void actualOnPostExecute(@NonNull AnkiActivity deckPicker, TaskData result) {
//            deckPicker.onRequireDeckListUpdate();
//            if (deckPicker.mDeckPickerFragment.mFragmented) {
//                deckPicker.mDeckPickerFragment.loadStudyOptionsFragment(false);
//            }
            try {
                deckPicker.refreshDeckListUI(false);

            } catch (Exception e) {
            }

        }
    }


    public void createSubdeckDialog() {
        createSubDeckDialog(mContextMenuDid);
    }


    private void createSubDeckDialog(long did) {
        final Resources res = getResources();
        mDialogEditText = new EditText(this);
        mDialogEditText.setSingleLine();
        mDialogEditText.setSelection(mDialogEditText.getText().length());
        new MaterialDialog.Builder(this)
                .title(R.string.create_subdeck)
                .customView(mDialogEditText, true)
                .positiveText(R.string.dialog_ok)
                .negativeText(res.getString(R.string.dialog_cancel))
                .onPositive((dialog, which) -> {
                    String textValue = mDialogEditText.getText().toString();
                    String newName = getCol().getDecks().getSubdeckName(did, textValue);
                    if (Decks.isValidDeckName(newName)) {
                        createSubNewDeck(newName);
                    } else {
                        Timber.i("createSubDeckDialog - not creating invalid subdeck name '%s'", newName);
                        UIUtils.showThemedToast(this, getString(R.string.invalid_deck_name), false);
                    }
                    dismissAllDialogFragments();
                    try {
                        refreshDeckListUI(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                })
                .onNegative((dialog, which) -> dismissAllDialogFragments())
                .build().show();
    }


    public void createNewDeck(String deckName) {
        Timber.i("AnkiActivity:: Creating new deck...");
        getCol().getDecks().id(deckName, true);
        try {
            refreshDeckListUI(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void createSubNewDeck(String deckName) {
        Timber.i("AnkiActivity:: Creating new sub deck...");
        try {
            long newID = getCol().getDecks().id(deckName, true);
            long parentConfID = getCol().getDecks().get(mContextMenuDid).getLong("conf");
            getCol().getDecks().get(newID).put("conf", parentConfID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            refreshDeckListUI(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onCreateCustomStudySession() {

        try {
            refreshDeckListUI(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onExtendStudyLimits() {

        try {
            refreshDeckListUI(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if ((requestCode == PICK_EXPORT_FILE) && (resultCode == RESULT_OK)) {
            if (exportToProvider(intent, true)) {
                UIUtils.showSimpleSnackbar(this, getString(R.string.export_save_apkg_successful), true);
            } else {
                UIUtils.showSimpleSnackbar(this, getString(R.string.export_save_apkg_unsuccessful), false);
            }
        } else if ((requestCode == PICK_APKG_FILE) && (resultCode == RESULT_OK)) {
            ImportUtils.ImportResult importResult = ImportUtils.handleFileImport(this, intent);
            if (!importResult.isSuccess()) {
                ImportUtils.showImportUnsuccessfulDialog(this, importResult.getHumanReadableMessage(), false);
            }
        }
    }


    @Override
    public void importAdd(String importPath) {
        Timber.d("importAdd() for file %s", importPath);
        try {
            CollectionTask.launchCollectionTask(IMPORT, importAddListener(),
                    new TaskData(importPath));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    protected TaskListener importAddListener() throws Exception {
        throw new Exception("not implemented import listener");
    }


    protected TaskListener importReplaceListener() throws Exception {
        throw new Exception("not implemented import replace listener");
    }


    @Override
    public void importReplace(String importPath) {
        try {
            CollectionTask.launchCollectionTask(IMPORT_REPLACE, importReplaceListener(), new TaskData(importPath));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void showImportDialog(int id) {
        showImportDialog(id, "");
    }


    public static final int PICK_APKG_FILE = 13;


    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void showImportDialog(int id, String message) {
        // On API19+ we only use import dialog to confirm, otherwise we use it the whole time
        if ((CompatHelper.getSdkVersion() < 19)
                || (id == ImportDialog.DIALOG_IMPORT_ADD_CONFIRM)
                || (id == ImportDialog.DIALOG_IMPORT_REPLACE_CONFIRM)) {
            Timber.d("showImportDialog() delegating to ImportDialog");
            AsyncDialogFragment newFragment = ImportDialog.newInstance(id, message, this);
            showAsyncDialogFragment(newFragment);
        } else {
            Timber.d("showImportDialog() delegating to file picker intent");
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
            intent.putExtra("android.content.extra.FANCY", true);
            intent.putExtra("android.content.extra.SHOW_FILESIZE", true);
            startActivityForResultWithoutAnimation(intent, PICK_APKG_FILE);
        }
    }


}

