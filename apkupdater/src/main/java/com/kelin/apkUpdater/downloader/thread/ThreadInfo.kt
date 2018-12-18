package com.kelin.apkUpdater.downloader.thread

/**
 * **描述:** 文件下载线程的下载信息。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  3:39 PM
 *
 * **版本:** v 1.0.0
 */
data class ThreadInfo(
        /**
         * 线程ID。
         */
        val id: Int,
        /**
         * 下载地址。
         */
        val url: String,
        /**
         * 多线程下载时，当前线程的开始位置。
         */
        var begin: Long,
        /**
         * 多线程下载时，当前线程的结束位置。
         */
        var end: Long,
        /**
         * 已下载的长度。
         */
        var loadedLength: Long)