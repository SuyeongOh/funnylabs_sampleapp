package com.inniopia.funnylabs_sdk.utils;

import android.graphics.Point;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Size;
import android.view.Display;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
public class CameraUtils {
    public static class SmartSize{
        Size size;
        int longRange;
        int shortRange;
        public SmartSize(int width, int height){
            size = new Size(width, height);
            longRange = Math.max(width, height);
            shortRange = Math.min(width, height);
        }

        public Size getSize(){
            return size;
        }
    }

    public static final SmartSize SIZE_1080P = new SmartSize(1920, 1080);

    public static SmartSize getDisplaySmartSize(Display display){
        Point p = new Point();
        display.getSize(p);
        return new SmartSize(p.x, p.y);
    }

    public static Size getPreviewOutputSize(Display display, CameraCharacteristics cameraCharacteristics, Class targetClass){
        SmartSize screenSize = getDisplaySmartSize(display);

        StreamConfigurationMap configMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        Size[] allSizes = configMap.getOutputSizes(targetClass);

        List<SmartSize> validSizes = Arrays.stream(allSizes).sorted(Comparator.comparingInt(size -> size.getWidth() * size.getHeight()))
                .map(size -> new SmartSize(size.getWidth(), size.getHeight()))
                .collect(Collectors.toList());

        Collections.reverse(validSizes);

        //return screenSize.size;

        return validSizes.get(validSizes.size()/2).size;
    }
}
