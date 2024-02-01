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
        private Size size;
        private int longSide;
        private int shortSide;

        public SmartSize(int width, int height) {
            this.size = new Size(width, height);
            this.longSide = Math.max(size.getWidth(), size.getHeight());
            this.shortSide = Math.min(size.getWidth(), size.getHeight());
        }

        public Size getSize() {
            return size;
        }

        public int getLong() {
            return longSide;
        }

        public int getShort() {
            return shortSide;
        }

        @Override
        public String toString() {
            return "SmartSize(" + longSide + "x" + shortSide + ")";
        }
    }

    public static final SmartSize SIZE_1080P = new SmartSize(1920, 1080);

    public static SmartSize getDisplaySmartSize(Display display){
        Point p = new Point();
        display.getSize(p);
        return new SmartSize(p.x, p.y);
    }

    public static Size getPreviewOutputSize(Display display, CameraCharacteristics cameraCharacteristics, Class targetClass){
        // Find which is smaller: screen or 1080p
        SmartSize screenSize = getDisplaySmartSize(display);
        boolean hdScreen = screenSize.getLong() >= SIZE_1080P.getLong() || screenSize.getShort() >= SIZE_1080P.getShort();
        SmartSize maxSize = hdScreen ? SIZE_1080P : screenSize;

        // If image format is provided, use it to determine supported sizes; else use target class
        StreamConfigurationMap config = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        assert config != null;
        Size[] allSizes;
        allSizes = config.getOutputSizes(targetClass);

        // Get available sizes and sort them by area from largest to smallest
        return java.util.Arrays.stream(allSizes)
                .map(size -> new SmartSize(size.getWidth(), size.getHeight()))
                .sorted((a, b) -> Integer.compare(b.getLong() * b.getShort(), a.getLong() * a.getShort()))
                .filter(size -> size.getLong() <= maxSize.getLong() && size.getShort() <= maxSize.getShort())
                .findFirst()
                .orElse(null)
                .getSize();
    }
}
