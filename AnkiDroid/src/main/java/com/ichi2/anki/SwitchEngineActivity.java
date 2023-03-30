package com.ichi2.anki;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.libanki.Consts;
import com.ichi2.utils.OKHttpUtil;

import java.io.IOException;

import androidx.annotation.IdRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import okhttp3.Call;
import okhttp3.Response;
import timber.log.Timber;

import static com.ichi2.anki.DeckPicker.REFRESH_VOICE_INFO;
import static com.ichi2.libanki.Consts.KEY_REST_ONLINE_SPEAK_COUNT;
import static com.ichi2.libanki.Consts.KEY_SELECT_ONLINE_SPEAK_ENGINE;


public class SwitchEngineActivity extends AnkiActivity implements View.OnClickListener {
    private TextView /*tx_online_count,*/ tx_select_online, tx_select_offline/*, buy*/;


    public static void OpenSwitchEngineActivity(Context context) {
        Intent intent = new Intent(context, SwitchEngineActivity.class);
        context.startActivity(intent);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speak_engine);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setTitle("");

            int[] attrs = new int[] {
                    R.attr.reviewStatusBarColor,
                    R.attr.primaryTextColor222222,
            };
            TypedArray ta = obtainStyledAttributes(attrs);
            toolbar.setBackground(ta.getDrawable(0));
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText("朗读设置");
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setTextColor(ta.getColor(1, ContextCompat.getColor(this, R.color.black)));
            // Decide which action to take when the navigation button is tapped.
//            toolbar.setNavigationOnClickListener(v -> onNavigationPressed());
        }
//        tx_online_count = findViewById(R.id.online_count);
//        tx_online_count.setText("剩余在线朗读次数：" + AnkiDroidApp.getSharedPrefs(this).getInt(KEY_REST_ONLINE_SPEAK_COUNT, 0));
        tx_select_online = findViewById(R.id.select_online);
        tx_select_offline = findViewById(R.id.select_offline);
        findViewById2(R.id.rl_online_engine);
        findViewById2(R.id.rl_offline_engine);
        findViewById2(R.id.buy);
        setTitle("选择引擎");
        if (AnkiDroidApp.getSharedPrefs(this).getBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false)) {
            tx_select_online.setVisibility(View.VISIBLE);
            tx_select_offline.setVisibility(View.GONE);
        } else {
            tx_select_online.setVisibility(View.GONE);
            tx_select_offline.setVisibility(View.VISIBLE);
        }
//        handler = new Handler();

    }


//    private Handler handler;


    @Override
    protected void onResume() {
        super.onResume();
//        getAccount().getToken(this, new MyAccount.TokenCallback() {
//            @Override
//            public void onSuccess(String token) {
//                OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "users/voiceInfo", token, "", new OKHttpUtil.MyCallBack() {
//                    @Override
//                    public void onFailure(Call call, IOException e) {
//
//                    }
//
//
//                    @Override
//                    public void onResponse(Call call, String token, Object arg1, Response response) {
//                        if (response.isSuccessful()) {
//                            try {
//                                final org.json.JSONObject object = new org.json.JSONObject(response.body().string());
//                                final org.json.JSONObject item = object.getJSONObject("data");
//                                Timber.e("init voice info success: %s", item.toString());
//
//                                mBuyOnlineEngineUrl = item.getString("buy_url");
//                                String num = item.getString("total");
//                                handler.post(() -> tx_online_count.setText("剩余在线朗读次数：" + num));
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                            }
//                        } else {
//                            Timber.e("init voice info failed, error code %d", response.code());
//                        }
//
//
//                    }
//                });
//            }
//
//
//            @Override
//            public void onFail(String message) {
//                Timber.e("need login while using online speak engine ");
////                Toast.makeText(SwitchEngineActivity.this, "当前未使用Anki记忆卡账号登录，无法使用在线语音引擎", Toast.LENGTH_SHORT).show();
////                Intent myAccount = new Intent(SwitchEngineActivity.this, MyAccount.class);
////                myAccount.putExtra("notLoggedIn", true);
////                startActivityForResultWithAnimation(myAccount, REFRESH_VOICE_INFO, ActivityTransitionAnimation.FADE);
//            }
//        });
    }


    private String mBuyOnlineEngineUrl;


    public <T extends View> T findViewById2(@IdRes int id) {
        T view = findViewById(id);
        view.setOnClickListener(this);
        return view;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.rl_offline_engine) {
            tx_select_online.setVisibility(View.GONE);
            tx_select_offline.setVisibility(View.VISIBLE);
            AnkiDroidApp.getSharedPrefs(SwitchEngineActivity.this).edit().putBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, false).apply();
        } else {
            getAccount().getToken(this, new MyAccount.TokenCallback() {
                @Override
                public void onSuccess(String token) {
                    switch (id) {
//                        case R.id.buy:
//                            if (mBuyOnlineEngineUrl == null || mBuyOnlineEngineUrl.isEmpty()) {
//                                Toast.makeText(SwitchEngineActivity.this, "正在获取充值地址，请稍候", Toast.LENGTH_SHORT).show();
//                            } else {
//                                getAccount().getToken(SwitchEngineActivity.this, new MyAccount.TokenCallback() {
//                                    @Override
//                                    public void onSuccess(String token) {
//                                        WebViewActivity.openUrlInApp(SwitchEngineActivity.this, String.format(mBuyOnlineEngineUrl, token, BuildConfig.VERSION_NAME), token, REFRESH_VOICE_INFO);
//                                    }
//
//
//                                    @Override
//                                    public void onFail(String message) {
//                                        Timber.e("need login while using online speak engine ");
//                                        Toast.makeText(SwitchEngineActivity.this, "当前未使用Anki记忆卡账号登录，无法使用在线语音引擎", Toast.LENGTH_SHORT).show();
//                                        Intent myAccount = new Intent(SwitchEngineActivity.this, MyAccount.class);
//                                        myAccount.putExtra("notLoggedIn", true);
//                                        startActivityForResultWithAnimation(myAccount, REFRESH_VOICE_INFO, ActivityTransitionAnimation.FADE);
//                                    }
//                                });
//                            }
//                            break;
                        case R.id.rl_online_engine:
                            tx_select_online.setVisibility(View.VISIBLE);
                            tx_select_offline.setVisibility(View.GONE);
                            AnkiDroidApp.getSharedPrefs(SwitchEngineActivity.this).edit().putBoolean(KEY_SELECT_ONLINE_SPEAK_ENGINE, true).apply();
                            break;
                    }
                }


                @Override
                public void onFail(String message) {
                    Timber.e("need login while using online speak engine ");
                    Toast.makeText(SwitchEngineActivity.this, "当前未使用Anki记忆卡账号登录，无法使用在线语音引擎", Toast.LENGTH_SHORT).show();
                    Intent myAccount = new Intent(SwitchEngineActivity.this, ChooseLoginServerActivity.class);
                    myAccount.putExtra("notLoggedIn", true);
                    startActivityForResultWithAnimation(myAccount, REFRESH_VOICE_INFO, ActivityTransitionAnimation.FADE);
                }
            });
        }


    }


}
