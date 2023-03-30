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

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.async.Connection;
import com.ichi2.async.Connection.Payload;
import com.ichi2.libanki.AreaCode;
import com.ichi2.libanki.Consts;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.Permissions;
import com.ichi2.utils.FormatCheckUtils;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static com.ichi2.libanki.AreaCode.AREA_CODE_JSON;

public class MyAccount extends AnkiActivity {
    private final static int STATE_LOG_IN = 1;
    private final static int STATE_LOGGED_IN = 2;

    private View mLoginToMyAccountView;
    private View mLoggedIntoMyAccountView;

    private EditText mPhoneNum;
    private EditText mAuthCode;

    private TextView mUsernameLoggedIn, mAreaCode;
    private TextView mSendAuthCode;

    private MaterialDialog mProgressDialog;
    Toolbar mToolbar = null;
    private TextInputLayout mAuthCodeLayout;


    private void switchToState(int newState) {
        switch (newState) {
            case STATE_LOGGED_IN:
                String username = AnkiDroidApp.getSharedPrefs(getBaseContext()).getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "");
                mUsernameLoggedIn.setText(username);
                mToolbar = mLoggedIntoMyAccountView.findViewById(R.id.toolbar);
                if (mToolbar != null) {
                    mToolbar.setTitle(getString(R.string.sync_account));  // This can be cleaned up if all three main layouts are guaranteed to share the same toolbar object
                    setSupportActionBar(mToolbar);
                }
                setContentView(mLoggedIntoMyAccountView);
                break;

            case STATE_LOG_IN:
                mToolbar = mLoginToMyAccountView.findViewById(R.id.toolbar);
                if (mToolbar != null) {
                    mToolbar.setTitle(getString(R.string.sync_account));  // This can be cleaned up if all three main layouts are guaranteed to share the same toolbar object
                    setSupportActionBar(mToolbar);
                }
                setContentView(mLoginToMyAccountView);
                break;
        }


        supportInvalidateOptionsMenu();  // Needed?
    }


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    String mBindKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AdaptionUtil.isUserATestClient()) {
            finishWithoutAnimation();
            return;
        }

        mayOpenUrl(Uri.parse(getResources().getString(R.string.register_url)));
        initAllContentViews();

        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        if (preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_HKEY, "").length() > 0) {
            switchToState(STATE_LOGGED_IN);
        } else {
            switchToState(STATE_LOG_IN);
        }
        Bundle bundle=getIntent().getExtras();
        if(bundle!=null){
            mBindKey = bundle.getString("bind_key");
            TextView titleText = ((TextView) findViewById(R.id.title_text));
            if (mBindKey != null && !mBindKey.isEmpty() && titleText != null) {
                titleText.setText("请绑定手机号");
            }
        }

    }


//    public void attemptLogin() {
//        String username = mPhoneNum.getText().toString().trim(); // trim spaces, issue 1586
//        String password = mAuthCode.getText().toString();
//
//        if (!"".equalsIgnoreCase(username) && !"".equalsIgnoreCase(password)) {
//            Timber.i("Attempting auto-login");
//            Connection.login(loginListener, new Connection.Payload(new Object[] {username, password,
//                    HostNumFactory.getInstance(this)}));
//        } else {
//            Timber.i("Auto-login cancelled - username/password missing");
//        }
//    }


    private void saveUserInformation(String username, String hkey) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        editor.putString("username", username);
        editor.putString("hkey", hkey);
        editor.putString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, username);
        editor.putString(Consts.KEY_SAVED_ANKI_CHINA_HKEY, hkey);
        editor.putInt(Consts.KEY_ANKI_ACCOUNT_SERVER, Consts.LOGIN_SERVER_ANKICHINA);
        Consts.LOGIN_SERVER = Consts.LOGIN_SERVER_ANKICHINA;
        editor.apply();
    }


    String cacheKey = "";
    String cacheToken = "";


    private void saveAuthKey(String key, String expired_at) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        cacheKey = key;
        editor.putString("key", key);
        editor.putString("key_expired_at", expired_at);
        editor.apply();
    }


    protected void saveToken(String token, String expired_at) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        cacheToken = token;
        editor.putString("token", token);
        editor.putString(Consts.KEY_SAVED_ANKI_CHINA_TOKEN, token);
        editor.putString("token_expired_at", expired_at);
        editor.apply();
    }


    String anki_username;
    String anki_password;


    protected void saveANKIUserInfo(String user_name, String password, boolean vip, String vipUrl) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        anki_username = user_name;
        anki_password = password;

        editor.putString("anki_username", user_name).putString("anki_password", password).putBoolean(Consts.KEY_IS_VIP, vip).putString(Consts.KEY_VIP_URL, vipUrl).apply();
    }


    public static boolean loginAnkiChina(Context context) {
        return AnkiDroidApp.getSharedPrefs(context).getBoolean("login_ankichina", false);
    }


    private String[] getANKIUserInfo() {
        if (!anki_username.isEmpty() && !anki_password.isEmpty()) {
            return new String[] {anki_username, anki_password};
        }
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        anki_username = preferences.getString("anki_username", "");
        anki_username = preferences.getString("anki_password", "");
        return new String[] {anki_username, anki_password};
    }


    private String getAuthKey() {
        //获取auth key
        if (!cacheKey.isEmpty()) {
            return cacheKey;
        }
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        cacheKey = preferences.getString("key", "");
        return cacheKey;
    }


    public static final String TOKEN_IS_EXPIRED = "TOKEN_IS_EXPIRED";
    public static final String NO_TOKEN_RECORD = "NO_TOKEN_RECORD";
    public static final String NO_WRITEABLE_PERMISSION = "NO_WRITEABLE_PERMISSION";
    public static final String NOT_LOGIN_ANKI_CHINA = "NOT_LOGIN_ANKI_CHINA";



    interface TokenCallback {
        void onSuccess(String token);

        void onFail(String message);
    }


    public void getToken(Context context, TokenCallback callback) {
        //获取auth key

//        if (CollectionHelper.getInstance().getColSafe(context) == null) {
//            callback.onFail(NO_WRITEABLE_PERMISSION);
//            return;
//        }
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);
        if (!Consts.savedAnkiChinaAccount(preferences)) {
            callback.onFail(NOT_LOGIN_ANKI_CHINA);
            return;
        }
        cacheToken = preferences.getString("token", "");
        String expired = preferences.getString("token_expired_at", "");
//        Timber.i("this token will expired:"+expired+",and now is :"+Calendar.getInstance().getTime().toString());
        if (expired == null || expired.isEmpty() || cacheToken == null || cacheToken.isEmpty()) {
            callback.onFail(NO_TOKEN_RECORD);
            return;
        }
        if (!Permissions.hasStorageAccessPermission(context)) {
            callback.onSuccess(cacheToken);//没存储权限，直接返回记录的token
            return;
        }
//        long now=Calendar.getInstance().getTimeInMillis();
        long now = getCol(context).getTime().intTimeMS();
//        Timber.i("compare time :"+Calendar.getInstance().getTimeInMillis()+","+now);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = null;
        try {
            date = sdf.parse(expired);
        } catch (ParseException e) {
            e.printStackTrace();
            callback.onFail(TOKEN_IS_EXPIRED);
//            showLoginDialog();
            return;
        }
        Calendar calendar = getCol(context).getTime().calendar();
        calendar.setTime(date);
//        Timber.i("this token will expired(timestamp):"+calendar.getTimeInMillis()+",and now is :"+now);
        if (calendar.getTimeInMillis() < now) {
            callback.onFail(TOKEN_IS_EXPIRED);
//            showLoginDialog();
            return;
        }
        callback.onSuccess(cacheToken);
    }


//    void onTokenExpired() {
//        AsyncDialogFragment newFragment = SyncErrorDialog.newInstance(SyncErrorDialog.DIALOG_USER_NOT_LOGGED_IN_SYNC, "");
//        showAsyncDialogFragment(newFragment, NotificationChannels.Channel.SYNC);
//    }


    private void preLogin() {
        // Hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mPhoneNum.getWindowToken(), 0);

        String phone = mPhoneNum.getText().toString().trim();
        String area = mAreaCode.getText().toString().replace("+","").trim();

        String authCode = mAuthCode.getText().toString();

        if (FormatCheckUtils.isChinaPhoneLegal(phone) && !"".equalsIgnoreCase(authCode) && authCode.length() == 6) {
            try {
                org.json.JSONObject jo = new org.json.JSONObject();
                jo.put("phone", phone);
                jo.put("area", area);
                jo.put("code", authCode);
                jo.put("key", getAuthKey());
                jo.put("bind_key", mBindKey);
                Timber.d("我最后的参数是:"+jo.toString());
                Consts.saveAndUpdateLoginServer(Consts.LOGIN_SERVER_ANKICHINA);
                Connection.sendCommonPost(preLoginListener, new Connection.Payload("authorizations", jo.toString(), Payload.REST_TYPE_POST, HostNumFactory.getInstance(this)));
            } catch (Exception e) {
                UIUtils.showSimpleSnackbar(this, R.string.invalid_account, true);

            }
        } else {
            UIUtils.showSimpleSnackbar(this, R.string.invalid_phone, true);
        }
    }


    protected void login() {
        // Hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mPhoneNum.getWindowToken(), 0);
        String[] info = getANKIUserInfo();
        String username = info[0]; // trim spaces, issue 1586
        String password = info[1];
        if (!"".equalsIgnoreCase(username) && !"".equalsIgnoreCase(password)) {
//            Consts.LOGIN_SERVER = Consts.LOGIN_SERVER_ANKICHINA;
            Consts.saveAndUpdateLoginServer(Consts.LOGIN_SERVER_ANKICHINA);
            Connection.login(loginListener, new Connection.Payload(new Object[] {username, password,
                    HostNumFactory.getInstance(this)}));
        } else {
            UIUtils.showSimpleSnackbar(this, R.string.invalid_phone, true);
        }
    }


    Handler authCodeTimer = new Handler();
    int timerCount = 60;
    boolean continueTimer;


    void startAuthCodeTimer() {
        if (continueTimer) {
            return;
        }
        continueTimer = true;
        timerCount = 60;
        authCodeTimer.removeCallbacksAndMessages(null);
        authCodeTimer.postDelayed(new Runnable() {
            @Override
            public void run() {
                timerCount--;

                authCodeTimer.removeCallbacksAndMessages(null);
                if (continueTimer && timerCount > 0) {
                    authCodeTimer.postDelayed(this, 1000);
                    mSendAuthCode.setText(String.format("%s%s", timerCount, getString(R.string.count_hint)));
                } else if (timerCount == 0) {
                    continueTimer = false;
                    mSendAuthCode.setText(getString(R.string.re_get_auth_hint));
                } else {
                    mSendAuthCode.setText(getString(R.string.auth_hint));
                }
            }
        }, 1000);
    }


    void stopAuthCodeTimer() {
        continueTimer = false;
        authCodeTimer.removeCallbacksAndMessages(null);
        if (mSendAuthCode != null) {
            mSendAuthCode.setText(getString(R.string.auth_hint));
        }
        timerCount = 60;
    }


    private void sendAuthCode() {
        // Hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mPhoneNum.getWindowToken(), 0);
        if (continueTimer) {
            return;
        }
        startAuthCodeTimer();
        String phone = mPhoneNum.getText().toString().trim();
        String area = mAreaCode.getText().toString().replace("+","").trim();

        if (!"".equalsIgnoreCase(phone)) {
            try {
                org.json.JSONObject jo = new org.json.JSONObject();
                jo.put("phone", phone);
                jo.put("area", area);
                Connection.sendCommonPost(sendAuthCodeListener, new Connection.Payload("verification-codes", jo.toString(), Payload.REST_TYPE_POST, HostNumFactory.getInstance(this)));
            } catch (Exception e) {
                UIUtils.showSimpleSnackbar(this, R.string.invalid_username_password, true);

            }
        } else {
            UIUtils.showSimpleSnackbar(this, R.string.invalid_username_password, true);
        }
    }


    Dialog mAreaCodeDialog;


    private void showAreaCodeList() {
        if (mAreaCodeDialog == null) {
            mAreaCodeDialog = new Dialog(this, R.style.DialogTheme2);

            //2、设置布局
            View view = View.inflate(this, R.layout.dialog_choose_area_code, null);
            mAreaCodeDialog.setContentView(view);

            Window window = mAreaCodeDialog.getWindow();
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mAreaCodeDialog.findViewById(R.id.area_close).setOnClickListener(view1 -> {
                mAreaCodeDialog.dismiss();
            });
            ListView listView = mAreaCodeDialog.findViewById(R.id.area_list);
            List<AreaCode> areaCodes = new ArrayList<>();
            JSONArray array = new JSONArray(AREA_CODE_JSON);
            for (int i = 0; i < array.length(); i++) {
                AreaCode item = new AreaCode();
                item.setAreaName(array.getJSONObject(i).getString("name"));
                item.setAreaCode(array.getJSONObject(i).getString("code"));
                areaCodes.add(item);
            }
            AreaAdapter adapter = new AreaAdapter(this, R.layout.item_area_code, areaCodes);

            listView.setAdapter(adapter);
            //5、将适配器加载到控件中
            listView.setAdapter(adapter);
            //6、为列表中选中的项添加单击响应事件
            listView.setOnItemClickListener((parent, view12, i, l) -> {
//                Toast.makeText(MyAccount.this, "您选择的地区是：" + areaCodes.get(i).getAreaName(), Toast.LENGTH_LONG).show();
                mAreaCode.setText("+"+areaCodes.get(i).getAreaCode());
                mAreaCodeDialog.dismiss();
            });
        }

        if (mAreaCodeDialog.isShowing()) {
            mAreaCodeDialog.dismiss();
            return;
        }
        mAreaCodeDialog.show();
    }


    public void logout(Context context) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);
        Editor editor = preferences.edit();
        editor.putString("username", preferences.getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, ""));
        editor.putString("hkey", preferences.getString(Consts.KEY_SAVED_ANKI_WEB_HKEY, ""));
        editor.putString("token", "");

        editor.putString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "");
        editor.putString(Consts.KEY_SAVED_ANKI_CHINA_HKEY, "");
        editor.putString(Consts.KEY_SAVED_ANKI_CHINA_TOKEN, "");

        editor.remove(Consts.KEY_SYNC_CHINA_SESSION);//退出登录ankichina后需要清楚同步session，同时清空synclog
        try {
            getCol().getDb().execute("delete from synclog");
        } catch (Exception e) {
            e.printStackTrace();
//            getCol().getDb().execute("drop table synclog");
//            getCol().getDb().execute("create table if not exists synclog (" + "    id             integer not null,"
//                    + "    type             integer not null," + "    mod             integer not null" + ")");
        }

        Consts.LOGIN_SERVER = preferences.getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "").isEmpty() ? Consts.LOGIN_SERVER_NOT_LOGIN : Consts.LOGIN_SERVER_ANKIWEB;
        editor.putInt(Consts.KEY_ANKI_ACCOUNT_SERVER, Consts.LOGIN_SERVER);

        editor.apply();
        HostNumFactory.getInstance(context).reset();
        //  force media resync on deauth
        if (Permissions.hasStorageAccessPermission(context)) {
            getCol(context).getMedia().forceResync();
        }
        if (context == this) {
            switchToState(STATE_LOG_IN);
        }
        MobclickAgent.onProfileSignOff();
    }


    public void onQuit(View view) {
        onBackPressed();
    }


    private void resetPassword() {
        if (AdaptionUtil.hasWebBrowser(this)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(getResources().getString(R.string.resetpw_url)));
            startActivityWithoutAnimation(intent);
        } else {
            UIUtils.showThemedToast(this, getResources().getString(R.string.no_browser_notification) + getResources().getString(R.string.resetpw_url), false);
        }
    }


    public void loginWithAnkiWeb(View view) {
        Intent myAccount = new Intent(this, MyAccount2.class);
        myAccount.putExtra("notLoggedIn", true);
        startActivityWithAnimation(myAccount, ActivityTransitionAnimation.FADE);
        finishWithoutAnimation();
    }


    Button mLoginButton;


    private void initAllContentViews() {
        mLoginToMyAccountView = getLayoutInflater().inflate(R.layout.my_account, null);
        mPhoneNum = mLoginToMyAccountView.findViewById(R.id.username);
        mPhoneNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSendAuthCode.setEnabled(mAreaCode.getText().toString().replace("+", "").trim() == "86" ?
                        FormatCheckUtils.isChinaPhoneLegal(s.toString()) : s.toString().length() > 5);
            }


            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mAuthCode = mLoginToMyAccountView.findViewById(R.id.auth_code);
        mAuthCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mLoginButton.setEnabled(FormatCheckUtils.isChinaPhoneLegal(mPhoneNum.getText().toString()) && s.length() == 6);
            }


            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mAreaCode = mLoginToMyAccountView.findViewById(R.id.area_code);
        mAreaCode.setOnClickListener(v -> showAreaCodeList());
        mSendAuthCode = mLoginToMyAccountView.findViewById(R.id.send_auth_code);
        mSendAuthCode.setOnClickListener(v -> sendAuthCode());
        mLoginButton = mLoginToMyAccountView.findViewById(R.id.login_button);
        mLoginButton.setOnClickListener(v -> preLogin());
//        loginButton.setOnClickListener(v -> login());

//        Button resetPWButton = mLoginToMyAccountView.findViewById(R.id.reset_password_button);
//        resetPWButton.setOnClickListener(v -> resetPassword());

//        Button signUpButton = mLoginToMyAccountView.findViewById(R.id.sign_up_button);
//        signUpButton.setOnClickListener(v -> openUrl(Uri.parse(getResources().getString(R.string.register_url))));

        mLoggedIntoMyAccountView = getLayoutInflater().inflate(R.layout.my_account_logged_in, null);
        mUsernameLoggedIn = mLoggedIntoMyAccountView.findViewById(R.id.username_logged_in);

        mLoggedIntoMyAccountView.findViewById(R.id.logout_button).setOnClickListener(v -> logout(this));

    }


    /**
     * Listeners
     */
    Connection.TaskListener loginListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {
            Timber.d("loginListener.onPreExecute()");
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = StyledProgressDialog.show(MyAccount.this, "",
                        getResources().getString(R.string.alert_logging_message), false);
            }
        }


        @Override
        public void onPostExecute(Payload data) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (data.success) {
                Timber.i("User successfully logged in!");
                saveUserInformation((String) data.data[0], (String) data.data[1]);
                MobclickAgent.onProfileSignIn((String) data.data[0]);
                Intent i = MyAccount.this.getIntent();
                if (i.hasExtra("notLoggedIn") && i.getExtras().getBoolean("notLoggedIn", false)) {
                    MyAccount.this.setResult(RESULT_OK, i);
                    finishWithAnimation(ActivityTransitionAnimation.FADE);
                } else {
                    // Show logged view
                    mUsernameLoggedIn.setText((String) data.data[0]);
                    switchToState(STATE_LOGGED_IN);
                }

            } else {
                Consts.resumeLoginServer();
                Timber.e("Login failed, error code %d", data.returnType);
                if (data.returnType == 403) {
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.invalid_auth_code, true);
                } else {
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.connection_error_message, true);
                }
            }
        }


        @Override
        public void onDisconnected() {
            Consts.resumeLoginServer();
            UIUtils.showSimpleSnackbar(MyAccount.this, R.string.youre_offline, true);
        }
    };

    Connection.TaskListener preLoginListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {
            Timber.d("preLoginListener.onPreExecute()");
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = StyledProgressDialog.show(MyAccount.this, "",
                        getResources().getString(R.string.alert_logging_message), false);
            }
        }


        @Override
        public void onPostExecute(Payload data) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (data.success) {
                Timber.i("User successfully preLogged in!");
                try {
                    org.json.JSONObject result = ((org.json.JSONObject) data.result).getJSONObject("data");
                    String anki_username = result.getString("anki_username");
                    String anki_password = result.getString("anki_password");
//                    String anki_username = "zhangsan";
//                    String anki_password ="zhangsan";
                    org.json.JSONObject meta = result.getJSONObject("meta");
                    String token = meta.getString("token");
                    String expired_at = meta.getString("expired_at");
                    boolean isVip = result.getJSONObject("vip_info").getBoolean("is_vip");
                    String vipUrl = result.getJSONObject("vip_info").getString("vip_url");
                    saveToken(token, expired_at);
                    saveANKIUserInfo(anki_username, anki_password, isVip, vipUrl);
                    login();
                } catch (JSONException e) {
                    e.printStackTrace();
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.sync_generic_error, true);
                }
            } else {
                Consts.resumeLoginServer();
                Timber.e("preLogin failed, error code %d", data.statusCode);
                if (data.statusCode == 403) {
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.invalid_auth_code, true);
                } else {
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.connection_error_message, true);
                }
            }
        }


        @Override
        public void onDisconnected() {
            Consts.resumeLoginServer();
            UIUtils.showSimpleSnackbar(MyAccount.this, R.string.youre_offline, true);
        }
    };
    Connection.TaskListener sendAuthCodeListener = new Connection.TaskListener() {

        @Override
        public void onProgressUpdate(Object... values) {
            // Pass
        }


        @Override
        public void onPreExecute() {
            Timber.d("sendAuthCodeListener.onPreExecute()");
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = StyledProgressDialog.show(MyAccount.this, "",
                        getResources().getString(R.string.alert_sending_auth_code), false);
            }
        }


        @Override
        public void onPostExecute(Payload data) {
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
            }

            if (data.success) {
                Timber.i("send auth code successfully!");
                try {
                    org.json.JSONObject result = ((org.json.JSONObject) data.result).getJSONObject("data");
                    String key = result.getString("key");
                    String expired_at = result.getString("expired_at");
                    saveAuthKey(key, expired_at);
                } catch (JSONException e) {
                    e.printStackTrace();
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.sync_generic_error, true);
                }
            } else {
                Timber.e("send auth code failed, error code %d", data.statusCode);
                if (data.statusCode == 500) {
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.send_too_much_auth_code_one_day, true);
                } else {
                    UIUtils.showSimpleSnackbar(MyAccount.this, R.string.network_error, true);
                }
            }
        }


        @Override
        public void onDisconnected() {
            UIUtils.showSimpleSnackbar(MyAccount.this, R.string.youre_offline, true);
        }
    };


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Timber.i("MyAccount - onBackPressed()");
            finishWithAnimation(ActivityTransitionAnimation.FADE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAuthCodeTimer();
    }
}



class AreaAdapter extends ArrayAdapter<AreaCode> {
    public AreaAdapter(Context context, int resource, List<AreaCode> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AreaCode item = getItem(position);
        View view;
        ViewHolder viewHolder;
        if (convertView==null){
            view= LayoutInflater.from(getContext()).inflate(R.layout.item_area_code,parent,false);
            viewHolder= new ViewHolder();
            viewHolder.area = view.findViewById(R.id.area);
            viewHolder.code = view.findViewById(R.id.code);
            view.setTag(viewHolder);
        }else {
            view=convertView;
            viewHolder= (ViewHolder) view.getTag();
        }

        viewHolder.area.setText(item.getAreaName());
        viewHolder.code.setText(item.getAreaCode());
        return view;
    }

    private static class ViewHolder {
        TextView area;
        TextView code;
    }

}
