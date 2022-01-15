package com.ichi2.anki;


import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.GravityEnum;
import com.afollestad.materialdialogs.MaterialDialog;

import com.google.android.material.bottomnavigation.BottomNavigationMenuView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarItemView;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.analytics.UsageAnalytics;
import com.ichi2.anki.dialogs.AsyncDialogFragment;
import com.ichi2.anki.dialogs.ConfirmationDialog;
import com.ichi2.anki.dialogs.DatabaseErrorDialog;
import com.ichi2.anki.dialogs.DeckPickerAnalyticsOptInDialog;
import com.ichi2.anki.dialogs.DeckPickerBackupNoSpaceLeftDialog;
import com.ichi2.anki.dialogs.DeckPickerExportCompleteDialog;
import com.ichi2.anki.dialogs.DeckPickerNoSpaceLeftDialog;
import com.ichi2.anki.dialogs.DialogHandler;
import com.ichi2.anki.dialogs.ExportDialog;
import com.ichi2.anki.dialogs.MediaCheckDialog;
import com.ichi2.anki.dialogs.SyncErrorDialog;
import com.ichi2.anki.exception.ConfirmModSchemaException;
import com.ichi2.anki.receiver.SdCardReceiver;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.Connection;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.DeviceID;
import com.ichi2.libanki.Model;
import com.ichi2.libanki.Models;
import com.ichi2.libanki.importer.AnkiPackageImporter;
import com.ichi2.libanki.sync.AnkiChinaSyncer;
import com.ichi2.libanki.sync.CustomSyncServerUrlException;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.ui.CustomStyleDialog;
import com.ichi2.utils.JSONException;
import com.ichi2.utils.OKHttpUtil;
import com.ichi2.utils.Permissions;
import com.ichi2.utils.SyncStatus;
import com.ichi2.utils.VersionUtils;
//import com.ichi2.widget.NoScrollViewPager;
import com.ichi2.widget.WidgetStatus;
import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;
import com.umeng.analytics.MobclickAgent;
import com.umeng.commonsdk.UMConfigure;
import com.umeng.socialize.PlatformConfig;


import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
//import androidx.viewpager.widget.ViewPager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import okhttp3.Call;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.anki.MyAccount.NOT_LOGIN_ANKI_CHINA;
import static com.ichi2.anki.SelfStudyActivity.ALL_DECKS_ID;
import static com.ichi2.async.CollectionTask.TASK_TYPE.CHECK_DATABASE;
import static com.ichi2.async.CollectionTask.TASK_TYPE.CHECK_MEDIA;
import static com.ichi2.async.CollectionTask.TASK_TYPE.FIND_EMPTY_CARDS;
import static com.ichi2.async.CollectionTask.TASK_TYPE.LOAD_COLLECTION_COMPLETE;
import static com.ichi2.async.CollectionTask.TASK_TYPE.LOAD_DECK_COUNTS;
import static com.ichi2.async.CollectionTask.TASK_TYPE.REPAIR_COLLECTION;

import static com.ichi2.libanki.Consts.URL_PRIVATE;
import static com.ichi2.libanki.Consts.URL_USER_PROTOCOL;


public class DeckPicker extends AnkiActivity implements BottomNavigationView.OnItemSelectedListener ,
        StudyOptionsFragment.StudyOptionsListener, SyncErrorDialog.SyncErrorDialogListener, MediaCheckDialog.MediaCheckDialogListener,

        ActivityCompat.OnRequestPermissionsResultCallback {
    ViewPager2 viewPager;
    BottomNavigationView bottomNavigationView;
    private MenuItem menuItem;
    public static final int REQUEST_BROWSE_CARDS = 101;
    private DeckPickerFragment mDeckPickerFragment;
    private Statistics mStatisticsFragment;
    private SettingFragment mSettingFragment;
    public static final int RESULT_MEDIA_EJECTED = 202;
    public static final int RESULT_DB_ERROR = 203;
    public static final int RESULT_UPDATE_REST_SPACE = 204;
//    private final AnkiFragment[] mFragments = new AnkiFragment[] {mDeckPickerFragment, mStatisticsFragment, mSettingFragment};

    public static final int INDEX_DECK_PICKER = 0;
    public static final int INDEX_STATISTICS = 1;
    public static final int INDEX_SETTING = 2;
    public static final int REPORT_FEEDBACK = 4;
    public static final int LOG_IN_FOR_SYNC = 6;
    public static final int SHOW_INFO_WELCOME = 8;
    public static final int SHOW_INFO_NEW_VERSION = 9;
    public static final int REPORT_ERROR = 10;
    public static final int SHOW_STUDYOPTIONS = 11;
    public static final int ADD_NOTE = 12;
    public static final int BE_VIP = 997;
    public static final int REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML = 998;
    public static final int REFRESH_VOICE_INFO = 999;

    public static final int CHANGE_ACCOUNT = 14;


    public static final int REQUEST_PATH_UPDATE = 1;

//    public DeckPickerFragment getFragmentMethod() {
//        return mDeckPickerFragment;
//    }


    private View mBottomAddMenuIcon;
    private MaterialDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.navigation_activity);
        AnkiChinaSyncer.SYNCING = false;

//        Toolbar toolbar = findViewById(R.id.toolbar);
//        if (toolbar != null) {
//            setSupportActionBar(toolbar);
//        }
        viewPager = findViewById(R.id.viewpager);
        viewPager.setUserInputEnabled(false);
//        viewPager.setScrolledListener(new CustomScrollViewPager.ScrolledListener() {
//            @Override
//            public void onScroll() {
//                openCardBrowser();
//            }
//        });
        bottomNavigationView = findViewById(R.id.navigation);
        bottomNavigationView.setItemIconTintList(null);
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) bottomNavigationView.getChildAt(0);
        mBottomAddMenuIcon = menuView.getChildAt(2).findViewById(com.google.android.material.R.id.navigation_bar_item_icon_view);
//        menuView.getItemIconSize()
//        mBottomAddMenuIcon =( (NavigationBarItemView)menuView.getChildAt(2)).setIcon();

        final ViewGroup.LayoutParams layoutParams = mBottomAddMenuIcon.getLayoutParams();
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        layoutParams.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 37, displayMetrics);
        layoutParams.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 37, displayMetrics);
        mBottomAddMenuIcon.setLayoutParams(layoutParams);

        restoreWelcomeMessage(savedInstanceState);
        registerExternalStorageListener();
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        if (!preferences.contains(CONFIRM_PRIVATE_STRATEGY) && !mShowingPrivateStrategyDialog) {
            mShowingPrivateStrategyDialog = true;
            new MaterialDialog.Builder(this)
                    .title(R.string.collection_load_welcome_request_permissions_title)
                    .titleGravity(GravityEnum.CENTER)
                    .content(getClickableSpan(this))
                    .negativeText(R.string.dialog_disagree)
                    .positiveText(R.string.dialog_agree)
                    .onPositive((innerDialog, innerWhich) -> {
                        preferences.edit().putBoolean(CONFIRM_PRIVATE_STRATEGY, true).apply();
                        mShowingPrivateStrategyDialog = false;
                        continueActivity(preferences);
                    })
                    .positiveColor(ContextCompat.getColor(this, R.color.new_primary_color))
                    .onNegative((innerDialog, innerWhich) -> {
                        mShowingPrivateStrategyDialog = false;
                        finishWithoutAnimation();
                    })
                    .negativeColor(ContextCompat.getColor(this, R.color.new_primary_text_secondary_color))
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .show();
        } else {
            continueActivity(preferences);
        }
        int count = preferences.getInt(START_APP_COUNT, 0);
        count++;
        if (count == 5) {
            CustomStyleDialog customDialog = new CustomStyleDialog.Builder(this)
                    .setTitle("给个好评，鼓励一下吧！")
                    .setMessage("每一份好评对我们都是极大的鼓励，也是我们持续优化的动力")
                    .setPositiveButton("好评鼓励", (dialog, which) -> {
                        dialog.dismiss();
                        goAppShop(DeckPicker.this, BuildConfig.APPLICATION_ID, "");
                    })
                    .setNegativeButton("残忍拒绝", (dialog, which) -> dialog.dismiss())
                    .create();
            customDialog.show();
        }
        preferences.edit().putInt(START_APP_COUNT, count).apply();

    }


    public static void goAppShop(Context context, String myAppPkg, String shopPkg) {
        if (TextUtils.isEmpty(myAppPkg)) {
            return;
        }

        try {
            Uri uri = Uri.parse("market://details?id=" + myAppPkg);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (!TextUtils.isEmpty(shopPkg)) {
                intent.setPackage(shopPkg);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 如果没有该应用商店，则显示系统弹出的应用商店列表供用户选择
            goAppShop(context, myAppPkg, "");
        }
    }


    /**
     * Show a message when the SD card is ejected
     */
    private BroadcastReceiver mUnmountReceiver = null;


    private void registerExternalStorageListener() {
        if (mUnmountReceiver == null) {
            mUnmountReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction().equals(SdCardReceiver.MEDIA_EJECT)) {
                        onSdCardNotMounted();
                    } else if (intent.getAction().equals(SdCardReceiver.MEDIA_MOUNT)) {
                        restartActivity();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(SdCardReceiver.MEDIA_EJECT);
            iFilter.addAction(SdCardReceiver.MEDIA_MOUNT);
            registerReceiver(mUnmountReceiver, iFilter);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mUnmountReceiver != null) {
            unregisterReceiver(mUnmountReceiver);
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
//        if(mRoot!=null){
//            ((ViewGroup)mRoot.getParent()).removeView(mRoot);
//        }
        Timber.d("onDestroy()");
    }


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    private void continueActivity(SharedPreferences preferences) {
        boolean colOpen = firstCollectionOpen(false);
        Timber.i("colOpen: %b", colOpen);
        if (colOpen) {
            startLoadingCollection();
            // Show any necessary dialogs (e.g. changelog, special messages, etc)
            showStartupScreensAndDialogs(preferences, 0);

        } else {
            // Show error dialogs
            if (Permissions.hasStorageAccessPermission(this)) {

                if (!AnkiDroidApp.isSdCardMounted()) {
                    Timber.i("SD card not mounted");
                    onSdCardNotMounted();
                } else if (!CollectionHelper.isCurrentAnkiDroidDirAccessible(this)) {
                    Timber.i("AnkiDroid directory inaccessible");
                    Intent i = Preferences.getPreferenceSubscreenIntent(this, "com.ichi2.anki.prefs.advanced");
                    this.startActivityForResultWithoutAnimation(i, REQUEST_PATH_UPDATE);
                    Toast.makeText(this, R.string.directory_inaccessible, Toast.LENGTH_LONG).show();
                } else {
                    Timber.i("Displaying database error");
                    this.showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_LOAD_FAILED);
                }
                Connection.sendCommonGet(fetchCustomDialogListener, new Connection.Payload("common/dialog", "?unionid=" + DeviceID.getDeviceId(this), Connection.Payload.REST_TYPE_GET, "", "nothing", HostNumFactory.getInstance(this)));

            }
        }
        mVipExpireAt = AnkiDroidApp.getSharedPrefs(this).getString(Consts.KEY_VIP_EXPIRED, "");
        mVip = AnkiDroidApp.getSharedPrefs(this).getBoolean(Consts.KEY_IS_VIP, false);
        if (mVipExpireAt.isEmpty()) {
            mVip = false;
        } else {
            long now = getCol(this).getTime().intTimeMS();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            try {
                Date date = sdf.parse(mVipExpireAt);
                Calendar calendar = getCol(this).getTime().calendar();
                calendar.setTime(date);
                if (calendar.getTimeInMillis() < now) {
                    mVip = false;
                }
            } catch (ParseException e) {
                e.printStackTrace();
                mVip = false;
            }
        }
        AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(Consts.KEY_IS_VIP, mVip).apply();

        Bugly.init(getApplicationContext(), "8793e55d11", false);
        UMConfigure.setLogEnabled(BuildConfig.DEBUG);
        UMConfigure.init(this, "5f71f96680455950e49ab67c", "channel1", UMConfigure.DEVICE_TYPE_PHONE, "ankichina");

        String FileProvider = BuildConfig.APPLICATION_ID+".apkgfileprovider";
        PlatformConfig.setWeixin("wx577ed7c2bdb5d5ee", "4c027fb240e2cc6df88d549883f14c4f");
        PlatformConfig.setWXFileProvider(FileProvider);
        PlatformConfig.setQQFileProvider(FileProvider);
        PlatformConfig.setQQZone("1111286732","eNH62Yz3e56MJVFW");
//        PlatformConfig.setSinaWeibo("3921700954", "04b48b094faeb16683c32669824ebdad", "http://sns.whalecloud.com");

        MobclickAgent.setPageCollectionMode(MobclickAgent.PageMode.AUTO);
        Beta.checkUpgrade(false, true);
    }


//    private void checkRestServerSpace(String token,String resultFor){
//        OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "clouds/current", token,resultFor, checkRestServerSpaceListener);
//    }

    private final OKHttpUtil.MyCallBack checkRestServerSpaceListener = new OKHttpUtil.MyCallBack() {
        @Override
        public void onFailure(Call call, IOException e) {

        }


        @Override
        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
//            Timber.i("http get result:%s,body:%s", response.toString() ,response.body()==null?"":response.body().string());
            if (response.isSuccessful()) {
//                            Timber.i("fetch server space successfully:%s", response.body().string());
                try {
                    JSONObject result = (new JSONObject(response.body().string())).getJSONObject("data");
                    Timber.i("fetch server space result:%s ", result.toString());
                    long total = result.getLong("origin_size");
                    long used = result.getLong("origin_used_size");
                    String totalStr = result.getString("size");
                    String usedStr = result.getString("used_size");
                    String hint = String.format("%s/%s", usedStr, totalStr);
                    String hintStr = String.format(getString(R.string.upgrade_cloud_space), hint);
                    long rest = total - used;
                    Timber.i("fetch server space result:%d,%d,%d", total, used, rest);
//                    getNavigationView().getMenu().findItem(R.id.nav_cloud_space).setTitle(hintStr);
                    saveServerRestSpace(rest);
                    String resultFor = (String) (((Object[]) arg1)[0]);
                    boolean byUser = (boolean) (((Object[]) arg1)[1]);
                    if ("nothing".equals(resultFor)) {
                        return;
                    }
                    if (rest <= 0) {
                        if (!byUser) {
                            showNoSpaceDialog();
                        } else {
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_NO_ENOUGH_SERVER_SPACE);
                        }
                        return;
                    }
                    //获取剩余空间
                    runOnUiThread(() -> syncInternal(token, resultFor));

                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                    UIUtils.showSimpleSnackbar(DeckPicker.this, R.string.sync_generic_error, true);
                }
            } else {
                Timber.e("fetch server space failed, error code %d", response.code());
                String hintStr = String.format(getString(R.string.upgrade_cloud_space), "请登录/刷新");
                if ("nothing".equals(String.valueOf(arg1))) {
                    return;
                }
                UIUtils.showSimpleSnackbar(DeckPicker.this, R.string.network_error, true);
            }
        }
    };


    /**
     * Flag to indicate whether the activity will perform a sync in its onResume.
     * Since syncing closes the database, this flag allows us to avoid doing any
     * work in onResume that might use the database and go straight to syncing.
     */

    protected boolean mSyncOnResume = false;
    protected boolean mRefreshVipStateOnResume = true;


    private boolean mVip = false;
    private int mVipDay;
    private String mVipExpireAt;


    //VIP页面地址在登录和非登录态是不一样的，如有APP登录或者登出操作请重新调此接口更新URL
    private void refreshVipState(boolean isConnected) {
        runOnUiThread(() -> {
            mVip = mVip && isConnected;//联网失败默认无vip
            for (int i = 0; i < mFragments.size(); i++) {
                mFragments.get(i).onRefreshVipState(mVip, mVipUrl, mVipDay, mVipExpireAt);
            }
        });
    }


    //设置字体颜色,参数如getResources().getColor(R.color.colorBlue)
    public static SpannableStringBuilder ForeGroundColorSpan(String content, int start, int end, int colorId) {
        if (end > content.length()) {
            end = content.length();
        }
        SpannableStringBuilder ssb = new SpannableStringBuilder(content);
        ssb.setSpan(new ForegroundColorSpan(colorId), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }


    public void getVipInfo() {
        mRefreshVipStateOnResume = false;
        if (!Permissions.hasStorageAccessPermission(this)) {
            return;
        }
        mVip = AnkiDroidApp.getSharedPrefs(DeckPicker.this).getBoolean(Consts.KEY_IS_VIP, false);
        mVipUrl = AnkiDroidApp.getSharedPrefs(DeckPicker.this).getString(Consts.KEY_VIP_URL, "");
        refreshVipState(true);
        getAccount().getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                //获取vip状态
                OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/vipInfo", token, "", new OKHttpUtil.MyCallBack() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        refreshVipState(false);
                        if (mOpenVipHtmlWhenGetUrl) {
                            mOpenVipHtmlWhenGetUrl = false;
                            runOnUiThread(() -> Toast.makeText(DeckPicker.this, "信息获取失败，请检查网络或稍候再试", Toast.LENGTH_SHORT).show());
                        }
                    }


                    @Override
                    public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                        if (response.isSuccessful()) {
//                            Timber.i("init vip info successfully!:%s", response.body());
                            try {
                                final JSONObject object = new JSONObject(response.body().string());
                                final JSONObject item = object.getJSONObject("data");
                                mVipUrl = item.getString("vip_url");
                                Timber.i("get vip url ：%s", mVipUrl);
                                if (!mVip && mTurnToVipHtml) {
                                    mTurnToVipHtml = false;
                                    WebViewActivity.openUrlInApp(DeckPicker.this, String.format(mVipUrl, token, BuildConfig.VERSION_NAME), token, BE_VIP);
                                }
                                mVip = item.getBoolean("is_vip");
                                mVipDay = item.getInt("vip_day");
                                mVipExpireAt = item.getString("vip_end_at");
                                AnkiDroidApp.getSharedPrefs(DeckPicker.this).edit()
                                        .putBoolean(Consts.KEY_IS_VIP, mVip)
                                        .putString(Consts.KEY_VIP_URL, mVipUrl)
                                        .putString(Consts.KEY_VIP_EXPIRED, mVipExpireAt).apply();
                                if (mOpenVipHtmlWhenGetUrl) {
                                    mOpenVipHtmlWhenGetUrl = false;
                                    openVipUrl(mVipUrl);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Timber.e("init vip info failed, error code %d", response.code());
                        }
                        refreshVipState(true);
                    }
                });
            }


            @Override
            public void onFail(String message) {
                OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/vipInfo", "", "", new OKHttpUtil.MyCallBack() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        refreshVipState(false);
                    }


                    @Override
                    public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            Timber.i("init vip info successfully!:%s", response.body());
                            try {
                                final JSONObject object = new JSONObject(response.body().string());
                                final JSONObject item = object.getJSONObject("data");
                                mVipUrl = item.getString("vip_url");
                                mVip = item.getBoolean("is_vip");
                                mVipDay = item.getInt("vip_day");
                                mVipExpireAt = item.getString("vip_end_at");
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Timber.e("init vip info failed, error code %d", response.code());

                        }
                        refreshVipState(false);
                    }
                });


            }
        });
    }


    boolean mOpenVipHtmlWhenGetUrl = false;


    @Override
    public void openVipUrl(String url) {
        getAccount().getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                if (url != null && !url.isEmpty()) {
                    WebViewActivity.openUrlInApp(DeckPicker.this, String.format(url, token, BuildConfig.VERSION_NAME), token, BE_VIP);
                } else {
                    new MaterialDialog.Builder(DeckPicker.this)
                            .title("获取账号信息失败")
                            .content("是否重试?")
                            .positiveText(R.string.dialog_ok)
                            .onPositive((dialog, which) -> {
                                mOpenVipHtmlWhenGetUrl = true;
                                getVipInfo();
                            })
                            .negativeText(R.string.dialog_cancel)
                            .show();
                }
            }


            @Override
            public void onFail(String message) {
                Toast.makeText(DeckPicker.this, "当前未使用Anki记忆卡账号登录，无法获得超级学霸功能", Toast.LENGTH_SHORT).show();
                Intent myAccount = new Intent(DeckPicker.this, MyAccount.class);
                myAccount.putExtra("notLoggedIn", true);
                startActivityForResultWithAnimation(myAccount, REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML, ActivityTransitionAnimation.FADE);
                handleGetTokenFailed(message);
            }
        });
    }


    @Override
    protected void onResume() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        Consts.LOGIN_SERVER = preferences.getInt(Consts.KEY_ANKI_ACCOUNT_SERVER, 0);
        super.onResume();
        //以下三种情况需要刷新vip状态
        //1、从查看权益页面出来
        //2、登录页出来
        //3、刚打开app
        Timber.i("refresh vip state on resume:%s", mRefreshVipStateOnResume);

        if (mRefreshVipStateOnResume) {
            getVipInfo();
        }
        Timber.i("on resume and state check,mSyncOnResume:" + mSyncOnResume + "," + colIsOpen() + "," + viewPager + "," + viewPager.getCurrentItem() + "," + mFragments.size());
        if (!Permissions.hasStorageAccessPermission(this)) {
            return;
        }
        if (mSyncOnResume) {
            Timber.i("Performing Sync on Resume");
            sync();
            mSyncOnResume = false;
        } else if (colIsOpen()) {
//            selectNavigationItem(R.id.nav_decks);
            if (viewPager != null && viewPager.getCurrentItem() == 0 && mFragments.size() > 0 && mFragments.get(0).getAnkiActivity() != null) {
                if (((DeckPickerFragment) mFragments.get(0)).mDueTree == null) {
                    Timber.i("Performing updateDeckList on Resume quick");
                    ((DeckPickerFragment) mFragments.get(0)).updateDeckList(true);
                }

            }

        }
        /** Complete task and enqueue fetching nonessential data for
         * startup. */
        CollectionTask.launchCollectionTask(LOAD_COLLECTION_COMPLETE);

        mActivityPaused = false;
        //获取全局配置
        if (!pulledConfigFromService) {
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "configs/global", token, false, getServiceConfigCallback);
                }


                @Override
                public void onFail(String message) {

                }
            });

        }

        if (!pulledKeyFromService) {
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/common/getServerKeyByApp", token, false, getServiceKeyCallback);
                }


                @Override
                public void onFail(String message) {

                }
            });

        }
    }


    public void onSdCardNotMounted() {
        UIUtils.showThemedToast(this, getResources().getString(R.string.sd_card_not_mounted), false);
        finishWithoutAnimation();
    }


    private Dialog addMenuBottomDialog;
    private Dialog mCloudCustomDialog;


    private void showCloudCustomDialog(String title, String content) {
        //1、使用Dialog、设置style
//        if (mCloudCustomDialog == null) {
        mCloudCustomDialog = new Dialog(this);
        //2、设置布局
        View view = View.inflate(this, R.layout.start_up_cloud_dialog, null);
        mCloudCustomDialog.setContentView(view);
        Window window = mCloudCustomDialog.getWindow();
        mCloudCustomDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        //设置弹出位置
        window.setGravity(Gravity.CENTER);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        WindowManager.LayoutParams lps = window.getAttributes();
//        lps.horizontalMargin = 0.1f;

//        lps.gravity = Gravity.CENTER;
//        window.setAttributes(lps);
        //设置弹出动画
//        window.setWindowAnimations(R.style.main_menu_animStyle);
        //设置对话框大小
        final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        window.setLayout((int) (displayMetrics.widthPixels * 0.9), ViewGroup.LayoutParams.WRAP_CONTENT);
        mCloudCustomDialog.findViewById(R.id.content_layout).setPadding((int) (displayMetrics.widthPixels * 0.095), 0, (int) (displayMetrics.widthPixels * 0.095), 0);
        ImageView logo = mCloudCustomDialog.findViewById(R.id.logo);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) logo.getLayoutParams();
        params.leftMargin = (int) (displayMetrics.widthPixels * 0.145);
        logo.setLayoutParams(params);
        mCloudCustomDialog.findViewById(R.id.confirm).setOnClickListener(view1 -> mCloudCustomDialog.dismiss());
        ((TextView) mCloudCustomDialog.findViewById(R.id.title)).setText(title);
        ((TextView) mCloudCustomDialog.findViewById(R.id.content)).setText(content);
//        }

        mCloudCustomDialog.show();
    }


    Connection.TaskListener fetchCustomDialogListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {
            Timber.d("fetchCustomDialogListener.onPreExecute()");
        }


        @Override
        public void onPostExecute(Connection.Payload data) {
            if (data.success) {
                Timber.i("fetch server dialog:%s", data.result.toString());
                try {
                    JSONObject result = ((JSONObject) data.result).getJSONObject("data");
                    boolean show = result.getBoolean("show");
                    String title = result.getString("title");
//                    String title = "欢迎来到ANKI记忆卡！";
                    String content = result.getString("content");
//                    String content = "ANKI记忆卡 需要存储权限，我们只用来存储你的ANKI记忆卡 集合、记忆卡片媒体和备份。我们的代码是开源的，由探索者撰写，并且受到数百万人的信任。\n\n如果有任何疑问，请访问我们的应用内手册或访问我们的支持论坛。\n\n感谢你尝试ANKI记忆卡\n— ANKI探索者开发团队";
                    if (show) {
                        showCloudCustomDialog(title, content);
                    }
                    Timber.i("fetch server dialog:%s,%s,%s", show, title, content);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Timber.e("fetch server dialog failed, error code %d", data.statusCode);
            }
        }


        @Override
        public void onDisconnected() {
        }
    };
    private EditText mDialogEditText;


    protected void refreshDeckListUI(boolean onlyRefresh) {
        if (onlyRefresh) {
            mDeckPickerFragment.notifyDataSetChanged();
        } else {
            onRequireDeckListUpdate();
        }
    }


    private void showBottomDialog(MenuItem item) {
        if (!Permissions.hasStorageAccessPermission(this)) {
            firstCollectionOpen();
            return;
        }
        //1、使用Dialog、设置style
        if (addMenuBottomDialog == null) {
            addMenuBottomDialog = new Dialog(this, R.style.DialogTheme);
            //2、设置布局
            View view = View.inflate(this, R.layout.dialog_bottom, null);
            addMenuBottomDialog.setContentView(view);
            addMenuBottomDialog.setOnDismissListener(dialog1 -> item.setIcon(R.mipmap.tab_bar_add_selected));
            Window window = addMenuBottomDialog.getWindow();
//            addMenuBottomDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            //设置弹出位置
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams lps = window.getAttributes();
            lps.verticalMargin = 0.1f;
            window.setAttributes(lps);
            //设置弹出动画
//        window.setWindowAnimations(R.style.main_menu_animStyle);
            //设置对话框大小
            final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 69, displayMetrics));
            addMenuBottomDialog.findViewById(R.id.ll_add_card).setOnClickListener(view1 -> {
                addMenuBottomDialog.dismiss();
                Timber.i("Adding Note");
                addNote();

            });
            addMenuBottomDialog.findViewById(R.id.ll_add_deck).setOnClickListener(view1 -> {
                addMenuBottomDialog.dismiss();
                mDialogEditText = new EditText(this);
                mDialogEditText.setSingleLine(true);
                // mDialogEditText.setFilters(new InputFilter[] { mDeckNameFilter });
                new MaterialDialog.Builder(this)
                        .title(R.string.new_deck)
                        .positiveText(R.string.dialog_ok)
                        .customView(mDialogEditText, true)
                        .onPositive((dialog, which) -> {
                            String deckName = mDialogEditText.getText().toString();
                            if (Decks.isValidDeckName(deckName)) {
                                createNewDeck(deckName);
                            } else {
                                Timber.i("configureFloatingActionsMenu::addDeckButton::onPositiveListener - Not creating invalid deck name '%s'", deckName);
                                UIUtils.showThemedToast(this, getString(R.string.invalid_deck_name), false);
                            }
                        })
                        .negativeText(R.string.dialog_cancel)
                        .show();

            });
        }
        if (addMenuBottomDialog.isShowing()) {
            addMenuBottomDialog.dismiss();
            return;
        }
        item.setIcon(R.mipmap.tab_bar_add_normal);
        addMenuBottomDialog.show();
    }


    private void addNote() {
        Intent intent = new Intent(this, NoteEditor.class);
        intent.putExtra(NoteEditor.EXTRA_CALLER, NoteEditor.CALLER_DECKPICKER);
        startActivityForResultWithAnimation(intent, ADD_NOTE, ActivityTransitionAnimation.LEFT);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu in activity:" + menu);

        return super.onCreateOptionsMenu(menu);
    }


//    List<AnkiFragment> mNeedCollectionResultList;


//    public void startLoadingCollection(int index) {
//        if (mNeedCollectionResultList == null) {
//            mNeedCollectionResultList = new ArrayList<>();
//        }
//        mNeedCollectionResultList.add(mFragments[index]);
//        startLoadingCollection();
//    }

    List<AnkiFragment> mFragments = new ArrayList<>();


    @Override
    public void onCollectionLoaded(Collection col) {

        Timber.i("onCollectionLoaded:%s", mFragments.isEmpty());
        if (mFragments.isEmpty()) {
            mDeckPickerFragment = new DeckPickerFragment();
            mFragments.add(mDeckPickerFragment);
            mStatisticsFragment = new Statistics();
            mFragments.add(mStatisticsFragment);
            mSettingFragment = new SettingFragment();
            mFragments.add(mSettingFragment);
        }else {
            return;
        }


        viewPager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), mFragments));
//        viewPager.addOnPageChangeListener(this);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }


            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position >= 1) {
                    position += 2;
                }
                menuItem = bottomNavigationView.getMenu().getItem(position);
                menuItem.setChecked(true);
                invalidateOptionsMenu();
            }


            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
            }
        });
        bottomNavigationView.setOnItemSelectedListener(this);
        bottomNavigationView.setSelectedItemId(R.id.tab_one);


    }


    public void openSourceMarket() {
        getAccount().getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                WebViewActivity.openUrlInApp(DeckPicker.this, String.format(getResources().getString(R.string.shared_decks_url), token, BuildConfig.VERSION_NAME), token);
//                openUrl(Uri.parse(getResources().getString(R.string.shared_decks_url, token)));
            }


            @Override
            public void onFail(String message) {
                WebViewActivity.openUrlInApp(DeckPicker.this, String.format(getResources().getString(R.string.shared_decks_url), "", BuildConfig.VERSION_NAME), "");
            }
        });

    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.tab_one:
                viewPager.setCurrentItem(0);
                break;
            case R.id.tab_two:
                openSourceMarket();
                break;
            case R.id.tab_three:
                showBottomDialog(item);
                break;
            case R.id.tab_four:
                viewPager.setCurrentItem(1);
                break;
            case R.id.tab_five:
                viewPager.setCurrentItem(3);
                break;
        }
        return false;
    }


    public void restoreFromBackup(String path) {
        importReplace(path);
    }


//    @Override
//    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//    }
//
//
//    @Override
//    public void onPageSelected(int position) {
//        if (position >= 1) {
//            position += 2;
//        }
//        menuItem = bottomNavigationView.getMenu().getItem(position);
//        menuItem.setChecked(true);
//        invalidateOptionsMenu();
//    }
//
//
//    @Override
//    public void onPageScrollStateChanged(int state) {
//
//    }


    class ViewPagerAdapter extends FragmentStateAdapter {
        List<AnkiFragment> fragments;


        public ViewPagerAdapter(FragmentManager fm, List<AnkiFragment> fragments) {
//            super();
            super(fm,DeckPicker.this.getLifecycle());
            this.fragments = fragments;
        }



        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return fragments.get(position);
        }


        @Override
        public int getItemCount() {
            return fragments.size();
        }
    }



    /**
     * If we have accepted the "We will show you permissions" dialog, don't show it again on activity rebirth
     */
    private boolean mClosedWelcomeMessage;
    private boolean mShowingPrivateStrategyDialog;
    private boolean mShowingStorageRequestDialog;
    protected static final int REQUEST_STORAGE_PERMISSION = 0;
    protected static final String CONFIRM_PRIVATE_STRATEGY = "CONFIRM_PRIVATE_STRATEGY";
    protected static final String HAD_DENIED_STORAGE_ACCESS = "HAD_DENIED_STORAGE_ACCESS";
    protected static final String AUTO_TURN_TO_LOGIN = "AUTO_TURN_TO_LOGIN";
    protected static final String START_APP_COUNT = "START_APP_COUNT";

    public synchronized boolean firstCollectionOpen(){
        return firstCollectionOpen(true);
    }
    public synchronized boolean firstCollectionOpen(boolean byUser) {
        if (mShowingPrivateStrategyDialog || mShowingStorageRequestDialog) {
            return false;
        }
        if (Permissions.hasStorageAccessPermission(this)) {
            Timber.i("User has permissions to access collection");
            // Show error dialog if collection could not be opened

            return CollectionHelper.getInstance().getColSafe(this) != null;
        } else if (mClosedWelcomeMessage) {
            // DEFECT #5847: this fails if the activity is killed.
            //Even if the dialog is showing, we want to show it again.
            ActivityCompat.requestPermissions(this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_STORAGE_PERMISSION);
            return false;
        } else if (!mShowingStorageRequestDialog ) {
            if(!byUser&&AnkiDroidApp.getSharedPrefs(this).getBoolean(HAD_DENIED_STORAGE_ACCESS,false)){
                AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(HAD_DENIED_STORAGE_ACCESS,true).apply();
                Connection.sendCommonGet(fetchCustomDialogListener, new Connection.Payload("common/dialog", "?unionid=" + DeviceID.getDeviceId(this), Connection.Payload.REST_TYPE_GET, "", "nothing", HostNumFactory.getInstance(this)));
                onCollectionLoaded(null);
                return false;
            }
            mShowingStorageRequestDialog = true;
            Timber.i("Displaying initial permission request dialog:%s", mShowingStorageRequestDialog);
            // Request storage permission if we don't have it (e.g. on Android 6.0+)
            new MaterialDialog.Builder(this)
                    .title(R.string.need_storage_permission_title)
                    .titleColor(ContextCompat.getColor(this, R.color.new_primary_color))
                    .titleGravity(GravityEnum.CENTER)
                    .content(R.string.need_storage_permission_content)
                    .negativeText(R.string.need_storage_permission_deny)
                    .positiveText(R.string.need_storage_permission_confirm)

                    .positiveColor(ContextCompat.getColor(this, R.color.new_primary_color))

                    .onPositive((innerDialog, innerWhich) -> {
                        mClosedWelcomeMessage = true;

                        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                REQUEST_STORAGE_PERMISSION);

                        mShowingStorageRequestDialog = false;

                    })
                    .onNegative((innerDialog, innerWhich) -> {
                        mShowingStorageRequestDialog = false;
                        AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(HAD_DENIED_STORAGE_ACCESS,true).apply();
                        Connection.sendCommonGet(fetchCustomDialogListener, new Connection.Payload("common/dialog", "?unionid=" + DeviceID.getDeviceId(this), Connection.Payload.REST_TYPE_GET, "", "nothing", HostNumFactory.getInstance(this)));

                        onCollectionLoaded(null);
                    })

                    .negativeColor(ContextCompat.getColor(this, R.color.new_primary_text_secondary_color))
                    .cancelable(false)
                    .canceledOnTouchOutside(false)
                    .show();

            return false;
        } else {
            return false;
        }
    }


    public void showExportDialog() {
        String msg = getResources().getString(R.string.confirm_apkg_export);
        showDialogFragment(ExportDialog.newInstance(msg, this));
    }


    public void showStartupScreensAndDialogs(SharedPreferences preferences, int skip) {

        if (!BackupManager.enoughDiscSpace(CollectionHelper.getCurrentAnkiDroidDirectory(this))) {
            Timber.i("Not enough space to do backup");
            showDialogFragment(DeckPickerNoSpaceLeftDialog.newInstance());
        } else if (preferences.getBoolean("noSpaceLeft", false)) {
            Timber.i("No space left");
            showDialogFragment(DeckPickerBackupNoSpaceLeftDialog.newInstance());
            preferences.edit().remove("noSpaceLeft").apply();
        } else if ("".equals(preferences.getString("lastVersion", ""))) {
            Timber.i("Fresh install");
            preferences.edit().putString("lastVersion", VersionUtils.getPkgVersionName()).apply();
            onFinishedStartup();
        } else if (skip < 2 && !preferences.getString("lastVersion", "").equals(VersionUtils.getPkgVersionName())) {
            Timber.i("AnkiDroid is being updated and a collection already exists.");
            // The user might appreciate us now, see if they will help us get better?
            if (!preferences.contains(UsageAnalytics.ANALYTICS_OPTIN_KEY)) {
                showDialogFragment(DeckPickerAnalyticsOptInDialog.newInstance());
            }

            // For upgrades, we check if we are upgrading
            // to a version that contains additions to the database integrity check routine that we would
            // like to run on all collections. A missing version number is assumed to be a fresh
            // installation of AnkiDroid and we don't run the check.
            long current = VersionUtils.getPkgVersionCode();
            Timber.i("Current AnkiDroid version: %s", current);
            long previous;
            if (preferences.contains(UPGRADE_VERSION_KEY)) {
                // Upgrading currently installed app
                previous = getPreviousVersion(preferences, current);
            } else {
                // Fresh install
                previous = current;
            }
            preferences.edit().putLong(UPGRADE_VERSION_KEY, current).apply();

            // Delete the media database made by any version before 2.3 beta due to upgrade errors.
            // It is rebuilt on the next sync or media check
            if (previous < 20300200) {
                Timber.i("Deleting media database");
                File mediaDb = new File(CollectionHelper.getCurrentAnkiDroidDirectory(this), "collection.media.ad.db2");
                if (mediaDb.exists()) {
                    mediaDb.delete();
                }
            }
            // Recommend the user to do a full-sync if they're upgrading from before 2.3.1beta8
            if (previous < 20301208) {
                Timber.i("Recommend the user to do a full-sync");
                mRecommendFullSync = true;
            }

            // Fix "font-family" definition in templates created by AnkiDroid before 2.6alhpa23
            if (previous < 20600123) {
                Timber.i("Fixing font-family definition in templates");
                try {
                    Models models = getCol().getModels();
                    for (Model m : models.all()) {
                        String css = m.getString("css");
                        if (css.contains("font-familiy")) {
                            m.put("css", css.replace("font-familiy", "font-family"));
                            models.save(m);
                        }
                    }
                    models.flush();
                } catch (JSONException e) {
                    Timber.e(e, "Failed to upgrade css definitions.");
                }
            }

            // Check if preference upgrade or database check required, otherwise go to new feature screen
            int upgradePrefsVersion = AnkiDroidApp.CHECK_PREFERENCES_AT_VERSION;
            int upgradeDbVersion = AnkiDroidApp.CHECK_DB_AT_VERSION;

            // Specifying a checkpoint in the future is not supported, please don't do it!
            if (current < upgradePrefsVersion) {
                Timber.e("Checkpoint in future produced.");
                UIUtils.showSimpleSnackbar(this, "Invalid value for CHECK_PREFERENCES_AT_VERSION", false);
                onFinishedStartup();
                return;
            }
            if (current < upgradeDbVersion) {
                Timber.e("Invalid value for CHECK_DB_AT_VERSION");
                UIUtils.showSimpleSnackbar(this, "Invalid value for CHECK_DB_AT_VERSION", false);
                onFinishedStartup();
                return;
            }

            // Skip full DB check if the basic check is OK
            //TODO: remove this variable if we really want to do the full db check on every user
            boolean skipDbCheck = false;
            //if (previous < upgradeDbVersion && getCol().basicCheck()) {
            //    skipDbCheck = true;
            //}

            //noinspection ConstantConditions
            if ((!skipDbCheck && previous < upgradeDbVersion) || previous < upgradePrefsVersion) {
                if (previous < upgradePrefsVersion) {
                    Timber.i("showStartupScreensAndDialogs() running upgradePreferences()");
                    upgradePreferences(previous);
                }
                // Integrity check loads asynchronously and then restart deck picker when finished
                //noinspection ConstantConditions
                if (!skipDbCheck && previous < upgradeDbVersion) {
                    Timber.i("showStartupScreensAndDialogs() running integrityCheck()");
                    //#5852 - since we may have a warning about disk space, we don't want to force a check database
                    //and show a warning before the user knows what is happening.
                    new MaterialDialog.Builder(this)
                            .title(R.string.integrity_check_startup_title)
                            .content(R.string.integrity_check_startup_content)
                            .positiveText(R.string.integrity_check_positive)
                            .negativeText(R.string.close)
                            .onPositive((materialDialog, dialogAction) -> integrityCheck())
                            .onNeutral((materialDialog, dialogAction) -> this.restartActivity())
                            .onNegative((materialDialog, dialogAction) -> this.restartActivity())
                            .canceledOnTouchOutside(false)
                            .cancelable(false)
                            .build()
                            .show();

                } else if (previous < upgradePrefsVersion) {
                    Timber.i("Updated preferences with no integrity check - restarting activity");
                    // If integrityCheck() doesn't occur, but we did update preferences we should restart DeckPicker to
                    // proceed
                    this.restartActivity();
                }
            } else {
                // If no changes are required we go to the new features activity
                // There the "lastVersion" is set, so that this code is not reached again
//                if (VersionUtils.isReleaseVersion()) {
//                    Timber.i("Displaying new features");
//                    Intent infoIntent = new Intent(this, Info.class);
//                    infoIntent.putExtra(Info.TYPE_EXTRA, Info.TYPE_NEW_VERSION);
//
//                    if (skip != 0) {
//                        startActivityForResultWithAnimation(infoIntent, SHOW_INFO_NEW_VERSION,
//                                ActivityTransitionAnimation.LEFT);
//                    } else {
//                        startActivityForResultWithoutAnimation(infoIntent, SHOW_INFO_NEW_VERSION);
//                    }
//                } else {
                Timber.i("Dev Build - not showing 'new features'");
                // Don't show new features dialog for development builds
                preferences.edit().putString("lastVersion", VersionUtils.getPkgVersionName()).apply();
                String ver = getResources().getString(R.string.updated_version, VersionUtils.getPkgVersionName());
                UIUtils.showSnackbar(this, ver, true, -1, null, findViewById(R.id.root_layout), null);
                showStartupScreensAndDialogs(preferences, 2);
//                }
            }
        } else {
            // this is the main call when there is nothing special required
            Timber.i("No startup screens required");
            onFinishedStartup();
        }
    }


    // Callback method to handle database integrity check
    public void integrityCheck() {
        //#5852 - We were having issues with integrity checks where the users had run out of space.
        //display a dialog box if we don't have the space
        CollectionHelper.CollectionIntegrityStorageCheck status = CollectionHelper.CollectionIntegrityStorageCheck.createInstance(this);
        if (status.shouldWarnOnIntegrityCheck()) {
            Timber.d("Displaying File Size confirmation");
            new MaterialDialog.Builder(this)
                    .title(R.string.check_db_title)
                    .content(status.getWarningDetails(this))
                    .positiveText(R.string.integrity_check_continue_anyway)
                    .onPositive((dialog, which) -> performIntegrityCheck())
                    .negativeText(R.string.dialog_cancel)
                    .show();
        } else {
            performIntegrityCheck();
        }
    }


    private void performIntegrityCheck() {
        Timber.i("performIntegrityCheck()");
        CollectionTask.launchCollectionTask(CHECK_DATABASE, mCheckDatabaseListener);
    }


    public CheckDatabaseListener mCheckDatabaseListener = new CheckDatabaseListener();



    public class CheckDatabaseListener extends TaskListener {
        @Override
        public void onPreExecute() {
            mProgressDialog = StyledProgressDialog.show(DeckPicker.this, AnkiDroidApp.getAppResources().getString(R.string.app_name),
                    getResources().getString(R.string.check_db_message), false);
        }


        @Override
        public void onPostExecute(TaskData result) {
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }

            if (result == null) {
                handleDbError();
                return;
            }

            if (!result.objAtIndexIs(0, Collection.CheckDatabaseResult.class)) {
                if (result.getBoolean()) {
                    Timber.w("Expected result data, got nothing");
                } else {
                    handleDbError();
                }
                return;
            }

            Collection.CheckDatabaseResult databaseResult = (Collection.CheckDatabaseResult) result.getObjArray()[0];

            if (!result.getBoolean() || databaseResult.getFailed()) {
                if (databaseResult.getDatabaseLocked()) {
                    handleDbLocked();
                } else {
                    handleDbError();
                }
                return;
            }


            int count = databaseResult.getCardsWithFixedHomeDeckCount();
            if (count != 0) {
                String message = getResources().getString(R.string.integrity_check_fixed_no_home_deck, count);
                UIUtils.showThemedToast(DeckPicker.this, message, false);
            }

            String msg;
            long shrunkInMb = Math.round(databaseResult.getSizeChangeInKb() / 1024.0);
            if (shrunkInMb > 0.0) {
                msg = getResources().getString(R.string.check_db_acknowledge_shrunk, (int) shrunkInMb);
            } else {
                msg = getResources().getString(R.string.check_db_acknowledge);
            }
            // Show result of database check and restart the app
            showSimpleMessageDialog(msg, true);
        }


        @Override
        public void onProgressUpdate(TaskData value) {
            mProgressDialog.setContent(value.getString());
        }
    }


    private void restoreWelcomeMessage(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        mClosedWelcomeMessage = savedInstanceState.getBoolean("mClosedWelcomeMessage");
    }


    public static SpannableString getClickableSpan(Context context) {
        String str = context.getString(R.string.collection_load_welcome_request_permissions_details);
        SpannableString spannableString = new SpannableString(str);
        //设置下划线文字
        int protocolIndex = str.indexOf("的用户协议") + 1;
        int privateIndex = str.indexOf("和隐私政策") + 1;
        spannableString.setSpan(new UnderlineSpan(), protocolIndex, protocolIndex + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        //设置文字的单击事件
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
//                Toast.makeText(MainActivity.this,"使用条款",Toast.LENGTH_SHORT).show();
                WebViewActivity.openUrlInApp(context, URL_USER_PROTOCOL, "");
            }
        }, protocolIndex, protocolIndex + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        //设置文字的前景色
        spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.private_text_color)), protocolIndex, protocolIndex + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        //设置下划线文字
        spannableString.setSpan(new UnderlineSpan(), privateIndex, privateIndex + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        //设置文字的单击事件
        spannableString.setSpan(new ClickableSpan() {
            @Override
            public void onClick(View widget) {
//                Toast.makeText(MainActivity.this,"隐私政策",Toast.LENGTH_SHORT).show();
                WebViewActivity.openUrlInApp(context, URL_PRIVATE, "");
            }
        }, privateIndex, privateIndex + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        //设置文字的前景色
        spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(context, R.color.private_text_color)), privateIndex, privateIndex + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }


    // flag asking user to do a full sync which is used in upgrade path
    private boolean mRecommendFullSync = false;


    /**
     * Perform the following tasks:
     * Automatic backup
     * loadStudyOptionsFragment() if tablet
     * Automatic sync
     */
    private void onFinishedStartup() {
        // create backup in background if needed
        if (getCol() == null) {
            return;
        }
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        if (preferences.getBoolean(AUTO_TURN_TO_LOGIN, true) && !Consts.isLogin()) {
            Intent myAccount = new Intent(this, MyAccount.class);
            myAccount.putExtra("notLoggedIn", true);
            startActivityForResultWithAnimation(myAccount, LOG_IN_FOR_SYNC, ActivityTransitionAnimation.FADE);
            preferences.edit().putBoolean(AUTO_TURN_TO_LOGIN, false).apply();
        }
        BackupManager.performBackupInBackground(getCol().getPath(), getCol().getTime());
        // Force a full sync if flag was set in upgrade path, asking the user to confirm if necessary
        if (mRecommendFullSync) {
            mRecommendFullSync = false;
            try {
                getCol().modSchema();
            } catch (ConfirmModSchemaException e) {
                Timber.w("Forcing full sync");
                // If libanki determines it's necessary to confirm the full sync then show a confirmation dialog
                // We have to show the dialog via the DialogHandler since this method is called via an async task
                Resources res = getResources();
                Message handlerMessage = Message.obtain();
                handlerMessage.what = DialogHandler.MSG_SHOW_FORCE_FULL_SYNC_DIALOG;
                Bundle handlerMessageData = new Bundle();
                handlerMessageData.putString("message", res.getString(R.string.full_sync_confirmation_upgrade) +
                        "\n\n" + res.getString(R.string.full_sync_confirmation));
                handlerMessage.setData(handlerMessageData);
                getDialogHandler().sendMessage(handlerMessage);
            }
        }
        // Open StudyOptionsFragment if in fragmented mode

        automaticSync();
    }


    // For automatic syncing
    // 10 minutes in milliseconds.
    public static final long AUTOMATIC_SYNC_MIN_INTERVAL = 600000;


    private void automaticSync() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);

        // Check whether the option is selected, the user is signed in and last sync was AUTOMATIC_SYNC_TIME ago
        // (currently 10 minutes)
        String hkey = preferences.getString("hkey", "");
        long lastSyncTime = preferences.getLong("lastSyncTime", 0);
        if (hkey.length() != 0 && preferences.getBoolean("automaticSyncMode", true) &&
                Connection.isOnline() && getCol().getTime().intTimeMS() - lastSyncTime > AUTOMATIC_SYNC_MIN_INTERVAL) {
            Timber.i("Triggering Automatic Sync");
            sync();
        }
    }


    protected static final String UPGRADE_VERSION_KEY = "lastUpgradeVersion";


    protected long getPreviousVersion(SharedPreferences preferences, long current) {
        long previous;
        try {
            previous = preferences.getLong(UPGRADE_VERSION_KEY, current);
        } catch (ClassCastException e) {
            try {
                // set 20900203 to default value, as it's the latest version that stores integer in shared prefs
                previous = preferences.getInt(UPGRADE_VERSION_KEY, 20900203);
            } catch (ClassCastException cce) {
                // Previous versions stored this as a string.
                String s = preferences.getString(UPGRADE_VERSION_KEY, "");
                // The last version of AnkiDroid that stored this as a string was 2.0.2.
                // We manually set the version here, but anything older will force a DB check.
                if ("2.0.2".equals(s)) {
                    previous = 40;
                } else {
                    previous = 0;
                }
            }
            Timber.d("Updating shared preferences stored key %s type to long", UPGRADE_VERSION_KEY);
            // Expected Editor.putLong to be called later to update the value in shared prefs
            preferences.edit().remove(UPGRADE_VERSION_KEY).apply();
        }
        Timber.i("Previous AnkiDroid version: %s", previous);
        return previous;
    }


    private void upgradePreferences(long previousVersionCode) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        // clear all prefs if super old version to prevent any errors
        if (previousVersionCode < 20300130) {
            Timber.i("Old version of Anki - Clearing preferences");
            preferences.edit().clear().apply();
        }
        // when upgrading from before 2.5alpha35
        if (previousVersionCode < 20500135) {
            Timber.i("Old version of Anki - Fixing Zoom");
            // Card zooming behaviour was changed the preferences renamed
            int oldCardZoom = preferences.getInt("relativeDisplayFontSize", 100);
            int oldImageZoom = preferences.getInt("relativeImageSize", 100);
            preferences.edit().putInt("cardZoom", oldCardZoom).apply();
            preferences.edit().putInt("imageZoom", oldImageZoom).apply();
            if (!preferences.getBoolean("useBackup", true)) {
                preferences.edit().putInt("backupMax", 0).apply();
            }
            preferences.edit().remove("useBackup").apply();
            preferences.edit().remove("intentAdditionInstantAdd").apply();
        }

        if (preferences.contains("fullscreenReview")) {
            Timber.i("Old version of Anki - Fixing Fullscreen");
            // clear fullscreen flag as we use a integer
            try {
                boolean old = preferences.getBoolean("fullscreenReview", false);
                preferences.edit().putString("fullscreenMode", old ? "1" : "0").apply();
            } catch (ClassCastException e) {
                // TODO:  can remove this catch as it was only here to fix an error in the betas
                preferences.edit().remove("fullscreenMode").apply();
            }
            preferences.edit().remove("fullscreenReview").apply();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE_PERMISSION && permissions.length == 1) {
            Timber.i("request permission result:%s", grantResults[0]);
            Connection.sendCommonGet(fetchCustomDialogListener, new Connection.Payload("common/dialog", "?unionid=" + DeviceID.getDeviceId(this), Connection.Payload.REST_TYPE_GET, "", "nothing", HostNumFactory.getInstance(this)));
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                this.invalidateOptionsMenu();
                startLoadingCollection();
                showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(this), 0);
            } else {
                AnkiDroidApp.getSharedPrefs(this).edit().putBoolean(HAD_DENIED_STORAGE_ACCESS,true).apply();
                onCollectionLoaded(null);
            }
//            else {
//                rejectCount++;
//                // User denied access to file storage  so show error toast and display "App Info"
//                Toast.makeText(this, R.string.startup_no_storage_permission, Toast.LENGTH_LONG).show();
//                new MaterialDialog.Builder(this)
//                        .title(R.string.collection_load_welcome_request_permissions_title)
//                        .titleGravity(GravityEnum.CENTER)
//                        .content(getClickableSpan())
//                        .negativeText(R.string.dialog_cancel)
//                        .positiveText(R.string.dialog_ok)
//                        .onPositive((innerDialog, innerWhich) -> {
//                            mClosedWelcomeMessage = true;
//                            if (rejectCount > 1) {
//                                // Open the Android settings page for our app so that the user can grant the missing permission
////                                jumpForPermission = true;
//                                Intent intent = new Intent();
//                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
//                                intent.setData(uri);
//                                this.startActivityWithoutAnimation(intent);
//                            } else {
//                                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                        REQUEST_STORAGE_PERMISSION);
//                            }
//                        })
//                        .onNegative((innerDialog, innerWhich) -> {
//                            this.finishWithoutAnimation();
//                        })
//                        .cancelable(false)
//                        .canceledOnTouchOutside(false)
//                        .show();
////                finishWithoutAnimation();
//
//            }
        }
    }


    @Override
    protected TaskListener importAddListener() {
        return new ImportAddListener(this);
    }


    private static class ImportAddListener extends TaskListenerWithContext<DeckPicker> {
        public ImportAddListener(DeckPicker deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, TaskData result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            // If boolean and string are both set, we are signalling an error message
            // instead of a successful result.
            if (result != null && result.getBoolean() && result.getString() != null) {
                Timber.w("Import: Add Failed: %s", result.getString());
                deckPicker.showSimpleMessageDialog(result.getString());
            } else {
                Timber.i("Import: Add succeeded");
                AnkiPackageImporter imp = (AnkiPackageImporter) result.getObjArray()[0];
                deckPicker.showSimpleMessageDialog(TextUtils.join("\n", imp.getLog()));
                deckPicker.onRequireDeckListUpdate();
            }
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            if (deckPicker.mProgressDialog == null || !deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker,
                        deckPicker.getResources().getString(R.string.import_title), null, false);
            }
        }


        @Override
        public void actualOnProgressUpdate(@NonNull DeckPicker deckPicker, TaskData value) {
            deckPicker.mProgressDialog.setContent(value.getString());
        }
    }


    @Override
    protected final ImportReplaceListener importReplaceListener() {
        return new ImportReplaceListener(this);
    }


    private static class ImportReplaceListener extends TaskListenerWithContext<DeckPicker> {
        public ImportReplaceListener(DeckPicker deckPicker) {
            super(deckPicker);
        }


        @SuppressWarnings("unchecked")
        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, TaskData result) {
            Timber.i("Import: Replace Task Completed");
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            Resources res = deckPicker.getResources();
            if (result != null && result.getBoolean()) {
                deckPicker.onRequireDeckListUpdate();
            } else {
                deckPicker.showSimpleMessageDialog(res.getString(R.string.import_log_no_apkg), true);
            }
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            if (deckPicker.mProgressDialog == null || !deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker,
                        deckPicker.getResources().getString(R.string.import_title),
                        deckPicker.getResources().getString(R.string.import_replacing), false);
            }
        }


        @Override
        public void actualOnProgressUpdate(@NonNull DeckPicker deckPicker, TaskData value) {
            deckPicker.mProgressDialog.setContent(value.getString());
        }
    }


    @Override
    public ExportListener exportListener() {
        return new ExportListener(this);
    }


    private static class ExportListener extends TaskListenerWithContext<DeckPicker> {
        public ExportListener(DeckPicker deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.export_in_progress), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, TaskData result) {
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


    public void showMediaCheckDialog(int id) {
        showAsyncDialogFragment(MediaCheckDialog.newInstance(id, this));
    }


    public void showMediaCheckDialog(int id, List<List<String>> checkList) {
        showAsyncDialogFragment(MediaCheckDialog.newInstance(id, checkList, this));
    }


    /**
     * Show a specific sync error dialog
     *
     * @param id id of dialog to show
     */

    public void showSyncErrorDialog(int id) {
        showSyncErrorDialog(id, "");
    }


    /**
     * Show a specific sync error dialog
     *
     * @param id      id of dialog to show
     * @param message text to show
     */

    public void showSyncErrorDialog(int id, String message) {
        AsyncDialogFragment newFragment = SyncErrorDialog.newInstance(id, message, this);
        showAsyncDialogFragment(newFragment, NotificationChannels.Channel.SYNC);
    }


    /**
     * Show simple error dialog with just the message and OK button. Reload the activity when dialog closed.
     */
    private void showSyncErrorMessage(@Nullable String message) {
        String title = getResources().getString(R.string.sync_error);
        showSimpleMessageDialog(title, message, true);
    }


    /**
     * Show a simple snackbar message or notification if the activity is not in foreground
     *
     * @param messageResource String resource for message
     */
    private void showSyncLogMessage(@StringRes int messageResource, String syncMessage) {
        if (mActivityPaused) {
            Resources res = AnkiDroidApp.getAppResources();
            showSimpleNotification(res.getString(R.string.app_name),
                    res.getString(messageResource),
                    NotificationChannels.Channel.SYNC);
        } else {
            if (syncMessage == null || syncMessage.length() == 0) {
                if (messageResource == R.string.youre_offline && !Connection.getAllowSyncOnNoConnection()) {
                    //#6396 - Add a temporary "Try Anyway" button until we sort out `isOnline`
//                    View root = root.findViewById(R.id.root_layout);
                    UIUtils.showSnackbar(this, messageResource, false, R.string.sync_even_if_offline, (v) -> {
                        Connection.setAllowSyncOnNoConnection(true);
                        sync();
                    }, null);
                } else {
                    UIUtils.showSimpleSnackbar(this, messageResource, false);
                }
            } else {
                Resources res = AnkiDroidApp.getAppResources();
                showSimpleMessageDialog(res.getString(messageResource), syncMessage, false);
            }
        }
    }


    private boolean mActivityPaused = false;


    // Callback method to submit error report
    public void sendErrorReport() {
        AnkiDroidApp.sendExceptionReport(new RuntimeException(), "DeckPicker.sendErrorReport");
    }


    private RepairCollectionTask repairCollectionTask() {
        return new RepairCollectionTask(this);
    }


    private static class RepairCollectionTask extends TaskListenerWithContext<DeckPicker> {
        public RepairCollectionTask(DeckPicker deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.backup_repair_deck_progress), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, TaskData result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            if (result == null || !result.getBoolean()) {
                UIUtils.showThemedToast(deckPicker, deckPicker.getResources().getString(R.string.deck_repair_error), true);
                deckPicker.showCollectionErrorDialog();
            }
        }
    }


    protected void showCollectionErrorDialog() {
        getDialogHandler().sendEmptyMessage(DialogHandler.MSG_SHOW_COLLECTION_LOADING_ERROR_DIALOG);
    }


    // Callback method to handle repairing deck
    public void repairCollection() {
        Timber.i("Repairing the Collection");
        TaskListener listener = repairCollectionTask();
        CollectionTask.launchCollectionTask(REPAIR_COLLECTION, listener);
    }


    private final MediaCheckListener mediaCheckListener() {
        return new MediaCheckListener(this);
    }


    private static class MediaCheckListener extends TaskListenerWithContext<DeckPicker> {
        public MediaCheckListener(DeckPicker deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.check_media_message), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, TaskData result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            if (result != null && result.getBoolean()) {
                @SuppressWarnings("unchecked")
                List<List<String>> checkList = (List<List<String>>) result.getObjArray()[0];
                deckPicker.showMediaCheckDialog(MediaCheckDialog.DIALOG_MEDIA_CHECK_RESULTS, checkList);
            } else {
                deckPicker.showSimpleMessageDialog(deckPicker.getResources().getString(R.string.check_media_failed));
            }
        }
    }


    public void mediaCheck() {
        TaskListener listener = mediaCheckListener();
        CollectionTask.launchCollectionTask(CHECK_MEDIA, listener);
    }


    public void deleteUnused(List<String> unused) {
        com.ichi2.libanki.Media m = getCol().getMedia();
        for (String fname : unused) {
            m.removeFile(fname);
        }
        showSimpleMessageDialog(String.format(getResources().getString(R.string.check_media_deleted), unused.size()));
    }


    public void exit() {
        CollectionHelper.getInstance().closeCollection(false, "DeckPicker:exit()");
        finishWithoutAnimation();
    }


    public void handleDbError() {
        Timber.i("Displaying Database Error");
        showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_LOAD_FAILED);
    }


    public void handleDbLocked() {
        Timber.i("Displaying Database Locked");
        showDatabaseErrorDialog(DatabaseErrorDialog.DIALOG_DB_LOCKED);
    }


    // Helper function to check if there are any saved stacktraces
    public boolean hasErrorFiles() {
        for (String file : fileList()) {
            if (file.endsWith(".stacktrace")) {
                return true;
            }
        }
        return false;
    }


    // Sync with Anki Web
    public void sync() {
        sync(null, false);
    }


    @Override
    public void sync(String conflict) {
        sync(conflict, false);
    }


    public void sync(boolean byUser) {
        sync(null, byUser);
    }


    /**
     * The mother of all syncing attempts. this might be called from sync() as first attempt to sync a collection OR
     * from the mSyncConflictResolutionListener if the first attempt determines that a full-sync is required.
     *
     * @param syncConflictResolution Either "upload" or "download", depending on the user's choice.
     */
    public void sync(String syncConflictResolution, boolean byUser) {
        if (!Permissions.hasStorageAccessPermission(this)) {
            firstCollectionOpen();
            return;
        }
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        String hkey = preferences.getString("hkey", "");
        if (hkey == null || hkey.length() == 0) {
            if (mDeckPickerFragment != null) {
                mDeckPickerFragment.updatePullToSyncWrapper(false);
            }
            showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
        } else {
            getAccount().getToken(getBaseContext(), new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    Timber.i("before sync,we should confirm our free cloud space");
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "clouds/current", token, new Object[] {syncConflictResolution, byUser}, checkRestServerSpaceListener);
                }


                @Override
                public void onFail(String message) {
                    if (message.equals(NOT_LOGIN_ANKI_CHINA)) {
                        syncInternal("", syncConflictResolution);
                    } else {
                        handleGetTokenFailed(message);
                    }
                }
            });

        }
    }


    public boolean colIsOpen() {
        return CollectionHelper.getInstance().colIsOpen();
    }


    private void notifyServerSyncCompleted() {
        getAccount().getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                OKHttpUtil.put(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/current", token, "", notifyServerSyncCompletedListener);
//                Connection.sendCommonPUT(notifyServerSyncCompletedListener, new Connection.Payload("users/current", "", Connection.Payload.REST_TYPE_PUT, token, null, HostNumFactory.getInstance(DeckPicker.this)));
            }


            @Override
            public void onFail(String message) {
                handleGetTokenFailed(message);
            }
        });
    }


    private void syncInternal(String token, String syncConflictResolution) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        String hkey = preferences.getString("hkey", "");
        if (hkey == null || hkey.length() == 0) {
            if (mDeckPickerFragment != null) {
                mDeckPickerFragment.updatePullToSyncWrapper(false);
            }
            showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
        } else {
            if (Consts.loginAnkiChina()) {
                syncChina(token);
            } else {
                Connection.sync(mSyncListener,
                        new Connection.Payload(new Object[] {hkey, preferences.getBoolean("syn cFetchesMedia", true),
                                syncConflictResolution, HostNumFactory.getInstance(this), getServerRestSpace()}));
            }
        }
    }


    @Override
    protected void onSyncChinaStart() {
        if (mDeckPickerFragment != null) {
            runOnUiThread(() -> mDeckPickerFragment.onSync(true));
        }
    }


    @Override
    protected void onSyncChinaError(int code, String message) {
        if (mDeckPickerFragment != null) {
            runOnUiThread(() -> mDeckPickerFragment.onSync(false));
        }
    }


    @Override
    protected void onSyncCompletedAll() {
        SyncStatus.markSyncCompleted();
        runOnUiThread(() -> {
            if (mDeckPickerFragment != null) {
                mDeckPickerFragment.onSync(false);
            }
            invalidateOptionsMenu();
            serverRestSpace = -1;
            saveServerRestSpace(serverRestSpace);
            notifyServerSyncCompleted();
        });

    }


    @Override
    protected void onSyncCompletedData() {
        runOnUiThread(this::onRequireDeckListUpdate);

        WidgetStatus.update(DeckPicker.this);
        if (mDeckPickerFragment != null && mDeckPickerFragment.mFragmented) {
            try {
                mDeckPickerFragment.loadStudyOptionsFragment(false);
            } catch (IllegalStateException e) {
                // Activity was stopped or destroyed when the sync finished. Losing the
                // fragment here is fine since we build a fresh fragment on resume anyway.
                Timber.w(e, "Failed to load StudyOptionsFragment after sync.");
            }
        }
    }


    //通知sync结束；onresume；sync之前
    private OKHttpUtil.MyCallBack notifyServerSyncCompletedListener = new OKHttpUtil.MyCallBack() {
        @Override
        public void onFailure(Call call, IOException e) {

        }


        @Override
        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
            if (response.isSuccessful()) {
                Timber.i("notify server space successfully ");
                getAccount().getToken(getBaseContext(), new MyAccount.TokenCallback() {
                    @Override
                    public void onSuccess(String token) {
                        OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "clouds/current", token, new Object[] {"nothing", false}, checkRestServerSpaceListener);
                    }


                    @Override
                    public void onFail(String message) {
                        handleGetTokenFailed(message);
                    }
                });
            } else {
                Timber.i("notify server space failed ");
                new Handler(getMainLooper()).postDelayed(() -> {
                    notifyServerSyncCompleted();//5秒后重新通知一次
                }, 5000);
            }
        }
    };


    private final Connection.TaskListener mSyncListener = new Connection.CancellableTaskListener() {
        private String currentMessage;
        private long countUp;
        private long countDown;
        private boolean dialogDisplayFailure = false;


        @Override
        public void onDisconnected() {
            showSyncLogMessage(R.string.youre_offline, "");
        }


        @Override
        public void onCancelled() {
            showSyncLogMessage(R.string.sync_cancelled, "");
            if (!dialogDisplayFailure) {
                mProgressDialog.dismiss();
                // update deck list in case sync was cancelled during media sync and main sync was actually successful
                onRequireDeckListUpdate();
            }
            // reset our display failure fate, just in case it is re-used
            dialogDisplayFailure = false;
        }


        @Override
        public void onPreExecute() {
            countUp = 0;
            countDown = 0;
            final long syncStartTime = getCol().getTime().intTimeMS();

            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                try {
                    mProgressDialog = StyledProgressDialog
                            .show(DeckPicker.this, getResources().getString(R.string.sync_title),
                                    getResources().getString(R.string.sync_title) + "\n"
                                            + getResources().getString(R.string.sync_up_down_size, countUp, countDown),
                                    false);
                } catch (WindowManager.BadTokenException e) {
                    // If we could not show the progress dialog to start even, bail out - user will get a message
                    Timber.w(e, "Unable to display Sync progress dialog, Activity not valid?");
                    dialogDisplayFailure = true;
                    Connection.cancel();
                    return;
                }

                // Override the back key so that the user can cancel a sync which is in progress
                mProgressDialog.setOnKeyListener((dialog, keyCode, event) -> {
                    // Make sure our method doesn't get called twice
                    if (event.getAction() != KeyEvent.ACTION_DOWN) {
                        return true;
                    }

                    if (keyCode == KeyEvent.KEYCODE_BACK && Connection.isCancellable() &&
                            !Connection.getIsCancelled()) {
                        // If less than 2s has elapsed since sync started then don't ask for confirmation
                        if (getCol().getTime().intTimeMS() - syncStartTime < 2000) {
                            Connection.cancel();
                            mProgressDialog.setContent(R.string.sync_cancel_message);
                            return true;
                        }
                        // Show confirmation dialog to check if the user wants to cancel the sync
                        MaterialDialog.Builder builder = new MaterialDialog.Builder(DeckPicker.this);
                        builder.content(R.string.cancel_sync_confirm)
                                .cancelable(false)
                                .positiveText(R.string.dialog_ok)
                                .negativeText(R.string.continue_sync)
                                .onPositive((inner_dialog, which) -> {
                                    mProgressDialog.setContent(R.string.sync_cancel_message);
                                    Connection.cancel();
                                });
                        builder.show();
                        return true;
                    } else {
                        return false;
                    }
                });
            }

            // Store the current time so that we don't bother the user with a sync prompt for another 10 minutes
            // Note: getLs() in Libanki doesn't take into account the case when no changes were found, or sync cancelled
            SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(DeckPicker.this);
            preferences.edit().putLong("lastSyncTime", syncStartTime).apply();
        }


        @Override
        public void onProgressUpdate(Object... values) {
            Resources res = getResources();
            if (values[0] instanceof Boolean) {
                // this is the part Download missing media of syncing
                int total = (Integer) values[1];
                int done = (Integer) values[2];
                values[0] = (values[3]);
                values[1] = res.getString(R.string.sync_downloading_media, done, total);
            } else if (values[0] instanceof Integer) {
                int id = (Integer) values[0];
                if (id != 0) {
                    currentMessage = res.getString(id);
                }
                if (values.length >= 3) {
                    countUp = (Long) values[1];
                    countDown = (Long) values[2];
                }
            } else if (values[0] instanceof String) {
                currentMessage = (String) values[0];
                if (values.length >= 3) {
                    countUp = (Long) values[1];
                    countDown = (Long) values[2];
                }
            }
            if (mProgressDialog != null && mProgressDialog.isShowing()) {
                // mProgressDialog.setTitle((String) values[0]);
                mProgressDialog.setContent(currentMessage + "\n"
                        + res
                        .getString(R.string.sync_up_down_size, countUp / 1024, countDown / 1024));
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        public void onPostExecute(Connection.Payload data) {
            if (mDeckPickerFragment != null) {
                mDeckPickerFragment.updatePullToSyncWrapper(false);
            }
            String dialogMessage = "";
            String syncMessage = "";
            Timber.d("Sync Listener onPostExecute()");
            Resources res = getResources();
            try {
                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                }
            } catch (IllegalArgumentException e) {
                Timber.e(e, "Could not dismiss mProgressDialog. The Activity must have been destroyed while the AsyncTask was running");
                AnkiDroidApp.sendExceptionReport(e, "DeckPicker.onPostExecute", "Could not dismiss mProgressDialog");
            }
            syncMessage = data.message;
            boolean needRefreshServer = true;
            if (!data.success) {
                Object[] result = (Object[]) data.result;
                if (result[0] instanceof String) {
                    String resultType = (String) result[0];
                    if ("badAuth".equals(resultType)) {
                        // delete old auth information
                        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(DeckPicker.this);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("username", "");
                        editor.putString("hkey", "");
                        editor.apply();
                        // then show not logged in dialog
                        showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
                    } else if ("noServerSpace".equals(resultType)) {
                        long rest = (long) result[1];
                        long need = (long) result[2];
                        // show no changes message, use false flag so we don't show "sync error" as the Dialog title
                        dialogMessage = res.getString(R.string.no_enough_server_space, need / 1024.0 / 1024 / 1024, rest / 1024.0 / 1024 / 1024);
                        showSyncErrorDialog(SyncErrorDialog.DIALOG_NO_ENOUGH_SERVER_SPACE, dialogMessage);
                    } else if ("noChanges".equals(resultType)) {
                        SyncStatus.markSyncCompleted();
                        // show no changes message, use false flag so we don't show "sync error" as the Dialog title
                        showSyncLogMessage(R.string.sync_no_changes_message, "");
                    } else if ("clockOff".equals(resultType)) {
                        long diff = (Long) result[1];
                        if (diff >= 86100) {
                            // The difference if more than a day minus 5 minutes acceptable by ankiweb error
                            dialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff,
                                    res.getString(R.string.sync_log_clocks_unsynchronized_date));
                        } else if (Math.abs((diff % 3600.0) - 1800.0) >= 1500.0) {
                            // The difference would be within limit if we adjusted the time by few hours
                            // It doesn't work for all timezones, but it covers most and it's a guess anyway
                            dialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff,
                                    res.getString(R.string.sync_log_clocks_unsynchronized_tz));
                        } else {
                            dialogMessage = res.getString(R.string.sync_log_clocks_unsynchronized, diff, "");
                        }
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("fullSync".equals(resultType)) {
                        if (getCol().isEmpty()) {
                            // don't prompt user to resolve sync conflict if local collection empty
                            needRefreshServer = false;
                            sync("download");
                            // TODO: Also do reverse check to see if AnkiWeb collection is empty if Anki Desktop
                            // implements it
                        } else {
                            // If can't be resolved then automatically then show conflict resolution dialog
                            showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_CONFLICT_RESOLUTION);
                        }
                    } else if ("basicCheckFailed".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_basic_check_failed, res.getString(R.string.check_db));
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("dbError".equals(resultType)) {
                        showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_CORRUPT_COLLECTION, syncMessage);
                    } else if ("overwriteError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_overwrite_error);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("remoteDbError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_remote_db_error);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("sdAccessError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_write_access_error);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("finishError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_log_finish_error);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("connectionError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_connection_error);
                        if (result.length >= 1 && result[1] instanceof Exception) {
                            dialogMessage += "\n\n" + ((Exception) result[1]).getLocalizedMessage();
                        }
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("IOException".equals(resultType)) {
                        handleDbError();
                    } else if ("genericError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_generic_error);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("OutOfMemoryError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.error_insufficient_memory);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("sanityCheckError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_sanity_failed);
                        showSyncErrorDialog(SyncErrorDialog.DIALOG_SYNC_SANITY_ERROR,
                                joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("serverAbort".equals(resultType)) {
                        // syncMsg has already been set above, no need to fetch it here.
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("mediaSyncServerError".equals(resultType)) {
                        dialogMessage = res.getString(R.string.sync_media_error_check);
                        showSyncErrorDialog(SyncErrorDialog.DIALOG_MEDIA_SYNC_ERROR,
                                joinSyncMessages(dialogMessage, syncMessage));
                    } else if ("customSyncServerUrl".equals(resultType)) {
                        String url = result.length > 1 && result[1] instanceof CustomSyncServerUrlException
                                ? ((CustomSyncServerUrlException) result[1]).getUrl() : "unknown";
                        dialogMessage = res.getString(R.string.sync_error_invalid_sync_server, url);
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    } else {
                        if (result.length > 1 && result[1] instanceof Integer) {
                            int code = (Integer) result[1];
                            dialogMessage = rewriteError(code);
                            if (dialogMessage == null) {
                                dialogMessage = res.getString(R.string.sync_log_error_specific,
                                        Integer.toString(code), result[2]);
                            }
                        } else if (result[0] instanceof String) {
                            dialogMessage = res.getString(R.string.sync_log_error_specific, Integer.toString(-1), result[0]);
                        } else {
                            dialogMessage = res.getString(R.string.sync_generic_error);
                        }
                        showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                    }
                } else {
                    dialogMessage = res.getString(R.string.sync_generic_error);
                    showSyncErrorMessage(joinSyncMessages(dialogMessage, syncMessage));
                }
            } else {
                Timber.i("Sync was successful");
                if (data.data[2] != null && !"".equals(data.data[2])) {
                    Timber.i("Syncing had additional information");
                    // There was a media error, so show it
                    // Note: Do not log this data. May contain user email.
                    String message = res.getString(R.string.sync_database_acknowledge) + "\n\n" + data.data[2];
                    showSimpleMessageDialog(message);
                } else if (data.data.length > 0 && data.data[0] instanceof String
                        && ((String) data.data[0]).length() > 0) {
                    // A full sync occurred
                    String dataString = (String) data.data[0];
                    switch (dataString) {
                        case "upload":
                            Timber.i("Full Upload Completed");
                            showSyncLogMessage(R.string.sync_log_uploading_message, syncMessage);
                            break;
                        case "download":
                            Timber.i("Full Download Completed");
                            showSyncLogMessage(R.string.sync_log_downloading_message, syncMessage);
                            break;
                        default:
                            Timber.i("Full Sync Completed (Unknown Direction)");
                            showSyncLogMessage(R.string.sync_database_acknowledge, syncMessage);
                            break;
                    }
                } else {
                    Timber.i("Regular sync completed successfully");
                    showSyncLogMessage(R.string.sync_database_acknowledge, syncMessage);
                    getAccount().getToken(DeckPicker.this, new MyAccount.TokenCallback() {
                        @Override
                        public void onSuccess(String token) {
                            OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "configs/global", token, true, getServiceConfigCallback);
                        }


                        @Override
                        public void onFail(String message) {

                        }
                    });
                }
                // Mark sync as completed - then refresh the sync icon
                SyncStatus.markSyncCompleted();

//                initMenu(mToolbar.getMenu());
//                invalidateOptionsMenu();
                onRequireDeckListUpdate();
                WidgetStatus.update(DeckPicker.this);
                if (mDeckPickerFragment.mFragmented) {
                    try {
                        mDeckPickerFragment.loadStudyOptionsFragment(false);
                    } catch (IllegalStateException e) {
                        // Activity was stopped or destroyed when the sync finished. Losing the
                        // fragment here is fine since we build a fresh fragment on resume anyway.
                        Timber.w(e, "Failed to load StudyOptionsFragment after sync.");
                    }
                }

            }
            if (needRefreshServer) {
                serverRestSpace = -1;
                saveServerRestSpace(serverRestSpace);
                notifyServerSyncCompleted();
            }

            //在这里同步通知服务端刷新云存储空间
        }
    };


    @Override
    public void invalidateOptionsMenu() {
        super.invalidateOptionsMenu();
        Timber.i("invalidate options menu ");
    }


    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Nullable
    public String rewriteError(int code) {
        String msg;
        Resources res = getResources();
        switch (code) {
            case 407:
                msg = res.getString(R.string.sync_error_407_proxy_required);
                break;
            case 409:
                msg = res.getString(R.string.sync_error_409);
                break;
            case 413:
                msg = res.getString(R.string.sync_error_413_collection_size);
                break;
            case 500:
                msg = res.getString(R.string.sync_error_500_unknown);
                break;
            case 501:
                msg = res.getString(R.string.sync_error_501_upgrade_required);
                break;
            case 502:
                msg = res.getString(R.string.sync_error_502_maintenance);
                break;
            case 503:
                msg = res.getString(R.string.sync_too_busy);
                break;
            case 504:
                msg = res.getString(R.string.sync_error_504_gateway_timeout);
                break;
            default:
                msg = null;
                break;
        }
        return msg;
    }


    @Nullable
    public static String joinSyncMessages(@Nullable String dialogMessage, @Nullable String syncMessage) {
        // If both strings have text, separate them by a new line, otherwise return whichever has text
        if (!TextUtils.isEmpty(dialogMessage) && !TextUtils.isEmpty(syncMessage)) {
            return dialogMessage + "\n\n" + syncMessage;
        } else if (!TextUtils.isEmpty(dialogMessage)) {
            return dialogMessage;
        } else {
            return syncMessage;
        }
    }


    public void loginToSyncServer() {
        Intent myAccount = new Intent(this, ChooseLoginServerActivity.class);
        myAccount.putExtra("notLoggedIn", true);
        startActivityForResultWithAnimation(myAccount, LOG_IN_FOR_SYNC, ActivityTransitionAnimation.FADE);
    }


    public void goToUpgradeSpace() {
        getAccount().getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
//                WebViewActivity.openUrlInApp(DeckPicker.this, URL_UPGRADE_CLOUD_SPACE, token, RESULT_UPDATE_REST_SPACE);
                WebViewActivity.openUrlInApp(DeckPicker.this, mVipUrl, token, BE_VIP);
            }


            @Override
            public void onFail(String message) {
                handleGetTokenFailed(message);
            }
        });
    }


    protected void onTokenExpired() {
        super.onTokenExpired();
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("username", "");
        editor.putString("hkey", "");
        editor.putString("token", "");
        editor.apply();
        //  force media resync on deauth
        getCol().getMedia().forceResync();
        showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
    }


    protected void onNoWriteablePermission() {
        super.onNoWriteablePermission();
        Timber.e("onNoWriteablePermission!");
        firstCollectionOpen();
    }


    protected void startStudyOption(boolean withDeckOptions) {
        Intent intent = new Intent();
        intent.putExtra("withDeckOptions", withDeckOptions);
        intent.setClass(this, StudyOptionsActivity.class);
        startActivityForResultWithAnimation(intent, SHOW_STUDYOPTIONS, ActivityTransitionAnimation.LEFT);
    }


    protected boolean mTurnToVipHtml = false;


    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        Timber.i("on activity result:%s", requestCode);
        if (requestCode == REQUEST_PATH_UPDATE) {
            // The collection path was inaccessible on startup so just close the activity and let user restart
            finishWithoutAnimation();
        } else if (resultCode == RESULT_MEDIA_EJECTED) {
            this.onSdCardNotMounted();
            return;
        } else if (resultCode == RESULT_DB_ERROR) {
            handleDbError();
            return;
        } else if (resultCode == RESULT_UPDATE_REST_SPACE) {
            getAccount().getToken(getBaseContext(), new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "clouds/current", token, new Object[] {"nothing", false}, checkRestServerSpaceListener);
                }


                @Override
                public void onFail(String message) {
                    handleGetTokenFailed(message);
                }
            });
            return;
        }

        if (requestCode == REPORT_ERROR) {
            showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(this), 4);
        } else if (requestCode == SHOW_INFO_WELCOME || requestCode == SHOW_INFO_NEW_VERSION) {
            if (resultCode == RESULT_OK) {
                showStartupScreensAndDialogs(AnkiDroidApp.getSharedPrefs(this),
                        requestCode == SHOW_INFO_WELCOME ? 2 : 3);
            } else {
                finishWithAnimation(ActivityTransitionAnimation.DOWN);
            }
        } else if (requestCode == LOG_IN_FOR_SYNC) {
            mRefreshVipStateOnResume = true;
            mSyncOnResume = resultCode == RESULT_OK;
        } else if ((requestCode == REQUEST_REVIEW || requestCode == SHOW_STUDYOPTIONS)
                && resultCode == Reviewer.RESULT_NO_MORE_CARDS) {
            // Show a message when reviewing has finished
            if (getCol().getSched().count() == 0) {
                UIUtils.showSimpleSnackbar(this, R.string.studyoptions_congrats_finished, false);
            } else {
                UIUtils.showSimpleSnackbar(this, R.string.studyoptions_no_cards_due, false);
            }
            refreshDeckListUI(false);
        } else if (requestCode == REQUEST_BROWSE_CARDS) {
            // Store the selected deck after opening browser
            if (intent != null && intent.getBooleanExtra("allDecksSelected", false)) {
                AnkiDroidApp.getSharedPrefs(this).edit().putLong("browserDeckIdFromDeckPicker", -1L).apply();
            } else {
                long selectedDeck = getCol().getDecks().selected();
                AnkiDroidApp.getSharedPrefs(this).edit().putLong("browserDeckIdFromDeckPicker", selectedDeck).apply();
            }
        }
        if (requestCode == BE_VIP || requestCode == REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML || requestCode == SHOW_STUDYOPTIONS || requestCode == CHANGE_ACCOUNT) {
            mRefreshVipStateOnResume = true;
            mSyncOnResume = requestCode == CHANGE_ACCOUNT;
            mTurnToVipHtml = requestCode == REFRESH_LOGIN_STATE_AND_TURN_TO_VIP_HTML;
        }
        else {
            getSupportFragmentManager().getFragments();
            if (getSupportFragmentManager().getFragments().size() > 0) {
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                for (Fragment mFragment : fragments) {
                    mFragment.onActivityResult(requestCode, resultCode, intent);
                }
            }
        }
    }


    @Override
    public void onStop() {
        Timber.d("onStop()");
        super.onStop();
        if (colIsOpen()) {
            WidgetStatus.update(this);
            // Ignore the modification - a change in deck shouldn't trigger the icon for "pending changes".
            UIUtils.saveCollectionInBackground(true);
        }
    }


    @Override
    public void onPause() {
        Timber.d("onPause()");
        mActivityPaused = true;
        // The deck count will be computed on resume. No need to compute it now
        CollectionTask.cancelAllTasks(LOAD_DECK_COUNTS);
        super.onPause();
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {

            case KeyEvent.KEYCODE_A:
                Timber.i("Adding Note from keypress");
                addNote();
                break;

            case KeyEvent.KEYCODE_B:
                Timber.i("Open Browser from keypress");
                openCardBrowser(ALL_DECKS_ID);
                break;

            default:
                break;
        }

        return super.onKeyUp(keyCode, event);
    }


    protected void openInstructions() {
        WebViewActivity.openUrlInApp(DeckPicker.this, Consts.URL_INSTRUCTION, "");
    }


    public static ViewPropertyAnimator fadeIn(View view, int duration) {
        return fadeIn(view, duration, 0);
    }


    public static ViewPropertyAnimator fadeIn(View view, int duration, float translation) {
        return fadeIn(view, duration, translation, () -> view.setVisibility(View.VISIBLE));
    }


    public static ViewPropertyAnimator fadeIn(View view, int duration, float translation, Runnable startAction) {
        view.setAlpha(0);
        view.setTranslationY(translation);
        return view.animate()
                .alpha(1)
                .translationY(0)
                .setDuration(duration)
                .withStartAction(startAction);
    }


    @Override
    public void onRequireDeckListUpdate() {
//        Timber.i("onRequireDeckListUpdate,fragment:" + mDeckPickerFragment + ",context:" + mDeckPickerFragment.getContext());
        if (mDeckPickerFragment != null && mDeckPickerFragment.getContext() != null) {
            mDeckPickerFragment.updateDeckList(false);
        }
    }


    public static ViewPropertyAnimator fadeOut(View view, int duration) {
        return fadeOut(view, duration, 0);
    }


    public static ViewPropertyAnimator fadeOut(View view, int duration, float translation) {
        return fadeOut(view, duration, translation, () -> view.setVisibility(View.GONE));
    }


    public static ViewPropertyAnimator fadeOut(View view, int duration, float translation, Runnable endAction) {
        view.setAlpha(1);
        view.setTranslationY(0);
        return view.animate()
                .alpha(0)
                .translationY(translation)
                .setDuration(duration)
                .withEndAction(endAction);
    }


    public void addSharedDeck() {
        openUrl(Uri.parse(getResources().getString(R.string.shared_decks_url)));
    }
    // Callback to show confirm deck deletion dialog before deleting currently selected deck


    public void confirmDeckDeletion() {
        confirmDeckDeletion(mContextMenuDid);
    }


    @Override
    public DeleteDeckListener deleteDeckListener(long did) {
        return new DeleteDeckListener(did, this);
    }


    private static class DeleteDeckListener extends TaskListenerWithContext<DeckPicker> {
        private final long did;
        // Flag to indicate if the deck being deleted is the current deck.
        private boolean removingCurrent;


        public DeleteDeckListener(long did, DeckPicker deckPicker) {
            super(deckPicker);
            this.did = did;
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.delete_deck), false);
            if (did == deckPicker.getCol().getDecks().current().optLong("id")) {
                removingCurrent = true;
            }
        }


        @SuppressWarnings("unchecked")
        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, @Nullable TaskData result) {
            // In fragmented mode, if the deleted deck was the current deck, we need to reload
            // the study options fragment with a valid deck and re-center the deck list to the
            // new current deck. Otherwise we just update the list normally.
            if (deckPicker.mDeckPickerFragment.mFragmented && removingCurrent) {
                deckPicker.onRequireDeckListUpdate();
                deckPicker.mDeckPickerFragment.openStudyOptions(false);
            } else {
                deckPicker.onRequireDeckListUpdate();
            }

            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                try {
                    deckPicker.mProgressDialog.dismiss();
                } catch (Exception e) {
                    Timber.e(e, "onPostExecute - Exception dismissing dialog");
                }
            }
        }
    }


    public void handleEmptyCards() {
        CollectionTask.launchCollectionTask(FIND_EMPTY_CARDS, handlerEmptyCardListener());
    }


    private final HandleEmptyCardListener handlerEmptyCardListener() {
        return new HandleEmptyCardListener(this);
    }


    private static class HandleEmptyCardListener extends TaskListenerWithContext<DeckPicker> {
        public HandleEmptyCardListener(DeckPicker deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckPicker deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker, "",
                    deckPicker.getResources().getString(R.string.emtpy_cards_finding), false);
        }


        @Override
        public void actualOnPostExecute(@NonNull DeckPicker deckPicker, TaskData result) {
            final List<Long> cids = (List<Long>) result.getObjArray()[0];
            if (cids.size() == 0) {
                deckPicker.showSimpleMessageDialog(deckPicker.getResources().getString(R.string.empty_cards_none));
            } else {
                String msg = String.format(deckPicker.getResources().getString(R.string.empty_cards_count), cids.size());
                ConfirmationDialog dialog = new ConfirmationDialog();
                dialog.setArgs(msg);
                Runnable confirm = () -> {
                    deckPicker.getCol().remCards(cids);
                    UIUtils.showSimpleSnackbar(deckPicker, String.format(
                            deckPicker.getResources().getString(R.string.empty_cards_deleted), cids.size()), false);
                };
                dialog.setConfirm(confirm);
                deckPicker.showDialogFragment(dialog);
            }

            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
        }
    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("mClosedWelcomeMessage", mClosedWelcomeMessage);
    }


    private static final int MOVE_LIMITATION = 100;// 触发移动的像素距离
    private float mLastMotionX; // 手指触碰屏幕的最后一次x坐标
    private float mLastMotionY; // 手指触碰屏幕的最后一次y坐标


    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
//        Timber.i("dispatchTouchEvent:"+event );
        if (viewPager == null || viewPager.getCurrentItem() != 0) {
            return super.dispatchTouchEvent(event);
        }
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
                    openCardBrowser(ALL_DECKS_ID);
                    return true;
                }
                break;
        }
        return super.dispatchTouchEvent(event);
    }

}
