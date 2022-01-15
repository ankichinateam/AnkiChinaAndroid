package com.anki.tools;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import timber.log.Timber;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    TextView mHint;
    EditText mPath,mTarget;
    public static final String TAG = "AnkiTools";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mHint=findViewById(R.id.hint);
        mPath=findViewById(R.id.path);
        mTarget=findViewById(R.id.target);
            Timber.plant(new Timber.DebugTree());

        Timber.tag(TAG);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);

    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(this, "必须提供读写权限才能修改数据库", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    private static  String PATH1="/sdcard/AnkiChina";
    private static  String PATH2="/sdcard/AnkiDroid";
    String mCurrentPath;
    private void init(){
        File dbFile = new File(PATH1+"/"+"collection.anki2");
        if(dbFile.exists() ){
            mPath.setText(PATH1);
        }else  if(new File(PATH2+"/"+"collection.anki2").exists() ){
            mPath.setText(PATH2);
        }
        if(new File(mPath.getText()+"/"+"collection.anki2").exists() ){
            DB db = new DB(this,mPath.getText()+"/"+"collection.anki2");
            int ver = db.queryScalar("SELECT ver FROM col");
            db.close();
            Timber.i("当前Anki数据库版本为:"+ver);
            mHint.setText("当前Anki数据库版本为:"+ver);
        }else {
            mHint.setText("未检测到Anki数据库文件");
        }

    }

    public void confirm(View view){
        File dbFile = new File(mPath.getText()+"/"+"collection.anki2");
        if(dbFile.exists()&&dbFile.length()>0){
            DB db = new DB(this,mPath.getText()+"/"+"collection.anki2");
            db.execute("update col set ver = "+mTarget.getText());
            int ver = db.queryScalar("SELECT ver FROM col");
            db.close();
            Timber.i("当前Anki数据库版本为:"+ver);
            mHint.setText("当前Anki数据库版本为:"+ver);
        }else {
            Toast.makeText(this, "请输入正确的数据库所在目录的路径", Toast.LENGTH_SHORT).show();
        }
    }

}