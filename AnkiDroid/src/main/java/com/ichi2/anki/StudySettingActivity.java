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
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.libanki.Collection;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.DeckConfig;
import com.ichi2.preferences.StepsPreference;
import com.ichi2.ui.CustomStyleDialog;
import com.ichi2.utils.JSONArray;
import com.ichi2.utils.JSONObject;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import timber.log.Timber;

import static com.ichi2.anki.StudyOptionsFragment.getDeckIds;
import static com.ichi2.async.CollectionTask.TASK_TYPE.CONF_RESET;
import static com.ichi2.async.CollectionTask.TASK_TYPE.REORDER;
import static com.ichi2.preferences.StepsPreference.convertToJSON;


public class StudySettingActivity extends AnkiActivity implements View.OnClickListener {
    private TextView tx_max_learn_card, tx_max_review_card, tx_new_card_sequence, tx_learn_sequence, tx_interval_step, tx_interval_graduate, tx_interval_simple,tx_max_interval, tx_init_level, tx_medal_simple,
            tx_interval_decoration, tx_error_interval_step, tx_error_new_interval, tx_error_min_interval, tx_algorithm;
    private RelativeLayout rl_max_learn_card, rl_max_review_card, rl_new_card_sequence, rl_learn_sequence, rl_interval_step, rl_interval_graduate,rl_max_interval, rl_interval_simple, rl_init_level, rl_medal_simple,
            rl_interval_decoration, rl_error_interval_step, rl_error_new_interval, rl_error_min_interval;
    private LinearLayout ll_algorithm;


    @Override
    protected boolean isStatusBarTransparent() {
        return true;
    }


    @Override
    protected int getStatusBarColorAttr() {
        return R.attr.reviewStatusBarColor;
    }


    private DeckConfig mOptions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_study_setting);
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
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setText("学习设置");
            ((TextView) toolbar.findViewById(R.id.toolbar_title)).setTextColor(ta.getColor(1, ContextCompat.getColor(this, R.color.black)));
            ta.recycle();
            // Decide which action to take when the navigation button is tapped.
//            toolbar.setNavigationOnClickListener(v -> onNavigationPressed());
        }
        tx_max_learn_card = findViewById2(R.id.tx_max_learn_card);
        tx_max_review_card = findViewById2(R.id.tx_max_review_card);
        tx_new_card_sequence = findViewById2(R.id.tx_new_card_sequence);
        tx_learn_sequence = findViewById2(R.id.tx_learn_sequence);
        tx_interval_step = findViewById2(R.id.tx_interval_step);
        tx_interval_graduate = findViewById2(R.id.tx_interval_graduate);
        tx_interval_simple = findViewById2(R.id.tx_interval_simple);
        tx_max_interval = findViewById2(R.id.tx_deck_conf_max_ivl);
        tx_init_level = findViewById2(R.id.tx_init_level);
        tx_medal_simple = findViewById2(R.id.tx_medal_simple);
        tx_interval_decoration = findViewById2(R.id.tx_interval_decoration);
        tx_error_interval_step = findViewById2(R.id.tx_error_interval_step);
        tx_error_new_interval = findViewById2(R.id.tx_error_new_interval);
        tx_error_min_interval = findViewById2(R.id.tx_error_min_interval);
        tx_algorithm = findViewById2(R.id.tx_algorithm);

        rl_max_learn_card = findViewById2(R.id.rl_max_learn_card);
        rl_max_review_card = findViewById2(R.id.rl_max_review_card);
        rl_new_card_sequence = findViewById2(R.id.rl_new_card_sequence);
        rl_learn_sequence = findViewById2(R.id.rl_learn_sequence);
        rl_interval_step = findViewById2(R.id.rl_interval_step);
        rl_interval_graduate = findViewById2(R.id.rl_interval_graduate);
        rl_interval_simple = findViewById2(R.id.rl_interval_simple);
        rl_max_interval = findViewById2(R.id.rl_deck_conf_max_ivl);
        rl_init_level = findViewById2(R.id.rl_init_level);
        rl_medal_simple = findViewById2(R.id.rl_medal_simple);
        rl_interval_decoration = findViewById2(R.id.rl_interval_decoration);
        rl_error_interval_step = findViewById2(R.id.rl_error_interval_step);
        rl_error_new_interval = findViewById2(R.id.rl_error_new_interval);
        rl_error_min_interval = findViewById2(R.id.rl_error_min_interval);
        ll_algorithm = findViewById2(R.id.ll_algorithm);

        setTitle("学习设置");
        initOption();
    }


    private Collection mCol;
    private Deck mDeck;
    private long mDeckId;
    private String mDeckIdStr;


    private void initOption() {
        mCol = CollectionHelper.getInstance().getCol(this);
        if (mCol == null) {
            finishActivityWithFade(this);
            return;
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey("did")) {
            mDeck = mCol.getDecks().get(extras.getLong("did"));
        } else {
            mDeck = mCol.getDecks().current();
        }
        mDeckId = mDeck.getLong("id");
        mDeckIdStr = String.valueOf(mDeckId);
        mOptions = mCol.getDecks().confForDid(mDeckId);
        if (mDeck.getLong("conf") == 1 && mDeckId != 1) {
            long id = mCol.getDecks().confId(mDeck.getString("name"), mOptions.toString());
            mDeck.put("conf", id);
            mCol.getDecks().save(mDeck);
            mOptions = mCol.getDecks().confForDid(mDeckId);
        }


        buildList();
        updateValues();
    }


    private Map<Integer, String> mNewOrderMap = new HashMap<>();
    private String[] mNewOrderValues;
    private String[] mNewOrderLabels;
    private int mCurrentNewOrderValue;


    private Map<Integer, String> mStudyPreferenceMap = new HashMap<>();
    private String[] mStudyPreferenceValues;
    private String[] mStudyPreferenceLabels;
    private int mCurrentStudyPreferenceValue;

    private Map<Integer, String> mMindModeMap = new HashMap<>();
    private String[] mMindModeValues;
    private String[] mMindModeLabels;
    private String[] mMindModeHints;
    private int mCurrentMindModeValue;


    private void buildList() {
        mNewOrderValues = getResources().getStringArray(R.array.new_order_values);
        mNewOrderLabels = getResources().getStringArray(R.array.new_order_labels);
        for (int i = 0; i < mNewOrderValues.length; i++) {
            mNewOrderMap.put(Integer.valueOf(mNewOrderValues[i]), mNewOrderLabels[i]);
        }

        mStudyPreferenceValues = getResources().getStringArray(R.array.new_spread_values);
        mStudyPreferenceLabels = getResources().getStringArray(R.array.new_spread_labels);
        for (int i = 0; i < mStudyPreferenceValues.length; i++) {
            mStudyPreferenceMap.put(Integer.valueOf(mStudyPreferenceValues[i]), mStudyPreferenceLabels[i]);
        }

        mMindModeValues = getResources().getStringArray(R.array.mind_mode_values);
        mMindModeLabels = getResources().getStringArray(R.array.mind_mode_labels);
        mMindModeHints = getResources().getStringArray(R.array.mind_mode_hint);
        for (int i = 0; i < mMindModeValues.length; i++) {
            mMindModeMap.put(Integer.valueOf(mMindModeValues[i]), mMindModeLabels[i]);
        }
    }


    private int mSavedMaxNewCardNum,mSavedMaxRevCardNum;
    private void updateValues() {
        // new
        JSONObject newOptions = mOptions.getJSONObject("new");
        tx_max_learn_card.setText(newOptions.getString("perDay"));
        mCurrentNewOrderValue = Integer.parseInt(newOptions.getString("order"));
        tx_new_card_sequence.setText(mNewOrderMap.get(mCurrentNewOrderValue));
        tx_interval_step.setText(StepsPreference.convertFromJSON(newOptions.getJSONArray("delays")));
        tx_interval_graduate.setText(newOptions.getJSONArray("ints").getString(0));//毕业间隔
        tx_interval_simple.setText(newOptions.getJSONArray("ints").getString(1));//简单按钮间隔
        tx_init_level.setText(String.valueOf(newOptions.getInt("initialFactor") / 10));//初始难度

        // rev
        JSONObject revOptions = mOptions.getJSONObject("rev");
        tx_max_review_card.setText(revOptions.getString("perDay"));
        mCurrentStudyPreferenceValue = mCol.getConf().getInt("newSpread");//唯一一个全局设置
        tx_learn_sequence.setText(mStudyPreferenceMap.get(mCurrentStudyPreferenceValue));
        tx_medal_simple.setText(String.valueOf((int) Math.round((revOptions.getDouble("ease4") * 100))));
        Timber.i("ivl fct:" + revOptions.getDouble("ivlFct"));
        tx_interval_decoration.setText(String.valueOf((int) Math.round(revOptions.getDouble("ivlFct") * 100)));
        tx_max_interval.setText(revOptions.getString("maxIvl"));

        // lapses
        JSONObject lapOptions = mOptions.getJSONObject("lapse");
        tx_error_interval_step.setText(StepsPreference.convertFromJSON(lapOptions.getJSONArray("delays")));
        tx_error_new_interval.setText(String.valueOf((int) Math.round(lapOptions.getDouble("mult") * 100)));
        tx_error_min_interval.setText(lapOptions.getString("minInt"));


        SharedPreferences sharedPreferences = getSharedPreferences(STUDY_SETTING, 0);
//        mCurrentMindModeValue = sharedPreferences.getInt(KEY_MIND_MODE, 0);
        String savedMindModeValue = sharedPreferences.getString(KEY_MIND_MODE, "");

        Map<String, Integer> map = null;
        try {
            Gson gson = new Gson();
            map = gson.fromJson(savedMindModeValue, new TypeToken<Map<String, Integer>>() {
            }.getType());
        } catch (Exception e) {
            e.printStackTrace();
        }
//        Timber.i("我来取数据了 "+ map.get(mDeckIdStr));
        mCurrentMindModeValue = map != null && map.get(mDeckIdStr) != null ? map.get(mDeckIdStr).intValue() : 0;
        tx_algorithm.setText(mMindModeMap.get(mCurrentMindModeValue));
    }


    public static final String STUDY_SETTING = "STUDY_SETTING";
    public static String KEY_MIND_MODE = "KEY_MIND_MODE";

    public static String KEY_STOPPED = "KEY_STOPPED";


    public <T extends View> T findViewById2(@IdRes int id) {
        T view = findViewById(id);
        if (!(view instanceof TextView)) {
            view.setOnClickListener(this);
        }

        return view;
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        CustomStyleDialog customDialog = null;
        switch (id) {
            case R.id.rl_max_learn_card:

                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("每日新卡上限")
                        .setEditorText(tx_max_learn_card.getText().toString(), "学习一段时间后，未来每天新卡+复习卡约180张，大约需30分钟")
                        .addSingleTextChangedListener(new CustomStyleDialog.Builder.MyTextWatcher() {

                            @Override
                            public void beforeTextChanged(Dialog dialog, CharSequence s, int start, int count, int after) {

                            }


                            @Override
                            public void onTextChanged(Dialog dialog, CharSequence s, int start, int before, int count) {
                                if (s.toString().isEmpty()) {
                                    ((CustomStyleDialog) dialog).getSingleEditorModeHintView().setText("未来每天学习量=新卡数x6");
                                } else {
                                    try{
                                        int num = Integer.parseInt(s.toString()) * 6;
                                        int time = num * 10 / 60;
                                        ((CustomStyleDialog) dialog).getSingleEditorModeHintView().setText(String.format("学习一段时间后，未来每天新卡+复习卡约%s张，大约需%s分钟", num, time));
                                    }catch (Exception e) {
                                        e.printStackTrace();
                                        UIUtils.showThemedToast(StudySettingActivity.this, "请填写0至9999之间的数值",
                                                false);
                                    }

                                }
                            }


                            @Override
                            public void afterTextChanged(Dialog dialog, Editable s) {

                            }
                        })
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 0 && num <= 9999) {
                                    mOptions.getJSONObject("new").put("perDay", num);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写0至9999之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写0至9999之间的数值",
                                        false);
                            }

                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_max_review_card:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("每日复习数上限")
                        .setEditorText(tx_max_review_card.getText().toString(), "建议设置为最大9999，有多少复习多少")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 0 && num <= 9999) {
                                    mOptions.getJSONObject("rev").put("perDay", num);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写0至9999之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写0至9999之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_deck_conf_max_ivl:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("最大时间间隔")
                        .setEditorText(tx_max_review_card.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 0 && num <= 99999) {
                                    mOptions.getJSONObject("rev").put("maxIvl", num);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写0至99999之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写0至99999之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_interval_step:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("间隔的步伐(以分钟计)")
                        .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT)
                        .setEditorText(tx_interval_step.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            String num = ((CustomStyleDialog) dialog).getEditorText();
                            String validated = getValidatedStepsInput(num);
                            if (validated == null) {
                                UIUtils.showThemedToast(this, getResources().getString(R.string.steps_error), false);
                            } else if (TextUtils.isEmpty(validated)) {
                                UIUtils.showThemedToast(this, getResources().getString(R.string.steps_min_error),
                                        false);
                            } else {
                                mOptions.getJSONObject("new").put("delays", convertToJSON(num));
                                saveAndUpdateValues();
                                Timber.i("edit num:%s", num);
                                dialog.dismiss();
                            }

                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_interval_graduate:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("毕业时间间隔(以天计)")
                        .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT)
                        .setEditorText(tx_interval_graduate.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 1 && num <= 99) {
                                    JSONArray newInts = new JSONArray(); // [graduating, easy]
                                    newInts.put(num);
                                    newInts.put(mOptions.getJSONObject("new").getJSONArray("ints").getInt(1));
                                    mOptions.getJSONObject("new").put("ints", newInts);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_interval_simple:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("简单按钮对应的时间间隔")
                        .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT)
                        .setEditorText(tx_interval_simple.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 1 && num <= 99) {
                                    JSONArray newInts = new JSONArray();
                                    newInts.put(mOptions.getJSONObject("new").getJSONArray("ints").getInt(0));//
                                    newInts.put(num);
                                    mOptions.getJSONObject("new").put("ints", newInts);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_init_level:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("初始难度")
                        .setEditorText(tx_init_level.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 100 && num <= 999) {
                                    mOptions.getJSONObject("new").put("initialFactor", num * 10);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写100至999之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_new_card_sequence:
                final int[] selectPosition = {mCurrentNewOrderValue};
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("新卡顺序")
                        .setSelectListModeCallback(new CustomStyleDialog.Builder.SelectListModeCallback() {
                            @Override
                            public String[] getItemContent() {
                                return mNewOrderLabels;
                            }


                            @Override
                            public String[] getItemHint() {
                                return new String[0];
                            }


                            @Override
                            public int getDefaultSelectedPosition() {
                                return selectPosition[0];
                            }


                            @Override
                            public void onItemSelect(int position) {
                                selectPosition[0] = position;
                            }
                        })
                        .setPositiveButton("确认", (dialog, which) -> {
                            int oldValue = mOptions.getJSONObject("new").getInt("order");
                            if (oldValue != selectPosition[0]) {
                                mOptions.getJSONObject("new").put("order", selectPosition[0]);
                                CollectionTask.launchCollectionTask(REORDER, new ConfChangeHandler(StudySettingActivity.this),
                                        new TaskData(new Object[] {mOptions}));
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                customDialog.show();
                break;
            case R.id.rl_learn_sequence:
                final int[] selectPosition3 = {mCurrentStudyPreferenceValue};
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("学习顺序")
                        .setSelectListModeCallback(new CustomStyleDialog.Builder.SelectListModeCallback() {
                            @Override
                            public String[] getItemContent() {
                                return mStudyPreferenceLabels;
                            }


                            @Override
                            public String[] getItemHint() {
                                return new String[0];
                            }


                            @Override
                            public int getDefaultSelectedPosition() {
                                return selectPosition3[0];
                            }


                            @Override
                            public void onItemSelect(int position) {
                                selectPosition3[0] = position;
                            }
                        })
                        .setPositiveButton("确认", (dialog, which) -> {
                            int oldValue = mCol.getConf().getInt("newSpread");
                            if (oldValue != selectPosition3[0]) {
                                mCol.getConf().put("newSpread", selectPosition3[0]);
                                mCol.setMod();
                                CollectionTask.launchCollectionTask(REORDER, new ConfChangeHandler(StudySettingActivity.this),
                                        new TaskData(new Object[] {mOptions}));
                            }
                            dialog.dismiss();
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                customDialog.show();
                break;
            case R.id.rl_medal_simple:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("回答简单的时间间隔增加比例")
                        .setEditorText(tx_medal_simple.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try {
                                int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                                Timber.i("edit num:%s", num);
                                if (num >= 100 && num <= 1000) {
                                    mOptions.getJSONObject("rev").put("ease4", (Integer) num / 100.0f);
                                    saveAndUpdateValues();
                                    dialog.dismiss();
                                } else {
                                    UIUtils.showThemedToast(this, "请填写100至1000之间的数值",
                                            false);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写100至1000之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_interval_decoration:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("时间间隔因子")
                        .setEditorText(tx_interval_decoration.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try{
                            int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                            Timber.i("edit num: " + num / 100.0f);
                            if (num >= 0 && num <= 999) {
                                mOptions.getJSONObject("rev").put("ivlFct", num / 100.0f);
                                saveAndUpdateValues();
                                dialog.dismiss();
                            } else {
                                UIUtils.showThemedToast(this, "请填写0至999之间的数值",
                                        false);
                            } }catch (Exception e) {
                                e.printStackTrace();
                                UIUtils.showThemedToast(this, "请填写0至999之间的数值",
                                        false);
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_error_interval_step:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("间隔的步伐(以分钟计)")
                        .setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_CLASS_TEXT)
                        .setEditorText(tx_error_interval_step.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            String num = ((CustomStyleDialog) dialog).getEditorText();
                            String validated = getValidatedStepsInput(num);
                            if (validated == null) {
                                UIUtils.showThemedToast(this, getResources().getString(R.string.steps_error), false);
                            } else {
                                mOptions.getJSONObject("lapse")
                                        .put("delays", StepsPreference.convertToJSON(num));
                                saveAndUpdateValues();
                                Timber.i("edit num:%s", num);
                                dialog.dismiss();
                            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_error_new_interval:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("新的时间间隔")
                        .setEditorText(tx_error_new_interval.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try{
                            int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                            Timber.i("edit num:%s", num);
                            if (num >= 0 && num <= 100) {
                                mOptions.getJSONObject("lapse").put("mult", num / 100.0f);
                                saveAndUpdateValues();
                                dialog.dismiss();
                            } else {
                                UIUtils.showThemedToast(this, "请填写0至100之间的数值",
                                        false);
                            }}catch (Exception e) {
                e.printStackTrace();
                UIUtils.showThemedToast(this, "请填写0至100之间的数值",
                        false);
            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.rl_error_min_interval:
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("最小时间间隔(天)")
                        .setEditorText(tx_error_min_interval.getText().toString(), "")
                        .setPositiveButton("确认", (dialog, which) -> {
                            try{
                            int num = Integer.parseInt(((CustomStyleDialog) dialog).getEditorText());
                            Timber.i("edit num:%s", num);
                            if (num >= 1 && num <= 99) {
                                mOptions.getJSONObject("lapse").put("minInt", num);
                                saveAndUpdateValues();
                                dialog.dismiss();
                            } else {
                                UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                                        false);
                            }}catch (Exception e) {
                e.printStackTrace();
                UIUtils.showThemedToast(this, "请填写1至99之间的数值",
                        false);
            }
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                Timber.i("build a dialog!");
                customDialog.show();
                break;
            case R.id.ll_algorithm:
                final int[] selectPosition4 = {mCurrentMindModeValue};
                customDialog = new CustomStyleDialog.Builder(this)
                        .setTitle("选择记忆模式")
                        .setSelectListModeCallback(new CustomStyleDialog.Builder.SelectListModeCallback() {
                            @Override
                            public String[] getItemContent() {
                                return mMindModeLabels;
                            }


                            @Override
                            public String[] getItemHint() {
                                return mMindModeHints;
                            }


                            @Override
                            public int getDefaultSelectedPosition() {
                                return selectPosition4[0];
                            }


                            @Override
                            public void onItemSelect(int position) {
                                selectPosition4[0] = position;
                            }
                        })
                        .setPositiveButton("确认", (dialog, which) -> {
                            SharedPreferences sharedPreferences = getSharedPreferences(STUDY_SETTING, 0);
                            String oldValue = sharedPreferences.getString(KEY_MIND_MODE, "");
//                            Timber.i("看看保存成什么样子了 %s ", oldValue);
                            Map<String, Integer> oldMap = null;
                            Gson gson = new Gson();
                            try {
                                oldMap = gson.fromJson(oldValue, Map.class);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (oldMap == null) {
                                oldMap = new HashMap<>();
                            }
                            for (long did : getDeckIds(mDeckId, getCol())) {
//                                Timber.i("看看都是什么id %s,%s", did, selectPosition4[0]);
                                oldMap.put(String.valueOf(did), selectPosition4[0]);
                            }
                            String newValue = gson.toJson(oldMap);
                            sharedPreferences.edit().putString(KEY_MIND_MODE, newValue).apply();
                            mSavedMaxNewCardNum = mOptions.getJSONObject("new").getInt("perDay");
                            mSavedMaxRevCardNum= mOptions.getJSONObject("rev").getInt("perDay");
                            CollectionTask.launchCollectionTask(CONF_RESET, new ConfChangeHandler(StudySettingActivity.this, selectPosition4[0]),
                                    new TaskData(new Object[] {mOptions}));//先恢复默认，即长记模式
                            dialog.dismiss();
                        })
                        .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                        .create();
                customDialog.show();
                break;
        }


    }


    private void saveAndUpdateValues() {
        saveAndUpdateValues(true);
    }


    private void saveAndUpdateValues(boolean refreshOption) {
        if (!refreshOption) {
            updateValues();
        }
        mPreferenceChanged = true;
        try {
            getCol().getDecks().save(mOptions);
        } catch (RuntimeException e) {
            Timber.e(e, "DeckOptions - RuntimeException on saving conf");
            AnkiDroidApp.sendExceptionReport(e, "DeckOptionsSaveConf");
        }
        if (refreshOption) {
            initOption();
        }

    }


    private String getValidatedStepsInput(String steps) {
        JSONArray stepsAr = convertToJSON(steps);
        if (stepsAr == null) {
            return null;
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stepsAr.length(); i++) {
                sb.append(stepsAr.getString(i)).append(" ");
            }
            return sb.toString().trim();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
            Timber.i("DeckOptions - onBackPressed()");
            closeWithResult();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private boolean mPreferenceChanged = false;


    private void closeWithResult() {
        if (mPreferenceChanged) {
            setResult(RESULT_OK);
        } else {
            setResult(RESULT_CANCELED);
        }
        finishActivityWithFade(this, ActivityTransitionAnimation.FADE);
    }


    private static class ConfChangeHandler extends TaskListenerWithContext<StudySettingActivity> {
        public ConfChangeHandler(StudySettingActivity deckPreferenceHack) {
            super(deckPreferenceHack);
        }


        int selectMindMode = -1;


        public ConfChangeHandler(StudySettingActivity deckPreferenceHack, int selectMindMode) {
            super(deckPreferenceHack);
            this.selectMindMode = selectMindMode;
        }


        @Override
        public void actualOnPreExecute(@NonNull StudySettingActivity deckPreferenceHack) {

        }


        @Override
        public void actualOnPostExecute(@NonNull StudySettingActivity deckPreferenceHack, TaskData result) {

            if (selectMindMode < 0) {
                deckPreferenceHack.saveAndUpdateValues();
            }else {
                deckPreferenceHack.saveAndUpdateValues(true);
                  if (selectMindMode == 0) {

                    return;
                } else if (selectMindMode == 1) {
                    deckPreferenceHack.mOptions.getJSONObject("new").put("delays", convertToJSON("1 5 10"));
                    JSONArray newInts = new JSONArray();
                    newInts.put(deckPreferenceHack.mOptions.getJSONObject("new").getJSONArray("ints").getInt(0));//
                    newInts.put(3);
                    deckPreferenceHack.mOptions.getJSONObject("new").put("ints", newInts);
                    deckPreferenceHack.mOptions.getJSONObject("rev").put("ivlFct", 70 / 100.0f);
                    deckPreferenceHack.mOptions.getJSONObject("rev").put("maxIvl", 10);
                    deckPreferenceHack.saveAndUpdateValues();
                } else if (selectMindMode == 2) {
                    deckPreferenceHack.mOptions.getJSONObject("new").put("delays", convertToJSON("1 3 7 10 15 20"));
                    JSONArray newInts = new JSONArray();
                    newInts.put(deckPreferenceHack.mOptions.getJSONObject("new").getJSONArray("ints").getInt(0));//
                    newInts.put(1);
                    deckPreferenceHack.mOptions.getJSONObject("new").put("ints", newInts);
                    deckPreferenceHack.mOptions.getJSONObject("rev").put("maxIvl", 1);
                }
                deckPreferenceHack.mOptions.getJSONObject("new").put("perDay",deckPreferenceHack.mSavedMaxNewCardNum);
                deckPreferenceHack.mOptions.getJSONObject("rev").put("perDay",deckPreferenceHack.mSavedMaxRevCardNum);
                deckPreferenceHack.saveAndUpdateValues();
            }

        }
    }

}
