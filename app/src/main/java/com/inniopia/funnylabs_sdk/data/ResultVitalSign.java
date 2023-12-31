package com.inniopia.funnylabs_sdk.data;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class ResultVitalSign {
    //LF_HF_ratio는 stress 판별 value입니다. 3.0을 넘으면 스트레스가 많음/ 안넘으면 적절 지표로 저희 내부에선 생각하고 있습니다.
    @Getter @Setter public static ResultVitalSign vitalSignData = new ResultVitalSign();
    @Getter @Setter public float LF_HF_ratio = 0;
    @Getter @Setter public double HR_result = 0;
    @Getter @Setter public double RR_result = 0;
    @Getter @Setter public double spo2_result = 0;
    @Getter @Setter public double sdnn_result = 0;
    @Getter @Setter public double BP = 0;
    @Getter @Setter public double SBP = 0;
    @Getter @Setter public double DBP = 0;
}
