package com.inniopia.funnylabs_sdk.bvp;

public class HighPassFilter {
    private double previousValue;
    private double cutoffFrequency;

    public HighPassFilter(double cutoffFrequency) {
        this.previousValue = 0;
        this.cutoffFrequency = cutoffFrequency;
    }

    public double filter(double value, double deltaTime) {
        double RC = 1.0 / (this.cutoffFrequency * 2 * Math.PI);
        double alpha = RC / (RC + deltaTime);
        double newValue = value - alpha * (value - this.previousValue);
        this.previousValue = newValue;
        return newValue;
    }
}
