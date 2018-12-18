package com.kelin.apkUpdater.downloader.thread

import android.os.Handler
import com.kelin.apkUpdater.downloader.DownLoadService
import com.kelin.apkUpdater.downloader.FileInfo
import com.kelin.apkUpdater.downloader.ThreadApi
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * **描述:** 文件下载线程。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  5:00 PM
 *
 * **版本:** v 1.0.0
 */
class DownloadThread(private val fileInfo: FileInfo, private val threadInfo: ThreadInfo, private val threadApi: ThreadApi, private val handler: Handler, private val cacheDir: String) : Thread() {

    private var loadedLength = fileInfo.loadedLength
    var isDownLoading = false
    private var lastPercent = 0

    override fun run() {
        //2.如果数据库没有此下载线程的信息，则向数据库插入该线程信息
        if (!threadApi.isExist(threadInfo.url, threadInfo.id)) {
            threadApi.insertThread(threadInfo)
        }

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

            loadedLength += threadInfo.loadedLength
            //206-----部分内容和范围请求
            if (conn.responseCode == 206) {
                //读取数据
                inputStream = conn.inputStream
                val buf = ByteArray(8192) {
                    0
                }
                var len = inputStream.read(buf)
                while (len != -1) {
                    //写入文件
                    raf.write(buf, 0, len)
                    //发送广播给Activity,通知进度
                    loadedLength += len
                    val percent = ((loadedLength + 0.0) / fileInfo.length * 100).toInt()
                    if (percent != lastPercent && percent != 100) { //减少不必要的消息发送。
                        handler.obtainMessage(DownLoadService.MSG_LOAD_PROGRESS, percent, 0).sendToTarget()
                        lastPercent = percent
                    }
                    //暂停保存进度到数据库
                    if (!isDownLoading) {
                        threadApi.updateThread(threadInfo.url, threadInfo.id, loadedLength)
                        return
                    }
                    len = inputStream.read(buf)
                }
            }
            //下载完成，删除线程信息
            threadApi.deleteThread(threadInfo.url, threadInfo.id)
            //更新下载进度
            handler.obtainMessage(DownLoadService.MSG_LOAD_PROGRESS, 100, 0).sendToTarget()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            conn?.disconnect()
            raf?.close()
            inputStream?.close()
        }
    }
}