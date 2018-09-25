package com.kelin.apkUpdater.callback;

import com.kelin.apkUpdater.UpdateInfo;
import com.kelin.apkUpdater.Updater;

import java.io.File;

/**
 * 描述 下载文件的回调接口。
 * 创建人 kelin
 * 创建时间 2016/10/11  上午11:10
 * 包名 com.chengshi.downloader.callbacks
 */

public abstract class CompleteUpdateCallback implements IUpdateCallback {

    /**
     * 当用户需要静默下载的时候调用。通常情况下该方法不会执行，如果你使用了我所提供的Dialog(没有自定义Dialog的话)，在非强制更新的时候
     * 下载进度Dialog上会有一个名为悄悄下载的按钮，点击这个按钮后就会执行该方法。
     * 如果你使用了自定义Dialog的话你可以在用户希望静默下载的时候调用 {@link Updater#silentDownload()} 方法，这样的话该回调也会被执行。
     */
    @Override
    public void onSilentDownload() {}

    /**
     * 开始下载，在开始执行下载的时候调用。
     */
    public void onStartDownLoad() {
    }

    /**
     * 下载进度更新的时候调用。
     *
     * @param total      文件总大小(字节)。
     * @param current    当前的进度(字节)。
     * @param percentage 当前下载进度的百分比。
     */
    public void onProgress(long total, long current, int percentage) {
    }

    /**
     * 下载完成。
     *
     * @param apkFile 已经下载好的APK文件对象。
     * @param isCache 是否是缓存，如果改参数为true说明本次并没有真正的执行下载任务，因为上一次用户下载完毕后并没有进行安装。
     */
    public void onDownloadSuccess(File apkFile, boolean isCache) {
    }

    /**
     * 当下载被取消后调用。即表明用户不想进行本次更新，强制更新一般情况下是不能取消的，
     * 除非你调用了{@link com.kelin.apkUpdater.Updater.Builder#setCheckWiFiState(boolean)}方法并将参数设置为true，
     * 这就意味着你希望在用户的网络为4G网络的时候提醒用户，而这个提醒之后用户是可以取消下载的，当用户取消下载之后就会执行该方法。
     */
    public void onDownloadCancelled() {
    }

    /**
     * 等待下载，当网络连接断开或无可用网络等一些意外原因导致加载被搁置的时候调用。
     */
    public void onDownloadPending() {
    }

    /**
     * 下载暂停，当用户暂停了下载任务等一些其他原因导致下载被暂停时调用。
     */
    public void onDownloadPaused() {
    }

    /**
     * 当下载失败的时候调用。
     */
    public void onDownloadFailed() {
    }

    /**
     * 如果在安装过程中发生了意外导致安装失败会执行此方法。
     */
    public void onInstallFailed() {
    }

    /**
     * 当本次检测更新成功的时候调用。
     *
     * @param isAutoCheck    该参数说明了本次检测更新是手动检测更新还是自动检测更新。
     * @param haveNewVersion 是否有新的版本。
     *                       <code color="blue">true</code>表示有新的版本,
     *                       <code color="blue">false</code>则表示没有新的版本。
     * @param curVersionName 当前app的版本名称。
     * @param isForceUpdate  是否是强制更新。这个字段的值其实就{@link UpdateInfo#isForceUpdate()}和
     *                       {@link UpdateInfo#getForceUpdateVersionCodes()}}的返回值。
     */
    @Override
    public void onSuccess(boolean isAutoCheck, boolean haveNewVersion, String curVersionName, boolean isForceUpdate) {

    }

    /**
     * 当失败的时候被执行，无论是检测更新失败，还是下载失败，都会执行。
     *
     * @param isAutoCheck         该参数说明了本次检测更新是手动检测更新还是自动检测更新。
     * @param isCanceled          是否是用户取消了下载更新。
     * @param haveNewVersion      是否有新的版本。
     *                            <code color="blue">true</code>表示有新的版本,
     *                            <code color="blue">false</code>则表示没有新的版本。
     * @param curVersionName      当前app的版本名称。
     * @param checkMD5failedCount MD5校验失败失败次数，如果下载失败的原因是因为校验MD5失败的话该参数将会是一个大于0的数字。
     * @param isForceUpdate       是否是强制更新。这个字段的值其实就{@link UpdateInfo#isForceUpdate()}和
     *                            {@link UpdateInfo#getForceUpdateVersionCodes()}}的返回值。
     */
    @Override
    public void onFiled(boolean isAutoCheck, boolean isCanceled, boolean haveNewVersion, String curVersionName, int checkMD5failedCount, boolean isForceUpdate) {

    }

    @Override
    public void onCompleted() {
    }
}
