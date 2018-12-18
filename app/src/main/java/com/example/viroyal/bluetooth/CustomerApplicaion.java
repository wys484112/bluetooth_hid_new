package com.example.viroyal.bluetooth;

import android.app.Application;

import com.activeandroid.ActiveAndroid;

public class CustomerApplicaion extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        //初始化ActivityAndroid
        ActiveAndroid.initialize(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        //清理
        ActiveAndroid.dispose();
    }
}
