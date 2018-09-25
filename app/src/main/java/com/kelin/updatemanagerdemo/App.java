package com.kelin.updatemanagerdemo;

import android.app.Application;

import com.kelin.apkUpdater.Updater;
import com.squareup.leakcanary.LeakCanary;

/**
 * 描述 ${TODO}
 * 创建人 kelin
 * 创建时间 2017/7/6  下午5:55
 * 版本 v 1.0.0
 */

public class App extends Application {
    @Override
    public void onCreate() {
        if (!LeakCanary.isInAnalyzerProcess(this)) {
            LeakCanary.install(this);
        }
        Updater.init(this);
        super.onCreate();
    }
}
