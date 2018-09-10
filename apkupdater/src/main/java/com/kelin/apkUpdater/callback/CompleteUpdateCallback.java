package com.kelin.apkUpdater.callback;

import com.kelin.apkUpdater.UpdateInfo;

import java.io.File;

/**
 * 描述 下载文件的回调接口。
 * 创建人 kelin
 * 创建时间 2016/10/11  上午11:10
 * 包名 com.chengshi.downloader.callbacks
 */

public abstract class CompleteUpdateCallback implements IUpdateCallback {
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
     * 等待下载。
     */
    public void onDownloadPending() {
    }

    /**
     * 下载暂停。
     */
    public void onDownloadPaused() {
    }

    /**
     * 当下载失败的时候调用。
     *
     * @param checkMD5failedCount MD5校验失败失败次数。
     */
    public void onDownloadFailed(int checkMD5failedCount) {
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
     * <p>
     * 你可能会疑惑明明已经有{@link #onDownloadFailed(int)}方法了为什么失败后还会执行该方法。这里我要做下说明：
     * 这样做的想法有点复杂，暂时不知道用短短几句话描述，所以就先不做解释了。
     *
     * @param haveNewVersion      是否有新的版本。
     *                            <code color="blue">true</code>表示有新的版本,
     *                            <code color="blue">false</code>则表示没有新的版本。
     * @param curVersionName      当前app的版本名称。
     * @param successful          本次检测更新是否是成功的。这里的说所的成功的意思就是即检测到了新的版本且下载的安装包是有效且可以安装的。
     * @param checkMD5failedCount MD5校验失败失败次数，如果 successful 的值为true的话改字段的值绝对为0且毫无意义。只有当 successful 字段的
     *                            值为false的时候该字段的值才会有可能大于0，用于表示当前是第几次MD5校验失败。一般连续失败的次数过多您就需要提醒用户
     *                            变更一下网络环境再次重试了。因为有些网络环境确实会存在一直丢包的这种问题，就例如我以前遇到过的长城宽带
     *                            一直丢包，导致我下载的文件一直是损坏的。
     * @param isForceUpdate       是否是强制更新。这个字段的值其实就{@link UpdateInfo#isForceUpdate()}的返回值。
     */
    @Override
    public void onCompleted(boolean haveNewVersion, String curVersionName, boolean successful, int checkMD5failedCount, boolean isForceUpdate) {
    }
}
