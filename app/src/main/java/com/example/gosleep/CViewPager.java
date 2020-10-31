package com.example.gosleep;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.viewpager.widget.ViewPager;

public class CViewPager extends ViewPager {

    boolean enable;
    public CViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CViewPager(Context context) {
        super(context);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (enable) return super.onInterceptTouchEvent(ev);
        else{
            if (ev.getAction() == MotionEvent.ACTION_MOVE);
            else if(super.onInterceptTouchEvent(ev)) super.onTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (enable) return super.onTouchEvent(ev);
        else return ev.getAction() != MotionEvent.ACTION_MOVE && super.onTouchEvent(ev);
    }

    public void setEnable(boolean enable){
        this.enable = enable;
    }
}