package com.kelin.apkUpdater;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;


/**
 * 描述 工具集合类。
 * 创建人 kelin
 * 创建时间 2016/10/14  下午12:26
 */

public class UpdateHelper {

    private UpdateHelper() {
        throw new InstantiationError("Utility class don't need to instantiate！");
    }

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
     * 判断当前版本是否是强制更新。
     *
     * @return 如果是返回true，否则返回false。
     */
    public static boolean isForceUpdate(@NonNull UpdateInfo updateInfo, @NonNull Context context) {
        if (!updateInfo.isForceUpdate()) {
            return false;
        } else {
            int[] codes = updateInfo.getForceUpdateVersionCodes();
            if (codes == null || codes.length == 0) {
                return true;
            } else {
                for (int code : codes) {
                    if (getCurrentVersionCode(context) == code) {
                        return true;
                    }
                }
                return false;
            }
        }
    }

    /**
     * 获取当前的版本号。
     *
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
     *
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
     *
     * @param context {@link Activity} 对象。
     * @param apkFile 安装包的路径
     */
    public static boolean installApk(Context context, File apkFile) {
        if (apkFile == null || !apkFile.exists()) {
            return false;
        }
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addCategory("android.intent.category.DEFAULT");
        // 判断版本大于等于7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 给目标应用一个临时授权
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", apkFile);
            intent.setDataAndType(uri, context.getContentResolver().getType(uri));
        } else {
            intent.setDataAndType(Uri.fromFile(apkFile), getIntentType(apkFile));
        }
        context.startActivity(intent);
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static String getIntentType(File file) {
        String suffix = file.getName();
        String name = suffix.substring(suffix.lastIndexOf(".") + 1, suffix.length()).toLowerCase();
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(name);
    }

    /**
     * 删除上次更新存储在本地的apk
     */
    static void removeOldApk(@NonNull Context context) {
        //获取老ＡＰＫ的存储路径
        File apkFile = new File(getApkPathFromSp(context));

        if (apkFile.exists() && apkFile.isFile()) {
            if (apkFile.delete()) {
                getEdit(context).remove(SP_KEY_DOWNLOAD_APK_PATH);
                getEdit(context).remove(SP_KEY_DOWNLOAD_APK_VERSION_CODE);
            }
        }
    }

    static void putApkPath2Sp(Context context, String value) {
        getEdit(context).putString(SP_KEY_DOWNLOAD_APK_PATH, value).commit();
    }

    static String getApkPathFromSp(Context context) {
        return context.getSharedPreferences(CONFIG_NAME, Context.MODE_PRIVATE).getString(SP_KEY_DOWNLOAD_APK_PATH, "");
    }

    static void putApkVersionCode2Sp(Context context, int value) {
        getEdit(context).putInt(SP_KEY_DOWNLOAD_APK_VERSION_CODE, value).commit();
    }

    static int getApkVersionCodeFromSp(Context context) {
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


    public static String getFileMD5(File file) {
        if (!file.isFile()) {
            return null;
        }
        MessageDigest digest;
        FileInputStream in;
        byte buffer[] = new byte[1024];
        int len;
        try {
            digest = MessageDigest.getInstance("MD5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return bytesToHexString(digest.digest());
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }
}
