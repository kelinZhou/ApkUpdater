package com.kelin.apkUpdater.downloader

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.kelin.apkUpdater.downloader.thread.ThreadInfo

/**
 * **描述:** 下载过程中用来存放各个线程下载信息的数据库。
 *
 * **创建人:** kelin
 *
 * **创建时间:** 2018/12/17  3:43 PM
 *
 * **版本:** v 1.0.0
 */
class ThreadDBHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, 1), ThreadApi {
    companion object {

        /**
         * 数据库名称。
         */
        private const val DB_NAME = "downloadingInfo.db"
        /**
         * 线程信息表名称。
         */
        private const val DB_TABLE_NAME = "thread_info"
        /**
         * 线程ID字段。
         */
        private const val FIELD_THREAD_ID = "thread_id"
        /**
         * URL字段。
         */
        private const val FIELD_URL = "url"
        /**
         * 开始字节位置字段。
         */
        private const val FIELD_BEGIN = "begin"
        /**
         * 结束字节位置字段。
         */
        private const val FIELD_END = "end"
        /**
         * 已下载字节长度字段。
         */
        private const val FIELD_LOADED_LENGTH = "loaded_length"
        /**
         * 创建表的SQL语句。
         */
        private const val SQL_CREATE_TABLE = "CREATE TABLE $DB_TABLE_NAME(_id INTEGER PRIMARY KEY AUTOINCREMENT, $FIELD_THREAD_ID INTEGER, $FIELD_URL TEXT, $FIELD_BEGIN INTEGER, $FIELD_END INTEGER, $FIELD_LOADED_LENGTH INTEGER)"
        /**
         * 删除表的SQL语句。
         */
        private const val SQL_DROP_TABLE = "DROP TABLE IF EXISTS $DB_TABLE_NAME"
        /**
         * 插入线程信息。
         */
        private const val SQL_INSERT_THREAD = "INSERT INTO $DB_TABLE_NAME ($FIELD_THREAD_ID, $FIELD_URL, $FIELD_BEGIN, $FIELD_END, $FIELD_LOADED_LENGTH) values(?,?,?,?,?)";
        /**
         * 删除线程信息。
         */
        private const val SQL_DELETE_THREAD = "DELETE FROM $DB_TABLE_NAME WHERE $FIELD_URL = ? AND $FIELD_THREAD_ID = ?";
        /**
         * 跟新线程信息。
         */
        private const val SQL_UPDATE_THREAD = "UPDATE $DB_TABLE_NAME SET $FIELD_LOADED_LENGTH = ? WHERE $FIELD_URL = ? AND $FIELD_THREAD_ID = ?";
        /**
         * 查询指定$FIELD_URL的所有线程。
         */
        private const val SQL_SELECT_ALL_THREAD = "SELECT * FROM $DB_TABLE_NAME WHERE $FIELD_URL = ?";
        /**
         * 根据URL和线程ID查询指定线程。
         */
        private const val SQL_SELECT_THREAD_IS_EXISTS = "SELECT * FROM $DB_TABLE_NAME WHERE $FIELD_URL = ? AND $FIELD_THREAD_ID = ?"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL(SQL_DROP_TABLE)
        db?.execSQL(SQL_CREATE_TABLE)
    }

    override fun insertThread(threadInfo: ThreadInfo) {
        writableDatabase.execSQL(SQL_INSERT_THREAD, arrayOf(threadInfo.id, threadInfo.url, threadInfo.begin, threadInfo.end, threadInfo.loadedLength))
        writableDatabase.close()
    }

    override fun deleteThread(url: String, threadId: Int) {
        writableDatabase.execSQL(SQL_DELETE_THREAD, arrayOf(url, threadId))
        writableDatabase.close()
    }

    override fun updateThread(url: String, threadId: Int, loadedLength: Long) {
        writableDatabase.execSQL(SQL_UPDATE_THREAD, arrayOf(loadedLength, url, threadId))
        writableDatabase.close()
    }

    override fun getThreads(url: String): List<ThreadInfo> {
        val cursor = writableDatabase.rawQuery(SQL_SELECT_ALL_THREAD, arrayOf(url))
        val result = ArrayList<ThreadInfo>(cursor.count)
        while (cursor.moveToNext()) {
            result.add(ThreadInfo(cursor.getInt(cursor.getColumnIndex(FIELD_THREAD_ID)),
                    cursor.getString(cursor.getColumnIndex(FIELD_URL)),
                    cursor.getLong(cursor.getColumnIndex(FIELD_BEGIN)),
                    cursor.getLong(cursor.getColumnIndex(FIELD_END)),
                    cursor.getLong(cursor.getColumnIndex(FIELD_LOADED_LENGTH))))
        }
        cursor.close()
        writableDatabase.close()
        return result
    }

    override fun isExist(url: String, threadId: Int): Boolean {
        val cursor = writableDatabase.rawQuery(SQL_SELECT_THREAD_IS_EXISTS, arrayOf(url, threadId.toString()))
        val exist = cursor.moveToNext()
        cursor.close()
        writableDatabase.close()
        return exist
    }
}