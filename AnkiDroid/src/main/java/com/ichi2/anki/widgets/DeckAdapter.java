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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import timber.log.Timber;

import android.view.GestureDetector;
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

import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.DeckPicker;
import com.ichi2.anki.R;
import com.ichi2.libanki.Collection;

import com.ichi2.libanki.Deck;
import com.ichi2.libanki.sched.AbstractDeckTreeNode;
import com.ichi2.libanki.stats.Stats;
import com.ichi2.utils.AdaptionUtil;
import com.ichi2.widget.SwipeItemLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.ichi2.libanki.stats.Stats.AxisType.TYPE_LIFE;

public class DeckAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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
    private int mDeckNameDynColor;
    private Drawable mExpandImage;
    private Drawable mCollapseImage;
    private Drawable mFileBag;
    private Drawable mNoExpander = new ColorDrawable(Color.TRANSPARENT);

    // Listeners
    private View.OnClickListener mDeckClickListener;
    private View.OnClickListener mAdClickListener;
    private View.OnClickListener mMarketClickListener;
    private View.OnClickListener mDeckExpanderClickListener;
    private View.OnLongClickListener mDeckLongClickListener;
    private View.OnClickListener mCountsClickListener;

    private AnkiActivity mContext ;

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
        private final LinearLayout deckLayout;
        private final SwipeItemLayout root;
        private final LinearLayout countsLayout;
        //        public LinearLayout filebagLayout;
//        public ImageButton deckExpander;
//        public ImageButton indentView;
//        public ImageView fileBag;
        private final TextView deckName;
        private final TextView handled_num;
        private final TextView handled_percent;
        private final TextView deckNew;
        private final TextView deckLearn;
        private final TextView deckRev;
        private ProgressBar studyProgress;


        public ViewHolder(View v) {
            super(v);
            root = v.findViewById(R.id.root);
            deckLayout = v.findViewById(R.id.DeckPickerHoriz);
            countsLayout = v.findViewById(R.id.counts_layout);
//            deckExpander = (ImageButton) v.findViewById(R.id.deckpicker_expander);
//            indentView = (ImageButton) v.findViewById(R.id.deckpicker_indent);
            deckName = v.findViewById(R.id.deckpicker_name);
            deckNew = v.findViewById(R.id.deckpicker_new);
            deckLearn = v.findViewById(R.id.deckpicker_lrn);
            deckRev = v.findViewById(R.id.deckpicker_rev);
            handled_num = v.findViewById(R.id.handled_num);
            handled_percent = v.findViewById(R.id.handled_percent);
            studyProgress = v.findViewById(R.id.study_progress);
//            fileBag =   v.findViewById(R.id.file_bag);
//            filebagLayout =   v.findViewById(R.id.ll_file_bag);
        }
    }



    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        private Button btn_get_card;


        public EmptyViewHolder(View v) {
            super(v);
            btn_get_card = v.findViewById(R.id.btn_get_card);
        }
    }



    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final RelativeLayout main_ad_layout;
        private final TextView today_title;
        private final TextView main_ad_text;
        private final TextView new_card_num;
        private final TextView review_card_num;
        private final TextView cost_time;
        private final TextView tv_resource;
        private final ImageView remove_ad;


        public HeaderViewHolder(View v) {
            super(v);
            main_ad_layout = v.findViewById(R.id.main_ad_layout);
            main_ad_text = v.findViewById(R.id.main_ad_text);
            today_title = v.findViewById(R.id.today_title);
            remove_ad = v.findViewById(R.id.remove_ad);
            new_card_num = v.findViewById(R.id.new_card_num);
            review_card_num = v.findViewById(R.id.review_card_num);
            cost_time = v.findViewById(R.id.cost_time);
            tv_resource = v.findViewById(R.id.tv_resource);
        }
    }






    @SuppressLint("UseCompatLoadingForDrawables")
    public DeckAdapter(LayoutInflater layoutInflater, AnkiActivity context) {
        mLayoutInflater = layoutInflater;
        mDeckList = new ArrayList<>();
        mContext = context;
        // Get the colors from the theme attributes
        int[] attrs = new int[] {
                R.attr.zeroCountColor,
                R.attr.newCountColor,
                R.attr.learnCountColor,
                R.attr.reviewCountColor,
                R.attr.currentDeckBackground,
                android.R.attr.textColor,
                R.attr.dynDeckColor,
                R.attr.expandRef,
                R.attr.collapseRef,
                R.attr.fileBagRef};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        mZeroCountColor = ta.getColor(0, ContextCompat.getColor(context, R.color.black));
        mNewCountColor = ta.getColor(1, ContextCompat.getColor(context, R.color.black));
        mLearnCountColor = ta.getColor(2, ContextCompat.getColor(context, R.color.black));
        mReviewCountColor = ta.getColor(3, ContextCompat.getColor(context, R.color.black));
        mRowCurrentDrawable = ta.getResourceId(4, 0);
        mDeckNameDefaultColor = ta.getColor(5, ContextCompat.getColor(context, R.color.black));
        mDeckNameDynColor = ta.getColor(6, ContextCompat.getColor(context, R.color.primary_color));
        mExpandImage = ta.getDrawable(7);
        mCollapseImage = ta.getDrawable(8);
        mFileBag = ta.getDrawable(9);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            mFileBag = context.getDrawable(R.mipmap.home_card_group_normal);
//        }else {
//            mFileBag = context.getResources().getDrawable(R.mipmap.home_card_group_normal);
//        }
        ta.recycle();
    }


    public void setDeckClickListener(View.OnClickListener listener) {
        mDeckClickListener = listener;
    }


    public void setAdClickListener(View.OnClickListener listener) {
        mAdClickListener = listener;
    }


    public void setMarketClickListener(View.OnClickListener listener) {
        mMarketClickListener = listener;
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


    private String mAdText, mAdLink;


    public void updateAds(String text, String link) {
        mAdText = text;
        mAdLink = link;
        notifyItemChanged(0);
    }


    private String mNewCard, mNeedReviewCard, mCost;


    public void updateHeaderData(String newCard, String needReviewCard, String cost) {
        mNewCard = newCard;
        mNeedReviewCard = needReviewCard;
        mCost = cost;
        notifyItemChanged(0);
    }

    private Collection mCol;
    /**
     * Consume a list of {@link AbstractDeckTreeNode}s to render a new deck list.
     */
    public void buildDeckList(List<AbstractDeckTreeNode> nodes, Collection col) {
        mCol=col;
        mDeckList.clear();
        mNew = mLrn = mRev = 0;
        mNumbersComputed = true;
        mHasSubdecks = false;

        processNodes(nodes);
        notifyDataSetChanged();
    }


    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_LIST = 1;
    private final int VIEW_TYPE_EMPTY = 2;


    @Override
    public int getItemViewType(int position) {

        return position == 0 ? VIEW_TYPE_HEADER : mDeckList.size() == 0 ? VIEW_TYPE_EMPTY : VIEW_TYPE_LIST;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == VIEW_TYPE_LIST) {
            v = mLayoutInflater.inflate(R.layout.deck_item, parent, false);
            return new ViewHolder(v);
        } else if (viewType == VIEW_TYPE_HEADER) {
            v = mLayoutInflater.inflate(R.layout.deck_item_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            v = mLayoutInflater.inflate(R.layout.deck_item_no_decks_placeholder, parent, false);
            return new EmptyViewHolder(v);
        }

    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder tempHolder, int position) {
        // Update views for this node
        if (tempHolder instanceof HeaderViewHolder) {
            HeaderViewHolder holder = (HeaderViewHolder) tempHolder;
            holder.cost_time.setText(mCost == null || mCost.isEmpty() ? "0" : mCost);
            holder.new_card_num.setText(mNewCard == null || mNewCard.isEmpty() ? "0" : mNewCard);
            holder.review_card_num.setText(mNeedReviewCard == null || mNeedReviewCard.isEmpty() ? "0" : mNeedReviewCard);
            holder.tv_resource.setOnClickListener(mMarketClickListener);
            if (mAdText != null && !mAdText.isEmpty()) {
                holder.main_ad_layout.setVisibility(View.VISIBLE);
                holder.main_ad_text.setText(mAdText);
                holder.main_ad_layout.setOnClickListener(mAdClickListener);
                holder.remove_ad.setOnClickListener(v -> {
                    holder.main_ad_layout.setVisibility(View.GONE);
//                    LinearLayout.LayoutParams params= (LinearLayout.LayoutParams) holder.today_title.getLayoutParams();
//                    params.topMargin= AdaptionUtil.dip2px(mContext, 9);
                });
            } else {
                holder.main_ad_layout.setVisibility(View.GONE);
            }

        } else if (tempHolder instanceof EmptyViewHolder) {
            EmptyViewHolder holder = (EmptyViewHolder) tempHolder;
            holder.btn_get_card.setOnClickListener(mMarketClickListener);

        } else if (tempHolder instanceof DeckAdapter.ViewHolder) {
            position--;
            DeckAdapter.ViewHolder holder = (ViewHolder) tempHolder;
            AbstractDeckTreeNode node = mDeckList.get(position);
            // Set the expander icon and padding according to whether or not there are any subdecks
//        RelativeLayout deckLayout = holder.deckLayout;
//        int rightPadding = (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_right_padding);
            Timber.d("onBindViewHolder,mHasSubdecks:" + mHasSubdecks);
//        if (mHasSubdecks) {
//            int smallPadding = (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_left_padding_small);
//            deckLayout.setPadding(smallPadding, 0, rightPadding, 0);
//            holder.deckExpander.setVisibility(View.VISIBLE);
//            holder.fileBag.setVisibility(View.VISIBLE);
//            // Create the correct expander for this deck
//
//            setDeckExpander(holder.deckExpander,holder.fileBag,holder.indentView, node);
//        } else {
//            holder.deckExpander.setVisibility(View.GONE);
//            holder.fileBag.setVisibility(View.GONE);
//            int normalPadding = (int) deckLayout.getResources().getDimension(R.dimen.deck_picker_left_padding);
//            deckLayout.setPadding(normalPadding, 0, rightPadding, 0);
//        }
//        holder.fileBag.setOnClickListener(v -> holder.deckExpander.performClick());
//        holder.filebagLayout.setOnClickListener(v -> holder.deckExpander.performClick());
//        if (node.hasChildren()) {
//            holder.deckExpander.setTag(node.getDid());
//            holder.deckExpander.setOnClickListener(mDeckExpanderClickListener);
//        } else {
//            holder.deckExpander.setOnClickListener(null);
//        }
//        holder.deckLayout.setBackgroundResource(mRowCurrentDrawable);
            // Set background colour. The current deck has its own color
//        if (isCurrentlySelectedDeck(node)) {
//            holder.deckLayout.setBackgroundResource(mRowCurrentDrawable);
//            if (mPartiallyTransparentForBackground) {
//                setBackgroundAlpha(holder.deckLayout, SELECTED_DECK_ALPHA_AGAINST_BACKGROUND);
//            }
//        } else {
//            CompatHelper.getCompat().setSelectableBackground(holder.deckLayout);
//        }
            // Set deck name and colour. Filtered decks have their own colour
            holder.deckName.setText(node.getLastDeckNameComponent());
            if (mCol.getDecks().isDyn(node.getDid())) {
                holder.deckName.setTextColor(mDeckNameDynColor);
            } else {
                holder.deckName.setTextColor(mDeckNameDefaultColor);
            }
            double[] data  ;
            // Set the card counts and their colors
            if (node.shouldDisplayCounts()) {
                holder.deckNew.setText(String.valueOf(node.getNewCount()));
//            holder.deckNew.setTextColor((node.getNewCount() == 0) ? mZeroCountColor : mNewCountColor);
                holder.deckLearn.setText(String.valueOf(node.getLrnCount()));
//            holder.deckLearn.setTextColor((node.getLrnCount() == 0) ? mZeroCountColor : mLearnCountColor);
                holder.deckRev.setText(String.valueOf(node.getRevCount()+node.getLrnCount()));
//            holder.deckRev.setTextColor((node.getRevCount() == 0) ? mZeroCountColor : mReviewCountColor);
                data = node.getStudyData();
            }else {
                data = new double[] {0.0,0.0,0.0};
            }

            // Store deck ID in layout's tag for easy retrieval in our click listeners
            holder.deckLayout.setTag(node.getDid());
            holder.countsLayout.setTag(node.getDid());
//            double[] data = calculateStat(mCol, node.getDid());

            double percent=0;
            if(data[2]==0){
                //新卡已学完，显示已掌握
                percent = (data[0] + data[1] + data[2] <= 0) ? 0 : (data[0] / (data[0] + data[1] + data[2]) * 100);
                holder.handled_num.setText(String.format(Locale.CHINA, "%.0f/%.0f", data[0], (data[0] + data[1] + data[2])));

                holder.handled_percent.setText((String.format(Locale.CHINA, "已掌握 %.1f", percent)) + "%");
            }else {
                percent = (data[0] + data[1] + data[2] <= 0) ? 0 : ((data[0]+ data[1]) / (data[0] + data[1] + data[2]) * 100);
                holder.handled_num.setText(String.format(Locale.CHINA, "%.0f/%.0f", data[0]+ data[1], (data[0] + data[1] + data[2])));

                holder.handled_percent.setText((String.format(Locale.CHINA, "已学 %.1f", percent)) + "%");
            }

            holder.studyProgress.setMax(100 * 100);
            holder.studyProgress.setProgress((int) (percent * 100));

            // Set click listeners
            holder.deckLayout.setOnClickListener(mDeckClickListener);
            holder.deckLayout.setOnLongClickListener(mDeckLongClickListener);
            holder.countsLayout.setOnClickListener(mCountsClickListener);
        }

    }


//    private double[] calculateStat(Collection col, long deckId) {
//        //计算已熟悉/全部卡片数
//        Stats stats = new Stats(col, deckId);
//        stats.calculateCardTypes(TYPE_LIFE);
//        return stats.getSeriesList()[0];
//    }


    private void setBackgroundAlpha(View view, @SuppressWarnings("SameParameterValue") double alphaPercentage) {
        Drawable background = view.getBackground().mutate();
        background.setAlpha((int) (255 * alphaPercentage));
        view.setBackground(background);
    }


    private boolean isCurrentlySelectedDeck(AbstractDeckTreeNode node) {
        return node.getDid() == mCol.getDecks().current().optLong("id");
    }


    @Override
    public int getItemCount() {
        return mDeckList.isEmpty() ? 2 : mDeckList.size() + 1;
    }


    private void setDeckExpander(ImageButton expander, ImageView fileBag, ImageButton indent, AbstractDeckTreeNode node) {
        boolean collapsed = mCol.getDecks().get(node.getDid()).optBoolean("collapsed", false);
        // Apply the correct expand/collapse drawable
        Timber.d("setDeckExpander,collapsed:" + collapsed + ",has children：" + node.hasChildren());
        if (collapsed) {
            expander.setImageDrawable(mExpandImage);
            fileBag.setImageDrawable(mFileBag);
            expander.setContentDescription(expander.getContext().getString(R.string.expand));
        } else if (node.hasChildren()) {
            expander.setImageDrawable(mCollapseImage);
            fileBag.setImageDrawable(mFileBag);
            expander.setContentDescription(expander.getContext().getString(R.string.collapse));
        } else {
            expander.setImageDrawable(mNoExpander);
//            expander.setVisibility(View.GONE);
            fileBag.setImageDrawable(mNoExpander);
        }
        // Add some indenting for each nested level
        int width = (int) indent.getResources().getDimension(R.dimen.keyline_1) * 2 * node.getDepth();
        indent.setMinimumWidth(width);
    }


    private void processNodes(List<AbstractDeckTreeNode> nodes) {
        for (AbstractDeckTreeNode node : nodes) {
            // If the default deck is empty, hide it by not adding it to the deck list.
            // We don't hide it if it's the only deck or if it has sub-decks.
            if (node.getFullDeckName().contains("::")) {
                continue;//只添加一级节点
            }
            if (node.getDid() == 1 && nodes.size() > 1 && !node.hasChildren()) {
                if (mCol.getDb().queryScalar("select 1 from cards where did = 1") == 0) {
                    continue;
                }
            }
            // If any of this node's parents are collapsed, don't add it to the deck list
            for (Deck parent : mCol.getDecks().parents(node.getDid())) {
                mHasSubdecks = true;    // If a deck has a parent it means it's a subdeck so set a flag
                if (parent.optBoolean("collapsed")) {
                    return;
                }
            }
            mDeckList.add(node);

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


    @Nullable
    public Integer getNewCard() {
        if (mNumbersComputed) {
            return mNew;
        } else {
            return null;
        }
    }


    @Nullable
    public Integer getReviewCard() {
        if (mNumbersComputed) {
            return mRev;
        } else {
            return null;
        }
    }


    public List<AbstractDeckTreeNode> getDeckList() {
        return mDeckList;
    }
    private GestureDetector detector;
    private class TouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub
//            Toast.makeText(getApplicationContext(), "----?", event.getAction()).show();
            return detector.onTouchEvent(event);
//            return super.onTouch(v,event);
        }

    }
    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
//onFling方法的第一个参数是 手指按下的位置， 第二个参数是 手指松开的位置，第三个参数是手指的速度

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            float startX = e1.getX();//通过e1.getX（）获得手指按下位置的横坐标
            float endX = e2.getX();//通过e2.getX（）获得手指松开位置的横坐标
            float startY = e1.getY();//通过e1.getY（）获得手指按下位置的纵坐标
            float endY = e2.getY();//通过e2.getY（）获得手指松开的纵坐标
            if ((startX - endX) > 50 && Math.abs(startY - endY) < 200) {
                //(startX - endX) > 50 是手指从按下到松开的横坐标距离大于50
                // Math.abs(startY - endY) < 200 是手指从按下到松开的纵坐标的差的绝对值

                //在这里通过Intent实现界面转跳
                ((DeckPicker)mContext) .openCardBrowser();
            }

            if ((endX - startX) > 50 && Math.abs(startY - endY) <200) {
                //在这里通过Intent实现界面转跳
            }
//返回值是重点：如果返回值是true则动作可以执行，如果是flase动作将无法执行
            return true;
        }
    }
}
