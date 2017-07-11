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
    private final Builder mBuilder;
    private final UpdateCallback mCallback;
    private boolean isBindService;
    private ServiceConnection conn;
    private Intent mServiceIntent;
    private DefaultDialog mProgressDialog;
    private int mLatestVersionCode;
    private boolean mIsLoaded;
    private UpdateInfo mUpdateInfo;
    private boolean mHaveNewVersion;
    private boolean mIsChecked;
    private int mLocalVersionCode = 0xffff_ffff;

    /**
     * 私有构造函数，防止其他类创建本类对象。
     */
    private Updater(Builder builder) {
        mBuilder = builder;
        mCallback = mBuilder.callback;
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
                    downloadService.setOnProgressListener(new OnProgressListener() {

                        @Override
                        public void onStartLoad() {
                            if (mCallback != null) {
                                mCallback.onStartLoad();
                            }
                            if (!mBuilder.noDialog) {
                                mBuilder.loadDialogConfig.setForceUpdate(isForceUpdate(mUpdateInfo));
                                showProgressDialog();
                            }
                        }

                        @Override
                        public void onProgress(long total, long current, int percentage) {
                            if (percentage == 100 || total == current) {
                                Utils.putApkVersionCode2Sp(mBuilder.context, mUpdateInfo.getVersionCode());
                            }
                            if (mCallback != null) {
                                mCallback.onProgress(total, current, percentage);
                            }
                            if (!mBuilder.noDialog) {
                                updateProgressDialog(percentage);
                            }
                        }

                        @Override
                        public void onLoadSuccess(Uri downUri, boolean isCache) {
                            stopService();  //结束服务
                            if (mCallback != null) {
                                mCallback.onLoadSuccess(downUri, isCache);
                                mCallback.onCompleted(true, Utils.getCurrentVersionName(mBuilder.context));
                            }
                            Utils.installApk(mBuilder.context, downUri);
                        }

                        @Override
                        public void onLoadFailed() {
                            stopService();  //结束服务
                            if (mCallback != null) {
                                mCallback.onLoadFailed();
                                mCallback.onCompleted(true, Utils.getCurrentVersionName(mBuilder.context));
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
        return conn;
    }

    /**
     * 判断当前版本是否是强制更新。
     * @return 如果是返回true，否则返回false。
     */
    private boolean isForceUpdate(@NonNull UpdateInfo updateInfo) {
        if (!updateInfo.isForceUpdate()) {
            return false;
        } else {
            int[] codes = updateInfo.getForceUpdateVersionCodes();
            if (codes == null || codes.length == 0) {
                return true;
            } else {
                for (int code : codes) {
                    if (getLocalVersionCode() == code) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    private int getLocalVersionCode() {
        if (mLocalVersionCode == 0xffff_ffff) {
            mLocalVersionCode = Utils.getCurrentVersionCode(mBuilder.context);
        }
        return mLocalVersionCode;
    }

    /**
     * 显示进度条对话框。
     */
    private void showProgressDialog() {
        mProgressDialog = new DefaultDialog(mBuilder.context, mBuilder.loadDialogConfig);
        mProgressDialog.show();
    }

    /**
     * 更新进度条对话框进度。
     * @param percentage 当前的百分比。
     */
    private void updateProgressDialog(int percentage) {
        if (mProgressDialog != null) {
            mProgressDialog.updateDownLoadsProgress(percentage);
        }
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
        if (updateInfo != null && mUpdateInfo != updateInfo) {
            mIsChecked = true;
            mUpdateInfo = updateInfo;
            mBuilder.informDialogConfig.setMsg(updateInfo.getUpdateMessage());
            mLatestVersionCode = Utils.getApkVersionCodeFromSp(mBuilder.context);
            //如果这个条件满足说明上一次没有安装。有因为即使上一次没有安装最新的版本也有可能超出了上一次下载的版本，所以要在这里判断。
            if (mLatestVersionCode == updateInfo.getVersionCode() && new File(Utils.getApkPathFromSp(mBuilder.context)).exists()) {
                mIsLoaded = true;
            } else {
                Utils.removeOldApk(mBuilder.context);
            }
            if (updateInfo.getVersionCode() > getLocalVersionCode()) {
                mHaveNewVersion = true;
                if (!mBuilder.noDialog) {
                    mBuilder.informDialogConfig.setForceUpdate(isForceUpdate(updateInfo));
                    showUpdateInformDialog();
                } else {
                    if (mCallback != null) {
                        mCallback.onShowCheckHintDialog();
                    }
                }
            } else {
                if (mCallback != null) {
                    mCallback.onCompleted(false, Utils.getCurrentVersionName(mBuilder.context));
                }
            }
        }
    }

    /**
     * 显示更新提醒。
     */
    private void showUpdateInformDialog() {
        new DefaultDialog(mBuilder.context, mBuilder.informDialogConfig)
                .show(new DialogListener() {
                    @Override
                    public void onDialogDismiss(boolean isSure) {
                        respondCheckHandlerResult(isSure);
                    }
                });
    }

    /**
     * 设置检查更新的对话框的操作结果。如果你没有关闭默认的对话框使用自定义对话框的话请不要手动调用该方法。
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
     * @param isContinue 是否继续，如果继续则说明统一更新，否则就是不统一更新。
     */
    private void respondCheckHandlerResult(boolean isContinue) {
        if (isContinue && mHaveNewVersion) {
            mIsLoaded = false;
            if (mIsLoaded) {
                File file = new File(Utils.getApkPathFromSp(mBuilder.context));
                Uri apkPath = Uri.fromFile(file);
                if (mCallback != null) {
                    mCallback.onLoadSuccess(apkPath, true);
                    mCallback.onCompleted(true, Utils.getCurrentVersionName(mBuilder.context));
                }
                Utils.installApk(mBuilder.context, apkPath);
            } else {
                mBuilder.fileName = getApkName(mUpdateInfo);
                startDownload(mBuilder.fileName, mUpdateInfo.getDownLoadsUrl(), isForceUpdate(mUpdateInfo), mBuilder.mTitle, mBuilder.mDescription);
            }
        } else {
            if (mCallback != null) {
                mCallback.onLoadCancelled();
            }
        }
    }

    private String getApkName(@NonNull UpdateInfo updateInfo){
        return TextUtils.isEmpty(updateInfo.getApkName()) ? getDefaultApkName() : updateInfo.getApkName();
    }

    /**
     * 开始下载。
     * @param updateInfo apk名称。
     * @param notifyCationTitle 下载过程中通知栏的标题。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     * @param notifyCationDesc 下载过程中通知栏的描述。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     */
    public void download(@NonNull UpdateInfo updateInfo, CharSequence notifyCationTitle, CharSequence notifyCationDesc) {
        if (mIsChecked) {  //如果检查更新不是自己检查的就不能调用这个方法。
            throw new IllegalStateException("Because you update the action is completed, so you can't call this method.");
        }
        if (TextUtils.isEmpty(notifyCationTitle)) {
            notifyCationTitle = "正在下载更新";
        }
        mUpdateInfo = updateInfo;
        startDownload(getApkName(updateInfo), updateInfo.getDownLoadsUrl(), isForceUpdate(updateInfo), notifyCationTitle, notifyCationDesc);
    }

    /**
     * 开始下载。
     * @param apkName apk名称。
     * @param downLoadsUrl 下载地址。
     * @param isForceUpdate 是否强制更新。
     * @param notifyCationTitle 下载过程中通知栏的标题。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     * @param notifyCationDesc 下载过程中通知栏的描述。如果是强制更新的话该参数可以为null，因为强制更新没有通知栏提示。
     */
    private void startDownload(@NonNull String apkName, @NonNull String downLoadsUrl, boolean isForceUpdate, CharSequence notifyCationTitle, CharSequence notifyCationDesc) {
        mServiceIntent = new Intent(mBuilder.context, DownloadService.class);
        mServiceIntent.putExtra(DownloadService.KEY_APK_NAME, apkName);
        mServiceIntent.putExtra(DownloadService.KEY_DOWNLOAD_URL, downLoadsUrl);
        mServiceIntent.putExtra(DownloadService.KEY_IS_FORCE_UPDATE, isForceUpdate);
        mServiceIntent.putExtra(DownloadService.KEY_NOTIFY_TITLE, notifyCationTitle);
        mServiceIntent.putExtra(DownloadService.KEY_NOTIFY_DESCRIPTION, notifyCationDesc);

        mBuilder.context.startService(mServiceIntent);
        isBindService = mBuilder.context.bindService(mServiceIntent, getServiceConnection(), Context.BIND_AUTO_CREATE);
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
        private InformDialogConfig informDialogConfig = new InformDialogConfig();
        private DownloadDialogConfig loadDialogConfig = new DownloadDialogConfig();
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
        /**
         * 是否没有对话框。
         */
        private boolean noDialog;

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
         * 如果你希望自己创建对话框，而不适用默认提供的对话框，可以调用该方法将默认的对话框关闭。
         * 如果你关闭了默认的对话框的话就必须自己实现UI交互，并且在用户更新提示做出反应的时候调用
         * {@link #setCheckHandlerResult(boolean)} 方法。
         */
        public Builder setNoDialog() {
            this.noDialog = true;
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
