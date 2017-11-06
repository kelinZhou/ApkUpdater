package com.kelin.apkUpdater.callback;

import android.support.annotation.NonNull;

import com.kelin.apkUpdater.UpdateInfo;
import com.kelin.apkUpdater.Updater;

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
     * 当需要显示检查更新提示对话框的时候调用。你需要在这里进行检查更新提示对话框的显示。
     * 这个方法并不一定会调用，如果你在构建 {@link Updater.Builder} 的时候调用了
     * {@link Updater.Builder#setNoDialog()} 方法关闭了默认对话框的话，那么这个方法一定会执行，否则就不会执行。
     * 在用户做出相应的操作后，你应当调用{@link Updater#setCheckHandlerResult(boolean)}方法进行下一步的操作。
     *
     * @param updater {@link Updater}对象。
     * @param isForce 是否是强制更新。
     * @see Updater#setCheckHandlerResult(boolean)
     */
    public void onShowCheckHintDialog(Updater updater, @NonNull UpdateInfo updateInfo, boolean isForce) {
    }

    /**
     * 当需要加载下载进度对话框的时候调用，你需要在这做显示下载进度对话框的操作。
     * <p>这个方法并不一定会调用，如果你在构建 {@link Updater.Builder} 的时候调用了
     * {@link Updater.Builder#setNoDialog()} 方法关闭了默认对话框的话，那么这个方法一定会执行，否则就不会执行。
     * <p>也有可能这个方法会调用不止一次，如果你在构建 {@link Updater.Builder} 的时候没有调用
     * {@link Updater.Builder#setCheckWiFiState(boolean)}方法改变检测网络状态的话默认是会检测WIFI状态的。当WIFI状态改变的时候
     * 有可能会再次调用该方法，所以这里你要做好相应的判断，以避免Dialog会显示多次。
     * <p>这里只是单纯的做显示对话的操作，无需做其他任何处理，进度条的更新需要在{@link #onProgress(long, long, int)}方法中处理，
     * 您需要覆盖{@link #onProgress(long, long, int)}方法。
     *
     * @param isForce 是否是强制更新。
     *
     * @see Updater.Builder#setCheckWiFiState(boolean)
     * @see #onProgress(long, long, int)
     */
    public void onShowProgressDialog(boolean isForce) {
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
