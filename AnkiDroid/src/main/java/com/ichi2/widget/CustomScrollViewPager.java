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

package com.ichi2.widget;


import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Scroller;

import androidx.viewpager.widget.ViewPager;
import timber.log.Timber;

/**
 * 解决ViewPager滑动过于灵敏，只有滑动距离大于100才滑到另一页
 *
 * @author Administrator
 */
public class CustomScrollViewPager extends ViewPager {

    private static final String TAG = "dzt_pager";
    private static final int MOVE_LIMITATION = 100;// 触发移动的像素距离
    private float mLastMotionX; // 手指触碰屏幕的最后一次x坐标
    private int mCurScreen;

    private Scroller mScroller; // 滑动控件


    public CustomScrollViewPager(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }


    public CustomScrollViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        init(context);
    }


    private void init(Context context) {
        mScroller = new Scroller(context);
        mCurScreen = 0;// 默认设置显示第一个VIEW
    }


    public void setScrolledListener(ScrolledListener listener) {
        this.listener = listener;
    }


    private ScrolledListener listener;



    public interface ScrolledListener {
        void onScroll();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        final int action = event.getAction();
        final float x = event.getX();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                break;
            case MotionEvent.ACTION_UP:
                Timber.i("on action up:"+x+","+mLastMotionX+","+MOVE_LIMITATION);
                if (Math.abs(x - mLastMotionX) > MOVE_LIMITATION) {
                    // snapToDestination(); // 跳到指定页
                    if (listener != null) {
                        listener.onScroll();
                    } else {
                        snapToScreen(getCurrentItem());
                    }
//                    return true;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(event);
    }


//    @Override
    public void computeScroll() {
        // TODO Auto-generated method stub
        super.computeScroll();

//        if (mScroller.computeScrollOffset()) {
//            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
//            invalidate();
//        }

    }


    /**
     * 根据滑动的距离判断移动到第几个视图
     */
    public void snapToDestination() {
        final int screenWidth = getWidth();
        final int destScreen = (getScrollX() + screenWidth / 2) / screenWidth;
        snapToScreen(destScreen);
    }


    /**
     * 滚动到制定的视图
     *
     * @param whichScreen 视图下标
     */
    public void snapToScreen(int whichScreen) {
        // whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() -
        // 1));
        if (getScrollX() != (whichScreen * getWidth())) {

            final int delta = whichScreen * getWidth() - getScrollX();

            mScroller.startScroll(getScrollX(), 0, delta, 0,
                    Math.abs(delta) * 2);
            mCurScreen = whichScreen;
            invalidate();
        }
    }


    /**
     * 用于拦截手势事件的，每个手势事件都会先调用这个方法。Layout里的onInterceptTouchEvent默认返回值是false,
     * 这样touch事件会传递到childview控件 ，如果返回false子控件可以响应，否则了控件不响应，这里主要是拦截子控件的响应，
     * 对ViewGroup不管返回值是什么都会执行onTouchEvent
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        // TODO Auto-generated method stub
        final int action = arg0.getAction();
        final float x = arg0.getX();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastMotionX = x;
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(arg0);
    }
}
