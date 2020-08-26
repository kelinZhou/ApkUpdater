package com.kelin.apkUpdater.callback

import com.kelin.apkUpdater.ApkUpdater

/**
 * **描述:** 检查更新的监听。
 *
 *
 * **创建人:** kelin
 *
 *
 * **创建时间:** 2018/9/10  上午11:44
 *
 *
 * **版本:** v 1.0.0
 */
interface IUpdateCallback {
    /**
     * 当用户需要静默下载的时候调用。通常情况下该方法不会执行，如果你使用了我所提供的Dialog(没有自定义Dialog的话)，在非强制更新的时候
     * 下载进度Dialog上会有一个名为悄悄下载的按钮，点击这个按钮后就会执行该方法。
     * 如果你使用了自定义Dialog的话你可以在用户希望静默下载的时候调用 [ApkUpdater.silentDownload] 方法，这样的话该回调也会被执行。
     *
     * @param apkUpdater [ApkUpdater] 对象。
     */
    fun onSilentDownload(apkUpdater: ApkUpdater)

    /**
     * 当本次检测更新成功的时候调用。
     *
     * @param isAutoCheck    该参数说明了本次检测更新是手动检测更新还是自动检测更新。
     * @param haveNewVersion 是否有新的版本。
     * `true`表示有新的版本,
     * `false`则表示没有新的版本。
     * @param curVersionName 当前app的版本名称。
     * @param isForceUpdate  是否是强制更新。这个字段的值其实就[com.kelin.apkUpdater.UpdateInfo.isForceUpdate]和
     * [com.kelin.apkUpdater.UpdateInfo.forceUpdateVersionCodes]}的返回值。
     */
    fun onSuccess(isAutoCheck: Boolean, haveNewVersion: Boolean, curVersionName: String, isForceUpdate: Boolean)

    /**
     * 当失败的时候被执行，无论是检测更新失败，还是下载失败，都会执行。
     *
     * @param isAutoCheck         该参数说明了本次检测更新是手动检测更新还是自动检测更新。
     * @param isCanceled          是否是用户取消了下载更新。
     * @param haveNewVersion      是否有新的版本。
     * `true`表示有新的版本,
     * `false`则表示没有新的版本。
     * @param curVersionName      当前app的版本名称。
     * @param checkMD5failedCount MD5校验失败失败次数，如果下载失败的原因是因为校验MD5失败的话该参数将会是一个大于0的数字。
     * @param isForceUpdate       是否是强制更新。这个字段的值其实就[com.kelin.apkUpdater.UpdateInfo.isForceUpdate]和
     * [com.kelin.apkUpdater.UpdateInfo.forceUpdateVersionCodes]}的返回值。
     */
    fun onFiled(isAutoCheck: Boolean, isCanceled: Boolean, haveNewVersion: Boolean, curVersionName: String, checkMD5failedCount: Int, isForceUpdate: Boolean)

    /**
     * 当任务完毕后被调用，无论任务成功还是失败，也无论是否需要更新。
     * 该方法总是在[.onSuccess] 或
     * [.onFiled]方法之后调用。
     *
     *
     * 如果在检查更新阶段发现没有新的版本则会直接执行该方法，如果检查更新失败也会执行该方法，
     * 如果检测到了新的版本的话，那么这个方法就不会再检查更新阶段调用，一直等到下载完成或下载失败之后才会被执行。
     */
    fun onCompleted()
}