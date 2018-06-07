package com.kelin.apkUpdater;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.text.TextUtils;
import android.widget.Toast;

import com.kelin.apkUpdater.callback.DialogEventCallback;
import com.kelin.apkUpdater.callback.DownloadProgressCallback;
import com.kelin.apkUpdater.callback.UpdateCallback;
import com.kelin.apkUpdater.dialog.DefaultDialog;
import com.kelin.apkUpdater.dialog.DownloadDialogParams;
import com.kelin.apkUpdater.dialog.InformDialogParams;
import com.kelin.apkUpdater.util.NetWorkStateUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 描述 用来更新APK的核心类。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 * 版本 v 1.0.0
 */
public final class Updater {
    private final Builder mBuilder;
    private final UpdateCallback mCallback;
    private boolean isBindService;
    private ServiceConnection conn;
    private Intent mServiceIntent;
    private DefaultDialog mDefaultDialog;
    private boolean mIsLoaded;
    private UpdateInfo mUpdateInfo;
    private boolean mHaveNewVersion;
    private boolean mIsChecked;
    private static int sLocalVersionCode = 0xffff_ffff;
    private boolean mAutoInstall = true;
    private NetWorkStateChangedReceiver mNetWorkStateChangedReceiver;
    private OnLoadProgressListener mOnProgressListener;
    private boolean mIsProgressDialogHidden;
    private DefaultDialogListener mDialogListener = new DefaultDialogListener();
    private final Context mApplicationContext;

    /**
     * 私有构造函数，防止其他类创建本类对象。
     */
    private Updater(Builder builder) {
        mBuilder = builder;
        mCallback = mBuilder.callback;
        mApplicationContext = mBuilder.context.getApplicationContext();
        mDefaultDialog = new DefaultDialog(mBuilder.context);
    }

    @NonNull
    private ServiceConnection getServiceConnection() {
        if (conn == null) {
            conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    DownloadService.DownloadBinder binder = (DownloadService.DownloadBinder) service;
                    DownloadService downloadService = binder.getService();

                    //接口回调，下载进度
                    if (mOnProgressListener == null) {
                        mOnProgressListener = new OnLoadProgressListener();
                    }
                    downloadService.setOnProgressListener(mOnProgressListener);

                    downloadService.setServiceUnBindListener(new DownloadService.ServiceUnBindListener() {
                        @Override
                        public void onUnBind() {
                            isBindService = false;
                        }
                    });
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };
        }
        return conn;
    }


    private void registerNetWorkReceiver() {
        if (mNetWorkStateChangedReceiver == null) {
            mNetWorkStateChangedReceiver = new NetWorkStateChangedReceiver();
        }
        if (!mNetWorkStateChangedReceiver.isRegister()) {
            NetWorkStateUtil.registerReceiver(mApplicationContext, mNetWorkStateChangedReceiver);
        }
    }

    private void unregisterNetWorkReceiver() {
        if (mNetWorkStateChangedReceiver != null) {
            NetWorkStateUtil.unregisterReceiver(mApplicationContext, mNetWorkStateChangedReceiver);
        }
    }

    /**
     * 判断当前版本是否是强制更新。
     *
     * @return 如果是返回true，否则返回false。
     */
    public static boolean isForceUpdate(@NonNull UpdateInfo updateInfo, @NonNull Context context) {
        if (!updateInfo.isForceUpdate()) {
            return false;
        } else {
            int[] codes = updateInfo.getForceUpdateVersionCodes();
            if (codes == null || codes.length == 0) {
                return true;
            } else {
                for (int code : codes) {
                    if (getLocalVersionCode(context) == code) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * 判断当前版本是否是强制更新。
     *
     * @return 如果是返回true，否则返回false。
     */
    private boolean isForceUpdate(@NonNull UpdateInfo updateInfo) {
        return isForceUpdate(updateInfo, mApplicationContext);
    }

    private static int getLocalVersionCode(Context context) {
        if (sLocalVersionCode == 0xffff_ffff) {
            sLocalVersionCode = UpdateHelper.getCurrentVersionCode(context);
        }
        return sLocalVersionCode;
    }

    /**
     * 显示进度条对话框。
     */
    private void showProgressDialog() {
        if (!mBuilder.noDialog) {
            mDefaultDialog.show(mBuilder.loadDialogConfig, mDialogListener.changeState(DefaultDialogListener.STATE_DOWNLOAD));
        } else {
            if (mBuilder.dialogCallback != null) {
                mDefaultDialog.dismissAll();
                mBuilder.dialogCallback.onShowProgressDialog(isForceUpdate(mUpdateInfo));
            } else {
                throw new IllegalArgumentException("you mast call Updater's \"setCallback(UpdateCallback callback)\" Method。");
            }
        }
    }

    /**
     * 更新进度条对话框进度。
     *
     * @param percentage 当前的百分比。
     */
    private void updateProgressDialog(int percentage) {
        if (mDefaultDialog != null) {
            mDefaultDialog.updateDownLoadsProgress(percentage);
        }
    }

    private void stopService() {
        //判断是否真的下载完成进行安装了，以及是否注册绑定过服务
        if (isBindService) {
            mApplicationContext.unbindService(conn);
            isBindService = false;
        }
        mApplicationContext.stopService(mServiceIntent);
    }

    /**
     * 检查更新并自动安装。
     *
     * @param updateInfo 更新信息对象。
     */
    public void check(UpdateInfo updateInfo) {
        check(updateInfo, true);
    }

    /**
     * 检查更新。如果当前
     *
     * @param updateInfo  更新信息对象。
     * @param autoInstall 是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    public void check(UpdateInfo updateInfo, boolean autoInstall) {
        if (!autoInstall && mCallback == null) {
            throw new IllegalArgumentException("Because you neither set up to monitor installed automatically, so the check update is pointless.");
        }
        if (!NetWorkStateUtil.isConnected(mApplicationContext)) {
            mCallback.onCompleted(false, UpdateHelper.getCurrentVersionName(mApplicationContext));
            return;
        }
        if (updateInfo != null && mUpdateInfo != updateInfo) {
            if (TextUtils.isEmpty(updateInfo.getDownLoadsUrl())) {
                mCallback.onCompleted(updateInfo.getVersionCode() > getLocalVersionCode(mApplicationContext), UpdateHelper.getCurrentVersionName(mApplicationContext));
                return;
            }
            mAutoInstall = autoInstall;
            mIsChecked = true;
            mUpdateInfo = updateInfo;
            mBuilder.loadDialogConfig.setForceUpdate(isForceUpdate(updateInfo));
            mBuilder.informDialogConfig.setMsg(updateInfo.getUpdateMessage());
            //如果这个条件满足说明上一次没有安装。有因为即使上一次没有安装最新的版本也有可能超出了上一次下载的版本，所以要在这里判断。
            if (UpdateHelper.getApkVersionCodeFromSp(mApplicationContext) == updateInfo.getVersionCode() && new File(UpdateHelper.getApkPathFromSp(mApplicationContext)).exists()) {
                mIsLoaded = true;
            } else {
                UpdateHelper.removeOldApk(mApplicationContext);
            }
            if (updateInfo.getVersionCode() > getLocalVersionCode(mApplicationContext)) {
                mHaveNewVersion = true;
                if (!mBuilder.noDialog) {
                    mBuilder.informDialogConfig.setForceUpdate(isForceUpdate(updateInfo));
                    showUpdateInformDialog();
                } else {
                    if (mBuilder.dialogCallback != null) {
                        mBuilder.dialogCallback.onShowCheckHintDialog(Updater.this, updateInfo, isForceUpdate(updateInfo));
                    } else {
                        throw new IllegalArgumentException("you mast call Updater's \"setCallback(UpdateCallback callback)\" Method。");
                    }
                }
            } else {
                if (mCallback != null) {
                    mCallback.onCompleted(false, UpdateHelper.getCurrentVersionName(mApplicationContext));
                }
            }
        }
    }

    /**
     * 显示更新提醒。
     */
    private void showUpdateInformDialog() {
        mDefaultDialog.show(mBuilder.informDialogConfig, mDialogListener.changeState(DefaultDialogListener.STATE_CHECK_UPDATE));
    }

    /**
     * 设置检查更新的对话框的操作结果。如果你没有关闭默认的对话框使用自定义对话框的话请不要手动调用该方法。
     *
     * @param isContinue 是否继续，如果继续则说明统一更新，否则就是不统一更新。
     */
    public void setCheckHandlerResult(boolean isContinue) {
        if (!mBuilder.noDialog || !mIsChecked) {  //如果不是自定义UI交互或没有使用API提供的check方法检测更新的话不允许调用该方法。
            throw new IllegalStateException("Because of your dialog is not custom, so you can't call the method.");
        }
        respondCheckHandlerResult(isContinue);
    }

    /**
     * 响应检查更新的对话框的操作结果。如果你没有关闭默认的对话框使用自定义对话框的话请不要手动调用该方法。
     *
     * @param isContinue 是否继续，如果继续则说明同意更新，否则就是不同意更新。
     */
    private void respondCheckHandlerResult(boolean isContinue) {
        if (isContinue && mHaveNewVersion) {
            File apkFile;
            if (mIsLoaded && (apkFile = new File(UpdateHelper.getApkPathFromSp(mApplicationContext))).exists()) {
                if (mCallback != null) {
                    mCallback.onLoadSuccess(apkFile, true);
                    mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
                }
                if (mAutoInstall) {
                    boolean installApk = UpdateHelper.installApk(mApplicationContext, apkFile);
                    if (!installApk && mCallback != null) {
                        mCallback.onInstallFailed();
                    }
                }
            } else {
                if (checkCanDownloadable()) {
                    startDownload();
                }
            }
        } else {
            if (mCallback != null) {
                mCallback.onLoadCancelled();
                mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
            }
        }
    }

    private boolean checkCanDownloadable() {
        registerNetWorkReceiver();  //注册一个网络状态改变的广播接收者。无论网络是否连接成功都要注册，因为下载过程中可能会断网。
        if (!NetWorkStateUtil.isConnected(mApplicationContext) || (mBuilder.checkWiFiState && !NetWorkStateUtil.isWifiConnected(mApplicationContext))) {
            showWifiOrMobileUnusableDialog();
            return false;
        }
        return true;
    }

    private void showWifiOrMobileUnusableDialog() {
        if (NetWorkStateUtil.isConnected(mApplicationContext)) {
            showWiFiUnusableDialog();
        } else {
            showNetWorkUnusableDialog();
        }
    }

    private void showNetWorkUnusableDialog() {
        mDefaultDialog.showNetWorkUnusableDialog(mDialogListener.changeState(DefaultDialogListener.STATE_NETWORK_UNUSABLE));
    }

    private void showWiFiUnusableDialog() {
        mDefaultDialog.showWiFiUnusableDialog(mDialogListener.changeState(DefaultDialogListener.STATE_WIFI_UNUSABLE));
    }

    private String getApkName(@NonNull UpdateInfo updateInfo) {
        return TextUtils.isEmpty(updateInfo.getApkName()) ? getDefaultApkName() : updateInfo.getApkName();
    }

    /**
     * 开始下载。
     *
     * @param updateInfo 更新信息对象。
     */
    public void download(@NonNull UpdateInfo updateInfo) {
        download(updateInfo, true);
    }

    /**
     * 开始下载。
     *
     * @param updateInfo  更新信息对象。
     * @param autoInstall 是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    public void download(@NonNull UpdateInfo updateInfo, boolean autoInstall) {
        download(updateInfo, null, null, autoInstall);
    }

    /**
     * 开始下载。
     *
     * @param updateInfo        更新信息对象。
     * @param notifyCationTitle 下载过程中通知栏的标题。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     * @param notifyCationDesc  下载过程中通知栏的描述。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     * @param autoInstall       是否自动安装，true表示在下载完成后自动安装，false表示不需要安装。
     */
    public void download(@NonNull UpdateInfo updateInfo, CharSequence notifyCationTitle, CharSequence notifyCationDesc, boolean autoInstall) {
        if (mIsChecked) {  //如果检查更新不是自己检查的就不能调用这个方法。
            throw new IllegalStateException("Because you update the action is completed, so you can't call this method.");
        }
        if (!autoInstall && mCallback == null) {
            throw new IllegalArgumentException("Because you have neither set up to monitor installed automatically, so the download is pointless.");
        }
        if (TextUtils.isEmpty(updateInfo.getDownLoadsUrl())) {
            return;
        }
        mBuilder.mTitle = TextUtils.isEmpty(notifyCationTitle) ? "正在下载更新" : notifyCationTitle;
        mBuilder.mDescription = notifyCationDesc;
        mAutoInstall = autoInstall;
        mUpdateInfo = updateInfo;
        mBuilder.loadDialogConfig.setForceUpdate(isForceUpdate(updateInfo));
        if (checkCanDownloadable()) {
            startDownload();
        }
    }

    /**
     * 开始下载。
     */
    private void startDownload() {
        mServiceIntent = new Intent(mApplicationContext, DownloadService.class);
        mServiceIntent.putExtra(DownloadService.KEY_APK_NAME, getApkName(mUpdateInfo));
        mServiceIntent.putExtra(DownloadService.KEY_DOWNLOAD_URL, mUpdateInfo.getDownLoadsUrl());
        mServiceIntent.putExtra(DownloadService.KEY_IS_FORCE_UPDATE, isForceUpdate(mUpdateInfo));
        mServiceIntent.putExtra(DownloadService.KEY_NOTIFY_TITLE, mBuilder.mTitle);
        mServiceIntent.putExtra(DownloadService.KEY_NOTIFY_DESCRIPTION, mBuilder.mDescription);

        mApplicationContext.startService(mServiceIntent);
        isBindService = mApplicationContext.bindService(mServiceIntent, getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    /**
     * 获取默认的Apk名称。
     *
     * @return 返回一个以 "包名+日期" 命名的Apk名称。
     */
    private String getDefaultApkName() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-d", Locale.CHINA);
        String formatDate = format.format(new Date());
        return mApplicationContext.getPackageName() + formatDate + ".apk";
    }

    public static class Builder {

        private final Context context;
        private final InformDialogParams informDialogConfig = new InformDialogParams();
        private final DownloadDialogParams loadDialogConfig = new DownloadDialogParams();
        /**
         * 用来配置下载的监听回调对象。
         */
        UpdateCallback callback;
        /**
         * 通知栏的标题。
         */
        CharSequence mTitle;
        /**
         * 通知栏的描述。
         */
        CharSequence mDescription;
        /**
         * 是否没有对话框。
         */
        private boolean noDialog;
        /**
         * 是否检测WiFi链接状态。
         */
        private boolean checkWiFiState = true;
        private DialogEventCallback dialogCallback;

        public Builder(@NonNull Activity context) {
            this.context = context;
        }

        /**
         * 设置监听对象。
         *
         * @param callback 监听回调对象。
         */
        public Builder setCallback(UpdateCallback callback) {
            this.callback = callback;
            return this;
        }

        /**
         * 设置Dialog的样式。
         *
         * @param style 要设置的样式的资源ID。
         */
        public Builder setDialogTheme(@StyleRes int style) {
            InformDialogParams.setStyle(style);
            return this;
        }

        /**
         * 配置检查更新时对话框的标题。
         *
         * @param title 对话框的标题。
         */
        public Builder setCheckDialogTitle(CharSequence title) {
            informDialogConfig.setTitle(title);
            return this;
        }

        /**
         * 配置下载更新时对话框的标题。
         *
         * @param title 对话框的标题。
         */
        public Builder setDownloadDialogTitle(CharSequence title) {
            loadDialogConfig.setTitle(title);
            if (this.mTitle == null) {
                this.mTitle = title;
            }
            return this;
        }

        /**
         * 配置下载更新时对话框的消息。
         *
         * @param message 对话框的消息。
         */
        public Builder setDownloadDialogMessage(String message) {
            loadDialogConfig.setMsg(message);
            return this;
        }

        /**
         * 设置通知栏的标题。
         */
        public Builder setNotifyTitle(CharSequence title) {
            this.mTitle = title;
            return this;
        }

        /**
         * 设置通知栏的描述。
         */
        public Builder setNotifyDescription(CharSequence description) {
            this.mDescription = description;
            return this;
        }

        /**
         * 如果你希望自己创建对话框，而不使用默认提供的对话框，可以调用该方法将默认的对话框关闭。
         * 如果你关闭了默认的对话框的话就必须自己实现UI交互，并且在用户更新提示做出反应的时候调用
         * {@link #setCheckHandlerResult(boolean)} 方法。
         */
        public Builder setNoDialog(@NonNull DialogEventCallback callback) {
            this.noDialog = true;
            this.dialogCallback = callback;
            return this;
        }

        /**
         * 设置不检查WiFi状态，默认是检查WiFi状态的，也就是说如果在下载更新的时候如果没有链接WiFi的话默认是会提示用户的。
         * 但是如果你不希望给予提示，就可以通过调用此方法，禁用WiFi检查。
         *
         * @param check 是否检测WiFi连接状态，true表示检测，false表示不检测。默认检测。
         */
        public Builder setCheckWiFiState(boolean check) {
            this.checkWiFiState = check;
            return this;
        }

        /**
         * 构建 {@link Updater} 对象。
         *
         * @return 返回一个构建好的 {@link Updater} 对象。
         */
        public Updater builder() {
            return new Updater(this);
        }
    }

    private class NetWorkStateChangedReceiver extends NetWorkStateUtil.ConnectivityChangeReceiver {
        /**
         * 当链接断开的时候执行。
         *
         * @param type 表示当前断开链接的类型，是WiFi还是流量。如果为 {@link ConnectivityManager#TYPE_WIFI} 则说明当前断开链接
         *             的是WiFi，如果为 {@link ConnectivityManager#TYPE_MOBILE} 则说明当前断开链接的是流量。
         */
        @Override
        protected void onDisconnected(int type) {
            showNetWorkUnusableDialog();
        }

        /**
         * 当链接成功后执行。
         *
         * @param type 表示当前链接的类型，是WiFi还是流量。如果为 {@link ConnectivityManager#TYPE_WIFI} 则说明当前链接
         *             成功的是WiFi，如果为 {@link ConnectivityManager#TYPE_MOBILE} 则说明当前链接成功的是流量。
         */
        @Override
        protected void onConnected(int type) {
            switch (type) {
                case ConnectivityManager.TYPE_MOBILE:
                    if (isBindService) {
                        if (!mIsProgressDialogHidden) {
                            showProgressDialog();
                        }
                    } else {
                        if (mBuilder.checkWiFiState) {
                            showWiFiUnusableDialog();
                        } else {
                            startDownload();
                        }
                    }
                    break;
                case ConnectivityManager.TYPE_WIFI:
                    if (isBindService) {
                        showProgressDialog();
                    } else {
                        startDownload();
                    }
                    break;
            }
        }
    }

    private class OnLoadProgressListener implements DownloadProgressCallback {

        @Override
        public void onStartDownLoad() {
            if (mCallback != null) {
                mCallback.onStartDownLoad();
            }
            showProgressDialog();
        }

        @Override
        public void onProgress(long total, long current, int percentage) {
            if (percentage == 100 || total == current) {
                UpdateHelper.putApkVersionCode2Sp(mApplicationContext, mUpdateInfo.getVersionCode());
            }
            if (mCallback != null) {
                mCallback.onProgress(total, current, percentage);
            }
            if (mBuilder.dialogCallback != null) {
                mBuilder.dialogCallback.onProgress(total, current, percentage);
            }
            if (!mBuilder.noDialog) {
                updateProgressDialog(percentage);
            }
        }

        @Override
        public void onLoadSuccess(File apkFile, boolean isCache) {
            unregisterNetWorkReceiver();
            stopService();  //结束服务
            if (mCallback != null) {
                mCallback.onLoadSuccess(apkFile, isCache);
                mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
            }
            if (mAutoInstall) {
                UpdateHelper.installApk(mApplicationContext, apkFile);
            }
        }

        @Override
        public void onLoadFailed() {
            unregisterNetWorkReceiver();
            stopService();  //结束服务
            mDefaultDialog.dismissAll();
            if (mCallback != null) {
                mCallback.onLoadFailed();
                mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
            } else {
                Toast.makeText(mApplicationContext, "sorry, 跟新失败了~", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onLoadPaused() {
            if (mCallback != null) {
                mCallback.onLoadPaused();
            }
        }

        @Override
        public void onLoadPending() {
            if (mCallback != null) {
                mCallback.onLoadPending();
            }
        }
    }

    private class DefaultDialogListener implements DefaultDialog.DialogListener {
        /**
         * 表示当前的状态是检查更新。
         */
        private static final int STATE_CHECK_UPDATE = 0x0000_0010;
        /**
         * 表示当前的状态是无网络。
         */
        private static final int STATE_NETWORK_UNUSABLE = 0x0000_0011;
        /**
         * 表示当前的状态是无WiFi。
         */
        private static final int STATE_WIFI_UNUSABLE = 0x0000_0012;
        /**
         * 表示当前的状态是下载中。
         */
        private static final int STATE_DOWNLOAD = 0x0000_0013;
        private int mCurrentState;

        /**
         * 改变状态。
         *
         * @param currentState 要改变新状态。
         * @return 返回 DefaultDialogListener 本身。
         */
        DefaultDialogListener changeState(int currentState) {
            mCurrentState = currentState;
            return this;
        }

        @Override
        public void onDialogDismiss(boolean isSure) {
            switch (mCurrentState) {
                case STATE_CHECK_UPDATE:
                    respondCheckHandlerResult(isSure);
                    break;
                case STATE_NETWORK_UNUSABLE:
                    if (mCallback != null) {
                        if (isBindService) {
                            mCallback.onLoadCancelled();
                            mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
                        } else {
                            mCallback.onCheckCancelled();
                            mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
                        }
                    }
                    break;
                case STATE_WIFI_UNUSABLE:
                    if (isSure) {
                        startDownload();
                    } else {
                        if (mCallback != null) {
                            mCallback.onLoadCancelled();
                            mCallback.onCompleted(true, UpdateHelper.getCurrentVersionName(mApplicationContext));
                        }
                    }
                    break;
                case STATE_DOWNLOAD:
                    mIsProgressDialogHidden = true;
                    break;
            }
        }
    }

}
