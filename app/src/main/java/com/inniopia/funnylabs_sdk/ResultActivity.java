package com.inniopia.funnylabs_sdk;


import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;

import com.inniopia.funnylabs_sdk.data.ResultVitalSign;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {

    private TextView hr_textview;
    private TextView rr_textview;
    private TextView stress_textview;
    private TextView spo2_textview;
    private TextView sdnn_textview;
    private TextView sbp_textview;
    private TextView dbp_textview;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        bind();
        setValue();
    }

    private void bind(){
        hr_textview = findViewById(R.id.result_hr_value);
        rr_textview = findViewById(R.id.result_rr_value);
        stress_textview = findViewById(R.id.result_stress_value);
        spo2_textview = findViewById(R.id.result_spo2_value);
        sdnn_textview = findViewById(R.id.result_sdnn_value);
        sbp_textview = findViewById(R.id.result_sbp_value);
        dbp_textview = findViewById(R.id.result_dbp_value);
    }

    @SuppressLint("SetTextI18n")
    private void setValue(){
        ResultVitalSign vitalsign = ResultVitalSign.vitalSignData;

        hr_textview.setText(Double.toString(vitalsign.HR_result));
        rr_textview.setText(Double.toString(vitalsign.RR_result));
        stress_textview.setText(Double.toString(vitalsign.LF_HF_ratio));
        spo2_textview.setText(Double.toString(vitalsign.spo2_result));
        sdnn_textview.setText(Double.toString(vitalsign.sdnn_result));
        sbp_textview.setText(Double.toString(vitalsign.SBP));
        dbp_textview.setText(Double.toString(vitalsign.DBP));

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
