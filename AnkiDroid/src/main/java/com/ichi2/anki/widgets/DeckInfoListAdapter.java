/****************************************************************************************
 * Copyright (c) 2015 Houssam Salem <houssam.salem.au@gmail.com>                        *
 *                                                                                      *
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

package com.ichi2.anki.widgets;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.tabs.TabLayout;
import com.ichi2.anim.ActivityTransitionAnimation;
import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.AnkiDroidApp;
import com.ichi2.anki.CardBrowser;
import com.ichi2.anki.CardUtils;
import com.ichi2.anki.CollectionHelper;
import com.ichi2.anki.DeckPicker;
import com.ichi2.anki.Previewer;
import com.ichi2.anki.R;
import com.ichi2.anki.SelfStudyActivity;
import com.ichi2.anki.StudySettingActivity;
import com.ichi2.anki.WebViewActivity;
import com.ichi2.async.CollectionTask;
import com.ichi2.async.TaskData;
import com.ichi2.async.TaskListener;
import com.ichi2.async.TaskListenerWithContext;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Card;
import com.ichi2.libanki.Collection;

import com.ichi2.libanki.Consts;
import com.ichi2.libanki.Deck;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.libanki.stats.Stats;
import com.ichi2.utils.AESUtil;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.utils.CornerTransform;
import com.ichi2.utils.HtmlUtils;
import com.ichi2.utils.JSONObject;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ichi2.anki.AbstractFlashcardViewer.DECK_OPTIONS;
import static com.ichi2.anki.SelfStudyActivity.PREVIEW_CARDS;
import static com.ichi2.anki.SelfStudyActivity.getPositionMap;
import static com.ichi2.anki.StudyOptionsFragment.KEY_STRUCT_INIT;
import static com.ichi2.anki.widgets.CardsListAdapter.getFlagRes;
import static com.ichi2.async.CollectionTask.TASK_TYPE.DISMISS_MULTI;
import static com.ichi2.async.CollectionTask.TASK_TYPE.SEARCH_CARDS;

public class DeckInfoListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /* Make the selected deck roughly half transparent if there is a background */
    public static final double SELECTED_DECK_ALPHA_AGAINST_BACKGROUND = 0.45;

    private LayoutInflater mLayoutInflater;
    private List<AbstractDeckTreeNode> mDeckList;
    private int mZeroCountColor;
    private int mNewCountColor;
    private int mLearnCountColor;
    private int mReviewCountColor;
    private int mRowCurrentDrawable;
    private int mDeckNameDefaultColor;
    private int mDeckNameDefaultColorChild;
    private int mDeckNameDynColor;
    private Drawable mExpandImage;
    private Drawable mCollapseImage;
    private Drawable mNoExpander = new ColorDrawable(Color.TRANSPARENT);

    // Listeners
    private View.OnClickListener mDeckClickListener;
    private View.OnLongClickListener mDeckLongClickListener;
    private View.OnClickListener mButtonStartClickListener;
    private View.OnClickListener mSelfStudyClickListener;
    private View.OnClickListener mDeckExpanderClickListener;

    private View.OnClickListener mCountsClickListener;

    private Collection mCol;

    // Totals accumulated as each deck is processed
    private int mNew;
    private int mLrn;
    private int mRev;
    private boolean mNumbersComputed;

    // Flags
    private boolean mHasSubdecks;

    // Whether we have a background (so some items should be partially transparent).
    private boolean mPartiallyTransparentForBackground;



    // ViewHolder class to save inflated views for recycling
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public RelativeLayout deckLayout;
        public RelativeLayout endLayout;

        public LinearLayout countsLayout;
        public ImageButton deckExpander;
        public ImageButton indentView;
        public TextView deckName;
        public TextView deckNew, deckLearn, deckRev;
        public ImageView endIcon;


        public ViewHolder(View v) {
            super(v);
            deckLayout = (RelativeLayout) v.findViewById(R.id.DeckPickerHoriz);
            countsLayout = (LinearLayout) v.findViewById(R.id.counts_layout);
            deckExpander = (ImageButton) v.findViewById(R.id.deckpicker_expander);
            indentView = (ImageButton) v.findViewById(R.id.deckpicker_indent);
            deckName = (TextView) v.findViewById(R.id.deckpicker_name);
            deckNew = (TextView) v.findViewById(R.id.deckpicker_new);
            deckLearn = (TextView) v.findViewById(R.id.deckpicker_lrn);
            deckRev = (TextView) v.findViewById(R.id.deckpicker_rev);
            endIcon = (ImageView) v.findViewById(R.id.end_icon);
//            endLayout = (RelativeLayout) v.findViewById(R.id.end_layout);
        }
    }



    private AnkiActivity mContext;
    private SharedPreferences mPreference;
    private JSONObject mModelKeys;

    public DeckInfoListAdapter(LayoutInflater layoutInflater, AnkiActivity context) {
        mLayoutInflater = layoutInflater;
        mDeckList = new ArrayList<>();
        mContext = context;
        // Get the colors from the theme attributes
        int[] attrs = new int[] {
                R.attr.zeroCountColor,//0
                R.attr.newCountColor,//1
                R.attr.learnCountColor,//2
                R.attr.reviewCountColor,//3
                R.attr.currentDeckBackground,//4
                R.attr.primaryForthTextColor333333,//5
                R.attr.dynDeckColor,//6
                R.attr.expandRef,
                R.attr.collapseRef,
                R.attr.primary_text_third_color999999,};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        mZeroCountColor = ta.getColor(0, ContextCompat.getColor(context, R.color.black));
        mNewCountColor = ta.getColor(1, ContextCompat.getColor(context, R.color.black));
        mLearnCountColor = ta.getColor(2, ContextCompat.getColor(context, R.color.black));
        mReviewCountColor = ta.getColor(3, ContextCompat.getColor(context, R.color.black));
        mRowCurrentDrawable = ta.getResourceId(4, 0);
        mDeckNameDefaultColor = ta.getColor(5, ContextCompat.getColor(context, R.color.new_primary_text_forth_color));
        mDeckNameDefaultColorChild = ta.getColor(9, ContextCompat.getColor(context, R.color.new_primary_text_third_color));
        mDeckNameDynColor = ta.getColor(6, ContextCompat.getColor(context, R.color.primary_color));
        mExpandImage = ta.getDrawable(7);
        mCollapseImage = ta.getDrawable(8);
        ta.recycle();
        mPreference = AnkiDroidApp.getSharedPrefs(context);
        String modelKeyStr=mPreference.getString(Consts.KEY_SAVED_MODEL_KEY,"");
        if(!modelKeyStr.isEmpty()){
            mModelKeys=new JSONObject(modelKeyStr);
        }
    }


    public void setDeckClickListener(View.OnClickListener listener) {
        mDeckClickListener = listener;
    }


    public void setButtonStartClickListener(View.OnClickListener listener) {
        mButtonStartClickListener = listener;
    }


    public void setSelfStudyClickListener(View.OnClickListener listener) {
        mSelfStudyClickListener = listener;
    }


    public void setCountsClickListener(View.OnClickListener listener) {
        mCountsClickListener = listener;
    }


    public void setDeckExpanderClickListener(View.OnClickListener listener) {
        mDeckExpanderClickListener = listener;
    }


    public void setDeckLongClickListener(View.OnLongClickListener listener) {
        mDeckLongClickListener = listener;
    }


    /**
     * Sets whether the control should have partial transparency to allow a background to be seen
     */
    public void enablePartialTransparencyForBackground(boolean isTransparent) {
        mPartiallyTransparentForBackground = isTransparent;
    }


    Deck mCurrentDeck;


    /**
     * Consume a list of {@link AbstractDeckTreeNode}s to render a new deck list.
     */
    public void buildDeckList(List<AbstractDeckTreeNode> nodes, Collection col) {
        mCol=col;
        mDeckList.clear();
        mNew = mLrn = mRev = 0;
        mNumbersComputed = true;
        mHasSubdecks = false;
        mIniCollapsedStatus = mCurrentDeck == mCol.getDecks().current();
        mCurrentDeck = mCol.getDecks().current();
        processNames();
        long currentID = mCurrentDeck.optLong("id");
//        TreeMap<String, Long> map = mCol.getDecks().children(currentID);
//        if (!mIniCollapsedStatus&&!isInitStruct()) {
//            mIniCollapsedStatus = true;
//
////            Timber.i("buildDeckList：当前节点：%s", mCurrentDeck.optString("name"));
//            for (long id : mCurrentIDs) {
////                Timber.i("关闭节点：%s", mCol.getDecks().get(id).optString("name"));
//                mCol.getDecks().get(id).put("collapsed", true);//有孩子节点的只能设置为true
//
//            }
//            for (Deck parent : mCol.getDecks().parents(currentID)) {
//                // 将当前节点的父辈节点全展开
////                Timber.i("打开父节点：%s", parent.optString("name"));
//                parent.put("collapsed", false);
//                mCol.getDecks().save(parent);
//            }
//
//            // 将当前节点的孩子节点全展开
//            Set<String> keySet = map.keySet();
//            for (String str : keySet) {
////                Timber.i("打开孩子节点：%s", str);
//                mCol.getDecks().get(map.get(str)).put("collapsed", false);
//                mCol.getDecks().save(mCol.getDecks().get(map.get(str)));
//            }
//            // 如果的确有孩子，记得自己展开
//
//        }
//        if (map.size() > 0) {
////                Timber.i("打开自己节点：%s", mCurrentDeck.optString("name"));
            mCurrentDeck.put("collapsed", false);
//        }
        processNodes(nodes);
        notifyDataSetChanged();
    }

    private boolean isInitStruct(){
        StringBuilder initIds = new StringBuilder(AnkiDroidApp.getSharedPrefs(mContext).getString(KEY_STRUCT_INIT, ""));
        if (initIds.length() > 0) {
            String[] ids = initIds.toString().split(",");
            if (ids.length > 0) {
                for (String id : ids) {
                    if (mCol.getDecks().current().getLong("id") == Long.parseLong(id)) {
                        //已经初始化过了，直接跳过
                        return true;
                    }
                }
            }

        }
        //没初始化过，那现在也初始化了。
        if (!initIds.toString().isEmpty()) {
            initIds.append(",");
        }
        initIds.append(mCol.getDecks().current().getLong("id"));
        AnkiDroidApp.getSharedPrefs(mContext).edit().putString(KEY_STRUCT_INIT, initIds.toString()).apply();
        return false;
    }
    private boolean mIniCollapsedStatus = false;
    List<Long> mCurrentIDs = new ArrayList<>();


    private void processNames() {
        Deck deck = mCurrentDeck;
        long id = deck.optLong("id");
        Timber.i("processNames：当前节点：%s", deck.optString("name"));

        for (Deck parent : mCol.getDecks().parents(id)) {
            Timber.d("my parents names:%s", parent.optString("name"));
            if (!parent.optString("name").contains("::")) {
                //祖先节点
                long ancestorID = parent.optLong("id");
                mCurrentIDs.add(ancestorID);
                TreeMap<String, Long> map = mCol.getDecks().children(ancestorID);
                Set<String> keySet = map.keySet();
                for (String str : keySet) {
//                    Timber.d("find my id :%s,%s", str, map.get(str));
//                    if(str.split("::").length!=deck.optString("name").split("::").length)
                    mCurrentIDs.add(map.get(str));
                }
            }
        }
//
        if (mCurrentIDs.isEmpty()){
            mCurrentIDs.add(id);
            TreeMap<String, Long> map = mCol.getDecks().children(id);
            Set<String> keySet = map.keySet();
            for (String str : keySet) {
//                Timber.d("find my child id :%s,%s", str, map.get(str));
                mCurrentIDs.add(map.get(str));
            }
        }

//        mCurrentIDs.addAll(mCol.getDecks().childDids(id, mCol.getDecks().childMap()));
        TreeMap<String, Long> map = mCol.getDecks().children(id);
        Set<String> keySet = map.keySet();
        for (String str : keySet) {
            if (Decks.path(str).length > Decks.path(deck.optString("name")).length + 1) {
                mHasSubdecks = true;
            }
        }
        for (Long findID : mCurrentIDs) {
            Timber.d("find my id :%s", mCol.getDecks().get(findID).optString("name"));

        }
//        }
//        mIniCollapsedStatus=true;
    }


    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_LIST = 1;
    private final int VIEW_TYPE_BROWSER_TAB = 2;
    private final int VIEW_TYPE_BROWSER_LIST = 3;


    //mDeckList.size():5
    //0:header
    //1~5:list//<mDeckList.size()+1
    //6:browser tab//==mDeckList.size()+1
    //7~x:tab//>mDeckList.size()+1
    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : position == mDeckList.size() + 1 ? VIEW_TYPE_BROWSER_TAB : position > mDeckList.size() + 1 ? VIEW_TYPE_BROWSER_LIST : VIEW_TYPE_LIST;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == VIEW_TYPE_LIST) {
            v = mLayoutInflater.inflate(R.layout.deck_info_item, parent, false);
            return new ViewHolder(v);
        } else if (viewType == VIEW_TYPE_BROWSER_LIST) {
            v = mLayoutInflater.inflate(R.layout.deck_item_self_study, parent, false);
            return new CardsListAdapter.CardsViewHolder(v);
        } else if (viewType == VIEW_TYPE_HEADER) {
            v = mLayoutInflater.inflate(R.layout.deck_info_item_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            v = mLayoutInflater.inflate(R.layout.deck_info_item_browser, null, false);
            return new BrowserTabViewHolder(v);
        }
//        else {
//            v = mLayoutInflater.inflate(mStudyCountLayoutRes>0?mStudyCountLayoutRes:R.layout.item_self_study_count, parent, false);
//            return new CardsListAdapter.HeaderViewHolder(v);
//        }
    }


    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private Button mButtonStart;
        private TextView mTextDeckName;
        private TextView mTextDeckDescription;
        private TextView mTextTodayNew;
        private TextView mTextStudySetting;
        //    private TextView mTextTodayLrn;
        private TextView mTextTodayRev;
        //    private TextView mTextNewTotal;
        private TextView mTextTotal;
        private TextView mTextHandledPercent;
        private TextView mTextHandledNum;
        private TextView mTextCountLearning, mTextCountHandled, mTextCountNew, mTextCountHard;
        private TextView mTextETA;
        private TextView mTextCongratsMessage;
        private View mDeckInfoLayout;
        private ProgressBar mStudyProgress;
        private RelativeLayout mAdLayout;
        private ImageView mAdImage;
        private ImageView mSelfStudyAskIcon;
        private CardView mSelfStudyHandle, mSelfStudyMark, mSelfStudyAnswer, mSelfStudyCustom;
        private RelativeLayout mDeckListHeader;


        public HeaderViewHolder(View studyOptionsView) {
            super(studyOptionsView);
            mDeckInfoLayout = studyOptionsView.findViewById(R.id.studyoptions_deckinformation);
            mDeckListHeader = studyOptionsView.findViewById(R.id.rl_deck_list_header);
            mStudyProgress = studyOptionsView.findViewById(R.id.study_progress);
            mTextDeckName = studyOptionsView.findViewById(R.id.studyoptions_deck_name);
            mSelfStudyAskIcon = studyOptionsView.findViewById(R.id.self_study_ask_icon);
            mTextStudySetting = studyOptionsView.findViewById(R.id.study_setting);
            mTextDeckDescription = studyOptionsView.findViewById(R.id.studyoptions_deck_description);
            mTextDeckDescription.setMovementMethod(LinkMovementMethod.getInstance());
            mButtonStart = studyOptionsView.findViewById(R.id.studyoptions_start);
            mTextCongratsMessage = studyOptionsView.findViewById(R.id.studyoptions_congrats_message);
            mTextTodayNew = studyOptionsView.findViewById(R.id.studyoptions_new);
            mTextTodayRev = studyOptionsView.findViewById(R.id.studyoptions_rev);
            mTextTotal = studyOptionsView.findViewById(R.id.studyoptions_total);
            mTextHandledNum = studyOptionsView.findViewById(R.id.handled_num);
            mTextHandledPercent = studyOptionsView.findViewById(R.id.handled_percent);
            mTextETA = studyOptionsView.findViewById(R.id.studyoptions_eta);
            mTextCountLearning = studyOptionsView.findViewById(R.id.count_learning);
            mTextCountHandled = studyOptionsView.findViewById(R.id.count_handled);
            mTextCountHard = studyOptionsView.findViewById(R.id.count_hard);
            mTextCountNew = studyOptionsView.findViewById(R.id.count_new_card);
            mAdLayout = studyOptionsView.findViewById(R.id.ad_layout);
            mAdImage = studyOptionsView.findViewById(R.id.ad_image);
            mSelfStudyHandle = studyOptionsView.findViewById(R.id.self_study_handle);
            mSelfStudyMark = studyOptionsView.findViewById(R.id.self_study_mark);
            mSelfStudyAnswer = studyOptionsView.findViewById(R.id.self_study_answer);
            mSelfStudyCustom = studyOptionsView.findViewById(R.id.self_study_custom);
//            mButtonStart.setOnClickListener(mButtonClickListener);
        }
    }



    public static class BrowserTabViewHolder extends RecyclerView.ViewHolder {

        public TabLayout mTabLayout;
        public RecyclerView mList;
        public RelativeLayout mFixBottom;
        public TextView mMore;
        public final TextView cardsCount;


        public BrowserTabViewHolder(View v) {
            super(v);
            mTabLayout = v.findViewById(R.id.tab_layout);
            mList = v.findViewById(R.id.card_browser_list);
            mFixBottom = v.findViewById(R.id.rl_fix_bottom);
            cardsCount = v.findViewById(R.id.search_result_num);

            mMore = v.findViewById(R.id.tx_more);
        }
    }



    public String mTextButtonStart, mTextDeckName, mTextCongratsMessage, mTextDeckDescription, mTextTodayNew, mTextTodayRev, mTextCountHandled, mTextCountLearning, mTextCountNew, mTextCountHard, mTextTotal, mTextHandledPercent, mTextHandledNum, mTextETA, mFullDeckName;
    public int mStudyProgress;
    public boolean mIsDynamic, mButtonStartEnable;
    public int mDeckInfoLayoutVisible = View.GONE, mTextCongratsMessageVisible = View.GONE, mTextDeckDescriptionVisible = View.GONE;

    public static String AD_IMAGE_URL = "";
    public static String AD_LINK_URL = "";


    @VisibleForTesting()
    static Spanned formatDescription(String desc) {
        //#5715: In deck description, ignore what is in style and script tag
        //Since we don't currently execute the JS/CSS, it's not worth displaying.
        String withStrippedTags = Utils.stripHTMLScriptAndStyleTags(desc);
        //#5188 - fromHtml displays newlines as " "
        String withFixedNewlines = HtmlUtils.convertNewlinesToHtml(withStrippedTags);
        return CompatHelper.getCompat().fromHtml(withFixedNewlines);
    }


    Dialog mSelfStudyQADialog;


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder tempHolder, int position) {
        if (tempHolder instanceof HeaderViewHolder) {
            HeaderViewHolder holder = (HeaderViewHolder) tempHolder;
            holder.mTextDeckName.setText(mTextDeckName);
            holder.mDeckListHeader.setVisibility(mDeckList.isEmpty() ? View.GONE : View.VISIBLE);
//            holder.mTextCongratsMessage.setText(mTextCongratsMessage);
            holder.mTextDeckDescription.setText(mTextDeckDescription != null && mTextDeckDescription.length() > 0 ? formatDescription(mTextDeckDescription) : "");
            holder.mTextTodayNew.setText(mTextTodayNew);
            holder.mTextTodayRev.setText(mTextTodayRev);
            holder.mTextCountHandled.setText(mTextCountHandled);
            holder.mTextCountLearning.setText(mTextCountLearning);
            holder.mTextCountNew.setText(mTextCountNew);
            holder.mTextCountHard.setText(mTextCountHard);
            holder.mTextTotal.setText(mTextTotal);
            holder.mTextHandledPercent.setText(mTextHandledPercent);
            holder.mTextHandledNum.setText(mTextHandledNum);
            holder.mTextETA.setText(mTextETA);
            holder.mButtonStart.setText(mTextButtonStart);
            holder.mButtonStart.setOnClickListener(mButtonStartClickListener);
            holder.mSelfStudyAskIcon.setOnClickListener(v -> {
                if (mSelfStudyQADialog == null) {
                    mSelfStudyQADialog = new Dialog(mContext, R.style.CommonDialogTheme);
                    View view = View.inflate(mContext, R.layout.pop_window_self_study_introduce, null);
                    mSelfStudyQADialog.setContentView(view);
                    Window window = mSelfStudyQADialog.getWindow();
                    WindowManager.LayoutParams lps = window.getAttributes();
                    lps.width = WindowManager.LayoutParams.MATCH_PARENT;
                    window.setAttributes(lps);
                    window.setGravity(Gravity.BOTTOM);
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));


                }
                mSelfStudyQADialog.show();
            });
            holder.mStudyProgress.setProgress(mStudyProgress);
//            holder.mDeckInfoLayout.setVisibility(mDeckInfoLayoutVisible);
//            holder.mTextCongratsMessage.setVisibility(mTextCongratsMessageVisible);
            holder.mTextDeckDescription.setVisibility(mTextDeckDescriptionVisible);
            holder.mButtonStart.setEnabled(mButtonStartEnable);
            holder.mSelfStudyHandle.setTag(SelfStudyActivity.TAB_STUDY_STATE);
            holder.mSelfStudyMark.setTag(SelfStudyActivity.TAB_MARK_STATE);
            holder.mSelfStudyAnswer.setTag(SelfStudyActivity.TAB_ANSWER_STATE);
            holder.mSelfStudyCustom.setTag(SelfStudyActivity.TAB_CUSTOM_STATE);

            holder.mSelfStudyHandle.setOnClickListener(mSelfStudyClickListener);
            holder.mSelfStudyMark.setOnClickListener(mSelfStudyClickListener);
            holder.mSelfStudyAnswer.setOnClickListener(mSelfStudyClickListener);
            holder.mSelfStudyCustom.setOnClickListener(mSelfStudyClickListener);
            holder.mTextStudySetting.setVisibility(mCurrentDeck == null || mCurrentDeck.optInt("dyn", 0) == 1 ? View.GONE : View.VISIBLE);
            holder.mTextStudySetting.setOnClickListener(v -> {
                Intent i = new Intent(mContext, StudySettingActivity.class);
                i.putExtra("did", mCurrentDeck.optLong("id"));
                mContext.startActivityForResultWithAnimation(i, DECK_OPTIONS, ActivityTransitionAnimation.FADE);
            });
            if (AD_IMAGE_URL != null && !AD_IMAGE_URL.isEmpty() && AD_LINK_URL != null && !AD_LINK_URL.isEmpty()) {
                holder.mAdLayout.setVisibility(View.VISIBLE);
                CornerTransform transformation = new CornerTransform(mContext, AdaptionUtil.dip2px(mContext, 9));
                transformation.setExceptCorner(false, false, false, false);
                RequestOptions coverRequestOptions = new RequestOptions()
                        .transform(transformation)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)//不做磁盘缓存
                        .skipMemoryCache(true);//不做内存缓存
                Glide.with(mContext)
                        .asBitmap()
                        .load(AD_IMAGE_URL)
                        .apply(coverRequestOptions)
                        .into(holder.mAdImage);
                holder.mAdLayout.setOnClickListener(v -> WebViewActivity.openUrlInApp(mContext, AD_LINK_URL, ""));
            } else {
                holder.mAdLayout.setVisibility(View.GONE);
            }

        } else if (tempHolder instanceof ViewHolder) {
            position--;
            ViewHolder holder = (ViewHolder) tempHolder;
            AbstractDeckTreeNode node = mDeckList.get(position);
            // Set the expander icon and padding according to whether or not there are any subdecks
            RelativeLayout deckLayout = holder.deckLayout;
//        int rightPadding = (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_right_padding);
            if (mHasSubdecks) {
                int smallPadding = (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_left_padding_small);
                deckLayout.setPadding(smallPadding, 0, (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_right_padding), 0);
                holder.deckExpander.setVisibility(View.VISIBLE);
                // Create the correct expander for this deck
                setDeckExpander(holder.deckExpander, holder.indentView, node);
            } else {
                holder.deckExpander.setVisibility(View.GONE);
                int normalPadding = (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_left_padding);
                deckLayout.setPadding(normalPadding, 0, (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_right_padding), 0);
            }

            if (node.hasChildren()) {
                holder.deckExpander.setTag(node.getDid());
                holder.deckExpander.setOnClickListener(mDeckExpanderClickListener);

            } else {
                holder.deckExpander.setOnClickListener(null);
            }
//        holder.deckLayout.setBackgroundResource(mRowCurrentDrawable);
            // Set background colour. The current deck has its own color
//        if (isCurrentlySelectedDeck(node)) {
//            Timber.d("can be shown:%s", node.getDid());
//            Timber.d("mHasSubdecks:%s", node.hasChildren() + "");
//        if (mCurrentIDs.contains(node.getDid())) {
//            holder.deckLayout.setVisibility(View.VISIBLE);
//            holder.deckLayout.setBackgroundResource(mRowCurrentDrawable);
//            if (mPartiallyTransparentForBackground) {
//                setBackgroundAlpha(holder.deckLayout, SELECTED_DECK_ALPHA_AGAINST_BACKGROUND);
//            }
//        } else {
//            holder.deckLayout.setVisibility(View.GONE);
//            CompatHelper.getCompat().setSelectableBackground(holder.deckLayout);
//        }
            // Set deck name and colour. Filtered decks have their own colour
            holder.deckName.setText(node.getLastDeckNameComponent());

            if (mCol.getDecks().isDyn(node.getDid())) {
                holder.deckName.setTextColor(mDeckNameDynColor);
            } else {
                if (node.getDepth() == Decks.path(mCurrentDeck.optString("name")).length) {
                    holder.deckName.setTextColor(mDeckNameDefaultColor);
                } else {
                    holder.deckName.setTextColor(mDeckNameDefaultColorChild);
                }
            }

            // Set the card counts and their colors

            if (node.shouldDisplayCounts()) {
//                holder.deckNew.setText(String.valueOf(node.getNewCount()));
//                holder.deckLearn.setText(String.valueOf(node.getLrnCount()));
//                holder.deckRev.setText(String.valueOf(node.getRevCount()+node.getLrnCount()));
                holder.deckNew.setText(String.valueOf(node.getNewCount() + node.getRevCount() + node.getLrnCount()));
                String ids = Stats.deckLimit(node.getDid(), mCol);
                holder.deckRev.setText(String.valueOf(mCol.cardCount(ids)));


            }

            // Store deck ID in layout's tag for easy retrieval in our click listeners
            holder.deckLayout.setTag(node.getDid());
            holder.countsLayout.setTag(node.getDid());
            holder.endIcon.setTag(node.getDid());
//            holder.endLayout.setTag(node.getDid());

            // Set click listeners
            holder.deckLayout.setOnClickListener(mDeckClickListener);
            holder.deckLayout.setOnLongClickListener(mDeckLongClickListener);
            holder.countsLayout.setOnClickListener(mCountsClickListener);
            holder.endIcon.setOnClickListener(mCountsClickListener);
//            holder.endLayout.setOnClickListener(mCountsClickListener);
        } else if (mCurrentDeck != null) {
            if (tempHolder instanceof CardsListAdapter.CardsViewHolder) {

                CardsListAdapter.CardsViewHolder holder = (CardsListAdapter.CardsViewHolder) tempHolder;
                CardBrowser.CardCache card = mCards.get(position - mDeckList.size() - 1 - 1);
                Timber.i("refresh card:%s", card.getId());
                String question = card.getColumnHeaderText(CardBrowser.Column.SFLD);
                if (card.getColumnHeaderText(CardBrowser.Column.SUSPENDED).equals("True")) {
                    holder.deckQuestion.setTextColor(ContextCompat.getColor(mContext, R.color.new_primary_text_third_color));
//                    holder.deckAnswer.setTextColor(ContextCompat.getColor(mContext, R.color.new_primary_text_third_color));
                }
                String firstColumnStr=question.isEmpty()?card.getColumnHeaderText(CardBrowser.Column.MEDIA_NAME):card.getColumnHeaderText(CardBrowser.Column.SFLD);
                Pattern pattern = Pattern.compile("(?<=≯#).*?(?=#≮)");
                Matcher matcher = pattern.matcher(firstColumnStr);
                String val = "";

                if (matcher.find()) {
                    val = matcher.group(0);
                    try{
                        String key=mModelKeys.getString(String.valueOf(card.getCard().model().getLong("id")));
                        Timber.i("match key:%s", key);
                        holder.deckQuestion.setText(HtmlUtils.delHTMLTag(firstColumnStr.substring(0, firstColumnStr.indexOf("≯#")) + AESUtil.decrypt(val, key) + firstColumnStr.substring(firstColumnStr.indexOf("#≮") + 2)));
//                    holder.deckQuestion.setText(firstColumnStr.substring(0, firstColumnStr.indexOf("≯#")) + aesUtil.getDecryptedMessage(val) + firstColumnStr.substring(firstColumnStr.indexOf("#≮") + 2));
                    }catch (Exception e){
                        e.printStackTrace();
                        holder.deckQuestion.setText(firstColumnStr);
                    }
                }else {
                    holder.deckQuestion.setText(firstColumnStr);

                }

//                holder.deckAnswer.setText(card.getColumnHeaderText(CardBrowser.Column.ANSWER));
                holder.reviewCount.setText(card.getColumnHeaderText(CardBrowser.Column.REVIEWS));
                holder.forgetCount.setText(card.getColumnHeaderText(CardBrowser.Column.LAPSES));
                holder.due.setText(card.getColumnHeaderText(CardBrowser.Column.DUE2));

                holder.mark.setTag(card.getId());
                holder.flag.setTag(card.getId());
                holder.itemRoot.setTag(card.getId());
                LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.itemRoot.getLayoutParams();
                layoutParams.rightMargin = 0;
                holder.itemRoot.setLayoutParams(layoutParams);
                if (card.getCard().note().hasTag("marked")) {
                    holder.mark.setImageResource(R.mipmap.mark_star_normal);
                } else {
                    holder.mark.setImageResource(R.mipmap.note_star_unselected);
                }
                if (getFlagRes(card.getCard()) != -1) {
                    holder.flag.setImageResource(getFlagRes(card.getCard()));
                } else {
                    holder.flag.setImageResource(R.mipmap.note_flag_unselected);
                }

                holder.mark.setOnClickListener(v -> {
                    CollectionTask.launchCollectionTask(DISMISS_MULTI,
                            markCardHandler(),
                            new TaskData(new Object[] {new long[] {(long) v.getTag()}, Collection.DismissType.MARK_NOTE_MULTI}));
                    notifyDataSetChanged();
                });
                holder.flag.setOnClickListener(v -> {
                    if (mListPop == null) {
                        mListPop = new ListPopupWindow(mContext);
                        for (int i = 0; i < mFlagRes.length; i++) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("img", mFlagRes[i]);
                            map.put("content", mFlagContent[i]);
                            mFlagList.add(map);
                        }
                        mListPop.setAdapter(new SimpleAdapter(mContext, mFlagList, R.layout.item_flags_list, new String[] {"img", "content"}, new int[] {R.id.flag_icon, R.id.flag_text}));
                        mListPop.setWidth(v.getRootView().getWidth() / 2);
                        mListPop.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
                        mListPop.setModal(true);//设置是否是模式


                    }
                    mListPop.setOnItemClickListener((parent, view, position1, id) -> {
                        CollectionTask.launchCollectionTask(DISMISS_MULTI,
                                flagCardHandler(),
                                new TaskData(new Object[] {new long[] {(long) v.getTag()}, Collection.DismissType.FLAG, position1}));
                        notifyDataSetChanged();
                        mListPop.dismiss();
                    });
                    mListPop.setAnchorView(v);
                    mListPop.show();
                });

                holder.itemRoot.setOnClickListener(v -> {
                    Intent previewer = new Intent(mContext, Previewer.class);
                    long[] ids =  getAllCardIds();
                    long targetId = (long) v.getTag();
//                    if (ids.length > 100) {
//                        //为提高效率 直接复制卡牌
//                        long[] finalIds = new long[ids.length + 1];
//                        finalIds[0] = targetId;
//                        System.arraycopy(ids, 0, finalIds, 1, ids.length);
//                        previewer.putExtra("cardList", finalIds);
//                    } else {
//                        for (int i = 0; i < ids.length; i++) {
//                            if (ids[i] == targetId) {
//                                ids[i] = ids[0];
//                                ids[0] = targetId;
//                            }
//                        }
//                        previewer.putExtra("cardList", ids);
//                    }
                    for (int i = 0; i < ids.length; i++) {
                        if (ids[i] == targetId) {
                            previewer.putExtra("index", i);
                            break;
                        }
                    }
                    previewer.putExtra("cardList", ids);
                    mContext.startActivityForResultWithoutAnimation(previewer, PREVIEW_CARDS);
                });
//                holder.itemRoot.setOnLongClickListener(mDeckLongClickListener);
            } else if (tempHolder instanceof BrowserTabViewHolder) {
                BrowserTabViewHolder holder = (BrowserTabViewHolder) tempHolder;
                String[] tabArray = mContext.getResources().getStringArray(R.array.deck_info_browser_tab);
                holder.cardsCount.setText(String.format("筛选出%d张卡片", mCards.size()));
//                CardsListAdapter  mCardsAdapter = new CardsListAdapter(mContext.getLayoutInflater(), mContext, new CardsListAdapter.CardListAdapterCallback() {
//                    @Override
//                    public List<CardBrowser.CardCache> getCards() {
//                        if (mCards == null) {
//                            mCards = new ArrayList<>();
//                        }
//                        return mCards;
//                    }
//
//
//                    @Override
//                    public void onChangeMultiMode(boolean isMultiMode) {
//                    }
//
//
//                    @Override
//                    public void onItemSelect(int count) {
//                    }
//                });
//                mCardsAdapter.setStudyCountLayoutRes(R.layout.item_option_study_count);
//                mCardsAdapter.setDeckClickListener(view -> {
//                    Intent previewer = new Intent(mContext, Previewer.class);
//                    long[] ids =  getAllCardIds();
//                    long targetId = (long) view.getTag();
//                    if (ids.length > 100) {
//                        //为提高效率 直接复制卡牌
//                        long[] finalIds = new long[ids.length + 1];
//                        finalIds[0] = targetId;
//                        System.arraycopy(ids, 0, finalIds, 1, ids.length);
//                        previewer.putExtra("cardList", finalIds);
//                    } else {
//                        for (int i = 0; i < ids.length; i++) {
//                            if (ids[i] == targetId) {
//                                ids[i] = ids[0];
//                                ids[0] = targetId;
//                            }
//                        }
//                        previewer.putExtra("cardList", ids);
//                    }
//                    previewer.putExtra("index", 0);
//                    mContext.startActivityForResultWithoutAnimation(previewer, PREVIEW_CARDS);
//                });
//                holder.mList .setAdapter(mCardsAdapter);
//                holder.mList.setRecycledViewPool(new RecyclerView.RecycledViewPool());
//                holder.mList.setNestedScrollingEnabled(false);
//                holder.mList.setHasFixedSize(true);
//                holder.mList.setLayoutManager(new LinearLayoutManager(mContext));
                holder.mMore.setTag(SelfStudyActivity.TAB_MAIN_STATE);
                holder.mFixBottom.setVisibility(mDeckList.isEmpty() ? View.GONE : View.VISIBLE);
                holder.mMore.setOnClickListener(mSelfStudyClickListener);
                holder.mTabLayout.removeAllTabs();
                LinearLayout linearLayout = (LinearLayout) holder.mTabLayout.getChildAt(0);
                linearLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
                linearLayout.setDividerDrawable(ContextCompat.getDrawable(mContext,
                        R.drawable.divider_vertical)); //设置分割线的样式
                linearLayout.setDividerPadding((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 18, mContext.getResources().getDisplayMetrics())); //设置分割线间隔
                for (int i = 0; i < tabArray.length; i++) {
                    TabLayout.Tab tab = holder.mTabLayout.newTab();
                    View view = mContext.getLayoutInflater().inflate(R.layout.item_option_tab, null);
                    ((TextView) view.findViewById(R.id.name)).setText(tabArray[i]);
                    tab.setCustomView(view);
                    view.setTag(i);
                    int finalI = i;
                    view.setOnClickListener(v -> {
                        if (mContext.colIsOpen()) {
                            mCurrentSelectedTab = finalI;
                            holder.mTabLayout.selectTab(holder.mTabLayout.getTabAt(finalI));
                            //  estimate maximum number of cards that could be visible (assuming worst-case minimum row height of 20dp)
                            int numCardsToRender = (int) Math.ceil(holder.mList.getHeight() /
                                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, mContext.getResources().getDisplayMetrics())) + 5;
                            Timber.i("I wanna get %d cards", numCardsToRender);
                            // Perform database query to get all card ids
                            mSearchCardsHandler = new SearchCardsHandler();
                            String searchText = getRestrictByTab(finalI) + "deck:\"" + mCurrentDeck.getString("name") + "\" ";
                            CollectionTask.launchCollectionTask(SEARCH_CARDS,
                                    mSearchCardsHandler,
                                    new TaskData(new Object[] {
                                            searchText,
                                            true,
                                            numCardsToRender,
                                            0,
                                            0
                                    })
                            );
                        }
                    });
                    holder.mTabLayout.addTab(tab);
                }
                holder.mTabLayout.selectTab(holder.mTabLayout.getTabAt(mCurrentSelectedTab));
                if (!mInitBrowserCards) {
                    mInitBrowserCards = true;
                    holder.mTabLayout.getTabAt(0).getCustomView().performClick();
                }
            }

        }
    }

    ListPopupWindow mListPop;
    private final List<Map<String, Object>> mFlagList = new ArrayList<>();
    private final String[] mFlagContent = {"无标志", "红色标志", "橙色标志", "绿色标志", "蓝色标志"};
    private final int[] mFlagRes = {R.mipmap.button_white_flag_normal, R.mipmap.mark_red_flag_normal, R.mipmap.mark_yellow_flag_normal, R.mipmap.mark_green_flag_normal, R.mipmap.mark_blue_flag_normal};

    private  FlagCardHandler flagCardHandler() {
        return new  FlagCardHandler(this);
    }


    private static class FlagCardHandler extends  ListenerWithProgressBarCloseOnFalse {
        public FlagCardHandler(DeckInfoListAdapter browser) {
            super(browser);
        }


        @Override
        protected void actualOnValidPostExecute(DeckInfoListAdapter browser, TaskData result) {
            Card[] cards = (Card[]) result.getObjArray();
            browser.updateCardsInList(Arrays.asList(cards), null);
        }
    }

    private MarkCardHandler markCardHandler() {
        return new  MarkCardHandler(this);
    }


    private static class MarkCardHandler extends  ListenerWithProgressBarCloseOnFalse {
        public MarkCardHandler(DeckInfoListAdapter browser) {
            super(browser);
        }


        @Override
        protected void actualOnValidPostExecute(DeckInfoListAdapter browser, TaskData result) {
            Card[] cards = (Card[]) result.getObjArray();
            browser.updateCardsInList(CardUtils.getAllCards(CardUtils.getNotes(Arrays.asList(cards))), null);

        }
    }

    private static abstract class ListenerWithProgressBarCloseOnFalse extends  ListenerWithProgressBar {
        private final String mTimber;


        public ListenerWithProgressBarCloseOnFalse(String timber, DeckInfoListAdapter browser) {
            super(browser);
            mTimber = timber;
        }


        public ListenerWithProgressBarCloseOnFalse(DeckInfoListAdapter browser) {
            this(null, browser);
        }


        public void actualOnPostExecute(@NonNull DeckInfoListAdapter browser, TaskData result) {
            if (mTimber != null) {
                Timber.d(mTimber);
            }
            if (result.getBoolean()) {
                actualOnValidPostExecute(browser, result);
            }
        }


        protected abstract void actualOnValidPostExecute(DeckInfoListAdapter browser, TaskData result);
    }

    private static abstract class ListenerWithProgressBar extends TaskListenerWithContext<DeckInfoListAdapter> {
        public ListenerWithProgressBar(DeckInfoListAdapter browser) {
            super(browser);
        }


        @Override
        public void actualOnPreExecute(@NonNull DeckInfoListAdapter browser) {

        }
    }
    private void updateCardsInList(List<Card> cards, Map<Long, String> updatedCardTags) {
        List<CardBrowser.CardCache> cardList = mCards;
        Map<Long, Integer> idToPos = getPositionMap(cardList);
        for (Card c : cards) {
            // get position in the mCards search results HashMap
            Integer pos = idToPos.get(c.getId());
            if (pos == null || pos >= mCards.size()) {
                continue;
            }
            // update Q & A etc
            cardList.get(pos).load(true, 0, 1);
        }

        notifyDataSetChanged();
    }
    private int mCurrentSelectedTab = 0;
    private boolean mInitBrowserCards = false;
    public void notifyDataSetChangedAll(){
        mInitBrowserCards=false;
        notifyDataSetChanged();
    }

    private long[] getAllCardIds() {
        long[] l = new long[mCards.size()];
        for (int i = 0; i < mCards.size(); i++) {
            l[i] = mCards.get(i).getId();
        }
        return l;
    }


    private void updateDeckNum(TabLayout tabLayout) {
//        double[] stat =  new Stats(mContext.getCol(), mCurrentDeck.optLong("id")).calculateCardCommonInfoState();
//        int todayCard=Integer.parseInt(mTextTodayNew)+Integer.parseInt(mTextTodayRev);
//        double[] count = new double[]{todayCard,stat[0],stat[1]};
//        Timber.i("tab layout count:"+tabLayout.getTabCount());
//        for (int i = 0; i < tabLayout.getTabCount(); i++) {
//            ((TextView) tabLayout.getTabAt(i).view.findViewById(R.id.count)).setText("" + count[i]);
//        }
    }


    private String getRestrictByTab(int index) {
        String restrict = "";
        switch (index) {
            case 0: //全部
//                restrict = "(is:due) ";//今日待学：取出今天要复习的新卡和复习卡，学完之后消失在今日待学里
                restrict = "(duetoday:" + mTextTodayNew + ")";//今日待学：取出今天要复习的新卡和复习卡，学完之后消失在今日待学里
                break;
            case 1:
                restrict = "(-is:new)";//fixme 学习记录，把卡牌根据学习时间排序，最新学习的排在最前 ；需要有排序功能
                break;
            case 2:
                restrict = "((rated:31:1) or (rated:31:2))";
                break;
        }
        return restrict;
    }


    private List<CardBrowser.CardCache> mCards = new ArrayList<>();
    private SearchCardsHandler mSearchCardsHandler;



    private class SearchCardsHandler extends TaskListener {
//
//        @Override
//        public void actualOnPreExecute(@NonNull CardsListAdapter context) {
//
//        }
//
//
//        @Override
//        public void actualOnPostExecute(@NonNull TaskData result) {
//            if (result != null) {
//                mCards = result.getCards();
//                notifyDataSetChanged();
//
//
//            }
//        }
//
//
//        @Override
//        public void actualOnCancelled( ) {
//            super.actualOnCancelled( );
//        }
//
//
//        @Override
//        public void actualOnPreExecute(@NonNull @NotNull Object context) {
//
//        }
//
//
//        @Override
//        public void actualOnPostExecute(@NonNull @NotNull Object context, TaskData result) {
//
//        }


        @Override
        public void onPreExecute() {

        }


        @Override
        public void onPostExecute(TaskData result) {
            if (result != null) {
                mCards = result.getCards();
                notifyDataSetChanged();


            }
        }
    }


    private void setBackgroundAlpha(View view, @SuppressWarnings("SameParameterValue") double alphaPercentage) {
        Drawable background = view.getBackground().mutate();
        background.setAlpha((int) (255 * alphaPercentage));
        view.setBackground(background);
    }


    private boolean isCurrentlySelectedDeck(AbstractDeckTreeNode node) {
        return node.getDid() == mCurrentDeck.optLong("id");
    }


    private String mCurrentNodeName;


    private String getCurrentNodeName() {
        if (mCurrentNodeName == null) {
            mCurrentNodeName = mCurrentDeck.optString("name");
        }
        return mCurrentNodeName;
    }


    @Override
    public int getItemCount() {
        return mDeckList.size() + 1 + 1 + mCards.size();
    }


    private void setDeckExpander(ImageButton expander, ImageButton indent, AbstractDeckTreeNode node) {
        boolean collapsed = mCol.getDecks().get(node.getDid()).optBoolean("collapsed", false);
        // Apply the correct expand/collapse drawable
       if (node.hasChildren()) {
           if (collapsed) {
               expander.setImageDrawable(mExpandImage);
               expander.setContentDescription(expander.getContext().getString(R.string.expand));
           } else{
               expander.setImageDrawable(mCollapseImage);
               expander.setContentDescription(expander.getContext().getString(R.string.collapse));
           }
        } else {
            expander.setImageDrawable(mNoExpander);

        }
        // Add some indenting for each nested level
        int width = (int) indent.getResources().getDimension(R.dimen.keyline_1) * (node.getDepth() - (Decks.path(mCurrentDeck.optString("name")).length - 1) - 1);
//        Timber.i("min width：%d", width);
        indent.setMinimumWidth(width);
    }


    private void processNodes(List<AbstractDeckTreeNode> nodes) {

        for (AbstractDeckTreeNode node : nodes) {
            Timber.i("节点审核：%s", node.getFullDeckName());
//            if (!mCurrentIDs.contains(node.getDid())) {
//                continue;
//            }
            // If the default deck is empty, hide it by not adding it to the deck list.
            // We don't hide it if it's the only deck or if it has sub-decks.
            if (node.getDid() == 1 && nodes.size() > 1 && !node.hasChildren()) {
                if (mCol.getDb().queryScalar("select 1 from cards where did = 1") == 0) {
                    continue;
                }
            }
            // If any of this node's parents are collapsed, don't add it to the deck list
            for (Deck parent : mCol.getDecks().parents(node.getDid())) {
                if(parent==null){
                    continue;
                }
                Timber.i("我的父节点关闭啦：%s：%s", parent.optString("name"), parent.optBoolean("collapsed"));
//                mHasSubdecks = true;    // If a deck has a parent it means it's a subdeck so set a flag
                if (parent.optBoolean("collapsed")) {
                    return;
                }
            }
            if (/*node.getDid()!=mCurrentDeck.optLong("id")&&*/node.getDepth() > Decks.path(mCurrentDeck.optString("name")).length - 1) {
                Timber.i("添加节点到目录：%s", node.getFullDeckName());
                mDeckList.add(node);
            }


            // Add this node's counts to the totals if it's a parent deck
            if (node.getDepth() == 0) {
                if (node.shouldDisplayCounts()) {
                    mNew += node.getNewCount();
                    mRev += node.getLrnCount();
                    mRev += node.getRevCount();
                }
            }
            // Process sub-decks
            processNodes(node.getChildren());
        }
    }


    /**
     * Return the position of the deck in the deck list. If the deck is a child of a collapsed deck
     * (i.e., not visible in the deck list), then the position of the parent deck is returned instead.
     * <p>
     * An invalid deck ID will return position 0.
     */
    public int findDeckPosition(long did) {
        for (int i = 0; i < mDeckList.size(); i++) {
            if (mDeckList.get(i).getDid() == did) {
                return i;
            }
        }
        // If the deck is not in our list, we search again using the immediate parent
        List<Deck> parents = mCol.getDecks().parents(did);
        if (parents.size() == 0) {
            return 0;
        } else {
            return findDeckPosition(parents.get(parents.size() - 1).optLong("id", 0));
        }
    }


    @Nullable
    public Integer getEta() {
        if (mNumbersComputed) {
            return mCol.getSched().eta(new int[] {mNew, mLrn, mRev});
        } else {
            return null;
        }
    }


    @Nullable
    public Integer getDue() {
        if (mNumbersComputed) {
            return mNew + mLrn + mRev;
        } else {
            return null;
        }
    }


    public List<AbstractDeckTreeNode> getDeckList() {
        return mDeckList;
    }
}
