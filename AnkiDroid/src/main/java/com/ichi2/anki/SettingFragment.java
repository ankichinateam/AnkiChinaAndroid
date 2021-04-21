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

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.SyncErrorDialog;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.async.Connection;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.themes.Themes;
import com.ichi2.ui.SettingItem;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.TaskStackBuilder;
import timber.log.Timber;

import static com.ichi2.anki.MyAccount.NOT_LOGIN_ANKI_CHINA;
import static com.ichi2.anki.MyAccount.NO_TOKEN_RECORD;
import static com.ichi2.anki.MyAccount.TOKEN_IS_EXPIRED;
import static com.ichi2.libanki.Consts.URL_ANKI_COURSE;
import static com.ichi2.libanki.Consts.URL_FEEDBACK;
import static com.ichi2.libanki.Consts.URL_PRIVATE;
import static com.ichi2.libanki.Consts.URL_UPGRADE_CLOUD_SPACE;
import static com.ichi2.libanki.Consts.URL_USER_PROTOCOL;
import static com.ichi2.libanki.Consts.URL_VOLUNTEER;
import static com.ichi2.libanki.Consts.URL_VERSION;

public class SettingFragment extends AnkiFragment implements View.OnClickListener {

    View mRoot;


    @Override
    public void onCollectionLoaded(Collection col) {

    }


    private RelativeLayout rl_login;
    private LinearLayout mLl_defaultLink;

    private TextView mRl_user_name;

    private TextView mRl_cloud_space;
    private TextView mTv_logout;
    private ImageView mIv_entrance;
    private SwitchCompat mNightModeSwitch;
    private static final String NIGHT_MODE_PREFERENCE = "invertedColors";
    private static final int REQUEST_PATH_UPDATE = 1;
    public static final int REQUEST_PREFERENCES_UPDATE = 100;

    private LinearLayout mLl_switch_server;
    private TextView mTv_switch_server;
    private TextView mTv_server_name;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (mRoot == null) {
            final SharedPreferences preferences = getPreferences();
            mRoot = inflater.inflate(R.layout.fragment_setting, container, false);
            rl_login = mRoot.findViewById(R.id.rl_login);
            mLl_defaultLink = mRoot.findViewById(R.id.ll_default_link);
            mRl_user_name = mRoot.findViewById(R.id.tv_user_name);
            mTv_logout = mRoot.findViewById(R.id.tv_quit);
            mIv_entrance = mRoot.findViewById(R.id.iv_entrance);
            mRl_cloud_space = mRoot.findViewById(R.id.tv_cloud_space);

            mLl_switch_server = mRoot.findViewById(R.id.ll_switch_server);
            mTv_server_name = mRoot.findViewById(R.id.server_name);
            mTv_switch_server = mRoot.findViewById(R.id.tv_switch_server);

            rl_login.setOnClickListener(v -> getAnkiActivity().loginToSyncServer());
            mRoot.findViewById(R.id.rl_personal_setting).setOnClickListener(v -> {
                Timber.i("Navigating to settings");
                mOldColPath = CollectionHelper.getCurrentAnkiDroidDirectory(getAnkiActivity());
                // Remember the theme we started with so we can restart the Activity if it changes
                mOldTheme = Themes.getCurrentTheme(getContext());
                getAnkiActivity().startActivityForResultWithAnimation(new Intent(getAnkiActivity(), Preferences.class), REQUEST_PREFERENCES_UPDATE, ActivityTransitionAnimation.FADE);
            });
            mRoot.findViewById(R.id.rl_sync_set).setOnClickListener(v -> {
                Timber.i("AnkiDroid directory inaccessible");
                Intent i = Preferences.getPreferenceSubscreenIntent(getAnkiActivity(), "com.ichi2.anki.prefs.general");
                getAnkiActivity().startActivityWithAnimation(i, ActivityTransitionAnimation.FADE);
            });
            mNightModeSwitch = mRoot.findViewById(R.id.switch_dark_mode);
            mNightModeSwitch.setChecked(preferences.getBoolean(NIGHT_MODE_PREFERENCE, false));
            mNightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                applyNightMode(isChecked);
            });
            mRoot.findViewById(R.id.rl_dark_mode).setOnClickListener(v -> {
                Timber.i("Toggling Night Mode");
                mNightModeSwitch.performClick();
            });
            mRoot.findViewById(R.id.rl_cloud_space).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_anki_course).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_team).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_version).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_feedback).setOnClickListener(this);
            mRoot.findViewById(R.id.user_protocol).setOnClickListener(this);
            mRoot.findViewById(R.id.user_private).setOnClickListener(this);
            mLl_switch_server.setOnClickListener(this);
            new Handler().postDelayed(this::syncServerSettingItem, 500);//1秒后执行

        }
        return mRoot;
    }


    private void syncServerSettingItem() {
        Connection.sendCommonGet(initMoreDrawerMenuItemListener, new Connection.Payload("configs/1", "", Connection.Payload.REST_TYPE_GET, HostNumFactory.getInstance(getContext())));
    }


    Connection.TaskListener initMoreDrawerMenuItemListener = new Connection.TaskListener() {

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
                Timber.i("initMoreDrawerMenuItem successfully!:%s", data.result);
                try {
                    final JSONArray items = ((JSONObject) data.result).getJSONArray("data");
                    Timber.i("initMoreDrawerMenuItem %d ", items.length());
                    mLl_defaultLink.removeAllViews();
                    new Thread(() -> {
                        try {
                            List<DynamicItem> dynamicItems = new ArrayList<>();
                            for (int i = 0; i < items.length(); i++) {
                                JSONObject jsonObject = items.getJSONObject(i);
                                String title = jsonObject.optString("title");
                                String image = jsonObject.optString("image_url");
                                String link = jsonObject.optString("target_url");
                                Drawable drawable = null;
                                try {
                                    drawable = Drawable.createFromStream(
                                            new URL(image).openStream(), title + ".jpg");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                                dynamicItems.add(new DynamicItem(title, link, drawable));
                                Timber.i("load drawable result %s,%s ,%s  ", title, image, link);
                            }

                            for (int i = 0; i < dynamicItems.size(); i++) {
                                int finalI = i;
                                int[] attrs = new int[] {R.attr.settingItemBackgroundTop, R.attr.settingItemBackground, R.attr.settingItemBackgroundBottom, R.attr.settingItemBackgroundRound};
                                TypedArray ta = getAnkiActivity().obtainStyledAttributes(attrs);
                                Drawable background;
                                background = ta.getDrawable(dynamicItems.size() == 1 ? 3 : finalI == 0 ? 0 : finalI == dynamicItems.size() - 1 ? 2 : 1);
                                getAnkiActivity().runOnUiThread(() -> {
                                    DynamicItem dynamicItem = dynamicItems.get(finalI);
                                    SettingItem item = new SettingItem(getContext(), dynamicItem.title, dynamicItem.drawable);
                                    item.findViewById(R.id.rl_root).setBackground(background);
                                    item.setOnClickListener(v -> {
                                        WebViewActivity.openUrlInApp(getAnkiActivity(), dynamicItem.link, "", -1);
                                    });
                                    mLl_defaultLink.addView(item);
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }).start();


                } catch (Exception e) {
                    e.printStackTrace();
//                    UIUtils.showSimpleSnackbar(getAnkiActivity(), R.string.sync_menu_error, true);
                }
            } else {
                Timber.e("initMoreDrawerMenuItem failed, error code %d", data.statusCode);
//                UIUtils.showSimpleSnackbar(getAnkiActivity(), R.string.sync_menu_error, true);
//                if (data.returnType == 403) {
//                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.invalid_username_password, true);
//                } else {
//                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.connection_error_message, true);
//                }
            }
        }


        @Override
        public void onDisconnected() {
            UIUtils.showSimpleSnackbar(getAnkiActivity(), R.string.youre_offline, true);
        }
    };



    class DynamicItem {
        DynamicItem(String title, String link, Drawable drawable) {
            this.title = title;
            this.link = link;
            this.drawable = drawable;
        }


        String title;
        String link;
        Drawable drawable;
    }


    @Override
    public void onResume() {
        super.onResume();
        Timber.i("onResume");
        updateSwitchServerLayout();
        new Handler().postDelayed(this:: checkRestServerSpace, 1000);//1秒后执行


    }


    private void checkRestServerSpace() {
        getAnkiActivity().getAccount().getToken(getContext(), new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                Timber.i("get saved token on resume:%s", token);
                String username = getUserName();
                mRl_user_name.setText(username == null || username.isEmpty() ? "登录" : username);
                if (username == null || username.isEmpty()) {
                    mTv_logout.setVisibility(View.GONE);
                    mIv_entrance.setVisibility(View.VISIBLE);
                } else {
                    mTv_logout.setVisibility(View.VISIBLE);
                    mIv_entrance.setVisibility(View.GONE);
                }
                if (Consts.loginAnkiChina()) {
                    Connection.sendCommonGet(checkRestServerSpaceListener, new Connection.Payload("clouds/current", "", Connection.Payload.REST_TYPE_GET, token, "nothing", HostNumFactory.getInstance(getContext())));
                }

            }


            @Override
            public void onFail(String message) {
                String username = getUserName();
                mRl_user_name.setText(username == null || username.isEmpty() ? "登录" : username);
                if (username == null || username.isEmpty()) {
                    mTv_logout.setVisibility(View.GONE);
                    mIv_entrance.setVisibility(View.VISIBLE);
                } else {
                    mTv_logout.setVisibility(View.VISIBLE);
                    mIv_entrance.setVisibility(View.GONE);
                }
                String hintStr = String.format(getString(R.string.upgrade_cloud_space), "");
                mRl_cloud_space.setText(hintStr);
//                if (message.equals(TOKEN_IS_EXPIRED)) {
                getAnkiActivity().handleGetTokenFailed(message);
//                }


            }
        });
    }


    //刷新设置-获取剩余空间-未登录-登录-获取剩余空间-获取用户名
    private String getUserName() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getContext());
        return   preferences.getString("username", "");
    }


    Connection.TaskListener checkRestServerSpaceListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {
            Timber.d("checkRestServerSpaceListener.onPreExecute()");

        }


        @Override
        public void onPostExecute(Connection.Payload data) {
            if (data.success) {
                Timber.i("fetch server space successfully:%s", data.syncConflictResolution);
                try {
                    JSONObject result = ((JSONObject) data.result).getJSONObject("data");
                    long total = result.getLong("origin_size");
                    long used = result.getLong("origin_used_size");
                    String totalStr = result.getString("size");
                    String usedStr = result.getString("used_size");
                    String hint = String.format("%s/%s", usedStr, totalStr);
                    String hintStr = String.format(getString(R.string.upgrade_cloud_space), hint);
                    long rest = total - used;
                    Timber.i("fetch server space result:%d,%d,%d", total, used, rest);
                    mRl_cloud_space.setText(hintStr);
//                    getAnkiActivity().getNavigationView().getMenu().findItem(R.id.nav_cloud_space).setTitle(hintStr);
                    getAnkiActivity().saveServerRestSpace(rest);

                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                    UIUtils.showSimpleSnackbar(getAnkiActivity(), R.string.sync_generic_error, true);
                }
            } else {
                Timber.e("fetch server space failed, error code %d", data.statusCode);
                String hintStr = String.format(getString(R.string.upgrade_cloud_space), "");
                mRl_cloud_space.setText(hintStr);

            }
        }


        @Override
        public void onDisconnected() {
            UIUtils.showSimpleSnackbar(getAnkiActivity(), R.string.youre_offline, true);
        }
    };
    private String mOldColPath;
    private int mOldTheme;


    private SharedPreferences getPreferences() {
        return AnkiDroidApp.getSharedPrefs(getContext());
    }


    private void applyNightMode(boolean setToNightMode) {
        final SharedPreferences preferences = getPreferences();
        Timber.i("Night mode was %s", setToNightMode ? "enabled" : "disabled");
        preferences.edit().putBoolean(NIGHT_MODE_PREFERENCE, setToNightMode).apply();
        restartActivityInvalidateBackstack(getAnkiActivity());
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        final SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getContext());
        Timber.i("Handling Activity Result: %d. Result: %d", requestCode, resultCode);
        NotificationChannels.setup(getAnkiActivity());
        // Restart the activity on preference change
        if (requestCode == REQUEST_PREFERENCES_UPDATE) {
            if (mOldColPath != null && CollectionHelper.getCurrentAnkiDroidDirectory(getAnkiActivity()).equals(mOldColPath)) {
                // collection path hasn't been changed so just restart the current activity
                Timber.i("mOldColPath is equals current");
                if (mOldTheme != Themes.getCurrentTheme(getAnkiActivity())) {
                    // The current theme was changed, so need to reload the stack with the new theme
                    Timber.i("mOldTheme is not equals current");
                    restartActivityInvalidateBackstack(getAnkiActivity());
                }
//                else {
//                    Timber.i("mOldTheme is   equals current");
//                    getAnkiActivity().restartActivity();
//                }
            } else {
                Timber.i("mOldColPath is  not equals current");
                // collection path has changed so kick the user back to the DeckPicker
                CollectionHelper.getInstance().closeCollection(true, "Preference Modification: collection path changed");
                restartActivityInvalidateBackstack(getAnkiActivity());
            }
        } else if (requestCode == REQUEST_PATH_UPDATE) {
            // The collection path was inaccessible on startup so just close the activity and let user restart
            getAnkiActivity().finishWithoutAnimation();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }


    protected void restartActivityInvalidateBackstack(AnkiActivity activity) {
        Timber.i("AnkiActivity -- restartActivityInvalidateBackstack()");
        Intent intent = new Intent();
        intent.setClass(activity, activity.getClass());
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(activity);
        stackBuilder.addNextIntentWithParentStack(intent);
        stackBuilder.startActivities(new Bundle());
        activity.finishWithoutAnimation();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.user_protocol) {
            WebViewActivity.openUrlInApp(getAnkiActivity(), URL_USER_PROTOCOL, "");
        } else if (id == R.id.user_private) {
            WebViewActivity.openUrlInApp(getAnkiActivity(), URL_PRIVATE, "");
        } else if (id == R.id.rl_cloud_space) {
            MyAccount myAccount = new MyAccount();
            myAccount.getToken(getContext(), new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    WebViewActivity.openUrlInApp(getAnkiActivity(), URL_UPGRADE_CLOUD_SPACE, token, DeckPicker.RESULT_UPDATE_REST_SPACE);
                }


                @Override
                public void onFail(String message) {
                    if (message.equals(NOT_LOGIN_ANKI_CHINA)) {
                        Toast.makeText(getAnkiActivity(), "Anki Web账号登录，无需扩容", Toast.LENGTH_SHORT).show();
//                            UIUtils.showSimpleSnackbar(getAnkiActivity(), "Anki Web账号登录，无需扩容", true);
                        return;
                    } else if (message.equals(NO_TOKEN_RECORD)) {
                        getAnkiActivity().showSyncErrorDialog(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC);
                    }
                    getAnkiActivity().handleGetTokenFailed(message);
                }
            });
        } else if (id == R.id.rl_anki_course || id == R.id.rl_team || id == R.id.rl_version || id == R.id.rl_feedback) {
            if (id == R.id.rl_anki_course) {
                WebViewActivity.openUrlInApp(getAnkiActivity(), URL_ANKI_COURSE, "");
            } else if (id == R.id.rl_version) {
                WebViewActivity.openUrlInApp(getAnkiActivity(), URL_VERSION, "");
            } else if (id == R.id.rl_team) {
                WebViewActivity.openUrlInApp(getAnkiActivity(), URL_VOLUNTEER, "");
            } else {
                WebViewActivity.openUrlInApp(getAnkiActivity(), URL_FEEDBACK, "");
            }
        } else if (id == R.id.ll_switch_server) {
            final Dialog dialog = new Dialog(getAnkiActivity(), R.style.DialogTheme);
            dialog.setContentView(R.layout.dialog_switch_server);
            Window dialogWindow = dialog.getWindow();
            dialogWindow.setGravity(Gravity.TOP | Gravity.LEFT);
            WindowManager.LayoutParams lp = dialogWindow.getAttributes();
            int notificationBar = Resources.getSystem().getDimensionPixelSize(
                    Resources.getSystem().getIdentifier("status_bar_height", "dimen", "android"));
            int[] location = new int[2];
            mLl_switch_server.getLocationInWindow(location); //获取在当前窗体内的绝对坐标
            mLl_switch_server.getLocationOnScreen(location);//获取在整个屏幕内的绝对坐标
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.x = location[0];
            lp.y = location[1] + mLl_switch_server.getHeight() - notificationBar + 20;
            dialogWindow.setAttributes(lp);
            updateSwitchServerDialog(dialog);
            dialog.findViewById(R.id.rl_over_sea_server).setOnClickListener(view -> {
                String account = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "");
                if (account.isEmpty()) {
                    //未登陆，跳转到登陆
                    Intent myAccount = new Intent(getAnkiActivity(), MyAccount2.class);
                    myAccount.putExtra("notLoggedIn", true);
                    getAnkiActivity().startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);

                } else if (Consts.LOGIN_SERVER == Consts.LOGIN_SERVER_ANKICHINA) {
                    //已登陆，切换为当前状态
                    SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getAnkiActivity());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("username", preferences.getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, ""));
                    editor.putString("hkey", preferences.getString(Consts.KEY_SAVED_ANKI_WEB_HKEY, ""));
                    editor.putString("token", "");
                    Consts.LOGIN_SERVER = Consts.LOGIN_SERVER_ANKIWEB;
                    editor.putInt(Consts.KEY_ANKI_ACCOUNT_SERVER, Consts.LOGIN_SERVER);
                    editor.apply();
                    HostNumFactory.getInstance(getAnkiActivity()).reset();
                    //  force media resync on deauth
                    getAnkiActivity().getCol().getMedia().forceResync();
                    MobclickAgent.onProfileSignOff();
                    updateSwitchServerLayout();
                }
                checkRestServerSpace();
                dialog.dismiss();
            });
            dialog.findViewById(R.id.rl_china_server).setOnClickListener(view -> {
                String account = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "");
                if (account.isEmpty()) {
                    //未登陆，跳转到登陆
                    Intent myAccount = new Intent(getAnkiActivity(), MyAccount.class);
                    myAccount.putExtra("notLoggedIn", true);
                    getAnkiActivity().startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
                } else if (Consts.LOGIN_SERVER == Consts.LOGIN_SERVER_ANKIWEB) {

                    //已登陆，切换为当前状态
                    SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getAnkiActivity());
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString("username", preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, ""));// 切换为ankichina的登陆状态
                    editor.putString("hkey", preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_HKEY, ""));
                    editor.putString("token", preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_TOKEN, ""));
                    Consts.LOGIN_SERVER = Consts.LOGIN_SERVER_ANKICHINA;
                    editor.putInt(Consts.KEY_ANKI_ACCOUNT_SERVER, Consts.LOGIN_SERVER);
                    editor.apply();
                    HostNumFactory.getInstance(getAnkiActivity()).reset();
                    getAnkiActivity().getCol().getMedia().forceResync();
                    MobclickAgent.onProfileSignOff();
                    updateSwitchServerLayout();
                }

                checkRestServerSpace();
                dialog.dismiss();
            });
            dialog.show();
        }

    }


    private void updateSwitchServerLayout(){
        if(Consts.loginAnkiChina()){
            mLl_switch_server.setVisibility(View.VISIBLE);
            mTv_server_name.setText("国内服务器");
        }else if(Consts.loginAnkiWeb()){
            mLl_switch_server.setVisibility(View.VISIBLE);
            mTv_server_name.setText("海外服务器");
        }else{
            mLl_switch_server.setVisibility(View.GONE);
        }
    }

    private void updateSwitchServerDialog(Dialog dialog) {
        String chinaAccount = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "未登录同步账号");
        String ankiAccount = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "未登录同步账号");
        if (chinaAccount.isEmpty()||chinaAccount.equals("未登录同步账号")) {
            ((TextView) dialog.findViewById(R.id.quit_china_server)).setText("登陆");
            dialog.findViewById(R.id.quit_china_server).setSelected(false);
            dialog.findViewById(R.id.quit_china_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), MyAccount.class);
                myAccount.putExtra("notLoggedIn", true);
                getAnkiActivity().startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        } else {
            ((TextView) dialog.findViewById(R.id.quit_china_server)).setText("退出");
            dialog.findViewById(R.id.quit_china_server).setSelected(true);
            dialog.findViewById(R.id.quit_china_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), ChooseLoginServerActivity.class);
                myAccount.putExtra("notLoggedIn", false);
                getAnkiActivity().startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        }
        if (ankiAccount.isEmpty()||ankiAccount.equals("未登录同步账号")) {
            ((TextView) dialog.findViewById(R.id.quit_over_sea_server)).setText("登陆");
            dialog.findViewById(R.id.quit_over_sea_server).setSelected(false);
            dialog.findViewById(R.id.quit_over_sea_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), MyAccount2.class);
                myAccount.putExtra("notLoggedIn", true);
                getAnkiActivity().startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        } else {
            ((TextView) dialog.findViewById(R.id.quit_over_sea_server)).setText("退出");
            dialog.findViewById(R.id.quit_over_sea_server).setSelected(true);
            dialog.findViewById(R.id.quit_over_sea_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), ChooseLoginServerActivity.class);
                myAccount.putExtra("notLoggedIn", false);
                getAnkiActivity().startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        }
        ((TextView) dialog.findViewById(R.id.china_server_account)).setText(chinaAccount);
        ((TextView) dialog.findViewById(R.id.over_sea_server_account)).setText(ankiAccount);
        if (Consts.LOGIN_SERVER == Consts.LOGIN_SERVER_ANKICHINA) {
            dialog.findViewById(R.id.using_over_sea_server).setVisibility(View.GONE);
            dialog.findViewById(R.id.using_china_server).setVisibility(View.VISIBLE);
        } else if (Consts.LOGIN_SERVER == Consts.LOGIN_SERVER_ANKIWEB) {
            dialog.findViewById(R.id.using_over_sea_server).setVisibility(View.VISIBLE);
            dialog.findViewById(R.id.using_china_server).setVisibility(View.GONE);
        } else {
            dialog.findViewById(R.id.using_over_sea_server).setVisibility(View.GONE);
            dialog.findViewById(R.id.using_china_server).setVisibility(View.GONE);
        }
    }
}
