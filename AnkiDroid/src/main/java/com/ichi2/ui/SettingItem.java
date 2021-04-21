package com.ichi2.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.ichi2.anki.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class SettingItem extends FrameLayout {
    public SettingItem(@NonNull Context context) {
        super(context);
    }


    public SettingItem(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }


    public SettingItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


//    public SettingItem(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
//        super(context, attrs, defStyleAttr, defStyleRes);
//    }
    public SettingItem(Context context, String title, Drawable drawable){
        super(context);
        LayoutInflater.from(context).inflate(R.layout.item_setting, this );
        TextView tvTitle=findViewById(R.id.tv_title);
        tvTitle.setText(title);
        ImageView icon=findViewById(R.id.icon);
        icon.setImageDrawable( drawable);
    }




}
