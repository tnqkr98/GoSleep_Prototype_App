package com.example.gosleep;

import com.google.gson.annotations.SerializedName;

public class NetworkMonitorData {

    public static class ReqDataCo2{
        @SerializedName("Product_id")
        String Product_id;
        @SerializedName("Epoch")
        int Epoch;
        @SerializedName("Co2_concent")
        int Co2_concent;

        public ReqDataCo2(String product_id, int epoch,int co2_concent) {
            Product_id = product_id;
            Epoch = epoch;
            Co2_concent = co2_concent;
        }
    }

    public static class ResDataCo2{
        @SerializedName("code")
        String code;

        public ResDataCo2(String code) { this.code = code; }
        public String getCode() { return code;}
    }

}
