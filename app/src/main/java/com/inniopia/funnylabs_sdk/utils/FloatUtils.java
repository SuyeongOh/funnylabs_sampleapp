package com.inniopia.funnylabs_sdk.utils;

public class FloatUtils {
    public static float sum(float[] array){
        float total = 0;
        for(float i : array){
            total += i;
        }
        return total;
    }

    public static float mean(float[] array){
        return sum(array)/ array.length;
    }

    public static double[] floatArrayToDoubleArray (float[] array){
        double[] ret = new double[array.length];
        for(int i = 0; i < array.length; i++){
            ret[i] = array[i];
        }
        return ret;
    }
}
