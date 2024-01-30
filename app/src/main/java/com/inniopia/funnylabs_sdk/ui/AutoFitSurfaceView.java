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
//        if(aspectRatio == 0){
//            setMeasuredDimension(width, height);
//        } else{
//            int newWidth;
//            int newHeight;
//
//            float actualRatio = (width > height) ? aspectRatio : 1/aspectRatio;
//            if(width < height * actualRatio){
//                newHeight = height;
//                newWidth = Math.round(height * actualRatio);
//            } else{
//                newWidth = width;
//                newHeight = Math.round(width / actualRatio);
//            }
//
//            setMeasuredDimension(newWidth, newHeight);
//        }
    }
}