package com.inniopia.funnylabs_sdk;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.inniopia.funnylabs_sdk.data.ResultVitalSign;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class Vital {
    private Interpreter mPOSModule = null;
    private VitalLagacy mVitalLagacy;
    private static final String POS_TFL_MODULE_NAME = "pos.tflite";
    private static final int VIDEO_FRAME_RATE = 30;
    private ResultVitalSign lastResult = new ResultVitalSign();

    private static final int FACE_WIDTH = 50;
    private static final int FACE_HEIGHT = 50;

    int l = (int)(VIDEO_FRAME_RATE * 1.6);

    static final int FACE_PIXEL_COUNT = FACE_WIDTH * FACE_HEIGHT;
    static final int FRAME_PIXEL_COUNT = 1 * 3;
    public static final int BATCH_SIZE = 1;
    public static final int FRAME_WINDOW_SIZE = 256;
    private final int[] face_pixels = new int[FACE_PIXEL_COUNT];

    private final List<Long> mUtcTimeTempList = new ArrayList<>();
    private final float[] inputFloatArray = new float[BATCH_SIZE * 3 * FRAME_WINDOW_SIZE  * 1 * 1];

    private static float totalR = 0f;
    private static float totalG = 0f;
    private static float totalB = 0f;

    public Vital(Context context) {
        mVitalLagacy = new VitalLagacy();
        try {
            mPOSModule = new Interpreter(loadModelFile(context.getAssets()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean calculatePOSVital(@NonNull FaceImageModel faceImageModel, boolean activate_lagacy){
        if(activate_lagacy) {
            ResultVitalSign.vitalSignData = VitalLagacy.toResultVitalSign(mVitalLagacy.calculateVital(faceImageModel.bitmap));
            return ResultVitalSign.vitalSignData.SBP != 0;
        }
        Bitmap faceBitmap =
                Bitmap.createScaledBitmap(faceImageModel.bitmap, FACE_WIDTH, FACE_HEIGHT, false);
        faceBitmap.getPixels(face_pixels, 0, FACE_WIDTH, 0, 0, FACE_WIDTH, FACE_HEIGHT);
        int pixelOffset = FRAME_PIXEL_COUNT * mUtcTimeTempList.size();
        for (int i = 0; i < FACE_PIXEL_COUNT; i++) {
            final int c = face_pixels[i];
            float r = ((c >> 16) & 0xff) / 255.0f;
            float g = ((c >> 8) & 0xff) / 255.0f;
            float b = ((c) & 0xff) / 255.0f;

//            try{
//                inputFloatArray[pixelOffset + 3 * i] = r;
//                inputFloatArray[pixelOffset + 3 * i + 1] = g;
//                inputFloatArray[pixelOffset + 3 * i + 2] = b;
//            } catch (Exception e){e.printStackTrace();
//
//            }
            totalR += r;
            totalG += g;
            totalB += b;
        }

        inputFloatArray[pixelOffset] = totalR/FACE_PIXEL_COUNT;
        inputFloatArray[pixelOffset + 1] = totalG/FACE_PIXEL_COUNT;
        inputFloatArray[pixelOffset + 2] = totalB/FACE_PIXEL_COUNT;
        totalR = 0;
        totalG = 0;
        totalB = 0;

        mUtcTimeTempList.add(faceImageModel.frameUtcTimeMs);

        if (mUtcTimeTempList.size() == FRAME_WINDOW_SIZE * BATCH_SIZE) {

            FloatBuffer buffer = FloatBuffer.allocate(BATCH_SIZE * FRAME_WINDOW_SIZE  * FRAME_PIXEL_COUNT);
            buffer.put(inputFloatArray);
            buffer.rewind();

//            float[][] output = new float[1][257];
            float[] output = new float[1];
            mPOSModule.run(buffer, output);

            //시간이 좀 오래걸립니다. loading뷰 등 추가하면 좋아요
            lastResult.HR_result = output[0];
//            lastResult.RR_result = output[1];
//            lastResult.LF_HF_ratio = output[0][2];
//            lastResult.spo2_result = output[0][3];
//            lastResult.sdnn_result = output[0][4];
//            lastResult.SBP = output[0][5];
//            lastResult.DBP = output[0][6];

            Log.d("Result", String.format("HR : %f, RR : %f, Spo2 : %f" +
                            "\nStress : %f, SDNN : %f, (SBP, DBP) : ( %f, %f)"
                    ,lastResult.HR_result, lastResult.RR_result, lastResult.spo2_result
                    ,lastResult.LF_HF_ratio, lastResult.sdnn_result, lastResult.SBP, lastResult.DBP));
            mUtcTimeTempList.clear();
            ResultVitalSign.vitalSignData = lastResult;
            return true;
        } else{
            return false;
        }
    }

    private ByteBuffer loadModelFile(AssetManager assets)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(POS_TFL_MODULE_NAME);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
