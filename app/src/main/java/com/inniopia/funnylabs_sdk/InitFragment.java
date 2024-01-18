package com.inniopia.funnylabs_sdk;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;

import com.inniopia.funnylabs_sdk.data.Constant;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class InitFragment extends Fragment {

    private EditText bmiInputView;
    private Button applyBtn;
    private Switch cameraDirectionSwitch;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_init, container, false);

        bmiInputView = view.findViewById(R.id.init_view_bmi_input);
        applyBtn = view.findViewById(R.id.init_btn_submit);
        cameraDirectionSwitch = view.findViewById(R.id.init_view_camera_switch);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bmi = bmiInputView.getText().toString();
                Config.USER_BMI = Double.parseDouble(bmi);

                if(cameraDirectionSwitch.isChecked()){
                    Config.USE_CAMERA_DIRECTION = Constant.CAMERA_DIRECTION_BACK;
                }

                bmiInputView.clearFocus();
                MainActivity activity = (MainActivity) getActivity();
                activity.replaceFragment(new MainFragment());
            }
        });
    }
}
