package com.example.gosleep;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface NetworkAPI {
    @POST("/gosleep/mobile/modechange")
    Call<GoSleepActivity.ResData> callData(@Body GoSleepActivity.ReqData data);
}
