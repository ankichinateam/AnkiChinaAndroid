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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
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
import com.ichi2.utils.OKHttpUtil;
import com.umeng.analytics.MobclickAgent;

import org.acra.data.StringFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.TaskStackBuilder;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.anki.DeckPicker.CHANGE_ACCOUNT;
import static com.ichi2.anki.DeckPicker.ForeGroundColorSpan;

import static com.ichi2.anki.DeckPicker.goAppShop;
import static com.ichi2.libanki.Consts.URL_ANKI_COURSE;
import static com.ichi2.libanki.Consts.URL_FEEDBACK;
import static com.ichi2.libanki.Consts.URL_PRIVATE;

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
    private SwitchCompat mNightModeSwitch,mAutoSyncSwitch ;
    private static final String NIGHT_MODE_PREFERENCE = "invertedColors";
    private static final int REQUEST_PATH_UPDATE = 1;
    public static final int REQUEST_PREFERENCES_UPDATE = 100;

    private LinearLayout mLl_switch_server;
    private TextView mTv_switch_server;
    private TextView mTv_server_name;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Timber.i("on create view in setting fragment");
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

            rl_login.setOnClickListener(v -> ((DeckPicker) getAnkiActivity()).loginToSyncServer());
            mRoot.findViewById(R.id.rl_personal_setting).setOnClickListener(v -> {
                Timber.i("Navigating to settings");
                mOldColPath = CollectionHelper.getCurrentAnkiDroidDirectory(getAnkiActivity());
                // Remember the theme we started with so we can restart the Activity if it changes
                mOldTheme = Themes.getCurrentTheme(getContext());
                getAnkiActivity().startActivityForResultWithAnimation(new Intent(getAnkiActivity(), Preferences.class), REQUEST_PREFERENCES_UPDATE, ActivityTransitionAnimation.FADE);
            });
//            mRoot.findViewById(R.id.rl_sync_set).setOnClickListener(v -> {
//                Timber.i("AnkiDroid directory inaccessible");
//                Intent i = Preferences.getPreferenceSubscreenIntent(getAnkiActivity(), "com.ichi2.anki.prefs.general");
//                getAnkiActivity().startActivityWithAnimation(i, ActivityTransitionAnimation.FADE);
//            });
            mNightModeSwitch = mRoot.findViewById(R.id.switch_dark_mode);
            mNightModeSwitch.setChecked(preferences.getBoolean(NIGHT_MODE_PREFERENCE, false));
            mNightModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                applyNightMode(isChecked);
            });

            mAutoSyncSwitch = mRoot.findViewById(R.id.switch_auto_sync);
            mAutoSyncSwitch.setChecked(preferences.getBoolean("automaticSyncMode", true));
            mAutoSyncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                preferences.edit().putBoolean("automaticSyncMode", isChecked).apply();
            });
            mRoot.findViewById(R.id.rl_dark_mode).setOnClickListener(v -> {
                Timber.i("Toggling Night Mode");
                mNightModeSwitch.performClick();
            });
            mRoot.findViewById(R.id.vip_power).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_anki_course).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_team).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_version).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_feedback).setOnClickListener(this);
            mRoot.findViewById(R.id.user_protocol).setOnClickListener(this);
            mRoot.findViewById(R.id.user_private).setOnClickListener(this);
            mRoot.findViewById(R.id.rl_market_like).setOnClickListener(this);
            mLl_switch_server.setOnClickListener(this);
            mVipText = mRoot.findViewById(R.id.vip_text);
            mVipPower = mRoot.findViewById(R.id.vip_power);
            mVipText.setText(mVip ? getVipString(mVipDay) : getNotVipString());
            mVipPower.setText(mVip ? "查看权益" : "立即开通");
            OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "configs/1", "", "", new OKHttpUtil.MyCallBack() {
                @Override
                public void onFailure(Call call, IOException e) {

                }


                @Override
                public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Timber.i("initMoreDrawerMenuItem successfully!:%s", response.body());
                        try {
                            final JSONObject object = new JSONObject(response.body().string());
                            final JSONArray items = object.getJSONArray("data");
                            Timber.i("initMoreDrawerMenuItem %d ", items.length());

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
                            getAnkiActivity().runOnUiThread(() -> {
                                mLl_defaultLink.removeAllViews();
                            for (int i = 0; i < dynamicItems.size(); i++) {
                                int finalI = i;
                                int[] attrs = new int[] {R.attr.settingItemBackgroundTop, R.attr.settingItemBackground, R.attr.settingItemBackgroundBottom, R.attr.settingItemBackgroundRound};
                                TypedArray ta = getAnkiActivity().obtainStyledAttributes(attrs);
                                Drawable background;
                                background = ta.getDrawable(dynamicItems.size() == 1 ? 3 : finalI == 0 ? 0 : finalI == dynamicItems.size() - 1 ? 2 : 1);

                                    DynamicItem dynamicItem = dynamicItems.get(finalI);
                                    SettingItem item = new SettingItem(getContext(), dynamicItem.title, dynamicItem.drawable);
                                    item.findViewById(R.id.rl_root).setBackground(background);
                                    item.setOnClickListener(v -> {
                                        WebViewActivity.openUrlInApp(getAnkiActivity(), dynamicItem.link, "", -1);
                                    });
                                    mLl_defaultLink.addView(item);

                            }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        Timber.e("initMoreDrawerMenuItem failed, error code %d", response.code());
                    }
                }
            });
        }

        return mRoot;
    }


    private String mVipUrl;
    private boolean mVip;
    private int mVipDay;
    private String mVipExpireAt;
    private TextView mVipText;
    private TextView mVipPower;



    public SpannableStringBuilder getVipString(int vipDay) {
        return ForeGroundColorSpan(String.format("你已经成为超级学霸%d天", vipDay), 4, 9, Color.parseColor("#ffffdda2"));
    }


    public SpannableStringBuilder getNotVipString() {
        return ForeGroundColorSpan("开通超级学霸，尊享全部超能力", 2, 7, Color.parseColor("#ffffdda2"));
    }


    //VIP页面地址在登录和非登录态是不一样的，如有APP登录或者登出操作请重新调此接口更新URL
    protected void onRefreshVipState(boolean isVip, String vipUrl, int vipDay, String vipExpireAt) {
        Timber.i("onRefreshVipState:%s", vipUrl);
        mVip = isVip;
        mVipUrl = vipUrl;
        mVipDay = vipDay;
        mVipExpireAt = vipExpireAt;
        if (mVipText != null) {
            mVipText.setText(mVip ? getVipString(mVipDay) : getNotVipString());
        }
        if (mVipPower != null) {
            mVipPower.setText(isVip ? "查看权益" : "立即开通");
        }
    }


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
        new Handler().postDelayed(this::checkRestServerSpace, 1000);//1秒后执行


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
                    mRl_cloud_space.setVisibility(View.VISIBLE);
                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "clouds/current", token, "nothing", checkRestServerSpaceListener);

//                    Connection.sendCommonGet(checkRestServerSpaceListener, new Connection.Payload("clouds/current", "", Connection.Payload.REST_TYPE_GET, token, "nothing", HostNumFactory.getInstance(getContext())));
                }else {
                    mRl_cloud_space.setVisibility(View.GONE);
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
                String hintStr = "";
                mRl_cloud_space.setText(hintStr);
                mRl_cloud_space.setVisibility(View.GONE);
//                if (message.equals(TOKEN_IS_EXPIRED)) {
                ((DeckPicker) getAnkiActivity()).handleGetTokenFailed(message);
//                }
            }
        });
    }


    //刷新设置-获取剩余空间-未登录-登录-获取剩余空间-获取用户名
    private String getUserName() {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getContext());
        return preferences.getString("username", "");
    }


    private final OKHttpUtil.MyCallBack checkRestServerSpaceListener = new OKHttpUtil.MyCallBack() {
        @Override
        public void onFailure(Call call, IOException e) {
            e.printStackTrace();
        }


        @Override
        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
            if (response.isSuccessful()) {
                Timber.i("fetch server space successfully ");
                try {
                    JSONObject result = (new JSONObject(response.body().string())).getJSONObject("data");
                    long total = result.getLong("origin_size");
                    long used = result.getLong("origin_used_size");
                    String totalStr = result.getString("size");
                    String usedStr = result.getString("used_size");
                    String hint = String.format("%s/%s", usedStr, totalStr);
                    long rest = total - used;
                    Timber.i("fetch server space result:%d,%d,%d", total, used, rest);
                    getAnkiActivity().runOnUiThread(() -> {
                        mRl_cloud_space.setText(hint);
                        mRl_cloud_space.setVisibility(View.VISIBLE);
                    });

//                    getAnkiActivity().getNavigationView().getMenu().findItem(R.id.nav_cloud_space).setTitle(hintStr);
                    getAnkiActivity().saveServerRestSpace(rest);

                } catch (org.json.JSONException e) {
                    e.printStackTrace();
                    UIUtils.showSimpleSnackbar(getAnkiActivity(), R.string.sync_generic_error, true);
                }
            } else {
                Timber.e("fetch server space failed, error code %d", response.code());
                String hintStr = "";
                getAnkiActivity().runOnUiThread(() -> {
                    mRl_cloud_space.setText(hintStr);
                    mRl_cloud_space.setVisibility(View.GONE);
                });


            }

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

    @SuppressWarnings("deprecation")
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


    public DeckPicker getAnkiActivity() {
        return (DeckPicker) super.getAnkiActivity();
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.user_protocol) {
            WebViewActivity.openUrlInApp(getAnkiActivity(), URL_USER_PROTOCOL, "");
        } else if (id == R.id.user_private) {
            WebViewActivity.openUrlInApp(getAnkiActivity(), URL_PRIVATE, "");
        } else if (id == R.id.rl_market_like) {
            goAppShop( getAnkiActivity(), BuildConfig.APPLICATION_ID, "");
        } else if (id == R.id.vip_power) {
            Timber.i("click vip button");
            getAnkiActivity().openVipUrl(mVipUrl);
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


    private void updateSwitchServerLayout() {
        if (Consts.loginAnkiChina()) {
            mLl_switch_server.setVisibility(View.VISIBLE);
            mTv_server_name.setText("国内服务器");
        } else if (Consts.loginAnkiWeb()) {
            mLl_switch_server.setVisibility(View.VISIBLE);
            mTv_server_name.setText("海外服务器");
        } else {
            mLl_switch_server.setVisibility(View.GONE);
        }
    }


    private void updateSwitchServerDialog(Dialog dialog) {
        String chinaAccount = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "未登录同步账号");
        String ankiAccount = AnkiDroidApp.getSharedPrefs(getAnkiActivity()).getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "未登录同步账号");
        if (chinaAccount.isEmpty() || chinaAccount.equals("未登录同步账号")) {
            ((TextView) dialog.findViewById(R.id.quit_china_server)).setText("登陆");
            dialog.findViewById(R.id.quit_china_server).setSelected(false);
            dialog.findViewById(R.id.quit_china_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), MyAccount.class);
                myAccount.putExtra("notLoggedIn", true);
                getAnkiActivity().startActivityForResultWithAnimation(myAccount, CHANGE_ACCOUNT, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        } else {
            ((TextView) dialog.findViewById(R.id.quit_china_server)).setText("退出");
            dialog.findViewById(R.id.quit_china_server).setSelected(true);
            dialog.findViewById(R.id.quit_china_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), ChooseLoginServerActivity.class);
                myAccount.putExtra("notLoggedIn", false);
                getAnkiActivity().startActivityForResultWithAnimation(myAccount, CHANGE_ACCOUNT, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        }
        if (ankiAccount.isEmpty() || ankiAccount.equals("未登录同步账号")) {
            ((TextView) dialog.findViewById(R.id.quit_over_sea_server)).setText("登陆");
            dialog.findViewById(R.id.quit_over_sea_server).setSelected(false);
            dialog.findViewById(R.id.quit_over_sea_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), MyAccount2.class);
                myAccount.putExtra("notLoggedIn", true);
                getAnkiActivity().startActivityForResultWithAnimation(myAccount, CHANGE_ACCOUNT, ActivityTransitionAnimation.FADE);
                dialog.dismiss();
            });
        } else {
            ((TextView) dialog.findViewById(R.id.quit_over_sea_server)).setText("退出");
            dialog.findViewById(R.id.quit_over_sea_server).setSelected(true);
            dialog.findViewById(R.id.quit_over_sea_server).setOnClickListener(view -> {
                Intent myAccount = new Intent(getAnkiActivity(), ChooseLoginServerActivity.class);
                myAccount.putExtra("notLoggedIn", false);
                getAnkiActivity().startActivityForResultWithAnimation(myAccount, CHANGE_ACCOUNT, ActivityTransitionAnimation.FADE);
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
