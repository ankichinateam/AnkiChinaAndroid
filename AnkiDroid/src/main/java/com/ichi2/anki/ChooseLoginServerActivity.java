/***************************************************************************************
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


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Model;
import com.ichi2.libanki.Utils;
import com.ichi2.themes.Themes;
import com.ichi2.utils.JSONException;
import com.ichi2.utils.JSONObject;
import com.ichi2.utils.OKHttpUtil;
import com.ichi2.utils.okhttp.utils.OKHttpUtils;
import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;
import com.umeng.analytics.MobclickAgent;
import com.umeng.socialize.UMAuthListener;
import com.umeng.socialize.UMShareAPI;
import com.umeng.socialize.UMShareConfig;
import com.umeng.socialize.bean.SHARE_MEDIA;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import androidx.appcompat.widget.Toolbar;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class ChooseLoginServerActivity extends MyAccount {
    private LinearLayout mBtnLoginQQ, mBtnLoginWX;
    private FloatingActionButton mBtnLoginPhone, mBtnLoginMore;

    private UMShareAPI mShareAPI;

    private boolean mBoundWX, mBoundQQ;
    RelativeLayout qqLayout;
    RelativeLayout wxLayout;
    TextView bindHint;
    TextView unbindHint;
    TextView wxName;
    Button wxButton;
    TextView qqName;
    Button qqButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.setThemeLegacy(this);
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        if (!Consts.savedAnkiChinaAccount(preferences) ) {
            //未登录
            setContentView(R.layout.activity_choose_login_server);
        } else {
            //已登录
            setContentView(R.layout.activity_account_manager);
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                TextView title = toolbar.findViewById(R.id.toolbar_title);
                title.setVisibility(View.VISIBLE);
                title.setText("账号管理");
            }

            qqLayout = findViewById(R.id.qq_layout);
            wxLayout = findViewById(R.id.wx_layout);
            mMainLayout = findViewById(R.id.main_layout);
            bindHint = findViewById(R.id.bind_hint);
            unbindHint = findViewById(R.id.unbind_hint);
            wxName = findViewById(R.id.wx_name);
            wxButton = findViewById(R.id.bind_wx);
            wxButton.setOnClickListener(view -> {
                if(view.isSelected()){
                    //去绑定
                    mShareAPI.getPlatformInfo(ChooseLoginServerActivity.this, SHARE_MEDIA.WEIXIN, umWXBindListener);
                }else {
                    //解绑
                    unbind("weixin");
                }
            });
            qqName = findViewById(R.id.qq_name);
            qqButton = findViewById(R.id.bind_qq);
            qqButton.setOnClickListener(view -> {
                if(view.isSelected()){
                    //去绑定
                    mShareAPI.getPlatformInfo(ChooseLoginServerActivity.this, SHARE_MEDIA.QQ, umQQBindListener);
                }else {
                    //解绑
                    unbind("qq");
                }
            });
            updateButtonState();
        }

        mBtnLoginQQ = findViewById(R.id.login_button_qq);
        mBtnLoginWX = findViewById(R.id.login_button_wx);
        UMShareConfig config = new UMShareConfig();
        config.isNeedAuthOnGetUserInfo(true);
        UMShareAPI.get(this).setShareConfig(config);
        mShareAPI = UMShareAPI.get(this);
//        regToWx();
    }

    private void unbind(String type){
        getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                new MaterialDialog.Builder(ChooseLoginServerActivity.this)
                        .title("解除绑定")
                        .iconAttr(R.attr.dialogErrorIcon)
//                        .content("是否确认解绑"+(type.equals("weixin")?"微信":"QQ"))
                        .content("是否解除绑定当前账号？")
                        .positiveText("确认")
                        .negativeText("取消")
                        .onPositive((dialog2, which) -> {
                            dialog2.dismiss();
                            RequestBody formBody = new FormBody.Builder()
                                    .add("type", type)
                                    .build();
                            OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/unBindLogin", formBody, token, "", unBindCallback);
                        })
                        .build().show();

            }


            @Override
            public void onFail(String message) {

            }
        });
    }
    OKHttpUtil.MyCallBack unBindCallback =new OKHttpUtil.MyCallBack() {
        @Override
        public void onFailure(Call call, IOException e) {

        }


        @Override
        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
            if (response.isSuccessful()) {
                try {
                    JSONObject result = (new JSONObject(response.body().string()));
                    Timber.i("unbind result:%s ", result.toString());
                    int statusCode = result.getInt("status_code");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ChooseLoginServerActivity.this,result.getString("message"),Toast.LENGTH_SHORT).show();
                            if (statusCode == 0) {
                                runOnUiThread(ChooseLoginServerActivity.this::updateButtonState);
                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Timber.e("unbind wx failed, error code %d", response.code());
            }
        }
    };
    LinearLayout mMainLayout ;


    private void updateButtonState() {
        Handler mainHandler = new Handler();
         getToken(this, new MyAccount.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/getBindInfo", token, "", new OKHttpUtil.MyCallBack() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Timber.i("getBindInfo!:%s", e.getMessage());
                    }


                    @Override
                    public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                        if (response.isSuccessful()) {

                            try {
                                final JSONObject object = new JSONObject(response.body().string());
                                Timber.i("getBindInfo!:%s", object.toString());
                                final JSONObject item = object.getJSONObject("data");
                                mainHandler.post(() -> {
                                    mMainLayout.removeViews(0, mMainLayout.getChildCount());

                                    try {
                                        JSONObject wx = item.getJSONObject("weixin");
                                        wx.get("nickname");
                                        mBoundWX = true;
                                        wxName.setText(wx.getString("nickname"));
                                        wxName.setVisibility(View.VISIBLE);
                                        wxButton.setSelected(false);
                                        wxButton.setText("已绑定");

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        mBoundWX = false;
                                        wxName.setVisibility(View.GONE);
                                        wxButton.setSelected(true);
                                        wxButton.setText("去绑定");
                                    }

                                    try {
                                        JSONObject qq = item.getJSONObject("qq");
                                        qq.get("nickname");
                                        mBoundQQ = true;
                                        qqName.setText(qq.getString("nickname"));
                                        qqName.setVisibility(View.VISIBLE);
                                        qqButton.setSelected(false);
                                        qqButton.setText("已绑定");


                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        mBoundQQ = false;
                                        qqName.setVisibility(View.GONE);
                                        qqButton.setSelected(true);
                                        qqButton.setText("去绑定");
                                    }
                                    if(mBoundWX||mBoundQQ){
                                        mMainLayout.addView(bindHint);
                                        bindHint.setText("已绑定账号");
                                    }
                                    //已绑定账号放在列表的上方
                                    if (mBoundWX) {
                                        mMainLayout.addView(wxLayout);
                                    }
                                    if (mBoundQQ) {
                                        mMainLayout.addView(qqLayout);
                                    }
                                    if(!(mBoundWX&&mBoundQQ)){
                                        if(!mBoundWX&&!mBoundQQ){
                                            mMainLayout.addView(bindHint);
                                            bindHint.setText("绑定后可直接登录");
                                        }else {
                                            mMainLayout.addView(unbindHint);
                                        }
                                        if (!mBoundWX) {
                                            mMainLayout.addView(wxLayout);
                                        }
                                        if (!mBoundQQ) {
                                            mMainLayout.addView(qqLayout);
                                        }
                                    }

                                });

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            Timber.e("get token failed, error code %d", response.code());
                        }
                    }
                });

            }


            @Override
            public void onFail(String message) {

            }
        });
    }




    public void onBackPressed(View view) {
        onBackPressed();
    }

    public void onLoginQQButtonClick(View view) {
        mShareAPI.getPlatformInfo(this, SHARE_MEDIA.QQ, umQQAuthListener);
    }


    public void onLoginWXButtonClick(View view) {

        mShareAPI.getPlatformInfo(this, SHARE_MEDIA.WEIXIN, umWXAuthListener);

//        final SendAuth.Req req = new SendAuth.Req();
//        req.scope = "snsapi_userinfo";
//        req.state = "wechat_sdk_anki_china";
//        api.sendReq(req);
//        Intent myAccount = new Intent(this, MyAccount2.class);
//        myAccount.putExtra("notLoggedIn", !view.isSelected());
//        startActivityWithAnimation(myAccount,  ActivityTransitionAnimation.FADE);

    }


    private UMAuthListener umQQAuthListener = new UMAuthListener() {

        @Override
        public void onStart(SHARE_MEDIA share_media) {

        }


        @Override
        public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
            /**
             * 微信返回的openID和unionID都可以实现用户标识的需求，二者的区别在于，unionID可以实现同一个开发者账号下的应用之间账号打通的需求
             * openid：uid
             * unionid:unionid
             * accesstoken: accessToken （6.2以前用access_token）
             * refreshtoken: refreshtoken: （6.2以前用refresh_token）
             * 过期时间：expiration （6.2以前用expires_in）
             * name：name（6.2以前用screen_name）
             * 城市：city
             * 省份：province
             * 国家：country
             * 性别：gender
             * 头像：iconurl（6.2以前用profile_image_url）
             * */
            String name = map.get("name");
            String accesstoken = map.get("accessToken");
            Timber.i("QQ用户名：" + name);
            Timber.i("QQ accesstoken：" + accesstoken);
            RequestBody formBody = new FormBody.Builder()
                    .add("access_token", accesstoken)
                    .build();
            OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "qqAuth", formBody, "", "", new OKHttpUtil.MyCallBack() {
                @Override
                public void onFailure(Call call, IOException e) {

                }


                @Override
                public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject result = (new JSONObject(response.body().string()));
                            Timber.i("fetch service login qq result:%s ", result.toString());
                            int statusCode = result.getInt("status_code");
                            if (statusCode == 0) {
                                //登录成功
                                JSONObject data = result.getJSONObject("data");
                                runOnUiThread(() -> {
                                    onLoginSuccessfully(data);

                                });
                            } else {
                                if (statusCode == 1400) {
                                    //未绑定手机号
                                    JSONObject data = result.getJSONObject("data");
                                    String bindKey = data.getString("bind_key");
                                    Intent myAccount = new Intent(ChooseLoginServerActivity.this, MyAccount.class);
                                    myAccount.putExtra("notLoggedIn", true);
                                    myAccount.putExtra("bind_key", bindKey);
                                    startActivityForResultWithAnimation(myAccount,REQUEST_CODE_LOGIN_ANKI_WEB, ActivityTransitionAnimation.FADE);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Timber.e("fetch service login qq failed, error code %d", response.code());
                    }
                }
            });
            //拿到信息去请求登录接口。。。
//            ToastUtils.show("授权成功");
        }


        @Override
        public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
//            ToastUtils.show("授权失败");
        }


        @Override
        public void onCancel(SHARE_MEDIA share_media, int i) {

        }
    };
    private UMAuthListener umQQBindListener = new UMAuthListener() {

        @Override
        public void onStart(SHARE_MEDIA share_media) {

        }


        @Override
        public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
            /**
             * 微信返回的openID和unionID都可以实现用户标识的需求，二者的区别在于，unionID可以实现同一个开发者账号下的应用之间账号打通的需求
             * openid：uid
             * unionid:unionid
             * accesstoken: accessToken （6.2以前用access_token）
             * refreshtoken: refreshtoken: （6.2以前用refresh_token）
             * 过期时间：expiration （6.2以前用expires_in）
             * name：name（6.2以前用screen_name）
             * 城市：city
             * 省份：province
             * 国家：country
             * 性别：gender
             * 头像：iconurl（6.2以前用profile_image_url）
             * */
            String name = map.get("name");
            String accesstoken = map.get("accessToken");
            Timber.i("QQ用户名：" + name);
            Timber.i("QQ accesstoken：" + accesstoken);
            RequestBody formBody = new FormBody.Builder()
                    .add("access_token", accesstoken)
                    .add("type", "qq")
                    .build();
            getToken(ChooseLoginServerActivity.this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/bindLogin", formBody, token, "", new OKHttpUtil.MyCallBack() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }


                        @Override
                        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                try {
                                    JSONObject result = (new JSONObject(response.body().string()));
                                    Timber.i("fetch service login qq result:%s ", result.toString());
                                    int statusCode = result.getInt("status_code");
                                    runOnUiThread(() -> {
                                        Toast.makeText(ChooseLoginServerActivity.this,result.getString("message"),Toast.LENGTH_SHORT).show();
                                        if(statusCode==0)
                                            updateButtonState();
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Timber.e("fetch service login qq failed, error code %d", response.code());
                            }
                        }
                    });
                }


                @Override
                public void onFail(String message) {

                }
            });

            //拿到信息去请求登录接口。。。
//            ToastUtils.show("授权成功");
        }


        @Override
        public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
//            ToastUtils.show("授权失败");
        }


        @Override
        public void onCancel(SHARE_MEDIA share_media, int i) {

        }
    };
    private UMAuthListener umWXAuthListener = new UMAuthListener() {

        @Override
        public void onStart(SHARE_MEDIA share_media) {

        }


        @Override
        public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
            /**
             * 微信返回的openID和unionID都可以实现用户标识的需求，二者的区别在于，unionID可以实现同一个开发者账号下的应用之间账号打通的需求
             * openid：uid
             * unionid:unionid
             * accesstoken: accessToken （6.2以前用access_token）
             * refreshtoken: refreshtoken: （6.2以前用refresh_token）
             * 过期时间：expiration （6.2以前用expires_in）
             * name：name（6.2以前用screen_name）
             * 城市：city
             * 省份：province
             * 国家：country
             * 性别：gender
             * 头像：iconurl（6.2以前用profile_image_url）
             * */
            String name = map.get("name");
            String accesstoken = map.get("accessToken");
            String openid = map.get("uid");
            Timber.i("微信用户名：" + name);
            Timber.i("微信 accesstoken：" + accesstoken);
            Timber.i("微信 openid：" + openid);
            RequestBody formBody = new FormBody.Builder()
                    .add("access_token", accesstoken)
                    .add("openid", openid)
                    .build();
            OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "weixinAuth", formBody, "", "", new OKHttpUtil.MyCallBack() {
                @Override
                public void onFailure(Call call, IOException e) {

                }


                @Override
                public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject result = (new JSONObject(response.body().string()));
                            Timber.i("fetch service login wx result:%s ", result.toString());
                            int statusCode = result.getInt("status_code");
                            if (statusCode == 0) {
                                //登录成功
                                JSONObject data = result.getJSONObject("data");
                                runOnUiThread(() -> {
                                    onLoginSuccessfully(data);
                                });
                            } else {
                                if (statusCode == 1400) {
                                    //未绑定手机号
                                    JSONObject data = result.getJSONObject("data");
                                    String bindKey = data.getString("bind_key");
                                    Intent myAccount = new Intent(ChooseLoginServerActivity.this, MyAccount.class);
                                    myAccount.putExtra("notLoggedIn", true);
                                    myAccount.putExtra("bind_key", bindKey);
                                    startActivityForResultWithAnimation(myAccount,REQUEST_CODE_LOGIN_ANKI_WEB, ActivityTransitionAnimation.FADE);
                                }

                            }


                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        Timber.e("fetch service login wx failed, error code %d", response.code());
                    }
                }
            });
        }


        @Override
        public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
//            ToastUtils.show("授权失败");
        }


        @Override
        public void onCancel(SHARE_MEDIA share_media, int i) {

        }
    };
    private UMAuthListener umWXBindListener = new UMAuthListener() {

        @Override
        public void onStart(SHARE_MEDIA share_media) {

        }


        @Override
        public void onComplete(SHARE_MEDIA share_media, int i, Map<String, String> map) {
            String name = map.get("name");
            String accesstoken = map.get("accessToken");
            String openid = map.get("uid");
            Timber.i("微信用户名：" + name);
            Timber.i("微信 accesstoken：" + accesstoken);
            Timber.i("微信 openid：" + openid);
            RequestBody formBody = new FormBody.Builder()
                    .add("access_token", accesstoken)
                    .add("openid", openid)
                    .add("type", "weixin")
                    .build();
            getToken(ChooseLoginServerActivity.this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/bindLogin", formBody, token, "", new OKHttpUtil.MyCallBack() {
                        @Override
                        public void onFailure(Call call, IOException e) {

                        }


                        @SuppressLint("SuspiciousIndentation")
                        @Override
                        public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                try {
                                    JSONObject result = (new JSONObject(response.body().string()));
                                    Timber.i("fetch service login wx result:%s ", result.toString());
                                    int statusCode = result.getInt("status_code");
                                    runOnUiThread(() -> {
                                        Toast.makeText(ChooseLoginServerActivity.this,result.getString("message"),Toast.LENGTH_SHORT).show();
                                        if(statusCode==0)
                                        updateButtonState();
                                    });
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Timber.e("fetch service login wx failed, error code %d", response.code());
                            }
                        }
                    });
                }


                @Override
                public void onFail(String message) {

                }
            });

        }


        @Override
        public void onError(SHARE_MEDIA share_media, int i, Throwable throwable) {
//            ToastUtils.show("授权失败");
        }


        @Override
        public void onCancel(SHARE_MEDIA share_media, int i) {

        }
    };


    private void onLoginSuccessfully(JSONObject data) {
        String anki_username = data.getString("anki_username");
        String anki_password = data.getString("anki_password");
        JSONObject meta = data.getJSONObject("meta");
        String newToken = meta.getString("token");
        String expired_at = meta.getString("expired_at");
        boolean isVip = data.getJSONObject("vip_info").getBoolean("is_vip");
        String vipUrl = data.getJSONObject("vip_info").getString("vip_url");
        saveToken(newToken, expired_at);
        saveANKIUserInfo(anki_username, anki_password, isVip, vipUrl);
        login();
//        updateButtonState();
    }


    public void onLoginAnkiChinaButtonClick(View view) {
        if (view.isSelected()) {
            MyAccount account = new MyAccount();
            account.logout(this);
            onResume();
            return;
        }
        Intent myAccount = new Intent(this, MyAccount.class);
        myAccount.putExtra("notLoggedIn", !view.isSelected());
        startActivityForResultWithAnimation(myAccount,REQUEST_CODE_LOGIN_ANKI_WEB, ActivityTransitionAnimation.FADE);
    }


    public static final int REQUEST_CODE_LOGIN_ANKI_WEB=1002;
    public void onLoginAnkiWebButtonClick(View view) {
        if (view.isSelected()) {
            MyAccount2 account = new MyAccount2();
            account.logout(this);
            onResume();
            return;
        }
        Intent myAccount = new Intent(this, MyAccount2.class);
        myAccount.putExtra("notLoggedIn", !view.isSelected());
        startActivityForResultWithAnimation(myAccount,REQUEST_CODE_LOGIN_ANKI_WEB, ActivityTransitionAnimation.FADE);

    }


    public void quit(View view) {
        //退出登录
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        String chinaAccount = preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "");
        String ankiAccount = preferences.getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "");
        boolean isLoginAnkiChina = !chinaAccount.isEmpty();
        boolean isLoginAnkiWeb = !ankiAccount.isEmpty();
        //不应存在仅登录国外服务器的情况下进入该账号管理页面的情况，所以只做以下两种判断
        if (isLoginAnkiChina && isLoginAnkiWeb) {
            //登录了两个账号，选择退出的账号
            final Dialog dialog = new Dialog(this, R.style.DialogTheme2);
            dialog.setContentView(R.layout.dialog_switch_server);
            Window dialogWindow = dialog.getWindow();
            dialogWindow.setGravity(Gravity.CENTER);
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
            ((TextView) dialog.findViewById(R.id.quit_china_server)).setText("退出");
            ((TextView) dialog.findViewById(R.id.quit_over_sea_server)).setText("退出");
            ((TextView) dialog.findViewById(R.id.china_server_account)).setText(chinaAccount);
            ((TextView) dialog.findViewById(R.id.over_sea_server_account)).setText(ankiAccount);
            dialog.findViewById(R.id.rl_over_sea_server).setOnClickListener(view2 -> {
                new MaterialDialog.Builder(this)
                        .title("退出登录")
                        .iconAttr(R.attr.dialogErrorIcon)
                        .content("是否确认退出登录")
                        .positiveText("确认")
                        .negativeText("取消")
                        .onPositive((dialog2, which) -> {
                            dialog2.dismiss();
                            dialog.dismiss();
                            MyAccount2 account = new MyAccount2();
                            account.logout(this);
                        })
                        .build().show();
            });
            dialog.findViewById(R.id.rl_china_server).setOnClickListener(view2 -> {
                new MaterialDialog.Builder(this)
                        .title("退出登录")
                        .iconAttr(R.attr.dialogErrorIcon)
                        .content("是否确认退出登录")
                        .positiveText("确认")
                        .negativeText("取消")
                        .onPositive((dialog2, which) -> {
                            dialog2.dismiss();
                            dialog.dismiss();
                            MyAccount account = new MyAccount();
                            account.logout(this);
                            finishWithoutAnimation();
                        })
                        .build().show();
            });
            dialog.show();
        } else if (isLoginAnkiChina) {
            //仅登录国内服务器，直接退出账号
            new MaterialDialog.Builder(this)
                    .title("退出登录")
                    .iconAttr(R.attr.dialogErrorIcon)
                    .content("是否确认退出登录")
                    .positiveText("确认")
                    .negativeText("取消")
                    .onPositive((dialog, which) -> {
                        dialog.dismiss();
                        MyAccount account = new MyAccount();
                        account.logout(this);
                        finishWithoutAnimation();
                    })
                    .build().show();

        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        Intent i = getIntent();
        if (i.hasExtra("notLoggedIn") && i.getExtras().getBoolean("notLoggedIn", false)) {
            setResult(RESULT_OK, i);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==REQUEST_CODE_LOGIN_ANKI_WEB&&resultCode==RESULT_OK){
            finishWithoutAnimation();
        }
        UMShareAPI.get(this).onActivityResult(requestCode, resultCode, data);
    }
}
