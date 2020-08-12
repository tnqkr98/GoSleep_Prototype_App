package com.example.gosleep;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {
    private final static String BASE_URL = "http://ec2-54-180-107-2.ap-northeast-2.compute.amazonaws.com:80";
    private static Retrofit retrofit = null;

    private NetworkClient(){}
    public static Retrofit getClient(){
        if(retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}

