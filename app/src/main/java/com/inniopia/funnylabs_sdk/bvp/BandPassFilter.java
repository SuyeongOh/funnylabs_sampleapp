package com.inniopia.funnylabs_sdk.bvp;

public class BandPassFilter {
    private HighPassFilter highPassFilter;
    private LowPassFilter lowPassFilter;

    public BandPassFilter(double highPassCutoff, double lowPassCutoff) {
        reset(highPassCutoff, lowPassCutoff);
    }

    public void reset(double highPassCutoff, double lowPassCutoff) {
        this.highPassFilter = new HighPassFilter(highPassCutoff);
        this.lowPassFilter = new LowPassFilter(lowPassCutoff);
    }

    public double filter(double value, double deltaTime) {
        double highPassed = this.highPassFilter.filter(value, deltaTime);
        return this.lowPassFilter.filter(highPassed, deltaTime);
    }
}
