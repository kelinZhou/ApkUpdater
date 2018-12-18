package com.kelin.apkUpdater.downloader

import android.content.Context

/**
 * **描述:** 文件下载器的具体实现
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/18  1:44 PM
 *
 * **版本:** v 1.0.0
 */
internal class DownloaderImpl(private val context: Context) : Downloader {

    override fun startDownload(url: String, name: String) {
        context.startService(DownLoadService.getStartIntent(context, url, name))
    }

    override fun pauseDownload(url: String) {
        context.startService(DownLoadService.getPauseIntent(context, url))
    }

    override fun stopDownload(url: String, deleteResource: Boolean) {
        //not implementation
    }
}