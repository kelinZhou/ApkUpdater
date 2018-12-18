package com.kelin.apkUpdater.downloader

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.kelin.apkUpdater.downloader.thread.DownloadThread
import com.kelin.apkUpdater.downloader.thread.LoadInfoThread
import com.kelin.apkUpdater.downloader.thread.ThreadInfo

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
        private const val ACTION_START_DOWNLOAD = "action_start_download"
        private const val ACTION_STOP_DOWNLOAD = "action_stop_download"
        /**
         * 消息：创建文件成功。
         */
        const val MSG_LOAD_FILE_INFO_OK = 0xF0
        /**
         * 消息：更新下载进度。
         */
        const val MSG_LOAD_PROGRESS = 0xF1

        fun startDownload(context: Context, id: Int, url: String, name: String) {
            val intent = Intent(context, DownLoadService::class.java)
            intent.action = ACTION_START_DOWNLOAD
            intent.putExtra(KEY_DOWNLOAD_FILE_INFO, FileInfo(id, url, name, 0, 0))
            context.startService(intent)
        }

        fun stopDownLoad(context: Context) {
            val intent = Intent(context, DownLoadService::class.java)
            intent.action = ACTION_STOP_DOWNLOAD
            context.startService(intent)
        }
    }

    private val myHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message?) {
            if (msg != null) {
                when (msg.what) {
                    MSG_LOAD_FILE_INFO_OK -> {
                        if (msg.obj is FileInfo) {
                            val fileInfo = msg.obj as FileInfo
                            val threadApi = ThreadDBHelper(this@DownLoadService)
                            val threads = threadApi.getThreads(fileInfo.url)
                            val thread = if (threads.isEmpty()) {
                                ThreadInfo(0, fileInfo.url, 0, fileInfo.length, 0)
                            } else {
                                threads[0]
                            }
                            downloadThread = DownloadThread(fileInfo, thread, threadApi, this, cacheDir)
                            downloadThread!!.isDownLoading = true
                            downloadThread!!.start()
                        }
                    }
                    MSG_LOAD_PROGRESS -> {
                        val percent = msg.arg1
                        Log.i("=============", percent.toString())
                        if (percent == 100) {
                            Log.i("==================", "下载完成")
                        }
                    }
                }
            }
        }
    }
    private lateinit var cacheDir: String
    private var downloadThread: DownloadThread? = null

    override fun onCreate() {
        super.onCreate()
        cacheDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).absolutePath
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action != null) {
            when (intent.action) {
                ACTION_START_DOWNLOAD -> {
                    val fileInfo = intent.getSerializableExtra(KEY_DOWNLOAD_FILE_INFO)
                    if ((downloadThread == null || !downloadThread!!.isDownLoading) && fileInfo is FileInfo) {
                        LoadInfoThread(fileInfo, myHandler, cacheDir).start()
                    }
                }
                ACTION_STOP_DOWNLOAD -> {
                    downloadThread?.isDownLoading = false
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}