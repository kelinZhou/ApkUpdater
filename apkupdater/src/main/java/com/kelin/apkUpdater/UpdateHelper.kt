package com.kelin.apkUpdater

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.*

/**
 * 描述 工具集合类。
 * 创建人 kelin
 * 创建时间 2016/10/14  下午12:26
 */
object UpdateHelper {
    private const val CONFIG_NAME = "com_kelin_apkUpdater_config"
    /**
     * apk 文件存储路径
     */
    private const val SP_KEY_DOWNLOAD_APK_PATH = "com.kelin.apkUpdater.sp_key_download_apk_path"
    /**
     * 上一次下载的APK的版本号。
     */
    private const val SP_KEY_DOWNLOAD_APK_VERSION_CODE = "com.kelin.apkUpdater.sp_key_download_apk_version_code"
    /**
     * 用来获取下载Apk失败的次数。
     */
    private const val SP_KEY_DOWN_LOAD_APK_FAILED_COUNT = "com.kelin.apkUpadater.sp_key_down_load_apk_failed_count"

    /**
     * 判断当前版本是否是强制更新。
     *
     * @return 如果是返回true，否则返回false。
     */
    @JvmStatic
    fun isForceUpdate(updateInfo: UpdateInfo, context: Context): Boolean {
        return if (!updateInfo.isForceUpdate) {
            false
        } else {
            val codes = updateInfo.forceUpdateVersionCodes
            if (codes == null || codes.isEmpty()) {
                true
            } else {
                for (code in codes) {
                    if (getCurrentVersionCode(context) == code) {
                        return true
                    }
                }
                false
            }
        }
    }

    /**
     * 获取当前的版本号。
     *
     * @param context 需要一个上下文。
     * @return 返回当前的版本号。
     */
    @JvmStatic
    fun getCurrentVersionCode(context: Context): Int {
        val packageManager = context.packageManager
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return packageInfo?.versionCode ?: 0
    }

    /**
     * 获取当前的版本名称。
     *
     * @param context 需要一个上下文。
     * @return 返回当前的版本名称。
     */
    @JvmStatic
    fun getCurrentVersionName(context: Context): String {
        val packageManager = context.packageManager
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return if (packageInfo != null) packageInfo.versionName else "未知版本"
    }

    /**
     * 安装APK
     *
     * @param context [android.app.Activity] 对象。
     * @param apkFile 安装包的路径
     */
    @JvmStatic
    fun installApk(context: Context, apkFile: File?): Boolean {
        if (apkFile == null || !apkFile.exists()) {
            return false
        }
        val intent = Intent()
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory("android.intent.category.DEFAULT")
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val uri = FileProvider.getUriForFile(context, context.applicationContext.packageName + ".fileProvider", apkFile)
            intent.setDataAndType(uri, context.contentResolver.getType(uri))
            val resInfoList = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                context.grantUriPermission(resolveInfo.activityInfo.packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), getIntentType(apkFile))
        }
        context.startActivity(intent)
        try {
            Process.killProcess(Process.myPid())
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    private fun getIntentType(file: File): String? {
        return file.name.let { fileName ->
            fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length).toLowerCase(Locale.getDefault()).let {
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(it)
            }
        }
    }

    /**
     * 删除上次更新存储在本地的apk
     */
    @JvmStatic
    fun removeOldApk(context: Context) { //获取老ＡＰＫ的存储路径
        val apkFile = File(getApkPathFromSp(context))
        if (apkFile.exists() && apkFile.isFile) {
            if (apkFile.delete()) {
                getEdit(context).apply {
                    remove(SP_KEY_DOWNLOAD_APK_PATH)
                    remove(SP_KEY_DOWNLOAD_APK_VERSION_CODE)
                }.commit()
            }
        }
    }

    @JvmStatic
    fun clearDownloadFailedCount(context: Context) {
        getEdit(context).remove(SP_KEY_DOWN_LOAD_APK_FAILED_COUNT).commit()
    }

    @JvmStatic
    fun downloadFailedCountPlus(context: Context) {
        val failedCount = getDownloadFailedCount(context)
        getEdit(context).putInt(SP_KEY_DOWN_LOAD_APK_FAILED_COUNT, failedCount + 1).commit()
    }

    @JvmStatic
    fun getDownloadFailedCount(context: Context): Int {
        return context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).getInt(SP_KEY_DOWN_LOAD_APK_FAILED_COUNT, 0)
    }

    @JvmStatic
    fun putApkPath2Sp(context: Context, value: String?) {
        getEdit(context).putString(SP_KEY_DOWNLOAD_APK_PATH, value).commit()
    }

    @JvmStatic
    fun getApkPathFromSp(context: Context): String {
        return context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).getString(SP_KEY_DOWNLOAD_APK_PATH, "") ?: ""
    }

    @JvmStatic
    fun putApkVersionCode2Sp(context: Context, value: Int) {
        getEdit(context).putInt(SP_KEY_DOWNLOAD_APK_VERSION_CODE, value).commit()
    }

    @JvmStatic
    fun getApkVersionCodeFromSp(context: Context): Int {
        return context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).getInt(SP_KEY_DOWNLOAD_APK_VERSION_CODE, -1)
    }

    /**
     * 这个方法是返回一个sharedPreferences的Edit编辑器
     *
     * @param context 上下文
     * @return 返回一个Edit编辑器。
     */
    private fun getEdit(context: Context): SharedPreferences.Editor {
        val sp = context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE)
        return sp.edit()
    }

    @JvmStatic
    fun getFileSignature(file: File, type: SignatureType): String? {
        if (!file.isFile) {
            return null
        }
        val digest: MessageDigest
        var `in`: FileInputStream? = null
        val buffer = ByteArray(1024)
        var len: Int
        try {
            digest = MessageDigest.getInstance(type.typeName)
            `in` = FileInputStream(file)
            while (`in`.read(buffer, 0, 1024).also { len = it } != -1) {
                digest.update(buffer, 0, len)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            `in`?.close()
        }
        return bytesToHexString(digest.digest())
    }

    private fun bytesToHexString(src: ByteArray?): String? {
        return StringBuilder("").let { sb ->
            if (src == null || src.isEmpty()) {
                return null
            }
            for (aSrc in src) {
                Integer.toHexString(aSrc.toInt() and 0xFF).also {
                    if (it.length < 2) {
                        sb.append(0)
                    }
                    sb.append(it)
                }
            }
            sb.toString()
        }
    }
}