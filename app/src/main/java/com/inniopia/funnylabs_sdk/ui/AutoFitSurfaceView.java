package com.inniopia.funnylabs_sdk.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class AutoFitSurfaceView extends SurfaceView {

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private float aspectRatio = 0f;

    public void setAspectRatio(int width, int height){
        aspectRatio = (float)width / (float)height;
        getHolder().setFixedSize(width, height);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
//
//        int newWidth = width, newHeight = height;
//
//        if(aspectRatio == 0){
//            setMeasuredDimension(width, height);
//            return;
//        }
//
//        if(getDisplay().getWidth()/(float)width > 1){
//            if(getDisplay().getWidth() * aspectRatio > getDisplay().getHeight()){
//                //height에 맞춰 width 조절
//                newHeight = getDisplay().getHeight();
//                newWidth = (int)(height * aspectRatio);
//            } else{
//                //width에 맞춰 height 조절
//                newWidth = getDisplay().getWidth();
//                newHeight = (int)(newWidth/aspectRatio);
//            }
//        }
//        setMeasuredDimension(newWidth, newHeight);

    }
}