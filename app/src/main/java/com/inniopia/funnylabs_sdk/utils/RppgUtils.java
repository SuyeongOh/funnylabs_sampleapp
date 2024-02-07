package com.inniopia.funnylabs_sdk.utils;

public class RppgUtils {
    public static int TimeToLen(double seconds, long[] eventTimes) {
        if (eventTimes == null || eventTimes.length == 0) {
            return 1; // Return 1 if the input list is null or empty
        }

        long firstEventTime = eventTimes[0];
        for (int index = 0; index < eventTimes.length; index++) {
            if (eventTimes[index] - firstEventTime > 1000) { // 1.0 초 차이를 밀리초 단위로 계산
                return index;
            }
        }
        return 1;
    }

    public static int[] PeakDetect(double[] green_signal, long[] frameTimeArray, double bpm) {
        int windowSize = TimeToLen(1.0, frameTimeArray);

        windowSize = (windowSize * 60) / (int)bpm;

        int[] peakArray = new int[green_signal.length];

        int peakIndex = 0;
        while(peakIndex < (green_signal.length - windowSize)) {
            for (int i = peakIndex; i < Math.min(peakIndex + windowSize, green_signal.length); i++) {
                if (green_signal[i] > green_signal[peakIndex]) {
                    peakIndex = i;
                }
            }
            peakArray[peakIndex] = 1;

            for (int i = peakIndex; i < Math.min(peakIndex + windowSize, green_signal.length); i++) {
                if (green_signal[i] < green_signal[peakIndex]) {
                    peakIndex = i;
                }
            }
            peakArray[peakIndex] = 2;
        }
        return peakArray;
    }


}
