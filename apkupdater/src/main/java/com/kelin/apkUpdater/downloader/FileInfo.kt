package com.kelin.apkUpdater.downloader

import java.io.Serializable

/**
 * **描述:** 下载文件时用来保存文件信息。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  2:24 PM
 *
 * **版本:** v 1.0.0
 */
data class FileInfo(
        /**
         * 文件编号。
         */
        val id: Int,
        /**
         * 下载的Url地址。
         */
        val url: String,
        /**
         * 文件名称。
         */
        val name: String,
        /**
         * 文件长度。
         */
        var length: Long,
        /**
         * 已下载的文件长度。
         */
        var loadedLength: Long) : Serializable