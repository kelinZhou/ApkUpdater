package com.chengshi.apkUpdater.callback;

import android.net.Uri;

/**
 * 描述 下载文件的回调接口。
 * 创建人 kelin
 * 创建时间 2016/10/11  上午11:10
 * 包名 com.chengshi.downloader.callbacks
 */

public abstract class UpdateCallback implements OnProgressListener {

    /**
     * 开始下载。
     */
    @Override
    public void onStartLoad() {}

    /**
     * 下载进度更新。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    @Override
    public void onProgress(long total, long current, int percentage) {}

    /**
     * 下载完成。
     *
     * @param downUri 已经下载好的APK存储地址。
     */
    @Override
    public void onLoadSuccess(Uri downUri){};

    /**
     * 当下载失败的时候调用。
     */
    @Override
    public void onLoadFailed(){};

    /**
     * 下载暂停。
     */
    @Override
    public void onLoadPaused() {}

    /**
     * 等待下载。
     */
    @Override
    public void onLoadPending() {}

    /**
     * 当下载被取消后调用。这个取消有可能是人为取消,也有可能是意外取消。
     *
     * @param isForceUpdate 当前是否为强制更新。
     */
    public abstract void onLoadCancelled(boolean isForceUpdate);

    /**
     * 当任务完毕后被调用。无论任务成功还是失败，也无论是否需要更新。如果在检查更新阶段发现没有新的版本则会直接执行
     * 该方法，如果检查更新失败也会执行该方法，如果检测到了新的版本的话，那么这个方法就不会再检查更新阶段调用，一直
     * 等到下载完成或下载失败之后才会被执行。
     *
     * @param haveNewVersion 是否有新的版本。
     *                       <code color="blue">true</code>表示有新的版本,
     *                       <code color="blue">false</code>则表示没有新的版本。
     */
    public void onCompleted(boolean haveNewVersion) {}
}
