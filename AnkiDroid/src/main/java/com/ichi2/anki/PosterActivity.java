package com.ichi2.anki;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ichi2.themes.Themes;
import com.ichi2.ui.WarpLinearLayout;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONObject;
import com.ichi2.utils.QRCodeUtil;
import com.jaygoo.widget.Utils;
import com.umeng.socialize.ShareAction;
import com.umeng.socialize.UMShareListener;
import com.umeng.socialize.bean.SHARE_MEDIA;
import com.umeng.socialize.media.UMImage;
import com.umeng.socialize.media.UMWeb;

import static android.view.View.DRAWING_CACHE_QUALITY_HIGH;
import static com.ichi2.anki.widgets.DeckInfoListAdapter.AD_IMAGE_URL;

public class PosterActivity extends AnkiActivity {
//    {
//        "title":"法硕考研上岸合集", "origin_price":"99.99", "team_price":"88.88", "study_users":9999, "level":"研究生", "subject":
//        "法律", "tags":["考研", "政治考点", "法硕"],"quota":1, "countDown":3600, "members":[{
//        "nickname":"张三", "avatar":"https://anki-resource.oss-cn-shenzhen.aliyuncs.com/default/user-avatar.png"
//    },{
//        "nickname":"李四", "avatar":"https://anki-resource.oss-cn-shenzhen.aliyuncs.com/default/user-avatar.png"
//    },{
//        "nickname":"王五", "avatar":"https://anki-resource.oss-cn-shenzhen.aliyuncs.com/default/user-avatar.png"
//    }],"link":"http://file.ankichias.cn/h5"
//    }

    JSONObject root;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.setThemeLegacy(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poster);
        String poster = getIntent().getStringExtra("poster");
          root = new JSONObject(poster);
        JSONArray members = root.getJSONArray("members");

        setText(R.id.content_title, root.getString("title"));
        setText(R.id.content_classify, root.getString("level") + "/" + root.getString("subject"));
        setText(R.id.content_price_origin, "原价¥ " + root.getString("origin_price"));
        ((TextView) findViewById(R.id.content_price_origin)).getPaint().setFlags(Paint. STRIKE_THRU_TEXT_FLAG); //中划线

        setText(R.id.content_price, "¥ " + root.getString("team_price"));
        setText(R.id.content_num, root.getString("study_users") + "人正在学习");
        setText(R.id.content_time_remain, "拼团特惠仅剩" + root.getInt("quota") + "名额\n" + convertSeconds(root.getInt("countDown")) + "后结束");
        setText(R.id.content_num_remain, "还差" + (root.getInt("users")-members.length()) + "人，特享团购价");


        ImageView qrcode = findViewById(R.id.qr_code);
        Bitmap mBitmap = QRCodeUtil.createQRCodeBitmap(root.getString("link"), Utils.dp2px(this, 160), Utils.dp2px(this, 160));
        qrcode.setImageBitmap(mBitmap);

        LinearLayout memberLayout = findViewById(R.id.join_list);
        for (int i = 0; i < members.length(); i++) {
            if(root.getInt("users")>3){
                //需要人数大于3个，那就保留一个空位
                if (i > 1) {
                    break;//最多添加2个
                }
            }
//            if(root.getInt("users")==3) {
//                //需要人数刚好是3个，那就三个都占满
//                if (i > 2) {
//                    break;
//                }
//            }

            View view = getLayoutInflater().inflate(R.layout.item_poster_avatar, null);
            ImageView avatar = view.findViewById(R.id.avatar);
            if (i == 0) {
                view.findViewById(R.id.host_role).setVisibility(View.VISIBLE);
            }
            Glide.with(this)
                    .asBitmap()
                    .load(members.getJSONObject(i).getString("avatar"))
                    .into(avatar);
            memberLayout.addView(view);
        }

        for (int i = memberLayout.getChildCount(); i < 3; i++) {
            View view = getLayoutInflater().inflate(R.layout.item_poster_avatar_empty, null);
            memberLayout.addView(view);
        }


        JSONArray tags = root.getJSONArray("tags");
        for (int i = 0; i < tags.length(); i++) {
            View view = getLayoutInflater().inflate(R.layout.item_warp_poster, null);
            Button button = view.findViewById(R.id.text);
            button.setText(tags.getString(i));
            ((WarpLinearLayout) findViewById(R.id.tags_layout)).addView(view);
        }

        showShareDialog(findViewById(R.id.rootView));
    }


    private String convertSeconds(int seconds) {
        int hour = seconds / 3600;
        int minute = (seconds - hour * 3600) / 60;
        int seconds2 = (seconds - hour * 3600 - minute * 60);
        String hourStr = (hour > 9 ? "" : "0") + hour;
        String minuteStr = (minute > 9 ? "" : "0") + minute;
        String secondsStr = (seconds2 > 9 ? "" : "0") + seconds2;
        return hourStr + ":" + minuteStr + ":" + secondsStr;
    }


    private void setText(int id, String text) {
        ((TextView) findViewById(id)).setText(text);
    }

    private static final String PLATFORM_QQ = "QQ";
    private static final String PLATFORM_WEIXIN = "WEIXIN";
    //    private static final String PLATFORM_SINA = "SINA";
    private static final String PLATFORM_URL = "COPY_LINK";
    private static final String PLATFORM_WEIXIN_CIRCLE = "WEIXIN_CIRCLE";
    private static final String PLATFORM_TEAM_POSTER = "TEAM_POSTER";

    private Dialog mShareDialog;
    private LinearLayout mPlatformWetchat, mPlatformWetchatCircle, mPlatformQQ, mPlatformUrl, mPlatformImage;


    @SuppressWarnings("deprecation")
    private void showShareDialog(View viewSource) {

        if (mShareDialog == null) {
            mShareDialog = new Dialog(this, R.style.DialogTheme2);
            View view = View.inflate(this, R.layout.dialog_share, null);
            mShareDialog.setContentView(view);
            Window window = mShareDialog.getWindow();
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            mShareDialog.findViewById(R.id.close).setOnClickListener(v -> mShareDialog.dismiss());
            mPlatformWetchat = mShareDialog.findViewById(R.id.ll_weixin);
            mPlatformWetchatCircle = mShareDialog.findViewById(R.id.ll_weixin_circle);
            mPlatformQQ = mShareDialog.findViewById(R.id.ll_qq);
            mPlatformUrl = mShareDialog.findViewById(R.id.ll_link);
            mPlatformImage = mShareDialog.findViewById(R.id.ll_poster);

            mPlatformWetchat.setOnClickListener(view1 -> {
//                mShareDialog.dismiss();
                shareUrl(viewSource, SHARE_MEDIA.WEIXIN);
            });
            mPlatformWetchatCircle.setOnClickListener(view1 -> {
//                mShareDialog.dismiss();
                shareUrl(viewSource, SHARE_MEDIA.WEIXIN_CIRCLE);
            });
            mPlatformQQ.setOnClickListener(view1 -> {
//                mShareDialog.dismiss();
                shareUrl(viewSource, SHARE_MEDIA.QQ);
            });
            mPlatformImage.setOnClickListener(view1 -> {
                String url=MediaStore.Images.Media.insertImage(getContentResolver(), createBitmapFromView(viewSource),  root.getString("title"),BuildConfig.APPLICATION_ID );
                Toast.makeText(this,url==null?"保存失败":"已保存到本地相册",Toast.LENGTH_SHORT).show();

            });
            ((TextView)mShareDialog.findViewById(R.id.tv_poster)).setText("保存海报");

        }
        if (mShareDialog.isShowing()) {
            mShareDialog.dismiss();
            return;
        }
        mPlatformWetchat.setVisibility(View.VISIBLE);
        mPlatformWetchatCircle.setVisibility(View.VISIBLE);
        mPlatformQQ.setVisibility(View.VISIBLE);
        mPlatformImage.setVisibility(View.VISIBLE);
        mPlatformUrl.setVisibility(View.GONE);


        mShareDialog.setOnDismissListener(dialog -> finishWithoutAnimation());
        mShareDialog.show();
    }


    public void shareUrl(View view, SHARE_MEDIA share_media) {
        Bitmap bitmap =createBitmapFromView(view);
        UMImage imagelocal = new UMImage(this, bitmap);
        imagelocal.setThumb(new UMImage(this,bitmap));
        new ShareAction( this).withMedia(imagelocal )
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

    public static Bitmap createBitmapFromView(View view) {
        view.clearFocus();

        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight() , Bitmap.Config.ARGB_8888);
        if (bitmap != null) {
            Canvas canvas = new Canvas(bitmap);
            view.draw(canvas);
            canvas.setBitmap(null);

        }
        return bitmap;
    }
}
