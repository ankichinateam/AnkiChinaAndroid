package com.ichi2.libanki.sync;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.BackupManager;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.R;
import com.ichi2.async.CollectionTask;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Consts;
import com.ichi2.libanki.DB;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.DeckConfig;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Media;
import com.ichi2.libanki.Model;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.utils.Time;
import com.ichi2.ui.CustomStyleDialog;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONObject;
import com.ichi2.utils.OKHttpUtil;
import com.ichi2.utils.okhttp.listener.ProgressListener;
import com.ichi2.utils.okhttp.utils.OKHttpUtils;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import timber.log.Timber;

public class AnkiChinaSyncer {
    private final Activity mContext;
    private final SharedPreferences mPreferences;
    private final String mToken;
    private String mCurrentSession;
    private int mPostPageSize = 200;//默认200
    private Collection mCol;

    private final Handler mHandler;

    private final OnSyncCallback mCallback;


    public AnkiChinaSyncer(Activity context, String token, OnSyncCallback callback) {
        this.mContext = context;
        mToken = token;
        mCallback = callback;
        mCol = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
        mPreferences = AnkiDroidApp.getSharedPrefs(context);
        mHandler = new Handler(Looper.getMainLooper());
    }


    CustomStyleDialog mSyncChinaDialog;
    TextView mDialogTitle, mDialogProgress/*, mDialogButton*/;
    Button mDialogConfirm;
    boolean mCancel = false;

    private void showSyncChinaDialog(Activity context) {

        mSyncChinaDialog = new CustomStyleDialog(context, R.style.CommonDialogTheme);
        mSyncChinaDialog.setContentView(R.layout.dialog_sync_china);
        mDialogTitle = mSyncChinaDialog.findViewById(R.id.title);
        mDialogProgress = mSyncChinaDialog.findViewById(R.id.progress);
        mDialogConfirm = mSyncChinaDialog.findViewById(R.id.confirm);
        mSyncChinaDialog.setCancelable(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            if (!context.isFinishing() && !context.isDestroyed()) {
                mSyncChinaDialog.show();
            }
        } else {
            if (!context.isFinishing()) {
                mSyncChinaDialog.show();
            }
        }

    }


    private double mCurrentProgress;

    private String mCurrentState = "";


    @SuppressLint("DefaultLocale")
    private void updateDialogProgress(String title, String secondTitle, double progress) {
        if (!mCurrentState.equals(secondTitle)) {
            Timber.i("update dialog progress " + title + "," + secondTitle + "," + progress + "------------------ add " + (progress - mCurrentProgress));
        }
        mCurrentState = secondTitle;
        mCurrentProgress = progress;
        mHandler.post(() -> {
            mDialogTitle.setText(secondTitle);
            mDialogProgress.setText(String.format("%.2f", progress).concat("%"));
            if (title.equals(SYNCING_COMPLETED)) {
                if (mSyncChinaDialog.isShowing()&&!mContext.isFinishing()&&!mContext.isDestroyed()) {
                    mSyncChinaDialog.dismiss();
                }
                SYNCING = false;
                mCallback.onCompletedAll();
            }
            if (title.equals(SYNCING_MEDIA) && mSyncChinaDialog.isShowing()&&!mContext.isFinishing()&&!mContext.isDestroyed()) {
                mSyncChinaDialog.dismiss();
            }

        });
    }


    private void updateDialogMessage(String title, String message) {
        Timber.i("update dialog message " + title + "," + message);
        mHandler.post(() -> {
            mDialogTitle.setText(title);
            mDialogProgress.setText(message);

            if (title.equals(SYNCING_ERROR)) {
                SYNCING = false;
                mCancel = true;
                mCallback.onError(100, message);
                if (mSyncChinaDialog.isShowing()) {
                    mSyncChinaDialog.setCancelable(true);
                    mDialogConfirm.setVisibility(View.VISIBLE);
                    mSyncChinaDialog.findViewById(R.id.loading_view).setVisibility(View.GONE);
                    mDialogConfirm.setOnClickListener(v -> {
                        if (mSyncChinaDialog.isShowing()) {
                            mSyncChinaDialog.dismiss();
                        }
                    });
                }


            }
        });
    }


    private final String SYNCING_DATA = "数据同步中";
    private final String SYNCING_MEDIA = "多媒体同步中";
    private final String SYNCING_COMPLETED = "同步完成";
    private final String SYNCING_ERROR = "同步失败";

    private final String ERROR_NETWORK = "网络异常，请稍候再试";
    private final String ERROR_DATA = "数据异常";
    public static boolean SYNCING;



    public interface OnSyncCallback {
        void onError(int code, String message);

        void onCompletedAll();

        void onCompletedData();
    }


    //1、获取sessionkey
    //2、查看本地是否有synclog
    //3、如果本地没有synclog，则使用完整的数据库数据和服务端比对
    //4、如果本地有synclog，则将synclog和数据库对比，得出变化的数据，上报数据库
    //5、获得本地需要改动的返回内容，修改数据库
    //6、将修改后的数据库的内容保存到synclog里，供下次同步对比
    public void sync() {
        if (SYNCING) {
//            showSyncChinaDialog(mContext);
//            updateDialogMessage(SYNCING_ERROR, "同步状态异常，请重启APP");
            return;
        }
        SYNCING = true;
        Timber.i("start sync china");
        showSyncChinaDialog(mContext);
        BackupManager.performBackupInBackground(mCol.getPath(), true, CollectionHelper.getInstance().getTimeSafe(mContext));
        mCol.getDb().execute("create table if not exists synclog (" + "    id             integer not null,"
                + "    type             integer not null," + "    mod             integer not null" + ")");
        mCol.getDb().execute("create index if not exists ix_synclog on synclog (id,type);)");
        mCol.getDb().execute("create index if not exists ix_synclog_id on synclog (id);)");
        mCol.getDb().execute("create index if not exists ix_cards_id on cards (id);)");
        mCol.getDb().execute("create index if not exists ix_notes_id on notes (id);)");


        mCurrentSession = mPreferences.getString(Consts.KEY_SYNC_CHINA_SESSION, "");
        mPreferences.edit().putLong(Consts.KEY_LAST_STOP_TIME, System.currentTimeMillis()).apply();
        String url = Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/getKey";
        url += "?terminal_time=" + System.currentTimeMillis() / 1000 + "&terminal_crt=" + mCol.getCrt();
        if (!mCurrentSession.isEmpty()) {
            url += "&last_session_key=" + mCurrentSession;
        } else {
            //全量同步直接上传本地数据
            try {
                mCol.getDb().execute("delete from synclog");
            }catch (Exception e){
//                mCol=CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
//                mCol.getDb().execute("drop table synclog");
//                mCol.getDb().execute("create table if not exists synclog (" + "    id             integer not null,"
//                        + "    type             integer not null," + "    mod             integer not null" + ")");
                e.printStackTrace();

            }
        }

        updateDialogProgress(SYNCING_DATA, "整理数据中", 2);
        if (mCancel) {
            return;
        }
        OKHttpUtil.get(url, mToken, "", new OKHttpUtil.MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
            }


            @Override
            public void onResponse(Call call, String token, Object arg1, Response response) {
                if (mCancel) {
                    return;
                }
                if (response.isSuccessful()) {
                    try {
                        final JSONObject object = new JSONObject(response.body().string());
                        if (object.getInt("status_code") != 0) {
                            updateDialogMessage(SYNCING_ERROR, object.getString("message"));
                            return;
                        }
                        final JSONObject item = object.getJSONObject("data");
                        Timber.i("get session key successfully!:%s", object.toString());
                        mPostPageSize = item.getInt("page_size");
                        if (!mCurrentSession.isEmpty()) {
                            mCurrentSession = item.getString("session_key");
                            JSONObject localChangedData = compareSyncTable();
//                        Timber.i("ready to push local sync data:%s", localChangedData.toString());
                            RequestBody formBody = new FormBody.Builder()
                                    .add("data", localChangedData.toString())
                                    .add("session_key", mCurrentSession)
                                    .build();

                            updateDialogProgress(SYNCING_DATA, "上传数据中", 10);
                            mPostDataPerPercent = 20.0 / (mRestNoteList.size() + 1);
                            OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/postData", formBody, token, "", mPostLocalDataCallback);
                        } else {
                            //全量同步
                            mCurrentSession = item.getString("session_key");
                            updateDialogProgress(SYNCING_DATA, "上传数据中", 10);
                            File zip = zipAllCollection();
                            List<String> zipPath = new ArrayList<>();
                            zipPath.add(zip.getAbsolutePath());
                            OKHttpUtils.doPostRequest(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/postData", mCurrentSession, zipPath, uploadProgress, zip.getAbsolutePath(), token, new OKHttpUtils.MyCallBack() {
                                @Override
                                public void onFailure(Call call, final IOException e) {
                                    Timber.i("upload error------>%s %s", zip.getAbsolutePath(), e.getMessage());
                                    updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
                                    mUploadResultEndCount++;
                                }


                                @Override
                                public void onResponse(Call call, Object tag, Response response) throws IOException {
                                    mPostLocalDataCallback.onResponse(call, token, tag, response);
//                                    zip.delete();
                                }
                            });
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                        updateDialogMessage(SYNCING_ERROR, ERROR_DATA);
                    }
                } else {
                    Timber.e("get session key error, code %d", response.code());
                    updateDialogMessage(SYNCING_ERROR, ERROR_DATA);
                }
            }
        });
    }


    double mPostDataPerPercent = 20.0;
    double mPullNotesPerPercent = 35.0;
    private final OKHttpUtil.MyCallBack mPostLocalDataCallback = new OKHttpUtil.MyCallBack() {
        @Override
        public void onFailure(Call call, IOException e) {
            updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
        }


        @Override
        public void onResponse(Call call, String token, Object arg1, Response response) {
            if (response.isSuccessful()) {
                try {
                    final JSONObject object = new JSONObject(response.body().string());
//                    Timber.i("object:%s", object.toString());
                    if (object.getInt("status_code") != 0) {
                        updateDialogMessage(SYNCING_ERROR, object.getString("message"));
                        return;
                    }
                    updateDialogProgress(SYNCING_DATA, "上传数据中", mCurrentProgress + mPostDataPerPercent);//上传数据一共占用20，至此一共占用30%
                    if (mRestNoteList.size() > 0) {
                        JSONArray restNotes = mRestNoteList.pollLast();
                        RequestBody formBody = new FormBody.Builder()
                                .add("data", restNotes.toString())
                                .add("session_key", mCurrentSession)
                                .build();
                        Timber.i("ready to push local sync data:%s", restNotes.length());
                        OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/postData", formBody, token, "", mPostLocalDataCallback);
                        return;
                    }
                    updateDialogProgress(SYNCING_DATA, "获取同步数据中", 30);

                    OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/pullData?session_key=" + mCurrentSession, mToken, "", new OKHttpUtil.MyCallBack() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
                        }


                        @Override
                        public void onResponse(Call call, String token, Object arg1, Response response) {
                            if (response.isSuccessful()) {
                                try {
                                    updateDialogProgress(SYNCING_DATA, "获取同步数据中", 30);
                                    final JSONObject object = new JSONObject(response.body().string());
                                    if (object.getInt("status_code") != 0) {
                                        updateDialogMessage(SYNCING_ERROR, object.getString("message"));
                                        return;
                                    }
                                    Timber.e("main object:%s", object.toString());
                                    if (object.get("data") != null) {
                                        final JSONObject item = object.getJSONObject("data");
                                        int pageCount = item.getJSONObject("notes").getInt("page");
                                        mPullNotesPerPercent = 35.0 / pageCount;
                                        Timber.e("need download notes page count :%d", pageCount);
                                        handleServerData(item);//处理数据,notes占用35%，其他占用25%，整个过程结束一共占用90%

                                        try {
                                            String lastID = item.getJSONObject("notes").getString("last_id");
                                            if (lastID != null && !lastID.equals("null")) {
                                                fetchRestNotesFromServer(lastID);//还有多余数据就继续递归处理
                                                return;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    completeDataSync(token);//完成本次同步
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    updateDialogMessage(SYNCING_ERROR, ERROR_DATA);
                                }
                            } else {
                                Timber.e("pullData error, code %d", response.code());
                                updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    updateDialogMessage(SYNCING_ERROR, ERROR_DATA);
                }
            } else {
                Timber.e("PostLocalData error, code %d", response.code());
                updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
            }
        }
    };


    private void fetchRestNotesFromServer(String lastID) {
        OKHttpUtil.get(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/pullData?session_key=" + mCurrentSession + "&last_id=" + lastID, mToken, "", new OKHttpUtil.MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
            }


            @Override
            public void onResponse(Call call, String token, Object arg1, Response response) {
                if (response.isSuccessful()) {
                    try {
                        updateDialogProgress(SYNCING_DATA, "同步更多笔记中", mCurrentProgress + mPullNotesPerPercent);
                        final JSONObject object = new JSONObject(response.body().string());
//                        Timber.i("object:%s", object.toString());
                        if (object.getInt("status_code") != 0) {
                            updateDialogMessage(SYNCING_ERROR, object.getString("message"));
                            return;
                        }
                        if (object.get("data") != null) {
                            final JSONObject item = object.getJSONObject("data");
                            DB db = mCol.getDb();
                            try {
                                JSONArray replace = item.getJSONArray("replace");
                                if (replace.length() > 0) {
                                    db.getDatabase().beginTransaction();
                                    for (int i = 0; i < replace.length(); i++) {
                                        db.execute("insert or replace into notes values (?,?,?,?,?,?,?,?,?,?,?)",
                                                replace.getJSONArray(i).get(0), replace.getJSONArray(i).get(1), replace.getJSONArray(i).get(2), replace.getJSONArray(i).get(3), replace.getJSONArray(i).get(4), replace.getJSONArray(i).get(5), replace.getJSONArray(i).get(6), replace.getJSONArray(i).get(7), replace.getJSONArray(i).get(8), replace.getJSONArray(i).get(9), replace.getJSONArray(i).get(10));
                                    }
                                    db.getDatabase().setTransactionSuccessful();
                                    mCol.save();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            } finally {
                                db.getDatabase().endTransaction();
                            }
                            try {
                                String lastID = item.getString("last_id");
                                if (lastID != null && !lastID.equals("null")) {
                                    fetchRestNotesFromServer(lastID);//还有多余数据就继续递归处理
                                    return;
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        completeDataSync(token);//完成本次同步
                    } catch (Exception e) {
                        e.printStackTrace();
                        updateDialogMessage(SYNCING_ERROR, ERROR_DATA);
                    }
                } else {
                    Timber.e("pullData error, code %d", response.code());
                }
            }
        });
    }


    private JSONObject compareSyncTable() {
        mCol=CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
        CollectionHelper.getInstance().lockCollection();
        DB db = mCol.getDb();
        db.execute("create index if not exists ix_synclog_id on synclog (id);)");
        int num = db.queryScalar("SELECT id FROM synclog");
        if (num == 0) {
            //表是空的，则直接同步所有内容到该表
            Timber.w("no record in synclog table!");

        }
        JSONObject root = new JSONObject();
        JSONObject colJson = new JSONObject();
        JSONObject colJsonReplace = new JSONObject();
//        JSONObject decksJson = new JSONObject();
//        JSONObject modelsJson = new JSONObject();
//        JSONObject dconfJson = new JSONObject();
        colJsonReplace.put("crt", mCol.getCrt());
        colJsonReplace.put("mod", mCol.getMod());
        colJsonReplace.put("scm", mCol.getScm());
        colJsonReplace.put("ver", mCol.getVer());
        colJsonReplace.put("dty", mCol.getDirty() ? 1 : 0);
        colJsonReplace.put("usn", mCol.getUsnForSync());
        colJsonReplace.put("ls", mCol.getLs());
        colJsonReplace.put("conf", mCol.getConf());
        colJsonReplace.put("tags", mCol.getTagsJson());
        colJson.put("replace", colJsonReplace);
        root.put("col", colJson);
        root.put("decks", getChangedColJson(SYNC_LOG_TYPE_DECKS, mCol.getDecks().all()));
        root.put("dconf", getChangedColJson(SYNC_LOG_TYPE_DCONF, mCol.getDecks().allConf()));
        root.put("models", getChangedColJson(SYNC_LOG_TYPE_MODELS, mCol.getModels().all()));
        updateDialogProgress(SYNCING_DATA, "整理数据中", 6);
        root.put("cards", getChangedCardsOrNote(SYNC_LOG_TYPE_CARD));
        root.put("notes", getChangedCardsOrNote(SYNC_LOG_TYPE_NOTE));
        root.put("revlog", getAddedRevLog());
        CollectionHelper.getInstance().unlockCollection();
//        root.put("revlog", getChangedCardsOrNote(SYNC_LOG_TYPE_REVLOG));
        return root;
    }


    private final int SYNC_LOG_TYPE_DECKS = 0;
    private final int SYNC_LOG_TYPE_DCONF = 1;
    private final int SYNC_LOG_TYPE_MODELS = 2;
    private final int SYNC_LOG_TYPE_CARD = 3;
    private final int SYNC_LOG_TYPE_NOTE = 4;
    private final int SYNC_LOG_TYPE_REVLOG = 5;


    private <T extends JSONObject> JSONObject getChangedColJson(int type, List<T> newModels) {
        List<T> changedModel = new ArrayList<>();
        boolean deleted;
        Cursor cur = null;
        Map<Long, Long> oldModel = new HashMap<>();
        JSONObject modelsJson = new JSONObject();
        try {
            cur = CollectionHelper.getInstance() .getColSafe(AnkiDroidApp.getInstance()).getDb()
                    .getDatabase()
                    .query(
                            "SELECT id,mod FROM synclog WHERE type = " + type, null);
            while (cur.moveToNext()) {
                oldModel.put(cur.getLong(0), cur.getLong(1));
            }
        } finally {
            if (cur != null && !cur.isClosed()) {
                cur.close();
            }
        }

        if (oldModel.isEmpty()) {
            changedModel.addAll(newModels);
        } else {
            StringBuilder deletedModelSb = new StringBuilder();
            for (Map.Entry<Long, Long> entry : oldModel.entrySet()) {
//                System.out.println("key= " + entry.getKey() + " and value= " + entry.getValue());
                deleted = true;
                for (T model : newModels) {
                    if (!changedModel.contains(model) && (oldModel.get(model.getLong("id")) == null || oldModel.get(model.getLong("id")) < model.getLong("mod"))) {
                        //新增或修改过的deck
                        changedModel.add(model);
                    } else if (model.getLong("id") == entry.getKey()) {
                        //未被删除的deck
                        deleted = false;
                    }

                }
                if (deleted) {
                    if (deletedModelSb.length() != 0) {
                        deletedModelSb.append(",");
                    }
                    deletedModelSb.append(entry.getKey());
                }
            }
            if (deletedModelSb.length() > 0) {
                modelsJson.put("delete", new JSONArray(deletedModelSb.toString().split(",")));

//                modelsJson.put("delete", strArray2jsonArray(deletedModelSb.toString().split(",")));
            }
        }

        JSONArray changedModelJson = new JSONArray();
        for (T item : changedModel) {
            changedModelJson.put(item);
        }
        modelsJson.put("replace", changedModelJson);
        return modelsJson;
    }


    //    JSONArray mRestNotes;
    LinkedList<JSONArray> mRestNoteList = new LinkedList<>();


    //    int mNotePage = 0;
//    int mCurCount = 1000;
    private JSONObject getChangedCardsOrNote(int type) {
        String tableName = type == SYNC_LOG_TYPE_NOTE ? "notes" : "cards";
        Cursor cur = null;
        JSONObject result = new JSONObject();
        JSONArray changedData = new JSONArray();
        StringBuilder needDelete = new StringBuilder();
        int page = 0;
        int curCount = 1000;
        mCol=CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
        CollectionHelper.getInstance().lockCollection();
        while (curCount == 1000) {
            String sql = type == SYNC_LOG_TYPE_CARD ?
                    String.format(Locale.ENGLISH, "select b.id,b.nid,b.did,b.ord,b.mod,b.usn,b.type,b.queue,b.due,b.ivl,b.factor,b.reps,b.lapses,b.left,b.odue,b.odid,b.flags,b.data,a.id as aid,a.mod as premod from synclog a left join %s b on a.id=b.id where a.type = %d and (b.mod ISNULL or a.mod !=b.mod)" +
                            " union " +
                            "select b.id,b.nid,b.did,b.ord,b.mod,b.usn,b.type,b.queue,b.due,b.ivl,b.factor,b.reps,b.lapses,b.left,b.odue,b.odid,b.flags,b.data,a.id as aid,a.mod as premod from %s b left join synclog a on a.id=b.id and a.type =%d  where a.mod ISNULL or a.mod !=b.mod limit 1000 offset %d", tableName, type, tableName, type, page * 1000)
                    :
                    String.format(Locale.ENGLISH, "select b.id,b.guid,b.mid,b.mod,b.usn,b.tags,b.flds,b.sfld,b.csum,b.flags,b.data,a.id as aid,a.mod as premod from synclog a left join %s b on a.id=b.id where a.type = %d and (b.mod ISNULL or a.mod !=b.mod)" +
                            " union " +
                            "select b.id,b.guid,b.mid,b.mod,b.usn,b.tags,b.flds,b.sfld,b.csum,b.flags,b.data,a.id as aid,a.mod as premod from %s b left join synclog a on a.id=b.id  and a.type =%d  where a.mod ISNULL or a.mod !=b.mod limit 1000 offset %d", tableName, type, tableName, type, page * 1000);
//        String sql = String.format(Locale.ENGLISH, "select synclog.id as aid,synclog.mod as premod,%s.* from synclog left join %s on synclog.id = %s.id and synclog.type = %d" +
//                " union " +
//                "select synclog.id as aid,synclog.mod as premod,%s.* from %s left join synclog on synclog.id=%s.id and synclog.type = %d ", tableName,tableName,tableName, type, tableName,tableName,tableName, type);

            try {
                cur = mCol.getDb()
                        .getDatabase()
                        .query(sql, null);
                curCount = cur.getCount();
                if (curCount == 1000) {
                    page++;//可能有更多数据
                }
                while (cur.moveToNext()) {
                    String id = cur.getString(cur.getColumnIndex("id"));
                    String aid = cur.getString(cur.getColumnIndex("aid"));
                    if (id == null) {
                        //新的cards表里没有id，则该条已被删除
                        Timber.w("this card id is deleted");
                        if (needDelete.length() != 0) {
                            needDelete.append(",");
                        }
                        needDelete.append(aid);
                    } else if (aid == null || cur.getLong(cur.getColumnIndex("mod")) > cur.getLong(cur.getColumnIndex("premod"))) {
                        //没有旧id或修改时间比原表新，代表有更新或新增
                        List<Object> needReplace = new ArrayList<>();

                        for (int i = 0; i < cur.getColumnCount() - 2; i++) {
//                        if(cur.getType(i) == Cursor.FIELD_TYPE_STRING)
                            needReplace.add(cur.getType(i) != Cursor.FIELD_TYPE_STRING ? cur.getLong(i) : cur.getString(i));

                        }
                        if (type == SYNC_LOG_TYPE_NOTE) {
                            if (changedData.length() < mPostPageSize) {
                                changedData.put(new JSONArray(needReplace));
                            } else {
                                if (mRestNoteList.size() > 0 && mRestNoteList.getLast().length() < mPostPageSize) {
                                    mRestNoteList.getLast().put(new JSONArray(needReplace));
                                } else {
                                    JSONArray newArray = new JSONArray();
                                    newArray.put(new JSONArray(needReplace));
                                    mRestNoteList.addLast(newArray);
                                }

                            }
                        } else {
                            changedData.put(new JSONArray(needReplace));
                        }
                    }
                }

            } finally {
                if (cur != null && !cur.isClosed()) {
                    cur.close();
                }
            }
        }
        CollectionHelper.getInstance().unlockCollection();
        result.put("replace", changedData);
        Timber.e("放了%d条notes", changedData.length());
        if (needDelete.length() != 0) {
            result.put("delete", new JSONArray(needDelete.toString().split(",")));
        }
        return result;
    }


    private JSONObject getAddedRevLog() {
        Cursor cur = null;

        JSONObject result = new JSONObject();
        JSONArray changedData = new JSONArray();
//        String.format(Locale.ENGLISH, "select b.id,b.guid,b.mid,b.mod,b.usn,b.tags,b.flds,b.sfld,b.csum,b.flags,b.data,a.id as aid,a.mod as premod from synclog a left join %s b on a.id=b.id where a.type = %d and (b.mod ISNULL or a.mod !=b.mod)" +
//                " union " +
//                "select b.id,b.guid,b.mid,b.mod,b.usn,b.tags,b.flds,b.sfld,b.csum,b.flags,b.data,a.id as aid,a.mod as premod from %s b left join synclog a on a.id=b.id  and a.type =%d  where a.mod ISNULL or a.mod !=b.mod limit 1000 offset %d", tableName, type, tableName, type, page * 1000);
        String sql = "select b.id,b.cid,b.usn,b.ease,b.ivl,b.lastIvl,b.factor,b.time,b.type,a.id as aid,a.mod as premod from synclog a left join revlog b on a.id=b.id where a.type = 5 and (b.id ISNULL or a.mod !=b.id)" +
                " union " +
                "select b.id,b.cid,b.usn,b.ease,b.ivl,b.lastIvl,b.factor,b.time,b.type,a.id as aid,a.mod as premod from revlog b left join synclog a on a.id=b.id and a.type = 5 where a.mod ISNULL or a.mod !=b.id";
//        String sql = String.format(Locale.ENGLISH, "select synclog.id as aid,synclog.mod as premod,%s.* from synclog left join %s on synclog.id = %s.id and synclog.type = %d" +
//                " union " +
//                "select synclog.id as aid,synclog.mod as premod,%s.* from %s left join synclog on synclog.id=%s.id and synclog.type = %d ", tableName,tableName,tableName, type, tableName,tableName,tableName, type);

        try {
            cur = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getDb()
                    .getDatabase()
                    .query(sql, null);
            while (cur.moveToNext()) {
                String aid = cur.getString(cur.getColumnIndex("aid"));
                if (aid == null) {
                    //没有旧id，代表有新增
                    List<Object> needReplace = new ArrayList<>();
                    for (int i = 0; i < cur.getColumnCount() - 2; i++) {
//                        if(cur.getType(i) == Cursor.FIELD_TYPE_STRING)
                        needReplace.add(cur.getType(i) != Cursor.FIELD_TYPE_STRING ? cur.getLong(i) : cur.getString(i));

                    }
                    changedData.put(new JSONArray(needReplace));
                }
            }

        } finally {
            if (cur != null && !cur.isClosed()) {
                cur.close();
            }
        }
        result.put("replace", changedData);
        Timber.i("get added revlog:%s", result.toString());
        return result;
    }


    //35%+25%
    private void handleServerData(JSONObject item) {
        mCol =  CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
        CollectionHelper.getInstance().lockCollection();
        updateDialogProgress(SYNCING_DATA, "更新全局配置中", mCurrentProgress + 1);
        DB db = mCol.getDb();
        try {
            JSONObject remoteCol = item.getJSONObject("col").getJSONObject("replace");
//            db.execute("update col set id ="+remoteCol.getInt("id")+","+"set crt =\"+remoteCol.getLong(\"crt\")");
//            db.execute("update col set crt ="+remoteCol.getLong("crt"));
//            db.execute("update col set mod ="+remoteCol.getLong("mod"));
//            db.execute("update col set scm ="+remoteCol.getLong("scm"));
//            db.execute("update col set ver ="+remoteCol.getInt("ver"));
//            db.execute("update col set dty ="+remoteCol.getInt("dty"));
//            db.execute("update col set usn ="+remoteCol.getInt("usn"));
//            db.execute("update col set ls ="+remoteCol.getLong("ls"));
//            db.execute("update col set conf ="+remoteCol.getString("conf"));
//            db.execute("update col set tags ="+remoteCol.getString("tags"));
//            db.execute("update col set tags ="+remoteCol.getString("tags"));
//            db.execute("update col set id = %d,");
        Timber.i("remote col config:%s", remoteCol.toString());
            @SuppressLint("DefaultLocale") String sql = String.format("update col set id = %d,crt = %d,mod=%d,scm=%d,ver=%d,dty=%d,usn=%d,ls=%d,conf='%s',tags='%s'", remoteCol.getInt("id"), remoteCol.getLong("crt")
                    , remoteCol.getLong("mod"), remoteCol.getLong("scm"), remoteCol.getInt("ver"), remoteCol.getInt("dty"), remoteCol.getInt("usn"), remoteCol.getLong("ls"), remoteCol.getString("conf"), remoteCol.getString("tags") == null || !remoteCol.getString("tags").startsWith("{") ? "{}" : remoteCol.getString("tags"));
            db.execute(sql);
            mCol.load();
        } catch (Exception e) {
            e.printStackTrace();

        }
        Decks currentDecks = mCol.getDecks();

        //删除多余的内容
        try {
            JSONArray deletedDecks = item.getJSONObject("decks").getJSONArray("delete");
            if (deletedDecks.length() > 0) {
                double percent = 2.0 / deletedDecks.length();
                for (int i = 0; i < deletedDecks.length(); i++) {
                    String deckID = deletedDecks.getString(i);
                    currentDecks.rem(Long.parseLong(deckID));
                    updateDialogProgress(SYNCING_DATA, "删除多余牌组中", mCurrentProgress + percent);
                }
                mCol.save();
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }


        try {
            JSONArray deletedDConf = item.getJSONObject("dconf").getJSONArray("delete");
            if (deletedDConf.length() > 0) {
                double percent = 2.0 / deletedDConf.length();
                for (int i = 0; i < deletedDConf.length(); i++) {
                    String id = deletedDConf.getString(i);
                    mCol.getDecks().remConf(Long.parseLong(id));
                    updateDialogProgress(SYNCING_DATA, "删除多余牌组配置中", mCurrentProgress + percent);
                }
                mCol.save();
            }


        } catch (Exception e) {
//            e.printStackTrace();
        }

        try {
            JSONArray deletedModel = item.getJSONObject("models").getJSONArray("delete");
            if (deletedModel.length() > 0) {
                double percent = 2.0 / deletedModel.length();
                for (int i = 0; i < deletedModel.length(); i++) {
                    String id = deletedModel.getString(i);
                    mCol.getModels().rem(mCol.getModels().get(Long.parseLong(id)));
                    updateDialogProgress(SYNCING_DATA, "删除多余模板中", mCurrentProgress + percent);
                }
                mCol.save();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        updateDialogProgress(SYNCING_DATA, "删除多余卡牌中", mCurrentProgress + 2);
        try {
            JSONArray deletedCards = item.getJSONObject("cards").getJSONArray("delete");
            List<Long> sids = ids2longList(deletedCards);
            Timber.e("need delete cards num:%s", sids.size());
//            db.execute("DELETE FROM cards WHERE id IN " + sids);
            mCol.remCards(sids);
            mCol.save();
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateDialogProgress(SYNCING_DATA, "删除多余笔记中", mCurrentProgress + 2);
        try {
            JSONArray deletedNotes = item.getJSONObject("notes").getJSONArray("delete");
            long[] sids = ids2longArray(deletedNotes);
//            db.execute("DELETE FROM notes WHERE id IN " + sids);
            mCol.remNotes(sids);
            mCol.save();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject replaceDecks = item.getJSONObject("decks").getJSONObject("replace");
            if (replaceDecks.length() > 0) {
                double percent = 2.0 / replaceDecks.length();
                Iterator<String> it = replaceDecks.keys();
                while (it.hasNext()) {
                    String next = it.next();
                    try {
//                        mCol.getDecks().getDecks().put(Long.parseLong(next), new Deck(replaceDecks.getJSONObject(next)));
                        mCol.getDecks().update(new Deck(replaceDecks.getJSONObject(next)));
                        updateDialogProgress(SYNCING_DATA, "同步牌组数据中", mCurrentProgress + percent);
                    } catch (Exception e) {
                        //只遍历model id
                    }
                }
                mCol.save();
            }

        } catch (Exception e) {
//            e.printStackTrace();
        }
        db.getDatabase().beginTransaction();
        try {
            JSONArray replace = item.getJSONObject("revlog").getJSONArray("replace");
            if (replace.length() > 0) {
                for (int i = 0; i < replace.length(); i++) {
                    log(replace.getJSONArray(i).get(0), replace.getJSONArray(i).get(1), replace.getJSONArray(i).get(2), replace.getJSONArray(i).get(3), replace.getJSONArray(i).get(4), replace.getJSONArray(i).get(5), replace.getJSONArray(i).get(6), replace.getJSONArray(i).get(7), replace.getJSONArray(i).get(8));
                }
                db.getDatabase().setTransactionSuccessful();
                mCol.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.getDatabase().endTransaction();
        }
//        Timber.e("看看是null还是null：%s", (item.getJSONObject("dconf").toString()));
//        Timber.e("看看是null还是null：%s,%s", (item.getJSONObject("dconf").get("replace")==JSONObject.NULL), (item.getJSONObject("dconf").getString("replace").equals("null")));
        try {
            JSONObject replaceDConf = item.getJSONObject("dconf").getJSONObject("replace");
            if (replaceDConf.length() > 0) {
                double percent = 2.0 / replaceDConf.length();
                Iterator<String> it = replaceDConf.keys();
                while (it.hasNext()) {
                    String next = it.next();
                    try {
                        Long.parseLong(next);
                        mCol.getDecks().updateConf(new DeckConfig(replaceDConf.getJSONObject(next)));
                        updateDialogProgress(SYNCING_DATA, "同步牌组配置数据中", mCurrentProgress + percent);
                    } catch (Exception e) {
                        //只遍历model id
                    }
                }
                mCol.save();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            JSONObject replaceModels = item.getJSONObject("models").getJSONObject("replace");
            if (replaceModels.length() > 0) {
                double percent = 5.0 / replaceModels.length();
                Iterator<String> it = replaceModels.keys();
                while (it.hasNext()) {
                    String next = it.next();
                    try {
                        Long.parseLong(next);
                        mCol.getModels().update(new Model(replaceModels.getJSONObject(next)));
                        updateDialogProgress(SYNCING_DATA, "同步模板数据中", mCurrentProgress + percent);
                    } catch (Exception e) {
                        //只遍历model id
                    }
                }
                mCol.save();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        db.getDatabase().beginTransaction();
        try {
            JSONArray replace = item.getJSONObject("notes").getJSONArray("replace");
            if (replace.length() > 0) {
                double percent = mPullNotesPerPercent / replace.length();
                for (int i = 0; i < replace.length(); i++) {
//                String values = replace.getJSONArray(i).toString().replace("[", "").replace("]", "").replaceAll("\"","'").replaceAll("\u001f","\u001f");
//                String sql = "replace into notes(id,guid,mid,mod,usn,tags,flds,sfld,csum,flags,data) values ( " + values + ")";
//
                    db.execute("insert or replace into notes values (?,?,?,?,?,?,?,?,?,?,?)",
                            replace.getJSONArray(i).get(0), replace.getJSONArray(i).get(1), replace.getJSONArray(i).get(2), replace.getJSONArray(i).get(3), replace.getJSONArray(i).get(4), replace.getJSONArray(i).get(5), replace.getJSONArray(i).get(6), replace.getJSONArray(i).get(7), replace.getJSONArray(i).get(8), replace.getJSONArray(i).get(9), replace.getJSONArray(i).get(10));
                    updateDialogProgress(SYNCING_DATA, "同步笔记数据中", mCurrentProgress + percent);
                }
                db.getDatabase().setTransactionSuccessful();
                mCol.save();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.getDatabase().endTransaction();
        }
        db.getDatabase().beginTransaction();
        try {
            JSONArray replace = item.getJSONObject("cards").getJSONArray("replace");
            if (replace.length() > 0) {
                double percent = 5.0 / replace.length();
                for (int i = 0; i < replace.length(); i++) {
//                String values = replace.getJSONArray(i).toString().replace("[", "").replace("]", "").replaceAll("\"","'").replaceAll("\u001f","\u001f");
//                String sql = "replace into cards(id,nid,did,ord,mod,usn,type,queue,due,ivl,factor,reps,lapses,left,odue,odid,flags,data) values ( " + values + ")";
//                Timber.i("update dialog progress:%d", percent);
                    db.execute("insert or replace into cards values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            replace.getJSONArray(i).get(0), replace.getJSONArray(i).get(1), replace.getJSONArray(i).get(2), replace.getJSONArray(i).get(3), replace.getJSONArray(i).get(4), replace.getJSONArray(i).get(5), replace.getJSONArray(i).get(6), replace.getJSONArray(i).get(7), replace.getJSONArray(i).get(8), replace.getJSONArray(i).get(9), replace.getJSONArray(i).get(10), replace.getJSONArray(i).get(11), replace.getJSONArray(i).get(12), replace.getJSONArray(i).get(13), replace.getJSONArray(i).get(14), replace.getJSONArray(i).get(15), replace.getJSONArray(i).get(16), replace.getJSONArray(i).get(17));

                    updateDialogProgress(SYNCING_DATA, "同步卡牌数据中", mCurrentProgress + percent);
                }
                db.getDatabase().setTransactionSuccessful();
                mCol.save();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.getDatabase().endTransaction();
        }

        CollectionHelper.getInstance().unlockCollection();
    }


    protected void log(Object id, Object cid, Object usn, Object ease, Object ivl, Object lastIvl, Object factor, Object timeTaken, Object type) {
        try {
            CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getDb().execute("INSERT INTO revlog VALUES (?,?,?,?,?,?,?,?,?)",
                    id, cid, usn, ease, ivl, lastIvl, factor, timeTaken, type);
        } catch (SQLiteConstraintException e) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e1) {
                throw new RuntimeException(e1);
            }
            log(id, cid, usn, ease, ivl, lastIvl, factor, timeTaken, type);
        }
    }


    private void completeDataSync(String token) {
        RequestBody formBody = new FormBody.Builder()
                .add("session_key", mCurrentSession)
                .build();
        updateDialogProgress(SYNCING_DATA, "同步完成中", 90);
        OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/commit", formBody, token, "", new OKHttpUtil.MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
            }


            @Override
            public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Timber.e("complete sync succeed!");
                    try {
                        final JSONObject object = new JSONObject(response.body().string());
                        Timber.i("object:%s", object);
                        if (object.getInt("status_code") != 0) {
                            updateDialogMessage(SYNCING_ERROR, object.getString("message"));
                            return;
                        }
                        updateDialogProgress(SYNCING_DATA, "保存同步数据中", 92);
                        saveLatestData();
                        mPreferences.edit().putString(Consts.KEY_SYNC_CHINA_SESSION, mCurrentSession).apply();
                        updateDialogProgress(SYNCING_DATA, "同步完成", 100);
                        uploadLocalMediaFileInfo(token);
                        mCallback.onCompletedData();

                    } catch (Exception e) {
                        e.printStackTrace();
                        updateDialogMessage(SYNCING_ERROR, ERROR_DATA);

                    }
                } else {
                    Timber.e("complete sync error, code %d", response.code());
                    updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
                }
            }
        });
    }


    /***
     * 同步结束后将数据写入synclog
     */
    @SuppressLint("DefaultLocale")
    private void saveLatestData() {
        mCol =  CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
        CollectionHelper.getInstance().lockCollection();

        mCol.getDb().getDatabase().beginTransaction();
        try {
            mCol.getDb().execute("delete from synclog");
        }catch (Exception e){
//            mCol=CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
//            mCol.getDb().execute("drop table synclog");
//            mCol.getDb().execute("create table if not exists synclog (" + "    id             integer not null,"
//                    + "    type             integer not null," + "    mod             integer not null" + ")");
            e.printStackTrace();
        }
        List<Deck> newDecks = mCol.getDecks().all();
        StringBuilder values = new StringBuilder();
        for (Deck item : newDecks) {
            if (values.length() != 0) {
                values.append(",");
            }
            values.append("(").append(item.getLong("id")).append(",").append(SYNC_LOG_TYPE_DECKS).append(",").append(item.getLong("mod")).append(")");
        }
//        db.execute(String.format("insert into synclog values %s",sbDeck.toString()));

        List<Model> newModels = mCol.getModels().all();
//        StringBuilder sbModel=new StringBuilder();
        for (Model item : newModels) {
            if (values.length() != 0) {
                values.append(",");
            }
            values.append("(").append(item.getLong("id")).append(",").append(SYNC_LOG_TYPE_MODELS).append(",").append(item.getLong("mod")).append(")");
        }
//        db.execute(String.format("insert into synclog values %s",sbModel.toString()));

        List<DeckConfig> newDConf = mCol.getDecks().allConf();
//        StringBuilder sbDConf=new StringBuilder();
        for (DeckConfig item : newDConf) {
            if (values.length() != 0) {
                values.append(",");
            }
            values.append("(").append(item.getLong("id")).append(",").append(SYNC_LOG_TYPE_DCONF).append(",").append(item.getLong("mod")).append(")");
        }
        String sql = String.format("insert into synclog values %s", values.toString());
        Timber.i("sync to local synclog:%s", sql);
        mCol.getDb().execute(sql);

//        List<Card> newDConf = mCol.getDecks().allConf();
//        for(DeckConfig item:newDConf){
//            if(values.length()!=0)values.append(",");
//            values.append("(").append(item.getLong("id")).append(",").append(SYNC_LOG_TYPE_DCONF).append(",").append(item.getLong("mod")).append(")");
//        }
        mCol.getDb().execute(String.format("insert into synclog select cards.id,%d,cards.mod from cards", SYNC_LOG_TYPE_CARD));
        mCol.getDb().execute(String.format("insert into synclog select notes.id,%d,notes.mod from notes", SYNC_LOG_TYPE_NOTE));
        mCol.getDb().execute(String.format("insert into synclog select revlog.id,%d,revlog.id from revlog", SYNC_LOG_TYPE_REVLOG));
        mCol.getDb().getDatabase().setTransactionSuccessful();
        mCol.getDb().getDatabase().endTransaction();
//        mCol.fixIntegrity(new CollectionTask.ProgressCallback(null, AnkiDroidApp.getAppResources()));
        CollectionHelper.getInstance().unlockCollection();
//        db.execute(String.format("insert into synclog select revlog.id,%d,revlog.mod from revlog", SYNC_LOG_TYPE_REVLOG));

    }


    /**
     * 上传媒体文件信息给服务端，服务端比对完后下发文件的同步结果
     */
    private void uploadLocalMediaFileInfo(String token) {
        //获取本地media文件夹的目录
        updateDialogProgress(SYNCING_MEDIA, "", 1);
        File mediaDir = new File(Media.getCollectionMediaPath(CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getPath()));

//        if (!mediaDir.exists()) {
//            return;
//        }
        File[] files = mediaDir.listFiles();

//        if (files.length == 0) {
//            return;
//        }

        String[] localFiles = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            localFiles[i] = files[i].getName();
        }
        JSONArray filesJson = new JSONArray(localFiles);
//        Timber.i("local file list:%s", filesJson);
        RequestBody formBody = new FormBody.Builder()
                .add("file_list", filesJson.toString())
                .build();
        updateDialogProgress(SYNCING_MEDIA, "", 5);
        if (mCancel) {
            return;
        }
        OKHttpUtil.post(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/postFileInfo", formBody, token, "", new OKHttpUtil.MyCallBack() {
            @Override
            public void onFailure(Call call, IOException e) {
                updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
            }


            @Override
            public void onResponse(Call call, String token, Object arg1, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Timber.e("upload media file info succeed!");
                    try {
                        final JSONObject object = new JSONObject(response.body().string());
                        Timber.e("fetch media sync info from server:%s", object.toString());
                        if (object.getInt("status_code") != 0) {
                            updateDialogMessage(SYNCING_ERROR, object.getString("message"));
                            return;
                        }
                        final JSONObject item = object.getJSONObject("data");
                        updateDialogProgress(SYNCING_MEDIA, "", 10);
                        handleMediaSync(item, token);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Timber.e("upload media file info error, code %d", response.code());
                }
            }
        });
    }


    //进度：上传占45%，下载占30%。删除占10%
    private void handleMediaSync(JSONObject item, String token) {
        if (mCancel) {
            return;
        }
        mCol =  CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance());
        CollectionHelper.getInstance().lockCollection();
        try {
            JSONArray needDelete = item.getJSONArray("delete");
            if (needDelete.length() > 0) {
                double percent = 10.0 / needDelete.length();
                for (int i = 0; i < needDelete.length(); i++) {
                    String path = needDelete.getString(i);
                    new File(Media.getCollectionMediaPath(mCol.getPath()), path).delete();
                    updateDialogProgress(SYNCING_MEDIA, "", mCurrentProgress + percent);
                }
                mCol.save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (mCancel) {
            return;
        }
        try {
            JSONArray needUpload = item.getJSONArray("upload");
            Timber.i("there are %d file need to upload", needUpload.length());
            if (needUpload.length() > 0) {
                mTotalNeedUploadCount = needUpload.length();
                mUploadResultEndCount = 0;
                Map<String, Boolean> needUploadFileRecord = new HashMap<>();
                double percent = 45.0 / needUpload.length();
                for (int i = 0; i < needUpload.length(); i++) {
//                    Timber.i("this file need to upload: %s", needUpload.getString(i));
                    //上传文件
                    needUploadFileRecord.put(needUpload.getString(i), false);
                }

                uploadFileAsync(needUploadFileRecord, token, percent);
            } else {
                mTotalNeedUploadCount = mUploadResultEndCount = 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            JSONArray needDownload = item.getJSONArray("download_url");
            if (needDownload.length() > 0) {
                String[] paths = new String[needDownload.length()];
                for (int i = 0; i < needDownload.length(); i++) {
                    paths[i] = needDownload.getString(i);
                }
                mTotalNeedDownloadCount = needDownload.length();
                mDownloadResultEndCount = 0;
                //下载文件
                double percent = 30.0 / paths.length;
                for (String path : paths) {
                    String savePath = Media.getCollectionMediaPath(CollectionHelper.getCollectionPath(mContext)) + path.substring(path.lastIndexOf("/"));
                    OKHttpUtils.downloadAndSaveFile(path, savePath, path, new DownloadProgress(  savePath, percent), new OKHttpUtils.MyCallBack() {
                        @Override
                        public void onFailure(Call call, final IOException e) {
                            updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
                            mDownloadResultEndCount++;
                        }


                        @Override
                        public void onResponse(Call call, Object tag, Response response) throws IOException {

                        }
                    });
                }

            } else {
                mTotalNeedDownloadCount = mDownloadResultEndCount = 0;
                maybeCompleted();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        CollectionHelper.getInstance().unlockCollection();
    }


    private int mUploadResultEndCount;
    private boolean mUploadMediaEnd;
    private int mTotalNeedUploadCount;


    private File zipAllCollection() {
        File f = new File(CollectionHelper.getCollectionPath(mContext).replaceFirst("collection\\.anki2$", "tempAllSyncData" + System.currentTimeMillis() / 1000 + ".zip"));
        try (ZipOutputStream z = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(f)))) {
            z.setMethod(ZipOutputStream.DEFLATED);
            byte[] buffer = new byte[2048];
            try {
                File file = new File(CollectionHelper.getCollectionPath(mContext));
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file), 2048);
                z.putNextEntry(new ZipEntry(file.getName()));
                int count;
                while ((count = bis.read(buffer, 0, 2048)) != -1) {
                    z.write(buffer, 0, count);
                }
                z.closeEntry();
                bis.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            z.closeEntry();
            // Don't leave lingering temp files if the VM terminates.
            f.deleteOnExit();
        } catch (IOException e) {
            Timber.e(e, "Failed to create all data  zip: ");
            throw new RuntimeException(e);
        }
        return f;
    }


    private void uploadFileAsync(Map<String, Boolean> needUploadFileRecord, String token, double percent) {
        if (needUploadFileRecord.size() == 0) {
            return;
        }
        Pair<File, List<String>> changesZip = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getMedia().mediaNeedUploadZip2AnkiChina(needUploadFileRecord);
        File zip = changesZip.first;
        List<String> fnames = changesZip.second;
        if (fnames.size() != 0) {
            List<String> zipPath = new ArrayList<>();
            zipPath.add(zip.getAbsolutePath());
            OKHttpUtils.doPostRequest(Consts.ANKI_CHINA_BASE + Consts.API_VERSION + "napi/sync/uploadFile", mCurrentSession, zipPath, uploadProgress, zip.getAbsolutePath(), token, new OKHttpUtils.MyCallBack() {
                @Override
                public void onFailure(Call call, final IOException e) {
                    Timber.i("upload error------>%s %s", zip.getAbsolutePath(), e.getMessage());
                    updateDialogMessage(SYNCING_ERROR, ERROR_NETWORK);
                    mUploadResultEndCount++;
//                    maybeCompleted();
                }


                @Override
                public void onResponse(Call call, Object tag, Response response) throws IOException {
                    final JSONObject object = new JSONObject(response.body().string());
                    Timber.e("upload media result:%s", object.toString());
                    Timber.i("upload media result---->%s", tag);
                    for (String s : fnames) {
                        needUploadFileRecord.put(s, true);
                        mUploadResultEndCount++;
                        updateDialogProgress(SYNCING_MEDIA, "", mCurrentProgress + percent);
                    }
                    zip.delete();
                    if (!maybeCompleted()) {
                        uploadFileAsync(needUploadFileRecord, token, percent);
                    }
                }
            });
        }
    }


    final ProgressListener uploadProgress = (bytesRead, contentLength, done) -> {
        if (done) {
            Timber.i("upload file done");
        }
    };


    private boolean maybeCompleted() {
        mDownloadMediaEnd = mTotalNeedDownloadCount == mDownloadResultEndCount;
        mUploadMediaEnd = mTotalNeedUploadCount == mUploadResultEndCount;
        Timber.i("need download:%d,downloaded:%d,need upload:%d,uploaded:%d", mTotalNeedDownloadCount, mDownloadResultEndCount, mTotalNeedUploadCount, mUploadResultEndCount);
        if (mDownloadMediaEnd && mUploadMediaEnd) {
            updateDialogProgress(SYNCING_COMPLETED, "", 100);
        }
        return mDownloadMediaEnd && mUploadMediaEnd;
    }


    private int mDownloadResultEndCount;
    private int mTotalNeedDownloadCount;

    private boolean mDownloadMediaEnd;



    class DownloadProgress implements ProgressListener {
        String fileName;

        private double percent;


        DownloadProgress(  String fileName, double percent) {

            this.fileName = fileName;
            this.percent = percent;
        }


        @Override
        public void onProgress(long currentBytes, long contentLength, boolean done) {
            if (done) {
                Timber.i("%s download file done", fileName);
                updateDialogProgress(SYNCING_MEDIA, "", mCurrentProgress + percent);
                downloadFiles(fileName);
            }

        }


        private void downloadFiles(String zipFile) {
            try {
                ZipFile zipData = new ZipFile(new File(zipFile));
                int cnt = CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).getMedia().addFilesFromZipFromAnkiChina(zipData);
//                mDownloadCount += cnt;
                CollectionHelper.getInstance().getColSafe(AnkiDroidApp.getInstance()).log("received " + cnt + " files");
                // NOTE: The python version uses slices which return an empty list when indexed beyond what
                // the list contains. Since we can't slice out an empty sublist in Java, we must check
                // if we've reached the end and clear the fnames list manually.
//                if (cnt == fnames.size()) {
//                    fnames.clear();
//                } else {
//                    fnames = fnames.subList(cnt, fnames.size());
//                }
//                mCon.publishProgress(String.format(
//                        AnkiDroidApp.getAppResources().getString(R.string.sync_media_downloaded_count), mDownloadCount));
            } catch (IOException e) {
                Timber.e(e, "Error downloading media files");
            } finally {
                mDownloadResultEndCount++;
                maybeCompleted();
            }
        }

    }


    public static long[] ids2longArray(JSONArray ids) {
        long[] idArray = new long[ids.length()];
        for (int i = 0; i < ids.length(); i++) {
            idArray[i] = Long.parseLong(ids.getString(i));

        }
        return idArray;
    }


    public static List<Long> ids2longList(JSONArray ids) {
        List<Long> idArray = new ArrayList<>();
        for (int i = 0; i < ids.length(); i++) {
            idArray.add(Long.parseLong(ids.getString(i)));

        }
        return idArray;
    }


    public static String ids2str(JSONArray ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (ids != null) {
            String[] idArray = new String[ids.length()];
            for (int i = 0; i < ids.length(); i++) {
                idArray[i] = ids.getString(i);

            }
            String s = Arrays.toString(idArray);
            sb.append(s.substring(1, s.length() - 1));
        }
        sb.append(")");
        return sb.toString();
    }


    public static String ids2str(String[] ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        if (ids != null) {
            String s = Arrays.toString(ids);
            sb.append(s.substring(1, s.length() - 1));
        }
        sb.append(")");
        return sb.toString();
    }


    public static String strArray2jsonArray(String[] ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (ids != null) {
            String s = Arrays.toString(ids);
            sb.append(s.substring(1, s.length() - 1));
        }
        sb.append("]");
        return sb.toString();
    }


}
