package com.kelin.apkUpdater.callback;

import com.kelin.apkUpdater.UpdateInfo;

/**
 * **描述:** 检查更新的监听。
 * <p>
 * **创建人:** kelin
 * <p>
 * **创建时间:** 2018/9/10  上午11:44
 * <p>
 * **版本:** v 1.0.0
 */
public interface IUpdateCallback {

    /**
     * 当任务完毕后被调用。无论任务成功还是失败，也无论是否需要更新。如果在检查更新阶段发现没有新的版本则会直接执行
     * 该方法，如果检查更新失败也会执行该方法，如果检测到了新的版本的话，那么这个方法就不会再检查更新阶段调用，一直
     * 等到下载完成或下载失败之后才会被执行。
     *
     * @param haveNewVersion      是否有新的版本。
     *                            <code color="blue">true</code>表示有新的版本,
     *                            <code color="blue">false</code>则表示没有新的版本。
     * @param curVersionName      当前app的版本名称。
     * @param successful          本次检测更新是否是成功的。这里的说所的成功的意思就是即检测到了新的版本且下载的安装包是有效且可以安装的。
     * @param checkMD5failedCount MD5校验失败失败次数，如果 successful 的值为true的话改字段的值绝对为0且毫无意义。只有当 successful 字段的
     *                            值为false的时候该字段的值才会有可能大于0，用于表示当前是第几次MD5校验失败。一般连续失败的次数过多您就需要提醒用户
     *                            变更一下网络环境再次重试了。因为有些网络环境确实会存在一直丢包的这种问题，就例如我以前遇到过的长城宽带
     *                            一直丢包，导致我下载的文件一直是损坏的。
     * @param isForceUpdate       是否是强制更新。这个字段的值其实就{@link UpdateInfo#isForceUpdate()}的返回值。
     */
    void onCompleted(boolean haveNewVersion, String curVersionName, boolean successful, int checkMD5failedCount, boolean isForceUpdate);
}
