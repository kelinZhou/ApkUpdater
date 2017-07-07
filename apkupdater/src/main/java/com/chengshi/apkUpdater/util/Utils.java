package com.chengshi.apkUpdater.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.chengshi.apkUpdater.Updater;

import java.io.File;


/**
 * 描述 工具集合类。
 * 创建人 kelin
 * 创建时间 2016/10/14  下午12:26
 */

public class Utils {

    private static final String TAG = "Utils";
    private static final String CONFIG_NAME = "com_kelin_apkUpdater_config";
    /**
     * apk 文件存储路径
     */
    private static final String SP_KEY_DOWNLOAD_APK_PATH = "com.kelin.apkUpdater.apkPath";
    /**
     * 上一次下载的APK的版本号。
     */
    private static final String SP_KEY_DOWNLOAD_APK_VERSION_CODE = "com.kelin.apkUpdater.apkVersionCode";
    /**
     * 获取当前的版本号。
     * @param context 需要一个上下文。
     * @return 返回当前的版本号。
     */
    public static int getCurrentVersionCode(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null ? packageInfo.versionCode : 0;
    }

    /**
     * 获取当前的版本名称。
     * @param context 需要一个上下文。
     * @return 返回当前的版本名称。
     */
    public static String getCurrentVersionName(Context context) {
        PackageManager packageManager = context.getPackageManager();
        PackageInfo packageInfo = null;
        try {
            packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo != null ? packageInfo.versionName : "未知版本";
    }


    /**
     * 安装APK
     * @param context {@link Activity} 对象，用户有没有安装可以通过此{@link Activity} 的 {@link Activity#onActivityResult(int, int, Intent)} 方法进行检测。
     *                                如果 requestCode == {@link Updater#REQUEST_CODE_INSTALL_APK} 就说明是用户安装的结果。
     * @param apkPath 安装包的路径
     * @param requestCode 安装APK的请求码。
     */
    public static void installApk(Activity context, Uri apkPath, int requestCode) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory("android.intent.category.DEFAULT");
        intent.setDataAndType(apkPath, "application/vnd.android.package-archive");
        context.startActivityForResult(intent, requestCode);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 删除上次更新存储在本地的apk
     */
    public static void removeOldApk(@NonNull Context context) {
        //获取老ＡＰＫ的存储路径
        File apkFile = new File(getApkPathFromSp(context));

        if (apkFile.exists() && apkFile.isFile()) {
            boolean delete = apkFile.delete();
        }
    }


    public static void putApkPath2Sp(Context context, String value) {
        getEdit(context).putString(SP_KEY_DOWNLOAD_APK_PATH, value).commit();
    }

    public static String getApkPathFromSp(Context context) {
        return context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).getString(SP_KEY_DOWNLOAD_APK_PATH, "");
    }

    public static void putApkVersionCode2Sp(Context context, int value) {
        getEdit(context).putInt(SP_KEY_DOWNLOAD_APK_VERSION_CODE, value).commit();
    }

    public static int getApkVersionCodeFromSp(Context context) {
        return context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).getInt(SP_KEY_DOWNLOAD_APK_VERSION_CODE, -1);
    }

    /**
     * 这个方法是返回一个sharedPreferences的Edit编辑器
     *
     * @param context 上下文
     * @return 返回一个Edit编辑器。
     */
    private static SharedPreferences.Editor getEdit(Context context) {
        SharedPreferences sp = context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE);
        return sp.edit();
    }
}
