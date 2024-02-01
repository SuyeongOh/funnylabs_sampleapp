package com.inniopia.funnylabs_sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.mediapipe.tasks.components.containers.Detection;
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult;

import java.util.List;

import androidx.annotation.Nullable;

public class OverlayView extends View {
    private FaceDetectorResult result;
    private Paint boxPaint = new Paint();
    private Float scaleFactorWidth = 1f;
    private Float scaleFactorHeight = 1f;
    private Rect bounds;

    private static int FULL_SIZE_OF_WIDTH = 720;
    private static int FULL_SIZE_OF_HEIGHT = 1280;
    private static int FULL_SIZE_OF_DETECTION = FULL_SIZE_OF_WIDTH * FULL_SIZE_OF_HEIGHT;
    //popup의 민감도를 바꾸려면 이부분을 바꾸세요.

    private RectF predicisionBox = null;
    private float STANDARD_BIG_SIZE_OF_POPUP = 3f/4f;
    private float STANDARD_SMALL_SIZE_OF_POPUP = 1f/9f;

    private boolean isClear = false;
    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if(result != null) {
            if(result.detections().size() > 0){
                if(predicisionBox != null){
                    canvas.drawRect(predicisionBox, boxPaint);
                    predicisionBox = null;
                }
//                Detection person = result.detections().get(0);
//                RectF boundingBox = person.boundingBox();
//
//                float realTop = boundingBox.top * scaleFactorHeight;
//                float realBottom = boundingBox.bottom * scaleFactorHeight;
//                float realLeft = boundingBox.left * scaleFactorWidth;
//                float realRight = boundingBox.right * scaleFactorWidth;
//
//
//                @SuppressLint("DrawAllocation") //warning 방지 없어도 무관함
//                RectF drawRect = new RectF(realLeft, realTop, realRight, realBottom);
//                canvas.drawRect(drawRect, boxPaint);
                if(isClear){
                    isClear = false;
                }
            }
        }
    }

    public void setResults(FaceDetectorResult detectResult, int imageWidth, int imageHeight) {
        result = detectResult;

        scaleFactorWidth = getWidth() / (float)imageWidth;
        scaleFactorHeight = getHeight() / (float)imageHeight;

        invalidate();
    }

    public void setResults(FaceDetectorResult detectResult, RectF box) {
        result = detectResult;

        predicisionBox = box;

        invalidate();
    }

    public void clear(){
        //중복 방지 차원
        if(!isClear){
            isClear = true;
            result = null;
            boxPaint.reset();
            invalidate();
            initPaints();
        }
    }

    private void initPaints(){
        boxPaint.setColor(getContext().getColor(R.color.mp_primary));
        boxPaint.setStrokeWidth(8);
        boxPaint.setStyle(Paint.Style.STROKE);
    }

    public void setFullsize(int width, int height){
        FULL_SIZE_OF_WIDTH = width;
        FULL_SIZE_OF_HEIGHT = height;
    }
    public boolean isBigSize(RectF bBox){
        //bBox는 현재 1280x720 기준으로 동작하기 때문에 해당 사이즈에 맞게 값을 설정해줘야함
        return !(bBox.width() * bBox.height() < FULL_SIZE_OF_DETECTION * STANDARD_BIG_SIZE_OF_POPUP);
    }

    public boolean isSmallSize(RectF bBox){
        return !((bBox.width() > 50) && (bBox.height() > 50));
    }

    public boolean isOutBoundary(RectF bBox){
        Log.d("Jupiter", "FULL SIZE : " + FULL_SIZE_OF_WIDTH + ", " + FULL_SIZE_OF_HEIGHT);
        return bBox.left + bBox.width() > FULL_SIZE_OF_WIDTH
                || bBox.top + bBox.height() > FULL_SIZE_OF_HEIGHT;
    }
}
