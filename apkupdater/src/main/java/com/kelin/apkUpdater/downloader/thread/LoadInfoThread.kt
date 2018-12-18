package com.kelin.apkUpdater.downloader.thread

import android.os.Handler
import com.kelin.apkUpdater.downloader.DownLoadService
import com.kelin.apkUpdater.downloader.FileInfo
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * **描述:** 获取文件信息时所需要的线程。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  2:51 PM
 *
 * **版本:** v 1.0.0
 */
class LoadInfoThread(private val fileInfo: FileInfo, private val handler: Handler, private val cacheDir: String) : Thread() {

    override fun run() {
        var conn: HttpURLConnection? = null
        var raf: RandomAccessFile? = null
        try {
            //1.连接网络文件
            val url = URL(fileInfo.url)
            url.openConnection()
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.requestMethod = "GET"
            if (conn.responseCode == 200) {
                //2.获取文件长度
                val len = conn.contentLength.toLong()
                if (len > 0) {
                    val dir = File(cacheDir)
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    //3.创建等大的本地文件
                    val file = File(dir, fileInfo.name)
                    //创建随机操作的文件流对象,可读、写、删除
                    raf = RandomAccessFile(file, "rwd");
                    raf.setLength(len) //设置文件大小
                    fileInfo.length = len
                    //4.从mHandler的消息池中拿个消息，附带mFileBean和MSG_CREATE_FILE_OK标示发送给mHandler
                    handler.obtainMessage(DownLoadService.MSG_LOAD_FILE_INFO_OK, fileInfo).sendToTarget();
                }
            }
        } catch (e: Exception) {
            e.printStackTrace();
        } finally {
            conn?.disconnect()
            try {
                raf?.close()
            } catch (e: IOException) {
                e.printStackTrace();
            }
        }
    }
}