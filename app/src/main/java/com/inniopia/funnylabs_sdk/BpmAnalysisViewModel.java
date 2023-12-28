package com.inniopia.funnylabs_sdk;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import com.inniopia.funnylabs_sdk.data.ResultVitalSign;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class BpmAnalysisViewModel extends AndroidViewModel {
    private Vital vital;
    private Context mContext;

    public BpmAnalysisViewModel(@NonNull Application application, Context context) {
        super(application);
        vital = new Vital(context);
        mContext = context;
    }

    public void addFaceImageModel(@NonNull FaceImageModel faceImageModel){
        calculateAnalysis(faceImageModel);
    }

    private void calculateAnalysis(@NonNull FaceImageModel faceImageModel){
        if(!vital.calculatePOSVital(faceImageModel, true)){
            return;
        }
        Intent intent = new Intent(mContext, ResultActivity.class);
        mContext.startActivity(intent);
    }
}
