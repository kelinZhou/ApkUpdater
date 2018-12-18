package com.kelin.apkUpdater.downloader

import android.content.Context

/**
 * **描述:** 文件下载器。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/18  1:35 PM
 *
 * **版本:** v 1.0.0
 */
interface Downloader {

    companion object {
        fun createDownloader(context: Context): Downloader = DownloaderImpl(context)
    }

    /**
     * 启动下载。
     * @param url 下载链接。
     * @param name 文件名。
     */
    fun startDownload(url: String, name: String)

    /**
     * 暂停下载。
     * @param url 下载链接。
     */
    fun pauseDownload(url: String)

    /**
     * 停止下载。
     * @param url 下载链接。
     * @param deleteResource 是否删除已下载的资源文件，可以为空。默认删除。
     */
    fun stopDownload(url: String, deleteResource: Boolean = true)
}