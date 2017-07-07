package com.chengshi.apkUpdater;

import android.support.annotation.NonNull;

/**
 * 描述 需要更新的Apk信息对象。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 */

public interface UpdateInfo {

    /**
     * 获取网络上的版本号。
     * @return 返回当前对象的版本号字段的值。
     */
    int getVersionCode();

    /**
     * 获取最新版本的下载链接。
     * @return 返回当前对象的下载链接字段的值。
     */
    String getDownLoadsUrl();

    /**
     * 是否强制更新。
     * @return <code color="blue">true</code> 表示强制更新, <code color="blue">false</code> 则相反。
     */
    boolean isForceUpdate();

    /**
     * 获取Apk文件名(例如 xxx.apk 或 xxx)。后缀名不是必须的。但一定不能返回null。
     */
    @NonNull String getApkName();

    /**
     * 获取更新的内容。
     */
    CharSequence getUpdateMessage();
}
