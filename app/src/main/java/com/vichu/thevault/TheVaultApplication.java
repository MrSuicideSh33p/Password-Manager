package com.vichu.thevault;

import android.app.Application;
import android.content.Context;

public class TheVaultApplication extends Application {
    private static TheVaultApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getAppContext() {
        return instance.getApplicationContext();
    }
}
