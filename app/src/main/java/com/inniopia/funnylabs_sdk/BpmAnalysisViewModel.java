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

    public boolean addFaceImageModel(@NonNull FaceImageModel faceImageModel){
        return calculateAnalysis(faceImageModel);
    }

    private boolean calculateAnalysis(@NonNull FaceImageModel faceImageModel){
        return vital.calculatePOSVital(faceImageModel, true);
    }
    public void clearAnalysis(){
        vital.clearAnalysis();
    }
    public Vital getVital() {
        return vital;
    }
}
