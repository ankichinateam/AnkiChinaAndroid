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

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import android.net.Uri;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.R;
import com.ichi2.anki.SelfStudyActivity;
import com.ichi2.anki.WebViewActivity;
import com.ichi2.compat.CompatHelper;
import com.ichi2.libanki.Collection;

import com.ichi2.libanki.Deck;
import com.ichi2.libanki.Decks;
import com.ichi2.libanki.Utils;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.libanki.sched.DeckDueTreeNode;
import com.ichi2.libanki.sched.DeckTreeNode;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.utils.CornerTransform;
import com.ichi2.utils.HtmlUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TreeMap;

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
    private View.OnClickListener mButtonStartClickListener;
    private View.OnClickListener mSelfStudyClickListener;
    private View.OnClickListener mDeckExpanderClickListener;
    private View.OnLongClickListener mDeckLongClickListener;
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
        }
    }



    private AnkiActivity mContext;


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
        mCol = col;
        mDeckList.clear();
        mNew = mLrn = mRev = 0;
        mNumbersComputed = true;
        mHasSubdecks = false;
        mIniCollapsedStatus=mCurrentDeck == mCol.getDecks().current();
        mCurrentDeck = mCol.getDecks().current();
        processNames();

        if (!mIniCollapsedStatus) {
            mIniCollapsedStatus = true;

            long currentID = mCurrentDeck.optLong("id");
            Timber.i("buildDeckList：当前节点：%s", mCurrentDeck.optString("name"));
            for (long id : mCurrentIDs) {
                Timber.i("关闭节点：%s", mCol.getDecks().get(id).optString("name"));
                mCol.getDecks().get(id).put("collapsed", true);//有孩子节点的只能设置为true

            }
            for (Deck parent : mCol.getDecks().parents(currentID)) {
                // 将当前节点的父辈节点全展开
                Timber.i("打开父节点：%s", parent.optString("name"));
                parent.put("collapsed", false);
                mCol.getDecks().save(parent);
            }
            TreeMap<String, Long> map = mCol.getDecks().children(currentID);
            // 将当前节点的孩子节点全展开
            Set<String> keySet = map.keySet();
            for (String str : keySet) {
                Timber.i("打开孩子节点：%s", str);
                mCol.getDecks().get(map.get(str)).put("collapsed", false);
                mCol.getDecks().save(mCol.getDecks().get(map.get(str)));
            }
            // 如果的确有孩子，记得自己展开
            if (map.size() > 0) {
                Timber.i("打开自己节点：%s", mCurrentDeck.optString("name"));
                mCurrentDeck.put("collapsed", false);
            }
        }
        processNodes(nodes);

        notifyDataSetChanged();
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
//                TreeMap<String, Long> map = mCol.getDecks().children(ancestorID);
//                Set<String> keySet = map.keySet();
//                for (String str : keySet) {
//                    Timber.d("find my id :%s,%s", str, map.get(str));
////                    if(str.split("::").length!=deck.optString("name").split("::").length)
//                    mCurrentIDs.add(map.get(str));
//                }
            }
        }
//
//        if (mCurrentIDs.isEmpty()) {
        mCurrentIDs.add(id);
        mCurrentIDs.addAll(mCol.getDecks().childDids(id, mCol.getDecks().childMap()));
        TreeMap<String, Long> map = mCol.getDecks().children(id);
        Set<String> keySet = map.keySet();
        for (String str : keySet) {
            if (Decks.path(str).length>Decks.path(deck.optString("name")).length+1){
                mHasSubdecks=true;
            }
        }
        for (Long findID : mCurrentIDs) {
            Timber.d("find my id :%s", findID);

        }
//        }
//        mIniCollapsedStatus=true;
    }


    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_LIST = 1;


    @Override
    public int getItemViewType(int position) {
        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_LIST;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == VIEW_TYPE_HEADER) {
            v = mLayoutInflater.inflate(R.layout.deck_info_item_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            v = mLayoutInflater.inflate(R.layout.deck_info_item, parent, false);
            return new ViewHolder(v);
        }
    }


    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private Button mButtonStart;
        private TextView mTextDeckName;
        private TextView mTextDeckDescription;
        private TextView mTextTodayNew;
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
        private CardView mSelfStudyHandle,mSelfStudyMark,mSelfStudyAnswer,mSelfStudyCustom;

        public HeaderViewHolder(View studyOptionsView) {
            super(studyOptionsView);
            mDeckInfoLayout = studyOptionsView.findViewById(R.id.studyoptions_deckinformation);
            mStudyProgress = studyOptionsView.findViewById(R.id.study_progress);
            mTextDeckName = studyOptionsView.findViewById(R.id.studyoptions_deck_name);
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


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder tempHolder, int position) {
        if (tempHolder instanceof HeaderViewHolder) {
            HeaderViewHolder holder = (HeaderViewHolder) tempHolder;
            holder.mTextDeckName.setText(mTextDeckName);
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
                holder.mAdLayout.setOnClickListener(v ->WebViewActivity.openUrlInApp(mContext, AD_LINK_URL,  "")  );
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
            Timber.d("can be shown:%s", node.getDid());
            Timber.d("mHasSubdecks:%s", mHasSubdecks + "");
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
                if (node.getDepth() == Decks.path(mCurrentDeck.optString("name")).length  ) {
                    holder.deckName.setTextColor(mDeckNameDefaultColor);
                } else {
                    holder.deckName.setTextColor(mDeckNameDefaultColorChild);
                }
            }

            // Set the card counts and their colors
            if (node.shouldDisplayCounts()) {
                holder.deckNew.setText(String.valueOf(node.getNewCount()));
                holder.deckLearn.setText(String.valueOf(node.getLrnCount()));
                holder.deckRev.setText(String.valueOf(node.getRevCount()+node.getLrnCount()));
            }

            // Store deck ID in layout's tag for easy retrieval in our click listeners
            holder.deckLayout.setTag(node.getDid());
            holder.countsLayout.setTag(node.getDid());
            holder.endIcon.setTag(node.getDid());

            // Set click listeners
            holder.deckLayout.setOnClickListener(mDeckClickListener);
            holder.deckLayout.setOnLongClickListener(mDeckLongClickListener);
            holder.countsLayout.setOnClickListener(mCountsClickListener);
            holder.endIcon.setOnClickListener(mCountsClickListener);
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
        return mDeckList.size() + 1;
    }


    private void setDeckExpander(ImageButton expander, ImageButton indent, AbstractDeckTreeNode node) {
        boolean collapsed = mCol.getDecks().get(node.getDid()).optBoolean("collapsed", false);
        // Apply the correct expand/collapse drawable
        if (collapsed) {
            expander.setImageDrawable(mExpandImage);
            expander.setContentDescription(expander.getContext().getString(R.string.expand));
        } else if (node.hasChildren()) {
            expander.setImageDrawable(mCollapseImage);
            expander.setContentDescription(expander.getContext().getString(R.string.collapse));
        } else {
            expander.setImageDrawable(mNoExpander);

        }
        // Add some indenting for each nested level
        int width = (int) indent.getResources().getDimension(R.dimen.keyline_1) * (node.getDepth() - (Decks.path(mCurrentDeck.optString("name")).length - 1) - 1);
        Timber.i("min width：%d", width);
        indent.setMinimumWidth(width);
    }


    private void processNodes(List<AbstractDeckTreeNode> nodes) {

        for (AbstractDeckTreeNode node : nodes) {
            Timber.i("节点审核：%s", node.getFullDeckName());
            if (!mCurrentIDs.contains(node.getDid())) {
                continue;
            }
            // If the default deck is empty, hide it by not adding it to the deck list.
            // We don't hide it if it's the only deck or if it has sub-decks.
            if (node.getDid() == 1 && nodes.size() > 1 && !node.hasChildren()) {
                if (mCol.getDb().queryScalar("select 1 from cards where did = 1") == 0) {
                    continue;
                }
            }
            // If any of this node's parents are collapsed, don't add it to the deck list
            for (Deck parent : mCol.getDecks().parents(node.getDid())) {
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
