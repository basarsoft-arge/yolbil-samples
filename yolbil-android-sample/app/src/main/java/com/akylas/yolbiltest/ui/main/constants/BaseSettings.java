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
}