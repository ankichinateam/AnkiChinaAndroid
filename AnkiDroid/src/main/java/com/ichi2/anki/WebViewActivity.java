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


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.gson.Gson;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.dialogs.AsyncDialogFragment;
import com.ichi2.anki.dialogs.ImportDialog;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.libanki.importer.AnkiPackageImporter;
import com.ichi2.themes.StyledProgressDialog;
import com.ichi2.themes.Themes;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONObject;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMWeb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;
import timber.log.Timber;

import static com.ichi2.anim.ActivityTransitionAnimation.LEFT;
import static com.ichi2.anki.DeckPicker.SHOW_STUDYOPTIONS;
import static com.ichi2.anki.dialogs.ImportDialog.DIALOG_IMPORT_ADD_CONFIRM;
import static com.umeng.socialize.utils.DeviceConfigInternal.context;

public class WebViewActivity extends AnkiActivity {
    public static void openUrlInApp(AnkiActivity context, String url, String token, String title, int requestCode) {
        Intent intent = new Intent(context, WebViewActivity.class);
        Timber.i("set url:%s，token is %s", url, token);
        intent.putExtra("url", url);
        intent.putExtra("title", title);
        intent.putExtra("token", token);
        if (requestCode > 0) {
            intent.putExtra("requestCode", requestCode);
            context.startActivityForResultWithoutAnimation(intent, requestCode);
        } else {
            context.startActivityWithoutAnimation(intent);
        }
    }


    public static void openUrlInApp(AnkiActivity context, String url, String token, int requestCode) {
        Intent intent = new Intent(context, WebViewActivity.class);
        Timber.i("set url:%s，token is %s", url, token);

        intent.putExtra("url", url);
        intent.putExtra("token", token);
        if (requestCode > 0) {
            intent.putExtra("requestCode", requestCode);
            context.startActivityForResultWithoutAnimation(intent, requestCode);
        } else {
            context.startActivityWithoutAnimation(intent);
        }
    }


    public static void openUrlInApp(Context context, String url, String token) {
        Intent intent = new Intent(context, WebViewActivity.class);
        Timber.i("set url:%s，token is %s", url, token);

        intent.putExtra("url", url);
        intent.putExtra("token", token);
        context.startActivity(intent);
    }


    private WebView webView;


    @SuppressLint( {"AddJavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    @SuppressWarnings("deprecation")
    protected void onCreate(Bundle savedInstanceState) {
        Themes.setThemeLegacy(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
//            toolbar.inflateMenu(R.menu.web_view);
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
        webView.getSettings().setUserAgentString("User-Agent:Android");

        webView.getSettings().setAllowFileAccess(true);    // 可以读取文件缓存
        webView.getSettings().setAppCacheEnabled(true);    //开启H5(APPCache)缓存功能
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        String appCachePath = getApplicationContext().getCacheDir().getAbsolutePath();
        webView.getSettings().setAppCachePath(appCachePath);
//        webView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ONLY);
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            //构建我们的进度监听器
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), fileName);
            final ProgressResponseBody.ProgressListener listener = (bytesRead, contentLength1, done) -> {
                //计算百分比并更新ProgressBar
                if (contentLength1 != 0) {
                    final int percent = (int) (100 * bytesRead / contentLength1);
                    mProgressDialog.setProgress(percent);
                }
                if (done) {
                    runOnUiThread(() -> {
                        mProgressDialog.dismiss();
                        //fixme 下载的如果不是卡牌，需要执行通用程序
                        AsyncDialogFragment newFragment = ImportDialog.newInstance(DIALOG_IMPORT_ADD_CONFIRM, file.getAbsolutePath(), WebViewActivity.this);
                        showAsyncDialogFragment(newFragment);
                    });
                }
            };
            OkHttpClient client = new OkHttpClient.Builder()
                    .addNetworkInterceptor(chain -> {
                        Response response = chain.proceed(chain.request());
                        return response.newBuilder()
                                .body(new ProgressResponseBody(response.body(), listener))
                                .build();
                    })
                    .build();
            //发送响应
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }


                @Override
                public void onResponse(Call call, Response response) {
                    Timber.i("onResponse:" + response.isSuccessful());
                    writeFile(file, response);
                }
            });

            mProgressDialog = new MaterialDialog.Builder(WebViewActivity.this)
                    .title("正在下载")
                    .content("请不要做任何操作，保持屏幕常亮，切换页面或APP会导致下载中断！")
                    .progress(false, 100, false)
                    .cancelable(false)
                    .negativeText("取消下载")
                    .onNegative((dialog, which) -> {
                        call.cancel();
                        dialog.dismiss();
                    })
                    .show();

        });
        webView.setWebChromeClient(
                new WebChromeClient() {
                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        writeData(getIntent().getStringExtra("token"));
                        WebViewActivity.this.getSupportActionBar().setTitle(title);
                    }

                }
        );
        webView.setWebViewClient(
                new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        writeData(getIntent().getStringExtra("token"));
                    }


                    @SuppressWarnings("deprecation")
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Timber.i("shouldOverrideUrlLoading:%s", url);
                        if (url.startsWith("openexternalbrowser://url=")) {
//                            startThirdpartyApp("https://www.baidu.com");
                            openUrl(Uri.parse(url.replace("openexternalbrowser://url=", "")));
                        } else if (urlCanLoad(url.toLowerCase())) {  // 加载正常网页
                            view.loadUrl(url, map);
                        } else {  // 打开第三方应用或者下载apk等
                            startThirdpartyApp(url);
                        }
                        return true;
                    }
                }
        );
        webView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                //按返回键操作并且能回退网页
                if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                    //后退
                    webView.goBack();
                    return true;
                }
            }
//                JSONObject jsonObject=new JSONObject();
//                JSONArray platform=new JSONArray();
//                platform.put()
//                showShareDialog();
            return false;
        });

        webView.addJavascriptInterface(new JavaScriptFunction(), "AnkiDroidJS");
        map.put("Referer", "https://file.ankichinas.cn");
        webView.loadUrl(getIntent().getStringExtra("url"), map);

    }


    Map<String, String> map = new HashMap<>();


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }


    private long exitTime;


    public void onBackPressed() {
        //如果可以返回上一级，而不是直接退出应用程序
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                UIUtils.showSimpleSnackbar(this, "再操作一次退出页面", true);
                exitTime = System.currentTimeMillis();
            } else {
                if (getIntent().getIntExtra("requestCode", -1) > 0) {
                    setResult(DeckPicker.RESULT_UPDATE_REST_SPACE);
                }
                finishActivityWithFade(this);
            }
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.web_view, menu);
        menu.findItem(R.id.action_quit).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                finishActivityWithFade(WebViewActivity.this);
                return false;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }


    private void writeFile(File file, Response response) {
        OutputStream outputStream = null;
        InputStream inputStream = response.body().byteStream();
        try {
            outputStream = new FileOutputStream(file);
            int len = 0;
            byte[] buffer = new byte[1024 * 10];
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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


    public void writeData(String token) {
        String key = "anki_json_web_token";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            webView.evaluateJavascript("window.localStorage.setItem('" + key + "','" + token + "');", null);
        } else {
            webView.loadUrl("javascript:localStorage.setItem('" + key + "','" + token + "');", map);
        }
    }


    @Override
    protected TaskListener importAddListener() {
        return new ImportAddListener(this);
    }


    private MaterialDialog mProgressDialog;



    private static class ImportAddListener extends TaskListenerWithContext<WebViewActivity> {
        public ImportAddListener(WebViewActivity deckPicker) {
            super(deckPicker);
        }


        @Override
        public void actualOnPostExecute(@NonNull WebViewActivity deckPicker, TaskData result) {
            if (deckPicker.mProgressDialog != null && deckPicker.mProgressDialog.isShowing()) {
                deckPicker.mProgressDialog.dismiss();
            }
            // If boolean and string are both set, we are signalling an error message
            // instead of a successful result.
            if (result.getBoolean() && result.getString() != null) {
                Timber.w("Import: Add Failed: %s", result.getString());
                deckPicker.showSimpleMessageDialog(result.getString());
            } else {
                Timber.i("Import: Add succeeded");
                AnkiPackageImporter imp = (AnkiPackageImporter) result.getObjArray()[0];

                new MaterialDialog.Builder(deckPicker)
                        .title("已成功导入")
                        .content(TextUtils.join("\n", imp.getLog()))
                        .cancelable(false)
                        .positiveColorRes(R.color.primary_color)
                        .positiveText("前往学习")
                        .onPositive((dialog, which) -> {
                            Intent intent = new Intent();
                            intent.putExtra("withDeckOptions", false);
                            Timber.i("now select id:%s", imp.getID());
                            if (imp.getID() < 0) {
                                UIUtils.showSimpleSnackbar(deckPicker, "跳转失败，请回到App主界面前往学习", true);
                                return;
                            }
                            deckPicker.getCol().getDecks().select(imp.getID());
                            intent.setClass(deckPicker, StudyOptionsActivity.class);
                            deckPicker.startActivityForResultWithAnimation(intent, SHOW_STUDYOPTIONS, ActivityTransitionAnimation.LEFT);
                        })
                        .negativeText("确定")
                        .onNegative((dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
//                deckPicker.showSimpleMessageDialog(TextUtils.join("\n", imp.getLog()));
            }
        }


        @Override
        public void actualOnPreExecute(@NonNull WebViewActivity deckPicker) {
            deckPicker.mProgressDialog = StyledProgressDialog.show(deckPicker,
                    deckPicker.getResources().getString(R.string.import_title), null, false);
        }


        @Override
        public void actualOnProgressUpdate(@NonNull WebViewActivity deckPicker, TaskData value) {
            deckPicker.mProgressDialog.setContent(value.getString());
        }
    }



    public static class ProgressResponseBody extends ResponseBody {

        //回调接口
        interface ProgressListener {
            /**
             * @param bytesRead     已经读取的字节数
             * @param contentLength 响应总长度
             * @param done          是否读取完毕
             */
            void update(long bytesRead, long contentLength, boolean done);
        }



        private final ResponseBody responseBody;
        private final ProgressListener progressListener;
        private BufferedSource bufferedSource;


        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }


        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }


        @Override
        public long contentLength() {
            return responseBody.contentLength();
        }


        @Override
        public BufferedSource source() {
            if (bufferedSource == null) {
                bufferedSource = Okio.buffer(source(responseBody.source()));
            }
            return bufferedSource;
        }


        private Source source(Source source) {
            return new ForwardingSource(source) {
                long totalBytesRead = 0L;


                @Override
                public long read(Buffer sink, long byteCount) throws IOException {
                    long bytesRead = super.read(sink, byteCount);
                    totalBytesRead += bytesRead != -1 ? bytesRead : 0;   //不断统计当前下载好的数据
                    //接口回调
                    progressListener.update(totalBytesRead, responseBody.contentLength(), bytesRead == -1);
                    return bytesRead;
                }
            };
        }

    }



    public class JavaScriptFunction {

        private final Gson mGson = new Gson();


        @JavascriptInterface
        public void socialShare(String jsonParams) {
            Timber.i("start social share：" + jsonParams);
            try {
//                jsonParams = {
//                        "platform":["QQ","SINA","WEIXIN","WEIXIN_CIRCLE","TEAM_POSTER"],
//                "title":"标题",
//                        "desc":"描述",
//                        "link":"链接地址",
//                        "thumb":"缩略图"
//              }
                JSONObject root = new JSONObject(jsonParams);
                showShareDialog(jsonParams);
            } catch (Exception e) {
                Timber.w(e);
            }

        }


        @JavascriptInterface
        public String getAppVersion() {
            return BuildConfig.VERSION_NAME;
        }

    }



    private static final String PLATFORM_QQ = "QQ";
    private static final String PLATFORM_WEIXIN = "WEIXIN";
    //    private static final String PLATFORM_SINA = "SINA";
    private static final String PLATFORM_URL = "COPY_LINK";
    private static final String PLATFORM_WEIXIN_CIRCLE = "WEIXIN_CIRCLE";
    private static final String PLATFORM_TEAM_POSTER = "TEAM_POSTER";

    private Dialog mShareDialog;
    private LinearLayout mPlatformWetchat, mPlatformWetchatCircle, mPlatformQQ, mPlatformUrl, mPlatformImage;


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void showShareDialog(String jsonParams) {
        JSONObject root = new JSONObject(jsonParams);
        JSONArray platform = root.getJSONArray("platform");
        String title = root.getString("title");
        String desc = root.getString("desc");
        String link = root.getString("link");
        String thumb = root.getString("thumb");


        if (mShareDialog == null) {
            mShareDialog = new Dialog(this, R.style.DialogTheme2);
            //2、设置布局
            View view = View.inflate(this, R.layout.dialog_share, null);
            mShareDialog.setContentView(view);
            Window window = mShareDialog.getWindow();
//            addMenuBottomDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            //设置弹出位置
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            WindowManager.LayoutParams lps = window.getAttributes();
//            lps.verticalMargin = 0.1f;
//            window.setAttributes(lps);
//            //设置弹出动画
////        window.setWindowAnimations(R.style.main_menu_animStyle);
//            //设置对话框大小
//            final DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mShareDialog.findViewById(R.id.close).setOnClickListener(v -> mShareDialog.dismiss());
            mPlatformWetchat = mShareDialog.findViewById(R.id.ll_weixin);
            mPlatformWetchatCircle = mShareDialog.findViewById(R.id.ll_weixin_circle);
            mPlatformQQ = mShareDialog.findViewById(R.id.ll_qq);
            mPlatformUrl = mShareDialog.findViewById(R.id.ll_link);
            mPlatformImage = mShareDialog.findViewById(R.id.ll_poster);

            mPlatformWetchat.setOnClickListener(view1 -> {
                mShareDialog.dismiss();
                shareUrl(link, title, desc, thumb, SHARE_MEDIA.WEIXIN);
            });
            mPlatformWetchatCircle.setOnClickListener(view1 -> {
                mShareDialog.dismiss();
                shareUrl(link, title, desc, thumb, SHARE_MEDIA.WEIXIN_CIRCLE);
            });
            mPlatformQQ.setOnClickListener(view1 -> {
                mShareDialog.dismiss();
                shareUrl(link, title, desc, thumb, SHARE_MEDIA.QQ);
            });
            mPlatformUrl.setOnClickListener(view1 -> {
                mShareDialog.dismiss();
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                // 创建普通字符型ClipData
                ClipData mClipData = ClipData.newPlainText("Anki记忆卡", link);
                // 将ClipData内容放到系统剪贴板里。
                cm.setPrimaryClip(mClipData);
                Toast.makeText(this, "已复制链接到剪贴板", Toast.LENGTH_SHORT).show();
            });

        }
        if (mShareDialog.isShowing()) {
            mShareDialog.dismiss();
            return;
        }
        mPlatformWetchat.setVisibility(View.GONE);
        mPlatformWetchatCircle.setVisibility(View.GONE);
        mPlatformQQ.setVisibility(View.GONE);
        mPlatformUrl.setVisibility(View.GONE);
        mPlatformImage.setVisibility(View.GONE);

        for (int i = 0; i < platform.length(); i++) {
            String platformName = platform.getString(i);
            if (platformName.equals(PLATFORM_QQ)) {
                mPlatformQQ.setVisibility(View.VISIBLE);
            }
            if (platformName.equals(PLATFORM_WEIXIN)) {
                mPlatformWetchat.setVisibility(View.VISIBLE);
            }
            if (platformName.equals(PLATFORM_URL)) {
                mPlatformUrl.setVisibility(View.VISIBLE);
            }
            if (platformName.equals(PLATFORM_WEIXIN_CIRCLE)) {
                mPlatformWetchatCircle.setVisibility(View.VISIBLE);
            }
            if (platformName.equals(PLATFORM_TEAM_POSTER)) {
                mPlatformImage.setVisibility(View.VISIBLE);
                JSONObject team_info = root.getJSONObject("team_info");
                mPlatformImage.setOnClickListener(view1 -> {
                    mShareDialog.dismiss();
                    Intent intent = new Intent(this, PosterActivity.class);
                    intent.putExtra("poster", team_info.toString());
                    startActivityWithoutAnimation(intent);
                });
            }
        }
        if(!isFinishing()&&!isDestroyed())
        mShareDialog.show();
    }


    public void shareUrl(String url, String title, String desc, String thumb, SHARE_MEDIA share_media) {
        UMWeb web = new UMWeb(url);
        web.setTitle(title);
        web.setThumb(new UMImage(this, thumb));
        web.setDescription(desc);
        new ShareAction(this).withMedia(web)
                .setPlatform(share_media)
                .setCallback(new UMShareListener() {
                    @Override
                    public void onStart(SHARE_MEDIA share_media) {

                    }


                    @Override
                    public void onResult(SHARE_MEDIA share_media) {

                    }


                    @Override
                    public void onError(SHARE_MEDIA share_media, Throwable throwable) {

                    }


                    @Override
                    public void onCancel(SHARE_MEDIA share_media) {

                    }
                }).share();
    }


}
