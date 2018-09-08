package com.kelin.apkUpdater.callback;

import com.kelin.apkUpdater.UpdateInfo;

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
     * 下载进度更新的时候调用。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    @Override
    public void onProgress(long total, long current, int percentage) {
    }

    /**
     * 下载完成。
     *
     * @param apkFile 已经下载好的APK文件对象。
     * @param isCache 是否是缓存，如果改参数为true说明本次并没有真正的执行下载任务，因为上一次用户下载完毕后并没有进行安装。
     */
    @Override
    public void onLoadSuccess(File apkFile, boolean isCache) {
    }

    /**
     * 当下载被取消后调用。即表明用户不想进行本次更新，强制更新一般情况下是不能取消的，
     * 除非你调用了{@link com.kelin.apkUpdater.Updater.Builder#setCheckWiFiState(boolean)}方法并将参数设置为true，
     * 这就意味着你希望在用户的网络为4G网络的时候提醒用户，而这个提醒之后用户是可以取消下载的，当用户取消下载之后就会执行该方法。
     */
    public void onLoadCancelled() {
    }

    /**
     * 等待下载。
     */
    @Override
    public void onLoadPending() {
    }

    /**
     * 下载暂停。
     */
    @Override
    public void onLoadPaused() {
    }

    /**
     * 当下载失败的时候调用。
     */
    @Override
    public void onLoadFailed() {
    }

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
     * @param successful     本次检测更新是否是成功的。这里的说所的成功的意思就是即检测到了新的版本且下载的安装包是有效且可以安装的。
     * @param isForceUpdate  是否是强制更新。这个字段的值其实就{@link UpdateInfo#isForceUpdate()}的返回值。
     */
    public void onCompleted(boolean haveNewVersion, String curVersionName, boolean successful, boolean isForceUpdate) {
    }
}
