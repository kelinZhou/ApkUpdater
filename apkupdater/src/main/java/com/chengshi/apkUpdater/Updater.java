package com.chengshi.apkUpdater;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.chengshi.apkUpdater.callback.DialogListener;
import com.chengshi.apkUpdater.callback.OnProgressListener;
import com.chengshi.apkUpdater.callback.ServiceUnBindListener;
import com.chengshi.apkUpdater.callback.UpdateCallback;
import com.chengshi.apkUpdater.dialog.DefaultDialog;
import com.chengshi.apkUpdater.dialog.DownloadDialogConfig;
import com.chengshi.apkUpdater.dialog.InformDialogConfig;
import com.chengshi.apkUpdater.service.DownloadService;
import com.chengshi.apkUpdater.util.Utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 描述 用来更新APK的核心类。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 * 版本 v 1.0.0
 */
public final class Updater {

    /**
     * 安装APK的请求码。
     */
    public static final int REQUEST_CODE_INSTALL_APK = 0X0000_0010;
    private final Builder mBuilder;
    private final UpdateCallback mCallback;
    private boolean isBindService;
    private ServiceConnection conn;
    private boolean isForceUpdate;
    private Intent mServiceIntent;
    private DefaultDialog mInformDialog;
    private int mLatestVersionCode;
    private boolean mIsLoaded;

    /**
     * 私有构造函数，防止其他类创建本类对象。
     */
    private Updater(Builder builder) {
        mBuilder = builder;
        mCallback = mBuilder.callback;
        conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DownloadService.DownloadBinder binder = (DownloadService.DownloadBinder) service;
                DownloadService downloadService = binder.getService();

                //接口回调，下载进度
                downloadService.setOnProgressListener(new OnProgressListener() {

                    @Override
                    public void onStartLoad() {
                        mCallback.onStartLoad();
                        mBuilder.loadDialogConfig.setForceUpdate(isForceUpdate);
                        mInformDialog = new DefaultDialog(mBuilder.context, mBuilder.loadDialogConfig);
                        mInformDialog.show();
                    }

                    @Override
                    public void onProgress(long total, long current, int percentage) {
                        if (percentage == 100 || total == current) {
                            Utils.putApkVersionCode2Sp(mBuilder.context, mLatestVersionCode);
                        }
                        mCallback.onProgress(total, current, percentage);
                        if (mInformDialog != null) {
                            mInformDialog.updateDownLoadsProgress(percentage);
                        }
                    }

                    @Override
                    public void onLoadSuccess(Uri downIdUri) {
                        Utils.installApk(mBuilder.context, downIdUri, Updater.REQUEST_CODE_INSTALL_APK);
                        stopService();  //结束服务
                        mCallback.onLoadSuccess(downIdUri);
                        mCallback.onCompleted(true);
                    }

                    @Override
                    public void onLoadFailed() {
                        stopService();  //结束服务
                        mCallback.onLoadFailed();
                        mCallback.onCompleted(true);
                    }

                    @Override
                    public void onLoadPaused() {
                        mCallback.onLoadPaused();
                    }

                    @Override
                    public void onLoadPending() {
                        mCallback.onLoadPending();
                    }
                });

                downloadService.setServiceUnBindListener(new ServiceUnBindListener() {
                    @Override
                    public void onUnBind() {
                        isBindService = false;
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {}
        };
    }

    private void stopService() {
        //判断是否真的下载完成进行安装了，以及是否注册绑定过服务
        if (isBindService) {
            mBuilder.context.unbindService(conn);
            isBindService = false;
        }
        mBuilder.context.stopService(mServiceIntent);
    }

    public void check(UpdateInfo updateInfo) {
        int latestVersionCode = Utils.getApkVersionCodeFromSp(mBuilder.context);
        //如果这个条件满足说明上一次没有安装。有因为即使上一次没有安装最新的版本也有可能超出了上一次下载的版本，所以要在这里判断。
        if (latestVersionCode == updateInfo.getVersionCode() && new File(Utils.getApkPathFromSp(mBuilder.context)).exists()) {
            mIsLoaded = true;
        } else {
            Utils.removeOldApk(mBuilder.context);
        }
        if (updateInfo.getVersionCode() > Utils.getCurrentVersionCode(mBuilder.context)) {
            isForceUpdate = updateInfo.isForceUpdate();
            mLatestVersionCode = updateInfo.getVersionCode();
            String fileName = updateInfo.getApkName();
            if (!TextUtils.isEmpty(fileName)) {
                mBuilder.fileName = fileName;
            } else {
                mBuilder.fileName = getDefaultApkName();
            }
            showUpdateInform(updateInfo.getDownLoadsUrl(), updateInfo.getUpdateMessage());
        } else {
            mCallback.onCompleted(false);
        }
    }

    /**
     * 显示更新提醒。
     */
    private void showUpdateInform(final String downLoadUrl, CharSequence updateMessage) {
        InformDialogConfig dialogNormalConfig = mBuilder.informDialogConfig;
        dialogNormalConfig.setMsg(updateMessage);
        new DefaultDialog(mBuilder.context, dialogNormalConfig)
                .show(new DialogListener() {
                    @Override
                    public void onDialogDismiss(boolean isSure, boolean isUpdate) {
                        if (isSure) {
                            if (mIsLoaded) {
                                File file = new File(Utils.getApkPathFromSp(mBuilder.context));
                                Uri apkPath = Uri.fromFile(file);
                                Utils.installApk(mBuilder.context, apkPath, Updater.REQUEST_CODE_INSTALL_APK);
                            } else {
                                mServiceIntent = new Intent(mBuilder.context, DownloadService.class);
                                mServiceIntent.putExtra(DownloadService.KEY_APK_NAME, mBuilder.fileName);
                                mServiceIntent.putExtra(DownloadService.KEY_DOWNLOAD_URL, downLoadUrl);
                                mServiceIntent.putExtra(DownloadService.KEY_IS_FORCE_UPDATE, isForceUpdate);
                                mServiceIntent.putExtra(DownloadService.KEY_NOTIFY_TITLE, mBuilder.mTitle);
                                mServiceIntent.putExtra(DownloadService.KEY_NOTIFY_DESCRIPTION, mBuilder.mDescription);

                                mBuilder.context.startService(mServiceIntent);
                                isBindService = mBuilder.context.bindService(mServiceIntent, conn, Context.BIND_AUTO_CREATE);
                            }
                        } else {
                            mCallback.onLoadCancelled(isForceUpdate);
                        }
                    }
                });
    }

    /**
     * 获取默认的Apk名称。
     *
     * @return 返回一个以 "包名+日期" 命名的Apk名称。
     */
    private String getDefaultApkName() {
        String formatDate = SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT).format(new Date());
        return mBuilder.context.getPackageName() + formatDate + ".apk";
    }

    public static class Builder {

        private final Activity context;
        private final InformDialogConfig informDialogConfig = new InformDialogConfig();
        private final DownloadDialogConfig loadDialogConfig = new DownloadDialogConfig();
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
         * APK名称。
         */
        String fileName;

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
            setNotifyTitle(title);
            return this;
        }

        /**
         * 配置下载更新时对话框的消息。
         *
         * @param message 对话框的消息。
         */
        public Builder setCheckDialogMessage(String message) {
            informDialogConfig.setMsg(message);
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
        Builder setNotifyTitle(CharSequence title) {
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
         * 构建 {@link Updater} 对象。
         *
         * @return 返回一个构建好的 {@link Updater} 对象。
         */
        public Updater builder() {
            return new Updater(this);
        }
    }
}
