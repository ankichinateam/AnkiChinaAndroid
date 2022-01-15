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

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ichi2.anki.AnkiActivity;
import com.ichi2.anki.R;

import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

public class OrderListAdapter extends RecyclerView.Adapter<OrderListAdapter.ViewHolder> {


    private LayoutInflater mLayoutInflater;
    private View.OnClickListener mItemClickListener;
    private View.OnClickListener mTvOrderClickListener;
    private View.OnClickListener mIvOrderClickListener;


    public void setItemClickListener(View.OnClickListener listener) {
        mItemClickListener = listener;
    }


    /**
     * 展示排序方式
     *
     * @param listener
     */
    public void setTvOrderClickListener(View.OnClickListener listener) {
        mTvOrderClickListener = listener;
    }


    /**
     * 修改升序/降序
     *
     * @param listener
     */
    public void setIvOrderClickListener(View.OnClickListener listener) {
        mIvOrderClickListener = listener;
    }


    private AnkiActivity mContext;



    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final RelativeLayout rootLayout;
        public final TextView orderName;
        public final TextView orderText;
        public final ImageView order;


        public ViewHolder(View v) {
            super(v);
            orderName = v.findViewById(R.id.name);
            rootLayout = v.findViewById(R.id.root_layout);
            orderText = v.findViewById(R.id.tv_order);
            order = v.findViewById(R.id.order);

        }
    }



    private Drawable mEmptyFlag;
    private Drawable mEmptyMark;


    public OrderListAdapter(LayoutInflater layoutInflater, AnkiActivity context) {
        mLayoutInflater = layoutInflater;
        mContext = context;
    }


    List<OrderItem> mData = new ArrayList<>();


    public void setItems(List<OrderItem> data) {
        mData = data;
        notifyDataSetChanged();
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mLayoutInflater.inflate(R.layout.item_order_list, parent, false);
        return new ViewHolder(v);
    }


    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
//        Timber.i("onBindViewHolder:%s", position);
        OrderItem item = mData.get(position);

        holder.orderName.setText(item.name);
        holder.orderName.setSelected(item.selected);

        holder.order.setSelected( item.asc);
        holder.order.setVisibility(item.selected?View.VISIBLE:View.GONE);

        holder.orderText.setText(item.asc?"升序":"降序");
        holder.orderText.setVisibility(item.selected?View.VISIBLE:View.GONE);


        holder.order.setOnClickListener(mIvOrderClickListener);
        holder.orderText.setOnClickListener(mTvOrderClickListener);

        holder.rootLayout.setTag(item);
//        holder.orderName.setOnClickListener(mItemClickListener);
        holder.rootLayout.setOnClickListener(mItemClickListener);
    }


    @Override
    public int getItemCount() {
        return mData.size();
    }


    public static class OrderItem {
        public String name;
        public int index;
        public boolean selected;
        public boolean asc;
    }
}
