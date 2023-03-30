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


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.libanki.Consts;
import com.ichi2.themes.Themes;

public class ChooseLoginServerActivity2 extends AnkiActivity {
    private Button mBtnLoginAnki;
    private Button mBtnLoginChina;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.setThemeLegacy(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_login_server);
        mBtnLoginAnki = findViewById(R.id.login_button_anki);
        mBtnLoginChina = findViewById(R.id.login_button_phone);

    }

    public void onQuit(View view){
        onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = AnkiDroidApp.getSharedPrefs(getBaseContext());
        String usernameChina = preferences.getString(Consts.KEY_SAVED_ANKI_CHINA_PHONE, "");
        if ( TextUtils.isEmpty(usernameChina)) {
            mBtnLoginChina.setText("手机号登录");
            mBtnLoginChina.setSelected(false);
        }else{
            mBtnLoginChina.setText("退出手机号");
            mBtnLoginChina.setSelected(true);
        }
        String usernameWeb = preferences.getString(Consts.KEY_SAVED_ANKI_WEB_ACCOUNT, "");
        if ( TextUtils.isEmpty(usernameWeb)) {
            mBtnLoginAnki.setText("AnkiWeb登录");
            mBtnLoginAnki.setSelected(false);
        }else {
            mBtnLoginAnki.setText("退出AnkiWeb账号");
            mBtnLoginAnki.setSelected(true);
        }
    }


    public void onLoginAnkiWebButtonClick(View view){
        if(view.isSelected()){
            MyAccount2 account=new MyAccount2();
            account.logout(this);
            onResume();
            return;
        }
        Intent myAccount = new Intent(this, MyAccount2.class);
        myAccount.putExtra("notLoggedIn", !view.isSelected());
        startActivityWithAnimation(myAccount,  ActivityTransitionAnimation.FADE);

    }

    public void onLoginAnkiChinaButtonClick(View view){
        if(view.isSelected()){
            MyAccount account=new MyAccount();
            account.logout(this);
            onResume();
            return;
        }
        Intent myAccount = new Intent(this, MyAccount.class);
        myAccount.putExtra("notLoggedIn", !view.isSelected());
        startActivityWithAnimation(myAccount,  ActivityTransitionAnimation.FADE);
    }


    @Override
    protected void onPause() {
        super.onPause();
        Intent i =  getIntent();
        if (i.hasExtra("notLoggedIn") && i.getExtras().getBoolean("notLoggedIn", false)) {
            setResult(RESULT_OK, i);
        }
    }
}
