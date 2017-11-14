package com.kelin.apkUpdater.callback;

import java.io.File;

/**
 * 描述 下载文件的回调接口。
 * 创建人 kelin
 * 创建时间 2016/10/11  上午11:10
 * 包名 com.chengshi.downloader.callbacks
 */

public abstract class UpdateCallback implements DownloadProgressCallback {
    /**
     * 开始下载，在开始执行下载的时候调用。
     */
    @Override
    public void onStartDownLoad() {
    }

    /**
     * 下载完成。
     *
     * @param apkFile 已经下载好的APK文件对象。
     * @param isCache 是否是缓存，如果改参数为true说明本次并没有真正的执行下载任务，因为上一次用户下载完毕后并没有进行
     */
    @Override
    public void onLoadSuccess(File apkFile, boolean isCache) {
    }

    /**
     * 当下载失败的时候调用。
     */
    @Override
    public void onLoadFailed() {
    }

    /**
     * 下载暂停。
     */
    @Override
    public void onLoadPaused() {
    }

    /**
     * 等待下载。
     */
    @Override
    public void onLoadPending() {
    }

    /**
     * 检查更新被取消。如果当前设备无网络可用则会执行该方法。
     */
    public void onCheckCancelled() {
    }

    /**
     * 当下载被取消后调用。即表明用户不想进行本次更新，强制更新一般情况下是不能取消的，除非你设置了需要检查WIFI而WIFI又没有链接。
     */
    public abstract void onLoadCancelled();

    /**
     * 如果在安装过程中发生了意外导致安装失败会执行此方法。
     */
    public void onInstallFailed() {
    }

    /**
     * 当任务完毕后被调用。无论任务成功还是失败，也无论是否需要更新。如果在检查更新阶段发现没有新的版本则会直接执行
     * 该方法，如果检查更新失败也会执行该方法，如果检测到了新的版本的话，那么这个方法就不会再检查更新阶段调用，一直
     * 等到下载完成或下载失败之后才会被执行。
     *
     * @param haveNewVersion 是否有新的版本。
     *                       <code color="blue">true</code>表示有新的版本,
     *                       <code color="blue">false</code>则表示没有新的版本。
     * @param curVersionName 当前app的版本名称。
     */
    public void onCompleted(boolean haveNewVersion, String curVersionName) {
    }

    /**
     * 下载进度更新的时候调用。如果您是自定义的UI交互的话您需要覆盖此方法，并在此方法中做更新进度的操作。
     * 您还需要在进度完成后进行销毁dialog的操作。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    @Override
    public void onProgress(long total, long current, int percentage) {
    }
}
