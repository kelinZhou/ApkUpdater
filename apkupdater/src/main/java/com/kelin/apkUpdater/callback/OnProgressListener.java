package com.kelin.apkUpdater.callback;

import android.net.Uri;

/**
 * 描述 下载过程的监听对象。
 * 创建人 kelin
 * 创建时间 2017/3/15  下午1:13
 * 版本 v 1.0.0
 */

public interface OnProgressListener {

    /**
     * 当下载开始的时候调用。
     */
    void onStartLoad();

    /**
     * 当下在进度改变的时候调用。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    void onProgress(long total, long current, int percentage);

    /**
     * 下载完成。
     * @param downUri 已经下载好的APK存储地址。
     * @param isCache 是否是缓存，如果改参数为true说明本次并没有真正的执行下载任务，因为上一次用户下载完毕后并没有进行
     *                安装操作。而本次检测更新网络上的最新版本和上一次下载的版本号是相同的。
     */
    void onLoadSuccess(Uri downUri, boolean isCache);

    /**
     * 当下载失败的时候调用。
     */
    void onLoadFailed();

    /**
     * 当下载被暂停的时候调用。
     */
    void onLoadPaused();

    /**
     * 当正在等待下载的时候调用。
     */
    void onLoadPending();
}
