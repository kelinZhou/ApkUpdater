package com.kelin.apkUpdater.downloader

import com.kelin.apkUpdater.downloader.thread.ThreadInfo

/**
 * **描述:** 下载线程信息的接口。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  4:30 PM
 *
 * **版本:** v 1.0.0
 */
interface ThreadApi {

    /**
     * 插入线程信息。
     * @param threadInfo 要插入的线程信息对象。
     */
    fun insertThread(threadInfo: ThreadInfo)

    /**
     * 删除指定URL的所有线程。
     */
    fun deleteThread(url:String)

    /**
     * 更新线程信息。
     * @param url 要更新的线程的URL。
     * @param threadId 要更新的线程ID。
     * @param loadedLength 当前线程已下载的字节长度。
     */
    fun updateThread(url: String, threadId: Int, loadedLength: Long)

    /**
     * 获取指定URL的所有线程信息。
     * @param url 要获取的URL。
     */
    fun getThreads(url: String): MutableList<ThreadInfo>

    /**
     * 判断指定线程是否存在。
     * @param url 线程的URL。
     * @param threadId 线程ID。
     */
    fun isExist(url: String, threadId: Int): Boolean
}