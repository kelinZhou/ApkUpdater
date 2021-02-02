package com.kelin.apkUpdater

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.app.Service
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.text.TextUtils
import com.kelin.apkUpdater.UpdateHelper.getAppName
import com.kelin.apkUpdater.UpdateHelper.putApkPath2Sp
import com.kelin.apkUpdater.callback.DownloadProgressCallback
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 描述 下载APK的服务。
 * 创建人 kelin
 * 创建时间 2017/3/15  上午10:18
 * 版本 v 1.0.0
 */
class DownloadService : Service() {

    //下载任务ID
    private var downloadId: Long = 0
    private var updateType = UpdateType.UPDATE_WEAK.code
    private var mApkName: String? = null
    private var mLastFraction = -0x1
    private var mIsLoadFailed = false
    private var mCursor: Cursor? = null
    private var mLastStatus = 0
    private var mIsStarted = false
    /**
     * 服务被解绑的监听。
     */
    private var serviceUnBindListener: (() -> Unit)? = null
    /**
     * 下载进度的监听。
     */
    private var onProgressListener: DownloadProgressCallback? = null
    /**
     * 下载完成的广播接收者。
     */
    private val downLoadBroadcast by lazy { DownLoadBroadcast() }
    /**
     * 下载管理器。
     */
    private val downloadManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getSystemService(DownloadManager::class.java)
        } else {
            getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        }
    }
    /**
     * 下载进度的观察者。
     */
    private val downloadObserver by lazy { DownloadChangeObserver() }
    /**
     * 工作线程。
     */
    private val scheduledExecutorService by lazy { Executors.newSingleThreadScheduledExecutor() }
    /**
     * 更新进度的任务。
     */
    private val progressRunnable by lazy { Runnable { updateProgress() } }
    /**
     * 用于下载过程中在UI线程处理工作线程发送来的消息。
     */
    private val downLoadHandler by lazy {
        @SuppressLint("HandlerLeak")
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (onProgressListener != null) {
                    when (msg.what) {
                        WHAT_PROGRESS -> {
                            val obj = msg.obj as Int
                            if (obj == DownloadManager.STATUS_RUNNING || mLastStatus != obj) {
                                mLastStatus = obj
                                when (obj) {
                                    DownloadManager.STATUS_FAILED -> {
                                        onProgressListener!!.onLoadFailed()
                                        mIsLoadFailed = true
                                    }
                                    DownloadManager.STATUS_PAUSED -> onProgressListener!!.onLoadPaused()
                                    DownloadManager.STATUS_PENDING -> onProgressListener!!.onLoadPending()
                                    DownloadManager.STATUS_RUNNING -> {
                                        if (mLastFraction == -0x1 && !mIsStarted) {
                                            mIsStarted = true
                                            onProgressListener!!.onStartDownLoad()
                                        }
                                        //被除数可以为0，除数必须大于0
                                        if (msg.arg1 >= 0 && msg.arg2 > 0) {
                                            var fraction = ((msg.arg1 + 0f) / msg.arg2 * 100).toInt()
                                            if (fraction == 0) fraction = 1
                                            if (mLastFraction != fraction) {
                                                mLastFraction = fraction
                                                onProgressListener!!.onProgress(msg.arg1.toLong(), msg.arg2.toLong(), mLastFraction)
                                            }
                                        }
                                    }
                                    DownloadManager.STATUS_SUCCESSFUL -> if (msg.arg1 >= 0 && msg.arg2 > 0) {
                                        var fraction = ((msg.arg1 + 0f) / msg.arg2 * 100).toInt()
                                        if (fraction == 0) fraction = 1
                                        if (mLastFraction != fraction) {
                                            mLastFraction = fraction
                                            onProgressListener!!.onProgress(msg.arg1.toLong(), msg.arg2.toLong(), mLastFraction)
                                        }
                                    }
                                }
                            }
                        }
                        WHAT_COMPLETED -> if (!mIsLoadFailed) {
                            val apkFile = msg.obj as? File
                            if (apkFile != null && apkFile.exists()) {
                                onProgressListener!!.onLoadSuccess(apkFile, false)
                            } else {
                                onProgressListener!!.onLoadFailed()
                            }
                        }
                    }
                }
            }
        }
    }


    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent): IBinder {
        val downloadUrl = intent.getStringExtra(KEY_DOWNLOAD_URL)
        updateType = intent.getIntExtra(KEY_IS_FORCE_UPDATE, UpdateType.UPDATE_WEAK.code)
        mApkName = intent.getStringExtra(KEY_APK_NAME)
        downloadApk(downloadUrl)
        return DownloadBinder()
    }

    /**
     * 下载最新APK
     */
    private fun downloadApk(url: String) {
        registerContentObserver()
        val request = DownloadManager.Request(Uri.parse(url))
        val visibility = if (updateType == UpdateType.UPDATE_FORCE.code) DownloadManager.Request.VISIBILITY_HIDDEN else DownloadManager.Request.VISIBILITY_VISIBLE
        request.setTitle(getAppName(applicationContext, "更新")).setNotificationVisibility(visibility).setDestinationInExternalFilesDir(applicationContext, Environment.DIRECTORY_DOWNLOADS, mApkName)
        //将下载请求放入队列， return下载任务的ID
        downloadId = downloadManager.enqueue(request)
        registerReceiver(downLoadBroadcast, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    /**
     * 注册ContentObserver
     */
    private fun registerContentObserver() {
        contentResolver.registerContentObserver(Uri.parse("content://downloads/my_downloads"), false, downloadObserver)
    }

    /**
     * 注销ContentObserver
     */
    private fun unregisterContentObserver() {
        contentResolver.unregisterContentObserver(downloadObserver)
    }

    /**
     * 关闭定时器，线程等操作
     */
    private fun close() {
        if (scheduledExecutorService != null && !scheduledExecutorService.isShutdown) {
            scheduledExecutorService.shutdown()
        }
        mCursor?.also {
            it.close()
            mCursor = null
        }
        downLoadHandler.removeCallbacksAndMessages(null)
    }

    /**
     * 发送Handler消息更新进度和状态
     */
    private fun updateProgress() {
        try {
            val bytesAndStatus = bytesAndStatus // getBytesAndStatus 方法是查询数据库，在查询的时候有些手机会崩溃，无奈。先这么处理一下。后续换其他的实现方式。
            downLoadHandler.sendMessage(downLoadHandler.obtainMessage(WHAT_PROGRESS, bytesAndStatus[0], bytesAndStatus[1], bytesAndStatus[2]))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 通过query查询下载状态，包括已下载数据大小，总大小，下载状态
     */
    private val bytesAndStatus: IntArray
        get() {
            val bytesAndStatus = intArrayOf(-1, -1, 0)
            if (mCursor == null) {
                mCursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
            } else {
                mCursor!!.requery()
            }
            mCursor?.apply {
                if (moveToFirst()) {
                    bytesAndStatus[0] = getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    //下载文件的总大小
                    bytesAndStatus[1] = getInt(getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    //下载状态
                    bytesAndStatus[2] = getInt(getColumnIndex(DownloadManager.COLUMN_STATUS))
                }
            }
            return bytesAndStatus
        }

    /**
     * 接受下载完成广播
     */
    private inner class DownLoadBroadcast : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            val action = intent.action
            if (!TextUtils.isEmpty(action)) {
                when (action) {
                    DownloadManager.ACTION_DOWNLOAD_COMPLETE -> if (downloadId == downId && downId != -1L && downloadManager != null) {
                        val apkFile = getRealFile(downloadManager.getUriForDownloadedFile(downloadId))
                        if (apkFile != null && apkFile.exists()) {
                            val realPath = apkFile.absolutePath
                            putApkPath2Sp(applicationContext, realPath)
                        }
                        updateProgress()
                        downLoadHandler.sendMessage(downLoadHandler.obtainMessage(WHAT_COMPLETED, apkFile))
                    }
                }
            }
        }
    }

    fun getRealFile(uri: Uri?): File? {
        if (null == uri) return null
        val scheme = uri.scheme
        var path: String? = null
        if (scheme == null || ContentResolver.SCHEME_FILE == scheme) {
            path = uri.path
        } else if (ContentResolver.SCHEME_CONTENT == scheme) {
            val cursor = applicationContext.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null)
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                    if (index > -1) {
                        path = cursor.getString(index)
                    }
                }
                cursor.close()
            }
        }
        return path?.let { File(it) }
    }

    /**
     * 监听下载进度
     */
    private inner class DownloadChangeObserver internal constructor() : ContentObserver(downLoadHandler) {
        /**
         * 当所监听的Uri发生改变时，就会回调此方法
         *
         * @param selfChange 此值意义不大, 一般情况下该回调值为false
         */
        override fun onChange(selfChange: Boolean) {
            synchronized(javaClass) { scheduledExecutorService.scheduleAtFixedRate(progressRunnable, 0, 100, TimeUnit.MILLISECONDS) }
        }
    }

    internal inner class DownloadBinder : Binder() {
        /**
         * 返回当前服务的实例
         *
         * @return 返回 [DownloadService] 对象。
         */
        val service: DownloadService
            get() = this@DownloadService
    }

    /**
     * 设置进度更新监听。
     *
     * @param onProgressListener [DownloadProgressCallback] 的实现类对象。
     */
    fun setOnProgressListener(onProgressListener: DownloadProgressCallback?) {
        this.onProgressListener = onProgressListener
    }

    /**
     * 设置进度更新监听。
     *
     * @param unBindListener 解绑的监听，被解绑时会被回调。
     */
    fun setServiceUnBindListener(unBindListener: () -> Unit) {
        this.serviceUnBindListener = unBindListener
    }

    override fun onUnbind(intent: Intent): Boolean {
        serviceUnBindListener?.invoke()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        close()
        unregisterReceiver(downLoadBroadcast)
        unregisterContentObserver()
    }

    companion object {
        /**
         * 表示当前的消息类型为更新进度。
         */
        const val WHAT_PROGRESS = 0x00000101
        /**
         * 表示当前的消息类型为下载完成。
         */
        private const val WHAT_COMPLETED = 0X00000102
        /**
         * 用来获取下载地址的键。
         */
        private const val KEY_DOWNLOAD_URL = "download_url"
        /**
         * 用来获取是否强制更新的键。
         */
        private const val KEY_IS_FORCE_UPDATE = "key_is_force_update"
        /**
         * 用来获取APK名称的键。
         */
        private const val KEY_APK_NAME = "key_apk_name"

        fun obtainIntent(context: Context, downloadUrl: String, updateType: UpdateType, apkName: String): Intent {
            return Intent(context, DownloadService::class.java).apply {
                putExtra(KEY_DOWNLOAD_URL, downloadUrl)
                putExtra(KEY_APK_NAME, apkName)
                putExtra(KEY_IS_FORCE_UPDATE, updateType.code)
            }
        }
    }
}