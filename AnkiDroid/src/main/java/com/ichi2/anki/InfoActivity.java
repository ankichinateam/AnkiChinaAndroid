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
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.ichi2.themes.Themes;

import androidx.appcompat.widget.Toolbar;
import timber.log.Timber;

import static com.ichi2.anim.ActivityTransitionAnimation.LEFT;

public class InfoActivity extends AnkiActivity {

    WebView webView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.setThemeLegacy(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
        getSupportActionBar().setTitle(getIntent().getStringExtra("url"));

        // Add a home button to the actionbar
//        getSupportActionBar().setHomeButtonEnabled(true);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        webView = findViewById(R.id.web_view);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
//        webView.getSettings().setAppCacheMaxSize(1024*1024*8);

        webView.getSettings().setAllowFileAccess(true);    // 可以读取文件缓存
        webView.getSettings().setAppCacheEnabled(true);    //开启H5(APPCache)缓存功能
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webView.getSettings().setAppCachePath(appCachePath);

        webView.setWebChromeClient(
                new WebChromeClient() {
//                    @Override
//                    public void onPageFinished(WebView view, String url) {
//                        super.onPageFinished(view, url);
//                        writeData(getIntent().getStringExtra("token"));
//                    }


                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        InfoActivity.this.getSupportActionBar().setTitle(title);
                    }

                }
        );
        webView.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                    }


                    @SuppressWarnings("deprecation")
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        if (urlCanLoad(url.toLowerCase())) {  // 加载正常网页
                            view.loadUrl(url);
                        } else {  // 打开第三方应用或者下载apk等
                            startThirdpartyApp(url);
                        }
                        return true;
                    }
                }
        );
        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    //按返回键操作并且能回退网页
                    if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                        //后退
                        webView.goBack();
                        return true;
                    }
                }
                return false;
            }
        });
        webView.loadUrl("https://www.ankichinas.com/app/about");
    }


    private boolean urlCanLoad(String url) {
        return url.startsWith("http://") || url.startsWith("https://") ||
                url.startsWith("ftp://") || url.startsWith("file://");
    }


    private void startThirdpartyApp(String url) {
        try {
            Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME); // 注释1
//            if (getPackageManager().resolveActivity(intent, 0) == null)
//            {  // 如果手机还没安装app，则跳转到应用市场
//                intent = new Intent(Intent.ACTION_VIEW, Uri.parse
//                        ("market://details?id=" + intent.getPackage())); // 注释2
//            }
            startActivityWithAnimation(intent, LEFT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }



}
