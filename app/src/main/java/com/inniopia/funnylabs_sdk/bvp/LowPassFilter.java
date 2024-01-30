package com.inniopia.funnylabs_sdk.bvp;

public class LowPassFilter {
    private double previousValue;
    private double cutoffFrequency;

    public LowPassFilter(double cutoffFrequency) {
        this.previousValue = 0;
        this.cutoffFrequency = cutoffFrequency;
    }

    public double filter(double value, double deltaTime) {
        double RC = 1.0 / (this.cutoffFrequency * 2 * Math.PI);
        double alpha = deltaTime / (RC + deltaTime);
        double newValue = this.previousValue + alpha * (value - this.previousValue);
        this.previousValue = newValue;
        return newValue;
    }
}