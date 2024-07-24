package com.kelin.updatemanagerdemo

import android.app.Application
import com.kelin.apkUpdater.ApkUpdater

/**
 * 描述
 * 创建人 kelin
 * 创建时间 2017/7/6  下午5:55
 * 版本 v 1.0.0
 */
class App : Application() {
    override fun onCreate() {
        ApkUpdater.init(this, 0, "$packageName.fileProvider")
        super.onCreate()
    }
}