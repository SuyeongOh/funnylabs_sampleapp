package com.inniopia.funnylabs_sdk;

import android.graphics.ImageFormat;

import androidx.camera.core.CameraSelector;

public class Config {
    public static final int IMAGE_BUFFER_SIZE = 3;
    public static final int PIXEL_FORMAT = ImageFormat.YUV_420_888;
    public static final int DELEGATE_CPU = 0;
    public static final int DELEGATE_GPU = 1;
    public static final float THRESHOLD_DEFAULT = 0.5F;
    public static final int OTHER_ERROR = 0;
    public static final int GPU_ERROR = 1;

    public static final int CAMERA_DIRECTION_FRONT = CameraSelector.LENS_FACING_FRONT;
    public static final int CAMERA_DIRECTION_BACK = CameraSelector.LENS_FACING_BACK;
    public static int USE_CAMERA_DIRECTION = CAMERA_DIRECTION_FRONT;

    public static final String TYPE_OF_BIG = "big";
    public static final String TYPE_OF_SMALL = "small";
    public static final String TYPE_OF_OUT = "out";

    public static int SCREEN_WIDTH = 0;
    public static int SCREEN_HEIGHT = 0;

    public static double USER_BMI = 0f;

    public static final double MIN_FREQUENCY = 0.83f;
    public static final double MAX_FREQUENCY = 2.5f;

    public static int IMAGE_READER_WIDTH;
    public static int IMAGE_READER_HEIGHT;
}
