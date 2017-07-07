package com.chengshi.apkUpdater.callback;

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
     */
    void onLoadSuccess(Uri downUri);

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
