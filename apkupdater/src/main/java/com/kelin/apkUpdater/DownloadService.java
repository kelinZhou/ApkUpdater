package com.kelin.apkUpdater;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.kelin.apkUpdater.callback.DownloadProgressCallback;
import com.kelin.apkUpdater.util.AssetUtils;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 描述 下载APK的服务。
 * 创建人 kelin
 * 创建时间 2017/3/15  上午10:18
 * 版本 v 1.0.0
 */

public class DownloadService extends Service {

    /**
     * 表示当前的消息类型为更新进度。
     */
    public static final int WHAT_PROGRESS = 0x0000_0101;
    /**
     * 表示当前的消息类型为下载完成。
     */
    private static final int WHAT_COMPLETED = 0X0000_0102;
    /**
     * 用来获取下载地址的键。
     */
    public static final String KEY_DOWNLOAD_URL = "download_url";
    /**
     * 用来获取通知栏标题的键。
     */
    public static final String KEY_NOTIFY_TITLE = "key_notify_title";
    /**
     * 用来获取通知栏描述的键。
     */
    public static final String KEY_NOTIFY_DESCRIPTION = "key_notify_description";
    /**
     * 用来获取是否强制更新的键。
     */
    public static final String KEY_IS_FORCE_UPDATE = "key_is_force_update";
    /**
     * 用来获取APK名称的键。
     */
    public static final String KEY_APK_NAME = "key_apk_name";

    private DownloadManager downloadManager;
    private DownloadChangeObserver downloadObserver;
    private BroadcastReceiver downLoadBroadcast;
    private ScheduledExecutorService scheduledExecutorService;

    //下载任务ID
    private long downloadId;
    private DownloadProgressCallback onProgressListener;

    public Handler downLoadHandler;

    private Runnable progressRunnable;
    private String mNotifyTitle;
    private boolean mIsForceUpdate;
    private String mNotifyDescription;
    private String mApkName;
    private int mLastFraction = 0xFFFF_FFFF;
    /**
     * 服务被解绑的监听。
     */
    private ServiceUnBindListener serviceUnBindListener;
    private boolean mIsLoadFailed;
    private Cursor mCursor;
    private int mLastStatus;
    private boolean mIsStarted;

    @Override
    public void onCreate() {
        super.onCreate();
        if (downLoadHandler == null) {
            downLoadHandler = getHandler();
        }
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                updateProgress();
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        String downloadUrl = intent.getStringExtra(KEY_DOWNLOAD_URL);
        mNotifyTitle = intent.getStringExtra(KEY_NOTIFY_TITLE);
        mIsForceUpdate = intent.getBooleanExtra(KEY_IS_FORCE_UPDATE, false);
        mNotifyDescription = intent.getStringExtra(KEY_NOTIFY_DESCRIPTION);
        mApkName = intent.getStringExtra(KEY_APK_NAME);
        downloadApk(downloadUrl);
        return new DownloadBinder();
    }

    /**
     * 下载最新APK
     */
    private void downloadApk(String url) {
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadObserver = new DownloadChangeObserver();

        registerContentObserver();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        int visibility = mIsForceUpdate ? DownloadManager.Request.VISIBILITY_HIDDEN : DownloadManager.Request.VISIBILITY_VISIBLE;
        request.setTitle(mNotifyTitle).setDescription(mNotifyDescription).setNotificationVisibility(visibility).setDestinationInExternalFilesDir(getApplicationContext(), Environment.DIRECTORY_DOWNLOADS, mApkName);
        /*将下载请求放入队列， return下载任务的ID*/
        downloadId = downloadManager.enqueue(request);

        registerBroadcast();
    }

    /**
     * 注册广播
     */
    private void registerBroadcast() {
        /*注册service 广播 1.任务完成时 2.进行中的任务被点击*/
        registerReceiver(downLoadBroadcast = new DownLoadBroadcast(), new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    /**
     * 注销广播
     */
    private void unregisterBroadcast() {
        if (downLoadBroadcast != null) {
            unregisterReceiver(downLoadBroadcast);
            downLoadBroadcast = null;
        }
    }

    /**
     * 注册ContentObserver
     */
    private void registerContentObserver() {
        if (downloadObserver != null) {
            getContentResolver().registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, downloadObserver);
        }
    }

    /**
     * 注销ContentObserver
     */
    private void unregisterContentObserver() {
        if (downloadObserver != null) {
            getContentResolver().unregisterContentObserver(downloadObserver);
        }
    }

    /**
     * 关闭定时器，线程等操作
     */
    private void close() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown()) {
            scheduledExecutorService.shutdown();
        }

        if (mCursor != null) {
            mCursor.close();
            mCursor = null;
        }

        if (downLoadHandler != null) {
            downLoadHandler.removeCallbacksAndMessages(null);
            downLoadHandler = null;
        }
    }

    /**
     * 发送Handler消息更新进度和状态
     */
    private void updateProgress() {
        int[] bytesAndStatus = getBytesAndStatus();
        downLoadHandler.sendMessage(downLoadHandler.obtainMessage(WHAT_PROGRESS, bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]));
    }

    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     */
    private int[] getBytesAndStatus() {
        int[] bytesAndStatus = new int[]{-1, -1, 0};
        if (mCursor == null) {
            DownloadManager.Query query = new DownloadManager.Query().setFilterById(downloadId);
            mCursor = downloadManager.query(query);
        } else {
            mCursor.requery();
        }
        if (mCursor != null && mCursor.moveToFirst()) {
            //已经下载文件大小
            bytesAndStatus[0] = mCursor.getInt(mCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
            //下载文件的总大小
            bytesAndStatus[1] = mCursor.getInt(mCursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
            //下载状态
            bytesAndStatus[2] = mCursor.getInt(mCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
        }
        return bytesAndStatus;
    }

    @SuppressLint("HandlerLeak")
    private Handler getHandler() {
        if (downLoadHandler != null) return downLoadHandler;
        return new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (onProgressListener != null) {
                    switch (msg.what) {
                        case WHAT_PROGRESS:
                            int obj = (int) msg.obj;
                            if (obj == DownloadManager.STATUS_RUNNING || mLastStatus != obj) {
                                mLastStatus = obj;
                                switch (obj) {
                                    case DownloadManager.STATUS_FAILED:
                                        onProgressListener.onLoadFailed();
                                        mIsLoadFailed = true;
                                        break;
                                    case DownloadManager.STATUS_PAUSED:
                                        onProgressListener.onLoadPaused();
                                        break;
                                    case DownloadManager.STATUS_PENDING:
                                        onProgressListener.onLoadPending();
                                        break;
                                    case DownloadManager.STATUS_RUNNING:
                                        if (mLastFraction == 0xffff_ffff && !mIsStarted) {
                                            mIsStarted = true;
                                            onProgressListener.onStartLoad();
                                        }
                                    case DownloadManager.STATUS_SUCCESSFUL:
                                        //被除数可以为0，除数必须大于0
                                        if (msg.arg1 >= 0 && msg.arg2 > 0) {
                                            int fraction = (int) ((msg.arg1 + 0f) / msg.arg2 * 100);
                                            if (fraction == 0) fraction = 1;
                                            if (mLastFraction != fraction) {
                                                mLastFraction = fraction;
                                                onProgressListener.onProgress(msg.arg1, msg.arg2, mLastFraction);
                                            }
                                        }
                                        break;
                                }
                            }
                            break;

                        case WHAT_COMPLETED:
                            if (!mIsLoadFailed) {
                                File apkFile = (File) msg.obj;
                                onProgressListener.onLoadSuccess(apkFile, false);
                            }
                            break;
                    }
                }
            }
        };
    }

    /**
     * 接受下载完成广播
     */
    private class DownLoadBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            long downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            switch (intent.getAction()) {
                case DownloadManager.ACTION_DOWNLOAD_COMPLETE:
                    if (downloadId == downId && downId != -1 && downloadManager != null) {
                        File apkFile = AssetUtils.queryDownloadedApk(downloadId, downloadManager);

                        if (apkFile != null && apkFile.exists()) {
                            String realPath = apkFile.getAbsolutePath();
                            UpdateHelper.putApkPath2Sp(getApplicationContext(), realPath);
                        }
                        updateProgress();
                        downLoadHandler.sendMessage(downLoadHandler.obtainMessage(WHAT_COMPLETED, apkFile));
                    }
                    break;
            }
        }
    }

    /**
     * 监听下载进度
     */
    private class DownloadChangeObserver extends ContentObserver {

        DownloadChangeObserver() {
            super(downLoadHandler);
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        }

        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         *
         * @param selfChange 此值意义不大, 一般情况下该回调值false
         */
        @Override
        public void onChange(boolean selfChange) {
            synchronized (this.getClass()) {
                scheduledExecutorService.scheduleAtFixedRate(progressRunnable, 0, 100, TimeUnit.MILLISECONDS);
            }
        }
    }

    class DownloadBinder extends Binder {
        /**
         * 返回当前服务的实例
         *
         * @return 返回 {@link DownloadService} 对象。
         */
        DownloadService getService() {
            return DownloadService.this;
        }

    }

    /**
     * 设置进度更新监听。
     *
     * @param onProgressListener {@link DownloadProgressCallback} 的实现类对象。
     */
    void setOnProgressListener(DownloadProgressCallback onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    /**
     * 设置进度更新监听。
     *
     * @param serviceUnBindListener {@link ServiceUnBindListener} 的实现类对象。
     */
    void setServiceUnBindListener(ServiceUnBindListener serviceUnBindListener) {
        this.serviceUnBindListener = serviceUnBindListener;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (serviceUnBindListener != null) {
            serviceUnBindListener.onUnBind();
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        close();
        unregisterBroadcast();
        unregisterContentObserver();
    }

    interface ServiceUnBindListener {

        /**
         * 当服务被解绑的时候回调。
         */
        void onUnBind();
    }
}
