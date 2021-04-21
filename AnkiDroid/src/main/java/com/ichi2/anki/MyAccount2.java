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

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.textfield.TextInputLayout;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.web.HostNumFactory;
import com.ichi2.async.Connection;
import com.ichi2.async.Connection.Payload;
import com.ichi2.libanki.Consts;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.ui.TextInputEditField;
import com.ichi2.utils.AdaptionUtil;

import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;

public class MyAccount2 extends AnkiActivity {
    private final static int STATE_LOG_IN = 1;
    private final static int STATE_LOGGED_IN = 2;

    private View mLoginToMyAccountView;
    private View mLoggedIntoMyAccountView;

    private EditText mUsername;
    private TextInputEditField mPassword;

    private TextView mUsernameLoggedIn;

    private MaterialDialog mProgressDialog;
    Toolbar mToolbar = null;
    private TextInputLayout mPasswordLayout;


    private void switchToState(int newState) {
        switch (newState) {
            case STATE_LOGGED_IN:
                String username = AnkiDroidApp.getSharedPrefs(getBaseContext()).getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "");
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AdaptionUtil.isUserATestClient()) {
            finishWithoutAnimation();
            return;
        }

        mayOpenUrl(Uri.parse(getResources().getString(R.string.register_url)));
        initAllContentViews();

        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        if (preferences.getString(Consts.KEY_SAVED_ANKI_WEB_HKEY, "").length() > 0) {
            switchToState(STATE_LOGGED_IN);
        } else {
            switchToState(STATE_LOG_IN);
        }
    }


    public void attemptLogin() {
        String username = mUsername.getText().toString().trim(); // trim spaces, issue 1586
        String password = mPassword.getText().toString();
        Consts.saveAndUpdateLoginServer(Consts.LOGIN_SERVER_ANKIWEB);
        if (!"".equalsIgnoreCase(username) && !"".equalsIgnoreCase(password)) {
            Timber.i("Attempting auto-login");
            Connection.login(loginListener, new Payload(new Object[] {username, password,
                    HostNumFactory.getInstance(this)}));
        } else {
            Timber.i("Auto-login cancelled - username/password missing");
        }
    }


    private void saveUserInformation(String username, String hkey) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        Editor editor = preferences.edit();
        editor.putString("username", mUsername.getText().toString().trim());
        editor.putString("hkey", hkey);
        editor.putString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, mUsername.getText().toString().trim());
        editor.putString(Consts.KEY_SAVED_ANKI_WEB_HKEY, hkey);
        editor.putInt(Consts.KEY_ANKI_ACCOUNT_SERVER, Consts.LOGIN_SERVER_ANKIWEB);
        Consts.LOGIN_SERVER=Consts.LOGIN_SERVER_ANKIWEB;
        editor.apply();
    }


    private void login() {
        // Hide soft keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mUsername.getWindowToken(), 0);

        String username = mUsername.getText().toString().trim(); // trim spaces, issue 1586
        String password = mPassword.getText().toString();
        Consts.saveAndUpdateLoginServer(Consts.LOGIN_SERVER_ANKIWEB);
        if (!"".equalsIgnoreCase(username) && !"".equalsIgnoreCase(password)) {
            Connection.login(loginListener, new Payload(new Object[] {username, password,
                    HostNumFactory.getInstance(this)}));
        } else {
            UIUtils.showSimpleSnackbar(this, R.string.invalid_username_password, true);
        }
    }


    public void logout(Context context) {
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(context);
        Editor editor = preferences.edit();
        editor.putString("username", preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE,""));//优先切换为另一个账号的登陆状态
        editor.putString("hkey", preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_HKEY,""));
        editor.putString("token", preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_TOKEN,""));

        editor.putString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "");
        editor.putString(Consts.KEY_SAVED_ANKI_WEB_HKEY, "");

        Consts.LOGIN_SERVER=preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE,"").isEmpty()?Consts.LOGIN_SERVER_NOT_LOGIN:Consts.LOGIN_SERVER_ANKICHINA;
        editor.putInt(Consts.KEY_ANKI_ACCOUNT_SERVER, Consts.LOGIN_SERVER);
        editor.apply();
        HostNumFactory.getInstance(context).reset();
        //  force media resync on deauth
        getCol().getMedia().forceResync();
        if (context == this) {
            switchToState(STATE_LOG_IN);
        }
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


    private void initAllContentViews() {
        mLoginToMyAccountView = getLayoutInflater().inflate(R.layout.my_account2, null);
        mUsername = mLoginToMyAccountView.findViewById(R.id.username);
        mPassword = mLoginToMyAccountView.findViewById(R.id.password);
        mPasswordLayout = mLoginToMyAccountView.findViewById(R.id.password_layout);

        mPassword.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                    case KeyEvent.KEYCODE_NUMPAD_ENTER:
                        login();
                        return true;
                    default:
                        break;
                }
            }
            return false;
        });

        Button loginButton = mLoginToMyAccountView.findViewById(R.id.login_button);
        loginButton.setOnClickListener(v -> login());

        Button resetPWButton = mLoginToMyAccountView.findViewById(R.id.reset_password_button);
        resetPWButton.setOnClickListener(v -> resetPassword());

        Button signUpButton = mLoginToMyAccountView.findViewById(R.id.sign_up_button);
        Uri url = Uri.parse(getResources().getString(R.string.register_url));
        signUpButton.setOnClickListener(v -> openUrl(url));

        mLoggedIntoMyAccountView = getLayoutInflater().inflate(R.layout.my_account_logged_in, null);
        mUsernameLoggedIn = mLoggedIntoMyAccountView.findViewById(R.id.username_logged_in);
        Button logoutButton = mLoggedIntoMyAccountView.findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> logout(getBaseContext()));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mPassword.setAutoFillListener((value) -> {
                //disable "show password".
                mPasswordLayout.setEndIconVisible(false);
                Timber.i("Attempting login from autofill");
                attemptLogin();
            });
        }
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
//            MyAccount myAccount = new MyAccount();
//            myAccount.logout(MyAccount2.this);//先登出
            if (mProgressDialog == null || !mProgressDialog.isShowing()) {
                mProgressDialog = StyledProgressDialog.show(MyAccount2.this, "",
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

                Intent i = MyAccount2.this.getIntent();
                if (i.hasExtra("notLoggedIn") && i.getExtras().getBoolean("notLoggedIn", false)) {
                    MyAccount2.this.setResult(RESULT_OK, i);
                    finishWithAnimation(ActivityTransitionAnimation.FADE);
                } else {
                    // Show logged view
                    mUsernameLoggedIn.setText((String) data.data[0]);
                    switchToState(STATE_LOGGED_IN);
                }
            } else {
                Timber.e("Login failed, error code %d", data.returnType);
                Consts.resumeLoginServer();
                if (data.returnType == 403) {
                    UIUtils.showSimpleSnackbar(MyAccount2.this, R.string.invalid_username_password, true);
                } else {
                    String message = getResources().getString(R.string.connection_error_message);
                    Object[] result = (Object[]) data.result;
                    if (result.length > 1 && result[1] instanceof Exception) {
                        showSimpleMessageDialog(message, ((Exception) result[1]).getLocalizedMessage(), false);
                    } else {
                        UIUtils.showSimpleSnackbar(MyAccount2.this, message, false);
                    }
                }

            }
        }


        @Override
        public void onDisconnected() {
            Consts.resumeLoginServer();
            UIUtils.showSimpleSnackbar(MyAccount2.this, R.string.youre_offline, true);
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

}
