package com.kelin.apkUpdater;

import android.support.annotation.Nullable;

/**
 * 描述 需要更新的Apk信息对象。
 * 创建人 kelin
 * 创建时间 2017/3/13  下午2:32
 */

public interface UpdateInfo {

    /**
     * 获取网络上的版本号。
     * @return 返回当前对象的版本号字段的值。
     */
    int getVersionCode();

    /**
     * 获取最新版本的下载链接。
     * @return 返回当前对象的下载链接字段的值。
     */
    String getDownLoadsUrl();

    /**
     * 是否强制更新。
     * @return <code color="blue">true</code> 表示强制更新, <code color="blue">false</code> 则相反。
     */
    boolean isForceUpdate();

    /**
     * 获取强制更新的版本号，如果你的本次强制更新是针对某个或某些版本的话，你可以在该方法中返回。前提是 {@link #isForceUpdate()}
     * 返回值必须为true，否则该方法的返回值是没有意义的。
     * @return 返回你要强制更新的版本号，可以返回 null ，如果返回 null 并且 {@link #isForceUpdate()} 返回 true 的话
     * 则表示所有版本全部强制更新。
     */
    @Nullable int[] getForceUpdateVersionCodes();

    /**
     * 获取Apk文件名(例如 xxx.apk 或 xxx)。后缀名不是必须的。
     */
    @Nullable String getApkName();

    /**
     * 获取更新的内容。就是你本次更新了那些东西可以在这里返回，这里返回的内容会现在是Dialog的消息中，如果你没有禁用Dialog的话。
     * @return 返回你本次更新的内容。
     */
    CharSequence getUpdateMessage();
}
