package com.kelin.apkUpdater.downloader

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.*
import android.util.Log
import com.kelin.apkUpdater.ActivityStackManager
import com.kelin.apkUpdater.UpdateHelper
import com.kelin.apkUpdater.downloader.thread.DownloadTask
import com.kelin.apkUpdater.downloader.thread.LoadInfoThread
import java.io.File

/**
 * **描述:** 下载文件时的服务。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  2:29 PM
 *
 * **版本:** v 1.0.0
 */
class DownLoadService : Service() {

    companion object {
        private const val KEY_DOWNLOAD_FILE_INFO = "key_download_file_info"
        private const val ACTION_DOWNLOAD_START = "action_start_download"
        private const val ACTION_DOWNLOAD_PAUSE = "action_stop_download"
        /**
         * 消息：创建文件成功。
         */
        internal const val MSG_LOAD_FILE_INFO_OK = 0xF0
        /**
         * 消息：更新下载进度。
         */
        internal const val MSG_LOAD_PROGRESS = 0xF1

        internal fun getStartIntent(context: Context, url: String, name: String): Intent {
            val intent = Intent(context, DownLoadService::class.java)
            intent.action = DownLoadService.ACTION_DOWNLOAD_START
            intent.putExtra(KEY_DOWNLOAD_FILE_INFO, FileInfo(0, url, name, 0, 0))
            return intent
        }

        internal fun getPauseIntent(context: Context, url: String): Intent {
            val intent = Intent(context, DownLoadService::class.java)
            intent.action = ACTION_DOWNLOAD_PAUSE
            return intent
        }
    }

    private val myHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message?) {
            if (msg != null) {
                when (msg.what) {
                    MSG_LOAD_FILE_INFO_OK -> {
                        if (msg.obj is FileInfo) {
                            val fileInfo = msg.obj as FileInfo
                            downloader = DownloadTask(fileInfo, (Runtime.getRuntime().availableProcessors() + 1).ushr(1), cacheDir, getThreadDBHelper(), this)
                            downloader!!.download()
                        }
                    }
                    MSG_LOAD_PROGRESS -> {
                        val percent = msg.arg1
                        Log.i("---------------", percent.toString())
                        if (percent == 100) {
                            val apkFile = msg.obj as File
                            sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(apkFile)))
                            Log.i("---------------", "下载完成")
                            UpdateHelper.installApk(ActivityStackManager.getInstance().applicationContext, apkFile)
//                            Log.i("================", if (installApk) "安装成功" else "安装失败")
                        }
                    }
                }
            }
        }
    }

    private lateinit var cacheDir: String
    private var downloader: DownloadTask? = null
    private var threadDBHelper: ThreadDBHelper? = null

    override fun onCreate() {
        super.onCreate()
        cacheDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action != null) {
            when (intent.action) {
                ACTION_DOWNLOAD_START -> {
                    val fileInfo = intent.getSerializableExtra(KEY_DOWNLOAD_FILE_INFO)
                    if ((downloader == null || !downloader!!.isDownloading()) && fileInfo is FileInfo) {
                        LoadInfoThread(fileInfo, myHandler, cacheDir).start()
                    }
                }
                ACTION_DOWNLOAD_PAUSE -> {
                    downloader?.pause()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun getThreadDBHelper(): ThreadDBHelper {
        if (threadDBHelper == null) {
            threadDBHelper = ThreadDBHelper.getInstance(this@DownLoadService)
        }
        return threadDBHelper!!
    }
}