package com.akylas.yolbiltest.ui.main.constants;


public class BaseSettings {

    // Singleton Instance
    public static final BaseSettings INSTANCE = new BaseSettings();

    // Private Constructor
    private BaseSettings() {}

    // Configuration fields

    private final String baseUrl = "https://domain";

    private final String accountId = "ACC_ID";
    private final String appCode = "APP_CODE";

    // Katman olarak eklenecek url
    private final String networkPbfUrl = "Katman URL";

    // Getter Methods
    public String getAccountId() {
        return accountId;
    }

    public String getAppCode() {
        return appCode;
    }
    public String getBASE_URL() {
        return baseUrl;
    }

    public String getBaseVectorPbfUrl() {
        return baseUrl + "/Service/api/v1/VectorMap/Pbf?accId=" + accountId + "&appCode=" + appCode + "&x={x}&y={y}&z={zoom}";
    }

    public String getNetworkPbfUrl() {
        return networkPbfUrl;
    }
}
