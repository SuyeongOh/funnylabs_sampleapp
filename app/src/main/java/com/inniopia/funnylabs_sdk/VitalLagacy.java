package com.inniopia.funnylabs_sdk;

import android.graphics.Bitmap;
import android.util.Log;

import com.github.psambit9791.jdsp.signal.Detrend;
import com.github.psambit9791.jdsp.signal.Smooth;
import com.github.psambit9791.jdsp.signal.peaks.FindPeak;
import com.github.psambit9791.jdsp.signal.peaks.Peak;
import com.github.psambit9791.jdsp.transform.DiscreteFourier;
import com.github.psambit9791.jdsp.transform.Hilbert;
import com.inniopia.funnylabs_sdk.bvp.BandPassFilter;
import com.inniopia.funnylabs_sdk.data.ResultVitalSign;
import com.inniopia.funnylabs_sdk.utils.FloatUtils;
import com.paramsen.noise.Noise;

import java.util.ArrayList;
import java.util.List;

import jsat.linear.DenseMatrix;
import jsat.linear.DenseVector;
import jsat.linear.Matrix;
import jsat.linear.Vec;
import uk.me.berndporr.iirj.Butterworth;

import static java.lang.Math.abs;

public class VitalLagacy {
    private static final int BUFFER_SIZE = 512;
    private static final int BPM_BUFFER_SIZE = 1;
    private static final int BPM_CALCULATION_FREQUENCY = 512;
    private static final int BP_CALCULATION_FREQUENCY = 512;
    public static int VIDEO_FRAME_RATE = 30;

    private static final int GAUSSIAN_W = 72;
    private static final int DETREND_POWER = 6;
    private long[] frameTimeArray = new long[BUFFER_SIZE];

    public static class Result {
        public float LF_HF_ratio = 0;
        public double HR_result = 0;
        public double RR_result = 0;
        public double spo2_result = 0;
        public double sdnn_result = 0;
        public double BP = 0;
        public double SBP = 0;
        public double DBP = 0;
    }

    private final Result lastResult = new Result();

    float RR1 = 0;
    float RR2 = 0;
    double SDNN_result;
    private long firstFrameTime = 0;
    private long lastFrameTime = 0;
    ArrayList<Float> NN = new ArrayList<Float>();
    ArrayList<Float> result = new ArrayList<Float>();
    final float r_banddy = -2335.36371041202f;
    final float b_banddy = -2335.36371041202f;

    private final double[][]f_pixel_buff = new double[3][BUFFER_SIZE];
    private int bufferIndex = 0;
    private int pixelIndex = 0;
    private final float[] bpm_Buffer = new float[BPM_BUFFER_SIZE];
    private final float[] rr_Buffer = new float[BPM_BUFFER_SIZE];
    private int bpm_buffer_index = 0;

    final float[] bpm = {0.0f};
    final float[] rr = {0.0f};

    public Result calculateVital(FaceImageModel model) {
        //rescale: resizez + gaussian
        if(pixelIndex == 0) firstFrameTime = model.frameUtcTimeMs;

        frameTimeArray[bufferIndex] = model.frameUtcTimeMs;

        Bitmap bitmap = model.bitmap;
        Bitmap test_cropped = Bitmap.createScaledBitmap(bitmap, GAUSSIAN_W, GAUSSIAN_W, false); //Gaussian pyramid 2단계 50x50 resize

        float totalR = 0, totalG = 0, totalB = 0;
        for(int i = 0; i < GAUSSIAN_W; i++){
            for(int j = 0; j< GAUSSIAN_W; j++){
                int pixels_buffer = test_cropped.getPixel(i,j);

                int r = (pixels_buffer & 0xFF0000) >> 16;
                int g = (pixels_buffer & 0x00FF00) >> 8;
                int b = (pixels_buffer & 0x0000FF);

                totalR += r;
                totalG += g;
                totalB += b;
            }
        }
        totalR = totalR / (float)(GAUSSIAN_W * GAUSSIAN_W);
        totalG = totalG / (float)(GAUSSIAN_W * GAUSSIAN_W);
        totalB = totalB / (float)(GAUSSIAN_W * GAUSSIAN_W);

        f_pixel_buff[0][bufferIndex]= Float.parseFloat(String.valueOf(totalR));
        f_pixel_buff[1][bufferIndex]= Float.parseFloat(String.valueOf(totalG));
        f_pixel_buff[2][bufferIndex]= Float.parseFloat(String.valueOf(totalB));

        if (bufferIndex % BPM_CALCULATION_FREQUENCY == BPM_CALCULATION_FREQUENCY - 1) {
            lastFrameTime = model.frameUtcTimeMs;
            //VIDEO_FRAME_RATE = 1000 / (int)((lastFrameTime - firstFrameTime) / pixelIndex);
            double[] pre_processed = preprocessing(f_pixel_buff);
            bpm_Buffer[bpm_buffer_index] = (get_HR(pre_processed,BUFFER_SIZE));
            rr_Buffer[bpm_buffer_index] = (get_RR(pre_processed,BUFFER_SIZE));
            bpm_buffer_index = (bpm_buffer_index + 1) % BPM_BUFFER_SIZE;
            int i;
            for (i = 0, bpm[0] = 0, rr[0] = 0; i < BPM_BUFFER_SIZE; i++) {
                bpm[0] += bpm_Buffer[i];
                rr[0] += rr_Buffer[i];
                if (bpm_Buffer[i] == 0.0f && rr_Buffer[i] == 0.0f) {
                    break;
                }
            }
            lastResult.HR_result = Math.round(bpm[0] /(i));
            lastResult.RR_result = Math.round(rr[0] /(i));
            //--------SDNN --------------------//
//            lastResult.sdnn_result = SDNN(bpm_Buffer, bpm_buffer_index);
//            lastResult.sdnn_result = Math.round(lastResult.sdnn_result * 100) / 100.0;
//            lastResult.sdnn_result = lastResult.sdnn_result * 100;
            //--------SDNN --------------------//
            lastResult.LF_HF_ratio = LF_HF_ratio(pre_processed,BUFFER_SIZE);

            try {
                lastResult.spo2_result = spo2(f_pixel_buff[0], f_pixel_buff[2], VIDEO_FRAME_RATE);
            } catch (Exception e) {
                lastResult.spo2_result = 0;
            }
            lastResult.spo2_result = Math.round(lastResult.spo2_result);
        }
        if ((pixelIndex % BP_CALCULATION_FREQUENCY) == (BP_CALCULATION_FREQUENCY - 1)) {
            double[] preprocessed_g = get_normG(f_pixel_buff[1]);
            double peak_avg = get_peak_avg(preprocessed_g,true);
            double valley_avg = get_peak_avg(preprocessed_g,false);
            Log.d("BP",""+peak_avg+":"+valley_avg);
            double bmi = Config.USER_BMI;

            //원래는 23.7889
            lastResult.SBP = 23.7889 + (95.4335 * peak_avg) + (4.5958 * bmi) - (5.109 * peak_avg * bmi);
            lastResult.DBP = -17.3772 - (115.1747 * valley_avg) + (4.0251 * bmi) + (5.2825 * valley_avg * bmi);

            lastResult.BP = lastResult.SBP * 0.33 + lastResult.DBP * 0.66;

            lastResult.HR_result = FloatUtils.mean(bpm_Buffer);
            lastResult.RR_result = FloatUtils.mean(rr_Buffer);
        }
        bufferIndex = (bufferIndex + 1) % BUFFER_SIZE;
        pixelIndex++;
        return lastResult;
    }

    public double[] preprocessing(double[][] pixel){

        String mode = "rectangular";
        int wsize = 4;


        double[][] smoothPixel = new double[pixel.length][pixel[0].length];
        double[] doubleTimeArray = new double[frameTimeArray.length];
        smoothPixel[0] = new Smooth(pixel[0], wsize, mode).smoothSignal();
        smoothPixel[1] = new Smooth(pixel[1], wsize, mode).smoothSignal();
        smoothPixel[2] = new Smooth(pixel[2], wsize, mode).smoothSignal();

        for(int i = 0; i < frameTimeArray.length; i++){
            doubleTimeArray[i] = frameTimeArray[i];
        }
        double[] timeSmoothing = new Smooth(doubleTimeArray, wsize, mode).smoothSignal();

        Vec v = new DenseVector(1);
        v.mutableAdd(1);

        DenseMatrix signal = new DenseMatrix(smoothPixel);

        Matrix[] qr = signal.qr();
        Matrix q = qr[0];

        Vec sVec = q.getColumn(0);

        DenseMatrix S = new DenseMatrix(v, sVec);

        DenseMatrix identify = DenseMatrix.eye(3);
        Matrix P = identify.subtract(S.transpose().multiply(S));

        Matrix Y = P.multiply(signal);

        Vec bvpVec = Y.getRow(1);


        Detrend d2 = new Detrend(bvpVec.arrayCopy(), DETREND_POWER);
        double[] d_g = d2.detrendSignal();


        BandPassFilter bpf = new BandPassFilter(Config.MAX_FREQUENCY, Config.MIN_FREQUENCY);

        double[] bpf_signal = new double[d_g.length];
        for(int i = 1; i < d_g.length; i++){
            bpf_signal[i] = bpf.filter(d_g[i], timeSmoothing[i] - timeSmoothing[i - 1]);
        }

        DiscreteFourier fft_r = new DiscreteFourier(bpf_signal);
        fft_r.dft();

        return fft_r.returnAbsolute(true);
    }

    public double[] get_normG(double[] g_pixel){
        String mode = "rectangular";
        int wsize = 5;
        Smooth g = new Smooth(g_pixel, wsize, mode);
        double[] s_g = g.smoothSignal();
        Detrend d2 = new Detrend(s_g,DETREND_POWER);
        double[] d_g = d2.detrendSignal();
        Vec v_g = DenseVector.toDenseVec(d_g);
        v_g = v_g.subtract(v_g.mean());
        v_g = v_g.divide(v_g.standardDeviation());
        return v_g.arrayCopy();
    }

    public double get_peak_avg(double[] arr, boolean flag){ // flag 0 : vally 1 : peak
        List<Double> peak = new ArrayList<Double>();
        if(flag)
            for( int i = 1 ; i < arr.length-1 ; i++){
                if( arr[i-1]< arr[i] && arr[i] > arr[i+1])
                    peak.add(arr[i]);
            }
        else{
            for( int i = 1 ; i < arr.length-1 ; i++){
                if( arr[i-1] > arr[i] && arr[i] < arr[i+1])
                    peak.add(arr[i]);
            }
        }

        double[] arrDouble = peak.stream() .mapToDouble(Double::doubleValue) .toArray();

        Vec v_g = DenseVector.toDenseVec(arrDouble);

        return v_g.mean();
    }


    public float get_HR(double[] real_dft, int buff_size) {
        int max_index = 0;
        float max_val = 0;
        float filter_interval = VIDEO_FRAME_RATE / (float)buff_size;
        for( int i =0 ; i < real_dft.length ; i++){
            if( i * filter_interval < 0.83 )
                continue;
            else if( i * filter_interval > 2.5){
                break;
            }
            else{
                if( real_dft[i] > max_val ) {
                    max_val = (float) real_dft[i];
                    max_index = i;
                }
            }
        }
        return max_index * filter_interval * 60;
    }
    public float get_RR(double[] real_dft, int buff_size) {
        int max_index = 0;
        float max_val = 0;
        float filter_interval = 10 / (float)buff_size;
        for( int i =0 ; i < real_dft.length ; i++){
            if( i * filter_interval < 0.18 )
                continue;
            else if( i * filter_interval > 0.5){
                break;
            }
            else{
                if( real_dft[i] > max_val ) {
                    max_val = (float) real_dft[i];
                    max_index = i;
                }
            }
        }
        return max_index * filter_interval * 48;
    }

    public float LF_HF_ratio(double[] real_dft,int buff_size){

        float LF = 0.0f;
        float HF = 0.0f;
        float filter_interval = VIDEO_FRAME_RATE / (float)buff_size;
        for( int i =0 ; i < real_dft.length ; i++){
            if( 0.8<= i * filter_interval && i * filter_interval < 1.5)
                LF += real_dft[i]; //1을 저장공간으로 사용
            else if( 1.5<= i * filter_interval && i * filter_interval <=4.0)
                HF += real_dft[i]; //1을 저장공간으로 사용
        }
        return LF/HF;
    }

    public double SDNN(float[] bpm_Buffer, int bpm_buffer_index){

        float sum = 0.0f;
        float avg;
        float dev = 0;
        double devSqvSum = 0;
        double var;


        if (bpm_Buffer[1] != 0) {
            if (bpm_buffer_index == 0) {

                RR1 = bpm_Buffer[BPM_BUFFER_SIZE - 1] / 60;
                RR2 = bpm_Buffer[0] / 60;

            } else {
                RR1 = bpm_Buffer[bpm_buffer_index - 1] / 60;
                RR2 = bpm_Buffer[bpm_buffer_index] / 60;
            }

            Log.d("R1R2", "RR1 : " + RR1 + " " + "RR2 : " + RR2);
            NN.add(abs(RR2 - RR1));

            if (NN.size() >= 2) {
                result.add(abs((NN.get(NN.size() - 2)) - (NN.get(NN.size() - 1))));
                Log.d("NN_result", "size : " + NN.size() + " " + "NN1 : " + NN.get(NN.size() - 2) + " " + "NN2 : " + NN.get(NN.size() - 1) + " " + "result : " + result);

                for (int i = 0; i < result.size(); i++) {
                    sum = sum + result.get(i);
                }

                avg = sum / result.size();

                for (int i = 0; i < result.size(); i++) {
                    dev = (abs(result.get(i) - avg));
                    devSqvSum = (devSqvSum + Math.pow(dev, 2));
                }

                var = devSqvSum / (result.size() - 1);
                SDNN_result = Math.sqrt(var);

                Log.d("NN_result", "SDNN : " + SDNN_result + " " + "size" + " " + result.size());
                Log.d("NN_result", "dev : " + dev + " " + "devSqvSum" + " " + devSqvSum + " " + "var  : " + var);
                Log.d("NN_result", "sum : " + sum + " " + "avg : " + avg);
            }

        }
        return SDNN_result;
    }

    public double spo2(double[] spo2_pixel_buff_R, double[] spo2_pixel_buff_B, int VIDEO_FRAME_RATE) {

        //---------BPF_FILTER-----------//
        double[] R_kernel = bandPassKernel(VIDEO_FRAME_RATE, 0.3d / (VIDEO_FRAME_RATE / 2), 2.5 / (VIDEO_FRAME_RATE / 2)); //BPF 생성 0.3/15~ 2.5/15사이
        double[] B_kernel = bandPassKernel(VIDEO_FRAME_RATE, 0.3d / (VIDEO_FRAME_RATE / 2), 2.5 / (VIDEO_FRAME_RATE / 2));
        double[] R_result = filter(spo2_pixel_buff_R, R_kernel); //BPF 통과_ 특정 주파수의 색만 남는다고함..주파수( 색의 파장 )
        double[] B_result = filter(spo2_pixel_buff_B, B_kernel);
        double R_reult_mean = 0;
        double B_reult_mean = 0;
        //--------SIGNAL SMOOTHING---// --> BPF의 평균값 제거를 통한 노이즈 제거
        for (double r_tmp : R_result) {
            R_reult_mean += r_tmp;
        }
        for (double b_tmp : B_result) {
            B_reult_mean += b_tmp;
        }
        R_reult_mean /= VIDEO_FRAME_RATE;
        B_reult_mean /= VIDEO_FRAME_RATE;
        for (int i = 0; i < R_result.length; i++) {
            R_result[i] -= R_reult_mean;
        }
        for (int i = 0; i < B_result.length; i++) {
            B_result[i] -= B_reult_mean;
        }
        //-------SECONDARY BPF(ButterWorth)-----// --> ButterWorth 필터로 2차 필터링 통과대역외의 일정 부분을 살리기 위함
        Butterworth butterworth = new Butterworth();
        butterworth.bandPass(9, VIDEO_FRAME_RATE, 0.1, 0.1);
        for (int i = 0; i < R_result[i]; i++) {
            R_result[i] = butterworth.filter(R_result[i]);
        }
        for (int i = 0; i < B_result[i]; i++) {
            B_result[i] = butterworth.filter(B_result[i]);
        }
        //----ENVELOPE 신호 포락선(상부, 하부 포함) ------// --> 필터링 결과의 포락선 제거
        Hilbert hilbert_R = new Hilbert(R_result);
        hilbert_R.hilbertTransform();
        double[] analytical_signal_R = hilbert_R.getAmplitudeEnvelope(); //--> Hilbert transform이 각 신호의 othogonal한 성분을 뽑아줌으로 결과 값 자체가 envelope한 신호임
        double R_envelope_mean = 0.0d;
        for (int i = 0; i < analytical_signal_R.length; i++) {
            R_envelope_mean += analytical_signal_R[i];
        }
        R_envelope_mean /= analytical_signal_R.length; // 포락선의 평균 측정


        Hilbert hilbert_B = new Hilbert(R_result);
        hilbert_B.hilbertTransform();
        double[] analytical_signal_B = hilbert_R.getAmplitudeEnvelope();
        Log.d("A", "AA");
        double B_envelope_mean = 0.0d;
        for (int i = 0; i < analytical_signal_R.length; i++) {
            B_envelope_mean += analytical_signal_R[i];
        }
        B_envelope_mean /= analytical_signal_B.length;

        //--- DETREND ---//{ 확인 필요 }
        //--R
        Detrend d_R = new Detrend(R_result, DETREND_POWER);
        double[] out_R = d_R.detrendSignal();

        ArrayList<Double> r_list = new ArrayList<Double>();
        for (int i = 0; i < out_R.length; i++) {
            if (abs(out_R[i]) < R_envelope_mean)
                r_list.add(R_envelope_mean);
        }
        //--B
        Detrend d_B = new Detrend(B_result, DETREND_POWER);
        double[] out = d_B.detrendSignal();

        ArrayList<Double> b_list = new ArrayList<Double>();
        for (int i = 0; i < out.length; i++) {
            if (abs(out[i]) < B_envelope_mean)
                b_list.add(B_envelope_mean);
        }

        //---BANDY FILTER---//
        double[] banddy_r = new double[r_list.size()];
        for (int i = 0; i < r_list.size(); i++) {
            banddy_r[i] = r_list.get(i) * r_banddy;
        }

        double[] banddy_b = new double[b_list.size()];
        for (int i = 0; i < b_list.size(); i++) {
            banddy_b[i] = b_list.get(i) * b_banddy;
        }

        //                            Hilbert hilbert = new Hilbert(R_result);
//                            hilbert.getOutput();
        //--- SpO2 estimate ---//
        //--- DFT ---//
        DiscreteFourier fft_r = new DiscreteFourier(banddy_r);
        fft_r.dft();
        double[] out_dft_r_real = fft_r.returnAbsolute(true);
        double[][] out_dft_r = fft_r.returnFull(true);

        DiscreteFourier fft_b = new DiscreteFourier(banddy_b);
        fft_b.dft();
        double[] out_dft_b_real = fft_b.returnAbsolute(true);
        double[][] out_dft_b = fft_b.returnFull(true);

        //--- Find Peak  & DEVIDED---//
        if (out_dft_r_real.length == 0) return 0.0;
        FindPeak fp_r = new FindPeak(out_dft_r_real);
        Peak out_r = fp_r.detectPeaks();
        int[] peaks_r = out_r.getPeaks();
        double[] through_dft_r = new double[peaks_r.length];
        for (int i = 0; i < peaks_r.length; i++) {
            through_dft_r[i] = abs(out_dft_r_real[peaks_r[i]] / out_dft_r_real[0]);
        }

        if (out_dft_b_real.length == 0) return 0.0;
        FindPeak fp_b = new FindPeak(out_dft_b_real);
        Peak out_b = fp_b.detectPeaks();
        int[] peaks_b = out_b.getPeaks();
        double[] through_dft_b = new double[peaks_b.length];
        for (int i = 0; i < peaks_b.length; i++) {
            through_dft_b[i] = abs(out_dft_b_real[peaks_b[i]] / out_dft_b_real[0]);
        }
        //--- CAL SPO2 RAW ---//
        int spo2_len = 0, spo2_cnt = 0;
        double spo2 = 0.0d;
        if (through_dft_r.length > through_dft_b.length) {
            spo2_len = through_dft_b.length;
        } else {
            spo2_len = through_dft_r.length;
        }
        for (int i = 0; i < spo2_len; i++) {
            double spo2_raw_tmp = 96.58d - -0.015 * through_dft_r[i] / through_dft_b[i] * 100;
            if (spo2_raw_tmp < 100) {
                spo2 += spo2_raw_tmp;
                spo2_cnt++;
            }
        }

        spo2 /= spo2_cnt;

        return spo2;

    }

    private static double[] blackmanWindow(int length) {

        double[] window = new double[length];
        double factor = Math.PI / (length - 1);

        for (int i = 0; i < window.length; ++i) {
            window[i] = 0.42d - (0.5d * Math.cos(2 * factor * i)) + (0.08d * Math.cos(4 * factor * i));
        }

        return window;
    }

    public static double[] lowPassKernel(int length, double cutoffFreq, double[] window) {

        double[] ker = new double[length + 1];
        double factor = Math.PI * cutoffFreq * 2;
        double sum = 0;

        for (int i = 0; i < ker.length; i++) {
            double d = i - length / 2;
            if (d == 0) ker[i] = factor;
            else ker[i] = Math.sin(factor * d) / d;
            ker[i] *= window[i];
            sum += ker[i];
        }

        // Normalize the kernel
        for (int i = 0; i < ker.length; ++i) {
            ker[i] /= sum;
        }

        return ker;
    }

    public static double[] bandPassKernel(int length, double lowFreq, double highFreq) {

        double[] ker = new double[length + 1];
        double[] window = blackmanWindow(length + 1);

        // Create a band reject filter kernel using a high pass and a low pass filter kernel
        double[] lowPass = lowPassKernel(length, lowFreq, window);

        // Create a high pass kernel for the high frequency
        // by inverting a low pass kernel
        double[] highPass = lowPassKernel(length, highFreq, window);
        for (int i = 0; i < highPass.length; ++i) highPass[i] = -highPass[i];
        highPass[length / 2] += 1;

        // Combine the filters and invert to create a bandpass filter kernel
        for (int i = 0; i < ker.length; ++i) ker[i] = -(lowPass[i] + highPass[i]);
        ker[length / 2] += 1;

        return ker;
    }

    public static double[] filter(double[] signal, double[] kernel) {

        double[] res = new double[signal.length];

        for (int r = 0; r < res.length; ++r) {

            int M = Math.min(kernel.length, r + 1);
            for (int k = 0; k < M; ++k) {
                res[r] += kernel[k] * signal[r - k];
            }
        }

        return res;
    }

    public int HSV_fft(Noise noise2, float[] frame_hue_avg, float[] fft_hue, int HUE_FRAME, boolean[] hue_filter){

        noise2.fft(frame_hue_avg, fft_hue);

        fft_hue[0] = fft_hue[1] = 0;

        for (int i = 0; i < HUE_FRAME / 2; i++) {
            fft_hue[i * 2 + 2] = fft_hue[i * 2 + 2] * (hue_filter[i] ? 1.0f : 0.0f);
        }

        int hue_hr_index = 0;
        float hue_max = 0.0f;

        for (int i = 0; i < HUE_FRAME / 2; i++) {
            if (fft_hue[i * 2 + 2] > hue_max) {
                hue_max = fft_hue[i * 2 + 2];
                hue_hr_index = i;
            }
        }

        return hue_hr_index;
    }

    public static ResultVitalSign toResultVitalSign(Result result){
        ResultVitalSign convert = new ResultVitalSign();
        convert.HR_result = result.HR_result;
        convert.RR_result = result.RR_result;
        convert.BP = result.BP;
        convert.DBP = result.DBP;
        convert.SBP = result.SBP;
        convert.LF_HF_ratio = result.LF_HF_ratio;
        convert.sdnn_result = result.sdnn_result;
        convert.spo2_result = result.spo2_result;
        return convert;
    }

    private int[] getAdjustPixel(Bitmap bitmap, int x, int y){
        int[] adjustPixel = new int[9];
        if(x >= 1){
            if(y >= 1){
                adjustPixel[0] = bitmap.getPixel(x - 1, y - 1);
                adjustPixel[6] = bitmap.getPixel(x - 1, y + 1);
                adjustPixel[3] = bitmap.getPixel(x - 1, y);
                adjustPixel[1] = bitmap.getPixel(x, y - 1);
                adjustPixel[4] = bitmap.getPixel(x, y);
                adjustPixel[7] = bitmap.getPixel(x, y + 1);
                adjustPixel[2] = bitmap.getPixel(x + 1, y - 1);
                adjustPixel[5] = bitmap.getPixel(x + 1, y);
                adjustPixel[8] = bitmap.getPixel(x + 1, y + 1);
            }
        }
        return adjustPixel;
    }

    public void clearAnalysis(){
        firstFrameTime = 0;
        lastFrameTime = 0;
        bufferIndex = 0;
        pixelIndex = 0;
        bpm_buffer_index = 0;
    }
}
