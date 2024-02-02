package com.inniopia.funnylabs_sdk.data;

public class Rppg {
    private int BUFFER_SIZE;
    public double[] lastBvpSignal;
    public double[] lastHrSignal;
    public double[][]f_pixel_buff;
    public float[] bpm_Buffer;
    public float[] rr_Buffer;
    public long[] frameTimeArray;
    public boolean[] peakArray;

    public Rppg(int buffer_size){
        BUFFER_SIZE = buffer_size;
        lastBvpSignal = new double[BUFFER_SIZE];
        lastHrSignal = new double[BUFFER_SIZE/2];
        f_pixel_buff = new double[3][BUFFER_SIZE];
        bpm_Buffer = new float[BUFFER_SIZE];
        rr_Buffer = new float[BUFFER_SIZE];
        frameTimeArray = new long[BUFFER_SIZE];
        peakArray = new boolean[BUFFER_SIZE];
    }
}
