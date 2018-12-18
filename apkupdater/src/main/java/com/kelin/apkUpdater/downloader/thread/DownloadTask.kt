package com.kelin.apkUpdater.downloader.thread

import android.os.Handler
import android.util.Log
import com.kelin.apkUpdater.ApkUpdater
import com.kelin.apkUpdater.UpdateHelper
import com.kelin.apkUpdater.downloader.DownLoadService
import com.kelin.apkUpdater.downloader.FileInfo
import com.kelin.apkUpdater.downloader.ThreadApi
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/**
 * **描述:** 处理多线程下载。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/18  10:08 AM
 *
 * **版本:** v 1.0.0
 */
class DownloadTask(private val fileInfo: FileInfo, private val threadCount: Int, private val cacheDir: String, private val threadApi: ThreadApi, private val handler: Handler) {

    private val loadedLength = AtomicLong(0)
    private val downloadTreads = ArrayList<DownloadThread>(threadCount)
    private val threadPool = Executors.newCachedThreadPool()
    private var isDownLoading = false

    fun download() {
        val threads = threadApi.getThreads(fileInfo.url)
        if (threads.isEmpty()) {
            val len = fileInfo.length / threadCount
            for (i: Int in 1..threadCount) {
                val threadInfo = ThreadInfo(i, fileInfo.url, len * (i - 1), if (i == threadCount) fileInfo.length else len * i - 1, 0)
                threads.add(threadInfo)
                threadApi.insertThread(threadInfo)
            }
        }
        for (thread in threads) {
            isDownLoading = true
            val downloadThread = DownloadThread(thread)
            downloadThread.isDownLoading = true
            threadPool.execute(downloadThread)
            downloadTreads.add(downloadThread)
        }
    }

    fun pause() {
        for (tread in downloadTreads) {
            tread.isDownLoading = false
        }
        isDownLoading = false
    }

    fun isDownloading() = isDownLoading

    inner class DownloadThread(private val threadInfo: ThreadInfo) : Thread() {

        var isDownLoading = false
        override fun run() {
            var conn: HttpURLConnection? = null
            var raf: RandomAccessFile? = null
            var inputStream: InputStream? = null
            try {
                //3.连接线程的url
                val url = URL(threadInfo.url)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 5000
                conn.requestMethod = "GET"
                //4.设置下载位置
                val start = threadInfo.begin + threadInfo.loadedLength //开始位置
                //conn设置属性，标记资源的位置(这是给服务器看的)
                conn.setRequestProperty("Range", "bytes=" + start + "-" + threadInfo.end)
                //5.寻找文件的写入位置
                val file = File(cacheDir, fileInfo.name)
                //创建随机操作的文件流对象,可读、写、删除
                raf = RandomAccessFile(file, "rwd")
                raf.seek(start) //设置文件写入位置
                loadedLength.addAndGet(threadInfo.loadedLength)
                //206-----部分内容和范围请求
                if (conn.responseCode == 206) {
                    //读取数据
                    inputStream = conn.inputStream
                    val buf = ByteArray(4096)
                    var len = inputStream.read(buf)
                    while (len != -1) {
                        //写入文件
                        raf.write(buf, 0, len)
                        loadedLength.addAndGet(len.toLong())
                        threadInfo.loadedLength += len
                        val percent = (loadedLength.get().toDouble() / fileInfo.length * 100).toInt()
                        if (percent != 100) { //如果到了百分百就表示下载完了，下载完成后会在后面发送消息，这里就不发送了。
                            handler.obtainMessage(DownLoadService.MSG_LOAD_PROGRESS, percent, 0).sendToTarget()
                        }
                        //暂停保存进度到数据库
                        if (!isDownLoading) {
                            threadApi.updateThread(threadInfo.url, threadInfo.id, threadInfo.loadedLength)
                            return
                        }
                        len = inputStream.read(buf)
                    }
                }
                isDownLoading = false
                checkDownloadSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                conn?.disconnect()
                raf?.close()
                inputStream?.close()
            }
        }

        @Synchronized
        private fun checkDownloadSuccess() {
            if (downloadTreads.any { !isDownLoading }) {
                downloadTreads.clear()
                threadApi.deleteThread(threadInfo.url)
                //更新下载进度
                handler.obtainMessage(DownLoadService.MSG_LOAD_PROGRESS, 100, 0, File(cacheDir, fileInfo.name)).sendToTarget()
            }
        }
    }
}