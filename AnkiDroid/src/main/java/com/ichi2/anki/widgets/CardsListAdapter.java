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

import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.CardBrowser;
import com.ichi2.anki.R;
import com.ichi2.libanki.Card;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

public class CardsListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private LayoutInflater mLayoutInflater;
    private CardListAdapterCallback mCallback;
    private View.OnClickListener mDeckClickListener;
    private View.OnClickListener mMarkClickListener;
    private View.OnClickListener mSetFlagClickListener;
    private View.OnLongClickListener mDeckLongClickListener;


    public void setDeckClickListener(View.OnClickListener listener) {
        mDeckClickListener = listener;
    }


    public void setDeckLongClickListener(View.OnLongClickListener listener) {
        mDeckLongClickListener = listener;
    }


    public void setMarkClickListener(View.OnClickListener listener) {
        mMarkClickListener = listener;
    }


    public void setFlagClickListener(View.OnClickListener listener) {
        mSetFlagClickListener = listener;
    }


    public void setCallback(CardListAdapterCallback mCallback) {
        this.mCallback = mCallback;
    }


    public interface CardListAdapterCallback {
        List<CardBrowser.CardCache> getCards();

        void onChangeMultiMode(boolean isMultiMode);

        void onItemSelect(int selectCount);

    }



    private AnkiActivity mContext;



    public static class CardsViewHolder extends RecyclerView.ViewHolder {

        public final LinearLayout itemRoot;
        public final TextView deckQuestion;
        public final TextView deckAnswer;
        public final TextView reviewCount;
        public final TextView forgetCount;
        public final TextView due;
        public final ImageView flag;
        public final ImageView mark;
        public final CheckBox stick;


        public CardsViewHolder(View v) {
            super(v);
            itemRoot = v.findViewById(R.id.DeckPickerHoriz);
            deckQuestion = v.findViewById(R.id.deck_question);
            deckAnswer = v.findViewById(R.id.deck_answer);
            reviewCount = v.findViewById(R.id.review_count);
            forgetCount = v.findViewById(R.id.forget_count);
            flag = v.findViewById(R.id.flag_icon);
            mark = v.findViewById(R.id.mark_icon);
            stick = v.findViewById(R.id.stick);
            due = v.findViewById(R.id.due);
//            fileBag =   v.findViewById(R.id.file_bag);
//            filebagLayout =   v.findViewById(R.id.ll_file_bag);
        }
    }



    public static class HeaderViewHolder extends RecyclerView.ViewHolder {

        public final TextView cardsCount;
        public final TextView edit;


        public HeaderViewHolder(View v) {
            super(v);
            cardsCount = v.findViewById(R.id.search_result_num);
            edit = v.findViewById(R.id.edit);

        }
    }



    private Drawable mEmptyFlag;
    private Drawable mEmptyMark;


    public CardsListAdapter(LayoutInflater layoutInflater, AnkiActivity context, CardListAdapterCallback callback) {
        mLayoutInflater = layoutInflater;
        mCallback = callback;
        mContext = context;
        int[] attrs = new int[] {
                R.attr.itemIconMarkEmpty,//0
                R.attr.itemIconFlagEmpty,//1
        };
        TypedArray ta = context.obtainStyledAttributes(attrs);
        mEmptyMark = ta.getDrawable(0);
        mEmptyFlag = ta.getDrawable(1);

    }


    public boolean isMultiCheckableMode() {
        return mMultiCheckableMode;
    }


    boolean mMultiCheckableMode = false;


    public void setMultiCheckable(boolean enable) {
        if(mMultiCheckableMode==enable)return;
        mMultiCheckableMode = enable;
        if (mMultiCheckableMode) {
            mSelectedPosition.clear();
            mCheckedCards.clear();
        }
        mCallback.onChangeMultiMode(enable);
        notifyDataSetChanged();
    }


    public void selectItem(boolean selectAll) {
        if (mMultiCheckableMode) {
            mSelectedPosition.clear();
            mCheckedCards.clear();
            if (selectAll) {
                for (CardBrowser.CardCache cache : mCallback.getCards()) {
                    mSelectedPosition.add(cache.getId());
                }
                mCheckedCards.addAll(mCallback.getCards());
            }
            notifyDataSetChanged();
        }

    }


    private final int VIEW_TYPE_HEADER = 0;
    private final int VIEW_TYPE_LIST = 1;


    @Override
    public int getItemViewType(int position) {

        return position == 0 ? VIEW_TYPE_HEADER : VIEW_TYPE_LIST;
    }

    private int mStudyCountLayoutRes;
    public void setStudyCountLayoutRes(int resLayout){
        mStudyCountLayoutRes=resLayout;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == VIEW_TYPE_LIST) {
            v = mLayoutInflater.inflate(R.layout.deck_item_self_study, parent, false);
            return new CardsViewHolder(v);
        } else {
            v = mLayoutInflater.inflate(mStudyCountLayoutRes>0?mStudyCountLayoutRes:R.layout.item_self_study_count, parent, false);
            return new HeaderViewHolder(v);
        }
    }


    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder tempHolder, int position) {
//        Timber.i("onBindViewHolder:%s", position);
        if (tempHolder instanceof CardsViewHolder) {
            position--;
            CardsViewHolder holder = (CardsViewHolder) tempHolder;
            CardBrowser.CardCache card = mCallback.getCards().get(position);
            String question = card.getColumnHeaderText(CardBrowser.Column.QUESTION);
            if(card.getColumnHeaderText(CardBrowser.Column.SUSPENDED).equals("True")){
                holder.deckQuestion.setTextColor(ContextCompat.getColor(mContext, R.color.new_primary_text_third_color));
                holder.deckAnswer.setTextColor(ContextCompat.getColor(mContext, R.color.new_primary_text_third_color));
            }
            if (question.isEmpty()) {
                holder.deckQuestion.setText(card.getColumnHeaderText(CardBrowser.Column.MEDIA_NAME));
            } else {
                holder.deckQuestion.setText(card.getColumnHeaderText(CardBrowser.Column.QUESTION));
            }

//            String answer = card.getColumnHeaderText(CardBrowser.Column.ANSWER);
//            if (answer.isEmpty()) {
                holder.deckAnswer.setText(card.getColumnHeaderText(CardBrowser.Column.ANSWER));
//            } else {
//                holder.deckQuestion.setText(card.getColumnHeaderText(CardBrowser.Column.QUESTION));
//            }
            holder.reviewCount.setText(card.getColumnHeaderText(CardBrowser.Column.REVIEWS));
            holder.forgetCount.setText(card.getColumnHeaderText(CardBrowser.Column.LAPSES));
            holder.due.setText(card.getColumnHeaderText(CardBrowser.Column.DUE2));

            holder.mark.setTag(card.getId());
            holder.flag.setTag(card.getId());
            holder.itemRoot.setTag(card.getId());
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) holder.itemRoot.getLayoutParams();
            if (mMultiCheckableMode) {
                layoutParams.rightMargin = -60;
                holder.itemRoot.setLayoutParams(layoutParams);
                holder.stick.setVisibility(View.VISIBLE);
            } else {
                layoutParams.rightMargin = 0;
                holder.itemRoot.setLayoutParams(layoutParams);
                holder.stick.setVisibility(View.GONE);
            }
            holder.stick.setChecked(mSelectedPosition.contains(card.getId()));
//            Timber.i("mSelectedPosition size " + mSelectedPosition.size() + "," + card.getId());
            holder.stick.setOnClickListener(v -> {
                if (mSelectedPosition.contains(card.getId())) {
                    mSelectedPosition.remove(card.getId());
                    mCheckedCards.remove(card);
                } else {
                    mSelectedPosition.add(card.getId());
                    mCheckedCards.add(card);
                }
                mCallback.onItemSelect(mSelectedPosition.size());
            });
//            holder.stick.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    Timber.i("check id "+card.getId()+","+isChecked);
//                    if(isChecked)mSelectedPosition.add(card.getId());
////                    else  mSelectedPosition.remove(card.getId());
//                }
//            });
            if (card.getCard().note().hasTag("marked")) {
                holder.mark.setImageResource(R.mipmap.mark_star_normal);
//                holder.mark.setVisibility(View.VISIBLE);
            } else {
                holder.mark.setImageResource(R.mipmap.note_star_unselected);
//                holder.flag.setVisibility(View.GONE);
            }
            if (getFlagRes(card.getCard()) != -1) {
                holder.flag.setImageResource(getFlagRes(card.getCard()));
//                holder.flag.setVisibility(View.VISIBLE);
            } else {
//                holder.flag.setImageDrawable(mEmptyFlag);
                holder.flag.setImageResource(R.mipmap.note_flag_unselected);
//                holder.flag.setVisibility(View.GONE);
            }


            holder.mark.setOnClickListener(mMarkClickListener);
            holder.flag.setOnClickListener(mSetFlagClickListener);

            holder.itemRoot.setOnClickListener(mDeckClickListener);
            holder.itemRoot.setOnLongClickListener(mDeckLongClickListener);
        } else {
            HeaderViewHolder holder = (HeaderViewHolder) tempHolder;
            holder.cardsCount.setText(String.format("筛选出%d张卡片", getItemCount() - 1));
            holder.cardsCount.setVisibility(isMultiCheckableMode()?View.GONE:View.VISIBLE);

            if(holder.edit!=null){
                holder.edit.setVisibility(isMultiCheckableMode()?View.GONE:View.VISIBLE);
                holder.edit.setOnClickListener(v -> {
                    setMultiCheckable(!mMultiCheckableMode);
                });
            }
        }
    }


    private  List<Long> mSelectedPosition = new ArrayList<>();
    private Set<CardBrowser.CardCache> mCheckedCards = Collections.synchronizedSet(new LinkedHashSet<>());

    public int selectItemCount(){
        return mSelectedPosition.size();
    }

    public List<Long> getSelectedItemIds(){
        return mSelectedPosition;
    }
    public Set<CardBrowser.CardCache> getSelectedCards(){
        return mCheckedCards;
    }
    public long[] getSelectedItemIdArray(){
        long[] temp=new long[mSelectedPosition.size()];
        for(int i=0;i<temp.length;i++){
            temp[i]=mSelectedPosition.get(i);
        }
        return temp;
    }
    public static int getFlagRes(Card card) {
        int flag = card.userFlag();
        switch (flag) {
            case 1:
                return R.mipmap.mark_red_flag_normal;
            case 2:
                return R.mipmap.mark_yellow_flag_normal;
            case 3:
                return R.mipmap.mark_green_flag_normal;
            case 4:
                return R.mipmap.mark_blue_flag_normal;
            default:
                return -1;
        }
    }


    @Override
    public int getItemCount() {
        return mCallback.getCards().size() + 1;
    }


}
